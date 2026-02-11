package com.example.sonar.ai.strategy;

import com.example.sonar.ai.handler.ExtractThrowHandler;
import com.example.sonar.ai.model.Rule;
import com.example.sonar.ai.model.Snippet;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.stmt.ThrowStmt;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.List;

public class ThrowDeclarationStrategy implements ExtractionStrategy<ThrowStmt>{

    public ThrowDeclarationStrategy() {
        ExtractionStrategy.handlerMap.put("RULE-024", new ExtractThrowHandler());
    }

    @Override
    public boolean supports(Rule rule, Node node) {
        List<String> list =
                List.of(StringUtils.split(rule.getScope(), ','));
        return list.contains("THROW_DECLARATION");
    }

    @Override
    public void extract(ThrowStmt node, Rule rule, File file, List<Snippet> snippets) {
        ExtractionStrategy.handlerMap.forEach((ruleId, handler) -> {
            if (ruleId.contains(rule.getId())) {
                handler.extract(node, rule, file, snippets);
            }
        });
    }
}
