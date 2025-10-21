# Implementation & Technical Design Plan (ReAct Pattern with TODO System)

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
- `ExecutionStep` (stepNumber, action, observation, tasksSnapshot) — NEW: ReAct loop state

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

### ReAct Loop Implementation (AgentService)
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
