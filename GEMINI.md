# GEMINI.md

角色与流程
- Gemini：规划/评审（Plan/Review）；Claude Code：实现/测试（Implement/Test）。
- 流程：plan → code → review → iterate。

教育用途声明（Educational Scope）
- 本仓库为“教学用最小 Coding Agent”示例，不是生产系统；评审侧重：功能正确性、与文档规格一致性、最小安全（路径越界、防误替换）。
- 不在评审范围：高并发、超大规模性能优化、复杂安全加固（除非直接影响示例正确性或清晰度）。
- 当进行代码或测试评审时，应明确：仅指出会影响学习理解或导致明显错误/误导的“实质问题”，避免提出纯生产级优化（如微性能、抽象层泛化、扩展性重构）。
- 允许保留简化实现（一次性读取文件、行级匹配、无差异预览）并在需要时通过注释或文档标注其“可扩展点”。

事实基线（当前仓库）
- 技术栈：Java 21，Spring Boot 3.5.6，Spring AI 1.0.3（spring-ai-bom），依赖 spring-ai-starter-model-openai。
- 架构：无状态服务端；客户端在 ToolRequest.contextHistory 携带上下文；无 ConversationContext。
- 已有代码：SimpleCoderApplication；Models（ContextEntry / ToolRequest / ToolResponse）及单测；Tools（PathValidator / ListDirTool / ReadFileTool / SearchTool / ReplaceTool）及单测；application.yml。
- 待实现：AgentService（auto 工具选择 + LLM 调用）、Controller（REST /api/agent）、最小 UI（单页 HTML/JS）、集成测试（端到端）。

当前开放问题（需后续处理）
1. ListDirTool 缺少结果数量上限与截断提示；FEATURES.md 规格声明“上限提示”，实现不符，可能在大目录下输出过长。
2. application.yml 使用模型 gpt-3.5-turbo（可能不可用），需替换为当前实际可用模型名（运行前确认供应商可用列表）。
3. 文档与配置示例不一致：IMPLEMENTATION.md 中 application.yml 示例使用旧键 `spring.ai.openai.model`，实际配置使用 `spring.ai.openai.chat.options.model`；保持统一避免误导。

已解决问题（保留追溯，不再列为开放问题）
- SearchTool 截断语义不一致：已统一行为（单文件达到上限→截断；目录完整遍历恰好达到上限→不截断），测试 `testSearchDirectoryExactLimitNoTruncation` 验证。
- ReadFileTool 行号与性能问题：已用基于索引循环修复，测试 `testReadDuplicateLinesLineNumbersUnique` 验证。

硬性约束
- 单轮交互：每次请求至多一个 Tool；无并行、无多步规划、无会话记忆。
- 安全：文件操作仅限仓库根；ReplaceTool 强制 old_string 唯一匹配。
- 依赖：任何使用前先查 pom.xml；不臆测新增依赖。

配置约定
- simple-coder.*：repo-root、max-file-lines、max-search-results。
- Spring AI：`spring.ai.openai.chat.options.model`；API key 环境变量 OPENAI_API_KEY。
- 模型名需更新为实际可用（gpt-3.5-turbo 可能不可用）。

信息来源优先级
1) docs/（sources、syntheses、references）
2) DeepWiki 原文（实际阅读）
3) 仍不明确时询问用户，并在 docs/ 补充引用锚点

工作准则
- 只指出“实质问题”，避免吹毛求疵。
- 修改前阅读相关代码/测试/配置，保持最小可行与可验证。

开发命令
- 构建：mvn clean compile
- 测试：mvn test
- 运行：mvn spring-boot:run（需 OPENAI_API_KEY）
- 打包：mvn clean package