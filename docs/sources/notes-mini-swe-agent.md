# Source 3: Mini SWE Agent

**来源**: SWE-agent/mini-swe-agent
**DeepWiki**: https://deepwiki.com/SWE-agent/mini-swe-agent

## 文档结构

`mini-swe-agent` 是一个极简主义的软件工程 Agent 系统，专为 SWE-Bench 评估设计：

1. **概览**
   - 架构概览
   - 关键特性
2. **快速开始**
   - 安装
   - 快速开始指南
   - 配置设置
3. **命令行界面**
   - `mini` 命令
   - `mini-extra` 实用工具
   - 交互模式
4. **核心架构**
   - Agent 系统
   - Model 集成
   - 执行环境
5. **用户界面**
   - Simple Command Line
   - Textual User Interface
   - Batch Processing
6. **配置系统**
   - 配置文件
   - 模板系统
7. **SWE-Bench 集成**
   - Batch Evaluation
   - Single Instance Testing
   - Results and Analysis
8. **高级特性**
   - Trajectory Inspector
   - Python API
   - Container Environments
9. **开发**
   - Testing Framework
   - Development Setup
   - CI/CD and Release
   - Coding Standards

---

## 1. 三组件架构

### 1.1 架构概览

`mini-swe-agent` 采用极简的三组件架构，定义在 `src/minisweagent/__init__.py` 中：

- **`Agent`**: 协议，定义任务执行与消息流编排接口
- **`Model`**: 协议，定义与语言模型交互的接口
- **`Environment`**: 协议，定义 bash 命令执行方式

**设计哲学**：
- 极简主义：`DefaultAgent` 核心约 100 行 Python 代码
- 无自定义工具或函数调用
- 线性消息历史
- 无状态动作，通过独立的 `subprocess.run` 调用执行

### 1.2 组件协议

```python
# Agent 协议
class Agent(Protocol):
    def step(self) -> None: ...
    def run(self) -> tuple[str, str]: ...

# Model 协议
class Model(Protocol):
    def query(self, messages: list) -> str: ...

# Environment 协议
class Environment(Protocol):
    def execute(self, command: str) -> tuple[str, int]: ...
```

---

## 2. DefaultAgent 实现

### 2.1 核心方法

#### `run()` 方法

位于 `src/minisweagent/agents/default.py`：

```python
def run(self) -> tuple[str, str]:
    # 1. 初始化消息历史
    self.messages = [
        {"role": "system", "content": self.render_template(system_template)},
        {"role": "user", "content": self.render_template(instance_template)}
    ]

    # 2. 主循环
    while True:
        try:
            self.step()  # 执行单步
        except NonTerminatingException as e:
            # 添加错误到历史，继续
            self.add_message("user", str(e))
        except TerminatingException as e:
            # 返回退出状态和消息
            return (type(e).__name__, str(e))
```

**关键流程**：
1. 使用 `system_template` 和 `instance_template` 初始化消息历史
2. 进入循环，持续调用 `step()`
3. **异常处理**：
   - `NonTerminatingException`: 错误字符串加入历史，循环继续
   - `TerminatingException`: 返回异常类型名和消息，结束执行

#### `step()` 方法

单步执行周期：

```python
def step(self) -> None:
    # 1. 查询模型
    response = self.query()

    # 2. 解析动作
    action = self.parse_action(response)

    # 3. 执行动作
    output, returncode = self.execute_action(action)

    # 4. 检查完成
    if self.has_finished(output):
        raise Submitted(output)

    # 5. 渲染观察
    observation = self.render_template(
        self.action_observation_template,
        output=output,
        returncode=returncode
    )

    # 6. 添加到历史
    self.add_message("user", observation)
```

**详细步骤**：

1. **查询模型**：
   - 调用 `self.query()` 与语言模型交互
   - `query()` 检查步数和成本限制，调用 `self.model.query(self.messages)`
   - 将模型响应加入消息历史

2. **解析动作**：
   - `self.parse_action(response)` 从响应中提取 shell 命令
   - 使用正则表达式查找 bash 代码块
   - 如果响应不包含恰好 1 个 bash 代码块，抛出 `FormatError` (`NonTerminatingException`)

3. **执行动作**：
   - `self.execute_action(action)` 调用 `self.env.execute()`
   - 如果超时，抛出 `ExecutionTimeoutError` (`NonTerminatingException`)

4. **检查完成**：
   - `self.has_finished(output)` 检查输出是否包含完成信号（如 `"COMPLETE_TASK_AND_SUBMIT_FINAL_OUTPUT"`）
   - 如果找到，抛出 `Submitted` (`TerminatingException`)

5. **渲染观察**：
   - 使用 `action_observation_template` 和 Jinja2 格式化命令输出
   - 包括截断逻辑处理大输出

6. **添加到历史**：
   - `self.add_message("user", observation)` 将观察加入消息历史

### 2.2 异常层级

#### NonTerminatingException

Agent 可以处理并恢复的条件，允许 `run()` 循环继续：

- **`FormatError`**: 模型输出格式不匹配（如 bash 代码块数量错误）
- **`ExecutionTimeoutError`**: 执行的动作超时

#### TerminatingException

需要停止 Agent 执行的条件：

- **`Submitted`**: Agent 明确表示已完成任务（输出特定短语如 `"COMPLETE_TASK_AND_SUBMIT_FINAL_OUTPUT"`）
- **`LimitsExceeded`**: 达到预定义的步数或成本限制

---

## 3. 模板系统

### 3.1 Jinja2 模板

系统使用 Jinja2 进行动态提示生成与输出格式化。

#### `render_template()` 方法

```python
def render_template(self, template: str, **kwargs) -> str:
    # 合并 agent、environment、model 配置变量
    variables = {**self._config, **kwargs}
    return jinja2.Template(template, undefined=jinja2.StrictUndefined).render(variables)
```

**特性**：
- 接受模板字符串和关键字参数
- 合并 agent、environment、model 配置变量
- 使用 `StrictUndefined` 确保所有变量已定义

### 3.2 关键模板

| 模板名 | 用途 | 组件示例 |
|--------|------|----------|
| `system_template` | Agent 的初始指令 | 系统角色、任务描述 |
| `instance_template` | 任务特定指令 | 问题描述、代码库信息 |
| `action_observation_template` | 格式化执行命令的输出 | `<returncode>`、`<output>`、`<output_head>`、`<elided_chars>`、`<output_tail>`、`<warning>` |
| `timeout_template` | 命令超时时通知 Agent | 超时消息 |
| `format_error_template` | 模型输出格式错误时使用 | 错误说明 |

**输出截断示例**（`action_observation_template`）：
```jinja2
<returncode>{{ returncode }}</returncode>
{% if output|length < max_output_length %}
<output>{{ output }}</output>
{% else %}
<output_head>{{ output[:max_output_length//2] }}</output_head>
<elided_chars>{{ output|length - max_output_length }}</elided_chars>
<output_tail>{{ output[-max_output_length//2:] }}</output_tail>
<warning>Output truncated</warning>
{% endif %}
```

---

## 4. Model 集成

### 4.1 Model 协议

位于 `src/minisweagent/__init__.py`：

```python
class Model(Protocol):
    def query(self, messages: list) -> str: ...
```

### 4.2 实现

- **`LitellmModel`**: 使用 LiteLLM 库支持多种模型提供商
- **`AnthropicModel`**: 直接集成 Anthropic API

### 4.3 模型选择

`get_model()` 函数位于 `src/minisweagent/models/__init__.py`：

- 通过命令行选项或 YAML 配置文件指定模型名称和类
- 示例配置：
  ```yaml
  model:
    name: "claude-3-5-sonnet-20241022"
    class: "AnthropicModel"
  ```

---

## 5. 执行环境

### 5.1 Environment 协议

```python
class Environment(Protocol):
    def execute(self, command: str) -> tuple[str, int]: ...
    def cleanup(self) -> None: ...
```

### 5.2 实现类型

| 环境类型 | 类名 | 用途 |
|----------|------|------|
| 本地执行 | `LocalEnvironment` | 使用 `subprocess.run` 在本地执行命令 |
| Docker 容器 | `DockerEnvironment` | 隔离执行环境，用于 SWE-Bench |
| Singularity 容器 | `SingularityEnvironment` | 可写沙箱目录 |

### 5.3 LocalEnvironment

```python
class LocalEnvironment:
    def execute(self, command: str) -> tuple[str, int]:
        result = subprocess.run(
            ["bash", "-c", command],
            cwd=self.cwd,
            env=self.env,
            timeout=self.timeout,
            capture_output=True,
            text=True
        )
        return (result.stdout + result.stderr, result.returncode)
```

**特性**：
- 使用 `subprocess.run` 执行
- 每个动作独立且无状态
- 配置工作目录、环境变量、超时

### 5.4 DockerEnvironment

#### 配置

`DockerEnvironmentConfig` 数据类：
- `image`: Docker 镜像名称
- `cwd`: 容器内工作目录
- `env`: 要设置的环境变量
- `forward_env`: 从宿主机转发的环境变量
- `timeout`: 命令超时
- `executable`: Docker 可执行文件路径

**优先级**：`env` 中指定的变量优先于通过 `forward_env` 转发的变量

#### 生命周期

1. **初始化**（`__init__`）：
   ```bash
   docker run -d --name <unique_name> <image> sleep <container_timeout>
   ```
   - 后台模式 (`-d`)
   - 唯一容器名
   - `sleep` 保持容器运行
   - 存储容器 ID

2. **执行**（`execute`）：
   ```bash
   docker exec -w <cwd> -e VAR1=val1 -e VAR2=val2 <container_id> bash -lc "<command>"
   ```
   - `-w`: 设置工作目录
   - `-e`: 转发指定环境变量
   - `bash -lc`: 执行命令
   - 捕获输出和返回码

3. **清理**（`cleanup`）：
   ```bash
   docker stop <container_id>
   docker rm -f <container_id>
   ```
   - 也在 `__del__` 中自动调用

### 5.5 SingularityEnvironment

#### 配置

`SingularityEnvironmentConfig` 数据类：
- `image`: Singularity 镜像路径或 URI
- `cwd`, `env`, `forward_env`, `timeout`, `executable`（同 Docker）
- `sandbox_build_retries`: 沙箱构建重试次数

#### 生命周期

1. **初始化**（`__init__`）：
   - 调用 `_build_sandbox` 创建可写沙箱目录

2. **沙箱构建**（`_build_sandbox`）：
   ```bash
   singularity build --sandbox <sandbox_dir> <image>
   ```
   - 带重试逻辑增强鲁棒性

3. **执行**（`execute`）：
   ```bash
   singularity exec --contain --cleanenv --pwd <cwd> --env VAR1=val1 --writable <sandbox_dir> bash -lc "<command>"
   ```
   - `--contain --cleanenv`: 隔离
   - `--pwd`: 设置工作目录
   - `--env`: 环境变量
   - `--writable`: 允许沙箱内修改

4. **清理**（`cleanup`）：
   ```python
   shutil.rmtree(self.sandbox_dir)
   ```
   - 也在 `__del__` 中触发

---

## 6. SWE-Bench 集成

### 6.1 批处理工作流

脚本：`src/minisweagent/run/extra/swebench.py`

#### 数据集加载

```python
def main():
    dataset = datasets.load_dataset(
        subset,  # "lite", "verified", 或自定义路径
        split=split
    )
```

#### 数据集过滤

`filter_instances()` 函数：

1. **Shuffling**（如果 `--shuffle` 启用）：
   - 按 `instance_id` 排序
   - 使用固定种子 (42) 随机打乱

2. **Filtering**：
   - 使用 `--filter-spec` 提供的正则表达式匹配 `instance_id`

3. **Slicing**：
   - 应用切片符号（如 `--slice 0:5`）选择子集

4. **Skipping Existing**：
   - 如果 `--redo-existing` 未设置，跳过 `preds.json` 中已有的实例

### 6.2 并行处理

使用 `concurrent.futures.ThreadPoolExecutor`：

```python
with ThreadPoolExecutor(max_workers=workers) as executor:
    futures = {
        executor.submit(process_instance, instance): instance
        for instance in filtered_instances
    }

    for future in process_futures(futures):
        # 处理完成的 future
        # 更新进度
        # 保存结果
```

**关键特性**：
- 工作线程数通过 `--workers` 控制
- 每个实例提交给 `process_instance` 函数
- `process_futures` 辅助函数遍历已完成的 futures
- `RunBatchProgressManager` 跟踪和显示进度
- `KeyboardInterrupt` 处理允许优雅取消待处理作业

### 6.3 结果保存模式

#### `preds.json`

`update_preds_file()` 函数：
- 为每个 `instance_id` 写入 `model_patch`（结果）
- 使用文件锁（`_OUTPUT_FILE_LOCK`）确保线程安全写入
- 增量更新，防止长运行中数据丢失

#### Trajectory 文件

`save_traj()` 函数：
- 为每个实例保存详细轨迹（`.traj.json`）
- 位于以 `instance_id` 命名的子目录
- 包含：`exit_status`、`result`、`extra_info`（如错误回溯）

#### 清理现有结果

处理实例前：
- 删除 `preds.json` 中该实例的现有条目
- 删除现有的 trajectory 文件
- 确保干净状态

---

## 7. 交互模式

### 7.1 InteractiveAgent

扩展 `DefaultAgent`，提供简单的 REPL 风格命令行界面。

#### 配置

`InteractiveAgentConfig` 扩展 `AgentConfig`：
- `mode`: 交互模式（confirm/yolo/human）
- `whitelist_actions`: 在 confirm 模式下跳过确认的正则模式
- `confirm_exit`: 是否在 Agent 要完成时请求确认

#### 实现细节

使用 `prompt_toolkit` 处理输入（带历史记录）和 `rich.Console` 显示消息。

**重写方法**：
- `query()`: 处理 `human` 模式，允许直接用户输入
- `execute_action()`: 基于当前 `mode` 和 `whitelist_actions` 检查是否需要确认
- `_prompt_and_handle_special()`: 管理模式切换命令（`/u`, `/c`, `/y`, `/h`）和用户输入

#### 用户交互模式

| 模式 | 描述 | 行为 |
|------|------|------|
| **confirm** | 默认模式 | LM 提议动作，用户必须确认后执行 |
| **yolo** | 自动执行 | LM 生成的动作立即执行，无需确认 |
| **human** | 直接控制 | 用户直接输入命令，绕过 LM |

**模式切换命令**：
- `/c` → confirm 模式
- `/y` → yolo 模式
- `/u` → human 模式
- `/h` → 显示帮助

### 7.2 TextualAgent

提供基于 `Textual` 库的高级可视化界面（TUI）。

#### 架构

- Agent 逻辑在后台线程运行
- UI 在主线程保持响应
- `_TextualAgent` 包装 `DefaultAgent` 并与 `TextualAgent` (App 类) 通信

#### 配置

`TextualAgentConfig` 类似 `InteractiveAgentConfig`，专为 TUI 设计。

#### 实现细节

**核心组件**：
- `SmartInputContainer`: 灵活的单行和多行输入处理
- 线程：允许 Agent 在后台运行，UI 保持交互
- 按键绑定：导航和模式切换

**按键绑定**：
- `c` → confirm 模式
- `y` → yolo 模式
- `u` → human 模式
- `h` / `LEFT` → 上一步
- `l` / `RIGHT` → 下一步
- `0` → 第一步
- `$` → 最后一步
- `Ctrl+T` → 在单行/多行输入间切换

**SmartInputContainer**：
- 支持单行和多行输入模式
- 使用 `Ctrl+T` 切换
- 在 confirm 模式下处理确认，在 human 模式下处理直接命令输入

### 7.3 与 DefaultAgent 的差异

| 维度 | DefaultAgent | InteractiveAgent | TextualAgent |
|------|--------------|------------------|--------------|
| **用户交互** | 无交互（批处理） | 基础 REPL CLI | 完整可视化 TUI |
| **界面技术** | 无 | `prompt_toolkit` + `rich` | `Textual` 框架 |
| **线程** | 同步 | 同步 | 多线程（后台 Agent + UI 主线程） |
| **输入处理** | 无 | `prompt_toolkit.PromptSession` | 自定义 `SmartInputContainer` |
| **导航** | 无 | 线性历史 | 步骤级导航（前/后/首/尾） |
| **模式** | 无 | confirm/yolo/human | confirm/yolo/human |

### 7.4 调用方式

- `mini` → `InteractiveAgent`（无 `-v` 标志）
- `mini -v` → `TextualAgent`（视觉模式）
- `mini-extra swebench` → `DefaultAgent`（批处理）
- `mini-extra swebench-single` → `InteractiveAgent`（单实例调试）

环境变量：`MSWEA_VISUAL_MODE_DEFAULT` 可设置默认视觉模式

---

## 8. 配置系统

### 8.1 配置结构

YAML 配置文件顶层键：

- `agent`: Agent 行为设置（提示模板、限制）
- `environment`: 执行环境配置
- `model`: 语言模型规格和参数
- `run`: 运行时行为（如输出文件路径）

#### agent 配置示例

```yaml
agent:
  system_template: "You are a software engineering assistant..."
  instance_template: "Fix the following issue: {{ problem_statement }}"
  action_observation_template: "<returncode>{{ returncode }}</returncode>..."
  format_error_template: "Invalid format. Please provide exactly one bash code block."
  step_limit: 50
  cost_limit: 10.0
```

#### environment 配置示例

```yaml
environment:
  class: "DockerEnvironment"
  image: "sweagent/swe-bench:latest"
  cwd: "/workspace"
  timeout: 30
```

#### model 配置示例

```yaml
model:
  name: "claude-3-5-sonnet-20241022"
  class: "AnthropicModel"
```

### 8.2 加载与优先级

- 从 YAML 文件加载（如 `default.yaml`, `mini.yaml`, `swebench.yaml`）
- 命令行参数和环境变量可覆盖
- 优先级：CLI 参数 > 环境变量 > YAML 配置

#### DefaultAgent 构造函数

```python
def __init__(self, **kwargs):
    # kwargs 直接设置配置选项
    # 存储在 AgentConfig 数据类中
```

---

## 9. 命令行界面

### 9.1 `mini` 命令

**REPL 风格交互 CLI**，用于本地开发：

```bash
mini                    # InteractiveAgent
mini -v                 # TextualAgent (visual mode)
```

**特性**：
- 基于 `visual` 标志选择 Agent 类
- 使用 `MSWEA_VISUAL_MODE_DEFAULT` 环境变量

### 9.2 `mini-extra` 实用工具

#### `swebench-single`

与单个 SWE-Bench 实例交互调试：

```bash
mini-extra swebench-single --instance-id <id>
```

- 使用 `InteractiveAgent`
- 加载特定实例元数据
- 设置 Docker 环境（通过 `get_sb_environment()`）

#### `swebench`

大规模评估的自动批处理：

```bash
mini-extra swebench --subset lite --workers 4 --slice 0:10
```

**关键选项**：
- `--subset`: 数据集名称（lite/verified/路径）
- `--split`: 数据集分割
- `--workers`: 并行线程数
- `--slice`: 实例切片（如 `0:10`）
- `--filter-spec`: 正则过滤 instance_id
- `--shuffle`: 随机打乱实例
- `--redo-existing`: 重新处理已有实例

**执行流程**：
1. 加载并过滤数据集
2. 使用 `ThreadPoolExecutor` 并行处理实例
3. 增量保存结果（`preds.json` + trajectory 文件）
4. `RunBatchProgressManager` 跟踪进度

---

## 10. 关键观察

### 10.1 设计哲学

- **极简主义**：核心 `DefaultAgent` 约 100 行
- **无工具抽象**：仅 bash 命令，无自定义工具或函数调用
- **线性历史**：简单的消息列表
- **无状态执行**：每个动作通过独立的 `subprocess.run`

### 10.2 扩展性

- **协议驱动**：`Agent`、`Model`、`Environment` 协议允许多种实现
- **模块化环境**：本地、Docker、Singularity 可互换
- **灵活配置**：YAML 配置 + CLI 覆盖
- **模板化提示**：Jinja2 支持动态内容生成

### 10.3 SWE-Bench 优化

- **批处理并行**：`ThreadPoolExecutor` 支持大规模评估
- **增量保存**：防止长运行中数据丢失
- **Docker 隔离**：为每个实例提供干净、可重现的环境
- **过滤与切片**：灵活的数据集选择

### 10.4 交互灵活性

- **三层交互**：批处理（DefaultAgent）→ REPL CLI（InteractiveAgent）→ TUI（TextualAgent）
- **模式系统**：confirm/yolo/human 适配不同工作流
- **步骤导航**（TUI）：前/后/首/尾步骤检查

### 10.5 异常驱动控制流

- `NonTerminatingException` → 错误恢复，循环继续
- `TerminatingException` → 优雅退出
- 清晰分离可恢复错误（格式、超时）vs 终止条件（提交、限制）

---

## 参考资料

- DeepWiki 仓库: https://deepwiki.com/SWE-agent/mini-swe-agent
- 查询结果:
  - 架构概览（Agent/Model/Environment 协议）
  - DefaultAgent 的 step()/run() 方法
  - 模板系统、消息历史、异常处理
  - 配置管理
  - Docker/Singularity 环境实现
  - SWE-Bench 数据集加载、过滤、并行处理
  - InteractiveAgent 和 TextualAgent 实现
  - 交互模式（confirm/yolo/human）

**最后更新**: 2025-10-19
