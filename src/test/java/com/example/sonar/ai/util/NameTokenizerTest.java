package com.example.sonar.ai.util;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * NameTokenizer 单元测试
 * 测试各种命名风格的拆分规则
 */
public class NameTokenizerTest {

    @Test
    void testCamelCase() {
        assertEquals(Arrays.asList("get", "user", "info"), NameTokenizer.tokenize("getUserInfo"));
        assertEquals(Arrays.asList("is", "valid"), NameTokenizer.tokenize("isValid"));
        assertEquals(Arrays.asList("calculate", "sum"), NameTokenizer.tokenize("calculateSum"));
    }

    @Test
    void testPascalCase() {
        assertEquals(Arrays.asList("user", "service"), NameTokenizer.tokenize("UserService"));
        assertEquals(Arrays.asList("test", "naming"), NameTokenizer.tokenize("TestNaming"));
        assertEquals(Arrays.asList("order", "controller"), NameTokenizer.tokenize("OrderController"));
    }

    @Test
    void testUpperCaseWithUnderscores() {
        assertEquals(Arrays.asList("max", "value"), NameTokenizer.tokenize("MAX_VALUE"));
        assertEquals(Arrays.asList("expired", "time"), NameTokenizer.tokenize("EXPIRED_TIME"));
        assertEquals(Arrays.asList("default", "timeout"), NameTokenizer.tokenize("DEFAULT_TIMEOUT"));
    }

    @Test
    void testSnakeCase() {
        assertEquals(Arrays.asList("user", "name"), NameTokenizer.tokenize("user_name"));
        assertEquals(Arrays.asList("user", "config"), NameTokenizer.tokenize("user_config"));
        assertEquals(Arrays.asList("order", "id"), NameTokenizer.tokenize("order_id"));
    }

    @Test
    void testConsecutiveUppercase() {
        // HTTP 作为一个整体 token
        assertEquals(Arrays.asList("get", "http", "status"), NameTokenizer.tokenize("getHTTPStatus"));
        assertEquals(Arrays.asList("parse", "xml", "file"), NameTokenizer.tokenize("parseXMLFile"));
        assertEquals(Arrays.asList("load", "json", "data"), NameTokenizer.tokenize("loadJSONData"));
        assertEquals(Arrays.asList("http", "client"), NameTokenizer.tokenize("HTTPClient"));
    }

    @Test
    void testNumbersAsSeparators() {
        // 数字仅作为分隔符，不输出为 token
        assertEquals(Arrays.asList("user", "config"), NameTokenizer.tokenize("user2Config"));
        assertEquals(Arrays.asList("config", "v"), NameTokenizer.tokenize("configV2"));
        assertEquals(Arrays.asList("test", "case"), NameTokenizer.tokenize("test123Case"));
    }

    @Test
    void testMixedCases() {
        // 混合多种命名风格
        assertEquals(Arrays.asList("get", "http", "status", "v"), NameTokenizer.tokenize("getHTTPStatusV2"));
        assertEquals(Arrays.asList("user", "pinyin", "name"), NameTokenizer.tokenize("userPinyinName"));
        assertEquals(Arrays.asList("parse", "html", "content"), NameTokenizer.tokenize("parseHTMLContent"));
    }

    @Test
    void testSingleWord() {
        assertEquals(Arrays.asList("user"), NameTokenizer.tokenize("user"));
        assertEquals(Arrays.asList("name"), NameTokenizer.tokenize("Name"));
        assertEquals(Arrays.asList("id"), NameTokenizer.tokenize("ID"));
    }

    @Test
    void testEdgeCases() {
        // 空字符串和 null
        assertTrue(NameTokenizer.tokenize("").isEmpty());
        assertTrue(NameTokenizer.tokenize(null).isEmpty());

        // 仅包含下划线或数字
        assertTrue(NameTokenizer.tokenize("___").isEmpty());
        assertTrue(NameTokenizer.tokenize("123").isEmpty());
        assertTrue(NameTokenizer.tokenize("_123_").isEmpty());
    }

    @Test
    void testFormatTokens() {
        assertEquals("[]", NameTokenizer.formatTokens(List.of()));
        assertEquals("[user]", NameTokenizer.formatTokens(List.of("user")));
        assertEquals("[user, name]", NameTokenizer.formatTokens(List.of("user", "name")));
        assertEquals("[get, user, info]", NameTokenizer.formatTokens(List.of("get", "user", "info")));
    }

    @Test
    void testLowercaseConversion() {
        // 所有 token 都应该转换为小写
        List<String> tokens = NameTokenizer.tokenize("getUserInfo");
        tokens.forEach(token -> assertEquals(token, token.toLowerCase(), "All tokens should be lowercase"));

        tokens = NameTokenizer.tokenize("HTTPClient");
        tokens.forEach(token -> assertEquals(token, token.toLowerCase(), "All tokens should be lowercase"));
    }
}
