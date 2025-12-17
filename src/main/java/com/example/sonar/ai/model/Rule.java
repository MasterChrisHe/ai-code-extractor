package com.example.sonar.ai.model;

/**
 * 规则实体类
 * 对应 CSV 文件中的一行规则配置
 */
public class Rule {
    private String id;
    private String scope; // e.g., CLASS_DECLARATION, METHOD_DECLARATION
    private String description;
    private String criteria;
    private String context;

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCriteria() { return criteria; }
    public void setCriteria(String criteria) { this.criteria = criteria; }

    public String getContext() { return context; }
    public void setContext(String context) { this.context = context; }
}
