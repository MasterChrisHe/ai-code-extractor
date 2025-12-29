package com.example.sonar.ai.service;

import com.example.sonar.ai.model.Rule;
import com.example.sonar.ai.model.Snippet;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CodeExtractorService 的集成测试。
 * 核心目标：验证 AST 解析器 (JavaCodeVisitor) 能否正确提取代码片段，
 * 尤其是能否精确地将行号定位到方法名/类名所在行 (而非注解行)。
 */
public class CodeExtractorServiceTest {

    private Path tempDir;
    private List<Rule> testRules;
    private Rule methodRule;

    // 用于 System.out 重定向
    private final PrintStream originalOut = System.out;
    private ByteArrayOutputStream outputStreamCaptor;

    @BeforeEach
    void setUp() throws IOException {
        // 创建一个临时目录作为源码根目录
        tempDir = Files.createTempDirectory("test-source");

        // 模拟规则：只关注方法声明
        methodRule = new Rule();
        methodRule.setId("M001");
        methodRule.setScope("METHOD_DECLARATION");
        methodRule.setDescription("Method Name Check");
        methodRule.setCriteria("Method name must follow camelCase.");
        methodRule.setContext("Context");
        testRules = Collections.singletonList(methodRule);

        // 重定向 System.out，以捕获 JSON 输出
        outputStreamCaptor = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStreamCaptor));
    }

    @AfterEach
    void tearDown() throws IOException {
        // 清理临时目录
        // 递归删除文件和目录
        Files.walk(tempDir)
                .sorted(Collections.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);

        // 恢复 System.out
        System.setOut(originalOut);
    }

    // 辅助方法：创建测试 Java 文件
    private File createTestFile(String name, String content) throws IOException {
        Path filePath = tempDir.resolve(name);
        Files.writeString(filePath, content);
        return filePath.toFile();
    }

    @Test
    void testMethodDeclarationExtraction_withAnnotation() throws IOException {
        // 模拟一个包含注解的方法，确保行号正确
        String code = """
                package com.test;

                import java.util.List; // Line 3

                public class UserService { // Line 5

                    @Override // Line 7
                    public List<String> getUserNames(String filter) { // Line 8
                        // 方法体
                        return List.of("A", "B");
                    }

                    private int getAge() { // Line 13
                        return 30;
                    }
                }
                """;

        createTestFile("UserService.java", code);

        CodeExtractorService service = new CodeExtractorService(tempDir.toString(), testRules);
        List<Snippet> snippets = service.extractAllCandidates();

        // 验证结果数量 (这是纯粹的单元逻辑验证)
        assertEquals(2, snippets.size(), "应该提取出 2 个方法声明");

        // 验证第一个方法（getUserNames）
        Snippet s1 = snippets.stream().filter(s -> s.getName().equals("getUserNames")).findFirst().get();
        assertEquals(8, s1.getLine(), "getUserNames 方法的行号应指向方法签名行");
        assertTrue(s1.getCode().startsWith("public List<String> getUserNames"));
    }

    @Test
    void testClassDeclarationExtraction() throws IOException {
        // 改变规则，测试类提取
        Rule classRule = new Rule();
        classRule.setScope("CLASS_DECLARATION");
        // 替换规则列表
        testRules = Collections.singletonList(classRule);

        String code = """
                package com.test; // Line 1

                /** JavaDoc */ // Line 3
                @Deprecated // Line 4
                public class MyClass { // Line 5

                }
                // comment // Line 8
                final class AnotherClass { // Line 9

                }
                """;

        createTestFile("MyClass.java", code);

        CodeExtractorService service = new CodeExtractorService(tempDir.toString(), testRules);
        List<Snippet> snippets = service.extractAllCandidates();

        assertEquals(2, snippets.size(), "应该提取出 2 个类声明");

        // 验证第一个类 (MyClass)
        Snippet c1 = snippets.stream().filter(s -> s.getName().equals("MyClass")).findFirst().get();
        assertEquals("MyClass", c1.getName());
        assertEquals(5, c1.getLine(), "MyClass 的行号应指向类名行");
        assertEquals("public class MyClass", c1.getCode());
    }

    @Test
    void testMethodJavadocExtraction() throws IOException {
        // 配置 Javadoc 规则
        Rule javadocRule = new Rule();
        javadocRule.setScope("JAVADOC");
        testRules = Collections.singletonList(javadocRule);

        String code = """
                package com.test;

                public class PaymentService {

                    /**
                     * Processes payment.
                     * This explanation should be extracted.
                     * @param amount The extraction amount
                     */
                    public void pay(double amount) { // Line 7 (Javadoc starts at 4)

                    }

                    /**
                     *
                     */
                     public void skip() {} // Empty Javadoc should be skipped
                }
                """;

        createTestFile("PaymentService.java", code);

        CodeExtractorService service = new CodeExtractorService(tempDir.toString(), testRules);
        List<Snippet> snippets = service.extractAllCandidates();

        assertEquals(1, snippets.size(), "Should extract 1 Javadoc with summary");

        Snippet s = snippets.get(0);
        assertEquals("pay", s.getName());
        assertTrue(s.getCode().contains("Processes payment.\nThis explanation should be extracted."),
                "Should contain description");
        assertFalse(s.getCode().contains("@param"), "Should exclude tags");
        assertTrue(s.getCode().contains("public void pay(double amount)"), "Should contain signature");

        // Javadoc start line is 5, but Summary "Processes payment." is on line 6
        assertEquals(6, s.getLine(), "Should point to Javadoc Summary line");
    }

    @Test
    void testMethodJavadocExtraction_withAccessModifiers() throws IOException {
        // 验证只提取 public 和 protected 方法的 javadoc，不提取 private 和 package-private 方法
        Rule javadocRule = new Rule();
        javadocRule.setScope("JAVADOC");
        testRules = Collections.singletonList(javadocRule);

        String code = """
                package com.test;

                public class AccessModifierTest {

                    /**
                     * Public method javadoc - should be extracted
                     */
                    public void publicMethod() {
                    }

                    /**
                     * Protected method javadoc - should be extracted
                     */
                    protected void protectedMethod() {
                    }

                    /**
                     * Private method javadoc - should NOT be extracted
                     */
                    private void privateMethod() {
                    }

                    /**
                     * Package-private method javadoc - should NOT be extracted
                     */
                    void packagePrivateMethod() {
                    }
                }
                """;

        createTestFile("AccessModifierTest.java", code);

        CodeExtractorService service = new CodeExtractorService(tempDir.toString(), testRules);
        List<Snippet> snippets = service.extractAllCandidates();

        // Should only extract 2 javadocs: public and protected methods
        assertEquals(2, snippets.size(), "Should extract only public and protected method javadocs");

        // Verify public method javadoc is extracted
        assertTrue(snippets.stream().anyMatch(s -> s.getName().equals("publicMethod")),
                "Should extract public method javadoc");

        // Verify protected method javadoc is extracted
        assertTrue(snippets.stream().anyMatch(s -> s.getName().equals("protectedMethod")),
                "Should extract protected method javadoc");

        // Verify private method javadoc is NOT extracted
        assertFalse(snippets.stream().anyMatch(s -> s.getName().equals("privateMethod")),
                "Should NOT extract private method javadoc");

        // Verify package-private method javadoc is NOT extracted
        assertFalse(snippets.stream().anyMatch(s -> s.getName().equals("packagePrivateMethod")),
                "Should NOT extract package-private method javadoc");
    }

    @Test
    void testClassJavadocExtraction() throws IOException {
        Rule javadocRule = new Rule();
        javadocRule.setScope("JAVADOC");
        testRules = Collections.singletonList(javadocRule);

        String code = """
                package com.test;

                /**
                 * Core utility class.
                 * @author Admin
                 */
                public class Utils { // Line 5 (Javadoc starts at 3)
                }
                """;

        createTestFile("Utils.java", code);

        CodeExtractorService service = new CodeExtractorService(tempDir.toString(), testRules);
        List<Snippet> snippets = service.extractAllCandidates();

        assertEquals(1, snippets.size());
        Snippet s = snippets.get(0);
        assertEquals("Utils", s.getName());
        assertTrue(s.getCode().contains("Core utility class."), "Should contain description");
        assertTrue(s.getCode().contains("public class Utils"), "Should contain signature");
        // Javadoc start line 3, Summary is line 4
        assertEquals(4, s.getLine());
    }

    /**
     * 测试 JSON 输出的格式和内容，模拟 CodeAnalysisEngine.main 的输出行为。
     */
    @Test
    void testJsonOutputFormat() throws IOException {
        // 模拟源码
        String code = """
                package com.test;

                public class Demo { // Line 3

                    @Override // Line 5
                    public void run() { // Line 6
                        // content
                    }
                }
                """;

        createTestFile("Demo.java", code);

        CodeExtractorService service = new CodeExtractorService(tempDir.toString(), testRules);
        List<Snippet> allCandidates = service.extractAllCandidates();

        // -------------------------------------------------------------
        // 核心步骤：模拟 CodeAnalysisEngine 中的 JSON 输出
        // -------------------------------------------------------------
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        // 将 JSON 打印到重定向的 System.out 中
        System.out.println(gson.toJson(allCandidates));
        // -------------------------------------------------------------

        String jsonOutput = outputStreamCaptor.toString().trim();

        // 1. 验证 JSON 格式是否正确
        assertFalse(jsonOutput.isEmpty(), "JSON 输出不应为空");
        assertTrue(jsonOutput.startsWith("["), "JSON 输出应以数组开始");
        assertTrue(jsonOutput.endsWith("]"), "JSON 输出应以数组结束");

        // 2. 验证关键数据点是否包含在 JSON 中 (例如，run 方法在第 6 行)
        assertTrue(jsonOutput.contains("\"name\": \"run\""), "JSON 应包含 'run' 方法名");
        assertTrue(jsonOutput.contains("\"line\": 6"), "JSON 应包含精确的行号 6");
        assertTrue(jsonOutput.contains("\"code\": \"public void run()\""), "JSON 应包含干净的代码片段");

        // 将捕获到的 JSON 输出到原始 System.out，让用户在控制台看到
        originalOut.println("--- 模拟的 JSON 输出 ---");
        originalOut.println(jsonOutput);

    }

    @Test
    void testClassJavadocExtraction_ScenarioUserReported() throws IOException {
        Rule javadocRule = new Rule();
        javadocRule.setScope("JAVADOC");
        testRules = Collections.singletonList(javadocRule);

        String code = """
                package com.test;

                /**
                 * ceshilei
                 *
                 * @author chris
                 * @date systemdate
                 */
                public class CeShiLei {

                    /**
                     * pencil
                     */
                    public void pencil() {
                        System.out.println("Pencil");
                    }
                }
                """;

        createTestFile("CeShiLei.java", code);

        CodeExtractorService service = new CodeExtractorService(tempDir.toString(), testRules);
        List<Snippet> snippets = service.extractAllCandidates();

        // With unified JAVADOC scope, both Class and Method Javadocs are extracted.
        // 1. Class Javadoc ("ceshilei")
        // 2. Method Javadoc ("pencil")
        assertEquals(2, snippets.size(), "Should extract Class and Method Javadocs");

        Snippet s = snippets.stream()
                .filter(snip -> snip.getCode().contains("ceshilei"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Class Javadoc not found"));

        System.out.println("Extracted Content: " + s.getCode());

        assertTrue(s.getCode().contains("ceshilei"), "Should contain summary 'ceshilei'");
        assertTrue(s.getCode().contains("public class CeShiLei"), "Should contain class signature");
    }
}