# GEMINI.md

角色与分工
- Gemini：规划者/评审者（Planner/Reviewer）。
- Claude Code：编码者（Coder）。

目标与范围（V0）
- Java 21 + Spring Boot + Spring AI，最小可用 Simple Coder Agent。
- 工具：read_file、list_dir/glob、search（regex/contains）、replace（exact old_string→new_string，唯一匹配）。
- Agent：单轮输入 → 至多一次工具调用 → 返回文本；不做并行/规划/记忆。
- 安全：仅读写仓库根内；不暴露密钥；网络仅用于模型 API。

信息来源顺序（禁止臆测）
- 优先：docs/（notes、syntheses、references）。
- 其次：DeepWiki 原文（实际阅读，不凭标题推断）：
  1) https://deepwiki.com/x1xhlol/system-prompts-and-models-of-ai-tools?ref=ghuntley.com
  2) https://deepwiki.com/ghuntley/how-to-build-a-coding-agent
  3) https://deepwiki.com/SWE-agent/mini-swe-agent
- 仍不明确：询问用户，并在 docs/ 添加引用锚点。

仓库现状
- Docs-only；关键文件：CLAUDE.md、README.md、docs/（含 sources 与 syntheses/architecture-patterns.md）。
- docs/README.md 中部分链接页面尚未创建（如 tool-systems.md、code-modification.md 等）。

Gemini 工作准则
- 只做规划与评审；不提出编码或 git 操作建议。
- 输出简洁，中文为主，英文技术术语保留原文；结论需可追溯至 docs/ 或 DeepWiki。

里程碑（Docs）
- M2：补齐缺失的 syntheses 页面。
- M3：补齐 references（glossary、citations）并与导航一致。

代码加入后（占位）
- 从 pom.xml 或 build.gradle 提取真实 build/test/format 命令写入文档；缺失则标注 TODO。