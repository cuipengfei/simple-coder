# FEATURES：教学用 Simple Coder Agent 功能与示例（含最小 UI 与会话期上下文）

范围
- 教学用途；单回合、单工具调用；会话期保留上下文（应用未关闭时）；仅仓库根内操作；最小 UI（待实现）。

当前实现状态
- 已实现工具：read_file（ReadFileTool）、list_dir / glob（ListDirTool，已含结果上限与截断语义）、search（SearchTool）、replace（ReplaceTool）、路径安全（PathValidator）。
- 已实现：AgentService（auto 路由 + fallback）、Controller `/api/agent`。
- 未实现：UI、集成测试（端到端 ChatClient + Controller）、ListDirTool 截断单测（逻辑已实现但缺测试）。

功能清单（V0 目标）
- read_file：读取文件文本。
- list_dir / glob：列出目录或按模式匹配文件。（已实现：结果数量上限 `max-list-results`，超过截断并附提示）
- search（regex/contains）：检索文件内容（统一截断语义：达到 max-search-results 早停；目录完整遍历恰好等于上限不标记截断；截断消息示例 `[TRUNCATED: reached limit 50 before completing search]`）。
- replace（exact，唯一匹配）：精确字符串替换，old_string 必须唯一出现。
- 会话期上下文：UI 层维护（尚未实现）。
- 最小 UI：单页、输入框 + 工具下拉 + 执行按钮 + 结果区 + 简要历史（尚未实现）。

功能描述
- read_file
  - 输入：相对路径（可选：行区间 file:START-END）。
  - 输出：带行号文本；超过 max-file-lines 截断并提示。
  - 失败：文件不存在 / 路径越界 / 非 regular file。
- list_dir / glob
  - 输入：目录或通配模式（Java PathMatcher glob）。
  - 输出：相对路径列表；超过 max-list-results 截断并附 `[TRUNCATED: first N items]`。
  - 失败：目录不存在 / 路径越界 / 输入指向文件。
- search（contains / regex）
  - 输入：`Search ['regex'|'case-sensitive'] 'pattern' in path`。
  - 输出：`file:line:snippet` 列表，snippet >100 字符截断；达到上限早停标记示例 `[TRUNCATED: reached limit 50 before completing search]`（目录完整遍历恰好达到上限不标记）。
  - 失败：路径不存在 / 正则语法错误 / prompt 解析失败 / 越界。
- replace（exact）
  - 输入：`Replace 'old' with 'new' in file`。
  - 验证：old != new；old 唯一出现；路径安全；文件存在且为 regular file。
  - 输出：成功摘要（不返回 diff）。
  - 失败：未命中 / 多次出现 / old==new / 解析失败 / 越界。
- 会话期上下文（计划）
  - 范围：保留最近 N 次请求/结果摘要，辅助后续轻任务引用。
  - 生命周期：刷新/关闭后清空；不做持久化。
- 最小 UI（计划）
  - 结构：多行输入框、工具选择（Auto/read/list/search/replace）、Submit、结果区、历史侧栏。
  - 状态：Loading / Error / Empty。

示例用法
- 搜索后读取：
  1) `Search 'Agent' in docs/sources` → 返回命中列表。
  2) `Read docs/sources/notes-system-prompts.md:1-20` → 前 20 行。
- read_file：`Read docs/syntheses/open-questions.md:1-15` → 前 15 行（可能截断提示）。
- list_dir：`List docs/sources` → Markdown 文件列表。
- glob：`List src/**/*.java` → 所有 Java 文件（大结果可能截断）。
- search：`Search regex 'foo[0-9]+bar' in src`。
- replace：`Replace 'Open Questions' with 'Unresolved Questions' in docs/syntheses/open-questions.md`（需唯一）。

TODO / 差异追踪
- [ ] ListDirTool 截断单测（大目录 > max-list-results 验证消息格式）。
- [ ] 集成测试：Controller + ChatClient 端到端（auto/fallback）。
- [ ] 最小 UI 单页（静态资源）。
- [ ] 配置与文档同步：在用户指南中展示截断消息示例。
- [ ] ReplaceTool 重叠子串风险评估（仅在真实误用出现时）。

注意
- 上下文与 UI 尚未实现；当前仅工具层与服务层准备。
- 所有文件操作受 PathValidator 限制；ReplaceTool 强制唯一匹配保障安全。
- AgentService 在响应 message 前缀添加 `[tool=name]` 便于追踪（UI 需适配）。
