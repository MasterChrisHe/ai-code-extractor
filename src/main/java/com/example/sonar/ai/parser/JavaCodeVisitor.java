package com.example.sonar.ai.parser;

import com.example.sonar.ai.model.Rule;
import com.example.sonar.ai.model.Snippet;
import com.example.sonar.ai.strategy.ExtractionStrategy;
import com.example.sonar.ai.strategy.ThreadDeclarationStrategy;
import com.example.sonar.ai.strategy.VariableDeclarationStrategy;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * AST 访问器
 * 遍历 Java 语法树并根据规则提取节点
 * 修复了 ClassOrInterfaceDeclaration 中错误的 isEnum() 调用。
 */
public class JavaCodeVisitor extends VoidVisitorAdapter<Map<Rule, List<Snippet>>> {

    private final File currentFile;
    private final List<ExtractionStrategy<?>> strategies;

    public JavaCodeVisitor(File file) {
        this.currentFile = file;
        this.strategies = new ArrayList<>();
        // 注册策略
        // 每个策略中根据不同的抽取规则又做handler管理，以ruleid为唯一标识
        this.strategies.add(new com.example.sonar.ai.strategy.ClassDeclarationStrategy());
        this.strategies.add(new com.example.sonar.ai.strategy.MethodDeclarationStrategy());
        this.strategies.add(new VariableDeclarationStrategy());
        this.strategies.add(new ThreadDeclarationStrategy());
    }

    @Override
    public void visit(ClassOrInterfaceDeclaration n, Map<Rule, List<Snippet>> collector) {
        super.visit(n, collector);
        applyStrategies(n, collector);
    }

    @Override
    public void visit(EnumDeclaration n, Map<Rule, List<Snippet>> collector) {
        super.visit(n, collector);
        applyStrategies(n, collector);
    }

    @Override
    public void visit(MethodDeclaration n, Map<Rule, List<Snippet>> collector) {
        super.visit(n, collector);
        applyStrategies(n, collector);
    }

    @Override
    public void visit(VariableDeclarator n, Map<Rule, List<Snippet>> collector) {
        super.visit(n, collector);
        applyStrategies(n, collector);
    }

    @Override
    public void visit(ObjectCreationExpr n, Map<Rule, List<Snippet>> collector) {
        super.visit(n, collector);
        applyStrategies(n, collector);
    }

    @Override
    public void visit(MethodCallExpr n, Map<Rule, List<Snippet>> collector) {
        super.visit(n, collector);
        applyStrategies(n, collector);
    }


    @SuppressWarnings("unchecked")
    private void applyStrategies(com.github.javaparser.ast.Node node, Map<Rule, List<Snippet>> collector) {
        collector.forEach((rule, snippets) -> {
            for (ExtractionStrategy strategy : strategies) {
                if (strategy.supports(rule, node)) {
                    // Start of safe logic to execute extract with captured wildcard
                    executeStrategy(strategy, node, rule, currentFile, snippets);
                }
            }
        });
    }

    @SuppressWarnings("unchecked")
    private <T extends com.github.javaparser.ast.Node> void executeStrategy(ExtractionStrategy<T> strategy,
                                                                            com.github.javaparser.ast.Node node, Rule rule, File file, List<Snippet> snippets) {
        try {
            // Unchecked cast is necessary here as we essentially checked 'supports' before
            strategy.extract((T) node, rule, file, snippets);
        } catch (ClassCastException e) {
            // Should be prevented by supports(), but good safety net
        }
    }

}
