package com.example.sonar.ai.handler;

import com.example.sonar.ai.model.Rule;
import com.example.sonar.ai.model.Snippet;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;

import java.io.File;
import java.util.List;

/**
 * 抽取类中的成员常量   适用的规则ruleId如下，具体规则请看ruleDescription.md：
 * RULE-026
 */
public class ExtractConstantHandler extends Handler {

    public void extract(FieldDeclaration field, Rule rule, File file, List<Snippet> snippets) {
        // 只抽取static final
        if (field.hasModifier(Modifier.Keyword.STATIC)
                && field.hasModifier(Modifier.Keyword.FINAL)) {

            for (VariableDeclarator var : field.getVariables()) {
                String type = var.getType().toString();
                String name = var.getNameAsString();
                String value = var.getInitializer()
                        .map(Object::toString)
                        .orElse("");
                int line = var.getName().getBegin().map(p -> p.line).orElse(
                        var.getBegin().map(p -> p.line).orElse(1));
                // 按要求打印格式
                String result = String.format(
                        "%s static final %s %s = %s;",
                        getAccessModifier(field),
                        type,
                        name,
                        value
                );
                snippets.add(new Snippet(rule, file, line, result, name, "FIELD_DECLARATION"));
            }
        }
    }

    private static String getAccessModifier(FieldDeclaration field) {
        if (field.hasModifier(Modifier.Keyword.PUBLIC)) {
            return "public";
        } else if (field.hasModifier(Modifier.Keyword.PRIVATE)) {
            return "private";
        } else if (field.hasModifier(Modifier.Keyword.PROTECTED)) {
            return "protected";
        } else {
            return ""; // default (包级权限)
        }
    }
}
