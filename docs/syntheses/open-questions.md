# Open Questions - 未解问题与空白点

本文档记录三个来源中未充分说明的问题、矛盾之处和研究空白。

---

## 1. 架构设计问题

### 1.1 并行执行的实际效果

**问题**:
- Same.dev 声称并行执行可获得 **3-5x 速度提升**，但未提供基准测试数据
- 其他平台（Amp, v0）也支持并行，但未提供性能对比

**空白点**:
- 并行执行在**真实场景**中的性能收益
- 并行 vs 串行的**错误率对比**
- 并行工具调用的**依赖冲突检测**机制细节

**潜在研究方向**:
1. 基准测试：对比并行/串行执行相同任务的时间和成功率
2. 冲突分析：识别哪些工具组合易产生竞态条件
3. 成本效益：并行带来的 API 调用成本增加 vs 时间节省

### 1.2 消息历史的内存管理

**问题**:
- Source 2 和 3 都维护完整对话历史，但未提及**内存限制**或**修剪策略**
- 长对话（如 SWE-Bench 任务）可能产生数千条消息

**空白点**:
- 何时触发消息历史修剪？
- 修剪策略：保留关键消息（系统提示、最近 N 条）vs 汇总（summarization）？
- Source 3 的步数限制（`step_limit`）是否间接控制历史长度？

**矛盾**:
- Source 1 未讨论历史管理（可能依赖平台默认行为）
- Source 3 提到 `step_limit` 和 `cost_limit`，但未说明如何处理历史溢出

---

## 2. 工具系统问题

### 2.1 edit_file 的完整实现

**问题**:
- Source 2 定义了 `EditFileInput` 结构体，但**未提供完整的 Go 实现**
- README 提到"处理文件创建、目录创建、字符串替换与唯一性验证"，但细节缺失

**空白点**:
- 如何处理 `old_str` 不唯一的情况？返回错误还是列出所有匹配？
- 文件不存在时，是否自动创建？如果是，是否创建父目录？
- 唯一性验证的实现：精确匹配还是支持正则？

**需要补充**:
```go
func EditFile(input json.RawMessage) (string, error) {
    var editInput EditFileInput
    // ... unmarshal ...

    // ❓ 文件不存在 → 创建？错误？
    // ❓ old_str 不存在 → 错误？
    // ❓ old_str 多次出现 → 错误？列出匹配？
    // ❓ 返回值：git diff？成功消息？
}
```

### 2.2 工具执行的并发安全

**问题**:
- Source 2 的 `Agent.Run()` 串行执行工具，但如果扩展到并行，如何保证并发安全？

**空白点**:
- 多个工具同时修改同一文件时的**锁机制**
- Source 1 提到 Amp 的 "write lock constraint"，但未详细说明实现
- 读写操作的**隔离级别**：是否允许"脏读"？

**Amp 的 "write lock" 提示**:
> "It defaults to parallel for reads, searches, diagnostics, writes (with a write lock constraint for disjoint writes), and subagents."

**未解答**:
- "disjoint writes" 如何定义？按文件？按代码块？
- 冲突检测是编译时（静态分析）还是运行时？
- 锁粒度：文件级？目录级？代码行级？

### 2.3 code_search 的结果限制逻辑

**问题**:
- Source 2 的 `code_search` 硬编码 `-m 50` 限制为前 50 个匹配
- 如果实际需要更多结果怎么办？

**空白点**:
- 为何选择 50？是否有基准测试支持？
- 是否应该作为参数暴露给 LLM？
- 超过 50 个匹配时，如何通知用户？

**潜在改进**:
```go
type CodeSearchInput struct {
    Pattern       string `json:"pattern"`
    MaxResults    int    `json:"max_results,omitempty"`  // 新增参数
    // ...
}
```

---

## 3. 验证与错误处理

### 3.1 错误修复的收敛性

**问题**:
- Source 1 多数平台限制错误修复为 **3 次循环**，但未说明此数字的依据

**空白点**:
- 3 次是经验值还是实验结果？
- 不同类型错误（语法 vs 逻辑）的修复成功率是否不同？
- 是否存在**自适应策略**：简单错误 1 次，复杂错误 5 次？

**已知数据点**:
- Same.dev: 最多 3 次
- Cursor Prompts: 最多 3 次
- Comet Assistant: 至少 5 次尝试（不同方法）

**需要研究**:
- 错误类型分类与对应的修复策略
- 修复成功率随尝试次数的衰减曲线

### 3.2 验证工具的粒度控制

**问题**:
- Amp 建议对**目录**运行 `get_diagnostics` 而非单文件，但未说明原因

**空白点**:
- 目录级诊断是否包含跨文件依赖检查？
- 单文件诊断可能遗漏哪些问题？
- 如何平衡性能（目录慢）与准确性？

**矛盾**:
- VSCode Agent 的 `get_errors` 针对单个文件
- Traycer AI 的 `get_diagnostics` 支持 glob 模式（`*.ts`）

**理想设计**:
```
get_diagnostics(
    scope: "file" | "directory" | "project",
    include_cross_file_issues: bool = true
)
```

---

## 4. 配置与扩展性

### 4.1 AGENTS.md 的标准化

**问题**:
- Amp 使用 `AGENTS.md` 存储配置，但**格式未标准化**

**空白点**:
- Markdown 的结构约定？YAML frontmatter？纯文本？
- 如何解析和更新（手动 vs 工具辅助）？
- 是否支持版本控制和团队共享？

**示例格式（猜测）**:
```markdown
# AGENTS.md

## Frequently Used Commands
- typecheck: `pnpm run typecheck`
- lint: `pnpm run lint`
- build: `pnpm run build`
- test: `pnpm test`

## Code Style Preferences
- Use TypeScript strict mode
- Prefer functional components (React)
- Max line length: 100

## Codebase Structure
- `/src/components` - React components
- `/src/utils` - Utility functions
- `/tests` - Test files
```

**需要明确**:
- 是否有 schema 验证？
- 如何与 TODO 系统（`todo_write`/`todo_read`）集成？

### 4.2 Source 3 的协议扩展

**问题**:
- 协议（`Agent`, `Model`, `Environment`）定义清晰，但**扩展示例有限**

**空白点**:
- 如何实现**自定义 Agent**？继承 `DefaultAgent` 还是从头实现？
- 如何实现**自定义 Model**？除了 LiteLLM 和 Anthropic，如何接入 OpenAI/Azure？
- 如何实现**自定义 Environment**？如 Kubernetes Pod、AWS Lambda？

**需要文档**:
1. Agent 扩展指南：重写哪些方法，保持哪些不变
2. Model 扩展指南：处理流式输出、多模态输入
3. Environment 扩展指南：网络隔离、资源限制、安全沙箱

---

## 5. 交互模式问题

### 5.1 Whitelist Actions 的实现

**问题**:
- Source 3 提到 `whitelist_actions`（正则模式）用于在 confirm 模式下跳过确认，但未提供示例

**空白点**:
- 正则语法：Python `re` 模块的完整语法？
- 匹配对象：命令字符串？命令类型？
- 如何避免安全风险（如 `rm -rf /`）？

**期望示例**:
```yaml
interactive:
  whitelist_actions:
    - "^ls "          # 允许 ls 命令无需确认
    - "^cat "         # 允许 cat 命令
    - "^git status"   # 允许 git status
```

**安全考虑**:
- 是否需要黑名单（禁止的命令）？
- 如何防止正则注入攻击？

### 5.2 TextualAgent 的性能开销

**问题**:
- Source 3 的 `TextualAgent` 使用多线程（后台 Agent + 主线程 UI），但未提及性能开销

**空白点**:
- UI 渲染对 Agent 执行速度的影响？
- 是否有性能对比（InteractiveAgent vs TextualAgent）？
- 大规模输出（如 1000 行日志）时的 TUI 性能？

**需要基准测试**:
- 相同任务在 DefaultAgent, InteractiveAgent, TextualAgent 下的执行时间
- UI 更新频率对性能的影响

---

## 6. SWE-Bench 集成问题

### 6.1 并行处理的容错

**问题**:
- Source 3 使用 `ThreadPoolExecutor` 并行处理实例，但未详细说明**错误隔离**

**空白点**:
- 一个实例失败是否影响其他实例？
- 如何处理部分成功、部分失败的情况？
- 是否支持断点续传（部分实例已完成）？

**已知**:
- `--redo-existing` 标志跳过已有实例
- 增量保存 `preds.json` 和 trajectory 文件

**未知**:
- 失败实例的重试策略？
- 如何记录和报告失败原因？

### 6.2 Docker 环境的资源限制

**问题**:
- Source 3 的 `DockerEnvironment` 未提及**资源限制**（CPU, 内存, 磁盘）

**空白点**:
- 是否使用 Docker 的 `--cpus`, `--memory` 标志？
- SWE-Bench 任务可能消耗大量资源，如何防止 OOM？
- 容器之间是否共享资源（如网络、缓存）？

**建议增强**:
```python
DockerEnvironmentConfig(
    image="sweagent/swe-bench:latest",
    cpu_limit=2.0,        # 新增
    memory_limit="4g",    # 新增
    disk_limit="10g",     # 新增
    network_mode="none",  # 新增（隔离网络）
)
```

---

## 7. 提示工程问题

### 7.1 系统提示的版本管理

**问题**:
- Source 1 收录了多个平台的系统提示，但未提及**版本控制**

**空白点**:
- 提示更新频率？
- 如何回滚到旧版本？
- 如何测试新提示的效果？

**最佳实践（猜测）**:
- 使用 Git 管理提示文件
- 语义化版本号（如 `v1.2.3`）
- A/B 测试框架评估新提示

### 7.2 提示与代码的耦合

**问题**:
- Source 1 的提示词中硬编码了工具名称和参数，如何与 Source 2 的代码定义同步？

**矛盾**:
- 提示词中描述的工具签名 vs 代码中的实际签名
- 如果代码修改了参数，提示词是否需要手动同步？

**理想方案**:
- 从代码自动生成提示词片段
- 类似 OpenAPI spec 的工具定义与提示词绑定

---

## 8. 未涵盖的主题

### 8.1 安全与沙箱

**完全缺失**:
- 恶意代码检测
- 沙箱逃逸防护
- 用户数据隐私

**需要补充**:
- Source 3 的容器环境是否足够安全？
- 如何防止 Agent 执行 `rm -rf /` 或网络攻击？

### 8.2 成本控制

**仅 Source 3 提及**:
- `cost_limit` 参数，但未说明成本计算方式

**空白点**:
- 成本如何计算？Token 数？API 调用次数？
- 不同模型的成本差异？
- 如何优化成本（如缓存、批处理）？

### 8.3 多模态支持

**完全缺失**:
- 图像输入（如截图、设计稿）
- 图表生成
- 视频处理

**潜在扩展**:
- Source 3 的 `Model` 协议是否支持多模态？
- 如何处理非文本输出（如 Matplotlib 图表）？

---

## 9. 研究优先级

### 高优先级（影响核心功能）

1. **edit_file 完整实现**（Source 2）
2. **并行执行的冲突检测**（Source 1）
3. **消息历史的修剪策略**（Source 2, 3）
4. **错误修复的收敛性研究**（Source 1）

### 中优先级（改进用户体验）

5. **AGENTS.md 格式标准化**（Source 1）
6. **协议扩展文档**（Source 3）
7. **whitelist_actions 示例**（Source 3）
8. **Docker 资源限制**（Source 3）

### 低优先级（高级特性）

9. **多模态支持**
10. **成本优化策略**
11. **安全沙箱增强**

---

## 10. 如何填补空白

### 10.1 实验方法

1. **基准测试**：对比不同策略（并行 vs 串行, 3 次 vs 5 次修复）
2. **代码审查**：分析 Source 2 的完整仓库（本文档基于片段）
3. **社区调研**：查找平台的公开讨论、issues、changelogs

### 10.2 文档改进

1. **补充缺失实现**：为 Source 2 的 edit_file 提供参考实现
2. **配置示例**：为 AGENTS.md, whitelist_actions 提供模板
3. **最佳实践指南**：总结错误修复、并行执行、资源限制的经验规则

### 10.3 工具开发

1. **提示词生成器**：从代码自动生成系统提示
2. **冲突检测器**：静态分析工具识别并行工具的潜在冲突
3. **成本计算器**：预测任务的 API 成本

---

## 参考资料

- [Source 1: System Prompts](../sources/notes-system-prompts.md)
- [Source 2: Coding Agent](../sources/notes-coding-agent.md)
- [Source 3: Mini SWE Agent](../sources/notes-mini-swe-agent.md)
- [Architecture Patterns](./architecture-patterns.md)
- [Code Modification Strategies](./code-modification.md)

**最后更新**: 2025-10-19
