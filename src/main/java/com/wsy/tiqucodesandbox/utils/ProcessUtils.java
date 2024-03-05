package com.wsy.tiqucodesandbox.utils;

import cn.hutool.core.util.StrUtil;
import com.wsy.tiqucodesandbox.model.ExecuteMessage;
import org.springframework.util.StopWatch;

import java.io.*;

/**
 * 执行工具类
 */
public class ProcessUtils {
    /**
     * 执行进程并获取信息
     * @param runProcess
     * @return
     */
    public static ExecuteMessage runProcessAndGetMessage(Process runProcess, String opName) {
        ExecuteMessage executeMessage = new ExecuteMessage();
        try {
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            int exitValue = runProcess.waitFor();
            executeMessage.setExitValue(exitValue);
            if (exitValue == 0) {
                System.out.println(opName + "成功");
                //分批获取进程的输出
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(runProcess.getInputStream()));
                StringBuilder compileOutputStringBuilder = new StringBuilder();
                String compileOutputLine;
                //逐行读取
                while ((compileOutputLine = bufferedReader.readLine()) != null) {
                    compileOutputStringBuilder.append(compileOutputLine).append("\n");
                }
                executeMessage.setMessage(compileOutputStringBuilder.toString());
            } else {
                //异常退出
                System.out.println(opName + "失败,错误码为:" + exitValue);
                //分批获取进程的正常输出
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(runProcess.getInputStream()));
                StringBuilder compileOutputStringBuilder = new StringBuilder();
                String compileOutputLine;
                //逐行读取
                while ((compileOutputLine = bufferedReader.readLine()) != null) {
                    compileOutputStringBuilder.append(compileOutputLine).append("\n");
                }
                executeMessage.setMessage(compileOutputStringBuilder.toString());
                //分批获取进程的输出
                BufferedReader errorBufferedReader = new BufferedReader(new InputStreamReader(runProcess.getErrorStream()));
                StringBuilder errorCompileOutputStringBuilder = new StringBuilder();
                String errorCompileOutputLine;
                //逐行读取
                while ((errorCompileOutputLine = errorBufferedReader.readLine()) != null) {
                    errorCompileOutputStringBuilder.append(errorCompileOutputLine).append("\n");
                }
                executeMessage.setErrorMessage(errorCompileOutputStringBuilder.toString());
            }
            stopWatch.stop();
            executeMessage.setTime(stopWatch.getLastTaskTimeMillis());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return executeMessage;
    }

    /**
     * 执行交互式进程并获取信息
     * @param runProcess
     * @return
     */
    @Deprecated
    public static ExecuteMessage runInteractProcessAndGetMessage(Process runProcess, String opName, String args) {
        ExecuteMessage executeMessage = new ExecuteMessage();
        try {
            InputStream inputStream = runProcess.getInputStream();
            OutputStream outputStream = runProcess.getOutputStream();

            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream);
            String[] s = args.split(" ");
            String join = StrUtil.join("\n", s) + "\n";

            outputStreamWriter.write(join);
            //相当于按了回车
            outputStreamWriter.flush();

            //分批获取进程的输出
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder compileOutputStringBuilder = new StringBuilder();
            String compileOutputLine;
            //逐行读取
            while ((compileOutputLine = bufferedReader.readLine()) != null) {
                compileOutputStringBuilder.append(compileOutputLine);
            }
            executeMessage.setMessage(compileOutputStringBuilder.toString());
            //释放资源，否则会卡死
            outputStreamWriter.close();
            outputStream.close();
            inputStream.close();
            runProcess.destroy();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return executeMessage;
    }
}
