package com.example.sonar.ai.handler;

import com.example.sonar.ai.model.Rule;
import com.example.sonar.ai.model.Snippet;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;

import java.io.File;
import java.util.List;


public abstract class Handler {

    /**
     * 对类和枚举操作
     * @param node
     * @param rule
     * @param file
     * @param snippets
     */
    public void extract(TypeDeclaration<?> node, Rule rule, File file, List<Snippet> snippets){
        throw new UnsupportedOperationException("Node not implemented yet");
    }


    /**
     * 对方法体操作
     * @param node
     * @param rule
     * @param file
     * @param snippets
     */
    public void extract(MethodDeclaration node, Rule rule, File file, List<Snippet> snippets){
        throw new UnsupportedOperationException("Method not implemented yet");
    }

    /**
     * 对变量操作
     * @param node
     * @param rule
     * @param file
     * @param snippets
     */
    public  void extract(VariableDeclarator node, Rule rule, File file, List<Snippet> snippets){
        throw new UnsupportedOperationException("Variable not implemented yet");
    }
}
