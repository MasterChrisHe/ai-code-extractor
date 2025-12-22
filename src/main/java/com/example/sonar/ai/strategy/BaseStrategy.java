package com.example.sonar.ai.strategy;

import com.example.sonar.ai.model.Rule;
import com.example.sonar.ai.model.Snippet;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;

import java.io.File;
import java.util.List;
import java.util.Map;

public abstract class BaseStrategy {

    private final File currentFile;

    public BaseStrategy(File file) {
        this.currentFile = file;
    }

    public void doClassOrInterfaceDeclaration(ClassOrInterfaceDeclaration n, Map<Rule, List<Snippet>> collector) {
        throw new UnsupportedOperationException(
                "doClassOrInterfaceDeclaration() is not supported in BaseStrategy"
        );
    }


    public void doEnumDeclaration(EnumDeclaration n, Map<Rule, List<Snippet>> collector) {
        throw new UnsupportedOperationException(
                "doEnumDeclaration() is not supported in BaseStrategy"
        );
    }


    public void doMethodDeclaration(MethodDeclaration n, Map<Rule, List<Snippet>> collector) {
        throw new UnsupportedOperationException(
                "doMethodDeclaration() is not supported in BaseStrategy"
        );
    }


    public void addSnippet(String scope, String name, int line, String code, Map<Rule, List<Snippet>> collector) {
        collector.forEach((rule, snippets) -> {
            if (scope.equalsIgnoreCase(rule.getScope())) {
                snippets.add(new Snippet(rule, currentFile, line, code, name));
            }
        });
    }
}
