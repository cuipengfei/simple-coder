# Code Modification Strategies - 代码修改策略对比

本文档详细对比不同系统的代码修改工具、模式和最佳实践。

---

## 1. 修改工具全景

### 1.1 工具矩阵

| 工具类型 | Source 1 平台示例 | Source 2 | Source 3 |
|----------|-------------------|----------|----------|
| **精确字符串替换** | edit_file (Amp, Same.dev, Orchids, Qoder, Claude Code) | edit_file (待实现) | 无（仅 bash） |
| **插入编辑** | insert_edit_into_file (VSCode Agent) | - | - |
| **行号替换** | lov-line-replace (Lovable) | - | - |
| **搜索替换** | search_replace (Qoder), string_replace (Same.dev) | - | - |
| **内容块替换** | replace_file_content (Windsurf) | - | - |
| **XML 格式替换** | proposed_file_replace_substring (Replit) | - | - |
| **文件创建/覆盖** | create_file (Amp, Qoder), lov-write (Lovable) | - | - |

---

## 2. edit_file 模式详解

### 2.1 核心参数

**通用模式**:
```
edit_file(
    file_path: string,
    old_str: string,
    new_str: string,
    replace_all: bool = false  // 可选
)
```

### 2.2 平台实现对比

#### Amp

**特性**:
- 返回 git-style diff
- `old_str` 必须存在于文件中
- `old_str` 和 `new_str` 必须不同
- `replace_all` 参数替换所有匹配

**限制**:
- 替换整个文件内容时，优先使用 `create_file`

**错误处理**:
- `old_str` 不存在 → 错误
- `old_str == new_str` → 错误

#### Same.dev

**使用场景**:
- 文件 < 2500 行时使用

**特殊标志**:
- `smart_apply`: 允许编辑重试

**替代方案**:
- 文件 > 2500 行 → 使用 `string_replace`
- 小编辑也推荐 `string_replace`

#### Orchids.app

**占位符约定**:
```typescript
// 修改前
function example() {
    // ... keep existing code ...
    const result = computeSomething();
    // ... rest of code ...
    return result;
}

// 修改后
function example() {
    // ... keep existing code ...
    const result = computeOptimized();  // 仅修改此行
    // ... rest of code ...
    return result;
}
```

**截断注释**:
- `// ... keep existing code ...`
- `// ... rest of code ...`

**删除代码**:
- 提供特定指令用于删除代码段

#### Qoder

**偏好**:
- 强烈偏好 `search_replace` 而非 `edit_file`（除非明确指示）

**占位符**:
- `// ... existing code ...`

**使用场景**:
- 用于提议对现有文件的编辑

#### Claude Code 2.0

**强调**:
- **保留精确缩进**：从 `Read` 工具输出复制 `old_string` 时，保留行号前缀后的确切缩进
- **唯一性要求**：确保 `old_string` 在文件中唯一，或使用 `replace_all`

**最佳实践**:
```python
# Read 工具输出
    1→def example():
    2→    result = old_value  # 注意：2 个空格缩进
    3→    return result

# old_string（正确）- 保留确切缩进
old_string = "    result = old_value"  # 4 个空格（行号后缩进）

# old_string（错误）- 缩进不匹配
old_string = "  result = old_value"  # 2 个空格
```

### 2.3 Source 2: edit_file 设计

**输入结构**:
```go
type EditFileInput struct {
    Path   string `json:"path"`
    OldStr string `json:"old_str"`
    NewStr string `json:"new_str"`
}
```

**功能描述**（来自 README）:
- 处理文件创建
- 处理目录创建
- 字符串替换，带唯一性验证

**注意**: 完整 Go 实现未在代码片段中提供

---

## 3. insert_edit 模式

### 3.1 VSCode Agent 实现

**工具名**: `insert_edit_into_file`

**参数**:
```
{
    explanation: string,  // 编辑说明
    filePath: string,
    code: string
}
```

**设计哲学**:
- "智能"工具，需要**最少提示**
- 自动推断插入位置

**占位符约定**:
```javascript
// ...existing code...
新代码在此插入
// ...existing code...
```

**示例**:
```javascript
// 原文件
function calculate() {
    const a = 1;
    const b = 2;
    return a + b;
}

// insert_edit_into_file 的 code 参数
function calculate() {
    const a = 1;
    // ...existing code...
    console.log('Debugging:', a, b);  // 插入此行
    // ...existing code...
}
```

---

## 4. replace 模式变体

### 4.1 行号替换（Lovable）

**工具名**: `lov-line-replace`

**参数**:
```
{
    file_path: string,
    search: string,
    first_replaced_line: number,
    last_replaced_line: number,
    replace: string
}
```

**特性**:
- **显式行号**：精确指定替换范围
- **省略号支持**：大段未变代码使用 `...`

**示例**:
```python
# 原文件（行号）
1: def process_data(data):
2:     validate(data)
3:     transform(data)
4:     save(data)
5:     return data

# lov-line-replace 调用
{
    "search": "validate(data)\n    transform(data)",
    "first_replaced_line": 2,
    "last_replaced_line": 3,
    "replace": "# Data processing steps\n    validate_and_transform(data)"
}

# 结果
1: def process_data(data):
2:     # Data processing steps
3:     validate_and_transform(data)
4:     save(data)
5:     return data
```

### 4.2 字符串替换（Same.dev）

**工具名**: `string_replace`

**使用场景**:
- 文件 > 2500 行
- 小编辑（即使文件较小）

**参数**: 同 `edit_file`

### 4.3 内容块替换（Windsurf）

**工具名**: `replace_file_content`

**参数**:
```
{
    file_path: string,
    replacements: ReplacementChunk[],
}

ReplacementChunk {
    target_content: string,   // 要替换的目标内容
    new_content: string       // 新内容
}
```

**特性**:
- 支持多个替换块
- `TargetContent` 精确匹配

### 4.4 搜索替换（Qoder）

**工具名**: `search_replace`

**用途**: 设计文档中的高效字符串替换

**参数**:
```
{
    file_path: string,
    replacements: [
        {
            original_text: string,
            new_text: string,
            replace_all: bool = false  // 可选
        },
        ...
    ]
}
```

**严格要求**:
1. **唯一性**: `original_text` 必须在文件中唯一（除非 `replace_all: true`）
2. **精确匹配**: 必须完全匹配，包括空格和换行
3. **顺序处理**: 替换按数组顺序应用

**示例**:
```json
{
  "file_path": "design.md",
  "replacements": [
    {
      "original_text": "## Old Section Title",
      "new_text": "## New Section Title"
    },
    {
      "original_text": "- Feature A\n- Feature B",
      "new_text": "- Feature A (enhanced)\n- Feature B\n- Feature C (new)",
      "replace_all": false
    }
  ]
}
```

### 4.5 XML 格式替换（Replit）

**工具名**: `<proposed_file_replace_substring>`

**XML 结构**:
```xml
<proposed_file_replace_substring>
    <file_path>path/to/file.js</file_path>
    <change_summary>Brief description of change</change_summary>
    <old_str>code to replace</old_str>
    <new_str>new code</new_str>
</proposed_file_replace_substring>
```

**另有**: `<proposed_file_replace>` 用于替换整个文件内容

---

## 5. 文件创建/覆盖

### 5.1 create_file（Amp）

**用途**:
- 创建新文件
- 覆盖现有文件

**参数**:
```
{
    file_path: string,
    content: string
}
```

**使用场景**:
- 替换整个文件内容（优于 `edit_file`）

### 5.2 create_file（Qoder）

**用途**: 创建新设计文件

**限制**: 内容最多 600 行

### 5.3 lov-write（Lovable）

**用途**:
- 主要用于创建新文件
- 作为 `lov-line-replace` 失败时的回退

**占位符鼓励**:
```javascript
function example() {
    // ... keep existing code
    newCode();
    // ... keep existing code
}
```

---

## 6. 占位符约定对比

### 6.1 占位符变体

| 平台 | 占位符格式 | 用途 |
|------|------------|------|
| **Qoder** | `// ... existing code ...` | 表示未变代码 |
| **Same.dev** | `// ... existing code ...` | 同上 |
| **Trae** | `// ... existing code ...` | 同上 |
| **VSCode Agent** | `// ...existing code...` | 同上（无空格） |
| **Orchids.app** | `// ... keep existing code ...` <br> `// ... rest of code ...` | 明确保留/剩余代码 |
| **Lovable** | `// ... keep existing code` | 简化版本 |

### 6.2 最佳实践

**何时使用占位符**:
- 编辑大文件时，仅显示修改附近的代码
- 减少 token 消耗
- 提高可读性

**何时避免占位符**:
- 小文件（< 50 行）
- 修改分散在多处
- 需要完整上下文理解变更

**示例（推荐）**:
```python
# 大文件修改（300 行）
def complex_function():
    # ... existing code ...

    # 仅修改此段
    new_logic = optimized_approach()

    # ... existing code ...
```

**示例（不推荐）**:
```python
# 小文件修改（10 行）
def simple_function():
    # ... existing code ...  # 无意义，不如显示完整
    result = new_value
    # ... existing code ...
```

---

## 7. Source 3: Bash 原生修改

### 7.1 无工具抽象

mini-swe-agent **不提供代码修改工具**，仅通过 bash 命令：

```bash
# 使用 sed 替换
sed -i 's/old_text/new_text/g' file.py

# 使用 cat + heredoc 创建文件
cat > file.py << 'EOF'
def new_function():
    pass
EOF

# 使用 echo 追加
echo "new_line" >> file.py

# 使用 vim/ed 编辑（需交互）
# 不适用于 Agent（需自动化）
```

### 7.2 优势与劣势

**优势**:
- **极简**: 无需定义工具抽象
- **灵活**: 任意 bash 命令可用
- **通用**: 不依赖特定 API

**劣势**:
- **易错**: 字符串转义、正则复杂
- **难验证**: 无类型检查
- **可读性差**: bash 语法对 LLM 和人类都不直观

---

## 8. 修改策略决策树

### 8.1 文件大小驱动

```
文件大小 < 2500 行?
├─ 是 → edit_file
└─ 否 → string_replace 或 line-replace
```

**来源**: Same.dev 明确的阈值

### 8.2 修改范围驱动

```
修改类型?
├─ 单点精确替换 → edit_file / search_replace
├─ 连续行块替换 → lov-line-replace
├─ 多处分散替换 → search_replace (数组)
├─ 整个文件重写 → create_file / proposed_file_replace
└─ 插入新代码 → insert_edit_into_file
```

### 8.3 验证需求驱动

```
是否需要唯一性保证?
├─ 是 → edit_file (默认) / search_replace (replace_all: false)
└─ 否 → edit_file (replace_all: true) / bash sed
```

---

## 9. 错误处理与验证

### 9.1 修改前验证

| 平台 | 验证机制 | 错误处理 |
|------|----------|----------|
| **Amp** | `old_str` 存在性检查 | 不存在 → 错误 |
| **Qoder** | `original_text` 唯一性检查 | 非唯一 → 错误（除非 replace_all） |
| **Claude Code** | 缩进精确匹配 | 不匹配 → 替换失败 |

### 9.2 修改后验证

**强制验证**（所有平台）:
1. **Same.dev**: `run_linter` 每次编辑后
2. **Amp**: `get_diagnostics` 任务完成后
3. **VSCode Agent**: `get_errors` 编辑文件后

**验证循环**:
```
修改代码 → 验证工具 → 发现错误
    ↓                       ↓
 继续下一步 ←──── 修复 ←─────┘
                  ↑
                  └── 最多 3 次，之后升级给用户
```

---

## 10. 关键洞察

### 10.1 工具选择策略

| 场景 | 推荐工具 | 理由 |
|------|----------|------|
| 小文件全局替换 | `edit_file` + `replace_all` | 简单直接 |
| 大文件单点修改 | `string_replace` | 性能优化 |
| 行号已知的修改 | `lov-line-replace` | 精确控制 |
| 多处相关修改 | `search_replace` (数组) | 原子性操作 |
| 整个文件重写 | `create_file` | 避免复杂替换逻辑 |
| 插入新代码 | `insert_edit_into_file` | 智能推断位置 |

### 10.2 占位符使用原则

1. **仅在必要时使用**：小文件（< 50 行）直接显示全部
2. **保持一致性**：项目内统一占位符格式
3. **明确边界**：`... keep existing code ...` 比 `...` 更清晰
4. **避免过度嵌套**：不要在占位符内再使用占位符

### 10.3 验证驱动开发

**黄金规则**:
> 永远不要跳过修改后验证，即使你"确定"代码正确。

**原因**:
- LLM 可能产生语法错误
- 缩进/空格问题人眼难察觉
- 跨文件依赖可能被破坏

**验证顺序**:
1. 语法检查（linter）
2. 类型检查（tsc, mypy）
3. 构建测试（build）
4. 单元测试（test）

---

## 参考资料

- [Source 1: System Prompts - Tool Integration](../sources/notes-system-prompts.md#3-工具集成与执行模式)
- [Source 2: Coding Agent - Edit Tool](../sources/notes-coding-agent.md#321-edit_file)
- [Source 3: Mini SWE Agent - Bash Commands](../sources/notes-mini-swe-agent.md#71-无工具抽象)

**最后更新**: 2025-10-19
