package com.example.sonar.ai.handler;

import com.example.sonar.ai.model.Rule;
import com.example.sonar.ai.model.Snippet;
import com.github.javaparser.ast.body.*;

import java.io.File;
import java.util.List;

/**
 * 抽取类的成员属性和方法。 适用的规则ruleId如下，具体规则请看ruleDescription.md：
 * RULE-022
 */
public class ExtractClassMethodFieldHandler extends Handler {

    @Override
    public void extract(TypeDeclaration<?> node, Rule rule, File file, List<Snippet> snippets) {
        if (node instanceof ClassOrInterfaceDeclaration) {
            StringBuilder cleanCode = new StringBuilder();
            int line = node.getRange()
                    .map(r -> r.begin.line)
                    .orElse(-1);
            String classDeclaration = buildClassDeclaration((ClassOrInterfaceDeclaration) node);
            //成员变量，方法
            node.getMembers().forEach(member -> {
                if (member instanceof FieldDeclaration field) {
                    cleanCode.append("    ")
                            .append(field.toString().trim())
                            .append("\n");
                }

                if (member instanceof MethodDeclaration method) {
                    cleanCode.append("    ")
                            .append(buildMethodDeclaration(method))
                            .append("\n");
                }

                if (member instanceof ConstructorDeclaration ctor) {
                    cleanCode.append("    ")
                            .append(buildConstructorDeclaration(ctor))
                            .append("\n");
                }
            });
            snippets.add(new Snippet(rule, file, line, cleanCode.toString(), classDeclaration, "CLASS_DECLARATION"));
        }
    }

    private String buildConstructorDeclaration(
            ConstructorDeclaration ctor) {
        StringBuilder sb = new StringBuilder();
        ctor.getModifiers().forEach(m ->
                sb.append(m.getKeyword().asString()).append(" ")
        );
        sb.append(ctor.getNameAsString()).append("(");
        sb.append(
                ctor.getParameters().stream()
                        .map(p -> p.getType() + " " + p.getName())
                        .collect(java.util.stream.Collectors.joining(", "))
        );
        sb.append(");");
        return sb.toString();
    }

    private String buildMethodDeclaration(
            MethodDeclaration method) {
        StringBuilder sb = new StringBuilder();
        // 修饰符
        method.getModifiers().forEach(m ->
                sb.append(m.getKeyword().asString()).append(" ")
        );
        // 返回值
        sb.append(method.getType()).append(" ");
        // 方法名
        sb.append(method.getNameAsString());
        // 参数
        sb.append("(");
        sb.append(
                method.getParameters().stream()
                        .map(p -> p.getType() + " " + p.getName())
                        .collect(java.util.stream.Collectors.joining(", "))
        );
        sb.append(")");
        // throws
        if (!method.getThrownExceptions().isEmpty()) {
            sb.append(" throws ");
            sb.append(
                    method.getThrownExceptions().stream()
                            .map(Object::toString)
                            .collect(java.util.stream.Collectors.joining(", "))
            );
        }
        sb.append(";");
        return sb.toString();
    }

    /**
     * 返回类声明的字符串
     *
     * @param clazz
     * @return
     */
    private String buildClassDeclaration(
            ClassOrInterfaceDeclaration clazz) {
        StringBuilder sb = new StringBuilder();
        // 1️⃣ 修饰符
        clazz.getModifiers().forEach(m ->
                sb.append(m.getKeyword().asString()).append(" ")
        );
        // 2️⃣ class / interface
        sb.append(clazz.isInterface() ? "interface " : "class ");
        // 3️⃣ 类名
        sb.append(clazz.getNameAsString());
        // 4️⃣ 泛型
        if (!clazz.getTypeParameters().isEmpty()) {
            sb.append("<");
            sb.append(
                    clazz.getTypeParameters().stream()
                            .map(Object::toString)
                            .collect(java.util.stream.Collectors.joining(", "))
            );
            sb.append(">");
        }
        // 5️⃣ extends
        if (!clazz.getExtendedTypes().isEmpty()) {
            sb.append(" extends ");
            sb.append(
                    clazz.getExtendedTypes().stream()
                            .map(Object::toString)
                            .collect(java.util.stream.Collectors.joining(", "))
            );
        }
        // 6️⃣ implements
        if (!clazz.getImplementedTypes().isEmpty()) {
            sb.append(" implements ");
            sb.append(
                    clazz.getImplementedTypes().stream()
                            .map(Object::toString)
                            .collect(java.util.stream.Collectors.joining(", "))
            );
        }
        return sb.toString();
    }

}
