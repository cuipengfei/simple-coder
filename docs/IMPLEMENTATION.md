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
3. `ListDirTool` — 列出目录 / glob（当前缺少结果上限，TODO）
4. `SearchTool` — 正则/包含搜索（统一截断：达到上限早停；目录完整遍历恰好等于上限不视为截断）
5. `ReplaceTool` — 精确唯一替换

### Phase 4: Agent Service (待实现)
- 单轮交互：解析 → 工具路由（auto 需 LLM）→ 执行 → 返回
- Spring AI ChatClient 集成（model + messages + tool selection）

### Phase 5: Minimal UI (待实现)
- REST API: `/api/agent` (Controller + AgentService)
- Static HTML/JS: 输入、工具选择、结果、简易历史

### Phase 6: Testing
- 单元测试（已覆盖各工具与模型）
- 集成测试（待添加：Controller + AgentService）
- 端到端（UI 驱动）

---
## Tech Design

### Architecture (三层 + 无状态)
```
Controller (REST, POST /api/agent)
    ↓
AgentService (单轮逻辑 + Spring AI ChatClient)
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

### AgentService (计划)
```java
public ToolResponse process(ToolRequest request) {
    // validate request
    // if toolType == auto → 调用模型选择工具
    // route → tool.execute(request)
    // return ToolResponse
}
```

### Data Flow (示例: 搜索后读取)
```
User: "Search 'Agent' in docs"
Controller → AgentService → SearchTool (PathValidator + Files.walk)
Result → ToolResponse(JSON)
User: "Read docs/sources/notes-system-prompts.md:1-20"
Controller → AgentService → ReadFileTool → 返回前 20 行
```

### Configuration (真实 current application.yml)
```yaml
simple-coder:
  repo-root: ${user.dir}
  max-file-lines: 500
  max-search-results: 50

spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      chat:
        options:
          model: gpt-3.5-turbo  # TODO: 更新为实际可用模型

server:
  port: 8080
```
注意：旧文档示例使用 `spring.ai.openai.model` 已过时，应使用 `spring.ai.openai.chat.options.model`。

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

### Testing (现状)
- 各工具均有 JUnit5 测试覆盖主要分支与错误场景
- SearchTool 追加 `testSearchDirectoryExactLimitNoTruncation` 验证截断语义
- 缺口：ListDirTool 大结果场景（尚无上限逻辑）

### Planned Enhancements
1. AgentService + Controller
2. Auto 模式：构造 prompt（含 request.buildContextSummary()）→ 模型返回工具名
3. ListDirTool 结果上限 + 截断提示键（配置：`simple-coder.max-list-results` 计划）
4. UI：单页应用，最小控件
5. 集成测试：Mock ChatClient（或提供假实现）验证 auto 路径分支
6. 模型名更新（避免不可用 gpt-3.5-turbo）

### Key Design Decisions
- 无状态服务端，减少持久化复杂度
- 单工具执行，避免并行/冲突锁设计
- 截断优先（避免超长响应）
- ReplaceTool 强制唯一匹配，防止大范围意外替换

### TODO Summary
- [ ] AgentService + Controller
- [ ] Auto 工具选择逻辑
- [ ] ListDirTool 上限与测试
- [ ] UI + 前端静态文件
- [ ] 集成测试
- [ ] 配置与文档同步（模型名 + 新增 max-list-results）

---
## Reference Mapping
- PathValidator → security boundary (docs + tests)
- ReadFileTool → line range + truncation
- SearchTool → unified truncation semantics
- ReplaceTool → unique occurrence enforcement

---
## Risks
| 风险 | 现状 | 缓解 |
|------|------|------|
| 模型名不可用 | 使用 gpt-3.5-turbo | 启动前替换为供应商当前可用列表项 |
| list_dir 过长输出 | 无上限 | 添加 max-list-results + 截断提示 |
| auto 路由缺失 | 未实现 | Phase 4 引入 ChatClient + 工具名称映射 |
| 集成测试缺失 | 工具单测已覆盖 | 添加 Controller + AgentService 集成用例 |

---
## Done vs Pending
- DONE: Models + Tools + 单元测试（含截断语义修复）
- PENDING: Service, Controller, UI, ListDirTool 限制, 集成测试, 模型名更新