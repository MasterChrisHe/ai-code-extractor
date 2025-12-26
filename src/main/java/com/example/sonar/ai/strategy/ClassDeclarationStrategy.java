package com.example.sonar.ai.strategy;

import com.example.sonar.ai.model.Rule;
import com.example.sonar.ai.model.Snippet;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;

import java.io.File;
import java.util.List;

/**
 * 类/接口/枚举声明抽取策略
 */
public class ClassDeclarationStrategy implements ExtractionStrategy<TypeDeclaration<?>> {

    @Override
    public boolean supports(Rule rule, Node node) {
        return "CLASS_DECLARATION".equalsIgnoreCase(rule.getScope()) &&
                (node instanceof ClassOrInterfaceDeclaration || node instanceof EnumDeclaration);
    }

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

        snippets.add(new Snippet(rule, file, line, sb.toString(), node.getNameAsString()));
    }
}
