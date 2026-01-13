package com.example.sonar.ai.handler;

import com.example.sonar.ai.model.Rule;
import com.example.sonar.ai.model.Snippet;
import com.github.javaparser.ast.body.*;

import java.io.File;
import java.util.List;

/**
 * 抽取接口Interface的成员属性。 适用的规则ruleId如下，具体规则请看ruleDescription.md：
 * RULE-014
 */
public class ExtractInterfaceFieldHandler extends Handler {

    @Override
    public void extract(TypeDeclaration<?> node, Rule rule, File file, List<Snippet> snippets) {
        if (node instanceof ClassOrInterfaceDeclaration iface) {
            StringBuffer sb = new StringBuffer();
            if (iface.isInterface()) {
                if (node.isPublic()) {
                    sb.append("public ");
                } else if (node.isPrivate()) {
                    sb.append("private ");
                } else if (node.isProtected()) {
                    sb.append("protected ");
                }
                if (node.isStatic()) {
                    sb.append("static ");
                }
                if (iface.isInterface()) {
                    sb.append("interface ");
                } else {
                    sb.append("class ");
                }
                sb.append(iface.getNameAsString());
                var methods = iface.getMethods();
                iface.getMembers().stream()
                        .filter(m -> m instanceof FieldDeclaration)
                        .map(m -> (FieldDeclaration) m)
                        .forEach(field -> field.getVariables().forEach(var -> {
                            String fieldName = var.getNameAsString();
                            int line = var.getRange()
                                    .map(r -> r.begin.line)
                                    .orElse(-1);
                            // 1️⃣ 非字面量初始化（ERROR）
                            if (var.getInitializer().isEmpty()
                                    || !var.getInitializer().get().isLiteralExpr()) {
                                //"不是基础常量（非常量初始化）"
                                snippets.add(new Snippet(rule, file, line, field.toString().trim(), sb.toString(), "CLASS_DECLARATION"));
                                return;
                            }
                            // 2️⃣ 命名不规范（WARN）
                            if (!isUpperSnakeCase(fieldName)) {
                                //"命名不符合基础常量规范"
                                snippets.add(new Snippet(rule, file, line, field.toString().trim(), sb.toString(), "CLASS_DECLARATION"));
                            }
                        }));
            }
        }
    }

    private static boolean isUpperSnakeCase(String name) {
        return name.matches("[A-Z0-9_]+");
    }
}
