package com.wsy.tiqucodesandbox;

import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.ArrayUtil;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import com.wsy.tiqucodesandbox.model.ExecuteCodeRequest;
import com.wsy.tiqucodesandbox.model.ExecuteCodeResponse;
import com.wsy.tiqucodesandbox.model.ExecuteMessage;
import org.springframework.util.StopWatch;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 使用docker代码沙箱
 */
public class JavaDockerCodeSandbox extends JavaCodeSandboxTemplate {

    public static final long TIME_OUT = 5000L;

    public static final Boolean FIRST_INIT = false;

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        return super.executeCode(executeCodeRequest);
    }

    @Override
    public List<ExecuteMessage> runFile(File userCodeFile, List<String> inpuList) {
        String userCodeParentPath = userCodeFile.getParentFile().getAbsolutePath();
        //创建容器，把文件复制到容器内
        //1.获取默认的Docker Client
        DockerClient dockerClient = DockerClientBuilder.getInstance().build();
        //2.拉取镜像
        String image = "openjdk:8-alpine";
        if (FIRST_INIT) {
            PullImageCmd pullImageCmd = dockerClient.pullImageCmd(image);
            PullImageResultCallback pullImageResultCallback = new PullImageResultCallback() {
                @Override
                public void onNext(PullResponseItem item) {
                    System.out.println("下载镜像: " + item.getStatus());
                    super.onNext(item);
                }
            };
            try {
                pullImageCmd.exec(pullImageResultCallback).awaitCompletion();
            } catch (InterruptedException e) {
                System.out.println("拉取镜像异常");
                throw new RuntimeException(e);
            }
        }
        System.out.println("下载完成");

        //3.创建容器
        //创建容器的启动配置
        HostConfig hostConfig = new HostConfig();
        //指定容器与本地文件的映射，容器挂载目录
        hostConfig.withMemory(100 * 1000 * 1000L);
        hostConfig.withMemorySwap(0L);
        hostConfig.withCpuCount(1L);
        hostConfig.setBinds(new Bind(userCodeParentPath, new Volume("/app")));
        CreateContainerCmd containerCmd = dockerClient.createContainerCmd(image);
        CreateContainerResponse createContainerResponse = containerCmd
                .withHostConfig(hostConfig)
                .withNetworkDisabled(true)
                .withReadonlyRootfs(true)
                .withAttachStdin(true)
                .withAttachStderr(true)
                .withAttachStdout(true)
                .withTty(true)
                .exec();
        String containerId = createContainerResponse.getId();

        //4.启动容器
        StartContainerCmd startContainerCmd = dockerClient.startContainerCmd(containerId);
        startContainerCmd.exec();

        //执行容器 docker exec quizzical_carver java -cp /app Main 1 3
        //执行命令并获取结果
        List<ExecuteMessage> executeMessages = new ArrayList<>();
        for (String inputArgs : inpuList) {
            StopWatch stopWatch = new StopWatch();
            String[] inputArrays = inputArgs.split(" ");
            String[] cmdArray = ArrayUtil.append(new String[]{"java", "-cp", "/app", "Main"}, inputArrays);
            ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(containerId)
                    .withCmd(cmdArray)
                    .withAttachStderr(true)
                    .withAttachStdout(true)
                    .withAttachStdin(true)
                    .exec();
            System.out.println("创建执行命令：" + execCreateCmdResponse);

            ExecuteMessage executeMessage = new ExecuteMessage();
            final String[] message = {null};
            final String[] errorMessage = {null};
            long time = 0L;
            //判断是否超时
            final boolean timeout[] = {true};
            String execCreateCmdResponseId = execCreateCmdResponse.getId();
            ExecStartResultCallback execStartResultCallback = new ExecStartResultCallback() {
                @Override
                public void onComplete() {
                    timeout[0] = false;
                    super.onComplete();
                }

                @Override
                public void onNext(Frame frame) {
                    if (frame.getStreamType().equals(StreamType.STDERR)) {
                        errorMessage[0] = new String(frame.getPayload());
                        System.out.println("输出错误结果:" + errorMessage[0]);
                    } else {
                        message[0] = new String(frame.getPayload());
                        System.out.println("输出结果:" + message[0]);
                    }
                    super.onNext(frame);
                }
            };
            //获取占用内存
            final long[] memory = {0L};
            StatsCmd statsCmd = dockerClient.statsCmd(containerId);
            ResultCallback<Statistics> resultCallBack = statsCmd.exec(new ResultCallback<Statistics>() {
                @Override
                public void onStart(Closeable closeable) {

                }

                @Override
                public void onNext(Statistics statistics) {
                    System.out.println("内存占用:" + statistics.getMemoryStats().getUsage());
                    memory[0] = Math.max(memory[0], statistics.getMemoryStats().getUsage());
                }

                @Override
                public void onError(Throwable throwable) {

                }

                @Override
                public void onComplete() {

                }

                @Override
                public void close() throws IOException {

                }
            });
            statsCmd.exec(resultCallBack);
            try {
                stopWatch.start();
                dockerClient.execStartCmd(execCreateCmdResponseId)
                        .exec(execStartResultCallback)
                        .awaitCompletion(TIME_OUT, TimeUnit.MILLISECONDS);
                stopWatch.stop();
                time = stopWatch.getLastTaskTimeMillis();
                statsCmd.close();
            } catch (InterruptedException e) {
                System.out.println("执行容器异常");
                throw new RuntimeException(e);
            }
            executeMessage.setMessage(message[0]);
            executeMessage.setErrorMessage(errorMessage[0]);
            executeMessage.setTime(time);
            executeMessage.setMemory(memory[0]);
            executeMessages.add(executeMessage);
        }
        return executeMessages;
    }
}
