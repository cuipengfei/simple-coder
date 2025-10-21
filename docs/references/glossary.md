# Glossary

This glossary is based on core concepts appearing across the three sources, ordered alphabetically.

---

## A

### Agent
**Definition**: Software entity that autonomously executes tasks by interacting with an LLM and invoking tools to complete complex workflows.

**Sources**:
- Source 2: `Agent` struct with client, getUserMessage, tools, verbose
- Source 3: `Agent` protocol defining `step()` and `run()` methods

**Related Terms**: DefaultAgent, InteractiveAgent, TextualAgent

### AGENTS.md
**Definition**: Configuration file used by Amp platform storing common commands, code style preferences, repository structure.

**Source**: Source 1 (Amp)

**Uses**:
- Persist configuration (vs transient TODO)
- Auto injected into agent context
- Team sharing support

---

## B

### Bash
**Definition**: Unix shell and command language for executing system commands.

**Sources**:
- Source 2: `bash` tool via `exec.Command("bash", "-c", command)`
- Source 3: mini-swe-agent single action type (no tool abstraction)

**Related Terms**: bash_tool, LocalEnvironment, subprocess

---

## C

### Code Search
**Definition**: Tool that searches codebase using regex or literal text patterns.

**Sources**:
- Source 1: multiple platforms (GrepRepo, code_search, semantic_search)
- Source 2: `code_search` tool using `ripgrep`

**Implementation**:
```go
rg -n --heading --no-color -i -t <type> <pattern> <path> -m 50
```

### Conversation History
**Definition**: Complete sequence of messages between agent and LLM to preserve contextual continuity.

**Sources**:
- Source 2: `conversation []anthropic.MessageParam`
- Source 3: `self.messages` list

**Composition**: user messages, assistant responses, tool results (as user messages)

---

## D

### DefaultAgent
**Definition**: Core agent implementation in mini-swe-agent (~100 lines Python).

**Source**: Source 3

**Key Methods**:
- `run()`: initialize history, loop `step()`, handle exceptions
- `step()`: query model → parse action → execute → check completion → render observation → append to history

**Design Philosophy**: Minimalism, no tool abstraction, linear history

### DockerEnvironment
**Definition**: Environment implementation executing commands in isolated Docker container.

**Source**: Source 3

**Lifecycle**:
1. Initialize: `docker run -d` start container
2. Execute: `docker exec` run command
3. Cleanup: `docker stop && docker rm -f`

**Use**: SWE-Bench evaluation; clean reproducible environment

---

## E

### edit_file
**Definition**: Tool that modifies file content via exact string replacement.

**Sources**:
- Source 1: multiple platforms (Amp, Same.dev, Orchids, Qoder, Claude Code)
- Source 2: defined but not fully implemented

**Parameters**: `file_path`, `old_str`, `new_str`, `replace_all?`

**Requirements**:
- `old_str` must exist and be unique (unless `replace_all: true`)
- Preserve exact indentation

### Environment
**Definition**: Protocol/interface defining how commands are executed.

**Source**: Source 3

**Implementation Types**:
- `LocalEnvironment`: local execution (`subprocess.run`)
- `DockerEnvironment`: Docker container
- `SingularityEnvironment`: Singularity sandbox

**Protocol Method**: `execute(command: str) -> (output: str, returncode: int)`

---

## F

### FormatError
**Definition**: Subclass of NonTerminatingException indicating model output format is invalid.

**Source**: Source 3

**Trigger**: Response does not contain exactly one bash code block

**Handling**: Append error string to message history; loop continues

---

## G

### GenerateSchema
**Definition**: Function in Source 2 that auto-generates JSON Schema from Go struct.

**Source**: Source 2

**Implementation**:
```go
func GenerateSchema[T any]() anthropic.ToolInputSchemaParam {
    reflector := jsonschema.Reflector{
        AllowAdditionalProperties: false,
        DoNotReference:            true,
    }
    schema := reflector.Reflect(new(T))
    return anthropic.ToolInputSchemaParam{...}
}
```

**Purpose**: Ensure type safety & validation for tool inputs

### get_diagnostics
**Definition**: Tool retrieving compile, lint, type-check errors and warnings for file/directory.

**Source**: Source 1 (Amp, Traycer AI)

**Usage Timing**: Forced after task completion to ensure code correctness

**Priority**: Amp recommends running on directory rather than single file

---

## I

### InputSchema
**Definition**: JSON Schema defining expected tool input parameters.

**Source**: Source 2

**Generation**: `GenerateSchema[T]()` auto-generates from Go struct

**Fields**: Type, Properties, Required

### InteractiveAgent
**Definition**: Extends DefaultAgent; provides REPL-style command line interaction.

**Source**: Source 3

**Modes**:
- `confirm`: LM proposes, user confirms
- `yolo`: auto-execute
- `human`: user directly inputs commands

**Stack**: `prompt_toolkit` (input) + `rich.Console` (display)

---

## J

### Jinja2
**Definition**: Python template engine used to dynamically build prompts and format output.

**Source**: Source 3

**Use Cases**:
- `system_template`: system prompt
- `instance_template`: task description
- `action_observation_template`: command output formatting

**Config**: `StrictUndefined` ensures all variables defined

---

## L

### list_files
**Definition**: Tool listing files in a directory.

**Source**: Source 2

**Implementation**: Uses `find` command; excludes `.devenv` and `.git`
```bash
find <path> -not -path "*/.devenv/*" -not -path "*/.git/*"
```

**Output**: JSON array of file paths

### LitellmModel
**Definition**: Model implementation using LiteLLM supporting multiple providers.

**Source**: Source 3

**Supports**: OpenAI, Anthropic, Azure, Cohere and others

---

## M

### Message History
See [Conversation History](#conversation-history)

### Model
**Definition**: Protocol defining interaction interface with language model.

**Source**: Source 3

**Protocol Method**: `query(messages: list) -> str`

**Implementations**: LitellmModel, AnthropicModel

---

## N

### NonTerminatingException
**Definition**: Recoverable exception allowing agent to continue.

**Source**: Source 3

**Subclasses**:
- `FormatError`: model output format error
- `ExecutionTimeoutError`: command timeout

**Handling**: Append error string to history; `run()` loop continues

---

## O

### Observation
**Definition**: Output after command execution, formatted then sent as user message to model.

**Source**: Source 3

**Template**: `action_observation_template`

**Components**: returncode, output (possibly truncated), warning

---

## P

### Parallel Execution
**Definition**: Strategy of executing multiple tool calls simultaneously to improve efficiency.

**Source**: Source 1

**Strategy Comparison**:
- **Same.dev**: parallel default, 3–5x speed claim
- **v0**: parallel during context collection
- **Amp**: multi-level parallel (reads/writes/subagents)
- **VSCode Agent**: restricted parallel (forbids parallel semantic_search)

**Not Implemented**: Source 2 & 3 (serial execution)

### Placeholder
**Definition**: Comment placeholder indicating unchanged code section during modification.

**Source**: Source 1

**Variants**:
- `// ... existing code ...` (Qoder, Same.dev, Trae)
- `// ...existing code...` (VSCode Agent)
- `// ... keep existing code ...` (Orchids, Lovable)

**Purpose**: Reduce token usage, improve readability

---

## R

### read_file
**Definition**: Tool for reading file contents.

**Source**: Source 2

**Implementation**: `os.ReadFile(path)`

**Error Handling**: File not found → log, return error

### replace_all
**Definition**: Parameter of edit_file controlling whether all matches are replaced.

**Source**: Source 1 (multiple platforms), Source 2

**Default**: `false` (replace only first match, uniqueness required)

**Purpose**: Global find & replace

### ripgrep (rg)
**Definition**: Fast CLI search tool supporting regex.

**Source**: Source 2

**Usage**: `code_search` tool

**Common Flags**:
- `-n`: line numbers
- `--heading`: group by filename
- `--no-color`: plain output
- `-i`: case-insensitive
- `-t`: file type filter
- `-m`: limit matches

### run_linter
**Definition**: Same.dev validation tool checking linting & runtime errors.

**Source**: Source 1 (Same.dev)

**Timing**: After major edits, before each version

**Error Recovery**: Up to 3 fix attempts then escalate to user

---

## S

### Schema
See [InputSchema](#inputschema)

### SWE-Bench
**Definition**: Software engineering benchmark dataset evaluating agent ability to fix real GitHub issues.

**Source**: Source 3

**Datasets**: lite, verified, or custom

**Integration**:
- Batch command: `mini-extra swebench`
- Single instance debug: `mini-extra swebench-single`
- Parallel processing: `ThreadPoolExecutor`

### step_limit
**Definition**: Maximum execution step count permitted for agent.

**Source**: Source 3

**Config Location**: YAML `agent.step_limit`

**Exceeded Handling**: Throw `LimitsExceeded` (TerminatingException)

### Submitted
**Definition**: Subclass of TerminatingException indicating task completion.

**Source**: Source 3

**Trigger**: Command output includes completion token (e.g. `"COMPLETE_TASK_AND_SUBMIT_FINAL_OUTPUT"`)

### System Prompt
**Definition**: Initial prompt defining agent identity, behavior rules, tool usage.

**Source**: Source 1, Source 3

**Components**:
- Identity definition ("You are...")
- Defensive constraints ("NEVER...")
- Communication strategy (conciseness, placeholders)
- Tool usage guidelines
- Error handling rules

---

## T

### TerminatingException
**Definition**: Exception requiring agent execution to stop.

**Source**: Source 3

**Subclasses**:
- `Submitted`: task completed
- `LimitsExceeded`: step/cost limit reached

**Handling**: `run()` returns exit status and message

### TextualAgent
**Definition**: Agent implementation providing Textual library driven advanced TUI.

**Source**: Source 3

**Architecture**: Background thread runs agent; main thread keeps UI responsive

**Features**:
- Step navigation (prev/next/first/last)
- `SmartInputContainer` (single/multi-line toggle)
- Key bindings (c/y/u/h/l/0/$)

### todo_write / todo_read
**Definition**: Amp transient TODO management tool.

**Source**: Source 1 (Amp)

**Schema**:
```
{
  id: string,
  content: string,
  status: "completed" | "in-progress" | "todo",
  priority: "medium" | "low" | "high"
}
```

**Key Requirement**: Mark completed immediately; do not batch

### ToolDefinition
**Definition**: Struct in Source 2 defining tool metadata and execution logic.

**Source**: Source 2

**Fields**:
```go
type ToolDefinition struct {
    Name        string
    Description string
    InputSchema anthropic.ToolInputSchemaParam
    Function    func(json.RawMessage) (string, error)
}
```

**Registration**: Add to `Agent.tools` slice

---

## V

### Validation
**Definition**: Process checking syntax, type, and lint errors after code modification.

**Source**: Source 1

**Mandatory Tools**:
- `run_linter` (Same.dev)
- `get_diagnostics` (Amp)
- `get_errors` (VSCode Agent)

**Timing**: After each edit, after task completion

**Error Recovery**: Iterative fixes, up to 3–5 attempts

---

## W

### whitelist_actions
**Definition**: Configuration for InteractiveAgent & TextualAgent specifying regex patterns of commands to skip confirmation in confirm mode.

**Source**: Source 3

**Example** (inferred):
```yaml
whitelist_actions:
  - "^ls "
  - "^cat "
  - "^git status"
```

**Safety**: Must avoid dangerous commands (e.g. `rm -rf /`)

---

## Term Statistics

### High Frequency Terms (appear in all 3 sources)

- Agent
- Tool
- Validation
- Error Handling
- Message History

### Medium Frequency Terms (appear in 2 sources)

- edit_file
- Parallel Execution
- Environment
- Template

### Low Frequency Terms (appear in 1 source)

- AGENTS.md (Source 1)
- ToolDefinition (Source 2)
- NonTerminatingException (Source 3)

---

## References

- [Source 1: System Prompts](../sources/notes-system-prompts.md)
- [Source 2: Coding Agent](../sources/notes-coding-agent.md)
- [Source 3: Mini SWE Agent](../sources/notes-mini-swe-agent.md)

**Last Updated**: 2025-10-19
