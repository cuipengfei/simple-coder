# Implementation & Technical Design Plan (ReAct Pattern with TODO System)

## Current Flow Summary (Multi-Step Integrated Implementation)

**Status**: Multi-step loop integrated (AgentService → AgentLoopService). Single-step mode achievable via maxSteps=1. TODO system / bash / powershell still pending.

### Request-Response Lifecycle

```
┌─────────────┐
│   Browser   │  (index.html)
│  (Client)   │
└──────┬──────┘
       │ POST /api/agent
       │ {prompt, toolType, contextHistory[]}
       ↓
┌─────────────────────────────────────────────────┐
│          AgentController.handle()               │
│  - Logs abbreviated request (first 120 chars)  │
│  - Delegates to AgentService.process()         │
└──────────────────┬──────────────────────────────┘
                   ↓
┌─────────────────────────────────────────────────┐
│          AgentService.process()                 │
│  1. Validate request (prompt non-empty)        │
│  2. Build context summary from history         │
│  3. Construct combined prompt:                 │
│     - Context History (if exists)              │
│     - User Request                             │
│  4. Invoke ChatClient with tools               │
│  5. Parse result (check "Error:" prefix)       │
│  6. Return ToolResponse                        │
└──────────────────┬──────────────────────────────┘
                   ↓
       ┌───────────┴───────────┐
       │                       │
       ↓                       ↓
┌──────────────┐      ┌────────────────┐
│ ChatClient   │      │  ToolsService  │
│ (Spring AI)  │─────→│   (@Tool)      │
│              │      │  - readFile    │
│ Configured   │      │  - listFiles   │
│ with         │      │  - searchText  │
│ ChatModel +  │      │  - replaceText │
│ Advisor      │      └────────┬───────┘
└──────────────┘               │
       │                       │
       │                  ┌────┴────────┐
       │                  │ PathValidator│
       │                  │  (safety)   │
       │                  └─────────────┘
       ↓
┌─────────────────────────────────────┐
│   LLM Processing                    │
│  1. Analyze prompt                  │
│  2. Select appropriate @Tool        │
│  3. Extract parameters from NL      │
│  4. Spring AI invokes tool method   │
│  5. Return result string            │
└──────────────────┬──────────────────┘
                   ↓
       ToolResponse {success, message, data, error}
                   ↓
       ResponseEntity<ToolResponse>
                   ↓
┌─────────────┐
│   Browser   │  Updates UI with result
└─────────────┘
```

### Data Flow & Dependencies

**Request Path**:
```
ToolRequest (model)
  ├─ prompt: String (user's natural language request)
  ├─ toolType: String (default "auto", ignored by backend)
  └─ contextHistory: List<ContextEntry> (client-maintained, max 20)
      └─ ContextEntry: {timestamp, prompt, result}
          └─ getSummary(): "User: X / Result: Y"
```

**Processing Dependencies**:
1. **ToolRequest.validate()** → throws IllegalArgumentException if invalid
2. **ToolRequest.buildContextSummary()** → formats history for LLM context
3. **ChatClient.prompt().user(combinedPrompt).tools(toolsService).call().content()** → returns String
4. **Error detection**: result.startsWith("Error:") → ToolResponse.error()
5. **Success**: ToolResponse.success(message, data)

**Tool Execution**:
```
ToolsService (@Tool annotated methods)
  ├─ Dependencies: PathValidator, @Value configs
  ├─ Injected limits: maxFileLines(500), maxListResults(200), maxSearchResults(50)
  └─ Error handling pattern:
      try { ... }
      catch (SecurityException e) { return "Error: Security violation - " + message }
      catch (IOException e) { return "Error: Failed to ... - " + message }
      catch (Exception e) { return "Error: " + message }
```

### Method Call Checklist

**AgentController.handle(ToolRequest)**:
- [x] Logs incoming request (abbreviated prompt)
- [x] Calls agentService.process(request)
- [x] Returns ResponseEntity.ok(response)

**AgentService.process(ToolRequest)**:
- [x] request.validate() - validates prompt and toolType non-empty
- [x] request.buildContextSummary() - formats history
- [x] StringBuilder for combined prompt construction
- [x] Conditional context append (if not blank)
- [x] chatClient.prompt().user().tools().call().content()
- [x] Result error prefix check ("Error:")
- [x] ToolResponse.error() or ToolResponse.success()
- [x] Exception catch-all → ToolResponse.error("AgentService error", e.getMessage())

**ChatClient.prompt() chain**:
- [x] .user(combinedPrompt) - sets user message
- [x] .tools(toolsService) - registers all @Tool methods
- [x] .call() - executes LLM request
- [x] .content() - extracts String result

**ToolsService @Tool methods** (common pattern):
- [x] PathValidator.validate(path) → Path (throws SecurityException)
- [x] Validation helpers (file exists, line ranges, etc.)
- [x] File I/O operations (Files.readAllLines, Files.walk, etc.)
- [x] Formatting helpers (line numbers, truncation messages)
- [x] Error wrapping: catch → return "Error: ..."

### Configuration Dependencies

**AiConfig.chatClient(ChatModel)**:
- [x] ChatClient.builder(chatModel)
- [x] .defaultAdvisors(new SimpleLoggerAdvisor())
- [x] .build()

**SimpleLoggerAdvisor** (CallAdvisor):
- [x] Logs user message (first 200 chars)
- [x] Logs token usage metrics (prompt tokens, generation tokens, total)
- [x] Order = 0 (runs first in advisor chain)

**PathValidator**:
- [x] Injected with @Value("${simple-coder.repo-root}")
- [x] normalize() → convert to absolute path
- [x] validate() → check within repo boundaries
- [x] Throws SecurityException if path escapes

### Key Characteristics (Current Implementation)

| Aspect | Current Behavior |
|--------|------------------|
| **Execution Model** | Single-turn: one request → one tool call → one response |
| **Statefulness** | Stateless server; client maintains contextHistory (max 20) |
| **Tool Selection** | Automatic by LLM (toolType ignored by backend) |
| **Error Handling** | String-based "Error:" prefix detection |
| **Concurrency** | Single-threaded (no parallel tool execution) |
| **Persistence** | None (no database, no file-based storage) |
| **Validation** | Input validation (ToolRequest); path safety (PathValidator) |
| **Truncation** | Resource limits enforced (500 lines, 200 files, 50 search results) |
| **Logging** | SLF4J + SimpleLoggerAdvisor for request/response tracking |

### Data Dependencies Map

```
ToolRequest ──validates──> IllegalArgumentException (if invalid)
            ──builds───> String (context summary)
            ──provides─> String (user prompt)
                           ↓
                    AgentService
                           ↓
                    ChatClient ←──configured── AiConfig
                           │                      ↓
                           │                ChatModel (injected)
                           │                SimpleLoggerAdvisor
                           ↓
                    LLM selects tool
                           ↓
                    ToolsService (@Tool methods)
                           │
                ┌──────────┴──────────┐
                ↓                     ↓
          PathValidator         @Value configs
          (repo-root)          (limits: lines, results, etc.)
                ↓                     ↓
          SecurityException      Resource truncation
          (if escape)            (partial results)
                           ↓
                      String result
                  ("Error:" or data)
                           ↓
                    ToolResponse
                  {success, message, data, error}
```

### Missing Features (Planned for ReAct Loop)

- ❌ Multi-step execution (while loop)
- ❌ ExecutionContext tracking (stepCount, terminated, reason)
- ❌ ExecutionStep recording (per-iteration state)
- ❌ TODO system integration (TaskManagerService)
- ❌ Termination condition detection
- ❌ Exception taxonomy integration (RecoverableException vs TerminalException)
- ❌ Bash/PowerShell tool execution
- ❌ Step aggregation and result formatting
- ❌ UI TODO panel

## Implementation Plan

### Phase 1: Project Setup
1. Spring Boot (Java 21, Maven) + Spring AI dependency
2. Configure application.yml (API key, repo root path, agent settings)
3. Directory structure: controller, service, model, tool

### Phase 2: Core Models
- `ToolRequest` (prompt, toolType, contextHistory) — stateless: history maintained client-side
- `ToolResponse` (success, message, data, error)
- `ContextEntry` (timestamp, prompt, result)
- `TaskItem` (id, content, status) — NEW: TODO system data model
- `ExecutionStep` (stepNumber, actionPrompt, toolName, resultSummary, tasksSnapshot) — ReAct loop iteration record [IMPLEMENTED]

### Phase 3: Tools (partially completed, extension needed)
1. `PathValidator` — path safety checks
2. `ToolsService` — unified service exposing file operation @Tool methods:
   - `readFile` — file read (line range + truncation)
   - `listFiles` — list directory / glob (limit `max-list-results` + truncation message)
   - `searchText` — regex / contains search (unified truncation semantics)
   - `replaceText` — exact unique replacement (old != new; must appear exactly once)
3. `TaskManagerService` — NEW: in-memory TODO management:
   - `createTasks(List<TaskItem> tasks)` @Tool
   - `updateTaskStatus(int taskId, String status)` @Tool
   - `getTasks()` @Tool (read-only query)
4. `BashToolService` — NEW: shell command execution:
   - `executeBashCommand(String command, String workingDirectory)` @Tool
5. `PowerShellToolService` — NEW: Windows PowerShell support:
   - `executePowerShellCommand(String command, String workingDirectory)` @Tool

### Phase 4: Agent Service & Controller (major refactor needed)
- **Current state**: Single-turn interaction (one request → one tool call → one response)
- **Target state**: ReAct loop implementation
  - Initialize: create ExecutionContext (step counter, TODO list, observations)
  - Loop (max 10 iterations):
    1. **Reasoning**: Build prompt with context + current state + TODO list + step count
    2. **Acting**: ChatClient.tools() → model selects tool → execute
    3. **Observing**: Capture tool result, update TODO if applicable, increment step
    4. **Check termination**: all TODOs completed / explicit finish / step limit / error
  - Return aggregated results + final TODO state
- Natural language support: user describes intent; model identifies tool & extracts parameters
- Error handling: recoverable errors continue loop; fatal errors terminate with message
- Note: backend currently ignores `ToolRequest.toolType`; tool selection fully model-driven; UI dropdown advisory only

### Phase 5: Minimal UI (partially implemented, TODO panel needed)
- REST API: `/api/agent` (Controller + AgentService)
- Static HTML: `src/main/resources/static/index.html`
  - Input, tool selection (default auto), result display
  - Client maintains context history (latest 20), sent with each request
  - **NEW**: TODO panel (displays task list from ToolResponse.data)
  - **NEW**: Step-by-step display (show intermediate results from each ReAct iteration)

### Phase 6: Testing (current status + expansions needed)
- Unit tests: model classes, `AgentController` covered
- **NEW**: ReAct loop tests (step limits, termination conditions, error recovery)
- **NEW**: TaskManagerService tests (create/update/query tasks)
- **NEW**: BashToolService tests (command execution, timeout, exit codes)
- **NEW**: PowerShellToolService tests (Windows-only, platform detection)
- Gaps: `ToolsService` method tests, end-to-end integration (Controller + ChatClient + real model + full ReAct loop), UI tests

---
## Tech Design

### Architecture (four layers + stateless + ReAct loop)
```
Controller (REST, POST /api/agent)
    ↓
AgentService (ReAct loop orchestration)
    ↓ (loop: max 10 iterations)
    ├─ Reasoning: build prompt with context + TODO + step count
    ├─ Acting: ChatClient.tools() → model selects tool → invoke
    ├─ Observing: collect result, update state
    └─ Termination check: done? continue? limit?
    ↓
ToolsService + TaskManagerService + BashToolService + PowerShellToolService
    (@Tool annotated methods + PathValidator)
```
Client carries full context; server stores no session (TODO list is request-scoped only).

### Exception Design

**Taxonomy Hierarchy** (Implemented in R-2, integrated in R-8):
```
AgentException (base)
├── RecoverableException (continue loop with observation)
│   ├── ToolExecutionException     - Tool failed but loop can continue
│   ├── ValidationException        - Input validation failed
│   └── ResourceLimitException     - Hit resource limit (e.g., max search results)
└── TerminalException (abort loop immediately)
    ├── SecurityViolationException - Path escape, security breach
    ├── StepLimitExceededException - Max ReAct steps exceeded
    └── SystemException            - Fatal system errors
```

**Design Rationale**:
- **Recoverable**: Errors that provide feedback to agent; loop continues, exception message becomes observation
- **Terminal**: Fatal errors requiring immediate loop termination; security violations, resource exhaustion, infinite loop prevention

**Implementation Status (R-8 Completed)**:
- Exception classes: 8 classes in `com.simplecoder.exception` package (R-2)
- Loop integration: Implemented in AgentLoopService.runLoop() (R-8)

**Actual Usage Pattern** (AgentLoopService.java):
```java
for (int i = 1; i <= max; i++) {
    String raw;
    try {
        raw = executor.execute(initialPrompt);
    } catch (RecoverableException e) {
        // R-8: Recoverable exception - capture as observation, continue loop
        raw = e.getMessage();
    } catch (TerminalException e) {
        // R-8: Terminal exception - capture error, abort loop immediately
        raw = e.getTerminationReason() + ": " + e.getMessage();
        String summary = truncate(raw, 80);
        ExecutionStep step = new ExecutionStep(i, initialPrompt, "unknown", summary, "");
        steps.add(step);
        terminated = true;
        reason = e.getTerminationReason();
        break;
    }
    // ... continue with normal flow
}
```

**Exception Behavior**:
- **RecoverableException**:
  - Exception message becomes ExecutionStep.resultSummary
  - Step counter increments normally
  - Loop continues to next iteration
  - Final termination reason: STEP_LIMIT (if max steps reached)

- **TerminalException**:
  - Exception creates final ExecutionStep with error details
  - Loop terminates immediately (break)
  - Final termination reason: e.getTerminationReason() (e.g., "SECURITY_VIOLATION", "SYSTEM_ERROR")
  - No further steps attempted

### ReAct Loop Implementation (AgentService Integration - R-11/R-12)

**Implementation Status (Post R-11/R-12)**:
- AgentService.process() integrated with AgentLoopService for multi-step execution
- SingleTurnExecutor wraps ChatClient invocations
- Aggregated result formatted and returned via ToolResponse
- Multi-step integration tests added (6 test cases in AgentServiceMultiStepIntegrationTest)

**Actual Implementation Pattern** (R-11):
```java
public ToolResponse process(ToolRequest request) {
    request.validate();

    // Build context-aware prompt
    String contextSummary = request.buildContextSummary();
    String combinedPrompt = buildCombinedPrompt(contextSummary, request.getPrompt());

    // Create executor wrapping ChatClient
    SingleTurnExecutor executor = (prompt) -> {
        return chatClient.prompt()
            .user(prompt)
            .tools(toolsService)
            .call()
            .content();
    };

    // Execute multi-step loop
    AgentLoopService loopService = new AgentLoopService(properties, executor);
    LoopResult result = loopService.runLoop(combinedPrompt);

    // Return aggregated result
    return ToolResponse.success("Multi-step execution completed", result.aggregated());
}
```

**Key Design Decisions**:
1. **SingleTurnExecutor as Lambda**: Encapsulates ChatClient logic inline, avoiding new classes
2. **Aggregated Output**: `AggregatedResultFormatter.format()` produces concise multi-step summary (<25 lines for 10 steps)
3. **Context Handling**: Initial prompt built once with context history; reused across loop iterations (R-6 simplification)
4. **ToolResponse Mapping**: `aggregated` string placed in `data` field; `message` indicates completion status

**Design Target (Future Enhancements)**:
The following pseudocode represents full ReAct capabilities (deferred to future sprints with TODO system integration):

```java
public ToolResponse process(ToolRequest request) {
    request.validate();
    ExecutionContext ctx = new ExecutionContext(maxSteps=10);
    List<ExecutionStep> steps = new ArrayList<>();

    while (!ctx.isTerminated() && ctx.stepCount < ctx.maxSteps) {
        // 1. Reasoning: build prompt
        String prompt = buildPrompt(request, ctx, steps);

        // 2. Acting: invoke ChatClient
        String result = chatClient.prompt()
            .user(prompt)
            .tools(toolsService, taskManager, bashTool, powershellTool)
            .call()
            .content();

        // 3. Observing: record step
        steps.add(new ExecutionStep(ctx.stepCount, prompt, result, taskManager.getTasks()));
        ctx.incrementStep();

        // 4. Check termination
        if (taskManager.allCompleted() || result.contains("TASK_COMPLETE")) {
            ctx.terminate("success");
        }
    }

    if (ctx.stepCount >= ctx.maxSteps) {
        return ToolResponse.error("Step limit exceeded", formatSteps(steps));
    }
    return ToolResponse.success("Task completed", formatSteps(steps));
}
```

### ToolsService Behavior & Truncation Semantics
(No changes from current implementation - see existing docs)

### TaskManagerService (NEW)
```java
@Service
public class TaskManagerService {
    private final ThreadLocal<List<TaskItem>> taskList = ThreadLocal.withInitial(ArrayList::new);

    @Tool(description = "Create or replace the entire task list for current request")
    public String createTasks(
        @ToolParam(description = "List of tasks with id, content, status") List<TaskItem> tasks) {
        taskList.get().clear();
        taskList.get().addAll(tasks);
        return "Created " + tasks.size() + " tasks";
    }

    @Tool(description = "Update status of a specific task")
    public String updateTaskStatus(
        @ToolParam(description = "Task ID to update") int taskId,
        @ToolParam(description = "New status: todo, in_progress, or completed") String newStatus) {
        TaskItem task = findTaskById(taskId);
        if (task == null) return "Error: Task " + taskId + " not found";
        task.setStatus(newStatus);
        return "Updated task " + taskId + " to " + newStatus;
    }

    @Tool(description = "Get current task list (read-only)")
    public String getTasks() {
        return formatTasks(taskList.get());
    }

    public boolean allCompleted() {
        return taskList.get().stream().allMatch(t -> "completed".equals(t.getStatus()));
    }

    public void clearTasks() {
        taskList.remove(); // cleanup after request
    }
}

record TaskItem(int id, String content, String status) {}
```

### BashToolService (NEW)
```java
@Service
public class BashToolService {
    @Value("${simple-coder.repo-root}")
    private String repoRoot;

    @Value("${simple-coder.bash.timeout:60}")
    private int timeoutSeconds;

    @Tool(description = "Execute bash/shell command and return output")
    public String executeBashCommand(
        @ToolParam(description = "Shell command to execute") String command,
        @ToolParam(description = "Working directory (optional, defaults to repo root)", required = false)
        String workingDirectory) {

        Path workDir = resolveWorkDir(workingDirectory);
        ProcessBuilder pb = new ProcessBuilder("bash", "-c", command)
            .directory(workDir.toFile())
            .redirectErrorStream(true);

        try {
            Process process = pb.start();
            String output = readOutput(process.getInputStream());
            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();
                return "Error: Command timed out after " + timeoutSeconds + " seconds";
            }

            int exitCode = process.exitCode();
            return String.format("Exit code: %d\nOutput:\n%s", exitCode, output);
        } catch (Exception e) {
            return "Error executing command: " + e.getMessage();
        }
    }
}
```

### PowerShellToolService (NEW)
```java
@Service
@ConditionalOnOS(OS.WINDOWS)
public class PowerShellToolService {
    // Similar to BashToolService but uses "powershell.exe", "-Command"
    // Only registered as @Tool on Windows platform
}
```

### PathValidator
- Normalize input path, convert to realPath if exists
- Validate path starts with repoRoot or throw SecurityException
- Used by all file operations in ToolsService

### Configuration (application.yml excerpt)
```yaml
simple-coder:
  repo-root: ${user.dir}
  max-file-lines: 500
  max-search-results: 50
  max-list-results: 200
  agent:
    max-steps: 10  # NEW: ReAct loop step limit
  bash:
    timeout: 60    # NEW: command timeout in seconds
  powershell:
    timeout: 60    # NEW: PowerShell timeout

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

logging:
  level:
    org.springframework.ai: DEBUG
    com.simplecoder.config.SimpleLoggerAdvisor: INFO
```
Risk & Troubleshooting: Current dummy-local with base-url http://localhost:4141 (no /v1); without a compatible OpenAI proxy/service native tool-calling (auto mode) will not function.

### Dependencies (pom.xml excerpt)
- spring-boot-starter-web
- spring-ai-starter-model-openai (version managed by spring-ai-bom)
- lombok
- spring-boot-starter-test (scope test)

### Known Issues / TODO
- Missing ReAct loop implementation (AgentService refactor needed)
- Missing TaskManagerService implementation
- Missing BashToolService and PowerShellToolService
- Missing UI TODO panel and step-by-step display
- Missing ToolsService unit tests (truncation, boundaries, error handling)
- Missing end-to-end integration tests (Controller + ChatClient + real model + ReAct loop)
- Truncation message examples need unified presentation in UI & docs
- When history is empty AgentService may still inject default summary (e.g. "No previous context."); recommend only injecting when history exists
- Need to determine ReAct loop termination signals (agent convention for signaling "task complete")
- Need to decide ThreadLocal vs request-scoped beans for TODO storage

### Key Decisions

**Architecture Decisions**:
- Stateless server to reduce persistence complexity (TODO list is request-scoped only)
- ReAct loop for multi-step execution (vs single-turn) to teach task decomposition
- Sequential tool execution to avoid parallel/locking complexity (educational clarity)
- Truncation prioritized (avoid overly long responses)
- replaceText enforces unique match to prevent broad unintended edits
- Use Spring AI native tool-calling (@Tool annotations) for simplicity & natural language support

**Security/Safety Decisions**:
- **No bash command whitelist**: Educational simplicity; trusts user not to execute destructive commands. Trade-off: easier to understand vs production safety.
- PathValidator for file operations: essential safety boundary (prevents directory traversal).
- Step limit (max 10): prevents infinite loops; adjustable via config for different complexity needs.
- Timeout for bash/powershell (60s default): prevents hanging processes; non-zero exit codes treated as non-fatal (returned to agent for learning).

**Data Storage Decisions**:
- **TODO in ThreadLocal**: Simple in-memory storage; auto-cleanup after request; no persistence needed for educational use. Alternative considered: request-scoped beans (more Spring-idiomatic but adds complexity).
- Context history client-side: keeps server stateless; 20-entry limit client-enforced.

**Teaching-Oriented Decisions**:
- Prefer simplicity over production patterns (e.g., no whitelist, in-memory TODO)
- Expose step-by-step reasoning via TODO panel (makes invisible reasoning visible)
- No parallel execution (sequential is easier to trace and understand)
- No complex error recovery (basic retry only; encourages understanding of failure modes)

---
## Reference Mapping
- PathValidator → path safety boundary
- ToolsService.readFile → line range + truncation
- ToolsService.searchText → unified truncation semantics
- ToolsService.listFiles → limit + glob safety + truncation message
- ToolsService.replaceText → uniqueness constraint
- AgentService → ReAct loop orchestration + multi-step execution
- TaskManagerService → in-memory TODO management (@Tool methods)
- BashToolService → shell command execution (no whitelist)
- PowerShellToolService → Windows PowerShell support
- AgentController → REST entry point
- ExecutionContext → ReAct loop state (step counter, termination flag)
- ExecutionStep → individual loop iteration record (action + observation)

---
## Risks
| Risk | Status | Mitigation |
|------|--------|------------|
| Missing ReAct loop implementation | Not started | Implement while loop in AgentService with step counter |
| Missing integration tests | Only unit tests | Add end-to-end integration tests with real model |
| Missing ToolsService tests | No tool method tests | Add positive/negative cases |
| Bash command security (no whitelist) | **Accepted trade-off** | Educational use only; document trust assumption; PathValidator still protects file access |
| Step limit may truncate complex tasks | Configurable (default 10) | Adjustable via config; expose warning when limit reached |
| TODO storage in ThreadLocal | Implementation pending | Test cleanup behavior; consider request-scoped alternative if issues |
| ReAct loop infinite loops | Prevented by step limit | Max 10 steps hard limit; configurable |
| Bash timeout edge cases | Default 60s | Timeout configurable; test long-running commands |
| replaceText overlapping matches | Theoretical risk | Maintain uniqueness constraint; add detection if misuse appears |
| Proxy/model dependency | dummy-local + placeholder local proxy | Provide explicit "direct tool mode" as future fallback |
| PowerShell Windows-only | Platform detection needed | Use @ConditionalOnOS; graceful degradation on Unix/macOS |
| Agent may not use TODO system | Model behavior dependency | System prompt engineering; provide examples in docs |
