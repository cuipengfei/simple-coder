# ROADMAP: Incremental Implementation Plan (ReAct Loop, TODO System, Bash Tool)

> Purpose: Precise, verifiable, minimal steps enabling iterative delivery. All tasks scoped to educational objectives; avoid over-engineering. Each task lists: Goal, Investigation, Test Cases (write first), Verification, Documentation, Boundaries. File is English-only per repository policy.

## Conventions
- ID Prefixes: R=ReAct loop, T=TODO system, B=Bash tool, P=PowerShell (lower priority), U=UI, V=Validation/Tests, D=Docs sync, X=Cross-cutting.
- Status Values: todo | in_progress | done | blocked.
- Definition of Done (DoD): All listed Verification & Test Cases implemented; docs updated; no unrelated changes.
- MCP Usage: Use only when external knowledge truly required. Keep investigation minimal: "Review existing docs (PRD/FEATURES/IMPLEMENTATION)" unless external domain knowledge is essential.
- External MCP queries retained ONLY for: B-3 (ProcessBuilder timeout best practices), V-6 (LLM context size limits).
- Shell Writes Prohibited: All file edits via Edit/Write tools; Bash tool never used for repository mutation.

## Phase Ordering (Suggested Sprints)
1. Sprint A: Core loop skeleton (R) + basic data structures.
2. Sprint B: TODO system (T) + integrate with loop.
3. Sprint C: Bash execution (B) + safety formatting.
4. Sprint D: UI TODO panel (U) + response shaping.
5. Sprint E: Test expansion (V) + docs sync (D).
6. Sprint F: Optional PowerShell (P) + refinements.

## Task Format (Hierarchical List)
Each task uses the following structure:
```
### Task <ID>: <Title>
Status: <todo|in_progress|done|blocked>
Goal: <concise outcome>
Investigation: <"Review existing docs" or explicit MCP query>
Test Cases (Write First - Human Review Required):
1. <test name + behavior>
2. <edge cases>
Verification:
- <non-test verification steps>
Documentation:
- <doc updates>
Boundaries:
- <scope limits>
```

---

## Sprint A – ReAct Loop Foundations

### Task R-1: Analyze current AgentService
Status: todo
Goal: Map single-turn flow & data dependencies
Investigation: Review existing docs + AgentService.java
Test Cases (Write First - Human Review Required): None (analysis only) – human reviews output document
Verification:
- Checklist of method calls confirmed
Documentation:
- IMPLEMENTATION.md add "Current Flow Summary"
Boundaries:
- No code changes

### Task R-2: Exception taxonomy design
Status: todo
Goal: Plan recoverable vs terminal exceptions
Investigation: Review existing docs (no external needed)
Test Cases (Write First - Human Review Required): None (design only)
Verification:
- Draft hierarchy in DESIGN snippet passes compile (if using placeholder types)
Documentation:
- IMPLEMENTATION.md "Exception Design"
Boundaries:
- Avoid implementation; design only

### Task R-3: ExecutionContext record
Status: todo
Goal: Define fields: stepCount, maxSteps, terminated, reason
Investigation: Review existing docs
Test Cases (Write First - Human Review Required):
1. Construction defaults test
2. Max steps value accessible
Verification:
- `mvn compile` passes
Documentation:
- FEATURES.md mention context; IMPLEMENTATION.md add structure
Boundaries:
- No logic yet

### Task R-4: ExecutionStep record
Status: todo
Goal: Define fields: stepNumber, actionPrompt, toolName, resultSummary, tasksSnapshot
Investigation: Review existing docs
Test Cases (Write First - Human Review Required):
1. Field population test
Verification:
- Compile success
Documentation:
- IMPLEMENTATION.md append
Boundaries:
- Keep lean

### Task R-5: Config max-steps
Status: todo
Goal: Add `simple-coder.agent.max-steps` property
Investigation: Review existing config patterns
Test Cases (Write First - Human Review Required):
1. Property load test
Verification:
- application.yml loads; value injectable
Documentation:
- FEATURES/PRD update “Step limit planned” clarified
Boundaries:
- Do not enable loop logic yet

### Task R-6: Loop skeleton
Status: todo
Goal: While (step<max && !terminated) invoke existing single tool call; collect steps
Investigation: Review existing docs
Test Cases (Write First - Human Review Required):
1. Step count increments test (mock ChatClient)
2. Stops at max steps without termination reason
Verification:
- Manual run returns aggregated placeholder steps
Documentation:
- IMPLEMENTATION.md replace target state pseudo
Boundaries:
- No exception semantics yet

### Task R-7: Integrate termination checks
Status: todo
Goal: Support reasons: COMPLETED (placeholder), STEP_LIMIT
Investigation: Review existing docs
Test Cases (Write First - Human Review Required):
1. MaxSteps reached scenario
2. Placeholder COMPLETED path
Verification:
- Loop stops at maxSteps; last reason==STEP_LIMIT
Documentation:
- FEATURES.md ReAct section updated
Boundaries:
- No real completion detection yet

### Task R-8: Recoverable vs terminal exception handling
Status: todo
Goal: Catch and classify; continue vs abort
Investigation: Review existing docs
Test Cases (Write First - Human Review Required):
1. Recoverable exception continues
2. Terminal exception aborts
Verification:
- Simulated thrown exceptions behave per design
Documentation:
- IMPLEMENTATION.md update Exception section
Boundaries:
- Keep messages concise

### Task R-9: Result aggregation format
Status: todo
Goal: Decide output consolidation (concise step summaries)
Investigation: Review existing docs
Test Cases (Write First - Human Review Required):
1. Aggregated string contains all step indices
Verification:
- Output length below threshold (count lines)
Documentation:
- FEATURES examples annotated
Boundaries:
- Avoid verbose logs

### Task R-10: Documentation sync (loop)
Status: todo
Goal: Reflect partial loop state as “In Progress”
Investigation: Review existing docs
Test Cases (Write First - Human Review Required): None
Verification:
- grep confirms no “implemented” misstatements
Documentation:
- PRD/FEATURES updated statuses
Boundaries:
- Keep wording minimal

### Sprint A Definition of Done
- [ ] R-1 through R-10 tasks marked done
- [ ] ExecutionContext and ExecutionStep compile successfully
- [ ] Loop skeleton increments step counter correctly (tests pass)
- [ ] Termination checks stop at max steps (tests pass)
- [ ] Documentation status updated to "ReAct Loop (In Progress)"
- [ ] Human approval obtained for all test cases before implementation
- [ ] `mvn test` all tests green

---

## Sprint B – TODO System Integration

### Task T-1: Storage strategy decision
Status: todo
Goal: Choose ThreadLocal vs request-scope bean
Investigation: Review existing docs
Test Cases (Write First - Human Review Required): None (decision)
Verification:
- Decision rationale recorded
Documentation:
- IMPLEMENTATION.md choice logged
Boundaries:
- Simplicity first

### Task T-2: TaskItem model
Status: todo
Goal: Define fields id, content, status
Investigation: Review existing docs
Test Cases (Write First - Human Review Required):
1. Enum transitions test
Verification:
- Compile success
Documentation:
- FEATURES add model mention
Boundaries:
- Status values fixed

### Task T-3: TaskManagerService create/update/query
Status: todo
Goal: @Tool methods skeleton
Investigation: Review existing tool annotation examples
Test Cases (Write First - Human Review Required):
1. Create task
2. Update task status
3. Query tasks snapshot
Verification:
- Tools registered by ChatClient
Documentation:
- IMPLEMENTATION.md tool list
Boundaries:
- No persistence beyond request

### Task T-4: Validation rules
Status: todo
Goal: Status transitions allowed; reject unknown statuses
Investigation: Review existing docs
Test Cases (Write First - Human Review Required):
1. Invalid status rejected
2. Valid transition accepted
Verification:
- Error messages standardized
Documentation:
- FEATURES “TODO System (Planned)” refined
Boundaries:
- Simple error wording

### Task T-5: Integrate tasks into loop
Status: todo
Goal: Capture snapshot per step
Investigation: Review existing docs
Test Cases (Write First - Human Review Required):
1. Step output includes task states
Verification:
- Snapshot present
Documentation:
- IMPLEMENTATION.md ReAct loop updated
Boundaries:
- Avoid large payload

### Task T-6: Completion detection
Status: todo
Goal: All tasks status==completed triggers termination
Investigation: Review termination taxonomy
Test Cases (Write First - Human Review Required):
1. Loop terminates early when tasks complete
Verification:
- Early termination reason COMPLETED
Documentation:
- FEATURES termination conditions
Boundaries:
- Maintain step limit safety

### Task T-7: Documentation sync (TODO)
Status: todo
Goal: Update PRD/FEATURES status
Investigation: Review existing docs
Test Cases (Write First - Human Review Required): None
Verification:
- grep audit for misstatements
Documentation:
- Docs updated
Boundaries:
- Keep clarity

### Sprint B Definition of Done
- [ ] T-1 through T-7 done
- [ ] TaskItem & TaskManagerService compile & tests pass
- [ ] Loop shows task snapshots each step
- [ ] Completion detection test green
- [ ] Documentation updated for TODO system
- [ ] Human approvals for test cases

---

## Sprint C – Bash Tool

### Task B-1: Environment check
Status: todo
Goal: Confirm bash availability path
Investigation: Review existing docs
Test Cases (Write First - Human Review Required):
1. Conditional skip when bash absent
Verification:
- Manual invocation success or documented absence
Documentation:
- AGENTS.md Shell Policy cross-ref
Boundaries:
- Do not fallback to PowerShell

### Task B-2: Command signature design
Status: todo
Goal: Decide parameters (command, workingDirectory?)
Investigation: Review existing docs
Test Cases (Write First - Human Review Required): None (design)
Verification:
- Draft method signature compiled
Documentation:
- IMPLEMENTATION.md spec
Boundaries:
- Minimal args

### Task B-3: Process execution + timeout
Status: todo
Goal: Implement timeout (60s configurable)
Investigation: MCP (external) query: ProcessBuilder timeout best practices
Test Cases (Write First - Human Review Required):
1. Quick command returns output
2. Timeout triggers forced kill
Verification:
- Timeout behavior observed
Documentation:
- FEATURES command execution section
Boundaries:
- No file writes

### Task B-4: Output formatting
Status: todo
Goal: Standard: Exit code + Output
Investigation: Review existing tool output styles
Test Cases (Write First - Human Review Required):
1. Exit code reported
Verification:
- Format stable; no trailing spaces
Documentation:
- PRD sample updated
Boundaries:
- Avoid huge outputs

### Task B-5: Error pathways
Status: todo
Goal: Non-zero exit vs timeout vs spawn failure
Investigation: Review existing docs
Test Cases (Write First - Human Review Required):
1. Non-zero exit case
2. Timeout case
Verification:
- Messages prefixed consistently
Documentation:
- IMPLEMENTATION.md error section
Boundaries:
- No shell redirection writes

### Task B-6: Docs sync (bash)
Status: todo
Goal: Mark as implemented (post tests)
Investigation: Review existing docs
Test Cases (Write First - Human Review Required): None
Verification:
- grep confirm phrasing
Documentation:
- FEATURES/PRD status flip
Boundaries:
- Note no whitelist

### Sprint C Definition of Done
- [ ] B-1 through B-6 done
- [ ] Timeout & error handling tests green
- [ ] Output formatting consistent
- [ ] Documentation updated for Bash tool
- [ ] Human approvals for test cases

---

## Sprint D – UI TODO Panel

### Task U-1: Analyze current index.html
Status: todo
Goal: Identify insertion point
Investigation: Review existing index.html
Test Cases (Write First - Human Review Required): None (analysis)
Verification:
- DOM anchor selected
Documentation:
- IMPLEMENTATION.md UI section
Boundaries:
- Do not redesign layout

### Task U-2: Response schema extension
Status: todo
Goal: Decide transport of tasks/steps
Investigation: Review minimal JSON patterns
Test Cases (Write First - Human Review Required):
1. Serialization of ToolResponse block
Verification:
- ToolResponse includes structured block
Documentation:
- FEATURES UI section
Boundaries:
- Backward compatible

### Task U-3: Panel markup + style
Status: todo
Goal: Minimal div + list rendering
Investigation: Review existing docs
Test Cases (Write First - Human Review Required):
1. Panel renders sample tasks (DOM test or manual)
Verification:
- Panel renders sample tasks
Documentation:
- PRD UI description updated
Boundaries:
- Avoid heavy JS libs

### Task U-4: Refresh logic
Status: todo
Goal: Update panel after each response
Investigation: Compare existing history logic
Test Cases (Write First - Human Review Required):
1. Tasks update after response
Verification:
- Tasks update reliably
Documentation:
- IMPLEMENTATION.md flow
Boundaries:
- Keep few DOM ops

### Task U-5: Docs sync (UI)
Status: todo
Goal: Reflect implemented status
Investigation: Review existing docs
Test Cases (Write First - Human Review Required): None
Verification:
- grep audit
Documentation:
- PRD/FEATURES updated
Boundaries:
- No screenshots mandatory

### Sprint D Definition of Done
- [ ] U-1 through U-5 done
- [ ] UI panel updates tasks per step
- [ ] Serialization tests green
- [ ] Human approvals for test cases
- [ ] Documentation updated for UI panel

---

## Sprint E – Test Expansion & Validation

### Task V-1: Inventory existing tests
Status: todo
Goal: List coverage gaps
Investigation: Review existing tests
Test Cases (Write First - Human Review Required): None (inventory)
Verification:
- Gap list produced
Documentation:
- REVIEW doc section
Boundaries:
- Planning only

### Task V-2: ToolsService truncation tests
Status: todo
Goal: Validate truncation messages
Investigation: Review spec strings (AGENTS.md)
Test Cases (Write First - Human Review Required):
1. Read truncation
2. List truncation
3. Search truncation
Verification:
- Messages match exactly
Documentation:
- Testing Strategy updated
Boundaries:
- No extra assertions

### Task V-3: ReplaceText uniqueness tests
Status: todo
Goal: Edge: 0,1,>1 occurrences
Investigation: Review uniqueness rule
Test Cases (Write First - Human Review Required):
1. Success single occurrence
2. Zero occurrences failure
3. Multiple occurrences failure
Verification:
- All cases behave as spec
Documentation:
- IMPLEMENTATION.md mention
Boundaries:
- No diff outputs

### Task V-4: ReAct loop termination tests
Status: todo
Goal: Step limit & task completion
Investigation: Review termination reasons list
Test Cases (Write First - Human Review Required):
1. Limit reached reason
2. Early completion reason
Verification:
- Reasons correct
Documentation:
- FEATURES ReAct updated
Boundaries:
- Keep messages short

### Task V-5: Bash command tests
Status: todo
Goal: Non-zero exit + timeout
Investigation: Review safe commands set
Test Cases (Write First - Human Review Required):
1. echo success
2. false non-zero exit
3. sleep timeout
Verification:
- Controlled outputs
Documentation:
- Docs include examples
Boundaries:
- No destructive cmds

### Task V-6: Aggregated output size check
Status: todo
Goal: Prevent large response overflow
Investigation: MCP query: LLM context size limits
Test Cases (Write First - Human Review Required):
1. Multi-step size assert
Verification:
- Response length under threshold
Documentation:
- IMPLEMENTATION.md risk table
Boundaries:
- No streaming yet

### Sprint E Definition of Done
- [ ] V-1 through V-6 done
- [ ] All new tests green
- [ ] Aggregation guard active
- [ ] Human approvals for test cases
- [ ] Documentation synchronized

---

## Sprint F – Optional PowerShell Support

### Task P-1: Platform detection
Status: todo
Goal: Conditional bean load
Investigation: Review Java OS detection examples
Test Cases (Write First - Human Review Required):
1. Windows detection true
2. Non-Windows skip
Verification:
- Detection works
Documentation:
- FEATURES optional section
Boundaries:
- Skip if not Windows

### Task P-2: Execution method
Status: todo
Goal: Mirror bash format
Investigation: Review bash implementation
Test Cases (Write First - Human Review Required):
1. Simple command executes
Verification:
- Output aligned
Documentation:
- IMPLEMENTATION.md added
Boundaries:
- Same timeout

### Task P-3: Safety parity
Status: todo
Goal: Ensure no divergence from bash policy
Investigation: Review Shell Policy
Test Cases (Write First - Human Review Required):
1. Error cases parity
Verification:
- Messages consistent
Documentation:
- AGENTS.md cross-ref
Boundaries:
- Avoid pwsh-only syntax

### Task P-4: Docs sync (powershell)
Status: todo
Goal: Mark availability conditional
Investigation: Review existing docs
Test Cases (Write First - Human Review Required): None
Verification:
- grep audit
Documentation:
- PRD/FEATURES updated
Boundaries:
- Keep optional label

### Sprint F Definition of Done
- [ ] P-1 through P-4 done
- [ ] PowerShell feature conditional load verified
- [ ] Tests green for Windows path
- [ ] Documentation updated

---

## Cross-Cutting / Finalization

### Task D-1: Residual wording audit
Status: todo
Goal: Remove outdated “planned” once delivered
Investigation: Review docs
Test Cases (Write First - Human Review Required): None
Verification:
- grep for “Planned” after completion
Documentation:
- All docs status coherent
Boundaries:
- Avoid premature flips

### Task D-2: Risk table refresh
Status: todo
Goal: Update IMPLEMENTATION.md risk statuses
Investigation: Review docs
Test Cases (Write First - Human Review Required): None
Verification:
- Table cells reflect reality
Documentation:
- IMPLEMENTATION.md updated
Boundaries:
- Keep concise

### Task D-3: Final review pass
Status: todo
Goal: Ensure alignment across AGENTS / PRD / FEATURES / IMPLEMENTATION
Investigation: Review docs
Test Cases (Write First - Human Review Required): None
Verification:
- Checklist complete
Documentation:
- Review note appended
Boundaries:
- No scope creep

---

## Human Review Checkpoints
- After writing test cases: Human reviews test logic, assertions, edge cases
- After implementation: Human runs `mvn test` to verify all tests pass
- After documentation: Human reviews status updates for accuracy

## Update Workflow Per Task (TDD Aligned)
1. Investigation (if listed): Review docs; external MCP only when essential.
2. Write Test Cases First: Create failing test(s) that verify expected behavior; human approval required before implementation.
3. Implement Minimal Code: Write just enough code to make tests pass.
4. Refactor (if needed): Improve code quality while keeping tests green.
5. Documentation Sync: Update docs to reflect implemented status.
6. Review & mark task status.

## Verification Checklist (Global)
- No Chinese characters in committed files.
- No PowerShell-specific syntax unless inside conditional PowerShell feature tasks.
- All file edits via Edit/Write tools; no shell write operations.
- Truncation messages match AGENTS.md strings exactly.
- Tests isolate new logic (no broad unrelated assertions).
- Documentation always distinguishes Implemented vs Planned features.

## Exit Criteria (Roadmap Completion)
- ReAct loop functioning with termination & task integration.
- TODO system fully operational (create/update/query + completion detection).
- Bash tool stable (timeout, error, non-zero exit handling).
- UI displays evolving tasks per step.
- Core test suite covers truncation, uniqueness, loop termination, bash execution.
- Documentation free of outdated planned/implemented mismatches.

End of roadmap.
