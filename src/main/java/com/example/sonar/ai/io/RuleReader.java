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
