package com.example.sonar.ai.strategy;

import com.example.sonar.ai.handler.ExtractThreadNameHandler;
import com.example.sonar.ai.model.Rule;
import com.example.sonar.ai.model.Snippet;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.TypeDeclaration;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.List;

/**
 * 类中的线程线程池检测
 */
public class ThreadDeclarationStrategy implements ExtractionStrategy<Node>{

    public ThreadDeclarationStrategy() {
        ExtractionStrategy.handlerMap.put("RULE-029", new ExtractThreadNameHandler());
    }

    @Override
    public boolean supports(Rule rule, Node node) {
        List<String> list =
                List.of(StringUtils.split(rule.getScope(), ','));
        return list.contains("THREAD_DECLARATION");
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
