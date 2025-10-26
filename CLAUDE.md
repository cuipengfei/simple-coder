# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

> All repository files (code, docs, comments, commit messages) must be English-only. User conversation is in Chinese, but technical terms and code remain in English.

## Scenario & Positioning
- Educational, non‑production example: minimize changes; focus on clarity + correctness.
- Multi-step ReAct loop (configurable via `simple-coder.agent.max-steps`): iterates reasoning/acting cycles until COMPLETED or step limit reached. Stateless server: ToolRequest.contextHistory fully provided each call; no session state.
- One tool invocation per loop iteration (Spring AI automatic tool selection). No multi‑tool orchestration within single step.
- No persistence layer (no DB/Redis); all prior context re‑supplied by client.

## Documentation-Driven Development
Four key docs in `docs/` (PRD, FEATURES, IMPLEMENTATION, ROADMAP). Always read relevant sections before changes.

## TDD Workflow (Critical)
1. Write failing tests first (or explicit analysis checklist for design-only tasks)
2. Human review of tests
3. Minimal implementation to green
4. Refactor
5. Docs sync
6. Mark task done in ROADMAP when DoD satisfied

## External Knowledge Sources
Use local docs first; MCP (`context7`, `deepwiki`) only for uncovered technical specifics (e.g. ProcessBuilder timeouts).

## Tech Stack
Java 21, Spring Boot 3.5.6, Spring AI 1.0.3, Maven.

## High‑Level Architecture
```
Frontend (index.html, chat UI)
    → POST /api/agent  (ToolRequest JSON)
        → AgentController.handle()
            → AgentService.process()
                → AgentLoopService.runLoop()  (R-11/R-12 integration)
                    → While (step < maxSteps && !terminated):
                        → SingleTurnExecutor (wraps ChatClient)
                            → ChatClient.prompt().tools(toolsService).call()
                                → Spring AI selects @Tool method (ToolsService)
                                    → PathValidator ensures repository‑root confinement
                                    → File / directory / text operations
                        → Collect ExecutionStep (stepNumber, toolName, resultSummary)
                        → Check termination signals (COMPLETED, STEP_LIMIT, exceptions)
                    → Return LoopResult (steps + aggregated output + finalContext)
    ← ToolResponse (success | error + aggregated result string)
Frontend renders user + agent messages (contextHistory maintained client‑side)
```
Key characteristics:
- Stateless server: ToolRequest.contextHistory fully provided each call.
- Multi-step loop: AgentLoopService orchestrates iterations; each step invokes ChatClient once.
- Automatic tool selection by LLM via descriptions and parameter metadata.
- Resource limits (line/file counts) enforced inside ToolsService.
- Aggregated output: <25 lines for 10 steps (current typical = 12 lines: header + 10 steps + total).

## Core Components
- SimpleCoderApplication: Spring Boot entry.
- AgentController: POST /api/agent → delegates to AgentService.
- AgentService: Builds combined prompt; delegates multi-step execution to AgentLoopService; returns aggregated output.
- AgentLoopService: Orchestrates loop, collects ExecutionStep, applies termination & exception taxonomy.
- SingleTurnExecutor: Functional seam for one ChatClient tool invocation.
- ToolsService: @Tool file operations (readFile, listFiles, searchText, replaceText) + truncation semantics.
- PathValidator: Repo-root confinement.
- AggregatedResultFormatter: Produces compact multi-step summary.
- Exception taxonomy: RecoverableException (continue) vs TerminalException (abort).

## Tool Semantics & Limits
- readFile: range truncation → `[TRUNCATED: showing first N lines, M more available]`.
- listFiles: count limit → `[TRUNCATED: first N items]`.
- searchText: early stop → `[TRUNCATED: reached limit N before completing search]`.
- replaceText: requires exactly one occurrence; otherwise `Error:` prefixed failure.

## Error Handling Pattern
- Tool failures return `Error:` prefix → converted to ToolResponse.error.
- RecoverableException message treated as observation; loop continues.
- TerminalException supplies termination reason; loop aborts.
- Catch-all in AgentService produces `AgentService error` response.

## Configuration & Runtime
`application.yml` keys: max-file-lines, max-list-results, max-search-results, agent.max-steps. Set OPENAI_API_KEY for real model endpoint.

## Frontend
Static HTML/JS; client retains last 20 interactions; sends full context each request; no server session state.

## Extending Functionality
1. New tool: @Tool method (concise description, explicit params) — MUST keep one ChatClient invocation per loop step.
2. Always path-validate user-supplied paths (PathValidator) before I/O.
3. Avoid persistence until roadmap explicitly expanded.
4. Preserve advisor ordering (SimpleLoggerAdvisor order=0) if adding advisors.
5. Add/update tests before changing aggregation size or truncation formats.

## Testing Strategy Snapshot
Covered:
- Loop: step limit & COMPLETED termination precedence
- Aggregation format & line bound (<25 lines; observed 12 for 10 steps)
- Exception taxonomy (recoverable vs terminal) control flow
- Core models & controller pathway
- AggregatedResultFormatter summarization contract

Deferred / Pending:
- ToolsService truncation & replace uniqueness unit tests
- End-to-end (real ChatClient + tool invocation path)
- TODO system integration (task snapshots & completion termination)
- Bash / PowerShell execution tests
- Structured step/task serialization (after TODO UI panel)

Contract:
- Any change increasing aggregated lines >12 or >25 bound requires updating tests first.

## Manual API Examples
```bash
# List Java files
curl -X POST http://localhost:8080/api/agent \
  -H "Content-Type: application/json" \
  -d '{"prompt":"list all java files","toolType":"auto","contextHistory":[]}'

# Read file segment
curl -X POST http://localhost:8080/api/agent \
  -H "Content-Type: application/json" \
  -d '{"prompt":"Read src/main/java/com/simplecoder/service/AgentService.java lines 1-40","toolType":"auto","contextHistory":[]}'

# Search pattern
curl -X POST http://localhost:8080/api/agent \
  -H "Content-Type: application/json" \
  -d '{"prompt":"search for AgentService in src/main/java","toolType":"auto","contextHistory":[]}'
```

## ROADMAP & Parallel Work
- All task creation/modification occurs in `ROADMAP.md` before coding.
- Parallel split: Implementation vs Tests/Docs for file isolation.
- Status flip (todo → done) only after tests green + docs synced + boundaries respected.

## Communication Policy (Runtime Interaction Only)
User-facing narrative in Chinese; code/identifiers/paths strictly English; no Chinese persisted in repo content.

## Extension Guardrails
- Maintain stateless contract; no server memory of prior interactions.
- No multi-tool chaining inside a single loop step.
- Preserve truncation message formats exactly.
- New tools must retain `Error:` failure signalling.
- One ChatClient call per step; deterministic side effects.

## When NOT to Change Things
- Do not reintroduce a single-turn shortcut; loop path is canonical.
- Avoid adding persistence/session state.
- Defer structured step/task JSON output until TODO system sprint.
- Do not expand aggregated output length without pre-updated tests.
- Avoid large refactors unless required for correctness/security.

## Common Maven Commands
```bash
mvn clean compile          # Compile
mvn test                   # Run all tests
mvn test -Dtest=ToolRequestTest                # Single test class
mvn test -Dtest="ToolRequestTest,ToolResponseTest"  # Multiple test classes
mvn spring-boot:run        # Run app (port 8080)
mvn clean package          # Build jar
```

End of file.
