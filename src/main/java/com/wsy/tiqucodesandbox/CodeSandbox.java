package com.wsy.tiqucodesandbox;


import com.wsy.tiqucodesandbox.model.ExecuteCodeRequest;
import com.wsy.tiqucodesandbox.model.ExecuteCodeResponse;

/**
 * 代码沙箱接口定义
 */
public interface CodeSandbox {
    /**
     * 执行代码
     * @param executeCodeRequest
     * @return
     */
    ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) throws InterruptedException;
}
