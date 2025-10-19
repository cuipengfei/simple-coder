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

## 项目状态

- **当前**: Docs-only（无代码）
- **待实现**: 参考 `docs/IMPLEMENTATION.md` 和 `docs/PRD.md`

## 核心架构（参考 docs/IMPLEMENTATION.md）

```
Controller (REST: /api/agent)
    ↓
AgentService (单轮逻辑 + Spring AI ChatClient)
    ↓
Tools (PathValidator + read/list/search/replace)
    ↓
ConversationContext (Session-scoped bean)
```

**关键约束**:
- **硬性单工具**: 每次请求仅执行一个 Tool；多步意图 → 返回提示
- **会话期上下文**: Session-scoped `ConversationContext`（无 Redis/DB）
- **路径安全**: `PathValidator` 限制仓库根内操作
- **唯一匹配**: `ReplaceTool` 强制 old_string 唯一

## 开发工作流

1. **构建命令**（待实现后提取）: `mvn clean install`, `mvn spring-boot:run`, `mvn test`
2. **脚手架**: Phase 1-2（参考 IMPLEMENTATION.md）
3. **工具实现**: Phase 3（PathValidator → ReadFile → ListDir → Search → Replace）
4. **Agent 逻辑**: Phase 4（AgentService 单轮流程）
5. **测试**: Phase 6（单元 + 集成 + 手动）

## 实践准则

- **闭环优先**: 能运行 > 优雅；参考 docs/syntheses/architecture-patterns.md § 8.3（快速原型场景）
- **依赖最小化**: 仅用已声明依赖；新增写入配置
- **可追溯**: 引用外部资料需在 docs/ 补充锚点（参考 docs/references/citations.md 格式）
- **避免臆测**: 未在 docs/ 找到 → DeepWiki → 用户询问
