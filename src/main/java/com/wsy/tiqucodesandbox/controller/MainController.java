package com.wsy.tiqucodesandbox.controller;

import com.wsy.tiqucodesandbox.JavaNativeCodeSandbox;
import com.wsy.tiqucodesandbox.model.ExecuteCodeRequest;
import com.wsy.tiqucodesandbox.model.ExecuteCodeResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController("/")
public class MainController {

    @Resource
    private JavaNativeCodeSandbox javaNativeCodeSandbox;

    @PostMapping("/executeCode")
    ExecuteCodeResponse executeCode(@RequestBody  ExecuteCodeRequest executeCodeRequest) {
        if (executeCodeRequest == null) {
            throw new RuntimeException("请求参数为空");
        }
        return javaNativeCodeSandbox.executeCode(executeCodeRequest);
    }

}
