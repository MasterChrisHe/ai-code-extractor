package com.example.sonar.ai.handler;

import com.example.sonar.ai.model.Rule;
import com.example.sonar.ai.model.Snippet;
import com.github.javaparser.ast.body.*;

import java.io.File;
import java.util.*;

/**
 * 抽取类的构造方法和重载方法。 适用的规则ruleId如下，具体规则请看ruleDescription.md：
 * RULE-010
 */
public class ExtractClassContructionOverrideHandler extends Handler {

    @Override
    public void extract(TypeDeclaration<?> node, Rule rule, File file, List<Snippet> snippets) {
        if (node instanceof ClassOrInterfaceDeclaration cid) {
            List<BodyDeclaration<?>> members = cid.getMembers();
            checkConstructors(cid, members, rule, file, snippets);
            checkOverloadedMethods(cid, members, rule, file, snippets);
        }
    }

    private static void checkConstructors(
            ClassOrInterfaceDeclaration clazz,
            List<BodyDeclaration<?>> members,
            Rule rule, File file, List<Snippet> snippets) {
        String className = clazz.getNameAsString();
        boolean inCtorBlock = false;
        boolean ctorBlockClosed = false;
        for (BodyDeclaration<?> member : members) {
            if (member instanceof ConstructorDeclaration) {
                if (ctorBlockClosed) {
                    int line = member.getRange()
                            .map(r -> r.begin.line)
                            .orElse(-1);
                    snippets.add(new Snippet(rule, file, line, member.toString().trim(), "类" + className + "中构造函数未连续出现", "CLASS_DECLARATION"));
                    return;
                }
                inCtorBlock = true;
            } else if (inCtorBlock) {
                // 构造函数块结束
                ctorBlockClosed = true;
            }
        }
    }

    private static void checkOverloadedMethods(
            ClassOrInterfaceDeclaration clazz,
            List<BodyDeclaration<?>> members,
            Rule rule, File file, List<Snippet> snippets) {
        String className = clazz.getNameAsString();
        String currentName = null;
        Set<String> finishedNames = new HashSet<>();
        for (BodyDeclaration<?> member : members) {
            if (!(member instanceof MethodDeclaration method)) {
                // 非方法，直接打断当前方法块
                if (currentName != null) {
                    finishedNames.add(currentName);
                    currentName = null;
                }
                continue;
            }
            String methodName = method.getNameAsString();
            // ❌ 之前已经结束过该方法的连续块
            if (finishedNames.contains(methodName)) {
                int line = member.getRange()
                        .map(r -> r.begin.line)
                        .orElse(-1);
                snippets.add(new Snippet(rule, file, line, method.toString().trim(), "类" + className + "中重载方法未连续出现", "CLASS_DECLARATION"));
            }
            // 新的连续块开始
            if (currentName == null) {
                currentName = methodName;
                continue;
            }
            // 连续出现同名方法，OK
            if (currentName.equals(methodName)) {
                continue;
            }
            // 当前块被打断
            finishedNames.add(currentName);
            currentName = methodName;
        }
    }
}
