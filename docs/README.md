# Coding Agent Research Documentation

本目录包含基于三个核心来源的 Coding Agent 系统研究文档。所有结论均基于实际阅读，避免基于标题或常识的推断。

## 文档结构

### 📚 Sources - 来源详细笔记
每个来源的完整摘录与分析：
- [Source 1: System Prompts and Models of AI Tools](./sources/notes-system-prompts.md)
- [Source 2: How to Build a Coding Agent](./sources/notes-coding-agent.md)
- [Source 3: Mini SWE Agent](./sources/notes-mini-swe-agent.md)

### 🔄 Syntheses - 主题综合分析
跨来源的主题整合与对比：
- [Architecture Patterns](./syntheses/architecture-patterns.md) - 架构设计模式对比
- [Tool Systems](./syntheses/tool-systems.md) - 工具系统设计与集成
- [Code Modification Strategies](./syntheses/code-modification.md) - 代码修改策略对比
- [Validation & Error Handling](./syntheses/validation-error-handling.md) - 验证与错误处理
- [Task Planning & Memory](./syntheses/task-planning-memory.md) - 任务规划与内存系统
- [Interactive Modes](./syntheses/interactive-modes.md) - 交互模式设计
- [Open Questions](./syntheses/open-questions.md) - 未解问题与空白点

### 📖 References - 术语与引用
- [Glossary](./references/glossary.md) - 术语表（基于出现频次）
- [Citations](./references/citations.md) - 完整引用与链接

## 核心发现概览

### 架构模式
1. **系统提示工程**（Source 1）- 多平台的身份定义、沟通策略、并行执行模式
2. **Go + Anthropic API**（Source 2）- 事件循环 + 工具注册的简洁实现
3. **三组件协议**（Source 3）- Agent/Model/Environment 的可扩展架构

### 关键差异
- **并行执行哲学**：Same.dev（默认并行）vs VSCode Agent（限制并行）vs mini-swe-agent（无显式指令）
- **代码修改工具**：edit_file vs insert_edit vs line-replace vs search_replace
- **交互模式**：REPL CLI vs Textual TUI vs 批处理
- **环境隔离**：本地执行 vs Docker vs Singularity

### 共识点
- **验证必要性**：所有系统都强制修改后验证（lint/diagnostics/get_errors）
- **错误恢复策略**：迭代修复 + 用户升级模式
- **消息历史管理**：线性对话历史 + 工具结果反馈循环
- **模板化提示**：Jinja2 或类似机制进行动态内容生成

## 使用建议

1. **快速查找**：使用 [Glossary](./references/glossary.md) 定位术语
2. **深入理解**：阅读 [Sources](./sources/) 中的原始摘录与引用
3. **对比分析**：查看 [Syntheses](./syntheses/) 中的跨源主题分析
4. **追溯源头**：所有结论通过 [Citations](./references/citations.md) 可追溯到原文

## 质量保证

- ✅ **可追溯性**：所有结论标注原文引用与锚点
- ✅ **不臆测**：不基于标题/常识推断内容
- ✅ **简洁清晰**：短句与列表优先，统一术语与链接格式
- ✅ **完整性**：覆盖架构、工具、验证、内存、交互五大维度

---

最后更新：2025-10-19
基于来源：
- x1xhlol/system-prompts-and-models-of-ai-tools
- ghuntley/how-to-build-a-coding-agent
- SWE-agent/mini-swe-agent
