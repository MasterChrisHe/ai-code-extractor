package com.example.sonar.ai.handler;

import com.example.sonar.ai.model.Rule;
import com.example.sonar.ai.model.Snippet;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.type.Type;

import java.io.File;
import java.util.List;
import java.util.Optional;

/**
 * 抽取遍历中是否有List操作。 适用的规则ruleId如下，具体规则请看ruleDescription.md：
 * RULE-027
 */
public class ExtractListLoopHandler extends Handler {

    @Override
    public void extract(MethodDeclaration node, Rule rule, File file, List<Snippet> snippets) {
        if (node.getBody().isEmpty()) {
            return;
        }
        BlockStmt body = node.getBody().get();
        // 1️⃣ 找出所有循环
        body.findAll(Statement.class).forEach(stmt -> {
            if (isLoop(stmt)) {
                checkLoop(stmt, rule, file, snippets);
            }
        });
    }

    private static boolean isLoop(Statement stmt) {
        return stmt instanceof ForStmt
                || stmt instanceof ForEachStmt
                || stmt instanceof WhileStmt
                || stmt instanceof DoStmt;
    }

    private static void checkLoop(Statement loopStmt, Rule rule, File file, List<Snippet> snippets) {
        loopStmt.findAll(MethodCallExpr.class).forEach(call -> {
            if (!"contains".equals(call.getNameAsString())) {
                return;
            }
            // 必须是 obj.contains(x)
            if (call.getScope().isEmpty()) return;
            Expression scope = call.getScope().get();
            if (isListType(scope, loopStmt)) {
                int line = call.getRange()
                        .map(r -> r.begin.line)
                        .orElse(-1);
                snippets.add(new Snippet(rule, file, line, loopStmt.toString(), call.toString(), "METHOD_DECLARATION"));
            }
        });
    }

    private static boolean isListType(Expression scope, Node context) {
        if (!(scope instanceof NameExpr)) return false;
        String varName = ((NameExpr) scope).getNameAsString();
        // 在当前方法内查找变量声明
        Optional<MethodDeclaration> method =
                context.findAncestor(MethodDeclaration.class);
        return method.map(methodDeclaration -> methodDeclaration.findAll(VariableDeclarator.class).stream()
                .anyMatch(var ->
                        varName.equals(var.getNameAsString())
                                && isListDeclaration(var.getType())
                )).orElse(false);
    }

    private static boolean isListDeclaration(Type type) {
        if (!type.isClassOrInterfaceType()) {
            return false;
        }
        String name = type.asClassOrInterfaceType()
                .getNameAsString();
        return "List".equals(name)
                || "ArrayList".equals(name)
                || "LinkedList".equals(name)
                || "CopyOnWriteArrayList".equals(name);
    }
}
