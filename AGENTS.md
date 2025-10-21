# AGENTS.md

> Unified operating specification for a Coding Agent performing code read/write / testing / review in this repository. Educational demonstration scenario: pursue "minimal viable + verifiable", avoid over‑engineering. Original Chinese narrative removed; all technical terms retained as in code.

## Goals & Scope
- Minimal educational Coding Agent; non‑production, no discussion of high concurrency or complex scaling.
- Single‑turn stateless: client sends full context each request (`ToolRequest.contextHistory`); server stores no session.
- At most one tool invocation per interaction (single automatic tool selection).

## Tech Stack & Dependencies
- Java 21, Spring Boot 3.5.6
- Spring AI 1.0.3 (`ChatClient` fluent API)
- Build: Maven (all dependencies defined strictly by `pom.xml`; do not assume new ones)
- Model name configuration: `spring.ai.openai.chat.options.model`; API key env var: `OPENAI_API_KEY`

## Architecture Overview
```
Client → POST /api/agent (ToolRequest JSON)
  AgentController
    → AgentService.process()
        → chatClient.prompt().tools(toolsService).call().content()
            (Spring AI auto: choose tool → extract parameters → invoke @Tool method)
                ToolsService (readFile | listFiles | searchText | replaceText)
                  → PathValidator (path safety)
Returns ToolResponse (success | error + optional data)
```
Traits: stateless / single tool / natural language driven.

## Core Models (package `com.simplecoder.model`)
- `ToolRequest`: `prompt`, `toolType` (deprecated kept for compatibility), `contextHistory`; methods: `validate()`, `buildContextSummary()`.
- `ToolResponse`: `success`, `message`, `data`, `error`; static factories: `success(...)`, `error(...)`.
- `ContextEntry`: `timestamp`, `prompt`, `result`; `getSummary()` for context compression.

## Service Components
- `ToolsService`: exposes four `@Tool` methods (selected automatically by Spring AI):
  - `readFile(filePath, startLine?, endLine?)` read file (optional line range).
  - `listFiles(path)` list directory or glob.
  - `searchText(pattern, searchPath, isRegex?, caseSensitive?)` text / regex search.
  - `replaceText(filePath, oldString, newString)` exact unique replacement.
- `PathValidator`: ensures all paths remain inside repository root; blocks escape.
- `AgentService`: validate request → build context summary → call `ChatClient` → capture exceptions and wrap as `ToolResponse.error(...)`.

## Tool Semantics & Resource Limits
- readFile: exceeding max lines → `"[TRUNCATED: showing first N lines, M more available]"`; empty file → `"empty file: 0 lines"`.
- listFiles: exceeding max count → `"[TRUNCATED: first N items]"`.
- searchText: hit result limit before full traversal → `"[TRUNCATED: reached limit N before completing search]"`; full traversal exactly equals limit is NOT truncation.
- replaceText: requires `oldString` occurrence count == 1; 0 or >1 → failure (uniqueness counted by non‑overlapping occurrences).

## Error & Return Conventions
- Internal tool validation failure (path escape / file missing / uniqueness not met) → return string prefixed `"Error: ..."`; wrapper handles it.
- Service layer exception unified wrapping: `ToolResponse.error("AgentService error", detail)`.
- Normal tool results returned via `ToolResponse.success("Tool execution result", data)`.

## Security & Hard Constraints
- File operations limited to repository root (no absolute paths, no upward escape like `..`).
- Single turn only; no multi-step planning / parallel tools / persistent memory.
- `replaceText` enforces unique match to avoid unintended edits.
 - Repository content policy: all committed files must be fully in English (no Chinese characters); interaction with user remains in Chinese per communication protocol.

## Truncation Strategy (Consistency)
- When line/item/result limits exceeded use standard prefix messages for test assertions and debugging.
- Do NOT add extra statistical fields to returns; natural language prefix suffices.

## Working Guidelines (Agent Behavior)
1. Before modification: read related classes + tests; confirm semantics; avoid unverified structural refactors.
2. Keep minimal viable: change only when needed for new tests or explicit defect fix.
3. No style debates: point out only substantive issues affecting understanding or correctness.
4. No unnecessary abstractions: defer abstraction; if potential extension, mark brief English comment `// extensibility: ...`.
5. Clarify ambiguous requirements; never implement on guess.
6. Dependencies/versions: confirm educational necessity before adding any library.

## Development Commands
- Build: `mvn clean compile`
- Test: `mvn test`
- Run: `mvn spring-boot:run` (requires `OPENAI_API_KEY`)
- Package: `mvn clean package`

## Testing Strategy
- Prioritize unit tests for existing models and tool classes; after logic changes add targeted minimal coverage tests.
- Assert truncation/error message prefix exact matches (stability).
- Avoid excessive tests for pure implementation details.

## Review Focus Points
- Functional correctness: behavior matches documented specification.
- Security boundaries: path validation, unique replacement, no leakage outside repository.
- Readability: clear concise naming; avoid redundant comments.
- Not in scope: micro‑performance, generalized abstraction, complex caching, parallel scheduling.

## Information Source Priority
1. `docs/` (sources / syntheses / references)
2. Current code and test actual behavior
3. If still unclear → ask user, then add clarification in `docs/` where appropriate

## Change Principles
- Root cause fix: address origin, avoid superficial patches.
- DRY: if logic repeats ≥2 times consider extraction; avoid over‑engineering one‑off cases.
- SOLID: keep class responsibilities single; do not mix multiple tool semantics into non‑tool classes.

## Execution Example (Request)
```json
POST /api/agent
{
  "prompt": "Read src/main/java/com/simplecoder/service/AgentService.java lines 1-40",
  "toolType": "auto",
  "contextHistory": []
}
```
Successful response example (truncation format per spec):
```json
{
  "success": true,
  "message": "Tool execution result",
  "data": "Read ... (lines 1-40 of X total)\n\n<file content>",
  "error": null
}
```

## Extension Notes (Out of Current Scope)
> Potential future evolution only: multi‑step planning, parallel tools, conversational memory, semantic cache, diff editing. Simplified implementation preserved for educational clarity.

---
This file is the root‑level unified specification; if subdirectories add more detailed AGENTS.md, the more specific file takes precedence.
