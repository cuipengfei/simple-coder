# Implementation & Tech Design Plan

## Implementation Plan

### Phase 1: Project Setup
1. Spring Boot (Java 21, Maven) + Spring AI dependency
2. Configure application.yml (API key, repo root path)
3. Directory structure: controller, service, model, tool

### Phase 2: Core Models
- `ToolRequest` - 用户请求（prompt, toolType, contextHistory）
- `ToolResponse` - 统一响应（success, message, data, error）
- `ConversationContext` - 会话期上下文（session-scoped bean）

### Phase 3: Tools（按依赖顺序）
1. `PathValidator` - 路径安全检查
2. `ReadFileTool` - 读取文件（支持行号范围）
3. `ListDirTool` - 列出目录/glob
4. `SearchTool` - 正则/包含搜索
5. `ReplaceTool` - 精确替换（唯一性验证）

### Phase 4: Agent Service
- 单轮交互：解析意图 → 工具路由 → 执行 → 格式化
- Spring AI 集成（ChatClient，Auto 模式选工具）

### Phase 5: Minimal UI
- REST API: `/api/agent`
- Static HTML + JS: 输入框 + 工具下拉 + 结果区 + 简易历史

### Phase 6: Testing
- 单元测试（工具独立验证）
- 集成测试（Spring Boot Test）
- 手动测试（连续任务场景）

---

## Tech Design

### Architecture（极简三层）
```
Controller (REST)
    ↓
AgentService (单轮逻辑 + Spring AI)
    ↓
Tools (read/list/search/replace)
```

### Key Components

#### Tool Interface
```java
public interface Tool {
    String getName();
    ToolResponse execute(ToolRequest request);
}
```

#### PathValidator（安全核心）
```java
public class PathValidator {
    private final String repoRoot;
    public void validate(String path) throws SecurityException;
}
```

#### ConversationContext（会话期）
```java
@Component
@Scope(value = SCOPE_SESSION, proxyMode = TARGET_CLASS)
public class ConversationContext {
    private final Deque<ContextEntry> history = new LinkedList<>();
    public void addEntry(String prompt, String result);
    public String getContextSummary(); // 最近 10 条
}
```

#### AgentService（核心流程）
```java
public ToolResponse process(ToolRequest request) {
    // 1. Auto 模式 → LLM 选工具
    // 2. 路由到 Tool
    // 3. 执行并格式化
    // 4. 更新 context
    // 5. 返回 ToolResponse
}
```

### Data Flow（单回合示例）
```
用户: "搜索 docs/ 中的 Agent"
  ↓
Controller → AgentService (Auto) → ChatClient 选 SearchTool
  ↓
SearchTool → PathValidator → Files.walk + Pattern
  ↓
返回 List<String> "file:line:snippet" (最多 50 条)
  ↓
ConversationContext.addEntry(...)
  ↓
JSON Response → UI 渲染
```

### Configuration（application.yml）
```yaml
simple-coder:
  repo-root: ${user.dir}
  max-file-lines: 500
  max-search-results: 50
  context-history-size: 10

spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      model: gpt-3.5-turbo
```

### Dependencies（核心）
```xml
<dependencies>
    <dependency>spring-boot-starter-web</dependency>
    <dependency>spring-ai-openai-spring-boot-starter</dependency>
    <dependency>lombok</dependency>
    <dependency>spring-boot-starter-test</dependency>
</dependencies>
```

### Minimal UI Structure
```html
<textarea id="prompt"></textarea>
<select id="tool">
  <option value="auto">Auto</option>
  <option>read/list/search/replace</option>
</select>
<button id="submit">Submit</button>
<pre id="result"></pre>
<div id="history"></div>
```

### Error Handling
- 路径越界 → 403 "路径超出仓库根"
- 文件不存在 → 404 "文件未找到"
- 替换不唯一 → 400 "需唯一匹配"
- 正则无效 → 400 "正则表达式错误"
- LLM 失败 → 503 "模型服务不可用"

### Testing Strategy
1. **单元测试**: PathValidator, 各 Tool 独立验证
2. **集成测试**: 单轮流程 + 会话上下文
3. **手动测试**: 连续任务、边界场景

---

## Key Design Decisions（极简原则）

1. **No Tool Framework** - 简单 `List<Tool>` 注入
2. **Session-Scoped Context** - Spring Session Bean（无 Redis/DB）
3. **Synchronous Only** - 单线程，无异步
4. **Minimal Validation** - 仅路径安全
5. **Plain HTML/JS** - 无前端框架
6. **Truncation Over Pagination** - 超限截断 + 提示

---

**参考文档**:
- `docs/sources/notes-coding-agent.md` § 2.1, § 3.2
- `docs/syntheses/architecture-patterns.md` § 4
- `docs/syntheses/code-modification.md` § 2.1
