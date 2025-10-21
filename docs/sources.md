# Documentation Production Plan (Docs Only, Avoid Title-Based Assumptions)

Source Links:
- https://deepwiki.com/x1xhlol/system-prompts-and-models-of-ai-tools?ref=ghuntley.com
- https://deepwiki.com/ghuntley/how-to-build-a-coding-agent
- https://deepwiki.com/SWE-agent/mini-swe-agent

Goals & Scope
- Produce only documents under docs/; do not infer content from titles or common knowledge; every point grounded in actual reading.

Process (Condensed)
- R1 Acquire & pre-read: record metadata (author, update time, link snapshot).
- R2 Close read & extract: quote key points by section, mark anchors/positions/timestamps.
- R3 Synthesize & questions: derive highlights & open issues from extracts; forbid unsubstantiated conclusions.
- R4 Cross-compare: consensus/divergence/gaps across the three sources.
- R5 Final draft & thematic navigation: create navigation & thematic pages based on actual reading results.

Existing Outputs (Named)
- sources/notes-system-prompts.md
- sources/notes-coding-agent.md
- sources/notes-mini-swe-agent.md
- syntheses/architecture-patterns.md
- syntheses/code-modification.md
- syntheses/open-questions.md
- references/glossary.md
- references/citations.md

Future Placeholders (Potential)
- interactive-modes.md (UI prototype done; summarize interaction modes: minimal UI, client context, truncation messaging & result presentation)
- validation-error-handling.md (post-tool execution validation strategies; pending integration tests)
- tool-systems.md (after expanding tool abstraction e.g., parallel or multi-step)
- task-planning-memory.md (if richer session context & planning added; may merge into open-questions or stand alone)

Milestones
- M1: Complete three notes (R1–R2).
- M2: Complete syntheses (R3–R4).
- M3: Complete references & thematic navigation (R5).
- M4: Add validation/interaction extension docs (dependent on implementation progress).

Quality Standards
- Traceable: every conclusion has source citation & anchor.
- No conjecture: avoid title/common knowledge inference; no unsupported implementation extensions.
- Concise & clear: short sentences & lists prioritized; unified terminology & link formatting.

Notes
- Original navigation README removed; navigation handled by FEATURES.md + this file.
