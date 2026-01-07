package com.example.sonar.ai.strategy;

import com.example.sonar.ai.handler.ExtractClassMethodVariableHandler;
import com.example.sonar.ai.handler.Handler;
import com.example.sonar.ai.model.Rule;
import com.example.sonar.ai.model.Snippet;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.Expression;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.List;
import java.util.Set;

/**
 * 方法内的变量检测
 */
public class VariableDeclarationStrategy implements ExtractionStrategy<VariableDeclarator> {

    public VariableDeclarationStrategy() {
        ExtractionStrategy.handlerMap.put("RULE-001", new ExtractClassMethodVariableHandler());
        ExtractionStrategy.handlerMap.put("RULE-002", new ExtractClassMethodVariableHandler());
        ExtractionStrategy.handlerMap.put("RULE-003", new ExtractClassMethodVariableHandler());
        ExtractionStrategy.handlerMap.put("RULE-011", new ExtractClassMethodVariableHandler());
    }


    @Override
    public boolean supports(Rule rule, Node node) {
        List<String> list =
                List.of(StringUtils.split(rule.getScope(), ','));
        return list.contains("VARIABLE_DECLARATION") &&
                (node instanceof VariableDeclarator);
    }

    @Override
    public void extract(VariableDeclarator node, Rule rule, File file, List<Snippet> snippets) {
        ExtractionStrategy.handlerMap.forEach((ruleId, handler) -> {
            if (ruleId.contains(rule.getId())) {
                handler.extract(node, rule, file, snippets);
            }
        });
    }
}
