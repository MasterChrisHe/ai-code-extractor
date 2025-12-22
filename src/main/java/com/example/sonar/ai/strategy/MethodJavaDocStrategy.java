package com.example.sonar.ai.strategy;

import com.example.sonar.ai.model.Rule;
import com.example.sonar.ai.model.Snippet;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * 抽取方法javadoc注释的策略
 */
public class MethodJavaDocStrategy extends BaseStrategy {


    public MethodJavaDocStrategy(File file) {
        super(file);
    }

    @Override
    public void doMethodDeclaration(MethodDeclaration n, Map<Rule, List<Snippet>> collector) {
        //抽取方法名，类名
        String className = n.findAncestor(ClassOrInterfaceDeclaration.class)
                .map(ClassOrInterfaceDeclaration::getNameAsString)
                .orElse("");
        String methodName = n.getNameAsString();

        int line = n.getRange()
                .map(r -> r.begin.line)
                .orElse(-1);
        //抽取javaDoc
        String javaDoc = n.getJavadoc()
                .map(javadoc -> javadoc.getDescription().toText()).orElse("");
        System.out.println("--------------------------------------------------");
        System.out.println("类名: " + className);
        System.out.println("方法: " + methodName);
        System.out.println("行号: " + line);
        System.out.println("javaDoc:" + javaDoc);
        super.addSnippet("METHOD_DECLARATION", methodName, line, javaDoc, collector);
    }

}
