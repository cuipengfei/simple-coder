# simple-coder

Educational Spring AI agent with ReAct pattern implementation.

## Collaboration Pattern: ROADMAP-Guided Dual-Agent Workflow

A concise pattern emerged for coordinating two coding agent instances under explicit user direction:

### 1. Single Source Planning
- `ROADMAP.md` is the canonical task ledger: immutable IDs (R-1…R-n) + Goal + Test Cases (RED first) + Verification + Boundaries.
- Status transitions (todo → done) are the only accepted progress signal; no ad‑hoc side notes elsewhere.

### 2. Explicit Task Carving
- User inspects gaps (e.g. missing AgentService integration) and commands insertion of new tasks (R-11, R-12) before coding.
- Each new task includes negative scope (“no TODO system”, “no schema change”) to prevent drift.
- Parallelism only after tasks are disjoint by file or concern (implementation vs tests/docs).

### 3. Deterministic Split
- Instance A (Implementation): structural code change (AgentService loop integration).
- Instance B (Validation + Sync): integration tests, doc status flips, DoD updates.
- Split is workload‑orthogonal (behavior vs evidence) to minimize merge collisions.

### 4. TDD Enforcement Per Task
- Each task: write failing tests first (or list of checks for analysis-only tasks) → minimal code → green → doc sync.
- Tests act as the merge contract; no “partial green” accepted.

### 5. Bounded Execution & Safety
- Boundaries section blocks scope creep (e.g. “no tasks snapshot yet”).
- Step limit + explicit termination reasons documented early so both agents share mental model.

### 6. Synchronization Rituals
- After green: ROADMAP status flip + DoD checkbox evaluation.
- Documentation updated only after corresponding code/tests land (prevents future tense drift).
- User arbitrates conflicts; agents do not negotiate directly.

### 7. Failure Handling Pattern
- RED phase failures are expected and explicitly labeled (not treated as regressions).
- Compilation/test breakages traced to signature drift or constructor changes → resolved before expanding scope.

### 8. Benefits
- Predictable parallelism (clear isolation rules).
- High auditability (all decisions anchored in ROADMAP diff + doc edits).
- Low coordination overhead: no real-time agent cross-talk required.

### 9. Reapplication Template
1. Declare task IDs in ROADMAP before coding.
2. Attach Goal / Test Cases / Boundaries.
3. Assign disjoint subsets (Impl vs Tests/Docs) to different agents.
4. Enforce RED → GREEN → DOC loop per task.
5. Flip status only after verification + doc alignment.

---
**Note for Second Agent Pass**: Append only if introducing a new coordination nuance (e.g. cross-sprint dependency handling); avoid repeating existing bullets.
