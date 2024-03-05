package com.wsy.tiqucodesandbox;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.wsy.tiqucodesandbox.model.ExecuteCodeRequest;
import com.wsy.tiqucodesandbox.model.ExecuteCodeResponse;
import com.wsy.tiqucodesandbox.model.ExecuteMessage;
import com.wsy.tiqucodesandbox.model.JudgeInfo;
import com.wsy.tiqucodesandbox.utils.ProcessUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
public abstract class JavaCodeSandboxTemplate implements CodeSandbox {

    public static final String GLOBAL_CODE_DIR_NAME = "tmpCode";

    public static final String GLOBAL_JAVA_CLASS_NAME = "Main.java";

    public static final long TIME_OUT = 5000L;

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        List<String> inpuList = executeCodeRequest.getInpuList();
        String code = executeCodeRequest.getCode();
        //1. 把用户代码保存为文件
        File userCodeFile = saveCodeToFile(code);

        //2. 编译代码，得到class文件
        ExecuteMessage compileFileExecuteMessage = compileFile(userCodeFile);
        System.out.println(compileFileExecuteMessage);

        //3. 执行代码，得到输出结果
        List<ExecuteMessage> runFileExecuteMessageList = runFile(userCodeFile, inpuList);

        //4.整理输出结果
        ExecuteCodeResponse outputResponse = getOutputResponse(runFileExecuteMessageList);

        //5.文件清理
        boolean deleteResult = deleteFile(userCodeFile);
        if (!deleteResult) {
            log.error("delete file error, userCodeFilePath = {}", userCodeFile.getParentFile().getAbsolutePath());
        }

        //6.返回输出结果
        return outputResponse;
    }

    /**
     * 把用户代码保存为文件
     * @param code 用户代码
     * @return
     */
    public File saveCodeToFile(String code) {
        String userDir = System.getProperty("user.dir");
        String globalCodePathName = userDir + File.separator + GLOBAL_CODE_DIR_NAME;
        //判断全局代码目录是否存在，没有则新建
        if (!FileUtil.exist(globalCodePathName)) {
            FileUtil.mkdir(globalCodePathName);
        }
        //把用户的代码隔离存放
        String userCodeParentPath = globalCodePathName + File.separator + UUID.randomUUID();
        String userCodePath = File.separator + GLOBAL_JAVA_CLASS_NAME;
        File userCodeFile = FileUtil.writeString(code, userCodeParentPath + userCodePath, StandardCharsets.UTF_8);
        return userCodeFile;
    }

    /**
     * 编译文件
     * @param userCodeFile
     * @return
     */
    public ExecuteMessage compileFile(File userCodeFile) {
        String compileCmd = String.format("javac -encoding utf-8 %s", userCodeFile.getAbsolutePath());
        try {
            Process compileProcess = Runtime.getRuntime().exec(compileCmd);
            ExecuteMessage executeMessage = ProcessUtils.runProcessAndGetMessage(compileProcess, "编译");
            if (executeMessage.getExitValue() != 0) {
                throw new RuntimeException("编译错误");
            }
            return executeMessage;
        } catch (Exception e) {
//            return getErrorResponse(e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 执行文件，获得执行结果列表
     * @param userCodeFile
     * @param inpuList
     * @return
     */
    public List<ExecuteMessage> runFile(File userCodeFile, List<String> inpuList) {
        String userCodeParentPath = userCodeFile.getParentFile().getAbsolutePath();
        List<ExecuteMessage> executeMessages = new ArrayList<>();
        for (String inputArgs : inpuList) {
            String runCmd = String.format("java -Xmx256m -Dfile.encoding=UTF-8 -cp %s Main %s", userCodeParentPath, inputArgs);
            try {
                Process runProcess = Runtime.getRuntime().exec(runCmd);
                //超时控制
                new Thread(() -> {
                    try {
                        Thread.sleep(TIME_OUT);
                        System.out.println("超时啦");
                        runProcess.destroy();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }).start();
                ExecuteMessage executeMessage = ProcessUtils.runProcessAndGetMessage(runProcess, "运行");
                executeMessages.add(executeMessage);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return executeMessages;
    }

    /**
     * 获取输出结果
     * @param executeMessages
     * @return
     */
    public ExecuteCodeResponse getOutputResponse(List<ExecuteMessage> executeMessages) {
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        List<String> outputList = new ArrayList<>();
        //取用时最大值，便于判断是否超时
        long maxTime = 0;
        for (ExecuteMessage executeMessage : executeMessages) {
            String errorMessage = executeMessage.getErrorMessage();
            if (StrUtil.isNotBlank(errorMessage)) {
                executeCodeResponse.setMessage(errorMessage);
                executeCodeResponse.setStatus(3);
                break;
            }
            outputList.add(executeMessage.getMessage());
            Long time = executeMessage.getTime();
            if (time != null) {
                maxTime = Math.max(maxTime, time);
            }
        }
        //程序正常执行完成
        if (outputList.size() == executeMessages.size()) {
            executeCodeResponse.setStatus(1);
        }
        executeCodeResponse.setOutputList(outputList);
        JudgeInfo judgeInfo = new JudgeInfo();
        judgeInfo.setTime(maxTime);
        //要借助第三方库来获取内存，此处不做实现
//        judgeInfo.setMemory();
        executeCodeResponse.setJudgeInfo(judgeInfo);
        return executeCodeResponse;
    }

    /**
     * 删除文件
     * @param userCodeFile
     * @return
     */
    public boolean deleteFile(File userCodeFile) {
       File userCodeParentFile = userCodeFile.getParentFile();
        if (userCodeParentFile != null) {
            return FileUtil.del(userCodeParentFile.getAbsoluteFile());
        }
        return true;
    }

    /**
     * 获取错误响应
     *
     * @param e
     * @return
     */
    private ExecuteCodeResponse getErrorResponse(Throwable e) {
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        executeCodeResponse.setOutputList(new ArrayList<>());
        executeCodeResponse.setMessage(e.getMessage());
        executeCodeResponse.setStatus(2);
        executeCodeResponse.setJudgeInfo(new JudgeInfo());
        return executeCodeResponse;
    }
}
