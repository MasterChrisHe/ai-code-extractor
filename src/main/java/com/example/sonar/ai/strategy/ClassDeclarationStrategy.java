package com.example.sonar.ai.strategy;

import com.example.sonar.ai.handler.*;
import com.example.sonar.ai.model.Rule;
import com.example.sonar.ai.model.Snippet;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.List;

/**
 * 类/接口/枚举声明抽取策略
 */
public class ClassDeclarationStrategy implements ExtractionStrategy<TypeDeclaration<?>> {

    public ClassDeclarationStrategy() {
        ExtractionStrategy.handlerMap.put("RULE-001", new ExtractClassMethodVariableHandler());
        ExtractionStrategy.handlerMap.put("RULE-002", new ExtractClassMethodVariableHandler());
        ExtractionStrategy.handlerMap.put("RULE-003", new ExtractClassMethodVariableHandler());
        ExtractionStrategy.handlerMap.put("RULE-009", new ExtractControllerRequestMappingHandler());
        ExtractionStrategy.handlerMap.put("RULE-010", new ExtractClassContructionOverrideHandler());
        ExtractionStrategy.handlerMap.put("RULE-014", new ExtractInterfaceFieldHandler());
        ExtractionStrategy.handlerMap.put("RULE-015", new ExtractClassMethodVariableHandler());
        ExtractionStrategy.handlerMap.put("RULE-022", new ExtractClassMethodFieldHandler());
    }

    @Override
    public boolean supports(Rule rule, Node node) {
        List<String> list =
                List.of(StringUtils.split(rule.getScope(), ','));
        return list.contains("CLASS_DECLARATION") &&
                (node instanceof ClassOrInterfaceDeclaration || node instanceof EnumDeclaration);
    }

    @Override
    public void extract(TypeDeclaration<?> node, Rule rule, File file, List<Snippet> snippets) {
        //遍历初始化中已添加的处理器
        ExtractionStrategy.handlerMap.forEach((ruleId, handler) -> {
            if (ruleId.contains(rule.getId())) {
                handler.extract(node, rule, file, snippets);
            }
        });
    }
}
