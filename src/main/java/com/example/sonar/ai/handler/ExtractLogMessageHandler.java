package com.example.sonar.ai.handler;

import com.example.sonar.ai.model.Rule;
import com.example.sonar.ai.model.Snippet;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;

import java.io.File;
import java.util.List;

/**
 * 抽取log日志调用方法的message内容。 适用的规则ruleId如下，具体规则请看ruleDescription.md：
 * RULE-031
 */
public class ExtractLogMessageHandler extends Handler {

    @Override
    public void extract(MethodCallExpr node, Rule rule, File file, List<Snippet> snippets) {
        String methodName = node.getNameAsString();
        if ("warn".equals(methodName) || "error".equals(methodName)) {
            int line = node.getRange()
                    .map(r -> r.begin.line)
                    .orElse(-1);
            snippets.add(new Snippet(rule, file, line, node.toString(), node.toString(), "LOG_DECLARATION"));
        }
    }
}
