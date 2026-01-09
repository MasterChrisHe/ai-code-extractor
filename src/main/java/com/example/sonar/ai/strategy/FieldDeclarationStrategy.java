package com.example.sonar.ai.strategy;

import com.example.sonar.ai.handler.ExtractFieldHandler;
import com.example.sonar.ai.model.Rule;
import com.example.sonar.ai.model.Snippet;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.FieldDeclaration;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.List;

public class FieldDeclarationStrategy implements ExtractionStrategy<FieldDeclaration> {

    public FieldDeclarationStrategy() {
        ExtractionStrategy.handlerMap.put("RULE-018", new ExtractFieldHandler());
        ExtractionStrategy.handlerMap.put("RULE-019", new ExtractFieldHandler());
    }

    @Override
    public boolean supports(Rule rule, Node node) {
        List<String> list =
                List.of(StringUtils.split(rule.getScope(), ','));
        return list.contains("FIELD_DECLARATION") &&
                (node instanceof FieldDeclaration);
    }

    @Override
    public void extract(FieldDeclaration node, Rule rule, File file, List<Snippet> snippets) {
        //遍历初始化中已添加的处理器
        ExtractionStrategy.handlerMap.forEach((ruleId, handler) -> {
            if (ruleId.contains(rule.getId())) {
                handler.extract(node, rule, file, snippets);
            }
        });
    }
}
