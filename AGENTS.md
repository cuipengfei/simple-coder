# AGENTS.md

> 面向在本仓库内执行代码读写/测试/评审的 Coding Agent 统一操作规范。教学示例场景，追求“最小可行 + 可验证”，避免过度设计。中文说明，保留英文技术术语。

## 目标与定位
- 教学用最小 Coding Agent；非生产环境，不讨论高并发/复杂扩展。
- 单轮无状态：客户端每次请求携带全部上下文 (`ToolRequest.contextHistory`)；服务端不存储会话。
- 每次交互至多一次工具调用（单工具自动选择）。

## 技术栈与依赖
- Java 21, Spring Boot 3.5.6
- Spring AI 1.0.3 (`ChatClient` fluent API)
- 构建：Maven（所有依赖以 `pom.xml` 为准；禁止臆测新增依赖）
- 模型名配置：`spring.ai.openai.chat.options.model`；API key 环境变量：`OPENAI_API_KEY`

## 架构概览
```
Client → POST /api/agent (ToolRequest JSON)
  AgentController
    → AgentService.process()
        → chatClient.prompt().tools(toolsService).call().content()
            (Spring AI 自动：选择工具 → 参数抽取 → 调用 @Tool 方法)
                ToolsService (readFile | listFiles | searchText | replaceText)
                  → PathValidator (路径安全)
返回 ToolResponse (success | error + data 可选)
```
特性：无状态 / 单工具 / 自然语言驱动。

## 核心模型 (package `com.simplecoder.model`)
- `ToolRequest`: `prompt`, `toolType`(已废弃保留兼容), `contextHistory`; 方法：`validate()`, `buildContextSummary()`。
- `ToolResponse`: `success`, `message`, `data`, `error`; 静态工厂：`success(...)`, `error(...)`。
- `ContextEntry`: `timestamp`, `prompt`, `result`; `getSummary()` 用于上下文压缩。

## 服务组件
- `ToolsService`: 提供四个 `@Tool` 方法（由 Spring AI 自动选择调用）：
  - `readFile(filePath, startLine?, endLine?)` 读取文件（可选行范围）。
  - `listFiles(path)` 目录列出或 glob。
  - `searchText(pattern, searchPath, isRegex?, caseSensitive?)` 文本/正则搜索。
  - `replaceText(filePath, oldString, newString)` 精确唯一替换。
- `PathValidator`: 保证所有路径均在仓库根内，防止越界访问。
- `AgentService`: 验证请求 → 构建上下文摘要 → 调用 `ChatClient` → 捕获异常统一包装为 `ToolResponse.error(...)`。

## 工具语义与资源约束
- readFile: 超出最大行数 → `"[TRUNCATED: showing first N lines, M more available]"`; 空文件 → `"empty file: 0 lines"`。
- listFiles: 超过最大数量 → `"[TRUNCATED: first N items]"`。
- searchText: 达到结果上限而未完整遍历 → `"[TRUNCATED: reached limit N before completing search]"`；完整遍历恰好等于上限不视为截断。
- replaceText: 要求 `oldString` 在文件中出现次数==1；次数为 0 或 >1 → 返回失败（唯一性按非重叠出现统计）。

## 错误与返回约定
- 工具内部校验失败（路径越界 / 文件不存在 / 唯一性不满足）→ 返回以 `"Error: ..."` 前缀的字符串；由上层封装。
- 服务层统一异常包装：`ToolResponse.error("AgentService error", detail)`。
- 正常工具结果统一通过 `ToolResponse.success("Tool execution result", data)`。

## 安全与硬性约束
- 文件操作仅限仓库根路径（禁止绝对路径与向上越界如 `..`）。
- 单轮交互，不做多步规划 / 并行工具 / 持久记忆。
- `replaceText` 强制唯一匹配避免误写。

## 截断策略（Consistency）
- 行/项/结果超限时使用标准前缀说明，便于测试断言与调试。
- 不在返回体中附加多余统计字段；用自然语言前缀说明即可。

## 工作准则（Agent 行为）
1. 修改前：阅读相关类 + 测试，确认语义；避免未验证的结构性重构。
2. 保持最小可行：仅在需要满足新测试或修复明确缺陷时改动。
3. 不做风格争论：仅指出会影响理解或造成错误的实质问题。
4. 不新增无用抽象：延迟抽象；若有潜在扩展点，用简短英文注释标注 `// extensibility: ...`。
5. 模糊需求务必向用户澄清，避免基于猜测实现。
6. 依赖/版本：任何新增库前先确认教学必要性并征询用户。

## 开发命令
- 构建：`mvn clean compile`
- 测试：`mvn test`
- 运行：`mvn spring-boot:run` (需 `OPENAI_API_KEY`)
- 打包：`mvn clean package`

## 测试策略
- 优先单测已有模型与工具类；修改逻辑后新增针对性测试（最小覆盖）。
- 断言截断/错误消息需匹配精确字符串前缀（保持稳定性）。
- 避免为纯实现细节添加过度测试。

## 评审关注点
- 功能正确性：行为与文档规范一致。
- 安全边界：路径校验、唯一替换、不泄露仓库外内容。
- 可读性：命名清晰、简洁；不额外冗长注释。
- 不关注：微性能优化、通用化抽象、复杂缓存、并行调度。

## 信息来源优先级
1. `docs/` 下 sources / syntheses / references
2. 当前代码与测试实际行为
3. 仍不明确 → 向用户询问，并在 `docs/` 适当文件补充说明

## 变更原则
- Root cause fix：定位源头问题，避免表面补丁。
- DRY：若重复逻辑出现≥2次，考虑抽取方法；但避免为一次性场景过度封装。
- SOLID：保持类职责单一；不要将多种工具语义混入非工具类。

## 执行示例（请求）
```json
POST /api/agent
{
  "prompt": "Read src/main/java/com/simplecoder/service/AgentService.java lines 1-40",
  "toolType": "auto",
  "contextHistory": []
}
```
成功响应示例（截断格式遵循约定）：
```json
{
  "success": true,
  "message": "Tool execution result",
  "data": "Read ... (lines 1-40 of X total)\n\n<file content>",
  "error": null
}
```

## 扩展提示（非当前范围）
> 以下仅为后续可能演进方向，不在当前实现要求：多步计划、并行工具、对话记忆、语义缓存、差异编辑。保留简化实现的教学可读性。

---
本文件为根级别统一规范；后续若子目录新增更细的 AGENTS.md，以子目录文件优先。

