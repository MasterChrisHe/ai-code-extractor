package com.example.sonar.ai.handler;

import com.example.sonar.ai.model.Rule;
import com.example.sonar.ai.model.Snippet;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.stmt.ThrowStmt;

import java.io.File;
import java.util.List;


public abstract class Handler {

    /**
     * 对所有类型操作
     *
     * @param node
     * @param rule
     * @param file
     * @param snippets
     */
    public void extract(Node node, Rule rule, File file, List<Snippet> snippets) {
        throw new UnsupportedOperationException("Node extraction not implemented yet");
    }

    /**
     * 对类或接口操作
     *
     * @param node
     * @param rule
     * @param file
     * @param snippets
     */
    public void extract(ClassOrInterfaceDeclaration node, Rule rule, File file, List<Snippet> snippets) {
        throw new UnsupportedOperationException("ClassOrInterfaceDeclaration extraction not implemented yet");
    }

    /**
     * 对类和枚举操作
     *
     * @param node
     * @param rule
     * @param file
     * @param snippets
     */
    public void extract(TypeDeclaration<?> node, Rule rule, File file, List<Snippet> snippets) {
        throw new UnsupportedOperationException("TypeDeclaration extraction not implemented yet");
    }


    /**
     * 对方法体操作
     *
     * @param node
     * @param rule
     * @param file
     * @param snippets
     */
    public void extract(MethodDeclaration node, Rule rule, File file, List<Snippet> snippets) {
        throw new UnsupportedOperationException("Method extraction not implemented yet");
    }

    /**
     * 对变量操作
     *
     * @param node
     * @param rule
     * @param file
     * @param snippets
     */
    public void extract(VariableDeclarator node, Rule rule, File file, List<Snippet> snippets) {
        throw new UnsupportedOperationException("Variable extraction not implemented yet");
    }

    /**
     * 对象创建
     *
     * @param node
     * @param rule
     * @param file
     * @param snippets
     */
    public void extract(ObjectCreationExpr node, Rule rule, File file, List<Snippet> snippets) {
        throw new UnsupportedOperationException("ObjectCreation extraction not implemented yet");
    }

    /**
     * 方法声明
     *
     * @param node
     * @param rule
     * @param file
     * @param snippets
     */
    public void extract(MethodCallExpr node, Rule rule, File file, List<Snippet> snippets) {
        throw new UnsupportedOperationException("MethodCallExpr extraction not implemented yet");
    }

    /**
     * 主动抛出异常声明
     *
     * @param node
     * @param rule
     * @param file
     * @param snippets
     */
    public void extract(ThrowStmt node, Rule rule, File file, List<Snippet> snippets) {
        throw new UnsupportedOperationException("ThrowStmt extraction not implemented yet");
    }

    /**
     * 成员变量
     *
     * @param node
     * @param rule
     * @param file
     * @param snippets
     */
    public void extract(FieldDeclaration node, Rule rule, File file, List<Snippet> snippets) {
        throw new UnsupportedOperationException("FieldDeclaration extraction not implemented yet");
    }
}
