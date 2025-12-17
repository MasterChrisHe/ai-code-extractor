package com.example.sonar.ai.service;

import com.example.sonar.ai.model.Rule;
import com.example.sonar.ai.model.Snippet;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
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
    }

    @AfterEach
    void tearDown() throws IOException {
        // 清理临时目录
        // 递归删除文件和目录
        Files.walk(tempDir)
             .sorted(Collections.reverseOrder())
             .map(Path::toFile)
             .forEach(File::delete);
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

        // 验证结果数量
        assertEquals(2, snippets.size(), "应该提取出 2 个方法声明");

        // 验证第一个方法（getUserNames）
        Snippet s1 = snippets.stream().filter(s -> s.getName().equals("getUserNames")).findFirst().get();
        // 关键验证：行号应该指向方法名所在行（第 8 行），而不是 @Override 所在的第 7 行。
        assertEquals(8, s1.getLine(), "getUserNames 方法的行号应指向方法签名行");
        // 验证代码片段不包含 @Override
        assertTrue(s1.getCode().startsWith("public List<String> getUserNames"));
        assertFalse(s1.getCode().contains("@Override"));

        // 验证第二个方法（getAge）
        Snippet s2 = snippets.stream().filter(s -> s.getName().equals("getAge")).findFirst().get();
        assertEquals("getAge", s2.getName());
        assertEquals(13, s2.getLine(), "getAge 方法的行号应指向方法签名行");
        assertEquals("private int getAge()", s2.getCode());
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
        // 关键验证：行号应指向类名所在行（第 5 行），而不是 @Deprecated 所在的第 4 行
        assertEquals(5, c1.getLine(), "MyClass 的行号应指向类名行");
        assertEquals("public class MyClass", c1.getCode());

        // 验证第二个类 (AnotherClass)
        Snippet c2 = snippets.stream().filter(s -> s.getName().equals("AnotherClass")).findFirst().get();
        assertEquals("AnotherClass", c2.getName());
        assertEquals(9, c2.getLine(), "AnotherClass 的行号应指向类名行");
        // 默认修饰符，不打印 private/public/protected
        assertEquals("class AnotherClass", c2.getCode()); 
    }
}
