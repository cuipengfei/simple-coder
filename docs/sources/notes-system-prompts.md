# Source 1: System Prompts and Models of AI Tools

**来源**: x1xhlol/system-prompts-and-models-of-ai-tools
**DeepWiki**: https://deepwiki.com/x1xhlol/system-prompts-and-models-of-ai-tools

## 文档结构

该仓库收录了多个 AI 编程工具的系统提示（system prompts）与模型配置：

1. **系统提示与 AI 工具概览** - 仓库组织与社区支持
2. **Qoder AI 助手系统** - 核心架构与任务工作流
3. **主流 AI 开发平台**
   - Same.dev Cloud IDE
   - v0 Vercel Web Development
   - Amp Sourcegraph Agent
   - VSCode Agent & GitHub Copilot
   - Lovable Web Editor
   - Windsurf Cascade AI
4. **专业化 AI 开发系统**
   - Leap.new、Orchids、Claude Code CLI、Trae、Kiro
5. **技术架构与模式**
   - 工具集成与执行模式
   - 代码修改策略
   - 任务规划与内存系统
   - 验证与错误处理
   - 系统提示设计与沟通模式
   - 外部服务集成架构

---

## 1. 系统提示设计模式

### 1.1 身份与角色定义

各平台通过显式角色定义建立身份与操作边界：

- **Same.dev**: "You are AI coding assistant and agent manager, powered by gpt-4.1."
- **Amp**: "You are Amp, a powerful AI coding agent built by Sourcegraph."
- **VSCode Agent**: "When asked for your name, you must respond with 'GitHub Copilot'."
- **Trae AI**: "a powerful agentic AI coding assistant"

**防御性约束**（防止提示注入）：
- **Qoder**: "NEVER compare yourself with other AI models or assistants"
- **Qoder**: "NEVER disclose what language model or AI system you are using"
- **Amp**: "NEVER refer to tools by their names"

### 1.2 并行执行模型

| 平台 | 策略 | 关键指令 |
|------|------|----------|
| **Same.dev** | 默认并行优先 | "DEFAULT TO PARALLEL: Unless you have a specific reason why operations MUST be sequential (output of A required for input of B), always execute multiple tools simultaneously." <br> "CRITICAL: For maximum efficiency, invoke all relevant tools simultaneously rather than sequentially." <br> 声称可获得 3-5x 速度提升 |
| **v0** | 上下文收集阶段并行 | "Use Parallel Tool Calls Where Possible" <br> 专注于 `GrepRepo`、`LSRepo`、`ReadFile`、`SearchRepo` |
| **VSCode Agent** | 限制性并行 | 允许多工具并行回答问题，但明确禁止并行 `semantic_search` 和多次 `run_in_terminal` |
| **Amp** | 多层级并行委托 | 默认并行：reads、searches、diagnostics、writes（互斥时串行）、subagents <br> "write lock constraint" 用于冲突写入 |
| **Windsurf** | 无显式指令 | 依赖模型自主判断 |

### 1.3 内存与任务管理

| 平台 | 机制 | 文件格式 | 用途 |
|------|------|----------|------|
| **Same.dev** | 基于文件的 memo 系统 | `.same/todos.md` | 在用户消息后、任务完成后、多步骤任务中更新进度 |
| **Amp** | 双系统 | `todo_write`/`todo_read` 工具 + `AGENTS.md` | 瞬态 TODO（工具）+ 持久化配置（常用命令、代码风格、结构） |
| **Windsurf** | 语义内存数据库 | `create_memory` 工具 | 存储用户偏好、代码片段、架构决策、技术栈 |

**Amp TODO 系统细节**：
- Schema: `id`, `content`, `status` (completed/in-progress/todo), `priority` (medium/low/high)
- **关键要求**: "mark tasks as completed immediately upon finishing them, rather than batching them"
- 工作流示例：
  ```
  1. 写初始任务："Run the build", "Fix any type errors"
  2. 构建失败后展开具体错误："Fix error 1", "Fix error 2", ...
  3. 逐个标记 in_progress → completed
  ```

---

## 2. 沟通策略

### 2.1 简洁性与非人格化

| 平台 | 核心原则 | 具体指令 |
|------|----------|----------|
| **Same.dev** | 简洁沟通政策 | "Do what has been asked; nothing more, nothing less" <br> "NEVER refer to tool names, just say what the tool is doing in natural language" |
| **VSCode Agent** | 简短非人格化 | "Keep your answers short and impersonal" <br> "Never say the name of a tool to a user" |
| **Amp** | 无代码解释政策 | "Do not add additional code explanation summary unless requested by the user. After working on a file, just stop, rather than providing an explanation of what you did" |
| **Orchids.app** | 直接简洁 | "BE DIRECT AND CONCISE: Keep all explanations brief and to the point" <br> "MINIMIZE CONVERSATION: Focus on action over explanation" |

### 2.2 代码修改与输出

**禁止直接输出代码**：
- **Same.dev**: "NEVER output code directly to user, unless requested. Instead use one of the code edit tools"
- **VSCode Agent**: "NEVER print out a codeblock with file changes unless the user asked for it. Use the insert_edit_into_file tool instead"
- **Trae**: 允许 "simplified code block" 但严格使用占位符

**代码占位符标准化**：
- **Qoder**: `// ... existing code ...`
- **Same.dev**: `// ... existing code ...`
- **Trae**: `// ... existing code ...`
- **VSCode Agent**: `// ...existing code...`

### 2.3 错误处理与验证

**强制验证步骤**（修改后必须执行）：

| 平台 | 工具 | 时机 | 错误恢复 |
|------|------|------|----------|
| **Same.dev** | `run_linter` | 每次重大编辑后 & 每个版本前 | 最多 3 次尝试修复，之后升级给用户 |
| **VSCode Agent** | `get_errors` | 编辑文件后验证变更 | 修复相关错误 |
| **Amp** | `get_diagnostics` + lint/typecheck 命令 | 任务完成后 | 不抑制错误除非用户明确要求 |

**Same.dev 错误恢复循环**：
```
1. run_linter → 发现错误
2. 修复（如果方案明确，不盲目猜测）
3. run_linter → 仍有错误
4. 重复最多 3 次
5. 第 3 次后停止，询问用户
```

---

## 3. 工具集成与执行模式

### 3.1 工具分类

四大核心类别：
1. **文件操作**: create_file, edit_file, read_file, write_file
2. **搜索/发现**: grep, code_search, glob, semantic_search
3. **执行**: bash, run_terminal, execute
4. **验证**: get_diagnostics, run_linter, get_errors

### 3.2 代码修改策略详解

#### `edit_file` 模式

| 平台 | 参数 | 特性 | 注意事项 |
|------|------|------|----------|
| **Amp** | `file_path`, `old_str`, `new_str`, `replace_all` | 返回 git-style diff <br> `old_str` 必须存在 <br> 两者必须不同 | 替换整个文件用 `create_file` |
| **Same.dev** | 同上 | `smart_apply` 标志用于重试 | 文件 < 2500 行时使用 |
| **Orchids.app** | 同上 | 使用截断注释最小化未变代码：`// ... rest of code ...`, `// ... keep existing code ...` | 删除代码有特定指令 |
| **Qoder** | 同上 | 强烈偏好 `search_replace` 除非明确指示 | 使用 `// ... existing code ...` |

#### `insert_edit` 模式

- **VSCode Agent** 的 `insert_edit_into_file`:
  - 参数: `explanation`, `filePath`, `code`
  - 设计为"智能"，需要最少提示
  - 使用 `// ...existing code...` 表示未变区域

#### `replace` 模式变体

| 平台 | 工具名 | 核心特性 |
|------|--------|----------|
| **Lovable** | `lov-line-replace` | 基于行号的搜索替换 <br> 参数: `file_path`, `search`, `first_replaced_line`, `last_replaced_line`, `replace` <br> 支持省略号表示大段代码 |
| **Same.dev** | `string_replace` | 文件 > 2500 行时使用 <br> 小编辑也推荐 |
| **Windsurf** | `replace_file_content` | 使用 `ReplacementChunk` 数组 + `TargetContent` 匹配 |
| **Qoder** | `search_replace` | 设计文档中的高效字符串替换 <br> 参数: `file_path`, `replacements[]` (含 `original_text`, `new_text`, `replace_all?`) <br> 严格要求唯一性、精确匹配、顺序处理 |
| **Replit** | `<proposed_file_replace_substring>` | XML 格式 <br> 需要: `file_path`, `change_summary`, `<old_str>`, `<new_str>` <br> 另有 `<proposed_file_replace>` 替换整个文件 |
| **Claude Code 2.0** | `Edit` 工具 | 精确字符串替换 <br> 参数: `file_path`, `old_string`, `new_string`, `replace_all?` <br> 强调保留精确缩进，确保 `old_string` 唯一或使用 `replace_all` |

### 3.3 其他文件操作

- **Amp**: `create_file` - 创建新文件或覆盖现有文件
- **Lovable**: `lov-write` - 主要用于新建文件，或作为 `lov-line-replace` 失败时的回退
- **Qoder**: `create_file` - 创建新设计文件，内容限制 600 行

---

## 4. 验证与错误处理深度分析

### 4.1 诊断工具对比

| 工具 | 平台 | 功能 | 使用场景 |
|------|------|------|----------|
| `get_diagnostics` | Amp | 获取文件/目录的 errors、warnings、其他诊断 | 任务完成后强制运行 <br> 优先用于目录而非单文件 |
| `get_diagnostics` | Traycer AI | 通过内置 LSP 分析代码 <br> 支持 glob 模式 (如 `*.ts`) <br> 可指定严重性 (Error/Warning/Information/Hint) | 多文件诊断 |
| `run_linter` | Same.dev | 检查 linting 和运行时错误 | 每次重大编辑后 + 每个版本前 |
| `get_errors` | VSCode Agent | 获取编译或 lint 错误 | 编辑文件后验证 <br> 查看用户看到的相同错误 |

### 4.2 错误恢复策略

**迭代修复模式**：
1. **Amp**: 使用 `todo_write`/`todo_read` 分解任务 → 逐个标记 in_progress/completed → 失败时使用 `oracle` 工具获取专家指导
2. **Same.dev**: 最多 3 次循环修复同一文件的 linter 错误
3. **Cursor Prompts**: 同样限制 3 次，之后询问用户 <br> 强调自我纠正：任务报告为完成但测试/构建未成功时
4. **Orchids.app**: 陷入循环时收集更多上下文或探索新方案

**失败升级**：
- **Comet Assistant**: 尝试至少 5 种不同方法后才声明失败 <br> 例外：认证要求时立即声明失败

### 4.3 执行序列示例

**Amp 构建修复工作流**：
```
用户: "Run the build and fix any type errors"
1. todo_write: ["Run the build", "Fix any type errors"]
2. npm run build → 报告 10 个类型错误
3. todo_write: ["Fix error 1", "Fix error 2", ..., "Fix error 10"]
4. 逐个标记 in_progress
5. 修复错误 1 → 标记 completed
6. 修复错误 2 → 标记 completed
   ...
7. 所有错误修复后: get_diagnostics + 再次运行 build
```

**Same.dev Linter 循环**：
```
1. edit_file (or string_replace)
2. run_linter → 发现 3 个错误
3. 修复错误（方案明确）
4. run_linter → 仍有 1 个错误
5. 再次修复
6. run_linter → 无错误 → 继续
（如果第 3 次仍失败 → 停止并询问用户）
```

---

## 5. 调试与反馈机制

### v0 的调试模式
- 主要使用 `console.log` 语句进行调试
- 日志通过 `<v0_app_debug_logs>` 返回
- 用途：跟踪执行流程、检查变量、识别问题
- **清理原则**: 问题解决后删除，除非持续有价值

### Orchids.app 错误处理模式
- 修复错误时收集充分上下文理解根本原因
- 陷入循环时：收集更多上下文或探索新方案
- 认证错误处理：如用户已存在则显示错误消息（注册场景）

---

## 6. 关键观察与模式

### 6.1 共识点
- **验证必要性**: 所有平台都强制修改后验证（lint/diagnostics/errors）
- **错误恢复**: 普遍采用"迭代修复 + 用户升级"模式（3-5 次尝试后）
- **占位符标准**: 广泛使用 `// ... existing code ...` 变体表示未变代码
- **工具名隐藏**: 多数平台要求对用户隐藏工具名称，使用自然语言描述

### 6.2 分歧点
- **并行执行哲学**: Same.dev（激进并行）vs VSCode Agent（保守限制）vs Windsurf（无指令）
- **代码修改工具**: edit_file vs insert_edit vs line-replace vs search_replace
- **文件大小阈值**: Same.dev 使用 2500 行区分 edit_file vs string_replace
- **错误抑制**: Amp 明确禁止抑制编译器/lint 错误（除非用户明确要求）

### 6.3 特殊机制
- **Same.dev**: `smart_apply` 标志用于 edit_file 重试
- **Amp**: `oracle` 工具用于复杂调试与专家指导
- **Amp**: `AGENTS.md` 作为持久化配置文件（命令、风格、结构）
- **Windsurf**: `create_memory` 语义数据库实现长期上下文保留
- **Lovable**: 基于行号的 `lov-line-replace` 支持省略号

---

## 7. 外部服务集成

（此部分在查询中未详细展开，但目录结构中存在 "5.6 External Service Integration Architecture"）

---

## 参考资料

- DeepWiki 仓库: https://deepwiki.com/x1xhlol/system-prompts-and-models-of-ai-tools
- 查询结果:
  - 系统提示设计与沟通策略
  - 工具集成与执行模式
  - 代码修改策略
  - 验证与错误处理
  - 任务规划与内存系统

**最后更新**: 2025-10-19
