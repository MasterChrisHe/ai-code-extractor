package com.example.sonar.ai.service;

import com.example.sonar.ai.io.RuleReader;
import com.example.sonar.ai.model.Rule;
import com.example.sonar.ai.model.Snippet;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class RuleTest {

    @Test
    void test1() throws IOException {
        String sourceDir = "E:\\softworkspace\\ai-code-extractor\\src\\main\\resources\\TestController.java";
        String rulesFile = "E:\\softworkspace\\ai-code-extractor\\src\\main\\resources\\rules-test.yaml";

        System.err.println("INFO: Starting Code Analysis Engine...");
        System.err.println("INFO: Source Dir: " + sourceDir);
        System.err.println("INFO: Rules File: " + rulesFile);

        try {
            RuleReader reader = new RuleReader();
            List<Rule> rules = reader.readRules(rulesFile);

            if (rules.isEmpty()) {
                System.err.println("WARN: No rules found or file is empty. Exiting.");
                return;
            }

            CodeExtractorService service = new CodeExtractorService(sourceDir, rules);
            List<Snippet> allCandidates = service.extractAllCandidates();

            Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
            // 注意：这是程序与外部 Groovy 脚本通信的唯一输出
            System.out.println(gson.toJson(allCandidates));

            System.err.println("INFO: Extraction complete. JSON output finished.");

        } catch (Exception e) {
            System.err.println("FATAL ERROR during extraction: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    @Test
    void testNaming() throws IOException {
        String sourceDir = "src/test/resources/TestNaming.java";
        String rulesFile = "src/test/resources/rules-naming.yaml";

        System.err.println("INFO: Starting Naming Extraction Test...");

        RuleReader reader = new RuleReader();
        List<Rule> rules = reader.readRules(rulesFile);

        CodeExtractorService service = new CodeExtractorService(sourceDir, rules);
        List<Snippet> allCandidates = service.extractAllCandidates();

        Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
        System.out.println(gson.toJson(allCandidates));

        // Basic verification
        assert !allCandidates.isEmpty();
    }

    @Test
    void testJavadocExtraction() throws IOException {
        String sourceDir = "src/test/resources/TestJavadoc.java";
        String rulesFile = "src/test/resources/rules-javadoc.yaml";

        System.err.println("INFO: Starting Javadoc Extraction Test...");

        RuleReader reader = new RuleReader();
        List<Rule> rules = reader.readRules(rulesFile);

        CodeExtractorService service = new CodeExtractorService(sourceDir, rules);
        List<Snippet> candidates = service.extractAllCandidates();

        Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
        System.out.println(gson.toJson(candidates));

        // 1. Verify exact count: Class Javadoc + Public Method + Protected Method = 3
        assertEquals(3, candidates.size(), "Should extract exactly 3 snippets (Class, Public, Protected)");

        // 2. Verify Class Javadoc
        Snippet classSnippet = candidates.stream()
                .filter(s -> s.getName().equals("TestJavadoc"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Class Javadoc not found"));

        assertTrue(classSnippet.getCode().contains("Class Javadoc."),
                "Class content should contain description");
        assertTrue(classSnippet.getCode().contains("public class TestJavadoc"),
                "Class content should contain signature");
        // Line number check: historical strategy points to summary line (line 4)
        assertEquals(4, classSnippet.getLine(), "Class Javadoc line number incorrect");

        // 3. Verify Public Method Javadoc
        Snippet publicMethod = candidates.stream()
                .filter(s -> s.getName().equals("publicMethod"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Public Method Javadoc not found"));

        assertTrue(publicMethod.getCode().contains("Public method Javadoc."),
                "Public method content should contain description");
        assertTrue(publicMethod.getCode().contains("public void publicMethod(int a)"),
                "Public method content should contain signature");
        // Line number check: historical strategy points to summary line (line 10)
        assertEquals(10, publicMethod.getLine(), "Public Method Javadoc line number incorrect");

        // 4. Verify Protected Method Javadoc
        Snippet protectedMethod = candidates.stream()
                .filter(s -> s.getName().equals("protectedMethod"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Protected Method Javadoc not found"));

        assertTrue(protectedMethod.getCode().contains("Protected method Javadoc."),
                "Protected method content should contain description");

        // 5. Verify Private/Package-Private NOT extracted
        boolean hasPrivate = candidates.stream().anyMatch(s -> s.getName().equals("privateMethod"));
        assertFalse(hasPrivate, "Should NOT extract private method javadoc");

        boolean hasPackage = candidates.stream().anyMatch(s -> s.getName().equals("packagePrivateMethod"));
        assertFalse(hasPackage, "Should NOT extract package-private method javadoc");
    }

    /**
     * 测试 RULE-025: 主动抛出的异常必须要填写详细的描述信息
     * 验证各种常见异常抛出方式的提取效果
     */
    @Test
    void testThrowExceptionExtraction() throws IOException {
        String sourceDir = "src/test/resources/TestThrowException.java";
        String rulesFile = "src/test/resources/rules-throw.yaml";

        System.err.println("INFO: Starting Throw Exception Extraction Test (RULE-025)...");

        RuleReader reader = new RuleReader();
        List<Rule> rules = reader.readRules(rulesFile);

        assertEquals(1, rules.size(), "Should have exactly 1 rule (RULE-025)");
        assertEquals("RULE-025", rules.get(0).getId(), "Rule ID should be RULE-025");

        CodeExtractorService service = new CodeExtractorService(sourceDir, rules);
        List<Snippet> candidates = service.extractAllCandidates();

        Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
        System.out.println(gson.toJson(candidates));

        // 验证提取结果不为空
        assertFalse(candidates.isEmpty(), "Should extract at least one throw statement");

        // 验证一些具体的提取结果
        // 1. 检查是否提取到了带消息的异常
        boolean hasMessageException = candidates.stream()
                .anyMatch(s -> s.getName() != null && s.getName().contains("用户不存在"));
        assertTrue(hasMessageException, "Should extract exception with '用户不存在' message");

        // 2. 检查所有提取的 snippet 都有正确的 scope
        boolean allHaveCorrectScope = candidates.stream()
                .allMatch(s -> "THROW_DECLARATION".equals(s.getScope()));
        assertTrue(allHaveCorrectScope, "All snippets should have THROW_DECLARATION scope");
    }

}
