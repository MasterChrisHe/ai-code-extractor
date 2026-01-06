package com.example.sonar.ai.handler;

import com.example.sonar.ai.model.Rule;
import com.example.sonar.ai.model.Snippet;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.comments.Comment;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.List;

/**
 * 抽取方法名和对应方法的javadoc。 适用的规则ruleId如下，具体规则请看ruleDescription.md：
 * RULE-005,RULE-006,RULE-007,RULE-008
 */
public class ExtractMethodAndJavaDocHandler extends Handler {

    @Override
    public void extract(MethodDeclaration node, Rule rule, File file, List<Snippet> snippets) {
        int line = node.getName().getBegin().map(p -> p.line).orElse(node.getBegin().map(p -> p.line).orElse(1));
        // 方法名
        String methodName = node.getNameAsString();
        // JavaDoc
        String javaDoc = "";
        if (node.getJavadoc().isPresent()) {
            javaDoc = node.getJavadoc().map(javadoc -> javadoc.getDescription().toText()).get();
        } else {
            javaDoc = node.getComment()
                    .map(Comment::getContent).orElse("无JavaDoc");
        }
        snippets.add(new Snippet(rule, file, line, javaDoc, methodName, "METHOD_DECLARATION"));
    }
}
