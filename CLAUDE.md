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

- **已完成**: Phase 1 (项目脚手架) + Phase 2 (核心 Models - 无状态设计)
- **当前阶段**: 准备 Phase 3 (Tools 实现)
- **架构现状**:
  - ✅ Spring Boot 3.5.6 + Spring AI 1.0.3 集成
  - ✅ ToolRequest/ToolResponse/ContextEntry 模型（无状态架构）
  - ✅ 客户端维护上下文，服务端无会话存储
  - ⏳ Tool 接口和实现（待实现）
  - ⏳ AgentService 和 Controller（待实现）

## 核心架构（参考 docs/IMPLEMENTATION.md）

### 三层架构（无状态设计）
```
Controller (REST: /api/agent)
    ↓
AgentService (单轮逻辑 + Spring AI ChatClient)
    ↓
Tools (PathValidator + read/list/search/replace)
```

**注意**: 服务端无状态，客户端在每次请求中携带完整会话历史

### 已实现的核心组件

#### 1. Model 层 (com.simplecoder.model)
- **ToolRequest**: 用户请求封装（无状态设计）
  - `prompt`: 用户自然语言输入
  - `toolType`: 工具选择（"auto" 由 LLM 选择，或指定 "read"/"list"/"search"/"replace"）
  - `contextHistory`: List<ContextEntry> - 客户端维护的会话历史
  - `validate()`: 验证必填字段
  - `buildContextSummary()`: 格式化上下文供 LLM 使用

- **ToolResponse**: 统一响应格式
  - `success`: 操作成功标志
  - `message`: 人类可读的结果/错误描述
  - `data`: 可选的结构化数据（类型取决于工具）
  - `error`: 可选的错误详情
  - 静态工厂方法：`success()`, `error()`

- **ContextEntry**: 会话历史条目
  - `timestamp`: 时间戳
  - `prompt`: 用户输入
  - `result`: 系统响应
  - `getSummary()`: 格式化摘要（截断长文本）

### 待实现组件（Phase 3-5）

#### 3. Tool 层 (com.simplecoder.tool) - Phase 3
```java
// Tool 接口
public interface Tool {
    String getName();
    ToolResponse execute(ToolRequest request);
}

// 实现顺序（有依赖）
1. PathValidator - 路径安全验证（仓库根限制）
2. ReadFileTool - 读取文件内容（支持行号范围）
3. ListDirTool - 列出目录/glob 模式
4. SearchTool - 正则/包含搜索（返回 file:line:snippet）
5. ReplaceTool - 精确替换（强制唯一匹配）
```

#### 4. Service 层 (com.simplecoder.service) - Phase 4
```java
// AgentService 核心流程（无状态）
public ToolResponse process(ToolRequest request) {
    // 1. 从请求获取客户端维护的上下文 (request.buildContextSummary())
    // 2. Auto 模式 → Spring AI ChatClient 选工具
    // 3. 路由到对应 Tool
    // 4. 执行工具并格式化结果
    // 5. 返回 ToolResponse（客户端负责更新历史）
}
```

#### 5. Controller 层 (com.simplecoder.controller) - Phase 5
- REST API: `POST /api/agent`
- 接收 `ToolRequest`，返回 `ToolResponse`

## 关键设计约束

### 单轮交互模式
- **硬性单工具**: 每次请求仅执行一个 Tool
- 多步意图 → 返回提示，由用户决定下一步
- 无自动规划/并行执行/多轮对话

### 安全限制
- **路径安全**: `PathValidator` 强制所有文件操作限制在仓库根目录内
- **唯一匹配**: `ReplaceTool` 强制 old_string 必须在文件中唯一匹配
- 无网络访问（除了 Spring AI 模型 API）

### 会话管理（无状态设计）
- **客户端维护**: 客户端在 `ToolRequest.contextHistory` 中发送完整会话历史
- **服务端无状态**: 无 session/Redis/DB，每次请求独立处理
- **上下文格式化**: `ToolRequest.buildContextSummary()` 将历史转为 LLM 可用格式
- **客户端职责**: 维护历史、限制大小、更新条目

### Spring AI 集成
- **版本**: Spring AI 1.0.3 (GA)
- **模型**: OpenAI ChatClient
- **工具选择**: Auto 模式下由 LLM 选择合适的 Tool
- **依赖**: 使用 `spring-ai-bom` 管理版本，实际依赖 `spring-ai-starter-model-openai`

## 开发命令

### 构建与测试
```bash
# 清理并编译
mvn clean compile

# 运行所有测试
mvn test

# 运行特定测试类
mvn test -Dtest=ToolRequestTest
mvn test -Dtest="ToolRequestTest,ToolResponseTest"

# 运行应用（需要环境变量 OPENAI_API_KEY）
mvn spring-boot:run

# 打包
mvn clean package
```

### 测试策略（按 Phase）
- **Phase 2**: Model 类测试（ToolRequest, ToolResponse, ContextEntry）
- **Phase 3**: Tool 单元测试（每个 Tool 独立验证）
- **Phase 4**: AgentService 集成测试
- **Phase 6**: 端到端集成测试

### 环境变量
```bash
# Windows
set OPENAI_API_KEY=sk-xxx

# Linux/Mac
export OPENAI_API_KEY=sk-xxx
```

## 实践准则

- **闭环优先**: 能运行 > 优雅；参考 docs/syntheses/architecture-patterns.md § 8.3（快速原型场景）
- **依赖最小化**: 仅用已声明依赖；新增写入配置
- **可追溯**: 引用外部资料需在 docs/ 补充锚点（参考 docs/references/citations.md 格式）
- **避免臆测**: 未在 docs/ 找到 → DeepWiki → 用户询问
