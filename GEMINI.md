# GEMINI.md

Concise guidance for Gemini (planning / review) role. Repository content must stay English-only (runtime user chat in Chinese; keep code terms English). Keep this file terse; defer details to PRD / FEATURES / IMPLEMENTATION / ROADMAP.

## 1. Role Boundary
- Gemini: scope control, architectural reasoning, doc alignment, risk/guardrail enforcement.
- Claude Code: implements minimal diffs + tests per approved plan.
- Never over-specify implementation minutiae already clear in docs/code.

## 2. Core Scenario
Educational, non-production coding agent demonstrating a multi-step ReAct loop (Reason → Act → Observe) over repository-scoped tools. Server remains stateless: every request supplies full contextHistory. No persistence (TODO system still pending). One tool invocation per loop step.

## 3. Architecture Snapshot
```
Browser (index.html, keeps last ≤20 interactions)
  → POST /api/agent (ToolRequest JSON)
    AgentController → AgentService → AgentLoopService
      loop(step < maxSteps && !terminated):
        SingleTurnExecutor → ChatClient.tools(ToolsService)
          @Tool (readFile | listFiles | searchText | replaceText)
            PathValidator (repo-root safety)
      AggregatedResultFormatter → ToolResponse (aggregated summary)
```
Constraints: single ChatClient call per step; deterministic side effects; aggregated output typical 12 lines (header + steps + total), hard bound <25 lines for 10 steps.

## 4. Tool & Output Contracts (MUST NOT CHANGE silently)
Truncation markers:
- readFile: `[TRUNCATED: showing first N lines, M more available]`
- listFiles: `[TRUNCATED: first N items]`
- searchText: `[TRUNCATED: reached limit N before completing search]` (exact limit with full traversal → no truncation message)
replaceText must find exactly one occurrence; otherwise return string beginning `Error:`.
All tool failures begin with `Error:` → mapped to error response semantics.

## 5. Loop & Exceptions
Step limit configurable (`simple-coder.agent.max-steps`, default 10). Termination reasons: COMPLETED (signal), STEP_LIMIT, plus TerminalException subclasses. Exception taxonomy:
- RecoverableException → message treated as observation; loop continues.
- TerminalException (SecurityViolationException, StepLimitExceededException, SystemException, etc.) → abort immediately.
Do not add new termination signals without updating docs + tests first.

## 6. Guardrails (Enforced)
- Stateless: no server memory beyond request scope.
- Exactly one tool invocation per loop iteration (no chaining/parallel inside a step).
- Preserve truncation strings verbatim.
- Maintain aggregated output line limits (<25 lines). Any increase requires prior test updates.
- No persistence layer, DB, or caching introduction without roadmap change.
- Path safety via PathValidator mandatory for any file path input.
- New tools must follow: single-action, deterministic, `Error:` prefix on failures.
- No silent dependency additions; justify in docs + pom.xml diff.

## 7. Planning & Review Workflow
1. Read relevant sections: ROADMAP task + PRD + FEATURES + IMPLEMENTATION.
2. Validate scope: reject scope creep; propose minimal viable path.
3. Enumerate test additions (fail-first) for any new/changed behavior.
4. Only then green-light implementation by Claude Code.
5. Post-implementation: verify tests, confirm contracts (loop line count, truncation strings, exception reasons) unchanged unless explicitly in scope.

## 8. When to Block / Request Clarification
Block if request would:
- Introduce persistence, multi-tool per step, parallelism, or broaden output length.
- Alter truncation or error message formats.
- Mix user-language (Chinese) into repo files.
- Add speculative features not on ROADMAP without explicit approval.

## 9. Testing Expectations (Current vs Gaps)
Covered: loop control flow, termination precedence, aggregation formatting, exception taxonomy, core models, controller pathway.
Pending (must precede related feature claims): ToolsService truncation & replacement uniqueness tests, TODO system tests (creation / completion termination), bash & powershell execution tests, end-to-end real model path.
Rule: Any behavioral change → add/update tests first; aggregation line count test must still pass (<25, typical 12).

## 10. Decision Summary (Do Not Revisit Without Cause)
- ReAct loop chosen for educational transparency over single-turn shortcut.
- Stateless design favors reproducibility and clarity.
- Unique-match replaceText prevents unintended bulk edits.
- No bash whitelist (documented educational trade-off) once shell tools arrive.
- Simplicity over production hardening (acceptable risk, documented in PRD).

## 11. Allowed Future Extensions (Roadmap-Gated)
- TODO system (@Tool) with per-step task snapshots and completion termination.
- Bash & conditional PowerShell tools (timeout, no whitelist).
- UI TODO panel + step display.
(Implement only when corresponding ROADMAP tasks move to in_progress.)

## 12. Reference Commands
```
mvn clean compile
mvn test
mvn test -Dtest=ToolRequestTest
mvn spring-boot:run
```

## 13. Review Checklist (Use Before Approving Changes)
- Scope matches ROADMAP task ID(s)?
- No new persistence or multi-tool chaining? 
- Truncation / error strings unchanged? 
- Aggregated output lines ≤ previous or justified with test update? 
- Tests added/updated for every new behavior? 
- Docs synced (statuses accurate, no stale "planned" where implemented)?

Keep this file concise. For rationale details, consult IMPLEMENTATION.md and ROADMAP.md.