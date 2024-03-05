package com.wsy.tiqucodesandbox.model;

import lombok.Data;

/**
 * 题目用例
 */
@Data
public class JudgeInfo {
    private String message;

    private Long time;

    private Long memory;
}
