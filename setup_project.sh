#!/bin/bash

# 定义基础目录
BASE_DIR="src/main/java/com/example/sonar/ai"

# 创建包目录结构
mkdir -p "$BASE_DIR/model"
mkdir -p "$BASE_DIR/io"
mkdir -p "$BASE_DIR/parser"
mkdir -p "$BASE_DIR/service"

echo "正在生成 Java 项目结构..."

# ==========================================
# 1. Model 包 (Rule.java, Snippet.java)
# ==========================================

cat > "$BASE_DIR/model/Rule.java" << 'EOF'
package com.example.sonar.ai.model;

/**
 * 规则实体类
 * 对应 CSV 文件中的一行规则配置
 */
public class Rule {
    private String id;
    private String scope; // e.g., CLASS_DECLARATION, METHOD_DECLARATION
    private String description;
    private String criteria;
    private String context;

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCriteria() { return criteria; }
    public void setCriteria(String criteria) { this.criteria = criteria; }

    public String getContext() { return context; }
    public void setContext(String context) { this.context = context; }
}
EOF

cat > "$BASE_DIR/model/Snippet.java" << 'EOF'
package com.example.sonar.ai.model;

import java.io.File;

/**
 * 代码片段实体类
 * 用于存储从源码中提取出的、待 AI 分析的代码块信息
 */
public class Snippet {
    private String ruleId;
    private String ruleDesc;
    private String ruleCriteria;
    private String ruleContext;
    private String file;
    private int line;
    private String code;
    private String name;

    public Snippet(Rule rule, File file, int line, String code, String name) {
        this.ruleId = rule.getId();
        this.ruleDesc = rule.getDescription();
        this.ruleCriteria = rule.getCriteria();
        this.ruleContext = rule.getContext();
        this.file = file.getAbsolutePath();
        this.line = line;
        this.code = code;
        this.name = name;
    }
}
EOF

# ==========================================
# 2. IO 包 (RuleReader.java)
# ==========================================

cat > "$BASE_DIR/io/RuleReader.java" << 'EOF'
package com.example.sonar.ai.io;

import com.example.sonar.ai.model.Rule;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 负责读取规则文件 (CSV)
 */
public class RuleReader {

    /**
     * 从 CSV 文件读取规则列表
     * @param rulesFile CSV 文件路径
     * @return 规则列表
     */
    public List<Rule> readRules(String rulesFile) {
        List<Rule> rules = new ArrayList<>();
        System.err.println("INFO: Reading rules from: " + rulesFile);
        
        try (BufferedReader br = new BufferedReader(new FileReader(rulesFile))) {
            String line;
            boolean headerSkipped = false;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                
                // 跳过标题行
                if (!headerSkipped) {
                    headerSkipped = true;
                    continue;
                }

                // 解析 CSV: RuleID, Scope, Description, Criteria, Context
                String[] parts = line.split(",", 5);
                if (parts.length >= 5) {
                    Rule rule = new Rule();
                    rule.setId(parts[0].trim());
                    rule.setScope(parts[1].trim());
                    rule.setDescription(parts[2].trim());
                    rule.setCriteria(parts[3].trim());
                    rule.setContext(parts[4].trim());
                    rules.add(rule);
                }
            }
        } catch (IOException e) {
            System.err.println("ERROR: Failed to read rules file: " + e.getMessage());
            System.exit(1);
        }
        return rules;
    }
}
EOF

# ==========================================
# 3. Parser 包 (JavaCodeVisitor.java) - 核心修复位置
# ==========================================

cat > "$BASE_DIR/parser/JavaCodeVisitor.java" << 'EOF'
package com.example.sonar.ai.parser;

import com.example.sonar.ai.model.Rule;
import com.example.sonar.ai.model.Snippet;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration; // 引入 EnumDeclaration
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * AST 访问器
 * 遍历 Java 语法树并根据规则提取节点
 * 修复了 ClassOrInterfaceDeclaration 中错误的 isEnum() 调用。
 */
public class JavaCodeVisitor extends VoidVisitorAdapter<Map<Rule, List<Snippet>>> {

    private final File currentFile;

    public JavaCodeVisitor(File file) {
        this.currentFile = file;
    }

    /**
     * 辅助方法：添加 Snippet 到收集器
     */
    private void addSnippet(String scope, String name, int line, String code, Map<Rule, List<Snippet>> collector) {
        collector.forEach((rule, snippets) -> {
            if (scope.equalsIgnoreCase(rule.getScope())) {
                snippets.add(new Snippet(rule, currentFile, line, code, name));
            }
        });
    }

    // --- 访问者方法重写 ---
    
    /**
     * 访问类和接口声明
     */
    @Override
    public void visit(ClassOrInterfaceDeclaration n, Map<Rule, List<Snippet>> collector) {
        super.visit(n, collector);
        
        // 1. 获取行号：优先使用类名所在的行，而非整个 Class 块的起始行（避免 @Annotation）
        int line = n.getName().getBegin().map(p -> p.line).orElse(
                   n.getBegin().map(p -> p.line).orElse(1));

        // 2. 构造代码片段：手动拼接签名，避免包含过长的方法体或无关注解
        StringBuilder sb = new StringBuilder();
        if (n.isPublic()) sb.append("public ");
        else if (n.isPrivate()) sb.append("private ");
        else if (n.isProtected()) sb.append("protected ");
        
        if (n.isStatic()) sb.append("static ");
        if (n.isInterface()) sb.append("interface ");
        // ClassOrInterfaceDeclaration 只能是 class 或 interface
        else sb.append("class "); 
        
        sb.append(n.getNameAsString());

        addSnippet("CLASS_DECLARATION", n.getNameAsString(), line, sb.toString(), collector);
    }
    
    /**
     * 访问枚举声明
     */
    @Override
    public void visit(EnumDeclaration n, Map<Rule, List<Snippet>> collector) {
        super.visit(n, collector);
        
        int line = n.getName().getBegin().map(p -> p.line).orElse(
                   n.getBegin().map(p -> p.line).orElse(1));

        StringBuilder sb = new StringBuilder();
        if (n.isPublic()) sb.append("public ");
        else if (n.isPrivate()) sb.append("private ");
        else if (n.isProtected()) sb.append("protected ");
        
        if (n.isStatic()) sb.append("static ");
        sb.append("enum "); 
        sb.append(n.getNameAsString());
        
        addSnippet("CLASS_DECLARATION", n.getNameAsString(), line, sb.toString(), collector);
    }

    /**
     * 访问方法声明
     */
    @Override
    public void visit(MethodDeclaration n, Map<Rule, List<Snippet>> collector) {
        super.visit(n, collector);
        
        // 1. 获取行号：精确获取方法名所在的行号（修复了注解导致行号不准确的问题）
        // 例如：@Override 在第 10 行，public void define() 在第 11 行，我们取第 11 行
        int line = n.getName().getBegin().map(p -> p.line).orElse(
                   n.getBegin().map(p -> p.line).orElse(1));

        // 2. 获取代码片段：使用 getDeclarationAsString() 
        // 该方法默认不包含注解 (excluding annotations)，只包含修饰符、返回值、名称和参数
        // 输出示例: "public void define(Context context)"
        String cleanCode = n.getDeclarationAsString(true, true, true);

        addSnippet("METHOD_DECLARATION", n.getNameAsString(), line, cleanCode, collector);
    }
}
EOF

# ==========================================
# 4. Service 包 (CodeExtractorService.java)
# ==========================================

cat > "$BASE_DIR/service/CodeExtractorService.java" << 'EOF'
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

    private Map<Rule, List<Snippet>> parseFile(File file) {
        Map<Rule, List<Snippet>> fileCandidates = new HashMap<>();
        rules.forEach(rule -> fileCandidates.put(rule, new ArrayList<>()));

        try {
            JavaParser parser = new JavaParser();
            Optional<CompilationUnit> result = parser.parse(file).getResult();

            if (result.isPresent()) {
                CompilationUnit cu = result.get();
                JavaCodeVisitor visitor = new JavaCodeVisitor(file);
                cu.accept(visitor, fileCandidates);
            }
        } catch (Exception e) {
            System.err.println("WARN: Failed to parse " + file.getName() + ": " + e.getMessage());
        }
        return fileCandidates;
    }

    public List<Snippet> extractAllCandidates() {
        List<Snippet> allCandidates = new ArrayList<>();
        List<File> javaFiles = findAllJavaFiles(sourceDir);
        
        System.err.println("INFO: Found " + javaFiles.size() + " Java files.");
        System.err.println("INFO: Total rules to check: " + rules.size());

        for (File file : javaFiles) {
            Map<Rule, List<Snippet>> fileCandidates = parseFile(file);
            fileCandidates.values().forEach(allCandidates::addAll);
        }
        
        System.err.println("INFO: Total candidates extracted: " + allCandidates.size());
        return allCandidates;
    }
}
EOF

# ==========================================
# 5. Main 入口 (CodeAnalysisEngine.java)
# ==========================================

cat > "$BASE_DIR/CodeAnalysisEngine.java" << 'EOF'
package com.example.sonar.ai;

import com.example.sonar.ai.io.RuleReader;
import com.example.sonar.ai.model.Rule;
import com.example.sonar.ai.model.Snippet;
import com.example.sonar.ai.service.CodeExtractorService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.List;

/**
 * 主程序入口
 */
public class CodeAnalysisEngine {

    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Usage: java -jar ai-code-extractor.jar <sourceDir> <rulesFile>");
            System.exit(1);
        }

        String sourceDir = args[0];
        String rulesFile = args[1];

        System.err.println("INFO: Starting Code Analysis Engine...");
        System.err.println("INFO: Source Dir: " + sourceDir);
        System.err.println("INFO: Rules File: " + rulesFile);

        try {
            RuleReader reader = new RuleReader();
            List<Rule> rules = reader.readRules(rulesFile);

            if (rules.isEmpty()) {
                System.err.println("WARN: No rules found or file is empty. Exiting.");
                System.exit(0);
            }

            CodeExtractorService service = new CodeExtractorService(sourceDir, rules);
            List<Snippet> allCandidates = service.extractAllCandidates();

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            // 注意：这是程序与外部 Groovy 脚本通信的唯一输出
            System.out.println(gson.toJson(allCandidates));

            System.err.println("INFO: Extraction complete. JSON output finished.");

        } catch (Exception e) {
            System.err.println("FATAL ERROR during extraction: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
EOF

# ==========================================
# 6. 生成 pom.xml
# ==========================================

cat > "pom.xml" << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example.sonar.ai</groupId>
    <artifactId>ai-code-extractor</artifactId>
    <version>1.0.0</version>

    <properties>
        <maven.compiler.source>11</maven.compiler.source>
        <maven.compiler.target>11</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <dependencies>
        <dependency>
            <groupId>com.github.javaparser</groupId>
            <artifactId>javaparser-core</artifactId>
            <version>3.25.10</version>
        </dependency>
        <dependency>
            <groupId>com.google.code.gson</groupId>
            <artifactId>gson</artifactId>
            <version>2.10.1</version>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-shade-plugin</artifactId>
                <version>3.4.1</version>
                <executions>
                    <execution>
                        <phase>package</phase>
                        <goals>
                            <goal>shade</goal>
                        </goals>
                        <configuration>
                            <transformers>
                                <transformer implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
                                    <mainClass>com.example.sonar.ai.CodeAnalysisEngine</mainClass>
                                </transformer>
                            </transformers>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
EOF

echo "Java 工程生成完毕！"
echo "请运行 'mvn clean package' 进行编译，生成的 JAR 包位于 target/ai-code-extractor-1.0.0.jar"