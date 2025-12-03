# MCP 架构说明文档

## 问题回答

### Q: 为什么需要一个 MCP 服务？是从 MCP 服务中获取消息的吗？

**答案**: **是的**。MCP 服务是 Codex 提供的标准接口，所有消息都通过 MCP 协议从 MCP 服务获取。

## MCP 是什么？

**MCP (Model Context Protocol)** 是一个标准化的协议，用于客户端和 AI 模型服务之间的通信。它定义了：
- 消息格式（JSON-RPC 2.0）
- 通信方式（STDIO、HTTP 等）
- 工具调用接口
- 事件通知机制

## 架构图

```
┌─────────────────────────────────────────────────────────────┐
│                    CC Wrapper (IDEA Plugin)                  │
│                    (MCP 客户端)                                │
│                                                               │
│  ┌─────────────────────────────────────────────────────┐    │
│  │         CodexMcpManager                             │    │
│  │  - 启动 codex mcp-server 进程                        │    │
│  │  - 通过 STDIO 通信                                    │    │
│  │  - 解析 JSON-RPC 消息                                 │    │
│  │  - 处理 codex/event 通知                             │    │
│  └─────────────────────────────────────────────────────┘    │
│                          │                                   │
│                          │ STDIO (标准输入输出)                │
│                          │ JSON-RPC 2.0 协议                  │
│                          ▼                                   │
└─────────────────────────────────────────────────────────────┘
                          │
                          │ ProcessBuilder
                          │ "codex mcp-server"
                          ▼
┌─────────────────────────────────────────────────────────────┐
│              Codex MCP Server (codex mcp-server)             │
│              (MCP 服务器)                                     │
│                                                               │
│  - 由 Codex CLI 提供                                          │
│  - 运行在独立进程中                                            │
│  - 通过 STDIO 接收请求和发送事件                               │
│  - 处理 Codex 工具调用（执行命令、修改文件等）                   │
│  - 发送 codex/event 通知（agent_message, exec_command 等）    │
└─────────────────────────────────────────────────────────────┘
```

## 为什么需要 MCP 服务？

### 1. **标准化接口**

MCP 提供了一个标准化的协议，让不同的客户端（如 CC Wrapper、happy-cli）都能以相同的方式与 Codex 交互。

### 2. **进程隔离**

- Codex MCP 服务器运行在**独立进程**中
- 即使 Codex 崩溃，也不会影响 IDEA 插件
- 更好的资源管理和错误隔离

### 3. **事件驱动架构**

MCP 服务器通过 `codex/event` 通知主动推送事件：
- `agent_message` - Codex 的回复
- `exec_command_begin/end` - 命令执行
- `patch_apply_begin/end` - 文件修改
- `agent_reasoning` - 推理过程
- 等等...

### 4. **工具调用能力**

MCP 协议支持工具调用（Tool Calls），让 Codex 可以：
- 执行命令（`exec_command`）
- 修改文件（`patch_apply`）
- 读取文件内容
- 等等...

## 消息获取流程

### 1. 启动 MCP 服务器

```kotlin
// CodexMcpManager.kt (第 78 行)
val processBuilder = ProcessBuilder("codex", "mcp-server")
process = processBuilder.start()
```

启动 `codex mcp-server` 进程，通过 STDIO（标准输入输出）通信。

### 2. 建立连接

```kotlin
// CodexMcpManager.kt (第 89-92 行)
val inputStream = process!!.inputStream  // 从服务器读取
val outputStream = process!!.outputStream // 向服务器写入
writer = OutputStreamWriter(outputStream, Charsets.UTF_8)
```

建立双向通信通道：
- **输入流 (inputStream)**: 从 MCP 服务器读取消息
- **输出流 (outputStream)**: 向 MCP 服务器发送请求

### 3. 监听消息

```kotlin
// CodexMcpManager.kt (第 95-115 行)
readerThread = Thread {
    val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
    var line: String?
    
    while (reader.readLine().also { line = it } != null) {
        // 解析 JSON-RPC 消息
        val message = parseMcpMessage(line!!)
        if (message != null) {
            notifyMessage(message)  // 通知 UI
        }
    }
}
```

在独立线程中持续读取 MCP 服务器发送的消息。

### 4. 解析消息

```kotlin
// CodexMcpManager.kt (第 277-279 行)
val method = json.get("method")?.asString
if (method == "codex/event") {
    val params = json.getAsJsonObject("params")
    val msg = params.getAsJsonObject("msg")
    val type = msg.get("type")?.asString  // 获取事件类型
    // ... 处理不同类型的事件
}
```

解析 JSON-RPC 2.0 格式的消息，提取 `codex/event` 通知。

### 5. 发送请求

```kotlin
// CodexMcpManager.kt (第 200-220 行)
val request = JsonObject().apply {
    addProperty("jsonrpc", "2.0")
    addProperty("id", System.currentTimeMillis())
    addProperty("method", "tools/call")
    add("params", JsonObject().apply {
        addProperty("name", "codex")
        add("arguments", JsonObject().apply {
            addProperty("prompt", enhancedPrompt)
            // ...
        })
    })
}
writer.write(gson.toJson(request))
writer.write("\n")
writer.flush()
```

向 MCP 服务器发送工具调用请求（启动 Codex 会话）。

## 消息格式

### 从 MCP 服务器接收的消息（通知）

```json
{
  "jsonrpc": "2.0",
  "method": "codex/event",
  "params": {
    "msg": {
      "type": "agent_message",
      "message": "Hello, I'm Codex!",
      "call_id": "12345",
      // ... 其他字段
    }
  }
}
```

### 向 MCP 服务器发送的请求

```json
{
  "jsonrpc": "2.0",
  "id": 1234567890,
  "method": "tools/call",
  "params": {
    "name": "codex",
    "arguments": {
      "prompt": "帮我写一个函数",
      "sandbox": "workspace-write",
      "cwd": "/path/to/project"
    }
  }
}
```

## 与 happy-cli 的对比

### happy-cli 的实现

```typescript
// happy-cli/src/codex/codexMcpClient.ts
import { Client } from '@modelcontextprotocol/sdk/client/index.js';
import { StdioClientTransport } from '@modelcontextprotocol/sdk/client/stdio.js';

this.transport = new StdioClientTransport({
    command: 'codex',
    args: ['mcp-server'],
    env: process.env
});

await this.client.connect(this.transport);

// 监听 codex/event 通知
this.client.setNotificationHandler(..., (data) => {
    const msg = data.params.msg;
    this.handler?.(msg);  // 处理事件
});
```

**特点**:
- 使用官方 MCP SDK (`@modelcontextprotocol/sdk`)
- 自动处理 JSON-RPC 协议
- 更高级的抽象

### CC Wrapper 的实现

```kotlin
// template-without-ai/src/main/kotlin/.../CodexMcpManager.kt
val processBuilder = ProcessBuilder("codex", "mcp-server")
process = processBuilder.start()

val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
while (reader.readLine() != null) {
    val message = parseMcpMessage(line)  // 手动解析 JSON-RPC
    // ...
}
```

**特点**:
- 手动实现 JSON-RPC 解析（因为 Kotlin 没有官方 MCP SDK）
- 更底层，但更灵活
- 功能与 happy-cli 一致

## 关键点总结

1. **MCP 服务器是必需的**
   - Codex 只提供 MCP 接口，不提供其他直接 API
   - 所有交互必须通过 MCP 协议

2. **消息来源**
   - ✅ **是的**，所有消息都从 MCP 服务器获取
   - MCP 服务器通过 `codex/event` 通知主动推送事件
   - 客户端通过 STDIO 读取这些事件

3. **通信方式**
   - 使用 **STDIO**（标准输入输出）
   - 协议：**JSON-RPC 2.0**
   - 格式：每行一个 JSON 对象

4. **优势**
   - 标准化协议
   - 进程隔离
   - 事件驱动
   - 工具调用支持

## 参考资料

- **MCP 协议规范**: [Model Context Protocol](https://modelcontextprotocol.io/)
- **Codex CLI**: `codex mcp-server` 命令
- **happy-cli 实现**: `happy-cli/src/codex/codexMcpClient.ts`
- **CC Wrapper 实现**: `template-without-ai/src/main/kotlin/.../CodexMcpManager.kt`

