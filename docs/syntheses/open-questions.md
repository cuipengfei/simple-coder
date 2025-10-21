# Open Questions - Unresolved Issues & Gaps

This document captures issues insufficiently clarified across the three sources, contradictions, and research gaps; it also reflects the current implementation status (including listFiles limits and truncation semantics).

---
## 1. Architectural Design Issues

### 1.1 Practical impact of parallel execution
**Issue**: Same.dev claims 3–5x improvement with parallelism but lacks baseline; project currently chooses single-tool serial to avoid conflicts and complexity.
**Gap**: Real benefit data for small educational repositories; conflict detection strategies (write/write, write/read).
**Direction**: Construct synthetic tasks to compare response time; evaluate need for minimal "parallel read operations" implementation.

### 1.2 Message history trimming strategy
**Status**: Stateless server; client implements context history (latest 20) sent with each request; trimming/summarization specification missing.
**Direction**: Empirical threshold N=10–20; beyond threshold keep recent plus compressed summary; consider read‑only config endpoint exposing current limit.

---
## 2. Tool System Issues

### 2.1 replaceText overlapping match risk
**Status**: Enforces unique `old` match; future misuse with nested or overlapping patterns may require detection.
**Open Question**: Need pre‑scan indexing to verify continuity & singularity?
**Possible Strategy**: Use indexOf + lastIndexOf + equality count; on multiple occurrences return contextual snippets prompting user refinement.

### 2.2 listFiles large result test gap
**Status**: Implemented `max-list-results` + truncation message; missing unit tests for messaging and boundary (exactly equals limit vs exceeds).
**Direction**: Build temporary directory ≥ (N+5) files; assert truncation vs non‑truncation scenarios.

### 2.3 searchText truncation configurable granularity
**Status**: Snippet fixed at 100 chars; question whether user adjustment needed.
**Risk**: Increases complexity; educational version remains fixed.

---
## 3. Verification & Error Handling

### 3.1 Insufficient integration test coverage
**Status**: Missing end‑to‑end (Controller + ChatClient + tool) validation of auto path.
**Open Question**: Need degraded path when ChatClient fails/unavailable?

### 3.2 Glob safety boundary visualization
**Status**: Docs describe base path extraction & safety validation; missing example showing glob escape rejection message format.
**Direction**: Add example: `List ../../secret/**/*` → Security error.

---
## 4. Configuration & Extension

### 4.1 Unified exposure of truncation parameters
**Status**: max-file-lines / max-search-results / max-list-results separated; UI not displaying these currently.
**Open Question**: Aggregate into single config structure returned to frontend?
**Direction**: Add read‑only config endpoint (evaluate with future UI implementation).

### 4.2 Explicit "direct tool mode"
**Status**: Backend ignores toolType; relies fully on model for tool selection & parameter extraction.
**Open Question**: Add bypass mode for scenarios without configured provider/model to still demonstrate four tools?
**Direction**: New backend endpoint or parameter enabling explicit ToolsService method invocation.

---
## 5. Documentation Consistency & Maintenance

### 5.1 Truncation message format standard
**Status**:
- searchText uses `[TRUNCATED: reached limit <N> before completing search]`
- listFiles uses `[TRUNCATED: first N items]`
- readFile uses `[TRUNCATED: showing first N lines, M more available]`
**Open Question**: Unify format vs keep semantic differences (early stop reason vs simple list clipping vs line count clipping).
**Direction**: Retain differences to highlight semantic distinction per operation.

### 5.2 Terminology & naming consistency
**Status**: Historical docs still contain older names like "ListDirTool".
**Direction**: Standardize to implementation terms: listFiles, readFile, searchText, replaceText.

---
## 6. Lower Priority Future Topics (monitor only)
- Parallel read operations (only if real performance bottlenecks emerge)
- replaceText multi‑point batch edit mode (outside educational scope)
- Post‑truncation pagination (currently fixed to first N)
- Enhanced prompt engineering (current minimal auto naming sufficient)
- UI improvements: show current truncation thresholds; explicit direct tool parameterization mode

---
## Priority Recommendations
| Task | Priority | Reason |
|------|----------|--------|
| Integration tests (auto) | High | Cover core path |
| listFiles truncation unit test | High | Current logic unverified |
| UI enhancement (threshold display / direct mode) | Medium | Demonstrates offline utility |
| Context summary strategy | Medium | Educational continuity |
| replaceText overlap detection | Low | No misuse cases yet |
| Truncation format unification decision | Low | Current differences explainable |

---
## References
- sources/notes-system-prompts.md
- sources/notes-coding-agent.md
- sources/notes-mini-swe-agent.md
- syntheses/architecture-patterns.md
- syntheses/code-modification.md

**Last Updated**: 2025-10-19 (synced with latest implementation status)
