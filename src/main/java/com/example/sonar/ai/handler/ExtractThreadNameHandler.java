package com.example.sonar.ai.handler;

import com.example.sonar.ai.model.Rule;
import com.example.sonar.ai.model.Snippet;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;

import java.io.File;
import java.util.List;
import java.util.Optional;

/**
 * 抽取线程或线程池的名称。 适用的规则ruleId如下，具体规则请看ruleDescription.md：
 * RULE-029
 */
public class ExtractThreadNameHandler extends Handler {

    @Override
    public void extract(Node node, Rule rule, File file, List<Snippet> snippets) {
        //创建线程或线程池时
        if (node instanceof ObjectCreationExpr) {
            ObjectCreationExpr oce = (ObjectCreationExpr) node;
            String type = oce.getType().asString();
            if ("Thread".equals(type)) {
                // 构造器线程名
                oce.getArguments().forEach(a -> {
                    if (a.isStringLiteralExpr() || a.isBinaryExpr() || a.isMethodCallExpr()) {
                        snippets.add(new Snippet(rule, file, line(node), findOwnerNode(node).get().toString(), resolveThreadName(a), "THREAD_DECLARATION"));
                    }
                });
            }
        }
        if (node instanceof MethodCallExpr) {
            MethodCallExpr mce = (MethodCallExpr) node;
            //thread.setName时
            if ("setName".equals(mce.getNameAsString())
                    && mce.getArguments().size() == 1) {
                snippets.add(new Snippet(rule, file, line(node), findOwnerNode(node).get().toString(), mce.getArguments().get(0).asStringLiteralExpr().asString(), "THREAD_DECLARATION"));
            }
            //暂时不考虑Executors.newXXXPool，没有定义线程工程的方法
        }
    }


    /**
     * 获取线程名称（包括带变量名的）
     *
     * @param expr
     * @return
     */
    private String resolveThreadName(Expression expr) {
        if (expr.isStringLiteralExpr()) {
            return expr.asStringLiteralExpr().asString(); // 固定字符串
        } else if (expr.isBinaryExpr()) {
            BinaryExpr be = expr.asBinaryExpr();
            // 处理加法表达式拼接
            if (be.getOperator() == BinaryExpr.Operator.PLUS) {
                return resolveThreadName(be.getLeft()) + resolveThreadName(be.getRight());
            }
        } else if (expr.isMethodCallExpr()) {
            return expr.asMethodCallExpr().toString(); // 方法调用无法静态解析值，只能返回表达式文本
        } else if (expr.isNameExpr()) {
            return expr.asNameExpr().getNameAsString(); // 变量名
        } else if (expr.isEnclosedExpr()) {
            return resolveThreadName(expr.asEnclosedExpr().getInner());
        }
        // 其他情况
        return expr.asStringLiteralExpr().asString();
    }

    /**
     * 寻找创建代码的接收变量
     *
     * @param node
     * @return
     */
    private Optional<Node> findOwnerNode(Node node) {
        Node current = node;
        while (current.getParentNode().isPresent()) {
            current = findFullStatement(current.getParentNode().get());
            // ① 变量声明：Thread t = new Thread()
            if (current instanceof VariableDeclarator) {
                return Optional.of(current);
            }
            // ② 赋值：t = new Thread()
            if (current instanceof AssignExpr) {
                return Optional.of(current);
            }
            // ③ 表达式语句：new Thread().start()
            if (current instanceof Statement) {
                return Optional.of(current);
            }
        }
        return Optional.empty();
    }

    private Node findFullStatement(Node node) {
        Node cur = node;
        while (cur != null) {
            if (cur instanceof ExpressionStmt || cur instanceof ReturnStmt) {
                return cur; // 整句代码
            }
            cur = cur.getParentNode().orElse(null);
        }
        return node; // fallback
    }

    private int line(Node n) {
        return n.getBegin().map(p -> p.line).orElse(-1);
    }
}
