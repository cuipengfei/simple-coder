# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 角色与协作

- **Claude Code**: 编码者（执行、实现、测试）
- **Gemini**: 规划者/评审者（架构设计、方案审查）
- **协作模式**: Gemini 规划 → Claude 编码 → Gemini 评审 → 迭代

## 技术栈（V0）

- Java 21 + Spring Boot + Spring AI
- 构建：Maven（若检测到 Gradle 则用 Gradle）
- 目标：最小可用 Simple Coder Agent（单轮工具调用）

## 文档架构（必读）

**信息来源优先级**（禁止臆测）：
1. **docs/** - 本地文档，已完成：
   - `docs/sources/` - 三个来源的详细笔记（system-prompts、coding-agent、mini-swe-agent）
   - `docs/syntheses/` - 跨来源综合分析（architecture-patterns、code-modification、open-questions）
   - `docs/references/` - 术语表（glossary）与引用（citations）

2. **DeepWiki 原文** - 仅当 docs/ 不明确时查阅（实际阅读，勿凭标题推断）：
   - https://deepwiki.com/x1xhlol/system-prompts-and-models-of-ai-tools
   - https://deepwiki.com/ghuntley/how-to-build-a-coding-agent
   - https://deepwiki.com/SWE-agent/mini-swe-agent

3. **用户询问** - 仍不明确时询问用户，并在 docs/ 补充引用锚点

**关键文档**：
- `docs/README.md` - 导航入口
- `docs/syntheses/architecture-patterns.md` - 三种架构范式对比（提示工程/工具注册/协议抽象）
- `docs/syntheses/code-modification.md` - edit_file 等工具设计模式
- `docs/references/glossary.md` - 术语表（Agent、ToolDefinition、edit_file 等）

## Agent 设计约束（V0）

- **单轮交互**: prompt → (工具调用?) → 文本响应
- **工具集**: read_file、list_dir/glob、search、replace（exact old_string→new_string，强制唯一匹配）
- **无并行/规划/记忆**: 参考 docs/syntheses/architecture-patterns.md § 4（Source 2/3 无并行）
- **安全**: 文件操作限仓库根；无密钥暴露；网络仅模型 API

## 开发工作流

1. **探测构建命令**（从 pom.xml/build.gradle 提取）→ 同步到 README.md
2. **脚手架**: 最小 Spring Boot + Spring AI（1 Controller + 1 Service）
3. **工具实现**: 参考 docs/sources/notes-coding-agent.md § 3.2（ToolDefinition 模式）
4. **Agent 逻辑**: 参考 docs/sources/notes-mini-swe-agent.md § 2（step/run 循环）
5. **验证**: 执行实际命令；测试最小化（JUnit 5，Happy Path）

## 实践准则

- **闭环优先**: 能运行 > 优雅；参考 docs/syntheses/architecture-patterns.md § 8.3（快速原型场景）
- **依赖最小化**: 仅用已声明依赖；新增写入配置
- **可追溯**: 引用外部资料需在 docs/ 补充锚点（参考 docs/references/citations.md 格式）
- **避免臆测**: 未在 docs/ 找到 → DeepWiki → 用户询问
