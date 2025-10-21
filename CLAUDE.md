# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

> Note: All repository files must be in English. User communication is in Chinese, but all code, comments, documentation, and commit messages must be in English only. Technical terms (class names, methods, dependencies, commands) retain their original English form.

## Scenario & Positioning
- Educational example, non‑production: aim for minimal changes that pass tests and meet functionality.
- Single‑turn stateless: client sends full context each request; server stores no session.
- Unsupported: planning, autonomous multi‑step flows, parallel tools, persistent memory.

## Tech Stack
- Java 21 + Spring Boot 3.5.6
- Spring AI 1.0.3 (ChatClient fluent API)
- Build: Maven

## Architecture Overview
```
Web UI (index.html) → POST /api/agent (ToolRequest JSON)
    ↓
    Controller (AgentController)
        → AgentService.process()
            → ChatClient.tools(toolsService) (Spring AI auto tool selection + parameter extraction)
            → ToolsService @Tool methods execute (readFile | listFiles | searchText | replaceText)
                → PathValidator path safety
                → File / directory / text operations
    ↓
Returns ToolResponse JSON (optional data)
    ↓
Web UI shows chat bubbles (user prompt + agent response)
```
Characteristics:
- Stateless: ToolRequest.contextHistory carries all history; server persists nothing.
- Single tool: exactly one tool invocation per request.
- Natural language: user sends prompt, model auto‑selects tool and extracts parameters.
- Chat UI: Tailwind CSS + Inter font, chat bubbles show history.

## Core Components
### Model Layer (com.simplecoder.model)
- ToolRequest: prompt, toolType (deprecated retained for compatibility), contextHistory; methods: validate(), buildContextSummary().
- ToolResponse: success, message, data, error; static factories: success(...) / error(...).
- ContextEntry: timestamp, prompt, result, getSummary() (for context compression).

### Configuration Layer (com.simplecoder.config)
- AiConfig: Configures ChatClient with SimpleLoggerAdvisor for basic request/response logging.
- SimpleLoggerAdvisor: Implements CallAdvisor and StreamAdvisor; logs user messages, truncated responses (200 chars), and token counts; set logging level to INFO to view logs.

### ToolsService (com.simplecoder.service.ToolsService)
Unified service exposing four @Tool annotated methods; Spring AI ChatClient invokes automatically:

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

Helper: PathValidator constrains all paths to repository root.

### AgentService (com.simplecoder.service.AgentService)
Processing flow (greatly simplified):
1. Validate ToolRequest.
2. Build context summary (if present).
3. Invoke ChatClient:
```java
String result = chatClient.prompt()
    .user(promptBuilder.toString())
    .tools(toolsService)  // register all @Tool methods
    .call()
    .content();
```
4. Spring AI auto: choose tool → extract parameters → invoke method → return result.
5. Catch exceptions and return ToolResponse.error("AgentService error", e.getMessage()).

## Tool Semantics & Resource Limits
- read: read file (optional line range); exceeding max lines → `[TRUNCATED: showing first N lines, M more available]`; empty file → `empty file: 0 lines`.
- list: list directory or glob; exceeding max count → `[TRUNCATED: first N items]`.
- search: regex or substring search; if limit reached before traversal completes → `[TRUNCATED: reached limit N before completing search]`.
- replace: exact unique replacement; old_string not found or occurrence count ≠ 1 → failure. Uniqueness counted by non‑overlapping occurrences (example: content "aaa" + old_string "aa" counts as 1).

## Error Handling
- Failure paths: model selection exception / tool execution exception → `ToolResponse.error(message, detail)`.
- `AgentService error` is unified wrapper layer.
- Internal tool errors (path escape, file missing, etc.) returned by tool method with "Error: ..." prefix.

## API Examples
POST /api/agent
Content-Type: application/json
Example request (natural language):
```
{
  "prompt": "search for any java file",
  "toolType": "auto",
  "contextHistory": []
}
```
Or structured format (compatibility):
```
{
  "prompt": "Read src/main/java/com/simplecoder/service/AgentService.java lines 1-40",
  "toolType": "auto",
  "contextHistory": []
}
```
Successful response example:
```
{
  "success": true,
  "message": "Tool execution result",
  "data": "Read ... (lines 1-40 of X total)\n\n<file content>",
  "error": null
}
```

## Testing Strategy
### Covered
- Model classes: ToolRequest (validation, context summary), ToolResponse (factories), ContextEntry (summary generation)
- AgentController: request validation, error handling, response format

### Not Covered (intentionally deferred for educational simplicity)
- ToolsService tool method unit tests (readFile, listFiles, searchText, replaceText)
- End‑to‑end integration tests with real ChatClient/model invocation
- Edge cases: replace overlapping match ("aaa" + "aa"), oversized contextHistory handling

### Testing API Manually
```bash
# Start server
mvn spring-boot:run

# Test listFiles tool (natural language)
curl -X POST http://localhost:8080/api/agent \
  -H "Content-Type: application/json" \
  -d '{"prompt":"list all java files","toolType":"auto","contextHistory":[]}'

# Test readFile tool (structured prompt)
curl -X POST http://localhost:8080/api/agent \
  -H "Content-Type: application/json" \
  -d '{"prompt":"Read src/main/resources/application.yml","toolType":"auto","contextHistory":[]}'

# Test searchText tool
curl -X POST http://localhost:8080/api/agent \
  -H "Content-Type: application/json" \
  -d '{"prompt":"search for AgentService in src/main/java","toolType":"auto","contextHistory":[]}'
```

## Common Maven Commands
```bash
# Clean and compile
mvn clean compile

# All tests
mvn test

# Single test class
mvn test -Dtest=ToolRequestTest

# Multiple test classes
mvn test -Dtest="ToolRequestTest,ToolResponseTest"

# Run application (default port 8080)
mvn spring-boot:run

# Run application with timeout (30 seconds for testing)
timeout 30 mvn spring-boot:run

# Kill existing process on port 8080 (Windows)
taskkill /F /FI "MEMUSAGE gt 0" | findstr "java"

# Package
mvn clean package
```

## Configuration Notes
Default config (application.yml) using local model:
```yaml
spring.ai.openai:
  api-key: dummy-local
  base-url: http://localhost:4141
  chat.options.model: gpt-4.1

server:
  port: 8080

logging:
  level:
    org.springframework.ai: DEBUG
    com.simplecoder.config.SimpleLoggerAdvisor: INFO
```
To use OpenAI official API, modify config or set environment variable:
- Windows: `set OPENAI_API_KEY=sk-xxx`
- Unix: `export OPENAI_API_KEY=sk-xxx`

Resource limit configuration:
- `simple-coder.repo-root`: ${user.dir} (Java system property, ensures file operations relative to project root)
- `simple-coder.max-file-lines`: 500 (max file lines read)
- `simple-coder.max-list-results`: 200 (list tool max results)
- `simple-coder.max-search-results`: 50 (search tool max results)

## Web UI Notes
Frontend chat‑style interface (`src/main/resources/static/index.html`):
- CDN dependencies: Tailwind CSS (styles) + Google Fonts Inter (font)
- Layout: chat application style; input at bottom; messages scroll above
- History: client JavaScript maintains contextHistory (max 20 entries) sent each request
- Message display: user messages blue bubble right; agent responses white bubble left
- Loading state: shows "Loading..." placeholder during request
- Shortcut: Ctrl/Cmd + Enter to send
- Stateless: refresh clears history (client memory only)

## Constraints & Non‑Goals
- No multi‑tool or parallel execution per request.
- No server‑side session persistence (no Redis/DB).
- File/path operations strictly limited to repo root (PathValidator).
- No large‑scale refactors unless for correctness fix.

## Future Extension Guidance
- New tool: add @Tool annotated method in ToolsService; Spring AI auto detects.
- Tool description should be clear for model selection.
- Parameter descriptions should be detailed for natural language extraction.
- Avoid unnecessary dependencies (mockito-core already covered by spring-boot-starter-test basics).

## Known but Deferred Edge Cases
- replace overlapping match ("aaa" + "aa") counts as 1; adjust only if stricter semantics required.
- Oversized contextHistory: client truncates; server processes as received.

## Communication Protocol
**IMPORTANT**: While all repository content (code, docs, comments) must be in English, when communicating with the user during conversation:
- Use **Chinese** for explanations, questions, and discussions
- Use **English** for technical terms (class names, methods, variables, library names)
- Use **English** for all code examples and file content
- Example: "我会修改 `AgentService.java` 文件中的 `process()` 方法来处理这个问题。"

End of file.
