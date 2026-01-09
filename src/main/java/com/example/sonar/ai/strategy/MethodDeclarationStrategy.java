package com.example.sonar.ai.strategy;

import com.example.sonar.ai.handler.ExtractClassMethodVariableHandler;
import com.example.sonar.ai.handler.ExtractListLoopHandler;
import com.example.sonar.ai.handler.ExtractMethodAndJavaDocHandler;
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

    public MethodDeclarationStrategy() {
        ExtractionStrategy.handlerMap.put("RULE-001", new ExtractClassMethodVariableHandler());
        ExtractionStrategy.handlerMap.put("RULE-002", new ExtractClassMethodVariableHandler());
        ExtractionStrategy.handlerMap.put("RULE-003", new ExtractClassMethodVariableHandler());
        ExtractionStrategy.handlerMap.put("RULE-005", new ExtractMethodAndJavaDocHandler());
        ExtractionStrategy.handlerMap.put("RULE-006", new ExtractMethodAndJavaDocHandler());
        ExtractionStrategy.handlerMap.put("RULE-007", new ExtractMethodAndJavaDocHandler());
        ExtractionStrategy.handlerMap.put("RULE-008", new ExtractMethodAndJavaDocHandler());
        ExtractionStrategy.handlerMap.put("RULE-012", new ExtractClassMethodVariableHandler());
        ExtractionStrategy.handlerMap.put("RULE-013", new ExtractClassMethodVariableHandler());
        ExtractionStrategy.handlerMap.put("RULE-015", new ExtractClassMethodVariableHandler());
        ExtractionStrategy.handlerMap.put("RULE-017", new ExtractClassMethodVariableHandler());
        ExtractionStrategy.handlerMap.put("RULE-020", new ExtractClassMethodVariableHandler());
        ExtractionStrategy.handlerMap.put("RULE-021", new ExtractClassMethodVariableHandler());
        ExtractionStrategy.handlerMap.put("RULE-027", new ExtractListLoopHandler());
    }

    @Override
    public boolean supports(Rule rule, Node node) {
        List<String> list =
                List.of(StringUtils.split(rule.getScope(), ','));
        return list.contains("METHOD_DECLARATION") && node instanceof MethodDeclaration;
    }

    @Override
    public void extract(MethodDeclaration node, Rule rule, File file, List<Snippet> snippets) {
        ExtractionStrategy.handlerMap.forEach((ruleId, handler) -> {
            if (ruleId.contains(rule.getId())) {
                handler.extract(node, rule, file, snippets);
            }
        });
    }
}
