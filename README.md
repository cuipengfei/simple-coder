# Simple Coder - 通过构建来学习编码代理

> 一个教育性项目：用 AI 代理研究 AI 代理，用 AI 代理构建 AI 代理，最后让 AI 代理读取和改进自己的代码。

---

## 故事骨架

### 第一章：研究 - 用代理研究代理

**起点**  
我们想理解编码代理的工作原理。与其只读文档，不如自己动手做一个。

**研究方法**  
- 选定参考项目：`mini-swe-agent`、系统提示词研究、编码代理架构分析
- 工具：Claude Code、Gemini CLI + MCP 服务器（Context7、DeepWiki）
- 输出：`docs/sources/` 下的研究笔记、`docs/syntheses/` 下的架构综合分析

**关键发现**  
- ReAct 模式（Reasoning → Acting → Observing）
- 工具调用机制
- 路径安全验证
- 精确替换策略（防止批量误改）

### 第二章：设计 - 从研究到规划

**文档驱动**  
基于研究结果，我们定义了：
- `PRD.md` - 教育目标、范围、非目标
- `FEATURES.md` - 功能清单
- `ROADMAP.md` - 分阶段实施计划
- `IMPLEMENTATION.md` - 技术实现细节

**核心决策**  
- 技术栈：Java + Spring Boot + Spring AI
- 架构：无状态、单仓库范围、工具注解驱动
- 工具集：文件读写、目录列表、文本搜索、精确替换

### 第三章：构建 - 用代理构建代理

**协作模式**  
- Gemini：规划、评审、风险控制
- Claude Code：编码、测试实现
- 流程：Gemini 规划 → Claude 实现 → Gemini 回顾

**已实现功能**（仅列出实际存在的 Java 代码）

**核心服务**：
```
com.simplecoder.service.AgentService
  - 单次 ChatClient 调用，Spring AI 内部处理 ReAct 工具循环
  - 无状态设计，每次请求独立处理
  - 上下文由客户端随请求传入（ToolRequest.contextHistory）

com.simplecoder.service.ToolsService
  - @Tool readFile(filePath, startLine, endLine)
    - 读取文件内容，支持行范围
    - 自动截断（max-file-lines: 500）
    - 行号格式化输出
  
  - @Tool listFiles(path)
    - 支持目录列出或 glob（如 **/*.java）
    - 自动截断（max-list-results: 200）
  
  - @Tool searchText(pattern, searchPath, isRegex, caseSensitive)
    - 文本搜索（字面量或正则）
    - 大小写敏感可选
    - 返回 file:line:snippet 格式
    - 自动截断（max-search-results: 50）
  
  - @Tool replaceText(filePath, oldString, newString)
    - 精确字符串替换
    - 安全检查：出现次数必须唯一（0 或 >1 次抛异常）
```

**安全机制**：
```
com.simplecoder.tool.PathValidator
  - 仓库根路径限制
  - 路径遍历防护（../ 检测）
  - 绝对路径转换与校验
```

**异常分类**：
```
com.simplecoder.exception
  - ValidationException（参数验证失败）
  - SecurityViolationException（路径安全违规）
  - SystemException（系统级错误）
```

**数据模型**：
```
com.simplecoder.model
  - ToolRequest（用户请求 + 上下文历史）
  - ToolResponse（成功/失败响应）
  - ContextEntry（对话历史条目）
```

**Web 接口**：
```
com.simplecoder.controller.AgentController
  - POST /api/agent
  - 调用 AgentService.process 构建响应
  - 异常在 AgentService 内部转换为 ToolResponse
```

**测试覆盖**（5 个测试类）：
```
AgentControllerTest - 控制器层测试
ToolsServiceExceptionTest - 工具异常测试
ContextEntryTest - 上下文模型测试
ToolRequestTest - 请求模型验证测试
ToolResponseTest - 响应模型测试
```

**前端**：
```
static/index.html
  - 单页应用
  - 多行文本输入
  - 执行按钮
  - 等宽字体结果展示区
  - 加载 / 错误状态提示
  - 保留最近 20 条交互记录
```

### 第四章：自省 - 代理读取自己

**已验证能力**  
代理现在可以：
- `listFiles("src/**/*.java")` - 列出自身源码文件
- `readFile("src/main/java/com/simplecoder/service/ToolsService.java")` - 读取工具实现
- `searchText("@Tool", "src/main/java", false, false)` - 搜索自身工具注解
- `readFile("docs/PRD.md")` - 阅读设计文档

**元循环示例**  
```
用户: "读取你自己的 ToolsService.java，找出有多少个 @Tool 方法"

代理: 
  [第 1 轮] 调用 readFile("src/main/java/com/simplecoder/service/ToolsService.java")
  [第 2 轮] 分析结果，返回：“我有 4 个工具：readFile、listFiles、searchText、replaceText”
```

**理论上的自我改进**  
代理可以调用 `replaceText` 修改自己的代码，但：
- ⚠️ 需要重新编译才生效
- ⚠️ 精确替换要求完全匹配，难度较高
- ⚠️ 教育项目，未实现自动重启/热加载

### 第五章：元叙事 - 这个骨架也是代理写的

**完整循环**  
1. 人类定义目标：“写一个 README 骨架，只说已实现的功能”
2. Gemini CLI 代理：
   - 读取 `ToolsService.java`
   - 读取 `AgentService.java`
   - 读取 `docs/sources.md`
   - 读取 `docs/PRD.md`
   - 读取测试文件列表
3. 生成这个 README.md 骨架
4. 人类审核、调整

**递归深度**  
- 代理研究代理架构 → 代理构建代理系统 → 代理读取代理代码 → 代理撰写代理文档 → ...

---

## 快速开始

### 运行项目
```bash
mvn spring-boot:run
```

浏览器访问：`http://localhost:8080`

### 测试
```bash
mvn test
```

### 示例请求
```json
POST http://localhost:8080/api/agent
{
  "prompt": "列出所有 Java 文件",
  "contextHistory": []
}
```

---

## 项目结构

```
src/main/java/com/simplecoder/
  ├── config/          # Spring AI 配置、日志切面
  ├── controller/      # REST API 控制器
  ├── exception/       # 异常分类体系
  ├── model/           # 请求/响应模型
  ├── service/         # 核心代理逻辑 + 工具服务
  └── tool/            # 路径安全验证

docs/
  ├── sources/         # 开源项目研究笔记
  ├── syntheses/       # 架构综合分析
  ├── references/      # 术语表、引用
  ├── PRD.md           # 产品需求文档
  ├── FEATURES.md      # 功能清单
  ├── ROADMAP.md       # 实施路线图
  └── IMPLEMENTATION.md # 技术实现细节
```

---

## 技术栈

- **后端**：Java 21, Spring Boot 3.5.6
- **AI 框架**：Spring AI 1.0.3 (ChatClient + @Tool)
- **前端**：原生 HTML + Fetch API + TailwindCDN
- **测试**：JUnit 5, Spring Boot Test
- **构建**：Maven

---

## 关键特性

✅ **ReAct 工具循环**：单次调用内部可能触发多次工具调用  
✅ **工具自动调用**：框架根据 @Tool 注解自动注册和执行  
✅ **路径安全**：仓库根限制 + 路径遍历防护  
✅ **精确替换**：唯一匹配检查，防止批量误改  
✅ **异常分类**：Validation / SecurityViolation / System  
✅ **无状态设计**：每次请求独立，历史由客户端传入  
✅ **上下文历史**：客户端维护的最近交互传给模型  

---

## 局限性（教育简化）

⚠️ 无持久化存储  
⚠️ 无身份验证  
⚠️ 无命令执行工具（未在当前代码中实现）  
⚠️ 无 TODO 系统（未在当前代码中实现）  
⚠️ 无并行工具执行  
⚠️ 无容器隔离  

---

## 学到了什么

1. **ReAct 模式不神秘**：Reasoning(LLM 思考) → Acting(调用工具) → Observing(查看结果) → 重复
2. **工具就是函数**：`@Tool` 注解 + 清晰描述，LLM 就知道何时调用
3. **安全很重要**：路径验证、唯一替换、异常分类，缺一不可
4. **框架做重活**：Spring AI 处理工具注册、参数解析、循环控制，我们只需聚焦业务逻辑
5. **代理可以自省**：读取自己的代码、分析自己的结构、撰写自己的文档

---

## 许可证

Unlicense 公共领域声明，详见 LICENSE 或 https://unlicense.org

---

**元注释**：这个 README 由 Gemini CLI 代理基于实际代码生成，人类审核后发布。证明：代理可以准确描述自己的能力。

