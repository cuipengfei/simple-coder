# FEATURES：教学用 Simple Coder Agent 功能与示例（含最小 UI 与会话期上下文）

范围
- 教学用途；单回合、以“单工具调用”为教学目标；会话期保留上下文（客户端维护）；仅仓库根内操作；最小 UI（已实现）。

当前实现状态
- 已实现工具：ToolsService 统一服务包含四个 @Tool 方法（readFile、listFiles、searchText、replaceText），并由 PathValidator 做路径安全。
- 已实现：AgentService（Spring AI 原生 tool-calling）、Controller `/api/agent`、最小 UI（含客户端上下文历史，最多 20 条；结果入历史前在前端截断至约 500 字符）。
- 已实现：截断语义
  - readFile：超出 `max-file-lines` → `[TRUNCATED: showing first N lines, M more available]`
  - listFiles：结果数超出 `max-list-results` → `[TRUNCATED: first N items]`
  - searchText：达到 `max-search-results` 早停 → `[TRUNCATED: reached limit N before completing search]`
    - 说明：若“恰好等于上限且已完整遍历”则不标记截断。
- 注意：后端当前忽略 ToolRequest.toolType；工具选择与参数抽取完全由 Spring AI 原生 tool-calling 决定。UI 下拉仅作提示。
- 未实现：ToolsService 单元测试、端到端集成测试（Controller + ChatClient + 实际模型）。

功能清单（V0 目标）
- read_file：读取文件文本（可选行号范围）。
- list_dir / glob：列出目录或按模式匹配的文件与目录（排序后返回）。
- search（regex/contains）：检索文本，返回 `file:line:snippet`（snippet 最长 100 字符）。
- replace（exact、唯一）：精确字符串替换，要求 old 与 new 不同，且 old 在文件中“恰好出现一次”。
- 会话期上下文（已实现）：客户端维护最近 20 次请求/结果摘要，随请求发送给后端；服务端无状态。
- 最小 UI（已实现）：单页，输入框 + 工具下拉（提示）+ 提交按钮 + 结果区 + 历史侧栏；Loading/Error 状态。

功能描述
- read_file
  - 输入：相对路径；可选 `startLine` / `endLine`（自然语言由模型解析）。
  - 输出：带行号文本；若选中行数超过 `max-file-lines` 则按上述文案标注截断。
  - 失败：文件不存在 / 路径越界 / 非 regular file / 行号非法。
- list_dir / glob
  - 输入：目录或通配模式（Java PathMatcher glob）。
  - 输出：相对路径列表（文件与目录），排序后返回；结果数超出 `max-list-results` 则附 `[TRUNCATED: first N items]`。
  - 失败：目录不存在 / 路径越界 / 输入指向文件。
- search（contains / regex）
  - 输入：pattern、searchPath、可选 `isRegex`、`caseSensitive`。
  - 输出：`file:line:snippet` 列表（snippet >100 字符截断）；达到上限早停时附 `[TRUNCATED: reached limit N before completing search]`；若恰等于上限且完成遍历则不标记。
  - 失败：路径不存在 / 正则语法错误 / 路径越界。
- replace（exact）
  - 输入：`filePath`、`oldString`、`newString`。
  - 校验：old != new；old 在文件中出现次数必须为 1；路径安全；文件存在且为 regular file。
  - 输出：成功摘要（不返回 diff）。
  - 失败：未命中 / 多次出现 / old==new / 解析失败 / 越界。

示例用法（自然语言优先）
- 列出 Java 文件：`List src/**/*.java`
- 搜索文本：`Search pattern='AgentService' in src/main/java caseSensitive=false`
- 读取文件：`Read src/main/java/com/simplecoder/service/AgentService.java lines 1-40`
- 替换文本：`Replace old='gpt-4.1' with new='gpt-4.1-mini' in src/main/resources/application.yml`（要求唯一）

TODO / 差异追踪
- [ ] ToolsService 单元测试（验证截断、边界条件、错误处理）。
- [ ] 端到端集成测试（Controller + ChatClient + 实际模型）。
- [ ] 文档统一展示截断消息示例（本页已列出，后续在 IMPLEMENTATION.md 汇总）。
- [ ] replaceText 叠加/重叠子串风险评估（仅在真实误用出现时）。

注意
- 所有文件操作受 PathValidator 限制；replaceText 强制唯一匹配保障安全。
- 自然语言支持：用户无需学习结构化命令，直接描述需求即可；工具与参数由模型解析。
- 上下文历史：由前端自动维护并发送，用户无感知。