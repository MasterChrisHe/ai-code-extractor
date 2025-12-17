package com.example.sonar.ai.parser;

import com.example.sonar.ai.model.Rule;
import com.example.sonar.ai.model.Snippet;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * AST 访问器
 * 遍历 Java 语法树并根据规则提取节点
 */
public class JavaCodeVisitor extends VoidVisitorAdapter<Map<Rule, List<Snippet>>> {

    private final File currentFile;

    public JavaCodeVisitor(File file) {
        this.currentFile = file;
    }

    // 辅助方法：只获取代码的第一行，避免发送过多 Token 给 AI
    private String getFirstLineCode(String fullCode) {
        if (fullCode == null) return "";
        return fullCode.split("\n", 2)[0].trim();
    }

    /**
     * 通用提取逻辑
     */
    private void addSnippet(String scope, String name, com.github.javaparser.ast.Node node, Map<Rule, List<Snippet>> collector) {
        node.getBegin().ifPresent(position -> {
            int line = position.line;
            String fullCode = node.toString();
            String firstLineCode = getFirstLineCode(fullCode);

            // 检查当前节点是否匹配任何一条规则的作用域
            collector.forEach((rule, snippets) -> {
                if (scope.equalsIgnoreCase(rule.getScope())) {
                    snippets.add(new Snippet(rule, currentFile, line, firstLineCode, name));
                }
            });
        });
    }

    // --- 访问者方法重写 ---

    @Override
    public void visit(ClassOrInterfaceDeclaration n, Map<Rule, List<Snippet>> collector) {
        super.visit(n, collector);
        addSnippet("CLASS_DECLARATION", n.getNameAsString(), n, collector);
    }

    @Override
    public void visit(MethodDeclaration n, Map<Rule, List<Snippet>> collector) {
        super.visit(n, collector);
        addSnippet("METHOD_DECLARATION", n.getNameAsString(), n, collector);
    }
}
