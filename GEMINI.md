# GEMINI.md

Roles & Flow
- Gemini: planning / review (focus only on architectural alignment & substantive issues); Claude Code: implementation / testing (produce minimal viable code).
- Process: plan → code → review → iterate.
- Educational context: non‑production example; forbid over‑engineering / nit‑picking; skip style & micro‑optimizations; close loop quickly with minimal dependencies.

Educational Scope Declaration
- Repository is a "minimal educational Coding Agent" example, not a production system; review emphasis: functional correctness, spec alignment, minimal safety (path boundary, prevent mistaken replace).
- Out of review scope: high concurrency, large‑scale performance tuning, complex security hardening (unless directly impacting clarity or correctness).
- During code/test review: highlight only issues that hinder learning or cause clear errors/misleading outcomes; avoid pure production optimizations (micro performance, abstraction generalization, extensibility refactors).
- Simplified implementations (single pass file read, line matching, no diff preview) are acceptable; annotate potential extension points with brief comments if needed.

Baseline Facts (Current Repository)
- Tech stack: Java 21, Spring Boot 3.5.6, Spring AI 1.0.3 (spring-ai-bom), depends on spring-ai-starter-model-openai.
- Architecture: stateless server; client carries context via ToolRequest.contextHistory; no ConversationContext.
- Existing code: SimpleCoderApplication; models (ContextEntry / ToolRequest / ToolResponse) with unit tests; tools (PathValidator / ListDirTool / ReadFileTool / SearchTool / ReplaceTool) with tests; AgentService (auto tool selection + LLM call) and Controller (REST /api/agent) implemented; application.yml.
- Pending: minimal UI (single-page HTML/JS), end‑to‑end integration tests (E2E).

Current Open Issues (to address later)
1. (No blocking open issues presently)

Note: Model name gpt-4.1 already set in config; treated as valid for current educational scenario; keep docs consistent with key `spring.ai.openai.chat.options.model`.

Resolved Issues (kept for traceability, no longer open)
- SearchTool truncation semantics divergence: unified behavior (single file limit hit → truncation; directory full traversal equals limit → no truncation), test `testSearchDirectoryExactLimitNoTruncation` verifies.
- ReadFileTool line numbering & performance: fixed with index loop; test `testReadDuplicateLinesLineNumbersUnique` verifies.

Hard Constraints
- Single turn: at most one Tool per request; no parallel, no multi‑step planning, no session memory.
- Security: file operations limited to repo root; ReplaceTool enforces old_string unique match.
- Dependencies: must check pom.xml before any usage; do not assume adding.

Configuration Conventions
- simple-coder.*: repo-root, max-file-lines, max-search-results.
- Spring AI: `spring.ai.openai.chat.options.model`; API key env var OPENAI_API_KEY.
- Model name must be updated if actual availability changes (gpt-3.5-turbo may be unavailable).

Information Source Priority
1) docs/ (sources, syntheses, references)
2) DeepWiki original text (actual reading)
3) If still unclear ask user, then add anchor citation in docs/

Working Guidelines
- Point out only substantive issues; avoid nit‑picking.
- Read related code/tests/config before changes; keep minimal viable & verifiable.

Development Commands
- Build: mvn clean compile
- Test: mvn test
- Run: mvn spring-boot:run (requires OPENAI_API_KEY)
- Package: mvn clean package
