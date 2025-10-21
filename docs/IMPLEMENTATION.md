# Implementation & Technical Design Plan

## Implementation Plan

### Phase 1: Project Setup
1. Spring Boot (Java 21, Maven) + Spring AI dependency
2. Configure application.yml (API key, repo root path)
3. Directory structure: controller, service, model, tool

### Phase 2: Core Models
- `ToolRequest` (prompt, toolType, contextHistory) — stateless: history maintained client-side
- `ToolResponse` (success, message, data, error)
- `ContextEntry` (timestamp, prompt, result)

### Phase 3: Tools (core completed)
1. `PathValidator` — path safety checks
2. `ToolsService` — unified service exposing four @Tool methods:
   - `readFile` — file read (line range + truncation)
   - `listFiles` — list directory / glob (limit `max-list-results` + truncation message)
   - `searchText` — regex / contains search (unified truncation semantics)
   - `replaceText` — exact unique replacement (old != new; must appear exactly once)

### Phase 4: Agent Service & Controller (completed, missing integration tests)
- Single-turn interaction: build context summary + user request → Spring AI native tool selection & parameter extraction → execute → return
- Integrates Spring AI `ChatClient` to auto invoke @Tool methods
- Natural language support: user describes intent; model identifies tool & extracts parameters
- Error path: execution exception → safe failure `ToolResponse`
- Note: backend currently ignores `ToolRequest.toolType`; tool selection fully model-driven; UI dropdown advisory only

### Phase 5: Minimal UI (implemented)
- REST API: `/api/agent` (Controller + AgentService)
- Static HTML: `src/main/resources/static/index.html`
  - Input, tool selection (default auto), result display
  - Client maintains context history (latest 20), sent with each request

### Phase 6: Testing (current status)
- Unit tests: model classes, `AgentController` covered
- Gaps: `ToolsService` method tests, end-to-end integration (Controller + ChatClient + real model), UI tests

---
## Tech Design

### Architecture (three layers + stateless)
```
Controller (REST, POST /api/agent)
    ↓
AgentService (single-turn logic + Spring AI ChatClient native tool invocation)
    ↓
ToolsService (@Tool annotated methods + PathValidator)
```
Client carries full context; server stores no session.

### ToolsService Behavior & Truncation Semantics
- readFile
  - Optional line range: startLine / endLine
  - Exceeds `max-file-lines` → truncation message `[TRUNCATED: showing first N lines, M more available]`
- listFiles (directory or glob)
  - Returns files & directories sorted
  - Count > `max-list-results` → truncation message `[TRUNCATED: first N items]`
- searchText
  - Outputs `file:line:snippet` (snippet max 100 chars)
  - Early stop reaching `max-search-results` → `[TRUNCATED: reached limit N before completing search]`
  - Exact equals limit after full traversal → no truncation
- replaceText
  - Constraints: old != new; old appears exactly once; path inside repo-root; file exists and is regular
  - Returns success summary (no diff)

### PathValidator
- Normalize input path, convert to realPath if exists
- Validate path starts with repoRoot or throw SecurityException

### AgentService (implemented)
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
Spring AI automatically performs: tool selection → parameter extraction → method invocation.

### Configuration (application.yml excerpt)
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
Risk & Troubleshooting: Current dummy-local with base-url http://localhost:4141 (no /v1); without a compatible OpenAI proxy/service native tool-calling (auto mode) will not function.

### Dependencies (pom.xml excerpt)
- spring-boot-starter-web
- spring-ai-starter-model-openai (version managed by spring-ai-bom)
- lombok
- spring-boot-starter-test (scope test)

### Known Issues / TODO
- Missing ToolsService unit tests (truncation, boundaries, error handling)
- Missing end-to-end integration tests (Controller + ChatClient + real model)
- Truncation message examples need unified presentation in UI & docs
- When history is empty AgentService may still inject default summary (e.g. "No previous context."); recommend only injecting when history exists

### Key Decisions
- Stateless server to reduce persistence complexity
- Single tool execution to avoid parallel/locking complexity
- Truncation prioritized (avoid overly long responses)
- replaceText enforces unique match to prevent broad unintended edits
- Use Spring AI native tool-calling (@Tool annotations) for simplicity & natural language support

---
## Reference Mapping
- PathValidator → path safety boundary
- ToolsService.readFile → line range + truncation
- ToolsService.searchText → unified truncation semantics
- ToolsService.listFiles → limit + glob safety + truncation message
- ToolsService.replaceText → uniqueness constraint
- AgentService → native tool invocation
- AgentController → REST entry point

---
## Risks
| Risk | Status | Mitigation |
|------|--------|------------|
| Missing integration tests | Only unit tests | Add end-to-end integration tests |
| Missing ToolsService tests | No tool method tests | Add positive/negative cases |
| replaceText overlapping matches | Theoretical risk | Maintain uniqueness constraint; add detection if misuse appears |
| Proxy/model dependency | dummy-local + placeholder local proxy | Provide explicit "direct tool mode" as future fallback |
