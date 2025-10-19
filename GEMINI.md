# GEMINI.md

角色与流程
- Gemini：规划/评审（Plan/Review）；Claude Code：实现/测试（Implement/Test）。
- 流程：plan → code → review → iterate。

事实基线（当前仓库）
- 技术栈：Java 21，Spring Boot 3.5.6，Spring AI 1.0.3（spring-ai-bom），依赖使用 spring-ai-starter-model-openai（仅新坐标）。
- 架构：无状态服务端；客户端在 ToolRequest.contextHistory 携带上下文；无 ConversationContext。
- 已有代码：SimpleCoderApplication；Model（ContextEntry/ToolRequest/ToolResponse）与对应单测；application.yml。
- 待实现：PathValidator；Tools（read/list/search/replace）；AgentService；Controller；最小 UI。

硬性约束
- 单轮交互：每次请求至多一个 Tool；无并行/无多步规划/无会话记忆。
- 安全：文件操作仅限仓库根；ReplaceTool 必须唯一匹配 old_string→new_string。
- 依赖：不得臆测依赖/框架，先查 pom.xml 再使用。

配置约定
- simple-coder.*：repo-root、max-file-lines、max-search-results。
- Spring AI：spring.ai.openai.chat.options.model；api-key 通过环境变量 OPENAI_API_KEY。
- 模型名请使用当前可用型号（gpt-3.5-turbo 可能不可用）。

信息来源优先级（不得臆测）
1) docs/（sources、syntheses、references）
2) DeepWiki（实际阅读原文）
3) 仍不明确时向用户询问，并在 docs/ 补充引用锚点

工作准则
- 只指出“实质问题”，避免吹毛求疵；严格遵循项目既有约定与风格。
- 提出变更前先读相关代码、测试与配置；保持最小可行与可验证（tests/linter）。

开发命令
- 构建/测试：mvn clean compile；mvn test
- 运行应用：mvn spring-boot:run（需 OPENAI_API_KEY）
- 打包：mvn clean package
