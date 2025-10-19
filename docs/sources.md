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
- R5 成稿与导航：在 docs/ 建立导航与主题页，基于阅读结果决定结构与标题。

拟产出（占位命名，阅读后再定最终标题）
- docs/README.md：导航与范围说明。
- docs/sources/
  - notes-source-1.md
  - notes-source-2.md
  - notes-source-3.md
- docs/syntheses/
  - themes-TBD.md（主题与结论，来自 R3/R4）
  - open-questions-TBD.md（未解问题）
- docs/references/
  - glossary-TBD.md（术语表，基于出现频次与上下文）
  - citations.md（完整引用与链接/锚点列表）

里程碑
- M1：完成三份 notes（R1–R2）。
- M2：完成 syntheses（R3–R4）。
- M3：完成 references 与导航（R5）。

质量标准
- 可追溯：所有结论需有对应原文引用与锚点。
- 不臆测：不以标题/常识推断；不引入实现细节。
- 简洁清晰：短句与列表优先；统一术语与链接格式。