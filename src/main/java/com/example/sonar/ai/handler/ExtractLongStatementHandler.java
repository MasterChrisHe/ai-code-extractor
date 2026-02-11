package com.example.sonar.ai.handler;

import com.example.sonar.ai.model.Rule;
import com.example.sonar.ai.model.Snippet;
import com.github.javaparser.Range;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;

import java.io.File;
import java.util.List;
import java.util.Optional;

/**
 * 抽取较长(>80字符)的语句和表达式
 * 抽取长语句换行后新行之首不是操作符的语句  适用的规则ruleId如下，具体规则请看ruleDescription.md：
 * RULE-023
 */
public class ExtractLongStatementHandler extends Handler {

    @Override
    public void extract(Node node, Rule rule, File file, List<Snippet> snippets) {
        if (node instanceof VariableDeclarator || node instanceof MethodCallExpr) {
            // eg. 变量声明int z=0;
            // boolean a1=a==0&&b==0&&c==0&&d==0&&e==0&&f==0&&g==0&&h==0&&i==0&&j==0&&k==0&&l==0&&m==0&&n==0&o==0&&p==0&q==0&&r==0;
            checkVariableDeclaration(node, rule, file, snippets);
            //检测每一行的操作符是否有操作符
            checkMethodDeclaration(node, rule, file, snippets);
        }
    }

    private void checkVariableDeclaration(Node node, Rule rule, File file, List<Snippet> snippets) {
        if (node.getRange().isEmpty()) {
            return;
        }
        int length = node.toString().length();
        int beginLine = node.getRange().get().begin.line;
        int endLine = node.getRange().get().end.line;

        if (length > 80 && beginLine == endLine) {
            Optional<MethodDeclaration> methodOpt =
                    node.findAncestor(MethodDeclaration.class);
            if (methodOpt.isPresent()) {
                String methodName = methodOpt.get().getDeclarationAsString(true, true, true);
                snippets.add(new Snippet(rule, file, beginLine, node.toString(),
                        methodName, "LONGSTATEMENT_DECLARATION"));
            }
        }
    }

    private void checkMethodDeclaration(Node node, Rule rule, File file, List<Snippet> snippets) {
        if (node instanceof MethodCallExpr mce) {
            for (Expression arg : mce.getArguments()) {
                // 只关心二元表达式
                if (!(arg instanceof BinaryExpr)) {
                    continue;
                }
                BinaryExpr expr = (BinaryExpr) arg;
                if (!expr.getRange().isPresent()) {
                    continue;
                }
                Range exprRange = expr.getRange().get();
                // 单行表达式不管
                if (exprRange.begin.line == exprRange.end.line) {
                    continue;
                }
                Expression left = expr.getLeft();
                if (!left.getRange().isPresent()) {
                    continue;
                }
                int leftEndLine = left.getRange().get().end.line;
                int exprEndLine = exprRange.end.line;
                if (leftEndLine < exprEndLine) {
                    Optional<MethodDeclaration> methodOpt =
                            node.findAncestor(MethodDeclaration.class);
                    if (methodOpt.isPresent()) {
                        String methodName = methodOpt.get().getDeclarationAsString(true, true, true);
                        snippets.add(new Snippet(rule, file, leftEndLine, node.toString(),
                                methodName, "LONGSTATEMENT_DECLARATION"));
                    }
                }
            }
        }
    }
}
