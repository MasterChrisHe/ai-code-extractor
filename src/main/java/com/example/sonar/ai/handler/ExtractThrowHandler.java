package com.example.sonar.ai.handler;

import com.example.sonar.ai.model.Rule;
import com.example.sonar.ai.model.Snippet;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ThrowStmt;

import java.io.File;
import java.util.List;
import java.util.Optional;

/**
 * 抽取主动抛出异常的message。 适用的规则ruleId如下，具体规则请看ruleDescription.md：
 * RULE-024
 */
public class ExtractThrowHandler extends Handler {

    @Override
    public void extract(ThrowStmt node, Rule rule, File file, List<Snippet> snippets) {
        int line = node.getBegin().map(pos -> pos.line).orElse(-1);
        Expression expr = node.getExpression();
        // 判断是否是 new 异常
        if (expr instanceof ObjectCreationExpr objectCreation) {
            //eg. throw new Exception("test E");
            // 获取异常构造参数
            objectCreation.getArguments().forEach(arg -> {
                String exceptionMessage = "";
                if (arg instanceof StringLiteralExpr str) {
                    exceptionMessage = str.getValue();
                } else {
                    exceptionMessage = arg.toString();
                }
                snippets.add(new Snippet(rule, file, line, node.toString(), exceptionMessage, "THROW_DECLARATION"));
            });
        } else if (expr instanceof NameExpr nameExpr) {
            //eg. Exception e = new Exception("test");
            //eg. return e;
            String varName = nameExpr.getNameAsString();
            Optional<VariableDeclarator> varDecl = node.findAncestor(BlockStmt.class)
                    .flatMap(block -> block.findFirst(VariableDeclarator.class,
                            vd -> vd.getNameAsString().equals(varName)));
            if (varDecl.isPresent()) {
                VariableDeclarator vd = varDecl.get();
                vd.getInitializer().ifPresent(init -> {
                    if (init instanceof ObjectCreationExpr oce2) {
                        if (!oce2.getArguments().isEmpty()) {
                            String type = vd.getType().toString();
                            String name = vd.getNameAsString();
                            String initializer = vd.getInitializer()
                                    .map(Expression::toString)
                                    .orElse(null);
                            //直接获取声明异常代码
                            String result = initializer == null
                                    ? type + " " + name
                                    : type + " " + name + " = " + initializer;
                            String message = oce2.getArguments().get(0).asStringLiteralExpr().asString();
                            snippets.add(new Snippet(rule, file, line, result, message, "THROW_DECLARATION"));
                        }
                    }
                });
            }
        }
    }
}
