package com.example.sonar.ai;

import com.example.sonar.ai.io.RuleReader;
import com.example.sonar.ai.model.Rule;
import com.example.sonar.ai.model.Snippet;
import com.example.sonar.ai.service.CodeExtractorService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * 主程序入口
 */
public class CodeAnalysisEngine {

    public static void main(String[] args) {
//        if (args.length != 2) {
//            System.err.println("Usage: java -jar ai-code-extractor.jar <sourceDir> <rulesFile>");
//            System.exit(1);
//        }
//
//        String sourceDir = args[0];
//        String rulesFile = args[1];


        String sourceDir = "E:\\softworkspace\\ai-code-extractor\\src\\main\\resources\\TestJavaCode.java";
        String rulesFile = "E:\\softworkspace\\ai-code-extractor\\src\\main\\resources\\rule-test.csv";

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
            String json = gson.toJson(allCandidates);
            System.out.println("json::" + json);
            System.err.println("INFO: Extraction complete. JSON output finished.");

            //输出字符串到json文件，与jar包同级
            service.writeJsonToFile(json);
            System.out.println("INFO: write json to json file completed");
        } catch (Exception e) {
            System.err.println("FATAL ERROR during extraction: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
