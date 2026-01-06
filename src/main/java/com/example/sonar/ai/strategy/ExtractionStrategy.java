package com.example.sonar.ai.strategy;

import com.example.sonar.ai.handler.Handler;
import com.example.sonar.ai.model.Rule;
import com.example.sonar.ai.model.Snippet;
import com.github.javaparser.ast.Node;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 抽取策略接口
 * 定义如何从 AST 节点中提取代码片段
 */
public interface ExtractionStrategy<T extends Node> {

    Map<String, Handler> handlerMap = new HashMap<>();

    /**
     * 判断策略是否适用于给定的规则和节点
     * Accepts generic Node to avoid ClassCastException during iteration
     */
    boolean supports(Rule rule, Node node);

    /**
     * 执行抽取逻辑
     */
    void extract(T node, Rule rule, File file, List<Snippet> snippets);
}
