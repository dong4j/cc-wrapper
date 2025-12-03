# CC Wrapper 插件改进说明

本文档记录了 CC Wrapper IDEA 插件的开发改进过程，包括从模板项目到 MVP 实现的完整过程。

## 项目概述

CC Wrapper 是一个 IntelliJ IDEA 插件，用于在 IDE 中集成 Codex CLI，通过 MCP (Model Context Protocol) 协议与 Codex 通信，并在 Tool Window
中展示交互过程和结果。

## 改进历程

### 1. 项目重命名与配置更新

#### 1.1 项目重命名

- **原项目名**: `template-without-ai`
- **新项目名**: `cc-wrapper`
- **插件 ID**: `dev.dong4j.ccwrapper`
- **插件名称**: `CC Wrapper`

#### 1.2 配置文件更新

- `gradle.properties`: 更新插件组、名称、版本等信息
- `plugin.xml`: 更新插件 ID、名称、依赖项
- `build.gradle.kts`: 添加 Kotlin 支持和相关依赖

### 2. 技术栈转换

#### 2.1 从 Java 到 Kotlin

- 添加 Kotlin 插件支持
- 更新编译配置，使用新的 `compilerOptions` DSL（修复弃用警告）
- 所有核心代码使用 Kotlin 实现

#### 2.2 依赖管理

- 添加 Gson 用于 JSON 解析
- 添加 Terminal 插件依赖（用于未来可能的终端集成）
- 移除 Lombok 依赖（Kotlin 不需要）

### 3. 核心功能实现

#### 3.1 Tool Window 实现

**文件**: `src/main/kotlin/dev/dong4j/ccwrapper/ui/CcWrapperToolWindowFactory.kt`

创建了 Tool Window Factory，用于在 IDEA 中注册和显示 "CC Wrapper" Tool Window。

**文件**: `src/main/kotlin/dev/dong4j/ccwrapper/ui/CcWrapperPanel.kt`

实现了主面板，包含：

- 消息列表展示区域（垂直布局）
- 输入框和发送按钮
- 状态标签
- 自动滚动到底部

#### 3.2 Codex MCP 客户端实现

**文件**: `src/main/kotlin/dev/dong4j/ccwrapper/mcp/CodexMcpManager.kt`

实现了完整的 Codex MCP 客户端，参考了 `happy-cli` 的 `CodexMcpClient` 实现：

**核心功能**:

- **连接管理**: 启动 `codex mcp-server` 进程，建立 stdio 通信
- **会话管理**:
    - `startSession()`: 启动新的 Codex 会话
    - `continueSession()`: 继续现有会话
    - 自动提取和管理 sessionId/conversationId
- **消息处理**:
    - 解析 JSON-RPC 2.0 协议消息
    - 处理 `codex/event` 通知
    - 提取各种事件类型（agent_message, exec_command_begin, exec_command_end 等）
- **项目信息传递**:
    - 设置进程工作目录为项目根路径
    - 在启动会话时设置 `cwd` 参数
    - 在 prompt 中添加项目上下文信息

**关键实现细节**:

```kotlin
// 设置工作目录
processBuilder.directory(java.io.File(projectBasePath))

// 启动会话时传递项目信息
addProperty("cwd", projectBasePath)
addProperty("prompt", enhancedPrompt) // 包含项目上下文
```

#### 3.3 项目信息工具

**文件**: `src/main/kotlin/dev/dong4j/ccwrapper/util/ProjectInfoUtil.kt`

实现了项目信息收集工具：

**功能**:

- 获取项目根路径
- 获取项目名称
- 检测项目类型（Maven、Gradle、Node.js、Rust、Go、Python、Ruby 等）
- 检测关键文件（README、配置文件等）
- 构建项目上下文信息字符串

**项目上下文格式**:

```
=== Project Context ===
Project Name: xxx
Project Type: Gradle
Project Path: /path/to/project
Key Files: README.md, build.gradle.kts, ...
=======================
```

#### 3.4 消息渲染组件

**文件**: `src/main/kotlin/dev/dong4j/ccwrapper/ui/components/CodexMessageRenderer.kt`

参考 `happy` 移动端的展示方式，实现了不同类型的消息渲染：

**支持的消息类型**:

- **用户消息**: 灰色背景，显示 "You:"
- **助手消息**: 白色背景，显示 "Codex:"
- **工具调用**: 显示命令执行，类似 `CommandView`
    - 图标：⚙️
    - 显示格式：`Executing: <command>`
- **工具结果**: 显示输出/错误，区分成功/失败
    - 成功：✅ 绿色
    - 失败：❌ 红色
    - 使用等宽字体显示输出
- **推理消息**: 黄色背景，显示思考过程
    - 图标：💭
    - 显示格式：`Thinking: <text>`
- **状态消息**: 蓝色背景，显示任务状态
    - 图标：ℹ️
- **错误消息**: 红色背景，突出显示错误
    - 图标：❌

**实现特点**:

- 使用 HTML 标签实现富文本显示
- 不同消息类型使用不同的背景色和图标
- 命令使用等宽字体和代码样式显示
- 输出支持多行显示和自动换行

#### 3.5 日志工具类

**文件**: `src/main/kotlin/dev/dong4j/ccwrapper/util/LogUtil.kt`

实现了统一的日志工具类，用于开发调试：

**功能**:

- 使用 `System.out.println` 输出到 IDEA 控制台
- 通过 `DEBUG` 常量控制是否输出（上线时设置为 `false` 即可禁用）
- 支持多种日志级别：`info`, `debug`, `warn`, `error`, `verbose`
- 自动添加时间戳、日志级别、线程名
- 支持参数格式化（类似 `String.format`）
- 支持异常堆栈跟踪输出
- 支持大对象（JSON）的格式化输出

**使用示例**:

```kotlin
LogUtil.info("连接 Codex...")
LogUtil.debug("收到消息: %s", message)
LogUtil.error("连接失败", exception)
LogUtil.debugLargeJson("响应数据", responseObject)
```

**上线时禁用**:
只需在 `LogUtil.kt` 中修改：

```kotlin
private const val DEBUG = false  // 改为 false 即可禁用所有日志
```

### 4. 数据流程设计

参考 `happy-cli` 的实现，设计了完整的数据流程：

#### 4.1 Codex 输出收集

**happy-cli 的实现** (`src/codex/runCodex.ts`):

- 监听 Codex MCP 事件（`exec_command_begin`, `exec_command_end` 等）
- 转换为标准格式：`{ type: 'tool-call', name: 'CodexBash', ... }`
- 通过 `session.sendCodexMessage()` 发送到服务器

**CC Wrapper 的实现**:

- 直接解析 Codex MCP 事件（不经过服务器）
- 在本地转换为 `CodexMessage` 对象
- 通过回调函数传递给 UI 组件

#### 4.2 消息格式

**happy-cli 发送的格式** (`src/api/apiSession.ts`):

```typescript
{
    role: 'agent',
    content: {
        type: 'codex',
        data: {
            type: 'tool-call',
            name: 'CodexBash',
            callId: '...',
            input: { command: '...', cwd: '...' },
            id: '...'
        }
    }
}
```

**CC Wrapper 内部格式**:

```kotlin
data class CodexMessage(
    val type: String,  // "user", "assistant", "tool-call", "tool-result", etc.
    val content: String,
    val timestamp: Long,
    val metadata: Map<String, Any>?  // 存储额外信息（command, output, error 等）
)
```

#### 4.3 消息类型映射

| Codex MCP 事件         | CC Wrapper 消息类型 | 说明          |
|----------------------|-----------------|-------------|
| `agent_message`      | `assistant`     | Codex 的文本回复 |
| `exec_command_begin` | `tool-call`     | 命令开始执行      |
| `exec_command_end`   | `tool-result`   | 命令执行完成      |
| `agent_reasoning`    | `reasoning`     | Codex 的思考过程 |
| `task_started`       | `status`        | 任务开始        |
| `task_complete`      | `status`        | 任务完成        |
| `turn_aborted`       | `status`        | 任务中止        |

### 5. UI 改进

#### 5.1 从表格到消息列表

**改进前**:

- 使用 `JBTable` 显示消息
- 三列：时间、类型、消息内容
- 信息密度高，可读性差

**改进后**:

- 使用垂直布局的 `JPanel`
- 每条消息独立渲染
- 不同类型消息有不同的视觉样式
- 参考 `happy` 移动端的展示方式

#### 5.2 消息渲染优化

**参考实现**:

- `happy/sources/components/CommandView.tsx`: 命令显示组件
- `happy/sources/components/tools/views/CodexBashView.tsx`: Codex 命令视图
- `happy/sources/components/tools/ToolView.tsx`: 工具调用视图

**实现特点**:

- 使用 HTML 标签实现富文本
- 不同消息类型使用不同颜色和图标
- 命令使用等宽字体和代码样式
- 支持多行输出和自动换行

### 6. 项目信息传递

参考 `happy-cli` 的实现，在启动 Codex session 时传递项目信息：

#### 6.1 工作目录设置

```kotlin
// 设置 Codex MCP 进程的工作目录
processBuilder.directory(java.io.File(projectBasePath))
```

#### 6.2 Session 配置

```kotlin
// 在启动会话时设置 cwd 参数
addProperty("cwd", projectBasePath)

// 在 prompt 中添加项目上下文
val projectContext = ProjectInfoUtil.buildProjectContext(project)
val enhancedPrompt = "$projectContext\n\n$prompt"
addProperty("prompt", enhancedPrompt)
```

#### 6.3 项目上下文内容

包含以下信息：

- 项目名称
- 项目类型（Maven、Gradle、Node.js 等）
- 项目路径
- 关键文件列表（README、配置文件等）

### 7. 代码质量改进

#### 7.1 日志系统

- 统一的日志工具类
- 开发时输出详细日志
- 上线时可一键禁用

#### 7.2 错误处理

- 完善的异常捕获和处理
- 用户友好的错误提示
- 详细的调试日志

#### 7.3 代码组织

- 清晰的包结构
- 职责分离（UI、MCP、工具类）
- 参考 happy-cli 的实现模式

## 技术参考

### 参考项目

1. **happy-cli** (`happy-cli/`)
    - Codex MCP 客户端实现：`src/codex/codexMcpClient.ts`
    - Codex 运行逻辑：`src/codex/runCodex.ts`
    - 消息发送：`src/api/apiSession.ts`

2. **happy** (`happy/`)
    - 消息显示：`sources/components/MessageView.tsx`
    - 命令视图：`sources/components/CommandView.tsx`
    - Codex 工具视图：`sources/components/tools/views/CodexBashView.tsx`

3. **codex-launcher** (`codex-launcher/`)
    - 终端管理：`src/main/kotlin/.../terminal/CodexTerminalManager.kt`
    - 命令构建：`src/main/kotlin/.../cli/CodexArgsBuilder.kt`

### 关键技术点

1. **MCP 协议**: Model Context Protocol，通过 stdio 进行 JSON-RPC 2.0 通信
2. **JSON-RPC 2.0**: Codex MCP 使用的通信协议
3. **项目信息提取**: 使用 IntelliJ Platform API 获取项目信息
4. **Swing UI**: 使用 Swing 实现 Tool Window 界面

## 使用说明

### 开发环境

1. **构建插件**:
   ```bash
   ./gradlew buildPlugin
   ```

2. **运行测试**:
   ```bash
   ./gradlew runIde
   ```

3. **启用日志**:
    - 默认已启用（`LogUtil.DEBUG = true`）
    - 日志输出到 IDEA 控制台

### 使用方法

1. **启动 Codex**:
    - 点击工具栏上的 "Launch Codex" 按钮
    - 或通过 `Tools` → `Launch Codex` 菜单

2. **发送消息**:
    - 在 Tool Window 底部的输入框中输入消息
    - 按 Enter 或点击 "Send" 按钮发送

3. **查看输出**:
    - 所有消息会显示在 Tool Window 的消息列表中
    - 不同类型的消息有不同的视觉样式
    - 自动滚动到最新消息

### 上线准备

1. **禁用日志**:
    - 在 `LogUtil.kt` 中设置 `DEBUG = false`

2. **构建发布版本**:
   ```bash
   ./gradlew buildPlugin
   ```

## 未来改进方向

### 短期改进

1. **消息格式化增强**:
    - 支持代码块语法高亮
    - 支持 Markdown 渲染
    - 支持文件路径链接（点击打开文件）

2. **命令执行展示**:
    - 实时显示命令执行进度
    - 支持查看完整输出（当前可能截断）
    - 支持复制命令和输出

3. **会话管理**:
    - 支持会话历史记录
    - 支持会话恢复
    - 支持多个会话切换

### 长期规划

1. **Claude Code 集成**:
    - 类似 Codex 的方式集成 Claude Code
    - 支持本地和远程两种模式

2. **配置界面**:
    - 允许用户配置 Codex 参数（模型、权限模式等）
    - 支持自定义项目上下文模板

3. **性能优化**:
    - 消息列表虚拟化（处理大量消息）
    - 异步消息处理
    - 消息缓存和持久化

## 总结

本次改进完成了从模板项目到 MVP 的完整实现，包括：

✅ 项目重命名和配置更新  
✅ 技术栈转换为 Kotlin  
✅ Codex MCP 客户端完整实现  
✅ Tool Window 和消息展示  
✅ 项目信息传递  
✅ 参考 happy 的 UI 展示方式  
✅ 统一的日志系统

插件现在可以：

- 启动 Codex MCP 会话
- 接收和显示 Codex 的各种消息类型
- 以友好的方式展示命令执行和结果
- 传递项目上下文给 Codex

为后续的功能扩展打下了良好的基础。

