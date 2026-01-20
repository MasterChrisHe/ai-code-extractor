import groovy.json.JsonSlurper
import groovy.json.JsonOutput
import java.nio.charset.StandardCharsets

class AIEngine {
    static final def API_URL = System.getenv("AI_API_URL")
    static final def API_TOKEN = System.getenv("AI_API_TOKEN") 
    static final def MODEL_NAME = System.getenv("AI_MODEL") ?: "default"
    static final def ENGINE_ID = "AI_ANALYZER"

    static final int TIMEOUT_MS = 30000 
    static final int MAX_RETRIES = 1 

    static final def BASE_SYSTEM_PROMPT = """
        你是资深代码审计专家。请严格按以下 JSON 格式输出审计结果：
        {
          "violation": true/false,
          "reasoning": "中文判定理由，内部引用强制使用单引号(')，严禁使用双引号",
          "message": "违规时填入提供的'固定错误描述'；合规时填 'OK'"
        }
        注意：不要输出任何 Markdown 标记，只返回 JSON 字符串。
        """

    /**
     * 调用 AI 接口
     * @param logBuffer 用于收集日志的列表，避免多线程打印错乱
     */
    static def callAI(String userPrompt, String systemPrompt, List<String> logBuffer) {
        if (!API_URL || !API_TOKEN) {
            logBuffer.add("❌ 错误: 环境变量 AI_API_URL 或 AI_API_TOKEN 未设置")
            return null
        }

        def payload = [
            model: MODEL_NAME,
            messages: [
                [role: "system", content: systemPrompt],
                [role: "user", content: userPrompt]
            ],
            stream: false
        ]

        // --- 修复中文显示逻辑 ---
        def jsonString = JsonOutput.toJson(payload)
        // 将 Unicode 转义字符 (\u4e2d) 还原为中文，便于在 Jenkins/终端日志中查看
        def readableJson = jsonString.replaceAll(/\\u([0-9a-fA-F]{4})/) { m, code -> 
            (Integer.parseInt(code, 16) as char).toString() 
        }
        logBuffer.add("📤 [Request Payload]: ${readableJson}") 

        for (int i = 0; i <= MAX_RETRIES; i++) {
            try {
                def conn = new URL(API_URL).openConnection()
                conn.setRequestMethod("POST")
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8") // 明确指定编码
                conn.setRequestProperty("Authorization", "Bearer ${API_TOKEN}")
                conn.setRequestProperty("Accept", "text/event-stream, application/json")
                conn.setDoOutput(true)
                conn.setConnectTimeout(10000)
                conn.setReadTimeout(TIMEOUT_MS)

                // 确保发送给 AI 的字节流也是 UTF-8
                conn.getOutputStream().withWriter(StandardCharsets.UTF_8.name()) { 
                    it.write(jsonString) 
                }

                if (conn.responseCode == 200) {
                    def responseString = conn.getInputStream().withReader(StandardCharsets.UTF_8.name()) { it.getText() }
                    // 同样对响应记录进行中文还原显示
                    def readableResponse = responseString.replaceAll(/\\u([0-9a-fA-F]{4})/) { m, code -> 
                        (Integer.parseInt(code, 16) as char).toString() 
                    }
                    logBuffer.add("📥 [Raw Response]: ${readableResponse}") 
                    return parseSseResponse(responseString)
                } else {
                    def errorMsg = conn.getErrorStream()?.getText(StandardCharsets.UTF_8.name())
                    logBuffer.add("⚠️ API 失败 (Code ${conn.responseCode}): ${errorMsg}")
                }
            } catch (Exception e) {
                logBuffer.add("⚠️ 尝试 ${i+1} 异常: ${e.getMessage()}")
            }
            if (i < MAX_RETRIES) sleep(1000)
        }
        return null
    }

    static def parseSseResponse(String rawText) {
        if (!rawText) return null
        if (!rawText.contains("data:")) {
            try {
                def json = new JsonSlurper().parseText(rawText)
                return json.choices[0]?.message?.content?.trim()
            } catch (e) { return rawText }
        }
        StringBuilder fullContent = new StringBuilder()
        rawText.eachLine { line ->
            def trimmed = line.trim()
            if (trimmed.startsWith("data:")) {
                def jsonStr = trimmed.substring(5).trim()
                if (jsonStr && jsonStr != "[DONE]") {
                    try {
                        def json = new JsonSlurper().parseText(jsonStr)
                        def part = json.choices[0]?.delta?.content ?: json.choices[0]?.message?.content
                        if (part) fullContent.append(part)
                    } catch (ignore) {}
                }
            }
        }
        return fullContent.toString().trim()
    }

    // --- 增加 logBuffer 参数 ---
    static def analyzeSingleSnippet(String ruleDesc, String code, String name, String ruleCriteria, String ruleContext, List<String> logBuffer) {
        def safeDesc = ruleDesc ?: "请参考详细判定逻辑"
        def safeContext = ruleContext ?: "无额外逻辑"

        def prompt = """
        [任务目标]
        分析提供的代码片段，判断其是否违反规则。

        [规则定义]
        核心规则: ${safeDesc}
        违规报错信息: ${ruleCriteria}

        [详细判定逻辑]
        ${safeContext}

        [待分析代码]
        ${code}
        """
        
        // 调用 AI 并传入日志 buffer
        def resultStr = callAI(prompt, BASE_SYSTEM_PROMPT, logBuffer)
        
        if (!resultStr) return null

        return cleanAndParseJson(resultStr, name, ruleCriteria, logBuffer)
    }

    static def cleanAndParseJson(String str, String name, String defaultMsg, List<String> logBuffer) {
        String jsonOnly = str
        try {
            def start = str.indexOf('{')
            def end = str.lastIndexOf('}')
            if (start != -1 && end != -1) {
                jsonOnly = str.substring(start, end + 1)
                // 增加 (?s) 以支持多行 reasoning 内容
                jsonOnly = jsonOnly.replaceAll(/(?s)"reasoning"\s*:\s*[‘''](.*?)[’'']\s*,\s*"message"/, { Object[] m -> 
                    def innerContent = m[1].toString().replace("\"", "'") 
                    return "\"reasoning\": \"${innerContent}\", \"message\"" 
                })
                return new JsonSlurper().parseText(jsonOnly)
            }
        } catch (e) {
            logBuffer.add("⚠️ JSON 解析失败，尝试正则提取...")
            return fallbackRegexParse(jsonOnly, defaultMsg)
        }
        return null
    }

    static def fallbackRegexParse(String jsonStr, String defaultMsg) {
        try {
            def result = [:]
            def vioMatch = (jsonStr =~ /"violation"\s*:\s*(true|false)/)
            if (vioMatch.find()) result.violation = vioMatch.group(1).toBoolean()
            else return null

            def reasonMatch = (jsonStr =~ /(?s)"reasoning"\s*:\s*["'‘]?\s*(.*?)\s*["'’]?\s*,\s*"message"/)
            if (reasonMatch.find()) result.reasoning = reasonMatch.group(1).replace("\"", "'")
            else result.reasoning = "AI 结果格式异常"
            
            result.message = defaultMsg
            return result
        } catch (ex) { return null }
    }
}
