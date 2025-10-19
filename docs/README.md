# Coding Agent Research Documentation

本目录汇总三个来源的阅读与综合结论，所有结论基于实际阅读。当前仓库实现进度已导致部分原先计划的 syntheses 文档未生成，已在此标注。

## 文档结构

### 📚 Sources - 来源笔记
- [Source 1: System Prompts and Models of AI Tools](./sources/notes-system-prompts.md)
- [Source 2: How to Build a Coding Agent](./sources/notes-coding-agent.md)
- [Source 3: Mini SWE Agent](./sources/notes-mini-swe-agent.md)

### 🔄 Syntheses - 综合（已存在）
- [Architecture Patterns](./syntheses/architecture-patterns.md)
- [Code Modification Strategies](./syntheses/code-modification.md)
- [Open Questions](./syntheses/open-questions.md)

### 🔄 Syntheses - 计划但尚未产出（占位）
以下在早期 README 中引用过，但文件尚未创建，避免 404：
- validation-error-handling.md（计划整合各平台验证/错误处理模式）
- tool-systems.md（工具系统抽象对比；目前由 architecture-patterns + code-modification 覆盖部分内容）
- task-planning-memory.md（任务规划与内存机制；可合并入 open-questions）
- interactive-modes.md（交互模式与 UI 架构；后续 UI 实现后再补）

### 📖 References - 术语与引用
- [Glossary](./references/glossary.md)
- [Citations](./references/citations.md)

## 当前实现与文档对齐
| 模块 | 代码状态 | 文档状态 | 差异/待补 |
|------|----------|----------|-----------|
| Models (ToolRequest/ToolResponse/ContextEntry) | 已实现 + 单测 | IMPLEMENTATION.md 描述 | 对齐 |
| PathValidator | 已实现 + 单测 | IMPLEMENTATION.md | 对齐 |
| ReadFileTool | 已实现 + 单测 | FEATURES.md 描述 | 对齐 |
| ListDirTool | 已实现 + 单测 | FEATURES.md 声明需上限 | 缺少结果上限（开放问题） |
| SearchTool | 已实现 + 单测（统一截断语义） | GEMINI.md/FEATURES.md 更新 | 文档已修正旧不一致 |
| ReplaceTool | 已实现 + 单测 | FEATURES.md | 对齐 |
| AgentService / Controller / UI | 未实现 | IMPLEMENTATION.md Phase 4/5 | 待开发 |

## 关键差异 & TODO 汇总
- ListDirTool 上限缺失（添加配置键 `simple-coder.max-list-results` 计划）。
- 模型配置使用 `gpt-3.5-turbo` 需更新为当前可用模型。
- 早期 README 引用的缺失 syntheses 文件已在本页标注为占位，避免死链；待实际内容出现再恢复链接。

## 使用建议
1. 优先阅读 Sources 获取原始上下文。
2. 综合对比查看 Architecture Patterns 与 Code Modification Strategies。
3. 查看 Open Questions 了解尚未填补的研究空白（并行执行收益、消息历史修剪策略等）。
4. 参考 Glossary 快速定位术语；Citations 做追溯。

## 质量保证
- ✅ 可追溯：Sources + Citations 链接完整。
- ✅ 不臆测：仅引用已读内容；未产出文件标注占位不伪造。
- ✅ 差异显式：开放问题集中在 GEMINI.md 与本页表格。

## 后续文档工作
| 任务 | 触发条件 | 目标 |
|------|----------|------|
| validation-error-handling.md | AgentService 实现后 | 汇总工具执行后验证策略 |
| task-planning-memory.md | 引入 auto + 上下文使用后 | 梳理最小任务规划模式 |
| interactive-modes.md | UI 原型完成后 | 总结单页交互与后续扩展模式 |
| tool-systems.md | 如果架构继续扩展 | 独立提炼工具抽象层比较 |

## 最后更新
2025-10-19（与实现同步；后续更改请更新本页 “关键差异 & TODO” 区域）