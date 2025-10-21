# FEATURES: Educational Simple Coder Agent Functionality & Examples (includes minimal UI & session context)

Scope
- Educational use; single turn; teaching focus on single tool invocation; session context retained client-side; operations constrained to repository root; minimal UI implemented.

Current Implementation Status
- Tools implemented: unified ToolsService exposing four @Tool methods (readFile, listFiles, searchText, replaceText) with PathValidator for path safety.
- Implemented: AgentService (Spring AI native tool-calling), Controller `/api/agent`, minimal UI (client context history up to 20 entries, stored in full).
- Truncation semantics implemented:
  - readFile: exceeds `max-file-lines` → `[TRUNCATED: showing first N lines, M more available]`
  - listFiles: count exceeds `max-list-results` → `[TRUNCATED: first N items]`
  - searchText: reaches `max-search-results` before full traversal → `[TRUNCATED: reached limit N before completing search]` (exactly equals limit with full traversal → no truncation)
- Note: Backend ignores ToolRequest.toolType; tool selection & parameter extraction entirely driven by Spring AI tool-calling. UI dropdown is advisory only.
- Not implemented: ToolsService unit tests, end-to-end integration tests (Controller + ChatClient + real model).

Feature List (V0 Targets)
- read_file: read file text (optional line range).
- list_dir / glob: list directory or pattern-matched files & directories (sorted output).
- search (regex/contains): return `file:line:snippet` (snippet max 100 chars).
- replace (exact, unique): exact string replacement requiring old != new and old appears exactly once.
- Session context (implemented): client maintains last 20 request/result summaries, sent with each request; server stateless.
- Minimal UI (implemented): single page with input box + advisory tool dropdown + submit button + result area + history sidebar; loading/error states.

Feature Details
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

Example Natural Language Usage
- List Java files: `List src/**/*.java`
- Search text: `Search pattern='AgentService' in src/main/java caseSensitive=false`
- Read file: `Read src/main/java/com/simplecoder/service/AgentService.java lines 1-40`
- Replace text: `Replace old='gpt-4.1' with new='gpt-4.1-mini' in src/main/resources/application.yml` (must be unique)

TODO / Diff Tracking
- [ ] ToolsService unit tests (validate truncation, boundaries, error handling).
- [ ] End-to-end integration tests (Controller + ChatClient + real model).
- [ ] Unified truncation message examples (listed here; later consolidated in IMPLEMENTATION.md).
- [ ] replaceText overlapping substring risk evaluation (only if real misuse appears).

Notes
- All file operations constrained by PathValidator; replaceText enforces unique match for safety.
- Natural language support: user describes needs directly; model parses tool & parameters.
- Context history: maintained & sent by frontend automatically; user transparent.
