package com.example.sonar.ai;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

/**
 * 独立的 Java 代码提取工具
 * 功能：读取 CSV 规则 -> 扫描目录 -> 解析 AST -> 输出 JSON 到 Stdout
 */
public class CodeExtractor {

    // 数据传输对象 (DTO)
    private static class Snippet {
        String ruleId;
        String ruleDesc;
        String ruleCriteria;
        String ruleContext;
        String file;
        int line;
        String code;
        String name;
    }

    // 规则对象
    private static class Rule {
        String id;
        String scope;
        String description;
        String criteria;
        String context;
    }

    public static void main(String[] args) {
        // 简单的参数校验
        if (args.length != 2) {
            System.err.println("Usage: java -jar ai-code-extractor.jar <sourceDir> <rulesFile>");
            System.exit(1);
        }

        String sourceDir = args[0];
        String rulesFile = args[1];

        System.err.println("INFO: Starting CodeExtractor...");
        System.err.println("INFO: Source Dir: " + sourceDir);
        System.err.println("INFO: Rules File: " + rulesFile);

        // 1. 读取规则
        List<Rule> rules = readRules(rulesFile);
        if (rules.isEmpty()) {
            System.err.println("WARN: No rules found or file is empty.");
        }

        // 2. 扫描并提取
        List<Snippet> allCandidates = new ArrayList<>();

        // 预先扫描所有 Java 文件，避免重复 IO
        List<File> javaFiles = findAllJavaFiles(sourceDir);
        System.err.println("INFO: Found " + javaFiles.size() + " Java files.");

        for (Rule rule : rules) {
            System.err.printf("INFO: Processing Rule [%s] -> Scope: %s%n", rule.id, rule.scope);
            int countBefore = allCandidates.size();

            for (File file : javaFiles) {
                parseFile(file, rule, allCandidates);
            }

            int extractedCount = allCandidates.size() - countBefore;
            System.err.printf("INFO: Extracted %d candidates for Rule [%s]%n", extractedCount, rule.id);
        }

        // 3. 输出结果到标准输出 (STDOUT)
        // 这是与 Groovy 脚本通信的唯一通道，必须保证是纯净的 JSON
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        System.out.println(gson.toJson(allCandidates));

        System.err.println("INFO: Extraction complete. JSON output finished.");
    }

    // --- 辅助方法 ---

    private static List<Rule> readRules(String rulesFile) {
        List<Rule> rules = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(rulesFile))) {
            String line;
            boolean headerSkipped = false;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                if (!headerSkipped) {
                    headerSkipped = true;
                    continue;
                }

                // 解析 CSV: RuleID, Scope, Description, Criteria, Context
                // 限制 split 为 5，确保最后一个字段可以包含逗号（如果有引号处理逻辑需更复杂，这里简化处理）
                String[] parts = line.split(",", 5);
                if (parts.length >= 5) {
                    Rule rule = new Rule();
                    rule.id = parts[0].trim();
                    rule.scope = parts[1].trim();
                    rule.description = parts[2].trim();
                    rule.criteria = parts[3].trim();
                    rule.context = parts[4].trim();
                    rules.add(rule);
                }
            }
        } catch (IOException e) {
            System.err.println("ERROR: Failed to read rules file: " + e.getMessage());
            System.exit(1);
        }
        return rules;
    }

    private static List<File> findAllJavaFiles(String sourceDir) {
        List<File> files = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(Paths.get(sourceDir))) {
            walk.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".java"))
                    .forEach(p -> files.add(p.toFile()));
        } catch (IOException e) {
            System.err.println("ERROR: Failed to walk directory: " + e.getMessage());
        }
        return files;
    }

    private static void parseFile(File file, Rule rule, List<Snippet> collector) {
        try {
            // 使用 JavaParser 解析 AST
            JavaParser parser = new JavaParser();
            // 解析配置（可选，根据需要调整）

            Optional<CompilationUnit> result = parser.parse(file).getResult();

            if (result.isPresent()) {
                CompilationUnit cu = result.get();

                cu.accept(new VoidVisitorAdapter<Void>() {
                    // 处理类/接口声明
                    @Override
                    public void visit(ClassOrInterfaceDeclaration n, Void arg) {
                        super.visit(n, arg);
                        if ("CLASS_DECLARATION".equalsIgnoreCase(rule.scope)) {
                            addSnippet(n.getNameAsString(), n.getBegin().map(p -> p.line).orElse(-1), n.toString());
                        }
                    }

                    // 处理方法声明
                    @Override
                    public void visit(MethodDeclaration n, Void arg) {
                        super.visit(n, arg);
                        if ("METHOD_DECLARATION".equalsIgnoreCase(rule.scope)) {
                            addSnippet(n.getNameAsString(), n.getBegin().map(p -> p.line).orElse(-1), n.toString());
                        }
                    }

                    private void addSnippet(String name, int line, String fullCode) {
                        // 只取第一行代码作为预览
                        String firstLineCode = fullCode.split("\n", 2)[0].trim();

                        Snippet s = new Snippet();
                        s.ruleId = rule.id;
                        s.ruleDesc = rule.description;
                        s.ruleCriteria = rule.criteria;
                        s.ruleContext = rule.context;
                        s.file = file.getAbsolutePath();
                        s.line = line;
                        s.code = firstLineCode; // 也可以传 fullCode，看 AI 需要
                        s.name = name;
                        collector.add(s);
                    }
                }, null);
            }
        } catch (Exception e) {
            System.err.println("WARN: Failed to parse " + file.getName() + ": " + e.getMessage());
        }
    }
}