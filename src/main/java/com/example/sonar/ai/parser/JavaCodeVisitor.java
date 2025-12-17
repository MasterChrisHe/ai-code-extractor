package com.example.sonar.ai.parser;

import com.example.sonar.ai.model.Rule;
import com.example.sonar.ai.model.Snippet;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration; // 引入 EnumDeclaration
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * AST 访问器
 * 遍历 Java 语法树并根据规则提取节点
 * 修复了 ClassOrInterfaceDeclaration 中错误的 isEnum() 调用。
 */
public class JavaCodeVisitor extends VoidVisitorAdapter<Map<Rule, List<Snippet>>> {

    private final File currentFile;

    public JavaCodeVisitor(File file) {
        this.currentFile = file;
    }

    /**
     * 辅助方法：添加 Snippet 到收集器
     */
    private void addSnippet(String scope, String name, int line, String code, Map<Rule, List<Snippet>> collector) {
        collector.forEach((rule, snippets) -> {
            if (scope.equalsIgnoreCase(rule.getScope())) {
                snippets.add(new Snippet(rule, currentFile, line, code, name));
            }
        });
    }

    // --- 访问者方法重写 ---
    
    /**
     * 访问类和接口声明
     */
    @Override
    public void visit(ClassOrInterfaceDeclaration n, Map<Rule, List<Snippet>> collector) {
        super.visit(n, collector);
        
        // 1. 获取行号：优先使用类名所在的行，而非整个 Class 块的起始行（避免 @Annotation）
        int line = n.getName().getBegin().map(p -> p.line).orElse(
                   n.getBegin().map(p -> p.line).orElse(1));

        // 2. 构造代码片段：手动拼接签名，避免包含过长的方法体或无关注解
        StringBuilder sb = new StringBuilder();
        if (n.isPublic()) sb.append("public ");
        else if (n.isPrivate()) sb.append("private ");
        else if (n.isProtected()) sb.append("protected ");
        
        if (n.isStatic()) sb.append("static ");
        if (n.isInterface()) sb.append("interface ");
        // ClassOrInterfaceDeclaration 只能是 class 或 interface
        else sb.append("class "); 
        
        sb.append(n.getNameAsString());

        addSnippet("CLASS_DECLARATION", n.getNameAsString(), line, sb.toString(), collector);
    }
    
    /**
     * 访问枚举声明
     */
    @Override
    public void visit(EnumDeclaration n, Map<Rule, List<Snippet>> collector) {
        super.visit(n, collector);
        
        int line = n.getName().getBegin().map(p -> p.line).orElse(
                   n.getBegin().map(p -> p.line).orElse(1));

        StringBuilder sb = new StringBuilder();
        if (n.isPublic()) sb.append("public ");
        else if (n.isPrivate()) sb.append("private ");
        else if (n.isProtected()) sb.append("protected ");
        
        if (n.isStatic()) sb.append("static ");
        sb.append("enum "); 
        sb.append(n.getNameAsString());
        
        addSnippet("CLASS_DECLARATION", n.getNameAsString(), line, sb.toString(), collector);
    }

    /**
     * 访问方法声明
     */
    @Override
    public void visit(MethodDeclaration n, Map<Rule, List<Snippet>> collector) {
        super.visit(n, collector);
        
        // 1. 获取行号：精确获取方法名所在的行号（修复了注解导致行号不准确的问题）
        // 例如：@Override 在第 10 行，public void define() 在第 11 行，我们取第 11 行
        int line = n.getName().getBegin().map(p -> p.line).orElse(
                   n.getBegin().map(p -> p.line).orElse(1));

        // 2. 获取代码片段：使用 getDeclarationAsString() 
        // 该方法默认不包含注解 (excluding annotations)，只包含修饰符、返回值、名称和参数
        // 输出示例: "public void define(Context context)"
        String cleanCode = n.getDeclarationAsString(true, true, true);

        addSnippet("METHOD_DECLARATION", n.getNameAsString(), line, cleanCode, collector);
    }
}
