package com.example.sonar.ai.service;

import com.example.sonar.ai.model.Rule;
import com.example.sonar.ai.model.Snippet;
import com.example.sonar.ai.parser.JavaCodeVisitor;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

/**
 * 业务逻辑服务类
 * 负责扫描文件、调用解析器、汇总结果
 */
public class CodeExtractorService {

    private final String sourceDir;
    private final List<Rule> rules;

    public CodeExtractorService(String sourceDir, List<Rule> rules) {
        this.sourceDir = sourceDir;
        this.rules = rules;
    }

    /**
     * 递归查找目录下所有 Java 文件
     */
    private List<File> findAllJavaFiles(String sourceDir) {
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

    /**
     * 解析单个文件
     */
    private Map<Rule, List<Snippet>> parseFile(File file) {
        // key 是规则，value 是该规则在该文件中匹配到的片段列表
        Map<Rule, List<Snippet>> fileCandidates = new HashMap<>();
        rules.forEach(rule -> fileCandidates.put(rule, new ArrayList<>()));

        try {
            JavaParser parser = new JavaParser();
            Optional<CompilationUnit> result = parser.parse(file).getResult();

            if (result.isPresent()) {
                CompilationUnit cu = result.get();
                JavaCodeVisitor visitor = new JavaCodeVisitor(file);
                
                // 访问 AST，visitor 会填充 fileCandidates
                cu.accept(visitor, fileCandidates);
            }
        } catch (Exception e) {
            System.err.println("WARN: Failed to parse " + file.getName() + ": " + e.getMessage());
        }
        return fileCandidates;
    }

    /**
     * 执行全量提取
     */
    public List<Snippet> extractAllCandidates() {
        List<Snippet> allCandidates = new ArrayList<>();
        List<File> javaFiles = findAllJavaFiles(sourceDir);
        
        System.err.println("INFO: Found " + javaFiles.size() + " Java files.");
        System.err.println("INFO: Total rules to check: " + rules.size());

        for (File file : javaFiles) {
            Map<Rule, List<Snippet>> fileCandidates = parseFile(file);
            // 汇总结果
            fileCandidates.values().forEach(allCandidates::addAll);
        }
        
        System.err.println("INFO: Total candidates extracted: " + allCandidates.size());
        return allCandidates;
    }
}
