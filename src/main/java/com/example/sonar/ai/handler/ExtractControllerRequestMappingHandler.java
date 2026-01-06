package com.example.sonar.ai.handler;

import com.example.sonar.ai.model.Rule;
import com.example.sonar.ai.model.Snippet;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 抽取Controller类的RequestMapping的请求路径 eg. /api/user/get。 适用的规则ruleId如下，具体规则请看ruleDescription.md：
 * RULE-009
 */
public class ExtractControllerRequestMappingHandler extends Handler {

    @Override
    public void extract(TypeDeclaration<?> node, Rule rule, File file, List<Snippet> snippets) {
        List<String> classPaths = List.of("");

        for (AnnotationExpr ann : node.getAnnotations()) {
            if ("RequestMapping".equals(ann.getNameAsString())) {
                classPaths = getPaths(ann);
            }
        }

        for (MethodDeclaration method : node.getMethods()) {
            String methodName = method.getNameAsString();
            for (AnnotationExpr ann : method.getAnnotations()) {
                if (!ann.getNameAsString().endsWith("Mapping")) {
                    continue;
                }
                List<String> methodPaths = getPaths(ann);
                // 方法级注解行号
                int line = ann.getRange()
                        .map(r -> r.begin.line)
                        .orElse(-1);
                for (String cp : classPaths) {
                    for (String mp : methodPaths) {
                        String fullPath = normalize(cp, mp);
                        snippets.add(new Snippet(rule, file, line, fullPath, methodName, "CLASS_DECLARATION"));
                    }
                }
            }
        }
    }

    private static List<String> getPaths(AnnotationExpr ann) {
        List<String> paths = new ArrayList<>();
        // @GetMapping("/x")
        if (ann instanceof SingleMemberAnnotationExpr) {
            paths.add(((SingleMemberAnnotationExpr) ann)
                    .getMemberValue()
                    .asStringLiteralExpr()
                    .asString());
        }
        // @RequestMapping(value = "/x")
        // @RequestMapping(value = {"/a","/b"})
        if (ann instanceof NormalAnnotationExpr) {
            for (MemberValuePair pair :
                    ((NormalAnnotationExpr) ann).getPairs()) {

                if (!"value".equals(pair.getNameAsString())
                        && !"path".equals(pair.getNameAsString())) {
                    continue;
                }
                Expression v = pair.getValue();
                if (v.isStringLiteralExpr()) {
                    paths.add(v.asStringLiteralExpr().asString());
                }
                if (v.isArrayInitializerExpr()) {
                    v.asArrayInitializerExpr()
                            .getValues()
                            .forEach(e ->
                                    paths.add(e.asStringLiteralExpr().asString())
                            );
                }
            }
        }
        // 没写 value/path 的情况
        if (paths.isEmpty()) {
            paths.add("");
        }
        return paths;
    }

    private String normalize(String a, String b) {
        return ("/" + a + "/" + b)
                .replaceAll("/+", "/")
                .replaceAll("/$", "");
    }
}