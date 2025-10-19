# Implementation & Tech Design Plan

## Implementation Plan

### Phase 1: Project Setup
1. Spring Boot (Java 21, Maven) + Spring AI dependency
2. Configure application.yml (API key, repo root path)
3. Directory structure: controller, service, model, tool

### Phase 2: Core Models
- `ToolRequest` (prompt, toolType, contextHistory) — 无状态：客户端维护历史
- `ToolResponse` (success, message, data, error)
- `ContextEntry` (timestamp, prompt, result)

### Phase 3: Tools（已完成核心）
1. `PathValidator` — 路径安全检查
2. `ToolsService` — 统一服务包含四个 @Tool 方法：
   - `readFile` — 读取文件（行号范围 + 截断）
   - `listFiles` — 列出目录 / glob（结果上限 `max-list-results` + 截断提示）
   - `searchText` — 正则/包含搜索（统一截断语义）
   - `replaceText` — 精确唯一替换（old != new；唯一出现）

### Phase 4: Agent Service & Controller（已完成，缺集成测试）
- 单轮交互：拼接上下文摘要 + 用户请求 → Spring AI 原生工具选择与参数提取 → 执行 → 返回
- 集成 Spring AI `ChatClient` 以使用 @Tool 注解自动调用
- 自然语言支持：用户直接描述需求；模型自动识别工具并提取参数
- 错误路径：执行异常 → 返回安全失败的 `ToolResponse`
- 重要说明：当前后端忽略 `ToolRequest.toolType`，工具选择完全由模型决定；UI 下拉仅作提示

### Phase 5: Minimal UI（已实现）
- REST API: `/api/agent` (Controller + AgentService)
- Static HTML: `src/main/resources/static/index.html`
  - 输入、工具选择（默认 auto）、结果展示
  - 客户端维护上下文历史（最近 20 条），随请求发送至后端

### Phase 6: Testing（现状）
- 单元测试：Model 类、`AgentController` 已覆盖
- 缺口：`ToolsService` 工具方法单测、端到端集成测试（Controller + ChatClient + 实际模型调用）、UI 测试

---
## Tech Design

### Architecture（三层 + 无状态）
```
Controller (REST, POST /api/agent)
    ↓
AgentService（单轮逻辑 + Spring AI ChatClient 原生工具调用）
    ↓
ToolsService (@Tool 注解方法 + PathValidator)
```
客户端携带完整上下文；服务端不保存会话。

### ToolsService 行为与截断语义
- readFile
  - 行号范围：startLine / endLine（可选）
  - 超出 `max-file-lines` → 截断并提示：`[TRUNCATED: showing first N lines, M more available]`
- listFiles（目录或 glob）
  - 返回“文件与目录”，排序后输出
  - 结果数 > `max-list-results` → 截断并提示：`[TRUNCATED: first N items]`
- searchText
  - 输出 `file:line:snippet`，snippet 最多 100 字符
  - 达到 `max-search-results` 早停 → 截断提示：`[TRUNCATED: reached limit N before completing search]`
  - 若恰等于上限且遍历完成，不标记截断
- replaceText
  - 安全约束：old != new；old 在文件中恰好出现一次；路径在 repo-root 内；文件存在且为 regular file
  - 输出成功摘要（不返回 diff）

### PathValidator
- 规范化输入路径，存在时转 realPath
- 验证必须以 repoRoot 开头，否则抛 SecurityException

### AgentService（已实现）
```java
public ToolResponse process(ToolRequest request) {
    request.validate();
    String contextSummary = request.buildContextSummary();

    StringBuilder promptBuilder = new StringBuilder();
    if (contextSummary != null && !contextSummary.isBlank()) {
        promptBuilder.append("Context History:\n").append(contextSummary).append("\n\n");
    }
    promptBuilder.append("User Request:\n").append(request.getPrompt());

    String result = chatClient.prompt()
        .user(promptBuilder.toString())
        .tools(toolsService)
        .call()
        .content();

    return ToolResponse.success("Tool execution result", result);
}
```
Spring AI 自动完成：工具选择 → 参数提取 → 方法调用。

### Configuration（当前 application.yml 摘要）
```yaml
simple-coder:
  repo-root: ${user.dir}
  max-file-lines: 500
  max-search-results: 50
  max-list-results: 200

spring:
  ai:
    openai:
      api-key: dummy-local
      base-url: http://localhost:4141
      chat:
        options:
          model: gpt-4.1

server:
  port: 8080
```
风险与排障：当前使用 dummy-local 与 base-url http://localhost:4141（无 /v1）；若未配置可用的 OpenAI 兼容代理/服务，原生 tool-calling（Auto 模式）将不可用。

### Dependencies（pom.xml 摘要）
- spring-boot-starter-web
- spring-ai-starter-model-openai（由 spring-ai-bom 管理版本）
- lombok
- spring-boot-starter-test（scope test）

### Known Issues / TODO
- ToolsService 缺单元测试（覆盖截断、边界条件、错误处理）
- 端到端集成测试缺失（Controller + ChatClient + 实际模型）
- 截断提示文案需在 UI 与文档中统一示例
- 当历史为空时，AgentService 仍可能注入默认摘要（如 “No previous context.”）；建议后续仅在存在历史时注入

### Key Decisions
- 无状态服务端，降低持久化复杂度
- 单工具执行，避免并行/锁复杂度
- 截断优先（避免超长响应）
- replaceText 强制唯一匹配，防止大范围意外替换
- 采用 Spring AI 原生 tool-calling（@Tool 注解），简化实现并支持自然语言输入

---
## Reference Mapping
- PathValidator → 安全边界
- ToolsService.readFile → 行范围 + 截断
- ToolsService.searchText → 统一截断语义
- ToolsService.listFiles → 上限 + glob 安全策略 + 截断提示
- ToolsService.replaceText → 唯一匹配约束
- AgentService → 原生工具调用
- AgentController → REST 入口

---
## Risks
| 风险 | 现状 | 缓解 |
|------|------|------|
| 集成测试缺失 | 仅单元测试 | 补端到端集成测试 |
| ToolsService 单测缺失 | 无工具方法单测 | 增补正反用例 |
| replaceText 重叠匹配 | 理论风险 | 保持唯一匹配约束；出现误用时再加检测 |
| 代理/模型依赖 | dummy-local + 本地代理占位 | 提供显式“工具直连模式”作为未来降级 |
