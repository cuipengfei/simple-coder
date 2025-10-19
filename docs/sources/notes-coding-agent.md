# Source 2: How to Build a Coding Agent

**来源**: ghuntley/how-to-build-a-coding-agent
**DeepWiki**: https://deepwiki.com/ghuntley/how-to-build-a-coding-agent

## 文档结构

该仓库是一个使用 Go 语言构建的渐进式 Coding Agent 教程：

1. **概览** - 项目目标与架构
2. **Agent 系统架构**
   - 核心组件
   - 工具系统
3. **Agent 实现**（渐进演进）
   - Basic Chat Agent
   - File Reading Agent
   - File Listing Agent
   - Bash Tool Agent
   - File Editing Agent
   - Code Search Agent
4. **开发环境**
   - 环境配置
   - 构建系统
   - 依赖管理
5. **使用指南**
   - 示例提示
   - Demo 内容

---

## 1. 核心架构

### 1.1 Agent 结构体

`Agent` 是系统的中心组件，定义在 Go 代码中：

```go
type Agent struct {
    client         *anthropic.Client
    getUserMessage func() (string, bool)
    tools          []ToolDefinition
    verbose        bool
}
```

**字段说明**：
- `client`: 与 Anthropic API 通信的客户端
- `getUserMessage`: 获取用户输入的函数（闭包）
- `tools`: 已注册的工具定义切片
- `verbose`: 详细日志标志

### 1.2 共享事件循环

`Agent.Run()` 方法实现核心事件循环：

```go
func (a *Agent) Run() {
    conversation := []anthropic.MessageParam{}

    for {
        // 1. 获取用户输入
        fmt.Print("\n> ")
        userInput, ok := a.getUserMessage()
        if !ok || userInput == "" {
            continue
        }

        // 2. 添加到对话历史
        conversation = append(conversation,
            anthropic.NewUserMessage(userInput))

        // 3. 调用 Claude API
        message := a.runInference(conversation)

        // 4. 处理响应
        for _, block := range message.Content {
            if block.Type == "tool_use" {
                // 执行工具
            } else {
                // 显示文本
            }
        }

        // 5. 发送工具结果（如有）
        if len(toolResults) > 0 {
            conversation = append(conversation,
                anthropic.NewUserMessage(toolResults...))
        }
    }
}
```

**关键流程**：
1. 打印提示并调用 `getUserMessage()` 获取输入
2. 空输入则跳过，有效输入转换为 `UserMessage` 并加入 `conversation`
3. 调用 `runInference()` 与 Claude API 交互
4. 遍历响应的 `Content` 块
5. 若检测到 `tool_use`，查找并执行对应工具，收集结果
6. 将工具结果作为新的用户消息发回 Claude

### 1.3 API 交互

`runInference()` 方法封装 API 通信：

```go
func (a *Agent) runInference(conversation []anthropic.MessageParam) *anthropic.Message {
    // 1. 转换工具定义为 API 格式
    tools := []anthropic.ToolUnionParam{}
    for _, toolDef := range a.tools {
        tools = append(tools, anthropic.ToolDefinitionParam{
            Name:        toolDef.Name,
            Description: toolDef.Description,
            InputSchema: toolDef.InputSchema,
        })
    }

    // 2. 调用 API
    message, err := a.client.Messages.New(context.Background(),
        anthropic.MessageNewParams{
            Model:      anthropic.ModelClaude3_7SonnetLatest,
            MaxTokens:  anthropic.Int(4096),
            Messages:   conversation,
            Tools:      tools,
        })

    // 3. 返回响应
    return message
}
```

**关键步骤**：
1. 将 `ToolDefinition` 转换为 `anthropic.ToolUnionParam`
2. 调用 `client.Messages.New`，传入对话历史、工具、模型、MaxTokens
3. 返回 `anthropic.Message` 对象

### 1.4 对话历史管理

- `conversation` 是 `[]anthropic.MessageParam` 切片
- 用户消息通过 `anthropic.NewUserMessage()` 添加
- Claude 响应通过 `message.ToParam()` 转换后添加
- 完整对话历史用于后续 API 调用，保持上下文连续性

---

## 2. 工具系统设计

### 2.1 ToolDefinition 结构

```go
type ToolDefinition struct {
    Name        string
    Description string
    InputSchema anthropic.ToolInputSchemaParam
    Function    func(json.RawMessage) (string, error)
}
```

**字段说明**：
- `Name`: 工具唯一标识符
- `Description`: 自然语言描述工具用途
- `InputSchema`: JSON Schema，定义期望的输入参数与类型
- `Function`: 执行工具逻辑的 Go 函数，接受 `json.RawMessage`，返回结果字符串或错误

### 2.2 Schema 生成

使用 `GenerateSchema[T]()` 函数自动生成 Schema：

```go
func GenerateSchema[T any]() anthropic.ToolInputSchemaParam {
    reflector := jsonschema.Reflector{
        AllowAdditionalProperties: false,
        DoNotReference:            true,
    }
    schema := reflector.Reflect(new(T))

    return anthropic.ToolInputSchemaParam{
        Type:       anthropic.F(schema.Type),
        Properties: anthropic.F(schema.Properties),
        Required:   anthropic.F(schema.Required),
    }
}
```

**特性**：
- 使用 `jsonschema.Reflector` 从 Go 结构体生成 Schema
- `AllowAdditionalProperties: false` - 禁止额外属性
- `DoNotReference: true` - 避免引用，生成内联 Schema
- 确保类型安全与输入验证

### 2.3 工具执行流程

在 `Agent.Run()` 的响应处理循环中：

```go
for _, block := range message.Content {
    if block.Type == "tool_use" {
        // 1. 查找工具
        var tool *ToolDefinition
        for _, t := range a.tools {
            if t.Name == block.Name {
                tool = &t
                break
            }
        }

        // 2. 执行工具
        result, err := tool.Function(block.Input)

        // 3. 格式化结果
        toolResults = append(toolResults,
            anthropic.ToolResultBlock{
                ToolUseID: block.ID,
                Content:   result,
                IsError:   err != nil,
            })
    }
}
```

**步骤**：
1. 遍历 `message.Content`，检测 `tool_use` 类型
2. 通过 `Name` 查找匹配的 `ToolDefinition`
3. 调用 `tool.Function` 执行，传入 `block.Input` (JSON)
4. 格式化为 `ToolResultBlock`，包含 `ToolUseID`、结果内容、错误标志
5. 收集所有结果，作为新的用户消息发回 Claude

---

## 3. Agent 渐进演进

### 3.1 演进路径

| Agent 类型 | 文件 | 新增工具 | 功能 |
|------------|------|----------|------|
| Chat Agent | `chat.go` | 无 | 基础对话循环，无工具 |
| File Reading | `read.go` | `read_file` | 读取文件内容 |
| File Listing | `list_files.go` | `list_files` | 目录探索 |
| Bash Command | `bash_tool.go` | `bash` | 执行 shell 命令 |
| File Editing | `edit_tool.go` | `edit_file` | 修改文件（详情未提供） |
| Code Search | `code_search_tool.go` | `code_search` | 使用 ripgrep 搜索代码 |

**共同点**：
- 所有 Agent 共享相同的 `Agent` 结构体和事件循环
- 主要区别在于初始化时注册的 `ToolDefinition` 切片
- 体现渐进增强的设计思路

### 3.2 工具实现详解

#### 3.2.1 `read_file`

**定义**:
```go
var ReadFileDefinition = ToolDefinition{
    Name:        "read_file",
    Description: "Read the contents of a file",
    InputSchema: GenerateSchema[ReadFileInput](),
    Function:    ReadFile,
}

type ReadFileInput struct {
    Path string `json:"path" jsonschema_description:"Relative path to file"`
}
```

**函数实现**:
```go
func ReadFile(input json.RawMessage) (string, error) {
    var readInput ReadFileInput
    if err := json.Unmarshal(input, &readInput); err != nil {
        panic(err)  // 不应发生，Schema 验证应确保
    }

    content, err := os.ReadFile(readInput.Path)
    if err != nil {
        log.Printf("Error reading file: %v", err)
        return "", err
    }

    return string(content), nil
}
```

**错误处理**：
- Unmarshal 失败 → `panic`（假设 Schema 验证已确保输入合法）
- `os.ReadFile` 失败 → 记录日志，返回空字符串和错误

#### 3.2.2 `list_files`

**定义**:
```go
var ListFilesDefinition = ToolDefinition{
    Name:        "list_files",
    Description: "List files in a directory",
    InputSchema: GenerateSchema[ListFilesInput](),
    Function:    ListFiles,
}

type ListFilesInput struct {
    Path string `json:"path" jsonschema_description:"Directory path (defaults to current)"`
}
```

**函数实现**:
```go
func ListFiles(input json.RawMessage) (string, error) {
    var listInput ListFilesInput
    if err := json.Unmarshal(input, &listInput); err != nil {
        panic(err)
    }

    // 使用 find 命令，排除 .devenv 和 .git
    cmd := exec.Command("find", listInput.Path,
        "-not", "-path", "*/.devenv/*",
        "-not", "-path", "*/.git/*")

    output, err := cmd.Output()
    if err != nil {
        log.Printf("Error listing files: %v", err)
        return "", err
    }

    // 分割成字符串切片并 JSON 序列化
    files := strings.Split(strings.TrimSpace(string(output)), "\n")
    jsonOutput, _ := json.Marshal(files)

    return string(jsonOutput), nil
}
```

**特性**：
- 使用 `find` 命令而非 Go 标准库
- 排除 `.devenv` 和 `.git` 目录
- 输出 JSON 格式的文件列表

#### 3.2.3 `bash`

**定义**:
```go
var BashDefinition = ToolDefinition{
    Name:        "bash",
    Description: "Execute a bash command",
    InputSchema: GenerateSchema[BashInput](),
    Function:    Bash,
}

type BashInput struct {
    Command string `json:"command" jsonschema_description:"Shell command to execute"`
}
```

**函数实现**:
```go
func Bash(input json.RawMessage) (string, error) {
    var bashInput BashInput
    if err := json.Unmarshal(input, &bashInput); err != nil {
        return "", err
    }

    cmd := exec.Command("bash", "-c", bashInput.Command)
    output, err := cmd.CombinedOutput()

    if err != nil {
        log.Printf("Bash error: %v", err)
        return fmt.Sprintf("Command failed: %s\nOutput: %s",
            err, string(output)), err
    }

    return string(output), nil
}
```

**错误处理**：
- Unmarshal 失败 → 返回错误（不 panic）
- 命令执行失败 → 记录日志，返回格式化错误消息（包含命令失败原因和输出）

#### 3.2.4 `edit_file`

**定义**（部分）:
```go
type EditFileInput struct {
    Path   string `json:"path"`
    OldStr string `json:"old_str"`
    NewStr string `json:"new_str"`
}
```

**功能描述**（来自 README）：
- 处理文件创建
- 处理目录创建
- 字符串替换，带唯一性验证

**注意**：完整 Go 实现未在提供的代码片段中

#### 3.2.5 `code_search`

**定义**:
```go
var CodeSearchDefinition = ToolDefinition{
    Name:        "code_search",
    Description: "Search for code patterns using ripgrep",
    InputSchema: GenerateSchema[CodeSearchInput](),
    Function:    CodeSearch,
}

type CodeSearchInput struct {
    Pattern       string `json:"pattern" jsonschema_description:"Search pattern (regex)"`
    Path          string `json:"path,omitempty" jsonschema_description:"Path to search in"`
    FileType      string `json:"file_type,omitempty" jsonschema_description:"File type filter"`
    CaseSensitive bool   `json:"case_sensitive,omitempty" jsonschema_description:"Case sensitive search"`
}
```

**函数实现**:
```go
func CodeSearch(input json.RawMessage) (string, error) {
    var searchInput CodeSearchInput
    if err := json.Unmarshal(input, &searchInput); err != nil {
        return "", err
    }

    if searchInput.Pattern == "" {
        return "", fmt.Errorf("Pattern is required")
    }

    // 构建 ripgrep 命令
    args := []string{
        "-n",           // 行号
        "--heading",    // 文件名分组
        "--no-color",   // 无颜色
    }

    if !searchInput.CaseSensitive {
        args = append(args, "-i")
    }
    if searchInput.FileType != "" {
        args = append(args, "-t", searchInput.FileType)
    }

    args = append(args, searchInput.Pattern)

    if searchInput.Path != "" {
        args = append(args, searchInput.Path)
    }

    // 限制输出为前 50 个匹配
    args = append(args, "-m", "50")

    cmd := exec.Command("rg", args...)
    output, err := cmd.CombinedOutput()

    // ripgrep 退出码 1 表示无匹配（非错误）
    if err != nil {
        if exitErr, ok := err.(*exec.ExitError); ok && exitErr.ExitCode() == 1 {
            return "No matches found", nil
        }
        log.Printf("ripgrep error: %v", err)
        return fmt.Sprintf("Search failed: %s", string(output)), err
    }

    return string(output), nil
}
```

**特性**：
- 使用 `ripgrep` (`rg`) 进行搜索
- 标志：`-n`（行号）、`--heading`（文件名分组）、`--no-color`
- 可选：大小写敏感、文件类型过滤
- **限制输出**：`-m 50`，防止响应过大
- **特殊错误处理**：退出码 1（无匹配）视为成功，返回 "No matches found"

---

## 4. 开发环境

### 4.1 环境配置

**推荐方式**：使用 `devenv` 提供可重现环境

```bash
devenv shell  # 进入开发环境
```

**手动方式**：确保 Go 1.24.2+ 已安装

### 4.2 `devenv.nix` 配置

提供的工具与包：
- **语言与工具链**: Go、Node.js 20、TypeScript、Python、Rust、.NET Core
- **其他工具**: Git、`ripgrep`
- **自定义脚本**: `hello` 脚本（使用 `env.GREET` 环境变量）

**enterShell 脚本**（进入 shell 时执行）：
```nix
enterShell = ''
  echo "Git: $(git --version)"
  echo "Go: $(go version)"
  echo "Python: $(python --version)"
  echo "Node: $(node --version)"
  echo "Rust: $(rustc --version)"
  echo ".NET: $(dotnet --version)"
  echo "ripgrep: $(rg --version)"
'';
```

### 4.3 构建系统

#### Makefile

```makefile
build:  # 构建所有 Agent 二进制文件
    go build -o chat chat.go
    go build -o read read.go
    # ...

fmt:  # 格式化 Go 文件
    go fmt *.go

check:  # 运行 vet 和 tidy
    go vet chat.go
    go vet read.go
    # ...
    go mod tidy

clean:  # 删除构建产物
    rm -f chat read list_files bash_tool edit_tool code_search

all: fmt check build
```

**常用命令**：
- `make build` - 构建所有二进制
- `make fmt` - 格式化代码
- `make check` - Vet + Tidy
- `make clean` - 清理
- `make all` - 完整流程（fmt → check → build）

#### 直接 Go 命令

```bash
# 构建单个 Agent
go build -o chat chat.go

# 直接运行
go run chat.go
go run read.go
go run code_search_tool.go
```

### 4.4 依赖管理

- **Go Modules**: `go mod tidy` 管理模块和下载依赖
- **devenv**: `devenv.nix` 中的 `venv.requirements` 可指定 Python 依赖

### 4.5 环境变量

**必需**：
- `ANTHROPIC_API_KEY` - Anthropic API 密钥

**自定义**（示例）：
- `env.GREET` - 在 `devenv.nix` 中定义，供 `hello` 脚本使用

### 4.6 特殊配置模式

- **`--verbose` 标志**：所有 Go 应用支持，用于详细日志（API 调用、工具执行、文件操作、错误跟踪）
- **`devenv` 可重现环境**：通过 `devenv.nix` 定义所有依赖与工具，确保跨机器一致性
- **`enterShell` / `enterTest` 脚本**：进入 shell 或运行测试时自动执行，验证工具版本

---

## 5. 通用模式

### 5.1 工具定义模式

所有工具遵循一致模式：

1. **定义 Input 结构体**：使用 `json` 和 `jsonschema_description` 标签
   ```go
   type ToolInput struct {
       Field string `json:"field" jsonschema_description:"Field description"`
   }
   ```

2. **生成 Schema**：`GenerateSchema[ToolInput]()`

3. **实现函数**：签名为 `func(json.RawMessage) (string, error)`
   ```go
   func ToolFunc(input json.RawMessage) (string, error) {
       var toolInput ToolInput
       json.Unmarshal(input, &toolInput)
       // ... 执行逻辑
       return result, err
   }
   ```

4. **创建 ToolDefinition**：
   ```go
   var ToolDef = ToolDefinition{
       Name:        "tool_name",
       Description: "Tool description",
       InputSchema: GenerateSchema[ToolInput](),
       Function:    ToolFunc,
   }
   ```

### 5.2 错误处理模式

- **Unmarshal 失败**：
  - `read_file` / `list_files`: `panic`（假设 Schema 验证确保合法性）
  - `bash` / `code_search`: 返回错误（更保守）

- **工具执行失败**：
  - 记录日志：`log.Printf("Error: %v", err)`
  - 返回格式化错误消息和 `error` 对象
  - `code_search` 特殊处理：退出码 1 视为"无匹配"而非错误

### 5.3 集中工具执行

在 `Agent.Run()` 中：
- 遍历 `message.Content` 块
- 检测 `tool_use` 类型
- 通过 `Name` 查找对应 `ToolDefinition`
- 执行 `Function`，传入 `block.Input`
- 收集结果为 `ToolResultBlock`
- 作为新的用户消息发回 Claude

---

## 6. 关键观察

### 6.1 设计优势
- **类型安全**：Go 类型系统 + JSON Schema 自动生成
- **渐进增强**：从简单 Chat 到复杂 Code Search 的清晰演进路径
- **简洁实现**：核心事件循环清晰，易于理解
- **工具模块化**：每个工具独立定义，易于扩展

### 6.2 实现细节
- **getUserMessage 闭包**：使用 `bufio.Scanner` 读取 `os.Stdin`
- **对话历史持久化**：完整 `conversation` 切片传递给每次 API 调用
- **工具结果反馈循环**：工具输出作为用户消息返回，保持对话连续性

### 6.3 限制与注意事项
- `edit_file` 完整实现未提供，仅有输入结构体定义
- 某些工具使用系统命令（`find`、`rg`）而非 Go 标准库
- 错误处理策略不统一（panic vs return error）

---

## 参考资料

- DeepWiki 仓库: https://deepwiki.com/ghuntley/how-to-build-a-coding-agent
- 查询结果:
  - Agent 系统架构（核心组件、工具系统）
  - Agent 实现（渐进演进、工具详细实现）
  - getUserMessage 与事件循环
  - 工具结果处理、对话历史、API 交互
  - 开发环境配置

**最后更新**: 2025-10-19
