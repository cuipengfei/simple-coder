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
            → (toolType=auto?) ModelToolSelectionStrategy (ChatClient 选择工具)
            → Tool 实例执行 (read | list | search | replace)
                → PathValidator 路径安全
                → 文件 / 目录 / 文本操作
返回 ToolResponse JSON （含前缀、可选 data）
```
特性：
- 无状态：ToolRequest.contextHistory 携带全部历史；服务端不落地。
- 单工具：每个请求仅一次工具调用。

## 核心组件
### Model 层 (com.simplecoder.model)
- ToolRequest：prompt、toolType（显式或"auto"）、contextHistory；方法：validate()、buildContextSummary()。
- ToolResponse：success、message、data、error；静态工厂：success(...) / error(...)。
- ContextEntry：timestamp、prompt、result、getSummary()（用于上下文压缩）。

### Tool 抽象 (com.simplecoder.tool)
```
public interface Tool {
  String getName();
  ToolResponse execute(ToolRequest request);
}
```
实现：read / list / search / replace（getName() 与协议名一致）。
辅助：PathValidator 限制所有路径在仓库根。

### AgentService (com.simplecoder.service.AgentService)
路由流程：
1. 空或空白 toolType 归一化为 "auto"。
2. 若为 auto：调用 toolSelectionStrategy.selectTool(request) 获取具体工具名。
3. 先按 Map key 直接查找（Spring 注入可能是 bean 名）。
4. 若为空：fallback 遍历 values，比较 Tool.getName()（忽略大小写），解决 bean 名 ≠ 协议名（如 bean "readFileTool"）。
5. 执行工具；若返回 message 非空，前缀 `[tool=<selected>]`。
6. 捕获异常统一返回 ToolResponse.error("AgentService error", e.getMessage())。
错误类型：未知工具 / 工具执行异常 / 策略选择异常。

### 工具选择策略 (ModelToolSelectionStrategy)
ChatClient fluent 调用：
```
String raw = chatClient.prompt()
  .system(s -> s.text(systemInstruction))
  .user(u -> u.text(userInstruction))
  .call()
  .content();
```
System 指令强约束输出：只能一个 token（read | list | search | replace）。
Normalization：trim → lowercase → 去首尾引号/反引号 → 若包含空格取首 token。
非法 token 抛 IllegalArgumentException，外层由 AgentService 捕获转错误响应。
## 工具语义与资源限制
- read：读取文件（可选行范围）；超出最大行数 → `[TRUNCATED: showing first N lines, M more available]`；空文件 → `empty file: 0 lines`。
- list：列出目录或 glob；超过最大数量 → `[TRUNCATED: first N items]`。
- search：正则或子串搜索；若在遍历未完成前达到结果上限 → `[TRUNCATED: reached limit N before completing search]`。
- replace：精确唯一替换；old_string 未找到或出现次数≠1 → 失败。唯一性按非重叠出现统计（示例：内容 "aaa" + old_string "aa" 视为 1 次）。

## 错误处理与前缀
- 成功：统一在返回 message 前添加 `[tool=<name>]` 便于追踪。
- 失败路径：未知工具 / 模型选择异常 / 工具运行异常 → `ToolResponse.error(message, detail)`。
- `AgentService error` 为统一包装层；`Unknown tool: <name>` 表示未能路由。

## 接口示例
POST /api/agent
Content-Type: application/json
请求示例：
```
{
  "prompt": "Read src/main/java/com/simplecoder/service/AgentService.java lines 1-40",
  "toolType": "read",
  "contextHistory": []
}
```
成功响应示例：
```
{
  "success": true,
  "message": "[tool=read] Read ... (lines 1-40 of X total)",
  "data": "<truncated or full content>",
  "error": null
}
```

## 测试覆盖概览
- 已覆盖：Model 类、各 Tool、AgentService（显式/auto/fallback/异常）、AgentController。
- 暂未覆盖：多步连续交互场景；replace 重叠匹配 edge case（目前接受现状）。

## 常用 Maven 命令
```bash
# 清理并编译
mvn clean compile

# 全部测试
mvn test

# 单个测试类
mvn test -Dtest=AgentServiceTest

# 多个测试类
mvn test -Dtest="ToolRequestTest,ToolResponseTest"

# 运行应用（需 OPENAI_API_KEY）
OPENAI_API_KEY=sk-xxx mvn spring-boot:run

# 打包
mvn clean package
```
环境变量设置：
Windows:
```cmd
set OPENAI_API_KEY=sk-xxx
```
Unix:
```bash
export OPENAI_API_KEY=sk-xxx
```
未设置 OPENAI_API_KEY 时 auto 模式会失败（模型调用异常）。

## 约束与非目标
- 不支持单请求多工具或并行执行。
- 不保存服务端会话状态（无 Redis/DB）。
- 文件/路径操作严格限制仓库根（PathValidator）。
- 不做大规模重构，除非为修复正确性。

## 后续扩展指引
- 新增 Tool：保持名称为单一小写 token；同步更新模型系统指令枚举。
- 保持成功消息前缀格式 `[tool=<name>]`。
- 若 bean 名与协议名分离，确保 fallback 逻辑仍适用。
- 避免引入不必要依赖（mockito-core 已由 spring-boot-starter-test 覆盖基础需求）。

## 已知但暂不处理的 Edge Cases
- replace 重叠匹配（"aaa" + "aa"）统计为 1 次；仅在需要严格语义时再调整。
- 超大 contextHistory：由客户端自行截断；服务端照单处理。

End of file.
