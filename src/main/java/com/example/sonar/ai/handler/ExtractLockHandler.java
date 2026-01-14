package com.example.sonar.ai.handler;

import com.example.sonar.ai.model.Rule;
import com.example.sonar.ai.model.Snippet;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.SynchronizedStmt;

import java.io.File;
import java.util.List;
import java.util.Optional;

public class ExtractLockHandler extends Handler {

    @Override
    public void extract(Node node, Rule rule, File file, List<Snippet> snippets) {
        //synchronized方法体
        if (node instanceof MethodDeclaration md) {
            if (md.isSynchronized()) {
                int line = md.getBegin().map(p -> p.line).orElse(-1);
                String methodName = md.getDeclarationAsString(true, true, true);
                snippets.add(new Snippet(rule, file, line, md.getBody().get().toString(), methodName, "LOCK_DECLARATION"));
            }
        } else if (node instanceof SynchronizedStmt stmt) {
            //synchronized代码块
            Expression lockExpr = stmt.getExpression();
            if (lockExpr.isClassExpr()) {
                int line = stmt.getBegin().map(p -> p.line).orElse(-1);
                Optional<CallableDeclaration> callable =
                        stmt.findAncestor(CallableDeclaration.class);
                String methodName = callable.map(c -> {
                    if (c instanceof MethodDeclaration) {
                        return c.getDeclarationAsString(true, true, true);
                    }
                    if (c instanceof ConstructorDeclaration) {
                        return c.getDeclarationAsString(true, true, true);
                    }
                    return "<unknown>";
                }).orElse("<unknown>");
                snippets.add(new Snippet(rule, file, line, stmt.toString(), methodName, "LOCK_DECLARATION"));
            }
        } else if (node instanceof ObjectCreationExpr oce) {
            //ReentrantLock锁等无锁结构
            String type = oce.getType().getNameAsString();
            if ("ReentrantLock".equals(type)) {
                int line = oce.getBegin().map(p -> p.line).orElse(-1);
                String methodName = oce.findAncestor(CallableDeclaration.class)
                        .map(c -> {
                            if (c instanceof MethodDeclaration) {
                                return c.getDeclarationAsString(true, true, true);
                            }
                            if (c instanceof ConstructorDeclaration) {
                                return c.getDeclarationAsString(true, true, true);
                            }
                            return "<unknown>";
                        })
                        .orElse("<unknown>");
                Optional<MethodDeclaration> mdOpt =
                        oce.findAncestor(MethodDeclaration.class);
                String methodBody = mdOpt
                        .flatMap(MethodDeclaration::getBody)
                        .map(BlockStmt::toString)
                        .orElse("");
                snippets.add(new Snippet(rule, file, line, methodBody, methodName, "LOCK_DECLARATION"));
            }
        }
    }
}
