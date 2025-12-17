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
