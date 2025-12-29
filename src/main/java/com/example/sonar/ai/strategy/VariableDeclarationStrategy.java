package com.example.sonar.ai.strategy;

import com.example.sonar.ai.model.Rule;
import com.example.sonar.ai.model.Snippet;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.Expression;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.List;

/**
 * 方法内的变量检测
 */
public class VariableDeclarationStrategy implements ExtractionStrategy<VariableDeclarator>{
    @Override
    public boolean supports(Rule rule, Node node) {
        List<String> list =
                List.of(StringUtils.split(rule.getScope(), ','));
        return list.contains("VARIABLE_DECLARATION") &&
                (node instanceof VariableDeclarator);
    }

    @Override
    public void extract(VariableDeclarator node, Rule rule, File file, List<Snippet> snippets) {
        int line = node.getName().getBegin().map(p -> p.line).orElse(
                node.getBegin().map(p -> p.line).orElse(1));

        String type = node.getType().toString();
        String name = node.getNameAsString();
        String initializer = node.getInitializer()
                .map(Expression::toString)
                .orElse(null);
        String result = initializer == null
                ? type + " " + name
                : type + " " + name + " = " + initializer;

        snippets.add(new Snippet(rule, file, line, result, node.getNameAsString(),"VARIABLE_DECLARATION"));
    }
}
