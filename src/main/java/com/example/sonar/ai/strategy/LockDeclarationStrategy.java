package com.example.sonar.ai.strategy;

import com.example.sonar.ai.handler.ExtractLockHandler;
import com.example.sonar.ai.model.Rule;
import com.example.sonar.ai.model.Snippet;
import com.github.javaparser.ast.Node;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.List;

public class LockDeclarationStrategy implements ExtractionStrategy<Node> {

    public LockDeclarationStrategy() {
        ExtractionStrategy.handlerMap.put("RULE-030", new ExtractLockHandler());
    }


    @Override
    public boolean supports(Rule rule, Node node) {
        List<String> list =
                List.of(StringUtils.split(rule.getScope(), ','));
        return list.contains("LOCK_DECLARATION");
    }

    @Override
    public void extract(Node node, Rule rule, File file, List<Snippet> snippets) {
        ExtractionStrategy.handlerMap.forEach((ruleId, handler) -> {
            if (ruleId.contains(rule.getId())) {
                handler.extract(node, rule, file, snippets);
            }
        });
    }
}
