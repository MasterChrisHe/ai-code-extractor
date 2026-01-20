package com.example.sonar.ai.strategy;

import com.example.sonar.ai.model.Rule;
import com.example.sonar.ai.model.Snippet;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.javadoc.Javadoc;

import java.io.File;
import java.util.List;
import java.util.Optional;

/**
 * 通用 Javadoc 抽取策略
 * 支持 CLASS_JAVADOC 和 METHOD_JAVADOC
 * 规则：
 * 1. 提取 Javadoc 的 Summary 部分（忽略标签）
 * 2. 结合类/方法的签名行
 * 3. 行号指向 Summary 的第一行
 */
public class JavadocStrategy implements ExtractionStrategy<BodyDeclaration<?>> {

    @Override
    public boolean supports(Rule rule, Node node) {
        boolean isLegacyClassScope = "CLASS_JAVADOC".equalsIgnoreCase(rule.getScope());
        boolean isLegacyMethodScope = "METHOD_JAVADOC".equalsIgnoreCase(rule.getScope());
        boolean isUnifiedScope = "JAVADOC".equalsIgnoreCase(rule.getScope());

        if (isUnifiedScope) {
            return node instanceof MethodDeclaration || node instanceof ClassOrInterfaceDeclaration
                    || node instanceof EnumDeclaration;
        }

        if (isLegacyClassScope) {
            return node instanceof ClassOrInterfaceDeclaration || node instanceof EnumDeclaration;
        } else if (isLegacyMethodScope) {
            return node instanceof MethodDeclaration;
        }
        return false;
    }

    @Override
    public void extract(BodyDeclaration<?> node, Rule rule, File file, List<Snippet> snippets) {
        // BodyDeclaration<?> might not expose getJavadoc() directly in all versions or
        // due to generics.
        // We cast to specific supported types to access getJavadoc() safely.
        Optional<Javadoc> javadocOpt = Optional.empty();

        if (node instanceof MethodDeclaration) {
            MethodDeclaration method = (MethodDeclaration) node;
            // Only extract javadoc for public and protected methods
            // Skip private and package-private methods
            if (method.isPublic() || method.isProtected()) {
                javadocOpt = method.getJavadoc();
            } else {
                // Skip private and package-private methods
                return;
            }
        } else if (node instanceof ClassOrInterfaceDeclaration) {
            javadocOpt = ((ClassOrInterfaceDeclaration) node).getJavadoc();
        } else if (node instanceof EnumDeclaration) {
            javadocOpt = ((EnumDeclaration) node).getJavadoc();
        }

        javadocOpt.ifPresent(javadoc -> {
            String summary = javadoc.getDescription().toText();
            // Javadoc strategy:
            // 1. Empty javadoc -> skip
            if (summary == null || summary.trim().isEmpty()) {
                return;
            }

            // 2. Line number: Calculate precise line number by scanning raw content
            // Default to start of Javadoc block
            int commentStartLine = node.getComment()
                    .flatMap(Node::getBegin)
                    .map(p -> p.line)
                    .orElse(node.getBegin().map(p -> p.line).orElse(1));

            int line = commentStartLine; // Fallback

            // Improve precision: Search for the first significant line of the summary in
            // the raw content
            try {
                String[] summaryLines = summary.split("\n");
                if (summaryLines.length > 0) {
                    String firstSummaryLine = summaryLines[0].trim();
                    if (!firstSummaryLine.isEmpty()) {
                        // Get raw javadoc content (without /** */) usually, but check implementation
                        String rawContent = node.getComment()
                                .map(com.github.javaparser.ast.comments.Comment::getContent).orElse("");
                        String[] lines = rawContent.split("\n");

                        for (int i = 0; i < lines.length; i++) {
                            if (lines[i].contains(firstSummaryLine)) {
                                // Line offset = i.
                                // Note: commentStartLine is where /** is.
                                // Usually /** is on its own line or start of content.
                                // getContent() typically excludes /** and */?
                                // JavadocComment.getContent() returns the text INSIDE.
                                // If /** is on line L, and content starts on line L (e.g. /** content */),
                                // deviation is 0?
                                // If /** \n * content, content is on L+1.

                                // We need to account for how JavaParser counts lines vs content.
                                // If we found the line in content at index i, does that map to commentStartLine
                                // + i?
                                // Mostly yes, assuming each \n in content maps to a new source line.
                                line = commentStartLine + i;

                                // Correction: if valid javadoc starts with /**\n, index 0 might be empty or *.
                                // Safe bet: The source line is commentStartLine + i IF the content split
                                // matches source lines.
                                // However, simple split might miss mixed newline types, but standard for now.
                                break;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // strict fallback
            }

            // Attempts to find specific line of summary could involve parsing 'summary'
            // or checking JavadocDescription elements, but getBegin is not reliably
            // available on Description in all versions.

            // 3. Signature extraction
            String signature = "";
            String name = "";
            if (node instanceof MethodDeclaration) {
                MethodDeclaration md = (MethodDeclaration) node;
                signature = md.getDeclarationAsString(true, true, true);
                name = md.getNameAsString();
            } else if (node instanceof TypeDeclaration) {
                TypeDeclaration<?> td = (TypeDeclaration<?>) node;
                name = td.getNameAsString();

                StringBuilder sb = new StringBuilder();
                if (td.isPublic())
                    sb.append("public ");
                else if (td.isPrivate())
                    sb.append("private ");
                else if (td.isProtected())
                    sb.append("protected ");
                if (td.isStatic())
                    sb.append("static ");

                if (td instanceof ClassOrInterfaceDeclaration) {
                    ClassOrInterfaceDeclaration cid = (ClassOrInterfaceDeclaration) td;
                    if (cid.isInterface())
                        sb.append("interface ");
                    else
                        sb.append("class ");
                } else if (td instanceof EnumDeclaration) {
                    sb.append("enum ");
                }
                sb.append(name);
                signature = sb.toString();
            }

            // Use simple \n as per original code
            String content = summary + "\n" + signature;
            snippets.add(new Snippet(rule, file, line, content, name, rule.getScope()));
        });
    }
}
