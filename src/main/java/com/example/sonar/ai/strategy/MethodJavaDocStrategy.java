package com.example.sonar.ai.strategy;

import com.example.sonar.ai.model.Rule;
import com.example.sonar.ai.model.Snippet;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;

import java.io.File;
import java.util.List;

/**
 * 方法 Javadoc 抽取策略
 */
public class MethodJavaDocStrategy implements ExtractionStrategy<MethodDeclaration> {

    @Override
    public boolean supports(Rule rule, Node node) {
        return "METHOD_JAVADOC".equalsIgnoreCase(rule.getScope()) && node instanceof MethodDeclaration;
    }

    @Override
    public void extract(MethodDeclaration node, Rule rule, File file, List<Snippet> snippets) {
        node.getJavadoc().ifPresent(javadoc -> {
            int line = node.getComment()
                    .flatMap(Node::getBegin)
                    .map(p -> p.line)
                    .orElse(node.getBegin().map(p -> p.line).orElse(1));

            String javadocContent = javadoc.getDescription().toText();
            // 如果 Javadoc 内容不为空，则提取
            if (javadocContent != null && !javadocContent.trim().isEmpty()) {
                snippets.add(new Snippet(rule, file, line, javadocContent, node.getNameAsString()));
            }
        });
    }
}
