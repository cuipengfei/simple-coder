# Learning from Open Source Projects

This document summarizes key learnings from three open source coding agent projects and proposes enhancements for Simple Coder.

**Research Sources**:
1. **Mini SWE Agent** (Python) - Minimalist architecture with protocol abstraction
2. **Coding Agent** (Go) - Progressive tutorial with type-safe tool system
3. **System Prompts Collection** - Multi-platform prompt engineering analysis (Same.dev, Amp, VSCode Agent, Windsurf, etc.)

---

## 1. Key Learnings from Mini SWE Agent

### 1.1 Exception-Driven Control Flow

**What they do**:
- Use exceptions for control flow, not just error reporting
- `NonTerminatingException` (recoverable): FormatError, ExecutionTimeoutError → add to history, continue loop
- `TerminatingException` (terminating): Submitted, LimitsExceeded → return result, exit loop

**Why it's good**:
- Clear separation between "error but can continue" vs "must stop"
- Clean code structure without complex if-else chains

**Core implementation** (~100 lines):
```python
def run():
    while True:
        try:
            step()  # query → parse action → execute → check completion
        except NonTerminatingException as e:
            add_message("user", str(e))  # continue
        except TerminatingException as e:
            return (exit_status, result)  # done
```

**What we should learn**:
- No need for over-engineering; clear loop + exception handling is sufficient
- Recoverable errors become part of conversation history
- Clean termination conditions

<!-- todo: low priority, last to do -->

---

### 1.2 Template System (Jinja2)

**What they do**:
- All prompts are templates: `system_template`, `instance_template`, `action_observation_template`
- Runtime variable injection: `{{ returncode }}`, `{{ output }}`, `{{ max_output_length }}`
- Output truncation logic in templates:
  ```jinja2
  {% if output|length < max_output_length %}
  <output>{{ output }}</output>
  {% else %}
  <output_head>{{ output[:max_output_length//2] }}</output_head>
  <elided_chars>{{ output|length - max_output_length }}</elided_chars>
  <output_tail>{{ output[-max_output_length//2:] }}</output_tail>
  {% endif %}
  ```

**Why it's good**:
- Prompts are not hardcoded
- Easy to maintain and modify
- Reusable across different scenarios

**What we should learn**:
- Use Mustache (Java equivalent of Jinja2) for dynamic prompt generation
- Separate prompt logic from code logic
- Template-based truncation messages

<!-- todo: yes, but we do not have a lot of system instructions, what is there to templaterize? -->

---

### 1.3 Protocol Abstraction

**What they do**:
```python
class Agent(Protocol):
    def step() -> None: ...
    def run() -> tuple[str, str]: ...

class Model(Protocol):
    def query(messages: list) -> str: ...

class Environment(Protocol):
    def execute(command: str) -> tuple[str, int]: ...
```
- Define interfaces, swap implementations
- LocalEnvironment / DockerEnvironment / SingularityEnvironment all implement same protocol

**Why it's good**:
- Decoupled architecture
- Easy to test with mock implementations
- Extensible for different execution environments

**What we should learn**:
- Even for educational projects, interface abstraction helps understand architecture layers
- Could define `ToolExecutor` interface for future extensibility

<!-- todo: what is this? i do not get it -->

---

### 1.4 Interactive Modes

**What they do**:
- **Confirm mode**: User confirms each action before execution
- **YOLO mode**: Auto-execute all actions without confirmation
- **Human mode**: User directly inputs commands, bypassing LLM
- Mode switching commands: `/c`, `/y`, `/u`, `/h`

**Why it's good**:
- Different scenarios need different interaction styles
- Educational: Confirm mode lets students observe each step
- Demo: YOLO mode for smooth presentation
- Debug: Human mode for direct control

**What we should learn**:
- V3 should implement CLI with these modes
- Teaching scenario uses Confirm, demo uses YOLO
<!-- todo: no, no need -->
---

### 1.5 Trajectory Recording

**What they do**:
- Record full session: steps, tool calls, results, errors
- Save to `.traj.json` files
- Enable research analysis and debugging

**Why it's good**:
- Reproducibility
- Debugging support (replay session)
- Research evaluation (SWE-Bench integration)

**What we should learn**:
- V3 should add trajectory recording to `.simple-coder/trajectories/`
- Useful for teaching (show students what happened)
<!-- ok, but low priority later -->
---

## 2. Key Learnings from Coding Agent (Go)

### 2.1 Progressive Evolution Teaching Approach

**What they do**:
```
Chat Agent (basic conversation)
  ↓
+ Read Tool (read files)
  ↓
+ List Tool (list directories)
  ↓
+ Bash Tool (execute commands)
  ↓
+ Edit Tool (edit files)
  ↓
+ Code Search Tool (search code)
```
- Each version independently runnable
- Students see **capability growing step by step**

**Why it's good**:
- Lower cognitive load (one concept per version)
- Each milestone is demonstrable
- Clear progression narrative

**What we should learn**:
- Simple Coder should also split into V0/V1/V2/V3
- Each version teaches a new architectural concept
- Versioned evolution in FEATURES.md
<!-- todo: no, not this one -->
---

### 2.2 Type-Safe Tool Definition

**What they do**:
```go
type ToolDefinition struct {
    Name        string
    Description string
    InputSchema anthropic.ToolInputSchemaParam
    Function    func(json.RawMessage) (string, error)
}

// Auto-generate schema from struct
type ReadFileInput struct {
    Path string `json:"path" jsonschema_description:"Relative path to file"`
}

schema := GenerateSchema[ReadFileInput]()
```

**Why it's good**:
- Compile-time type checking
- Schema generation via reflection (no manual JSON)
- Clear input/output contracts

**What we should learn**:
- Spring AI already achieves this via `@Tool` + `@ToolParam` annotations
- We already have this advantage in Java
<!-- todo: already have, no need -->
---

### 2.3 Unified Tool Execution

**What they do**:
```go
// In response processing loop
for _, block := range message.Content {
    if block.Type == "tool_use" {
        tool := lookupTool(block.Name)
        result := tool.Function(block.Input)
        toolResults.append(ToolResultBlock{
            ToolUseID: block.ID,
            Content:   result,
        })
    }
}
```

**Why it's good**:
- Centralized tool invocation logic
- Consistent error handling
- Easy to add logging/metrics

**What we should learn**:
- Keep tool execution logic unified
- Spring AI handles this automatically, but we should understand the pattern
<!-- todo: no action needed for this -->
---

### 2.4 Whitelist Security Mechanism

**What they do**:
- Bash tool is not unrestricted execution:
  - Whitelist: mvn, git status, ls, cat, grep, find
  - Blacklist: rm, sudo, curl (dangerous operations)

**Why it's good**:
- Security without losing functionality
- Educational: students learn about attack surfaces

**What we should learn**:
- V1 must include `BashCommandValidator` when adding Bash tool
- Define clear whitelist in configuration
<!-- todo: yes, we should be able to run bash or powershell commands, but no whitelist needed yet, lets be simplistic -->
---

## 3. Key Learnings from System Prompts (Multi-Platform Analysis)

### 3.1 Parallel Execution Power

**What platforms do**:
- **Same.dev**: "DEFAULT TO PARALLEL...claims 3-5x speed"
- **Strategy**:
  - Read operations can be parallel: `readFile(A) + searchText(B) + listFiles(C)`
  - Write operations need serial or locking: avoid conflicts
  - Validation must be serial: run after modifications

**Example prompt**:
```
When gathering context information, call multiple read/search tools in parallel:
- Example: readFile(A) + searchText(B) + listFiles(C) simultaneously
- Only use sequential execution when later tools depend on earlier results
- Parallel execution can achieve 3-5x speedup
```

**Why it's good**:
- Significant performance improvement
- Teaches students about tool dependency analysis
- Real-world optimization pattern

**What we should learn**:
- V2 should introduce parallel tool calling via system prompt guidance
- Spring AI supports multiple tool calls in single response
- Document parallel vs sequential decision making
<!-- todo: no, too complex, no need -->
---

### 3.2 Task Management System (Amp's TODO)

**What Amp does**:
```yaml
todo_write([
  {id: 1, content: "Run the build", status: "in_progress"},
  {id: 2, content: "Fix error 1", status: "todo"},
  {id: 3, content: "Fix error 2", status: "todo"}
])

# Key principles
- Mark task as completed IMMEDIATELY after finishing, not batched
- When errors occur, decompose into specific subtasks
- Display progress in UI so user sees what agent is doing
```

**Why it's good**:
- Transparent agent reasoning
- Progress visibility for users
- Helps agent stay organized

**What we should learn**:
- V1 must add TODO system (foundation of agentic architecture)
- Persist to `.simple-coder/tasks.json`
- Web UI displays task list with status
<!-- todo: ok, but not to local file, keep todo items in memory of java process -->
---

### 3.3 Mandatory Validation Loop

**What all platforms enforce**:
- Modify code → MUST validate
  - Same.dev: `run_linter` after each edit
  - Amp: `get_diagnostics` after task completion
  - VSCode Agent: `get_errors` after file edit

**Error recovery strategy**:
```
1. Modify code
2. Run validation tool
3. If errors found:
   a. Only fix if you have clear solution
   b. Do not blindly guess
   c. Max 3 attempts
4. Still failing → ask user
```

**Why it's good**:
- Prevents cascading errors
- Teaches defensive programming
- Real-world development practice

**What we should learn**:
- V2 add `getDiagnostics` tool + validation loop logic
- System prompt enforces validation after edits
- Max 3 repair attempts before escalation
<!-- todo: low priority, later -->
---

### 3.4 Code Modification Placeholder Convention

**What platforms use**:
- Common placeholder: `// ... existing code ...`
- **Orchids.app** more explicit:
  ```javascript
  // ... keep existing code ...  // unchanged code
  const result = newValue;       // only this line changed
  // ... rest of code ...         // subsequent code
  ```

**Why it's good**:
- Avoid returning entire file (save tokens)
- Clear indication of what changed
- Easier for user to review

**What we should learn**:
- Document this convention in system prompt
- Add examples to FEATURES.md
<!-- todo: no, no need -->
---

### 3.5 Communication Strategy

**What platforms enforce**:
- **Conciseness**: "Do what has been asked; nothing more, nothing less"
- **No direct code output**: "NEVER output code directly to user, use tools instead"
- **Hide tool names**: "Never say the name of a tool like 'edit_file', describe action naturally"

**Example**:
- ❌ Bad: "I will use the edit_file tool to modify the code"
- ✅ Good: "I'll update the configuration to fix the issue"

**What we should learn**:
- Update system prompt for more natural agent responses
- Teach students about LLM communication best practices
<!-- todo: yes, good -->
---

## 4. Proposed Enhancement Priorities

Based on research from all three projects:

### **Priority 1 (V1 Must-Have)**

1. ✅ **Multi-step Agentic Loop** (from Mini SWE Agent)
   - No longer "single tool call", but loop until task completion
   - Exception-driven control flow
   - Teaching value: ReAct pattern (Reasoning + Acting)
<!-- todo: yes, lets do react pattern, this is what i want most -->
2. ✅ **Task Management (TODO)** (from Amp)
   - Let students see agent's "thinking process"
   - Task decomposition + progress tracking
   - Teaching value: Transparent reasoning, complex task breakdown
<!-- todo: yes, but be simple, not over complext, still want to be simplistic not over engineer, keep todo items inside java memory object for educational purpose unless keeping in file is easier to do -->
3. ✅ **Bash Tool + Whitelist Validation** (from Coding Agent)
   - Execute `mvn compile/test`
   - Security mechanism (whitelist)
   - Teaching value: Security considerations in agent systems
<!-- todo: yes, bash and powershell, but no whitelist needed for now -->
### **Priority 2 (V2 Nice-to-Have)**

4. **Parallel Execution** (from System Prompts)
   - 3-5x performance improvement
   - Teaching value: Tool dependency analysis, optimization strategies
<!-- no -->
5. **Template System** (from Mini SWE Agent)
   - Prompt maintainability
   - Dynamic generation (Mustache for Java)
   - Teaching value: Separation of concerns
<!-- only if we have lot of system instructions -->
6. **Diagnostics Tool + Validation Loop** (from all platforms)
   - Mandatory validation
   - Error recovery loop (max 3 attempts)
   - Teaching value: Defensive programming, reliability
<!-- low priority -->
### **Priority 3 (V3 Polish)**

7. **CLI Interactive Modes** (from Mini SWE Agent)
   - Confirm/YOLO/Human modes
   - Use Confirm for teaching, YOLO for demos
   - Teaching value: User interaction design
<!-- no -->
8. **Trajectory Recording** (from Mini SWE Agent)
   - Research-friendly
   - Debugging support
   - Teaching value: Observability, reproducibility
<!-- no -->
### **Should NOT Learn (Too Complex)**

- ❌ Docker/Singularity environment isolation (unnecessary for educational project)
- ❌ SWE-Bench batch evaluation (too specialized)
- ❌ Complex memory system (Windsurf's semantic database)
- ❌ Multi-agent parallelism (Amp's subagents)

---

## 5. Impact on Documentation

If we adopt these learnings, documentation changes needed:

### **AGENTS.md Changes**

- Remove "minimal viable" positioning → Change to "teaching-oriented progressive architecture"
- Add "Architecture Evolution" section (V0 → V3)
- Add "Learned Patterns" section (exception-driven, parallel execution, validation loop, etc.)
- Add comparison with research projects

### **FEATURES.md Changes**

- Restructure by version (V0/V1/V2/V3)
- For each version: teaching goals + new features + comparison with open source
- Add comparison matrix (Simple Coder vs Mini SWE Agent vs Coding Agent)
- Add V1+ examples (multi-step tasks)

### **IMPLEMENTATION.md Changes**

- Add "Architecture Decision Records" (Why single-tool? Why stateless? Why Java?)
- Split implementation plan by version (V0/V1/V2/V3 independent sections)
- Add "Learned Implementation Patterns" (code pattern comparisons)
- Add detailed comparison section with all three projects

---

## 6. Version Evolution Summary
<!-- todo: read above , ignore section 6, ignore all below -->
### V0 (Current - Baseline)
**Teaching**: Basic tool calling mechanism (Spring AI @Tool)
- Single-turn stateless
- 4 tools: read/list/search/replace
- Web UI only

### V1 (Agentic Loop)
**Teaching**: ReAct pattern, exception-driven architecture, task decomposition
- Multi-step execution loop
- Exception hierarchy (NonTerminating vs Terminating)
- Bash tool with whitelist security
- Task management (TODO system)

### V2 (Optimization)
**Teaching**: Parallel execution, tool dependency analysis, validation-driven development
- Parallel tool calling
- Mustache template system
- Diagnostics tool + validation loop
- Simple memory system

### V3 (Polish)
**Teaching**: Multi-modal interaction, observability, research-oriented development
- CLI interactive modes (confirm/yolo/human)
- Trajectory recording
- Batch processing

---

## 7. Open Questions for Discussion

1. **V1 Scope**: Should we implement full exception hierarchy in V1, or start with simpler step counter?

2. **Parallel Strategy**: Same.dev is aggressive (parallel by default), VSCode Agent is conservative (restricted parallel). Which approach for V2?

3. **Template System**: Mustache vs FreeMarker vs Thymeleaf for Java? Which is most educational?

4. **TODO Persistence**: JSON file vs embedded H2 database? Trade-off between simplicity and features.

5. **CLI Priority**: Is V3 CLI necessary for educational project, or should we focus V2 deeper (add more tools)?

6. **Version Independence**: Should each version be a separate branch, or sequential commits on main branch?

---

## References

- [Mini SWE Agent Research Notes](./sources/notes-mini-swe-agent.md)
- [Coding Agent Research Notes](./sources/notes-coding-agent.md)
- [System Prompts Research Notes](./sources/notes-system-prompts.md)
- [Architecture Patterns Synthesis](./syntheses/architecture-patterns.md)
- [Code Modification Strategies Synthesis](./syntheses/code-modification.md)

**Last Updated**: 2025-10-21
