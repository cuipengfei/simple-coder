# Glossary - 术语表

本术语表基于三个来源中出现的核心概念，按字母顺序排列。

---

## A

### Agent
**定义**: 自主执行任务的软件实体，通过与语言模型交互并调用工具完成复杂工作流。

**出现来源**:
- Source 2: `Agent` 结构体，包含 client、getUserMessage、tools、verbose
- Source 3: `Agent` 协议，定义 `step()` 和 `run()` 方法

**相关术语**: DefaultAgent, InteractiveAgent, TextualAgent

### AGENTS.md
**定义**: Amp 平台使用的配置文件，存储常用命令、代码风格偏好、代码库结构。

**出现来源**: Source 1 (Amp)

**用途**:
- 持久化配置（vs 瞬态 TODO）
- 自动加入 Agent 上下文
- 支持团队共享

---

## B

### Bash
**定义**: Unix shell 和命令语言，用于执行系统命令。

**出现来源**:
- Source 2: `bash` 工具，通过 `exec.Command("bash", "-c", command)` 执行
- Source 3: mini-swe-agent 的唯一动作类型（无工具抽象）

**相关术语**: bash_tool, LocalEnvironment, subprocess

---

## C

### Code Search
**定义**: 使用正则或文本模式在代码库中搜索的工具。

**出现来源**:
- Source 1: 多个平台提供（GrepRepo, code_search, semantic_search）
- Source 2: `code_search` 工具，使用 `ripgrep`

**实现**:
```go
rg -n --heading --no-color -i -t <type> <pattern> <path> -m 50
```

### Conversation History
**定义**: Agent 与 LLM 之间的完整消息序列，用于保持上下文连续性。

**出现来源**:
- Source 2: `conversation []anthropic.MessageParam`
- Source 3: `self.messages` 列表

**组成**: 用户消息、助手响应、工具结果（作为用户消息）

---

## D

### DefaultAgent
**定义**: mini-swe-agent 的核心 Agent 实现，约 100 行 Python 代码。

**出现来源**: Source 3

**关键方法**:
- `run()`: 初始化历史，循环调用 `step()`，处理异常
- `step()`: 查询模型 → 解析动作 → 执行 → 检查完成 → 渲染观察 → 添加到历史

**设计哲学**: 极简主义、无工具抽象、线性历史

### DockerEnvironment
**定义**: 使用 Docker 容器隔离执行命令的环境实现。

**出现来源**: Source 3

**生命周期**:
1. 初始化：`docker run -d` 启动容器
2. 执行：`docker exec` 运行命令
3. 清理：`docker stop && docker rm -f`

**用途**: SWE-Bench 评估，提供干净、可重现的环境

---

## E

### edit_file
**定义**: 通过精确字符串替换修改文件内容的工具。

**出现来源**:
- Source 1: 多个平台（Amp, Same.dev, Orchids, Qoder, Claude Code）
- Source 2: 定义但未完整实现

**参数**: `file_path`, `old_str`, `new_str`, `replace_all?`

**要求**:
- `old_str` 必须存在且唯一（除非 `replace_all: true`）
- 保留精确缩进

### Environment
**定义**: 定义命令执行方式的协议/接口。

**出现来源**: Source 3

**实现类型**:
- `LocalEnvironment`: 本地执行（`subprocess.run`）
- `DockerEnvironment`: Docker 容器
- `SingularityEnvironment`: Singularity 沙箱

**协议方法**: `execute(command: str) -> (output: str, returncode: int)`

---

## F

### FormatError
**定义**: NonTerminatingException 的子类，表示模型输出格式不正确。

**出现来源**: Source 3

**触发条件**: 响应不包含恰好 1 个 bash 代码块

**处理**: 错误字符串加入消息历史，循环继续

---

## G

### GenerateSchema
**定义**: Source 2 中从 Go 结构体自动生成 JSON Schema 的函数。

**出现来源**: Source 2

**实现**:
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

**用途**: 确保工具输入的类型安全与验证

### get_diagnostics
**定义**: 获取文件/目录的编译、lint、类型检查错误和警告的工具。

**出现来源**: Source 1 (Amp, Traycer AI)

**使用时机**: 任务完成后强制运行，确保代码正确性

**优先级**: Amp 建议对目录而非单文件运行

---

## I

### InputSchema
**定义**: 定义工具期望输入参数的 JSON Schema。

**出现来源**: Source 2

**生成方式**: `GenerateSchema[T]()` 从 Go 结构体自动生成

**字段**: Type, Properties, Required

### InteractiveAgent
**定义**: 扩展 DefaultAgent，提供 REPL 风格命令行交互的 Agent 实现。

**出现来源**: Source 3

**交互模式**:
- `confirm`: LM 提议，用户确认
- `yolo`: 自动执行
- `human`: 用户直接输入命令

**技术栈**: `prompt_toolkit` (输入) + `rich.Console` (显示)

---

## J

### Jinja2
**定义**: Python 模板引擎，用于动态生成提示和格式化输出。

**出现来源**: Source 3

**使用场景**:
- `system_template`: 系统提示
- `instance_template`: 任务描述
- `action_observation_template`: 命令输出格式化

**配置**: `StrictUndefined` 确保所有变量已定义

---

## L

### list_files
**定义**: 列出目录中文件的工具。

**出现来源**: Source 2

**实现**: 使用 `find` 命令，排除 `.devenv` 和 `.git`
```bash
find <path> -not -path "*/.devenv/*" -not -path "*/.git/*"
```

**输出**: JSON 格式的文件路径数组

### LitellmModel
**定义**: 使用 LiteLLM 库支持多种模型提供商的 Model 实现。

**出现来源**: Source 3

**支持**: OpenAI, Anthropic, Azure, Cohere 等多种 API

---

## M

### Message History
参见 [Conversation History](#conversation-history)

### Model
**定义**: 定义与语言模型交互接口的协议。

**出现来源**: Source 3

**协议方法**: `query(messages: list) -> str`

**实现**: LitellmModel, AnthropicModel

---

## N

### NonTerminatingException
**定义**: 可恢复的异常，允许 Agent 继续执行。

**出现来源**: Source 3

**子类**:
- `FormatError`: 模型输出格式错误
- `ExecutionTimeoutError`: 命令超时

**处理**: 错误字符串加入消息历史，`run()` 循环继续

---

## O

### Observation
**定义**: 命令执行后的输出，格式化后作为用户消息发送给模型。

**出现来源**: Source 3

**模板**: `action_observation_template`

**组成**: returncode, output (可能截断), warning

---

## P

### Parallel Execution
**定义**: 同时执行多个工具调用以提高效率的策略。

**出现来源**: Source 1

**策略对比**:
- **Same.dev**: 默认并行，3-5x 速度提升
- **v0**: 上下文收集阶段并行
- **Amp**: 多层级并行（reads/writes/subagents）
- **VSCode Agent**: 限制性并行（禁止并行 semantic_search）

**未实现**: Source 2 和 3（串行执行）

### Placeholder
**定义**: 代码修改时表示未变部分的注释。

**出现来源**: Source 1

**变体**:
- `// ... existing code ...` (Qoder, Same.dev, Trae)
- `// ...existing code...` (VSCode Agent)
- `// ... keep existing code ...` (Orchids, Lovable)

**用途**: 减少 token 消耗，提高可读性

---

## R

### read_file
**定义**: 读取文件内容的工具。

**出现来源**: Source 2

**实现**: `os.ReadFile(path)`

**错误处理**: 文件不存在 → 记录日志，返回错误

### replace_all
**定义**: edit_file 工具的参数，控制是否替换所有匹配。

**出现来源**: Source 1 (多个平台), Source 2

**默认值**: `false`（仅替换第一个匹配，要求唯一性）

**用途**: 全局查找替换

### ripgrep (rg)
**定义**: 快速的命令行搜索工具，支持正则表达式。

**出现来源**: Source 2

**使用场景**: `code_search` 工具

**常用标志**:
- `-n`: 显示行号
- `--heading`: 文件名分组
- `--no-color`: 无颜色输出
- `-i`: 大小写不敏感
- `-t`: 文件类型过滤
- `-m`: 限制匹配数量

### run_linter
**定义**: Same.dev 的验证工具，检查 linting 和运行时错误。

**出现来源**: Source 1 (Same.dev)

**使用时机**: 每次重大编辑后、每个版本前

**错误恢复**: 最多 3 次修复循环，之后升级给用户

---

## S

### Schema
参见 [InputSchema](#inputschema)

### SWE-Bench
**定义**: 软件工程基准测试数据集,用于评估 Agent 修复真实 GitHub issues 的能力。

**出现来源**: Source 3

**数据集**: lite, verified, 或自定义

**集成**:
- 批处理命令: `mini-extra swebench`
- 单实例调试: `mini-extra swebench-single`
- 并行处理: `ThreadPoolExecutor`

### step_limit
**定义**: Agent 允许的最大执行步数。

**出现来源**: Source 3

**配置位置**: YAML `agent.step_limit`

**超出处理**: 抛出 `LimitsExceeded` (TerminatingException)

### Submitted
**定义**: TerminatingException 的子类，表示任务完成。

**出现来源**: Source 3

**触发**: 命令输出包含完成信号（如 `"COMPLETE_TASK_AND_SUBMIT_FINAL_OUTPUT"`）

### System Prompt
**定义**: 定义 Agent 身份、行为规则、工具使用方式的初始提示。

**出现来源**: Source 1, Source 3

**组成**:
- 身份定义（"You are..."）
- 防御性约束（"NEVER..."）
- 沟通策略（简洁性、占位符）
- 工具使用指南
- 错误处理规则

---

## T

### TerminatingException
**定义**: 需要停止 Agent 执行的异常。

**出现来源**: Source 3

**子类**:
- `Submitted`: 任务完成
- `LimitsExceeded`: 步数/成本限制

**处理**: `run()` 返回退出状态和消息

### TextualAgent
**定义**: 提供 Textual 库驱动的高级可视化界面（TUI）的 Agent 实现。

**出现来源**: Source 3

**架构**: 后台线程运行 Agent，主线程保持 UI 响应

**特性**:
- 步骤导航（前/后/首/尾）
- `SmartInputContainer`（单行/多行切换）
- 按键绑定（c/y/u/h/l/0/$）

### todo_write / todo_read
**定义**: Amp 的瞬态 TODO 管理工具。

**出现来源**: Source 1 (Amp)

**Schema**:
```
{
  id: string,
  content: string,
  status: "completed" | "in-progress" | "todo",
  priority: "medium" | "low" | "high"
}
```

**关键要求**: 立即标记完成，不批量处理

### ToolDefinition
**定义**: Source 2 中定义工具元数据和执行逻辑的结构体。

**出现来源**: Source 2

**字段**:
```go
type ToolDefinition struct {
    Name        string
    Description string
    InputSchema anthropic.ToolInputSchemaParam
    Function    func(json.RawMessage) (string, error)
}
```

**注册**: 添加到 `Agent.tools` 切片

---

## V

### Validation
**定义**: 代码修改后检查语法、类型、lint 错误的过程。

**出现来源**: Source 1

**强制工具**:
- `run_linter` (Same.dev)
- `get_diagnostics` (Amp)
- `get_errors` (VSCode Agent)

**时机**: 每次编辑后、任务完成后

**错误恢复**: 迭代修复，最多 3-5 次

---

## W

### whitelist_actions
**定义**: InteractiveAgent 和 TextualAgent 的配置，定义在 confirm 模式下跳过确认的命令正则模式。

**出现来源**: Source 3

**示例**（猜测）:
```yaml
whitelist_actions:
  - "^ls "
  - "^cat "
  - "^git status"
```

**安全**: 需避免危险命令（如 `rm -rf /`）

---

## 术语统计

### 高频术语（3 来源均出现）

- Agent
- Tool
- Validation
- Error Handling
- Message History

### 中频术语（2 来源出现）

- edit_file
- Parallel Execution
- Environment
- Template

### 低频术语（仅 1 来源）

- AGENTS.md (Source 1)
- ToolDefinition (Source 2)
- NonTerminatingException (Source 3)

---

## 参考资料

- [Source 1: System Prompts](../sources/notes-system-prompts.md)
- [Source 2: Coding Agent](../sources/notes-coding-agent.md)
- [Source 3: Mini SWE Agent](../sources/notes-mini-swe-agent.md)

**最后更新**: 2025-10-19
