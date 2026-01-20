import groovy.json.JsonSlurper
import groovy.json.JsonOutput
import java.nio.file.Paths

// --- 环境配置 ---
def SCRIPT_HOME = System.getenv("SCRIPT_HOME") ?: "/home/jenkins/agent/scripts"
def WORKSPACE_ROOT = System.getenv("WORKSPACE") ?: Paths.get(".").toRealPath().toString()

def RULES_FILE = "${SCRIPT_HOME}/rules.yaml"
def INTERMEDIATE_FILE = "${WORKSPACE_ROOT}/candidates.json"
def JAR_PATH = "${SCRIPT_HOME}/ai-code-extractor.jar" 

println "=== 步骤 1: 静态代码提取 ==="

// --- 核心修改：强制清理旧数据，防止旧 Prompt 逻辑残留 ---
def oldFile = new File(INTERMEDIATE_FILE)
if (oldFile.exists()) {
    println "🧹 发现旧的中间文件，正在清理以确保最新 rules.yaml 生效..."
    oldFile.delete()
}

println "🔍 正在扫描全量目录: ${WORKSPACE_ROOT}"

def allCandidates = []
try {
    // 校验 JAR 文件
    def jarFile = new File(JAR_PATH)
    if (!jarFile.exists() || jarFile.length() < 100) {
        println "❌ 错误: JAR 文件不存在或已损坏 (Size: ${jarFile.exists() ? jarFile.length() : 'Not Found'}) Path: ${JAR_PATH}"
        System.exit(1)
    }

    // 强制扫描根目录，不依赖 SCAN_TARGET
    def command = ["java", "-jar", JAR_PATH, WORKSPACE_ROOT, RULES_FILE]
    def proc = command.execute()
    
    // 使用 consumeProcessOutput 防止缓冲区死锁
    def outBuilder = new StringBuilder()
    def errBuilder = new StringBuilder()
    proc.consumeProcessOutput(outBuilder, errBuilder)
    
    proc.waitFor()
    
    def jsonOutput = outBuilder.toString()
    def errorOutput = errBuilder.toString()
    
    if (errorOutput) println "Java Extractor 日志:\n${errorOutput}"
    
    if (proc.exitValue() != 0) {
        println "❌ 提取器执行失败 (Exit Code: ${proc.exitValue()})"
        System.exit(1)
    }

    if (jsonOutput.trim()) {
        try {
            allCandidates = new JsonSlurper().parseText(jsonOutput)
        } catch (e) {
            println "❌ JSON 解析失败: ${e.getMessage()}"
            println "🐛 原始输出片段 (前500字符): ${jsonOutput.take(500)}"
            System.exit(1)
        }
    }
} catch (e) {
    println "❌ 提取器运行异常: ${e.getMessage()}"
    System.exit(1)
}

// 保存中间结果
def file = new File(INTERMEDIATE_FILE)
file.parentFile.mkdirs()
file.write(JsonOutput.prettyPrint(JsonOutput.toJson(allCandidates)))

println "✅ 提取完成. 找到待审计片段: ${allCandidates.size()} 个"
