package com.example.sonar.ai.model;

import java.io.File;

/**
 * 代码片段实体类
 * 用于存储从源码中提取出的、待 AI 分析的代码块信息
 */
public class Snippet {
    private String ruleId;
    private String ruleScope;
    private String ruleDesc;
    private String ruleCriteria;
    private String ruleContext;
    private String file;
    private int line;
    private String code;
    private String name;

    public Snippet(Rule rule, File file, int line, String code, String name, String scope) {
        this.ruleId = rule.getId();
        this.ruleScope = scope;
        this.ruleDesc = rule.getDescription();
        this.ruleCriteria = rule.getCriteria();
        this.ruleContext = rule.getContext();
        this.file = file.getAbsolutePath();
        this.line = line;
        this.code = code;
        this.name = name;
    }

    // 仅为测试添加 Getter
    public String getName() {
        return name;
    }

    public int getLine() {
        return line;
    }

    public String getCode() {
        return code;
    }

    public String getScope() {
        return ruleScope;
    }
}
