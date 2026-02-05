package com.example.sonar.ai.strategy;

import com.example.sonar.ai.handler.ExtractLogMessageHandler;
import com.example.sonar.ai.model.Rule;
import com.example.sonar.ai.model.Snippet;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.MethodCallExpr;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.List;

public class LogDeclarationStrategy implements ExtractionStrategy<MethodCallExpr>{

    public LogDeclarationStrategy() {
        ExtractionStrategy.handlerMap.put("RULE-031", new ExtractLogMessageHandler());
    }

    @Override
    public boolean supports(Rule rule, Node node) {
        List<String> list =
                List.of(StringUtils.split(rule.getScope(), ','));
        return list.contains("LOG_DECLARATION") &&
                (node instanceof MethodCallExpr);
    }

    @Override
    public void extract(MethodCallExpr node, Rule rule, File file, List<Snippet> snippets) {
        ExtractionStrategy.handlerMap.forEach((ruleId, handler) -> {
            if (ruleId.contains(rule.getId())) {
                handler.extract(node, rule, file, snippets);
            }
        });
    }
}
