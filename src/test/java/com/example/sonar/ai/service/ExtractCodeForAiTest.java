package com.example.sonar.ai.service;

import com.example.sonar.ai.io.RuleReader;
import com.example.sonar.ai.model.Rule;
import com.example.sonar.ai.model.Snippet;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ExtractCodeForAiTest {
    private List<Rule> testRules= new ArrayList<>();
    private final String testFilePath="E:\\softworkspace\\ai-code-extractor\\src\\main\\resources\\TestJavaCode.java";
    private final String testRuleFilePath="E:\\softworkspace\\ai-code-extractor\\src\\main\\resources\\rule-test.csv";

    @BeforeEach
    void setUp() throws IOException {
        // 创建一个临时目录作为源码根目录
        RuleReader reader = new RuleReader();
        testRules = reader.readRules(testRuleFilePath);

        if (testRules.isEmpty()) {
            System.err.println("WARN: No rules found or file is empty. Exiting.");
            System.exit(0);
        }
    }

    /**
     * 抽取每个方法的javaDOc
     * @throws Exception
     */
    @Test
    void extractMethodJavaDocForAi() throws Exception {
        CodeExtractorService service = new CodeExtractorService(testFilePath, testRules);
        List<Snippet> allCandidates = service.extractAllCandidates();

        Gson gson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
        String json = gson.toJson(allCandidates);
        System.out.println(json);

        //写入json文件
        service.writeJsonToFile(json);
        System.out.println("INFO: write json to json file completed");
    }
}
