package com.example.sonar.ai.io;

import com.example.sonar.ai.model.Rule;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 负责读取规则文件 (YAML)
 */
public class RuleReader {

    /**
     * 从 YAML 文件读取规则列表
     * 
     * @param rulesFile YAML 文件路径
     * @return 规则列表
     */
    public List<Rule> readRules(String rulesFile) {
        List<Rule> rules = new ArrayList<>();
        System.err.println("INFO: Reading rules from: " + rulesFile);

        try (InputStream inputStream = new FileInputStream(rulesFile)) {
            Yaml yaml = new Yaml();
            Object loaded = yaml.load(inputStream);

            if (loaded instanceof List) {
                // Top-level list (New Schema)
                @SuppressWarnings("unchecked")
                List<Map<String, String>> rulesList = (List<Map<String, String>>) loaded;
                for (Map<String, String> ruleMap : rulesList) {
                    Rule rule = new Rule();
                    rule.setId(ruleMap.getOrDefault("ruleId", ruleMap.get("id")));
                    rule.setScope(ruleMap.get("scope"));
                    rule.setDescription(ruleMap.getOrDefault("ruleDesc", ruleMap.get("description")));
                    rule.setCriteria(ruleMap.getOrDefault("ruleCriteria", ruleMap.get("criteria")));
                    rule.setContext(ruleMap.getOrDefault("ruleContext", ruleMap.get("context")));
                    rules.add(rule);
                }
            } else if (loaded instanceof Map) {
                // Nested 'rules' key (Legacy/Previous Schema)
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) loaded;
                if (data != null && data.containsKey("rules")) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, String>> rulesList = (List<Map<String, String>>) data.get("rules");
                    for (Map<String, String> ruleMap : rulesList) {
                        Rule rule = new Rule();
                        rule.setId(ruleMap.getOrDefault("ruleId", ruleMap.get("id")));
                        rule.setScope(ruleMap.get("scope"));
                        rule.setDescription(ruleMap.getOrDefault("ruleDesc", ruleMap.get("description")));
                        rule.setCriteria(ruleMap.getOrDefault("ruleCriteria", ruleMap.get("criteria")));
                        rule.setContext(ruleMap.getOrDefault("ruleContext", ruleMap.get("context")));
                        rules.add(rule);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("ERROR: Failed to read rules file: " + e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            System.err.println("ERROR: Failed to parse YAML rules: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
        return rules;
    }
}
