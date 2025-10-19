# 文档生产计划（Docs Only，避免基于标题的内容假设）

资料链接：
- https://deepwiki.com/x1xhlol/system-prompts-and-models-of-ai-tools?ref=ghuntley.com
- https://deepwiki.com/ghuntley/how-to-build-a-coding-agent
- https://deepwiki.com/SWE-agent/mini-swe-agent

目标与范围
- 仅产出 docs/ 下的文档；不根据资料标题或常识推断内容，一切以实际阅读为准。

流程（精简）
- R1 获取与预读：记录元数据（作者、更新时间、链接快照）。
- R2 精读与摘录：逐段引用原文要点，标注锚点/段落位置与时间戳。
- R3 归纳与问题：在摘录之上提炼要点与开放问题；严禁引入未证结论。
- R4 交叉对照：三份资料的共识/分歧/空白点。
- R5 成稿与主题导航：在 docs/ 建立导航与主题页，基于阅读结果决定结构与标题。

现有产出（已完成命名）
- sources/notes-system-prompts.md
- sources/notes-coding-agent.md
- sources/notes-mini-swe-agent.md
- syntheses/architecture-patterns.md
- syntheses/code-modification.md
- syntheses/open-questions.md
- references/glossary.md
- references/citations.md

后续占位（条件触发）
- validation-error-handling.md（工具执行后验证策略；待集成测试完成）
- tool-systems.md（工具抽象进一步扩展后，如新增并行或多步）
- task-planning-memory.md（引入会话期上下文与更多规划逻辑后，可能并入 open-questions）
- interactive-modes.md（UI 原型完成后总结交互模式）

里程碑
- M1：完成三份 notes（R1–R2）。
- M2：完成 syntheses（R3–R4）。
- M3：完成 references 与主题导航（R5）。
- M4：新增验证/交互扩展文档（视实现进度）。

质量标准
- 可追溯：所有结论需有对应原文引用与锚点。
- 不臆测：不以标题/常识推断；不引入实现细节未支持的扩展。
- 简洁清晰：短句与列表优先；统一术语与链接格式。

说明
- 原导航 README 已删除；导航功能由 FEATURES.md + 本文件承担。
