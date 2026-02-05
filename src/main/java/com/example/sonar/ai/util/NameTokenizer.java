package com.example.sonar.ai.util;

import java.util.ArrayList;
import java.util.List;

/**
 * 名称分词器工具类
 * 用于将类名、方法名、变量名拆分为语义化的 token 数组
 * 
 * 支持的命名风格：
 * - 驼峰/帕斯卡命名 (camelCase/PascalCase)
 * - 全大写+下划线 (UPPER_CASE)
 * - 下划线命名 (snake_case)
 * - 连续大写缩写块 (getHTTPStatus)
 * 
 * 规则：
 * - 数字仅作为分隔符，不作为 token 输出
 * - 所有 token 转换为小写
 * - 不支持连字符分隔（项目不使用）
 * - 不进行词典查询或语义推断
 */
public class NameTokenizer {

    /**
     * 将名称拆分为 token 列表
     * 
     * @param name 待拆分的名称（类名、方法名、变量名等）
     * @return token 列表，所有 token 均为小写
     * 
     *         示例：
     *         - getUserInfoById → [get, user, info, by, id]
     *         - EXPIRED_TIME → [expired, time]
     *         - user_config → [user, config]
     *         - getHTTPStatus → [get, http, status]
     *         - user2Config → [user, config]
     */
    public static List<String> tokenize(String name) {
        if (name == null || name.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> tokens = new ArrayList<>();
        StringBuilder currentToken = new StringBuilder();

        char[] chars = name.toCharArray();
        int i = 0;

        while (i < chars.length) {
            char c = chars[i];

            // 处理下划线：作为分隔符，结束当前 token
            if (c == '_') {
                if (currentToken.length() > 0) {
                    tokens.add(currentToken.toString().toLowerCase());
                    currentToken.setLength(0);
                }
                i++;
                continue;
            }

            // 处理数字：作为分隔符，不输出为 token
            if (Character.isDigit(c)) {
                if (currentToken.length() > 0) {
                    tokens.add(currentToken.toString().toLowerCase());
                    currentToken.setLength(0);
                }
                i++;
                continue;
            }

            // 处理字母
            if (Character.isLetter(c)) {
                // 大写字母的处理逻辑
                if (Character.isUpperCase(c)) {
                    // 如果当前 token 不为空
                    if (currentToken.length() > 0) {
                        // 检查当前 token 的最后一个字符
                        char lastChar = currentToken.charAt(currentToken.length() - 1);

                        // 如果最后一个字符是小写，说明遇到了新单词
                        if (Character.isLowerCase(lastChar)) {
                            // 保存之前的 token，开始新 token
                            tokens.add(currentToken.toString().toLowerCase());
                            currentToken.setLength(0);
                            currentToken.append(c);
                        } else {
                            // 最后一个字符是大写，说明是连续大写
                            // 需要判断下一个字符是否是小写
                            if (i + 1 < chars.length && Character.isLowerCase(chars[i + 1])) {
                                // 下一个是小写，说明当前大写字母是新单词的开头
                                // 例如：HTTPStatus 中的 S
                                // 保存之前的连续大写部分
                                tokens.add(currentToken.toString().toLowerCase());
                                currentToken.setLength(0);
                                currentToken.append(c);
                            } else {
                                // 下一个也是大写或数字或结束，继续添加
                                currentToken.append(c);
                            }
                        }
                    } else {
                        // currentToken 为空，开始新 token
                        currentToken.append(c);
                    }
                } else {
                    // 小写字母
                    currentToken.append(c);
                }
            }

            i++;
        }

        // 添加最后一个 token
        if (currentToken.length() > 0) {
            tokens.add(currentToken.toString().toLowerCase());
        }

        return tokens;
    }

    /**
     * 将 token 列表格式化为字符串表示
     * 
     * @param tokens token 列表
     * @return 格式化字符串，例如 "[get, user, info]"
     */
    public static String formatTokens(List<String> tokens) {
        if (tokens == null || tokens.isEmpty()) {
            return "[]";
        }
        return "[" + String.join(", ", tokens) + "]";
    }
}
