package com.example.sonar.ai.handler;

import com.example.sonar.ai.model.Rule;
import com.example.sonar.ai.model.Snippet;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/**
 * 抽取类中的成员变量   适用的规则ruleId如下，具体规则请看ruleDescription.md：
 * RULE-018,RULE-019
 */
public class ExtractFieldHandler extends Handler {

    public void extract(FieldDeclaration node, Rule rule, File file, List<Snippet> snippets) {
        Map<String, FieldAccessInfo> fieldMap = new LinkedHashMap<>();
        // 1. 收集成员变量
        node.getVariables().forEach(var -> {
            FieldAccessInfo info = new FieldAccessInfo();
            info.fd = node;
            info.fieldName = var.getNameAsString();
            fieldMap.put(info.fieldName, info);
        });

        // 2. 扫描方法
        node.findAncestor(ClassOrInterfaceDeclaration.class).get().getMethods().forEach(method ->
                analyzeMethod(method, fieldMap)
        );

        // 3. 打印结果
        fieldMap.values().forEach(info -> {
                    int line = info.fd.getRange()
                            .map(r -> r.begin.line)
                            .orElse(-1);
                    String message = "成员变量" + info.fieldName +
                            "，set方法 " + methodName(info.setter) +
                            "，get方法 " + methodName(info.getter);
                    snippets.add(new Snippet(rule, file, line, info.fd.toString(), message, "FIELD_DECLARATION"));
                }
        );
    }

    private static void analyzeMethod(MethodDeclaration method,
                                      Map<String, FieldAccessInfo> fieldMap) {
        if (method.getBody().isEmpty()) {
            return;
        }

        BlockStmt body = method.getBody().get();
        // setter：写字段
        body.findAll(AssignExpr.class).forEach(assign -> {
            String field = extractFieldName(assign.getTarget());
            if (fieldMap.containsKey(field)) {
                fieldMap.get(field).setter = method;
            }
        });

        // getter：读字段
        body.findAll(ReturnStmt.class).forEach(ret -> {
            ret.getExpression().ifPresent(expr -> {
                String field = extractFieldName(expr);
                if (fieldMap.containsKey(field)) {
                    fieldMap.get(field).getter = method;
                }
            });
        });
    }

    private static String extractFieldName(Expression expr) {
        // this.a
        if (expr instanceof FieldAccessExpr fa) {
            return fa.getNameAsString();
        }
        // a
        if (expr instanceof NameExpr ne) {
            return ne.getNameAsString();
        }
        return null;
    }

    private static String methodName(MethodDeclaration m) {
        return m == null ? "无" : m.getNameAsString() + "()";
    }

    class FieldAccessInfo {
        String fieldName;
        FieldDeclaration fd;
        MethodDeclaration getter;
        MethodDeclaration setter;
    }
}



