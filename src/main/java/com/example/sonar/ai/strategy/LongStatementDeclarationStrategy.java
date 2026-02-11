package com.example.sonar.ai.strategy;

import com.example.sonar.ai.handler.ExtractLongStatementHandler;
import com.example.sonar.ai.model.Rule;
import com.example.sonar.ai.model.Snippet;
import com.github.javaparser.ast.Node;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.List;
import java.util.stream.Stream;

/**
 * 长语句抽取策略
 */
public class LongStatementDeclarationStrategy implements ExtractionStrategy<Node> {

    public LongStatementDeclarationStrategy() {
        ExtractionStrategy.handlerMap.put("RULE-023", new ExtractLongStatementHandler());
    }

    @Override
    public boolean supports(Rule rule, Node node) {
        List<String> list = Stream.of(StringUtils.split(rule.getScope(), ','))
                .map(String::trim)
                .toList();
        return list.contains("LONGSTATEMENT_DECLARATION");
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
