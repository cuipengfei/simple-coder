# PRD: Educational Simple Coder Agent (Java + Spring AI)

Purpose & Positioning
- Educational: understand Coding Agent principles via minimal example (non‑production).
- Focus on concepts & methodology, not performance/usability.
- Core teaching objective: ReAct pattern (Reasoning → Acting → Observing loop) with transparent task management.

Audience
- Java learners, students, engineers/researchers interested in agentic systems.

Roles & Collaboration
- Gemini: planning/review.
- Claude Code: coding/testing.
- Flow: Gemini plan & review → Claude implement → Gemini retrospective.

Learning Outcomes
- Explain Agent, Tool, ReAct loop (teaching focus: multi-step task completion with reasoning).
- Describe minimal toolset boundaries (read/list/search/replace/bash, uniqueness, path safety).
- Understand task decomposition and progress tracking via TODO system.
- Produce traceable conclusions & reviews grounded in docs/ and DeepWiki.
- Recognize common risks (privilege escape, incorrect replace, excessive output, runaway loops) and minimal mitigations.

Current Status
- **Implemented**: Spring AI native ReAct loop via ChatClient with tools registration (autonomous tool calling, multi-turn reasoning, natural termination); four file tools (readFile, listFiles, searchText, replaceText) with PathValidator; stateless design; minimal UI; context history management.
- **In Progress**: TODO system + dynamic task snapshot integration; structured step-by-step reasoning exposure in UI.
- **Planned (Not Yet Implemented)**: In-memory TODO system with task management; bash/powershell command execution tools; UI TODO panel.
- The capabilities described below in "Scope" represent **design targets**. Refer to "Current Status" for what is actually available in the codebase today.

Scope (Educational Version - Design Target)
- Multi-step execution (Native): Spring AI's ChatClient handles ReAct pattern (Reasoning → Acting → Observing) internally via single `.call()` invocation. Framework autonomously decides tool usage, iteration count, and termination - no manual step counting needed.
- Session context (Implemented): retain brief message history (user request, model response, tool result summary) while app open; cleared on refresh; no cross-session persistence.
- Task management (Planned): in-memory TODO system; agent can create/update tasks to show reasoning and progress; exposed to user via UI; cleared on app restart.
- Minimal tools:
  - (Implemented) read_file, list_dir/glob, search (regex/contains), replace (exact, unique)
  - (Planned) bash tool (execute mvn/git commands, no whitelist for simplicity)
  - (Planned) powershell tool (Windows environment support)
  - (Planned) task_manager (create/update TODO items)
- Minimal UI:
  - (Implemented) Single-page; Input area (multiline textbox); Tool select dropdown (Auto/read/list/search/replace, default Auto); Execute button; Result area (monospace, copyable); Lightweight states (empty/loading/error banner); Note: backend ignores ToolRequest.toolType; selection is model-driven; dropdown is advisory.
  - (Planned) TODO panel (displays current task list with status: todo/in_progress/completed)

Non-Goals (Educational Simplicity)
- No parallel tool execution (sequential only for clarity).
- No complex validation loops, diagnostics tools, or automated error recovery beyond basic retry.
- No Docker/container isolation, environment sandboxing.
- No multi-model support, model switching.
- No accounts, persistent storage (TODO cleared on restart), permissions, authentication.
- No multi-page navigation, visual diff, advanced theming/accessibility.
- No cross-session memory (session-only context).
- No bash command whitelist/blacklist (trust user for educational simplicity).
- No CLI interactive modes (confirm/YOLO/human).
- No trajectory recording to files.
- No complex template systems (unless system prompts grow significantly).
- No progressive versioning (V0/V1/V2/V3 branches).

Key Principles
- Teaching first: present key concepts & observable outputs with minimal complexity.
- No blind inference: prioritize docs/, then DeepWiki; do not conclude from titles or assumptions.
- Concise communication: do requested work only; failure transparent (reason + minimal next step).
- Traceability: claims map to docs and source anchors.

Sample Learning Tasks
- (Implemented) Read & explain specific file segment (UI displays text result).
- (Implemented) Search occurrences of "Agent" in docs/ (list summary).
- (Implemented) Attempt a precise replacement: compare success/failure paths & messages.
- (Planned) Multi-step task: "Build the project and fix any compilation errors" (demonstrates ReAct loop + TODO management).
- (Planned) Observe TODO panel: how agent decomposes task, tracks progress, updates status.
- (Planned) Execute bash command: `mvn clean compile` and interpret output.

Success Criteria (Educational)
- Concept mastery: learner can verbally/in writing explain ReAct pattern, task decomposition, and tool boundaries.
- Task design: can propose 1–2 multi-step use cases with expected outcomes and task breakdowns.
- Risk awareness: proactively avoids path escape, non-unique replacement, runaway loops (step limits).
- UI usability: single page ≤6 primary controls; TODO panel shows task progress; results copyable; clear error messages; session context usable.
- Documentation alignment: answers trace back to docs/ & citation pages; concise expression.

Evaluation Methods
- Checklist self-audit: tick learning outcomes / tasks / risks / traceability / UI / context / TODO system.
- Short quiz: 3–5 questions covering ReAct pattern, task decomposition, tool boundaries, failure handling, UI/TODO behavior.
- Peer review: provide brief feedback on another learner's multi-step use case & task breakdown.

Milestones
- M1 (Docs): complete PRD, FEATURES, IMPLEMENTATION; sample multi-step tasks with expected outcomes & TODO states.
- M2 (Implementation draft): deliver ReAct loop, TODO system, bash tool, updated UI with TODO panel (Claude Code).
- M3 (Retrospective): refine wording, prompts & evaluation checklist based on usage feedback; validate step limits and termination conditions.

Limitations & Disclaimers
- For learning & demonstration only; not for production or sensitive data.
- No guarantees of quality or security; outputs are educational references.
- Current implementation status: Spring AI native ReAct loop fully functional via ChatClient tool registration; framework handles autonomous multi-turn reasoning and termination.
- Bash/powershell tools (Planned) will have no command whitelist: trusts user not to execute destructive commands (educational simplicity trade-off).
- Natural termination: Spring AI's ReAct loop terminates when the LLM determines the task is complete (no more tool calls needed). This provides more intelligent stopping than arbitrary step limits, though framework-level safeguards against infinite loops exist internally.
- TODO system (Planned) is in-memory only: cleared on app restart (no persistence for simplicity).
- Depends on available OpenAI-compatible service: application.yml currently dummy-local with base-url http://localhost:4141 (no /v1); without compatible proxy/service auto tool-calling mode will be unavailable.
