package com.example.sonar.ai.handler;

import com.example.sonar.ai.model.Rule;
import com.example.sonar.ai.model.Snippet;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.Expression;

import java.io.File;
import java.util.List;

/**
 * 抽取类，方法，变量名。 适用的规则ruleId如下，具体规则请看ruleDescription.md：
 * RULE-001,RULE-002,RULE-003
 */
public class ExtractClassMethodVariableHandler extends Handler {

    @Override
    public void extract(TypeDeclaration<?> node, Rule rule, File file, List<Snippet> snippets) {
        int line = node.getName().getBegin().map(p -> p.line).orElse(
                node.getBegin().map(p -> p.line).orElse(1));
        StringBuilder sb = new StringBuilder();
        if (node.isPublic())
            sb.append("public ");
        else if (node.isPrivate())
            sb.append("private ");
        else if (node.isProtected())
            sb.append("protected ");
        if (node.isStatic())
            sb.append("static ");
        if (node instanceof ClassOrInterfaceDeclaration) {
            ClassOrInterfaceDeclaration cid = (ClassOrInterfaceDeclaration) node;
            if (cid.isInterface())
                sb.append("interface ");
            else
                sb.append("class ");
        } else if (node instanceof EnumDeclaration) {
            sb.append("enum ");
        }
        sb.append(node.getNameAsString());
        snippets.add(new Snippet(rule, file, line, sb.toString(), node.getNameAsString(), "CLASS_DECLARATION"));
    }

    @Override
    public void extract(MethodDeclaration node, Rule rule, File file, List<Snippet> snippets) {
        int line = node.getName().getBegin().map(p -> p.line).orElse(
                node.getBegin().map(p -> p.line).orElse(1));
        String cleanCode = node.getDeclarationAsString(true, true, true);
        snippets.add(new Snippet(rule, file, line, cleanCode, node.getNameAsString(), "METHOD_DECLARATION"));
    }

    @Override
    public void extract(VariableDeclarator node, Rule rule, File file, List<Snippet> snippets) {
        int line = node.getName().getBegin().map(p -> p.line).orElse(
                node.getBegin().map(p -> p.line).orElse(1));
        String type = node.getType().toString();
        String name = node.getNameAsString();
        String initializer = node.getInitializer()
                .map(Expression::toString)
                .orElse(null);
        String result = initializer == null
                ? type + " " + name
                : type + " " + name + " = " + initializer;
        snippets.add(new Snippet(rule, file, line, result, node.getNameAsString(), "VARIABLE_DECLARATION"));
    }
}
