# PRD: Educational Simple Coder Agent (Java + Spring AI)

Purpose & Positioning
- Educational: understand Coding Agent principles via minimal example (non‑production).
- Focus on concepts & methodology, not performance/usability.

Audience
- Java learners, students, engineers/researchers interested in agents.

Roles & Collaboration
- Gemini: planning/review.
- Claude Code: coding/testing.
- Flow: Gemini plan & review → Claude implement → Gemini retrospective.

Learning Outcomes
- Explain Agent, Tool, single-turn flow (teaching focus: single tool invocation).
- Describe minimal toolset boundaries (read/list/search/replace, uniqueness, path safety).
- Produce traceable conclusions & reviews grounded in docs/ and DeepWiki.
- Recognize common risks (privilege escape, incorrect replace, excessive output) and minimal mitigations.

Scope (V0 Educational Version)
- Single turn: teaching objective is single tool invocation; uses Spring AI native tool-calling (backend not enforcing exactly one; model may choose 0/1/multiple); final output text; same session can reference prior context.
- Session context: retain brief message history (user request, model response, tool result summary) while app open; cleared on refresh; no cross-session persistence.
- Minimal tools: read_file, list_dir/glob, search (regex/contains), replace (exact, unique).
- Minimal UI:
  - Single-page.
  - Input area: multiline textbox (placeholder guidance).
  - Tool select: dropdown (Auto/read/list/search/replace, default Auto).
  - Execute button: Submit.
  - Result area: monospace, copyable; mark truncation/item limit when applicable.
  - Lightweight states: empty/loading/error banner.
  - Note: backend ignores ToolRequest.toolType; selection is model-driven; dropdown is advisory.

Non-Goals (V0)
- No parallelism, multi-step planning, complex validation, container/sandbox, multi-model.
- No accounts, persistent storage, permissions, multi-page navigation, visual diff, theming/accessibility advanced UI.
- No cross-session memory (session-only context).

Key Principles
- Teaching first: present key concepts & observable outputs with minimal complexity.
- No blind inference: prioritize docs/, then DeepWiki; do not conclude from titles or assumptions.
- Concise communication: do requested work only; failure transparent (reason + minimal next step).
- Traceability: claims map to docs and source anchors.

Sample Learning Tasks
- Read & explain specific file segment (UI displays text result).
- Search occurrences of "Agent" in docs/ (list summary).
- Attempt a precise replacement: compare success/failure paths & messages.
- Perform two light consecutive tasks using session context (e.g. search then read a hit).

Success Criteria (Educational)
- Concept mastery: learner can verbally/in writing explain core concepts & boundaries.
- Task design: can propose 1–2 minimal use cases with expected outcomes.
- Risk awareness: proactively avoids path escape & non-unique replacement.
- UI usability: single page ≤5 primary controls; results copyable; clear error messages; session context usable.
- Documentation alignment: answers trace back to docs/ & citation pages; concise expression.

Evaluation Methods
- Checklist self-audit: tick learning outcomes / tasks / risks / traceability / UI / context.
- Short quiz: 3–5 questions covering concepts, boundaries, failure handling, UI/context behavior.
- Peer review: provide brief feedback on another learner's use case & conclusions.

Milestones
- M1 (Docs): complete PRD, FEATURES, navigation, glossary & citations; sample tasks with expected textual outcomes.
- M2 (Implementation draft): deliver single-turn minimal capability, session context, minimal UI (Claude Code).
- M3 (Retrospective): refine wording, prompts & evaluation checklist based on usage feedback.

Limitations & Disclaimers
- For learning & demonstration only; not for production or sensitive data.
- No guarantees of quality or security; outputs are educational references.
- Depends on available OpenAI-compatible service: application.yml currently dummy-local with base-url http://localhost:4141 (no /v1); without compatible proxy/service auto tool-calling mode will be unavailable.
