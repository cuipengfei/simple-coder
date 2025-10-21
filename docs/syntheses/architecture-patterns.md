# Architecture Patterns

This document analyzes and compares core architectural patterns across the three sources.

---

## 1. Architectural Paradigm Comparison

| Dimension | Source 1 (System Prompts) | Source 2 (Coding Agent) | Source 3 (Mini SWE Agent) |
|------|---------------------------|-------------------------|---------------------------|
| **Core Model** | System prompt engineering driven | Event loop + tool registration | Three-component protocol (Agent/Model/Environment) |
| **Language/Stack** | Multi-platform (Python/TS/others) | Go + Anthropic SDK | Python + protocol abstraction |
| **Complexity** | High (multi-platform config) | Medium (~200–300 lines core) | Low (~100 lines core) |
| **Extensibility** | Config-driven | Slice of ToolDefinition | Protocol implementation |

---

## 2. Event Loop Design

### 2.1 Source 2: Go Event Loop

**Location**: `Agent.Run()` method

**Core Flow**:
```
1. Get user input (getUserMessage)
2. Append to conversation (conversation []MessageParam)
3. Call API (runInference)
4. Process response
   - Text blocks → display to user
   - tool_use blocks → execute tool, collect result
5. Send tool results back to model
6. Loop
```

**Characteristics**:
- Synchronous loop
- Full conversation persists (`conversation` slice)
- Tool results immediate feedback (as new user message)

### 2.2 Source 3: Agent.step() Loop

**Location**: `DefaultAgent.run()` + `step()`

**Core Flow**:
```
run():
1. Initialize message history (system + instance templates)
2. Loop calling step()
3. Catch exceptions (NonTerminating → continue, Terminating → exit)

step():
1. Query model
2. Parse action (extract bash code block)
3. Execute action (env.execute)
4. Check completion (has_finished)
5. Render observation (template)
6. Append to history (add_message)
```

**Characteristics**:
- Exception-driven control flow
- Template-based observation feedback
- No tool abstraction (bash commands only)
- Linear message history

### 2.3 Comparison Summary

| Feature | Source 2 (Go) | Source 3 (Python) |
|------|---------------|-------------------|
| **User Input** | Closure `getUserMessage` | Template initialization (no continuous input) |
| **API Call** | `runInference()` wrapper | `model.query()` protocol method |
| **Tool Execution** | Lookup `ToolDefinition` → execute `Function` | Parse bash block → `env.execute()` |
| **Result Feedback** | `ToolResultBlock` as user message | Observation text formatted via template |
| **Loop Control** | Infinite until user exits | Exception-driven exit |

---

## 3. Tool / Action Systems

### 3.1 Source 1: Tool Categories in Prompts

**Four Categories**:
1. **File operations**: create_file, edit_file, read_file, write_file
2. **Search/discovery**: grep, code_search, glob, semantic_search
3. **Execution**: bash, run_terminal, execute
4. **Validation**: get_diagnostics, run_linter, get_errors

**Characteristics**:
- Behavior defined via prompt text
- No code-level abstraction; relies on LLM interpretation
- Tool names & parameters inconsistent across platforms

### 3.2 Source 2: ToolDefinition Structure

**Go Struct**:
```go
type ToolDefinition struct {
    Name        string
    Description string
    InputSchema anthropic.ToolInputSchemaParam
    Function    func(json.RawMessage) (string, error)
}
```

**Characteristics**:
- Type safety: Go struct + JSON Schema auto generation
- Modular: each tool defined independently
- Unified execution: centralized lookup and invocation

**Example Tools**: read_file, list_files, bash, edit_file, code_search

### 3.3 Source 3: No Tool Abstraction

**Design Philosophy**:
- Bash commands only; no custom tools/functions
- Stateless execution: each action via separate `subprocess.run`
- Minimalism: core agent ~100 lines

**Action Flow**:
```
Model response → regex extract bash block → env.execute(command) → return (output, returncode)
```

### 3.4 Comparison Summary

| Dimension | Source 1 | Source 2 | Source 3 |
|------|----------|----------|----------|
| **Abstraction Level** | Prompt description | Go struct + Schema | None (bash native) |
| **Type Safety** | None | Strong (Go + Schema) | Weak (string commands) |
| **Extensibility** | Add prompt description | Add ToolDefinition | Change environment (Docker/Singularity) |
| **Validation** | LLM judgment | Schema validation | None (bash syntax only) |

---

## 4. Parallel Execution Strategies

### 4.1 Source 1: Multi-Platform Strategy Comparison

| Platform | Strategy | Key Instructions |
|------|------|----------|
| **Same.dev** | Aggressive parallel | "DEFAULT TO PARALLEL" (claims 3–5x speed) |
| **v0** | Context gathering parallel | Focus on read ops (GrepRepo, LSRepo, ReadFile) |
| **VSCode Agent** | Restricted parallel | Forbids parallel `semantic_search` & multiple `run_in_terminal` |
| **Amp** | Multi-level parallel | Parallel reads/searches/diagnostics/writes (serial on conflict)/subagents |
| **Windsurf** | No explicit directive | Relies on model judgment |

**Consensus**:
- Read operations broadly parallelizable
- Write operations require conflict detection
- Execution tools (bash/terminal) generally serial

### 4.2 Source 2 & 3: No Parallel Mechanism

 - **Source 2 (Go)**: Serial tool execution processing each `tool_use` block sequentially
 - **Source 3 (Python)**: Single action per `step()` execution

**Reasons**:
- Simplify implementation
- Avoid state conflicts
- Suited to single-user interaction scenario

---

## 5. Message History Management

### 5.1 Source 2: Go Slice

```go
conversation := []anthropic.MessageParam{
    anthropic.NewUserMessage(userInput),
    message.ToParam(),  // Claude response
    anthropic.NewUserMessage(toolResults...),
}
```

**Characteristics**:
- Full conversation history (user + model responses + tool results)
- Pass complete `conversation` each API call
- No trimming or summarization

### 5.2 Source 3: Python List

```python
self.messages = [
    {"role": "system", "content": system_template},
    {"role": "user", "content": instance_template},
    {"role": "assistant", "content": model_response},
    {"role": "user", "content": observation},
]
```

**Characteristics**:
- Linear message list
- Observations (command output) as user messages
- Append via `add_message(role, content)`

### 5.3 Source 1: Implicit Management

System prompts do not explicitly describe history management; assumed handled by underlying API/framework.

---

## 6. Error Handling & Exception Mechanisms

### 6.1 Source 1: Prompt-Level Error Recovery

**Mandatory Validation**:
- Same.dev: `run_linter` after edits, up to 3 fix cycles
- Amp: `get_diagnostics` forced post task completion
- VSCode Agent: `get_errors` after edit

**Escalation Strategy**:
- Escalate after 3–5 attempts
- Avoid blind guessing; fix only with clear plan

### 6.2 Source 2: Return-Value Errors

```go
func ToolFunc(input json.RawMessage) (string, error) {
    // ...
    if err != nil {
        log.Printf("Error: %v", err)
        return "", err
    }
    return result, nil
}
```

**Characteristics**:
- Go standard error return
- Errors logged
- `ToolResultBlock.IsError` flags error

### 6.3 Source 3: Exception-Driven

**Exception Hierarchy**:

```python
NonTerminatingException
├── FormatError            # model output format error
└── ExecutionTimeoutError  # command timeout

TerminatingException
├── Submitted              # task completed
└── LimitsExceeded         # step/cost limit reached
```

**Control Flow**:
```python
try:
    self.step()
except NonTerminatingException as e:
    self.add_message("user", str(e))  # continue
except TerminatingException as e:
    return (type(e).__name__, str(e))  # exit
```

**Characteristics**:
- Recoverable error → append, continue
- Termination → return exit status

### 6.4 Comparative Summary

| Source | Mechanism | Recoverable Errors | Termination |
|------|------|------------|----------|
| Source 1 | Prompt-guided iterative fixes | Lint errors, format errors | After 3–5 cycles |
| Source 2 | Return `error` value | Tool execution failure | User exits loop |
| Source 3 | Exception-driven | FormatError, ExecutionTimeoutError | Submitted, LimitsExceeded |

---

## 7. Configuration & Extensibility

### 7.1 Source 1: Prompt-Based Configuration

**Mechanism**:
- `.same/todos.md` (Same.dev)
- `AGENTS.md` (Amp)
 - `create_memory` tool (Windsurf)

**Characteristics**:
- Runtime mutable
- Platform-specific
- Natural language LLM-facing config

### 7.2 Source 2: Code-Level Extension

**Mechanism**:
- Add `ToolDefinition` to `Agent.tools` slice
- Auto-generate schema via `GenerateSchema[T]()`
- Progressive path: Chat → Read → List → Bash → Edit → CodeSearch

**Characteristics**:
- Compile-time type checking
- Requires rebuild
- Modular tool definitions

### 7.3 Source 3: Protocol Implementation

**Mechanism**:
- Implement `Agent`, `Model`, `Environment` protocols
- YAML config files (`agent`, `model`, `environment`, `run`)
- CLI parameter overrides

**Example**:
```yaml
environment:
  class: "DockerEnvironment"
  image: "sweagent/swe-bench:latest"

model:
  class: "AnthropicModel"
  name: "claude-3-5-sonnet-20241022"
```

**Characteristics**:
- Protocol-driven polymorphism
- Runtime YAML configuration
- No rebuild required

### 7.4 Summary Comparison

| Dimension | Source 1 | Source 2 | Source 3 |
|------|----------|----------|----------|
| **Config Location** | Filesystem (.md, .same/) | Code (Go slice) | YAML + CLI |
| **Change Cost** | Low (edit file) | High (recompile) | Low (edit YAML) |
| **Type Safety** | None | Strong | Medium (YAML validation) |
| **Extension** | Add prompt description | Add ToolDefinition | Implement protocol |

---

## 8. Key Insights

### 8.1 Architectural Trade-offs

| Aspect | Prompt Engineering (Source 1) | Tool Registration (Source 2) | Protocol Abstraction (Source 3) |
|------|---------------------|---------------------|---------------------|
| **Learning Curve** | Low | Medium | Medium-High |
| **Type Safety** | None | High | Medium |
| **Runtime Flexibility** | High | Low | High |
| **Performance Overhead** | Low (prompt parsing) | Medium (Go reflection) | Low (Python protocols) |
| **Debug Difficulty** | High (LLM unpredictability) | Medium | Low (clear execution path) |

### 8.2 Design Philosophy Differences

 - **Source 1**: Relies on LLM comprehension; behavior steered via prompting
 - **Source 2**: Strong typing & compile-time validation; production suitability
 - **Source 3**: Minimalist protocol-driven; suited for research & rapid iteration

### 8.3 Suitable Scenarios

| Scenario | Recommended Architecture | Rationale |
|------|----------|------|
| Rapid Prototyping | Source 1 or 3 | Low dev cost, fast iteration |
| Production System | Source 2 | Type safety, predictable behavior |
| Research Evaluation | Source 3 | Minimal implementation, easy modification & analysis |
| Multi-Environment Deployment | Source 3 | Protocol abstraction supports varied environments |

---

## References

- [Source 1: System Prompts](../sources/notes-system-prompts.md)
- [Source 2: Coding Agent](../sources/notes-coding-agent.md)
- [Source 3: Mini SWE Agent](../sources/notes-mini-swe-agent.md)

**Last Updated**: 2025-10-19
