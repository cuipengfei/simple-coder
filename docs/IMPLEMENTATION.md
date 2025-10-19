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

### Phase 3: Tools (已完成核心)
1. `PathValidator` — 路径安全检查
2. `ReadFileTool` — 读取文件（行号范围 + 截断）
3. `ListDirTool` — 列出目录 / glob（已实现结果上限 `max-list-results` + 截断提示 `[TRUNCATED: first N items]`）
4. `SearchTool` — 正则/包含搜索（统一截断：达到上限早停；目录完整遍历恰好等于上限不视为截断；截断消息示例 `[TRUNCATED: reached limit 50 before completing search]`）
5. `ReplaceTool` — 精确唯一替换（old != new；唯一出现）

### Phase 4: Agent Service & Controller (已完成，缺集成测试)
- 单轮交互：解析 → 工具路由（含 auto 模式）→ 执行 → 返回
- 已集成 Spring AI `ChatClient` 进行工具选择（`ModelToolSelectionStrategy`）
- Fallback：若模型返回名称不在映射，用工具 `getName()` 进行匹配
- 错误路径：未知工具 / 空工具名 / 执行异常 → 返回安全失败的 `ToolResponse`
- 响应 message 前缀 `[tool=<name>]` 便于追踪（前端需剥离展示可选）

### Phase 5: Minimal UI (待实现)
- REST API: `/api/agent` (已存在 Controller + AgentService)
- Static HTML/JS: 输入、工具选择（含 auto）、结果展示、简易上下文历史（客户端维护）

### Phase 6: Testing
- 单元测试：工具、模型、`AgentService`、`AgentController` 覆盖核心逻辑
- SearchTool：含 `testSearchDirectoryExactLimitNoTruncation` 验证“恰好等于上限不视为截断”语义
- 缺口：ListDirTool 截断场景（需模拟大结果）、集成测试（端到端：Controller + ChatClient + 工具执行）、UI 测试（UI 实现后）

---
## Tech Design

### Architecture (三层 + 无状态)
```
Controller (REST, POST /api/agent)
    ↓
AgentService (单轮逻辑 + Spring AI ChatClient 工具选择)
    ↓
Tools (PathValidator + read/list/search/replace)
```
客户端携带完整上下文；服务端不保存会话。

### Tool Interface
```java
public interface Tool {
    String getName();
    ToolResponse execute(ToolRequest request);
}
```

### PathValidator
- 输入路径标准化、realPath（存在时）
- 检查是否以 repoRoot 开头，否则 SecurityException

### AgentService (已实现)
```java
public ToolResponse process(ToolRequest request) {
    // validate request
    // if toolType == "auto" → ChatClient 分类出工具名
    // fallback 处理：模型返回名不在映射则遍历工具比对 getName()
    // 执行工具 → 返回 ToolResponse (message 前缀 [tool=name])
}
```

### Data Flow (示例: 搜索后读取)
```
User: "Search 'Agent' in docs/sources"
Controller → AgentService → SearchTool (PathValidator + Files.walk)
Result → ToolResponse(JSON)
User: "Read docs/sources/notes-system-prompts.md:1-20"
Controller → AgentService → ReadFileTool → 返回前 20 行
```

### Configuration (当前 application.yml 摘要)
```yaml
simple-coder:
  repo-root: ${user.dir}
  max-file-lines: 500
  max-search-results: 50
  max-list-results: 200

spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      chat:
        options:
          model: gpt-4.1

server:
  port: 8080
```
已更新为当前使用的 `gpt-4.1` 模型。

### Dependencies (pom.xml 摘要)
- spring-boot-starter-web
- spring-ai-starter-model-openai (由 spring-ai-bom 管理版本)
- lombok
- spring-boot-starter-test (scope test)

### Error Handling
- 路径越界 → ToolResponse.error + SecurityException 信息
- 文件不存在 / 非 regular file → 清晰 message
- SearchTool：regex 语法错误 → "Invalid regex pattern"
- ReplaceTool：多次匹配或未匹配 → 安全失败
- AgentService：未知工具名 / auto 返回空 / 映射失败 → 安全失败
- ListDirTool：不存在目录 / glob 基路径越界 / 输入指向文件 → 错误

### ListDirTool Glob 安全策略
- 先从模式中提取基路径 → PathValidator 校验 → Files.walk + PathMatcher 匹配 → 结果排序 → 应用上限与截断提示

### Testing (现状)
- 工具测试覆盖主要分支与错误场景
- SearchTool 含“exact limit no truncation”分支测试
- 缺口：ListDirTool 截断、端到端集成（auto + fallback）、UI 交互

### Planned Enhancements
1. ListDirTool 截断行为单测（验证 `[TRUNCATED: first N items]` 格式）
2. 集成测试：模拟 ChatClient + Controller 端到端 (auto/fallback)
3. Minimal UI 单页（静态资源）
4. 文档示例同步：截断消息格式统一展示
5. ReplaceTool 未来评估：重叠子串唯一匹配风险（真实误用出现时）
6. 会话期上下文前端实现（有限历史 N 条）

### Key Design Decisions
- 无状态服务端，减少持久化复杂度
- 单工具执行，避免并行/锁复杂度
- 截断优先（避免超长响应）
- ReplaceTool 强制唯一匹配，防止大范围意外替换
- auto 工具选择最小实现：无额外提示工程，仅名称分类
- 响应消息前缀 `[tool=name]` 供调试与 UI 分离显示

### TODO Summary
- [x] AgentService + Controller
- [x] Auto 工具选择逻辑 + fallback
- [x] ListDirTool 上限实现
- [ ] ListDirTool 截断单测
- [ ] UI + 前端静态文件
- [ ] 集成测试（Controller + ChatClient + 工具）
- [ ] 截断消息示例统一到文档与 UI 提示
- [ ] ReplaceTool 重叠匹配风险评估（仅在触发时）
- [ ] 会话期上下文 UI 实现

---
## Reference Mapping
- PathValidator → security boundary (docs + tests)
- ReadFileTool → line range + truncation
- SearchTool → unified truncation semantics
- ListDirTool → max-list-results + glob 安全策略 + 截断提示
- ReplaceTool → unique occurrence enforcement
- AgentService → auto routing + fallback + message 前缀
- AgentController → REST entrypoint

---
## Risks
| 风险 | 现状 | 缓解 |
|------|------|------|
| 集成测试缺失 | 仅单元测试 | 添加 Controller + ChatClient 集成用例 |
| ListDir 截断未测试 | 逻辑已实现 | 编写大目录截断单测 |
| ReplaceTool 重叠匹配场景 | 理论风险 | 保持唯一匹配约束；出现误用再加检测 |
| UI 缺失 | 仅后端接口 | 最小单页实现 + 手动验证 |
| 上下文未实现 | 规划中 | 前端维护有限历史 N 条 |

---
## Done vs Pending
- DONE: Models, Tools, AgentService, AgentController, SearchTool 截断语义测试, ListDirTool 上限逻辑, 模型名更新
- PENDING: ListDirTool 截断测试, 集成测试, UI, 截断消息示例统一, ReplaceTool 重叠风险评估, 会话上下文 UI