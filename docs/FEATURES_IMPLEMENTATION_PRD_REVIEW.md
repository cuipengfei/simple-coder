# Review: FEATURES.md / IMPLEMENTATION.md / PRD.md (2025-10-21, Update After Git Diff Check)

Conclusion First
- Previous substantive issue (planned features described as implemented) has been addressed: both `FEATURES.md` and `PRD.md` now clearly mark ReAct loop, TODO system, bash/powershell tools as "Planned" / "Not Yet Implemented". A dedicated "Current Status" section exists in PRD; FEATURES title revised with "(Planned...)" wording. Misleading present-tense statements removed.
- IMPLEMENTATION.md already separated planned vs existing; remains consistent with updated wording; no changes required.
- No new substantive inaccuracies introduced by latest unstaged edits (diff inspection shows only addition of planned qualifiers).

Verification Summary
1. FEATURES.md
   - Title now: "Educational Simple Coder Agent (Planned: ReAct Pattern with TODO System)" — removes implication of current availability.
   - Sections label features with (Implemented) vs (Planned). Multi-step examples explicitly annotated as "planned" and not available yet.
   - Risk removed: learners will not assume loop/tools exist.

2. PRD.md
   - Added "Current Status" section enumerating implemented vs planned capabilities before "Scope". Scope now tags planned items. Tool list differentiates implemented file operations from planned bash/powershell/task_manager tools.
   - Limitations section reiterates single-turn reality. Clarity achieved.

3. IMPLEMENTATION.md
   - Continues to list missing components under Known Issues / TODO. Wording already future-oriented; still aligned.

Remaining Minor Points (Non-blocking, Optional)
- FEATURES.md checklist section retains items without (Planned) prefix under TODO / Diff Tracking; they are implicitly planned—clarity acceptable; optional to prefix each with "Planned" for strict consistency.
- PowerShell support marked planned; AGENTS.md shell policy prefers bash and forbids reliance on PowerShell syntax. Consider adding a note in PRD/FEATURES that PowerShell will follow bash policy constraints (avoid advanced PowerShell-only constructs) — optional, not required now.

No Further Action Needed
- Documentation state now accurately reflects implementation vs design targets.
- Security, truncation semantics, uniqueness constraints continue to match AGENTS.md.

End of review update.
