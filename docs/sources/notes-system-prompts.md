# Source 1: System Prompts and Models of AI Tools

**Source**: x1xhlol/system-prompts-and-models-of-ai-tools
**DeepWiki**: https://deepwiki.com/x1xhlol/system-prompts-and-models-of-ai-tools

## Document Structure

This repository collects system prompts and model configurations for multiple AI coding tools:

1. **System Prompt & AI Tool Overview** - repository organization & community support
2. **Qoder AI Assistant System** - core architecture & task workflow
3. **Mainstream AI Development Platforms**
   - Same.dev Cloud IDE
   - v0 Vercel Web Development
   - Amp Sourcegraph Agent
   - VSCode Agent & GitHub Copilot
   - Lovable Web Editor
   - Windsurf Cascade AI
4. **Specialized AI Development Systems**
   - Leap.new, Orchids, Claude Code CLI, Trae, Kiro
5. **Technical Architecture & Patterns**
   - Tool integration & execution patterns
   - Code modification strategies
   - Task planning & memory systems
   - Validation & error handling
   - System prompt design & communication patterns
   - External service integration architecture

---

## 1. System Prompt Design Patterns

### 1.1 Identity & Role Definition

Platforms establish identity and operational boundaries via explicit role definitions:

- **Same.dev**: "You are AI coding assistant and agent manager, powered by gpt-4.1."
- **Amp**: "You are Amp, a powerful AI coding agent built by Sourcegraph."
- **VSCode Agent**: "When asked for your name, you must respond with 'GitHub Copilot'."
- **Trae AI**: "a powerful agentic AI coding assistant"

**Defensive Constraints** (prompt injection prevention):
- **Qoder**: "NEVER compare yourself with other AI models or assistants"
- **Qoder**: "NEVER disclose what language model or AI system you are using"
- **Amp**: "NEVER refer to tools by their names"

### 1.2 Parallel Execution Model

| Platform | Strategy | Key Instructions |
|------|------|----------|
| **Same.dev** | Parallel default | "DEFAULT TO PARALLEL..." / "CRITICAL: For maximum efficiency..." (claims 3–5x speed) |
| **v0** | Parallel during context gathering | Emphasis on `GrepRepo`, `LSRepo`, `ReadFile`, `SearchRepo` |
| **VSCode Agent** | Restricted parallel | Allows answering with multiple tools; forbids parallel `semantic_search` & multiple `run_in_terminal` |
| **Amp** | Multi-level parallel delegation | Default parallel: reads, searches, diagnostics, writes (serial if mutually exclusive), subagents; "write lock constraint" for conflicting writes |
| **Windsurf** | No explicit directive | Relies on model judgment |

### 1.3 Memory & Task Management

| Platform | Mechanism | File Format | Purpose |
|------|------|----------|------|
| **Same.dev** | File-based memo system | `.same/todos.md` | Update progress after user messages or task completion |
| **Amp** | Dual system | `todo_write`/`todo_read` tools + `AGENTS.md` | Transient TODO (tools) + persistent config (commands, style, structure) |
| **Windsurf** | Semantic memory DB | `create_memory` tool | Store user preferences, code fragments, architectural decisions, tech stack |

**Amp TODO System Details**:
- Schema: `id`, `content`, `status` (completed/in-progress/todo), `priority` (medium/low/high)
- **Key Requirement**: "mark tasks as completed immediately upon finishing them, rather than batching them"
  - Workflow example:
  ```
  1. Write initial tasks: "Run the build", "Fix any type errors"
  2. After build failure expand specific errors: "Fix error 1", "Fix error 2", ...
  3. Mark each individually in_progress → completed
  ```

---

## 2. Communication Strategies

### 2.1 Conciseness & Impersonal Tone

| Platform | Core Principle | Instructions |
|------|----------|----------|
| **Same.dev** | Concise policy | "Do what has been asked; nothing more, nothing less" / "NEVER refer to tool names..." |
| **VSCode Agent** | Short impersonal | "Keep your answers short and impersonal" / "Never say the name of a tool..." |
| **Amp** | No code explanation | "Do not add additional code explanation summary unless requested..." |
| **Orchids.app** | Direct concise | "BE DIRECT AND CONCISE..." / "MINIMIZE CONVERSATION..." |

### 2.2 Code Modification & Output

**Prohibition on direct code output**:
- **Same.dev**: "NEVER output code directly to user..."
- **VSCode Agent**: "NEVER print out a codeblock with file changes unless the user asked..."
- **Trae**: allows "simplified code block" but strict placeholder usage

**Standardized code placeholders**:
- **Qoder**: `// ... existing code ...`
- **Same.dev**: `// ... existing code ...`
- **Trae**: `// ... existing code ...`
- **VSCode Agent**: `// ...existing code...`

### 2.3 Error Handling & Validation

**Mandatory validation steps** (must run after modifications):

| Platform | Tool | Timing | Error Recovery |
|------|------|------|----------|
| **Same.dev** | `run_linter` | After major edits & before each version | Up to 3 fix attempts then escalate |
| **VSCode Agent** | `get_errors` | After editing file to validate changes | Fix related errors |
| **Amp** | `get_diagnostics` + lint/typecheck | After task completion | Do not suppress errors unless user requests |

**Same.dev Error Recovery Loop**:
```
1. run_linter → error found
2. Fix (only if solution clear; avoid blind guessing)
3. run_linter → errors remain
4. Repeat up to 3 times
5. After third attempt stop and ask user
```

---

## 3. Tool Integration & Execution Patterns

### 3.1 Tool Categories

Four core categories:
1. **File operations**: create_file, edit_file, read_file, write_file
2. **Search/discovery**: grep, code_search, glob, semantic_search
3. **Execution**: bash, run_terminal, execute
4. **Validation**: get_diagnostics, run_linter, get_errors

### 3.2 Code Modification Strategy Details

#### `edit_file` Mode

| Platform | Parameters | Traits | Notes |
|------|------|------|----------|
| **Amp** | `file_path`, `old_str`, `new_str`, `replace_all` | Returns git-style diff <br> `old_str` must exist <br> Must differ | Full-file replacement use `create_file` |
| **Same.dev** | Same | `smart_apply` flag for retry | Use when file < 2500 lines |
| **Orchids.app** | Same | Truncation placeholders minimize unchanged code: `// ... rest of code ...`, `// ... keep existing code ...` | Special instructions for deletion |
| **Qoder** | Same as above | Strong preference for `search_replace` unless explicitly instructed | Uses `// ... existing code ...` |

#### `insert_edit` Mode

- **VSCode Agent** `insert_edit_into_file`:
  - Parameters: `explanation`, `filePath`, `code`
  - Designed to be "smart" requiring minimal prompting
  - Uses `// ...existing code...` for unchanged regions

#### `replace` Mode Variants

| Platform | Tool Name | Core Features |
|------|--------|----------|
| **Lovable** | `lov-line-replace` | Line-number based search/replace <br> Parameters: `file_path`, `search`, `first_replaced_line`, `last_replaced_line`, `replace` <br> Ellipsis supported for large code sections |
| **Same.dev** | `string_replace` | Use when file > 2500 lines <br> Also recommended for small edits |
| **Windsurf** | `replace_file_content` | Uses `ReplacementChunk` array + `TargetContent` matching |
| **Qoder** | `search_replace` | Efficient string replacement defined in design docs <br> Parameters: `file_path`, `replacements[]` (`original_text`, `new_text`, `replace_all?`) <br> Strict uniqueness, exact match, sequential processing |
| **Replit** | `<proposed_file_replace_substring>` | XML format <br> Requires: `file_path`, `change_summary`, `<old_str>`, `<new_str>` <br> `<proposed_file_replace>` variant replaces entire file |
| **Claude Code 2.0** | `Edit` tool | Precise string replacement <br> Parameters: `file_path`, `old_string`, `new_string`, `replace_all?` <br> Preserve indentation; ensure `old_string` unique or use `replace_all` |

### 3.3 Other File Operations

- **Amp**: `create_file` - create new file or overwrite existing
- **Lovable**: `lov-write` - mainly for new files or fallback if `lov-line-replace` fails
- **Qoder**: `create_file` - create new design file, content limit 600 lines

---

## 4. Deep Analysis of Validation & Error Handling

### 4.1 Diagnostic Tool Comparison

| Tool | Platform | Function | Use Case |
|------|------|------|----------|
| `get_diagnostics` | Amp | Retrieve file/directory errors, warnings, other diagnostics | Mandatory post-task <br> Prefer directories over single file |
| `get_diagnostics` | Traycer AI | Built-in LSP analysis <br> Supports glob (e.g. `*.ts`) <br> Severity filtering (Error/Warning/Information/Hint) | Multi-file diagnostics |
| `run_linter` | Same.dev | Check linting & runtime errors | After major edits & before each version |
| `get_errors` | VSCode Agent | Get compile or lint errors | Validate post-edit <br> View same errors as user |

### 4.2 Error Recovery Strategies

**Iterative Repair Pattern**:
1. **Amp**: use `todo_write`/`todo_read` to decompose tasks → mark each in_progress/completed → on persistent failure use `oracle` for expert guidance
2. **Same.dev**: up to 3 repair loops for same file linter issues
3. **Cursor Prompts**: same 3-attempt cap then ask user <br> self-correct if task marked complete but tests/build fail
4. **Orchids.app**: when looping, gather more context or explore alternative approach

**Failure Escalation**:
- **Comet Assistant**: declare failure only after ≥5 distinct attempts <br> Exception: immediate failure for auth prerequisites

### 4.3 Execution Sequence Examples

**Amp Build Fix Workflow**:
```
User: "Run the build and fix any type errors"
1. todo_write: ["Run the build", "Fix any type errors"]
2. npm run build → reports 10 type errors
3. todo_write: ["Fix error 1", "Fix error 2", ..., "Fix error 10"]
4. Mark each in_progress
5. Fix error 1 → mark completed
6. Fix error 2 → mark completed
   ...
7. After all fixed: run get_diagnostics + rebuild
```

**Same.dev Linter Loop**:
```
1. edit_file (or string_replace)
2. run_linter → finds 3 errors
3. Fix errors (clear plan)
4. run_linter → still 1 error
5. Fix again
6. run_linter → no errors → continue
(If still failing after 3rd attempt → stop and ask user)
```

---

## 5. Debugging & Feedback Mechanisms

### v0 Debugging Mode
- Primarily uses `console.log` statements for debugging
- Logs returned via `<v0_app_debug_logs>`
- Purpose: trace execution flow, inspect variables, identify issues
- Cleanup principle: remove after issue resolved unless ongoing value

### Orchids.app Error Handling Mode
- Collect sufficient context to understand root cause while fixing errors
- When stuck in loop: gather more context or explore alternative approaches
- Auth error handling: if user already exists display error message (registration scenario)

---

## 6. Key Observations & Patterns

### 6.1 Consensus Points
- Validation necessity: all platforms enforce post-modification validation (lint/diagnostics/errors)
- Error recovery: widely use "iterative repair + user escalation" pattern (after 3–5 attempts)
- Placeholder standard: widely use `// ... existing code ...` variants for unchanged code
- Tool name hiding: most platforms require hiding tool names from user, using natural language description

### 6.2 Divergence Points
- Parallel execution philosophy: Same.dev (aggressive parallelism) vs VSCode Agent (conservative limits) vs Windsurf (no directive)
- Code modification tools: edit_file vs insert_edit vs line-replace vs search_replace
- File size threshold: Same.dev uses 2500 lines to distinguish edit_file vs string_replace
- Error suppression: Amp explicitly forbids suppressing compiler/lint errors (unless user explicitly requests)

### 6.3 Special Mechanisms
- Same.dev: `smart_apply` flag for edit_file retries
- Amp: `oracle` tool for complex debugging and expert guidance
- Amp: `AGENTS.md` as persistent config file (commands, style, structure)
- Windsurf: `create_memory` semantic database for long-term context retention
- Lovable: line-number-based `lov-line-replace` supports ellipsis

---

## 7. External Service Integration

(Not detailed in retrieved sources; directory lists "5.6 External Service Integration Architecture")

---

## References

- DeepWiki repository: https://deepwiki.com/x1xhlol/system-prompts-and-models-of-ai-tools
- Query findings:
  - System prompt design & communication strategies
  - Tool integration & execution patterns
  - Code modification strategies
  - Validation & error handling
  - Task planning & memory systems

**Last Updated**: 2025-10-19
