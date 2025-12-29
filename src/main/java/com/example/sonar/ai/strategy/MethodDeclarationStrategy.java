package com.example.sonar.ai.strategy;

import com.example.sonar.ai.model.Rule;
import com.example.sonar.ai.model.Snippet;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.List;

/**
 * 方法声明抽取策略
 */
public class MethodDeclarationStrategy implements ExtractionStrategy<MethodDeclaration> {

    @Override
    public boolean supports(Rule rule, Node node) {
        List<String> list =
                List.of(StringUtils.split(rule.getScope(), ','));
        return list.contains("METHOD_DECLARATION") && node instanceof MethodDeclaration;
    }

    @Override
    public void extract(MethodDeclaration node, Rule rule, File file, List<Snippet> snippets) {
        int line = node.getName().getBegin().map(p -> p.line).orElse(
                node.getBegin().map(p -> p.line).orElse(1));

        String cleanCode = node.getDeclarationAsString(true, true, true);

        snippets.add(new Snippet(rule, file, line, cleanCode, node.getNameAsString(),"METHOD_DECLARATION"));
    }
}
