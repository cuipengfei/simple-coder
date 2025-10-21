# Source 3: Mini SWE Agent

**Source**: SWE-agent/mini-swe-agent
**DeepWiki**: https://deepwiki.com/SWE-agent/mini-swe-agent

## Document Structure

`mini-swe-agent` is a minimalist software engineering agent system designed for SWE-Bench evaluation:

1. **Overview**
   - Architecture overview
   - Key features
2. **Quick Start**
   - Installation
   - Quick start guide
   - Configuration settings
3. **Command Line Interface**
   - `mini` command
   - `mini-extra` utilities
   - Interactive modes
4. **Core Architecture**
   - Agent system
   - Model integration
   - Execution environment
5. **User Interface**
   - Simple command line
   - Textual user interface
   - Batch processing
6. **Configuration System**
   - Config files
   - Template system
7. **SWE-Bench Integration**
   - Batch evaluation
   - Single instance testing
   - Results and analysis
8. **Advanced Features**
   - Trajectory inspector
   - Python API
   - Container environments
9. **Development**
   - Testing framework
   - Development setup
   - CI/CD and release
   - Coding standards

---

## 1. Three-Component Architecture

### 1.1 Architecture Overview

Defined in `src/minisweagent/__init__.py`:

- **`Agent`**: orchestrates task execution & message flow
- **`Model`**: language model interaction abstraction
- **`Environment`**: bash command execution abstraction

Design philosophy:
- Minimalism: `DefaultAgent` core ~100 lines
- No custom tools or function calls
- Linear message history
- Stateless actions via independent `subprocess.run`

### 1.2 Component Protocols

```python
# Agent Protocol
class Agent(Protocol):
    def step(self) -> None: ...
    def run(self) -> tuple[str, str]: ...

# Model Protocol
class Model(Protocol):
    def query(self, messages: list) -> str: ...

# Environment Protocol
class Environment(Protocol):
    def execute(self, command: str) -> tuple[str, int]: ...
```

---

## 2. DefaultAgent Implementation

### 2.1 Core Methods

#### `run()` Method

Located in `src/minisweagent/agents/default.py`:

```python
def run(self) -> tuple[str, str]:
    # 1. initialize message history
    self.messages = [
        {"role": "system", "content": self.render_template(system_template)},
        {"role": "user", "content": self.render_template(instance_template)}
    ]

    # 2. main loop
    while True:
        try:
            self.step()  # execute single step
        except NonTerminatingException as e:
            # append error to history, continue
            self.add_message("user", str(e))
        except TerminatingException as e:
            # return exit status and message
            return (type(e).__name__, str(e))
```

Key flow:
1. Initialize history
2. Loop calling `step()`
3. Non-terminating exceptions appended; terminating exceptions returned

#### `step()` Method

```python
def step(self) -> None:
    # 1. query model
    response = self.query()

    # 2. parse action
    action = self.parse_action(response)

    # 3. execute action
    output, returncode = self.execute_action(action)

    # 4. check completion
    if self.has_finished(output):
        raise Submitted(output)

    # 5. render observation
    observation = self.render_template(
        self.action_observation_template,
        output=output,
        returncode=returncode
    )

    # 6. append to history
    self.add_message("user", observation)
```

### 2.2 Exception Hierarchy

Recoverable (loop continues):
- `FormatError` – output format mismatch (e.g. wrong bash block count)
- `ExecutionTimeoutError` – action exceeded time limit

Termination conditions:
- `Submitted` – completion token observed (e.g. `COMPLETE_TASKAND_SUBMIT_FINAL_OUTPUT`)
- `LimitsExceeded` – step or cost limits reached

---

## 3. Template System

### 3.1 Jinja2 Templates

Used for prompt generation and output formatting.

```python
def render_template(self, template: str, **kwargs) -> str:
    # merge agent, environment, model config variables
    variables = {**self._config, **kwargs}
    return jinja2.Template(template, undefined=jinja2.StrictUndefined).render(variables)
```

Features:
- Accepts template string + kwargs
- Merges configuration dictionaries
- StrictUndefined enforces variable presence

### 3.2 Key Templates

| Template | Purpose | Elements |
|----------|---------|----------|
| `system_template` | Initial instructions | role, task description |
| `instance_template` | Instance-specific instructions | problem, repo info |
| `action_observation_template` | Format command output | `<returncode>`, `<output_head>`, `<elided_chars>`, `<output_tail>`, `<warning>` |
| `timeout_template` | Notify of timeout | timeout message |
| `format_error_template` | Describe format error | error message |

Output truncation logic (Jinja2):
```jinja2
<returncode>{{ returncode }}</returncode>
{% if output|length < max_output_length %}
<output>{{ output }}</output>
{% else %}
<output_head>{{ output[:max_output_length//2] }}</output_head>
<elided_chars>{{ output|length - max_output_length }}</elided_chars>
<output_tail>{{ output[-max_output_length//2:] }}</output_tail>
<warning>Output truncated</warning>
{% endif %}
```

---

## 4. Model Integration

Located in `src/minisweagent/__init__.py`:
```python
class Model(Protocol):
    def query(self, messages: list) -> str: ...
```

Implementations:
- `LitellmModel` – via LiteLLM multi-provider support
- `AnthropicModel` – direct Anthropic API

Model selection (`get_model()` in `models/__init__.py`): CLI/YAML configurable.

Example YAML:
```yaml
model:
  name: "claude-3-5-sonnet-20241022"
  class: "AnthropicModel"
```

---

## 5. Execution Environment

Protocol defines shell execution abstraction. Types:

| Type | Class | Purpose |
|------|-------|---------|
| Local | `LocalEnvironment` | Local `subprocess.run` |
| Docker | `DockerEnvironment` | Isolated SWE-Bench runs |
| Singularity | `SingularityEnvironment` | Writable sandbox |

Docker config: image, cwd, env, forward_env, timeout, executable (env overrides precedence). Lifecycle: init (detached container + keepalive), execute (workdir, env forwarding, capture output), cleanup.

Singularity config adds `sandbox_build_retries`; lifecycle includes sandbox build with retries, isolated execute flags, cleanup.

---

## 6. SWE-Bench Integration

Batch workflow script: `run/extra/swebench.py`.

Filtering: subset selection, regex filter, slice, shuffle (seed 42), skip existing unless redo flag.

Parallelism: `ThreadPoolExecutor` + incremental persistence (`preds.json`, trajectories) tracked by progress manager.

---

## 7. Interactive Interfaces

Layers: Batch (`DefaultAgent`) → REPL (`InteractiveAgent`) → TUI (`TextualAgent`).

Key bindings (TUI): `c` confirm, `y` yolo, `u` human, `h` previous, `l` next, `0` first, `$` last, `Ctrl+T` toggle input mode.

Invocation:
- `mini` → interactive
- `mini -v` → textual UI
- `mini-extra swebench` → batch
- `mini-extra swebench-single` → interactive single instance
Env var: `MSWEA_VISUAL_MODE_DEFAULT`.

Comparison:
| Dimension | DefaultAgent | InteractiveAgent | TextualAgent |
|-----------|--------------|------------------|--------------|
| Interaction | None | REPL | Rich TUI |
| Tech | None | prompt_toolkit + rich | Textual |
| Threads | Sync | Sync | Multi-thread |
| Input | None | PromptSession | SmartInputContainer |
| Navigation | None | Linear history | Step navigation |
| Modes | None | confirm/yolo/human | confirm/yolo/human |

---

## 8. Configuration System

YAML keys: `agent`, `environment`, `model`, `run`.
Load order precedence: CLI > env vars > YAML.

Constructor pattern:
```python
def __init__(self, **kwargs):
    # kwargs directly set configuration options
    # stored in AgentConfig dataclass
```

---

## 9. CLI Commands

`mini` (REPL) / `mini -v` (visual). `mini-extra` provides batch and single-instance utilities.

Batch options: `--subset`, `--split`, `--workers`, `--slice`, `--filter-spec`, `--shuffle`, `--redo-existing`.
Execution flow: load → filter → parallel process → incremental save → progress track.

---

## 10. Key Observations

Design philosophy: minimal core, no custom tool abstraction, linear history, stateless execution.
Extensibility: protocol-driven, modular environments, flexible config, template-based prompts.
Optimizations: parallel batch, incremental persistence, Docker isolation, flexible filtering.
Interaction flexibility: layered interfaces, mode system, navigation features.
Exception-driven control: clear separation of recoverable vs terminal conditions.

---

## References

- DeepWiki: https://deepwiki.com/SWE-agent/mini-swe-agent
- Findings: architecture, run/step methods, template system, message history, exception handling, configuration management, container environments, dataset loading/filtering, interactive & textual agents, interaction modes.

**Last Updated**: 2025-10-19
