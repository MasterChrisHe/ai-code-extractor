package com.example.sonar.ai.service;

import com.example.sonar.ai.io.RuleReader;
import com.example.sonar.ai.model.Rule;
import com.example.sonar.ai.model.Snippet;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

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
                System.exit(0);
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

}
