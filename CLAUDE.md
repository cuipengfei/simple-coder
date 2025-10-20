# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

> 说明：全文使用中文叙述，保留英文技术术语（类名、方法、依赖、命令等）。教育示例场景，倾向最小可行实现。

## 场景与定位
- 教学示例，非生产：追求最小改动即可通过测试并满足功能。
- 单轮无状态：客户端每次请求携带完整上下文；服务端不存储会话。
- 不支持：规划、自主多步、并行工具、持久记忆。

## 技术栈
- Java 21 + Spring Boot 3.5.6
- Spring AI 1.0.3（使用 ChatClient fluent API）
- 构建：Maven

## 架构总览
```
Client → POST /api/agent (ToolRequest JSON)
    Controller (AgentController)
        → AgentService.process()
            → ChatClient.tools(toolsService) (Spring AI 自动选择工具并提取参数)
            → ToolsService @Tool 方法执行 (readFile | listFiles | searchText | replaceText)
                → PathValidator 路径安全
                → 文件 / 目录 / 文本操作
返回 ToolResponse JSON （可选 data）
```
特性：
- 无状态：ToolRequest.contextHistory 携带全部历史；服务端不落地。
- 单工具：每个请求仅一次工具调用。
- 自然语言：用户发送自然语言 prompt，模型自动选择工具并提取参数。

## 核心组件
### Model 层 (com.simplecoder.model)
- ToolRequest：prompt、toolType（现已废弃，保留兼容性）、contextHistory；方法：validate()、buildContextSummary()。
- ToolResponse：success、message、data、error；静态工厂：success(...) / error(...)。
- ContextEntry：timestamp、prompt、result、getSummary()（用于上下文压缩）。

### ToolsService (com.simplecoder.service.ToolsService)
统一服务包含四个 @Tool 注解方法，Spring AI ChatClient 自动调用：

```java
@Tool(description = "Read file contents, optionally with line range")
public String readFile(
    @ToolParam(description = "File path relative to repository root") String filePath,
    @ToolParam(description = "Starting line number (optional)", required = false) Integer startLine,
    @ToolParam(description = "Ending line number (optional)", required = false) Integer endLine)

@Tool(description = "List directory contents or files matching glob pattern")
public String listFiles(
    @ToolParam(description = "Directory path or glob pattern") String path)

@Tool(description = "Search for text pattern (literal or regex) in files")
public String searchText(
    @ToolParam(description = "Text pattern to search for") String pattern,
    @ToolParam(description = "Directory or file path to search in") String searchPath,
    @ToolParam(description = "Whether pattern is regex", required = false) Boolean isRegex,
    @ToolParam(description = "Whether search is case-sensitive", required = false) Boolean caseSensitive)

@Tool(description = "Replace exact string in a file")
public String replaceText(
    @ToolParam(description = "File path relative to repository root") String filePath,
    @ToolParam(description = "Old string to replace") String oldString,
    @ToolParam(description = "New string to replace with") String newString)
```

辅助：PathValidator 限制所有路径在仓库根。

### AgentService (com.simplecoder.service.AgentService)
处理流程（已大幅简化）：
1. 验证 ToolRequest。
2. 构建上下文摘要（如有）。
3. 调用 ChatClient：
```java
String result = chatClient.prompt()
    .user(promptBuilder.toString())
    .tools(toolsService)  // 注册所有 @Tool 方法
    .call()
    .content();
```
4. Spring AI 自动：选择工具 → 提取参数 → 调用方法 → 返回结果。
5. 捕获异常统一返回 ToolResponse.error("AgentService error", e.getMessage())。
## 工具语义与资源限制
- read：读取文件（可选行范围）；超出最大行数 → `[TRUNCATED: showing first N lines, M more available]`；空文件 → `empty file: 0 lines`。
- list：列出目录或 glob；超过最大数量 → `[TRUNCATED: first N items]`。
- search：正则或子串搜索；若在遍历未完成前达到结果上限 → `[TRUNCATED: reached limit N before completing search]`。
- replace：精确唯一替换；old_string 未找到或出现次数≠1 → 失败。唯一性按非重叠出现统计（示例：内容 "aaa" + old_string "aa" 视为 1 次）。

## 错误处理
- 失败路径：模型选择异常 / 工具运行异常 → `ToolResponse.error(message, detail)`。
- `AgentService error` 为统一包装层。
- 工具内部错误（路径越界、文件不存在等）由工具方法返回 "Error: ..." 前缀字符串。

## 接口示例
POST /api/agent
Content-Type: application/json
请求示例（自然语言）：
```
{
  "prompt": "search for any java file",
  "toolType": "auto",
  "contextHistory": []
}
```
或结构化格式（兼容）：
```
{
  "prompt": "Read src/main/java/com/simplecoder/service/AgentService.java lines 1-40",
  "toolType": "auto",
  "contextHistory": []
}
```
成功响应示例：
```
{
  "success": true,
  "message": "Tool execution result",
  "data": "Read ... (lines 1-40 of X total)\n\n<file content>",
  "error": null
}
```

## 测试覆盖概览
- 已覆盖：Model 类、AgentController。
- 未覆盖：ToolsService 工具方法单测、端到端集成测试（ChatClient + 实际模型调用）。
- 暂不处理：replace 重叠匹配 edge case、多步连续交互场景。

## 常用 Maven 命令
```bash
# 清理并编译
mvn clean compile

# 全部测试
mvn test

# 单个测试类
mvn test -Dtest=ToolRequestTest

# 多个测试类
mvn test -Dtest="ToolRequestTest,ToolResponseTest"

# 运行应用
mvn spring-boot:run

# 打包
mvn clean package
```

## 配置说明
默认配置（application.yml）使用本地模型：
```yaml
spring.ai.openai:
  api-key: dummy-local
  base-url: http://localhost:4141
  chat.options.model: gpt-4.1
```
如需使用 OpenAI 官方 API，需修改配置或设置环境变量：
- Windows: `set OPENAI_API_KEY=sk-xxx`
- Unix: `export OPENAI_API_KEY=sk-xxx`

资源限制配置：
- `simple-coder.max-file-lines`: 500（单文件最大读取行数）
- `simple-coder.max-list-results`: 200（list 工具最大返回数）
- `simple-coder.max-search-results`: 50（search 工具最大结果数）

## 约束与非目标
- 不支持单请求多工具或并行执行。
- 不保存服务端会话状态（无 Redis/DB）。
- 文件/路径操作严格限制仓库根（PathValidator）。
- 不做大规模重构，除非为修复正确性。

## 后续扩展指引
- 新增工具：在 ToolsService 中添加 @Tool 注解方法，Spring AI 会自动识别。
- 工具描述需清晰，便于模型选择。
- 参数描述需详细，便于模型从自然语言提取。
- 避免引入不必要依赖（mockito-core 已由 spring-boot-starter-test 覆盖基础需求）。

## 已知但暂不处理的 Edge Cases
- replace 重叠匹配（"aaa" + "aa"）统计为 1 次；仅在需要严格语义时再调整。
- 超大 contextHistory：由客户端自行截断；服务端照单处理。

End of file.
