# Architecture Patterns - 架构设计模式对比

本文档对比分析三个来源中的核心架构模式。

---

## 1. 架构范式对比

| 维度 | Source 1 (System Prompts) | Source 2 (Coding Agent) | Source 3 (Mini SWE Agent) |
|------|---------------------------|-------------------------|---------------------------|
| **核心模型** | 系统提示工程驱动 | 事件循环 + 工具注册 | 三组件协议（Agent/Model/Environment） |
| **语言/技术栈** | 多平台（Python/TS/其他） | Go + Anthropic SDK | Python + 协议抽象 |
| **复杂度** | 高（多平台配置） | 中（约 200-300 行核心代码） | 低（核心约 100 行） |
| **扩展性** | 配置驱动 | 工具定义切片 | 协议实现 |

---

## 2. 事件循环设计

### 2.1 Source 2: Go 事件循环

**位置**: `Agent.Run()` 方法

**核心流程**:
```
1. 获取用户输入 (getUserMessage)
2. 添加到对话历史 (conversation []MessageParam)
3. 调用 API (runInference)
4. 处理响应
   - 文本块 → 显示给用户
   - tool_use 块 → 执行工具，收集结果
5. 发送工具结果回 Claude
6. 回到步骤 1
```

**特点**:
- **同步循环**
- **完整对话历史持久化**（`conversation` 切片）
- **工具结果即时反馈**（作为新的用户消息）

### 2.2 Source 3: Agent.step() 循环

**位置**: `DefaultAgent.run()` + `step()`

**核心流程**:
```
run():
1. 初始化消息历史（system + instance 模板）
2. 循环调用 step()
3. 捕获异常（NonTerminating → 继续，Terminating → 退出）

step():
1. 查询模型 (query)
2. 解析动作 (parse_action - 提取 bash 代码块)
3. 执行动作 (execute_action - 调用 env.execute)
4. 检查完成 (has_finished)
5. 渲染观察 (render_template - action_observation_template)
6. 添加到历史 (add_message)
```

**特点**:
- **异常驱动控制流**
- **模板化观察反馈**
- **无工具抽象**（仅 bash 命令）
- **线性消息历史**

### 2.3 对比总结

| 特性 | Source 2 (Go) | Source 3 (Python) |
|------|---------------|-------------------|
| **用户输入** | 闭包函数 `getUserMessage` | 模板初始化（无持续输入）|
| **API 调用** | `runInference()` 封装 | `model.query()` 协议方法 |
| **工具执行** | 查找 `ToolDefinition` → 执行 `Function` | 解析 bash 代码块 → `env.execute()` |
| **结果反馈** | `ToolResultBlock` 作为用户消息 | 观察文本通过模板格式化 |
| **循环控制** | 无限循环（用户退出） | 异常驱动退出 |

---

## 3. 工具/动作系统

### 3.1 Source 1: 系统提示中的工具分类

**四大类**:
1. **文件操作**: create_file, edit_file, read_file, write_file
2. **搜索/发现**: grep, code_search, glob, semantic_search
3. **执行**: bash, run_terminal, execute
4. **验证**: get_diagnostics, run_linter, get_errors

**特点**:
- 通过提示词定义工具行为
- 无代码级抽象，依赖 LLM 理解
- 平台间工具名称和参数不统一

### 3.2 Source 2: ToolDefinition 结构

**Go 结构体**:
```go
type ToolDefinition struct {
    Name        string
    Description string
    InputSchema anthropic.ToolInputSchemaParam
    Function    func(json.RawMessage) (string, error)
}
```

**特点**:
- **类型安全**: Go 结构体 + JSON Schema 自动生成
- **模块化**: 每个工具独立定义
- **统一执行**: 集中式工具查找和调用

**示例工具**: read_file, list_files, bash, edit_file, code_search

### 3.3 Source 3: 无工具抽象

**设计哲学**:
- **仅 bash 命令**：无自定义工具或函数调用
- **无状态执行**：每个动作通过独立的 `subprocess.run` 调用
- **极简主义**：核心 Agent 约 100 行

**动作流程**:
```
模型响应 → 正则提取 bash 代码块 → env.execute(command) → 返回 (output, returncode)
```

### 3.4 对比总结

| 维度 | Source 1 | Source 2 | Source 3 |
|------|----------|----------|----------|
| **抽象层级** | 提示词描述 | Go 结构体 + Schema | 无抽象（bash 原生） |
| **类型安全** | 无 | 强类型（Go + Schema） | 弱类型（字符串命令） |
| **扩展性** | 新增提示描述 | 新增 ToolDefinition | 环境切换（Docker/Singularity） |
| **验证** | LLM 自行判断 | Schema 验证 | 无验证（依赖 bash 语法） |

---

## 4. 并行执行策略

### 4.1 Source 1: 多平台策略对比

| 平台 | 策略 | 关键指令 |
|------|------|----------|
| **Same.dev** | 激进并行 | "DEFAULT TO PARALLEL" <br> 3-5x 速度提升 |
| **v0** | 上下文收集并行 | 专注于 read 操作（GrepRepo, LSRepo, ReadFile） |
| **VSCode Agent** | 限制性并行 | 禁止并行 `semantic_search` 和多次 `run_in_terminal` |
| **Amp** | 多层级并行 | 并行 reads/searches/diagnostics/writes（冲突时串行）/subagents |
| **Windsurf** | 无显式指令 | 依赖模型判断 |

**共识**:
- Read 操作普遍可并行
- Write 操作需冲突检测
- 执行类工具（bash/terminal）通常串行

### 4.2 Source 2 & 3: 无并行机制

- **Source 2 (Go)**: 串行工具执行，逐个处理 `tool_use` 块
- **Source 3 (Python)**: 单步执行，每次 `step()` 仅处理一个动作

**原因**:
- 简化实现
- 避免状态冲突
- 适用于单用户交互场景

---

## 5. 消息历史管理

### 5.1 Source 2: Go 切片

```go
conversation := []anthropic.MessageParam{
    anthropic.NewUserMessage(userInput),
    message.ToParam(),  // Claude 响应
    anthropic.NewUserMessage(toolResults...),
}
```

**特点**:
- 完整对话历史（用户消息 + 模型响应 + 工具结果）
- 每次 API 调用传递完整 `conversation`
- 无消息修剪或汇总

### 5.2 Source 3: Python 列表

```python
self.messages = [
    {"role": "system", "content": system_template},
    {"role": "user", "content": instance_template},
    {"role": "assistant", "content": model_response},
    {"role": "user", "content": observation},
]
```

**特点**:
- 线性消息列表
- 观察（命令输出）作为用户消息
- 通过 `add_message(role, content)` 追加

### 5.3 Source 1: 隐式管理

系统提示中未显式描述消息历史管理，假设由底层 API/框架处理。

---

## 6. 错误处理与异常机制

### 6.1 Source 1: 提示级错误恢复

**验证强制**:
- Same.dev: `run_linter` 每次编辑后，最多 3 次修复循环
- Amp: `get_diagnostics` 任务完成后强制运行
- VSCode Agent: `get_errors` 编辑后验证

**升级策略**:
- 3-5 次尝试后升级给用户
- 不盲目猜测，方案明确时才修复

### 6.2 Source 2: 返回值错误

```go
func ToolFunc(input json.RawMessage) (string, error) {
    // ...
    if err != nil {
        log.Printf("Error: %v", err)
        return "", err
    }
    return result, nil
}
```

**特点**:
- Go 标准错误返回
- 错误记录日志
- `ToolResultBlock.IsError` 标识错误

### 6.3 Source 3: 异常驱动

**异常层级**:

```python
NonTerminatingException
├── FormatError            # 模型输出格式错误
└── ExecutionTimeoutError  # 命令超时

TerminatingException
├── Submitted              # 任务完成
└── LimitsExceeded         # 步数/成本限制
```

**控制流**:
```python
try:
    self.step()
except NonTerminatingException as e:
    self.add_message("user", str(e))  # 继续
except TerminatingException as e:
    return (type(e).__name__, str(e))  # 退出
```

**特点**:
- 可恢复错误 → 加入历史，继续
- 终止条件 → 返回退出状态

### 6.4 对比总结

| 来源 | 机制 | 可恢复错误 | 终止条件 |
|------|------|------------|----------|
| Source 1 | 提示指导的迭代修复 | Lint 错误、格式错误 | 3-5 次循环后 |
| Source 2 | 返回值 `error` | 工具执行失败 | 用户退出循环 |
| Source 3 | 异常驱动 | FormatError, ExecutionTimeoutError | Submitted, LimitsExceeded |

---

## 7. 配置与扩展性

### 7.1 Source 1: 提示词配置

**机制**:
- `.same/todos.md` (Same.dev)
- `AGENTS.md` (Amp)
- `create_memory` 工具 (Windsurf)

**特点**:
- 运行时可修改
- 特定于平台
- 面向 LLM 的自然语言配置

### 7.2 Source 2: 代码级扩展

**机制**:
- 新增 `ToolDefinition` 到 `Agent.tools` 切片
- 通过 `GenerateSchema[T]()` 自动生成 Schema
- 渐进演进：Chat → Read → List → Bash → Edit → CodeSearch

**特点**:
- 编译时类型检查
- 需重新编译
- 模块化工具定义

### 7.3 Source 3: 协议实现

**机制**:
- 实现 `Agent`、`Model`、`Environment` 协议
- YAML 配置文件（`agent`、`model`、`environment`、`run`）
- CLI 参数覆盖

**示例**:
```yaml
environment:
  class: "DockerEnvironment"
  image: "sweagent/swe-bench:latest"

model:
  class: "AnthropicModel"
  name: "claude-3-5-sonnet-20241022"
```

**特点**:
- 协议驱动的多态
- 运行时配置（YAML）
- 无需重新编译

### 7.4 对比总结

| 维度 | Source 1 | Source 2 | Source 3 |
|------|----------|----------|----------|
| **配置位置** | 文件系统（.md, .same/） | 代码（Go 切片） | YAML + CLI |
| **修改成本** | 低（编辑文件） | 高（重新编译） | 低（编辑 YAML） |
| **类型安全** | 无 | 强 | 中（YAML 验证） |
| **扩展方式** | 新增提示描述 | 新增 ToolDefinition | 实现协议 |

---

## 8. 关键洞察

### 8.1 架构权衡

| 特性 | 提示工程（Source 1） | 工具注册（Source 2） | 协议抽象（Source 3） |
|------|---------------------|---------------------|---------------------|
| **学习曲线** | 低 | 中 | 中-高 |
| **类型安全** | 无 | 高 | 中 |
| **运行时灵活性** | 高 | 低 | 高 |
| **性能开销** | 低（提示解析） | 中（Go 反射） | 低（Python 协议） |
| **调试难度** | 高（LLM 行为不可预测） | 中 | 低（清晰的执行路径） |

### 8.2 设计哲学差异

- **Source 1**: 依赖 LLM 强大的理解能力，通过提示工程控制行为
- **Source 2**: 强类型、编译时验证，适合生产环境
- **Source 3**: 极简主义、协议驱动，适合研究和快速迭代

### 8.3 适用场景

| 场景 | 推荐架构 | 理由 |
|------|----------|------|
| 快速原型 | Source 1 或 3 | 低开发成本，快速迭代 |
| 生产系统 | Source 2 | 类型安全，可预测行为 |
| 研究评估 | Source 3 | 极简实现，易于修改和分析 |
| 多环境部署 | Source 3 | 协议抽象支持多种执行环境 |

---

## 参考资料

- [Source 1: System Prompts](../sources/notes-system-prompts.md)
- [Source 2: Coding Agent](../sources/notes-coding-agent.md)
- [Source 3: Mini SWE Agent](../sources/notes-mini-swe-agent.md)

**最后更新**: 2025-10-19
