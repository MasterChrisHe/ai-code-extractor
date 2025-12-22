package com.example.sonar.ai.parser;

import com.example.sonar.ai.model.Rule;
import com.example.sonar.ai.model.Snippet;
import com.example.sonar.ai.strategy.BaseStrategy;
import com.example.sonar.ai.strategy.MethodJavaDocStrategy;
import com.github.javaparser.Range;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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


    // --- 访问者方法重写 ---

    /**
     * 访问类和接口声明
     */
    @Override
    public void visit(ClassOrInterfaceDeclaration n, Map<Rule, List<Snippet>> collector) {
        super.visit(n, collector);
//
//        // 1. 获取行号：优先使用类名所在的行，而非整个 Class 块的起始行（避免 @Annotation）
//        int line = n.getName().getBegin().map(p -> p.line).orElse(
//                n.getBegin().map(p -> p.line).orElse(1));
//
//        // 2. 构造代码片段：手动拼接签名，避免包含过长的方法体或无关注解
//        StringBuilder sb = new StringBuilder();
//        if (n.isPublic()) sb.append("public ");
//        else if (n.isPrivate()) sb.append("private ");
//        else if (n.isProtected()) sb.append("protected ");
//
//        if (n.isStatic()) sb.append("static ");
//        if (n.isInterface()) sb.append("interface ");
//            // ClassOrInterfaceDeclaration 只能是 class 或 interface
//        else sb.append("class ");
//
//        sb.append(n.getNameAsString());
//
//        addSnippet("CLASS_DECLARATION", n.getNameAsString(), line, sb.toString(), collector);
    }


    /**
     * 访问枚举声明
     */
    @Override
    public void visit(EnumDeclaration n, Map<Rule, List<Snippet>> collector) {
        super.visit(n, collector);

//        int line = n.getName().getBegin().map(p -> p.line).orElse(
//                n.getBegin().map(p -> p.line).orElse(1));
//
//        StringBuilder sb = new StringBuilder();
//        if (n.isPublic()) sb.append("public ");
//        else if (n.isPrivate()) sb.append("private ");
//        else if (n.isProtected()) sb.append("protected ");
//
//        if (n.isStatic()) sb.append("static ");
//        sb.append("enum ");
//        sb.append(n.getNameAsString());
//
//        addSnippet("CLASS_DECLARATION", n.getNameAsString(), line, sb.toString(), collector);
    }

    /**
     * 访问方法声明
     */
    @Override
    public void visit(MethodDeclaration n, Map<Rule, List<Snippet>> collector) {
        super.visit(n, collector);

        BaseStrategy strategy = null;
        for (Map.Entry<Rule, List<Snippet>> entry : collector.entrySet()) {
            String ruleId = entry.getKey().getId();
            //需要根据ruleId，封装不同的策略
            switch (ruleId) {
                case "METHOD_javadocComment":
                    strategy = new MethodJavaDocStrategy(currentFile);
                    break;
            }
            strategy.doMethodDeclaration(n, collector);
        }
    }

}

