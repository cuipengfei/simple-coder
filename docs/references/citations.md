# Citations - 引用与链接

本文档提供所有结论的完整引用与可追溯锚点。

---

## DeepWiki 来源

### Source 1: System Prompts and Models of AI Tools

**仓库**: x1xhlol/system-prompts-and-models-of-ai-tools
**DeepWiki URL**: https://deepwiki.com/x1xhlol/system-prompts-and-models-of-ai-tools

**查询记录**:

1. **系统提示设计与沟通策略**
   - 查询: "What are the core system prompt design patterns and communication strategies used across different AI coding tools?"
   - DeepWiki 搜索 URL: https://deepwiki.com/search/what-are-the-core-system-promp_e0022435-fd4a-4294-8859-705f42cd4d04
   - 关键发现: 身份定义、并行执行模型、沟通策略、代码占位符标准化

2. **工具集成与代码修改策略**
   - 查询: "What are the tool integration and execution patterns? How do different systems handle code modification strategies?"
   - DeepWiki 搜索 URL: https://deepwiki.com/search/what-are-the-tool-integration_7419c90c-11b1-4b92-b28d-45ac213a5a7a
   - 关键发现: edit_file 模式、insert_edit 模式、replace 模式变体、文件操作工具

3. **验证与错误处理**
   - 查询: "What are the validation and error handling patterns used?"
   - DeepWiki 搜索 URL: https://deepwiki.com/search/what-are-the-validation-and-er_caed8262-89e3-419f-8194-e508dc79dc74
   - 关键发现: 诊断工具、错误恢复策略、验证循环、执行序列

4. **任务规划与内存系统**
   - 查询: "What task planning and memory systems are used?"
   - DeepWiki 搜索 URL: https://deepwiki.com/search/what-task-planning-and-memory_8a23489a-5d7a-4eee-8bad-e0c52b31e0f6
   - 关键发现: TODO 管理、AGENTS.md、create_memory 工具

**相关 Wiki 页面**:
- [Major AI Development Platforms](https://deepwiki.com/x1xhlol/system-prompts-and-models-of-ai-tools#3)
- [System Prompt Design and Communication Patterns](https://deepwiki.com/x1xhlol/system-prompts-and-models-of-ai-tools#5.5)

---

### Source 2: How to Build a Coding Agent

**仓库**: ghuntley/how-to-build-a-coding-agent
**DeepWiki URL**: https://deepwiki.com/ghuntley/how-to-build-a-coding-agent

**查询记录**:

1. **完整架构**
   - 查询: "What is the complete architecture of the coding agent system?"
   - DeepWiki 搜索 URL: https://deepwiki.com/search/what-is-the-complete-architect_5cc97ea2-c5ff-4cf6-a9de-74b31cd913e1
   - 关键发现: Agent 结构体、工具系统设计、Agent 类型演进

2. **事件循环与 API 交互**
   - 查询: "How is the getUserMessage function and shared event loop pattern implemented?"
   - DeepWiki 搜索 URL: https://deepwiki.com/search/how-is-the-getusermessage-func_3485a7cd-5b30-458a-83bb-a5341f58dd29
   - 关键发现: getUserMessage 闭包、事件循环、工具结果处理、对话历史管理、API 交互

3. **工具实现细节**
   - 查询: "What are the detailed implementations of individual tools?"
   - DeepWiki 搜索 URL: https://deepwiki.com/search/what-are-the-detailed-implemen_02af4427-1a50-4ec4-9a03-8d80d2663051
   - 关键发现: read_file, list_files, bash, edit_file, code_search 实现

4. **开发环境配置**
   - 查询: "What are the development environment setup requirements?"
   - DeepWiki 搜索 URL: https://deepwiki.com/search/what-are-the-development-envir_0a3d3a44-b647-468c-a569-a152d9efe4fa
   - 关键发现: devenv 配置、构建系统、依赖管理、环境变量

**相关 Wiki 页面**:
- [Agent System Architecture](https://deepwiki.com/ghuntley/how-to-build-a-coding-agent#2)
- [Agent Implementations](https://deepwiki.com/ghuntley/how-to-build-a-coding-agent#3)

---

### Source 3: Mini SWE Agent

**仓库**: SWE-agent/mini-swe-agent
**DeepWiki URL**: https://deepwiki.com/SWE-agent/mini-swe-agent

**查询记录**:

1. **完整架构**
   - 查询: "What is the complete architecture of the SWE agent system?"
   - DeepWiki 搜索 URL: https://deepwiki.com/search/what-is-the-complete-architect_47c56d2d-1207-454c-9c7c-4942b6f109c8
   - 关键发现: 三组件架构、Agent 系统设计、Model 集成、执行环境、CLI 与批处理

2. **Agent step() 和 run() 方法**
   - 查询: "What are the specific patterns used in the Agent's step() and run() methods?"
   - DeepWiki 搜索 URL: https://deepwiki.com/search/what-are-the-specific-patterns_5dfffdca-16da-48b4-8d61-85eb4780ecc5
   - 关键发现: 模板系统、消息历史、异常处理、配置管理

3. **Docker 与 Singularity 环境**
   - 查询: "What are the specific implementation details of Docker and Singularity environments?"
   - DeepWiki 搜索 URL: https://deepwiki.com/search/what-are-the-specific-implemen_17764d06-50d6-4659-8647-24bf4fa445af
   - 关键发现: DockerEnvironment 生命周期、SingularityEnvironment 沙箱、SWE-Bench 数据集处理、并行处理、结果保存

4. **交互模式详情**
   - 查询: "What are the specific details about the Textual User Interface and Interactive modes?"
   - DeepWiki 搜索 URL: https://deepwiki.com/search/what-are-the-specific-details_cbd074d8-bf93-460f-9c9c-30cfcfe9931f
   - 关键发现: InteractiveAgent, TextualAgent, confirm/yolo/human 模式

**相关 Wiki 页面**:
- [Overview](https://deepwiki.com/SWE-agent/mini-swe-agent#1)
- [Configuration System](https://deepwiki.com/SWE-agent/mini-swe-agent#6)
- [Template System](https://deepwiki.com/SWE-agent/mini-swe-agent#6.2)
- [SWE-Bench Integration](https://deepwiki.com/SWE-agent/mini-swe-agent#7)
- [Interactive Modes](https://deepwiki.com/SWE-agent/mini-swe-agent#3.3)
- [User Interfaces](https://deepwiki.com/SWE-agent/mini-swe-agent#5)
- [Textual User Interface](https://deepwiki.com/SWE-agent/mini-swe-agent#5.2)

---

## 引用格式规范

### 来源标注

所有结论使用以下格式标注来源：

**直接引用**:
> "You are AI coding assistant and agent manager, powered by gpt-4.1."
>
> —— Source 1: Same.dev 系统提示

**改述引用**:
> Amp 要求在任务完成后强制运行 `get_diagnostics` 和任何可用的 lint/typecheck 命令。
>
> —— Source 1: Amp 验证规则

**代码引用**:
```go
type Agent struct {
    client         *anthropic.Client
    getUserMessage func() (string, bool)
    tools          []ToolDefinition
    verbose        bool
}
```
—— Source 2: `Agent` 结构体定义

### 锚点约定

- **Wiki 页面**: 使用 DeepWiki URL + 章节锚点（如 `#3.2`）
- **文档内部**: 使用相对路径 + Markdown 锚点（如 `../sources/notes-system-prompts.md#31-并行执行模型`）

---

## 引用覆盖率

### Source 1 关键结论引用

| 结论 | 引用锚点 |
|------|----------|
| Same.dev 默认并行策略 | DeepWiki 搜索 e0022435, docs/sources/notes-system-prompts.md#121 |
| Amp TODO 系统 | DeepWiki 搜索 8a23489a, docs/sources/notes-system-prompts.md#13 |
| 代码占位符标准化 | DeepWiki 搜索 e0022435, docs/sources/notes-system-prompts.md#22 |
| 验证强制规则 | DeepWiki 搜索 caed8262, docs/sources/notes-system-prompts.md#23 |
| edit_file 平台对比 | DeepWiki 搜索 7419c90c, docs/sources/notes-system-prompts.md#32 |

### Source 2 关键结论引用

| 结论 | 引用锚点 |
|------|----------|
| Agent 事件循环 | DeepWiki 搜索 3485a7cd, docs/sources/notes-coding-agent.md#12 |
| ToolDefinition 结构 | DeepWiki 搜索 5cc97ea2, docs/sources/notes-coding-agent.md#21 |
| GenerateSchema 实现 | DeepWiki 搜索 02af4427, docs/sources/notes-coding-agent.md#22 |
| code_search 工具 | DeepWiki 搜索 02af4427, docs/sources/notes-coding-agent.md#325 |
| devenv 配置 | DeepWiki 搜索 0a3d3a44, docs/sources/notes-coding-agent.md#42 |

### Source 3 关键结论引用

| 结论 | 引用锚点 |
|------|----------|
| 三组件架构 | DeepWiki 搜索 47c56d2d, docs/sources/notes-mini-swe-agent.md#11 |
| DefaultAgent.run() | DeepWiki 搜索 5dfffdca, docs/sources/notes-mini-swe-agent.md#21 |
| 异常驱动控制流 | DeepWiki 搜索 5dfffdca, docs/sources/notes-mini-swe-agent.md#22 |
| Jinja2 模板系统 | DeepWiki 搜索 5dfffdca, docs/sources/notes-mini-swe-agent.md#31 |
| DockerEnvironment | DeepWiki 搜索 17764d06, docs/sources/notes-mini-swe-agent.md#54 |
| SWE-Bench 批处理 | DeepWiki 搜索 17764d06, docs/sources/notes-mini-swe-agent.md#62 |
| InteractiveAgent | DeepWiki 搜索 cbd074d8, docs/sources/notes-mini-swe-agent.md#71 |
| TextualAgent | DeepWiki 搜索 cbd074d8, docs/sources/notes-mini-swe-agent.md#72 |

---

## 验证清单

- [x] 所有三个来源的 DeepWiki URL 已记录
- [x] 所有查询的搜索 URL 已保存
- [x] 关键结论与 DeepWiki 锚点关联
- [x] 引用格式统一（直接引用、改述、代码）
- [x] 内部文档交叉引用正确

---

## 参考资料

- [Source 1: System Prompts](../sources/notes-system-prompts.md)
- [Source 2: Coding Agent](../sources/notes-coding-agent.md)
- [Source 3: Mini SWE Agent](../sources/notes-mini-swe-agent.md)
- [Architecture Patterns](../syntheses/architecture-patterns.md)
- [Code Modification Strategies](../syntheses/code-modification.md)
- [Open Questions](../syntheses/open-questions.md)
- [Glossary](./glossary.md)

**最后更新**: 2025-10-19
