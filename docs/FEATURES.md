# FEATURES: Educational Simple Coder Agent (Integrated ReAct Multi-step Execution)

Scope
- Educational use; **in progress** multi-step execution with ReAct pattern; teaching focus on task decomposition and reasoning transparency; session context retained client-side; operations constrained to repository root; minimal UI (TODO panel planned, not yet implemented).

Current Implementation Status
- Tools implemented: unified ToolsService exposing file operation @Tool methods (readFile, listFiles, searchText, replaceText) with PathValidator for path safety.
- ReAct Loop Components: Integrated multi-step loop via AgentService → AgentLoopService (exception handling, termination checks STEP_LIMIT/COMPLETED, aggregation).
- Data Structures: ExecutionStep (5 fields: stepNumber, actionPrompt, toolName, resultSummary, tasksSnapshot) and ExecutionContext (4 fields: stepCount, maxSteps, terminated, reason) complete with comprehensive tests.
- Planned: TODO management (@Tool methods), bash/powershell tools, UI TODO panel.
- Implemented: Controller `/api/agent`, minimal UI (client context history up to 20 entries, stored in full).
- Truncation semantics implemented:
  - readFile: exceeds `max-file-lines` → `[TRUNCATED: showing first N lines, M more available]`
  - listFiles: count exceeds `max-list-results` → `[TRUNCATED: first N items]`
  - searchText: reaches `max-search-results` before full traversal → `[TRUNCATED: reached limit N before completing search]` (exactly equals limit with full traversal → no truncation)
- Note: Backend now processes requests via integrated multi-step loop; single-step behavior achievable by setting `simple-coder.agent.max-steps=1`. Tool selection & parameter extraction still driven by Spring AI tool-calling. UI dropdown remains advisory.
- Not implemented: TODO system, bash/powershell tools, ToolsService unit tests, end-to-end integration tests (Controller + ChatClient + real model).

Feature List (Target Capabilities)
- **ReAct Loop (In Progress)**: Multi-step execution with Reasoning → Acting → Observing cycle (max 10 steps). AgentLoopService (standalone loop skeleton) fully implemented with exception handling, termination checks, and result aggregation; AgentService integration pending.
- **TODO System (Planned)**: In-memory task management; agent can create/update/complete tasks; exposed via @Tool methods and UI panel.
- **File Operations (Implemented)**:
  - read_file: read file text (optional line range).
  - list_dir / glob: list directory or pattern-matched files & directories (sorted output).
  - search (regex/contains): return `file:line:snippet` (snippet max 100 chars).
  - replace (exact, unique): exact string replacement requiring old != new and old appears exactly once.
- **Command Execution (Planned)**:
  - bash_command: execute shell commands (mvn, git, etc.); no whitelist for educational simplicity.
  - powershell_command: Windows PowerShell support (platform-specific).
- **Session context (Implemented)**: client maintains last 20 request/result summaries, sent with each request; server stateless.
- **Minimal UI (Partially Implemented)**: single page with input box + advisory tool dropdown + submit button + result area + history sidebar; loading/error states. TODO panel planned but not yet implemented.

Feature Details

## ReAct Loop (In Progress)
- **Implementation Status**: AgentLoopService (standalone loop skeleton) fully implemented and tested (100 lines). Sprint A tests: 32 total (AgentLoopService: 2 tests, Termination: 5 tests, Exception handling: 6 tests, ExecutionStep: 13 tests, ExecutionContext: 2 tests, AggregatedResultFormatter: 3 tests, AgentLoopProperties: 1 test). Components complete: exception handling (RecoverableException → continue, TerminalException → abort), termination checks (STEP_LIMIT, COMPLETED, SECURITY_VIOLATION), result aggregation (AggregatedResultFormatter), ExecutionStep tracking (5 fields), ExecutionContext state (4 fields). AgentService.process() integration pending.
- **Execution flow** (Design Target):
  1. Reasoning: Agent analyzes current state, context history, TODO list
  2. Acting: Selects and executes tool (file operation, bash command, TODO update)
  3. Observing: Collects tool result, updates TODO status if applicable
  4. Repeat until: task complete, step limit (max 10) reached, or error
- **Termination conditions** (Design Target):
  - All TODO items marked completed
  - Agent explicitly signals completion
  - Max steps (10) exceeded
  - Fatal error encountered
- **Step tracking** (Design Target): Each loop iteration increments step counter; exposed to agent via system message.

## TODO System (Planned)
- **Data model**: `TaskItem { id: int, content: string, status: "todo"|"in_progress"|"completed" }`
- **Storage**: In-memory list; cleared on app restart
- **Tool methods**:
  - `createTasks(List<TaskItem> tasks)`: Initialize task list for new request
  - `updateTaskStatus(int taskId, String newStatus)`: Update individual task status
  - `getTasks()`: Query current task list (read-only)
- **UI integration**: TODO panel displays live task list; auto-refreshes after each step
- **Teaching value**: Makes agent reasoning visible; shows task decomposition strategy

## File Operations (Implemented)
- read_file
  - Input: relative path; optional `startLine` / `endLine` (parsed from natural language by model).
  - Output: numbered lines; if selected range exceeds `max-file-lines` truncation message shown.
  - Failure: file missing / path escape / not regular file / invalid line numbers.
- list_dir / glob
  - Input: directory path or glob pattern (Java PathMatcher glob).
  - Output: sorted relative paths (files & directories); exceeding `max-list-results` appends `[TRUNCATED: first N items]`.
  - Failure: directory missing / path escape / input points to file.
- search (contains / regex)
  - Input: pattern, searchPath, optional `isRegex`, `caseSensitive`.
  - Output: `file:line:snippet` list (snippet >100 chars truncated); early stop with limit appends `[TRUNCATED: reached limit N before completing search]`; exact limit after full traversal → no truncation.
  - Failure: path missing / regex syntax error / path escape.
- replace (exact)
  - Input: `filePath`, `oldString`, `newString`.
  - Validation: old != new; old occurs exactly once; path safe; file exists and is regular.
  - Output: success summary (no diff returned).
  - Failure: not found / multiple occurrences / old==new / parse failure / escape.

## Command Execution (Planned)
- bash_command
  - Input: `command` (string), optional `workingDirectory` (defaults to repo root)
  - Output: stdout + stderr combined, exit code
  - Security: **No whitelist validation** (educational simplicity; trusts user)
  - Common uses: `mvn clean compile`, `mvn test`, `git status`, `git diff`
  - Timeout: 60 seconds default (configurable)
  - Platform: Unix/Linux/macOS/Git Bash on Windows
  - Failure: command not found / timeout / non-zero exit code (non-fatal; result returned to agent)
- powershell_command
  - Input: `command` (string), optional `workingDirectory`
  - Output: stdout + stderr combined, exit code
  - Platform: Windows only (tool unavailable on Unix/macOS)
  - Security: Same as bash - no whitelist
  - Common uses: Windows-specific tasks, COM automation
  - Failure: similar to bash_command

Example Natural Language Usage

## Single-tool Examples (Implemented)
- List Java files: `List src/**/*.java`
- Search text: `Search pattern='AgentService' in src/main/java caseSensitive=false`
- Read file: `Read src/main/java/com/simplecoder/service/AgentService.java lines 1-40`
- Replace text: `Replace old='gpt-4.1' with new='gpt-4.1-mini' in src/main/resources/application.yml` (must be unique)

## Multi-step Examples (Planned: ReAct + TODO)
**Note: These examples demonstrate planned functionality, not yet available in current implementation.**

**Example 1: Build and fix compilation errors**
```
User: "Build the project and fix any compilation errors"

Agent reasoning:
1. Create TODO: ["Run mvn compile", "Analyze errors", "Fix errors", "Verify build"]
2. Step 1: Execute bash_command("mvn clean compile")
3. Observe: 3 compilation errors found
4. Update TODO: Add 3 subtasks for each error
5. Step 2-4: For each error:
   - Read relevant file
   - Identify issue
   - Replace erroneous code
   - Update TODO status
6. Step 5: Re-run mvn compile
7. Observe: Build success
8. Mark all TODO completed
```

**Example 2: Search and document findings**
```
User: "Find all usages of 'ChatClient' and create a summary file"

Agent reasoning:
1. Create TODO: ["Search for ChatClient", "Read each file", "Create summary"]
2. Step 1: searchText("ChatClient", "src/main/java", regex=false)
3. Observe: Found in 3 files
4. Step 2-4: Read each file to understand context
5. Step 5: Create summary.md with findings (using replaceText on new file)
6. Mark TODO completed
```

TODO / Diff Tracking
- [x] AgentLoopService (standalone loop skeleton) (Task R-6 - Complete: 100 lines, 2 tests)
- [x] Termination condition integration (Task R-7 - Complete: STEP_LIMIT, COMPLETED detection, 5 tests)
- [x] Exception handling integration (Task R-8 - Complete: RecoverableException, TerminalException, 6 tests)
- [x] Result aggregation format (Task R-9 - Complete: AggregatedResultFormatter with 3 tests)
- [x] ExecutionStep record (Task R-4 - Complete: 5 fields, 13 tests)
- [x] ExecutionContext record (Task R-3 - Complete: 4 fields, 2 tests)
- [x] Exception taxonomy design (Task R-2 - Complete: 9 classes, hierarchy documented)
- [x] Max-steps configuration (Task R-5 - Complete: AgentLoopProperties + application.yml, 1 test)
- [ ] AgentService multi-step integration (replaces single-turn process() with loop invocation)
- [ ] TODO system: TaskManager service with @Tool methods
- [ ] Bash tool: execute shell commands with timeout
- [ ] PowerShell tool: Windows platform support
- [ ] UI TODO panel: display task list, auto-refresh after each step
- [ ] ToolsService unit tests (validate truncation, boundaries, error handling)
- [ ] End-to-end integration tests (Controller + ChatClient + real model + ReAct loop)
- [ ] Unified truncation message examples (listed here; later consolidated in IMPLEMENTATION.md)
- [ ] replaceText overlapping substring risk evaluation (only if real misuse appears)

Notes
- All file operations constrained by PathValidator; replaceText enforces unique match for safety.
- Natural language support: user describes needs directly; model parses tool & parameters.
- Context history: maintained & sent by frontend automatically; user transparent.
- (Planned) ReAct loop teaching value: demonstrates how agents break down complex tasks, reason about progress, and adapt based on observations.
- (Planned) TODO system teaching value: makes invisible reasoning visible; helps students understand task decomposition strategies.
- (Planned) Bash tool security trade-off: no whitelist simplifies implementation for educational use; trusts user not to execute destructive commands.
- (Planned) Step limit (max 10): prevents infinite loops while allowing reasonable multi-step tasks; adjustable via configuration.
