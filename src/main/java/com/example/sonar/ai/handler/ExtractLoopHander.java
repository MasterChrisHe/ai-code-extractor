package com.example.sonar.ai.handler;

import com.example.sonar.ai.model.Rule;
import com.example.sonar.ai.model.Snippet;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.*;

import java.io.File;
import java.util.List;

/**
 * 抽取所有循环体中的代码。 适用的规则ruleId如下，具体规则请看ruleDescription.md：
 * RULE-028
 */
public class ExtractLoopHander extends Handler {

    @Override
    public void extract(MethodDeclaration node, Rule rule, File file, List<Snippet> snippets) {
        if (node.getBody().isEmpty()) {
            return;
        }
        BlockStmt body = node.getBody().get();
        body.findAll(Statement.class).forEach(stmt -> {
            if (isLoop(stmt)) {
                int loopLine = stmt.getRange()
                        .map(r -> r.begin.line)
                        .orElse(-1);
                String methodName = node.getDeclarationAsString(true, true, true);
                snippets.add(new Snippet(rule, file, loopLine, stmt.toString(), methodName, "METHOD_DECLARATION"));
            }
        });
    }

    private static boolean isLoop(Statement stmt) {
        return stmt instanceof ForStmt
                || stmt instanceof ForEachStmt
                || stmt instanceof WhileStmt
                || stmt instanceof DoStmt;
    }
}
