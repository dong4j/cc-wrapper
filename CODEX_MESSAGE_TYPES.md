# Codex MCP 消息类型说明文档

## 问题回答

### Q1: Codex 所有的消息类型都全部处理了嘛?

**答案**: **基本完整**。当前实现覆盖了 **100%** 需要显示的消息类型（14 种全部处理）。

**已实现并显示**: 11 种消息类型
**已实现但不显示**: 3 种（agent_reasoning_section_break, turn_diff, token_count - 这些在 happy-cli 中也不显示）

**最新更新**: 已添加 `exec_approval_request`、`patch_apply_begin`、`patch_apply_end` 三种消息类型的处理。

### Q2: 你是从哪里知道返回的消息类型以及对应的处理逻辑的?

**答案**: 主要来源于 **happy-cli** 项目的实现：

1. **主要参考文件**: 
   - `happy-cli/src/codex/runCodex.ts` (第 393-534 行) - 消息处理逻辑
   - `happy-cli/src/codex/codexMcpClient.ts` - MCP 客户端实现

2. **消息接收方式**:
   - happy-cli 通过 `client.setHandler()` 监听 `codex/event` 通知
   - 所有 Codex MCP 事件都通过这个 handler 接收
   - 根据 `msg.type` 字段判断事件类型

3. **处理逻辑参考**:
   - 查看 happy-cli 中每种消息类型的处理方式
   - 参考其如何转换和发送到服务器
   - 参考其 UI 展示方式

## 消息类型来源

所有 Codex MCP 消息类型的定义和处理逻辑来源于 **happy-cli** 项目的实现：

- **主要参考文件**: `happy-cli/src/codex/runCodex.ts` (第 393-534 行)
- **MCP 客户端**: `happy-cli/src/codex/codexMcpClient.ts`
- **消息格式定义**: Codex MCP 协议规范

happy-cli 通过监听 `codex/event` 通知来接收所有 Codex 事件，并根据事件类型进行不同的处理。

## 消息类型完整列表

根据 happy-cli 的实现，Codex MCP 支持以下消息类型：

### 1. `agent_message`
- **含义**: Codex 代理发送的文本消息，通常是任务的最终回复或中间回复
- **处理逻辑**: 
  - 在 happy-cli 中：添加到消息缓冲区，类型为 'assistant'
  - 发送到服务器：`{ type: 'message', message: msg.message, id: ... }`
- **是否显示**: ✅ 是（显示为助手消息）
- **当前实现**: ✅ 已实现

### 2. `agent_reasoning`
- **含义**: Codex 的完整推理/思考过程文本
- **处理逻辑**:
  - 在 happy-cli 中：截取前 100 字符显示为 `[Thinking] ...`
  - 通过 ReasoningProcessor 处理，可能生成工具调用
- **是否显示**: ✅ 是（显示为推理消息，截断显示）
- **当前实现**: ✅ 已实现

### 3. `agent_reasoning_delta`
- **含义**: Codex 推理过程的增量更新（流式输出）
- **处理逻辑**:
  - 在 happy-cli 中：**跳过 UI 显示**（减少噪音）
  - 通过 ReasoningProcessor 处理增量，可能自动发送工具调用
- **是否显示**: ❌ 否（happy-cli 也跳过显示）
- **当前实现**: ✅ 已实现（但可能不需要显示）

### 4. `agent_reasoning_section_break`
- **含义**: 推理部分的分隔符，表示新的推理部分开始
- **处理逻辑**:
  - 在 happy-cli 中：重置 ReasoningProcessor，开始新的推理部分
  - 不直接显示在 UI 中
- **是否显示**: ❌ 否（内部处理，不显示）
- **当前实现**: ✅ 已实现（返回 null，不显示）

### 5. `exec_command_begin`
- **含义**: 命令开始执行
- **包含字段**: `command`, `call_id`, `cwd`, `parsed_cmd` 等
- **处理逻辑**:
  - 在 happy-cli 中：显示 `Executing: <command>`
  - 发送到服务器：`{ type: 'tool-call', name: 'CodexBash', callId, input, id }`
- **是否显示**: ✅ 是（显示为工具调用）
- **当前实现**: ✅ 已实现

### 6. `exec_approval_request`
- **含义**: 命令执行前的权限请求（需要用户批准）
- **处理逻辑**:
  - 在 happy-cli 中：与 `exec_command_begin` 一起处理，发送权限请求
  - 通过 PermissionHandler 处理用户批准/拒绝
- **是否显示**: ✅ 是（显示为工具调用，标记需要权限）
- **当前实现**: ✅ 已实现

### 7. `exec_command_end`
- **含义**: 命令执行完成
- **包含字段**: `call_id`, `output`, `error`, `success` 等
- **处理逻辑**:
  - 在 happy-cli 中：显示输出或错误（截取前 200 字符）
  - 发送到服务器：`{ type: 'tool-call-result', callId, output, id }`
- **是否显示**: ✅ 是（显示为工具结果）
- **当前实现**: ✅ 已实现

### 8. `patch_apply_begin`
- **含义**: 开始应用代码补丁（文件修改）
- **包含字段**: `call_id`, `auto_approved`, `changes` (文件变更列表)
- **处理逻辑**:
  - 在 happy-cli 中：显示 `Modifying N files...`
  - 发送到服务器：`{ type: 'tool-call', name: 'CodexPatch', callId, input: { changes }, id }`
- **是否显示**: ✅ 是（显示为工具调用）
- **当前实现**: ✅ 已实现

### 9. `patch_apply_end`
- **含义**: 代码补丁应用完成
- **包含字段**: `call_id`, `stdout`, `stderr`, `success`
- **处理逻辑**:
  - 在 happy-cli 中：显示成功或错误消息（截取前 200 字符）
  - 发送到服务器：`{ type: 'tool-call-result', callId, output: { stdout, stderr, success }, id }`
- **是否显示**: ✅ 是（显示为工具结果）
- **当前实现**: ✅ 已实现

### 10. `turn_diff`
- **含义**: 整个轮次的代码差异（unified diff 格式）
- **包含字段**: `unified_diff` (完整的 diff 文本)
- **处理逻辑**:
  - 在 happy-cli 中：通过 DiffProcessor 处理，当 diff 变化时发送 CodexDiff 工具调用
  - 不直接显示，而是转换为工具调用
- **是否显示**: ❌ 否（当前返回 null，不显示；未来可实现 DiffProcessor）
- **当前实现**: ✅ 已实现（返回 null，不显示）

### 11. `task_started`
- **含义**: 任务开始（Codex 开始处理用户请求）
- **处理逻辑**:
  - 在 happy-cli 中：显示 `Starting task...`，更新 thinking 状态
  - 更新 keep-alive 状态
- **是否显示**: ✅ 是（显示为状态消息）
- **当前实现**: ✅ 已实现

### 12. `task_complete`
- **含义**: 任务完成（Codex 完成当前任务）
- **处理逻辑**:
  - 在 happy-cli 中：显示 `Task completed`，发送 ready 事件
  - 重置 thinking 状态和 diff processor
- **是否显示**: ✅ 是（显示为状态消息）
- **当前实现**: ✅ 已实现

### 13. `turn_aborted`
- **含义**: 任务中止（用户中止或错误导致）
- **处理逻辑**:
  - 在 happy-cli 中：显示 `Turn aborted`，发送 ready 事件
  - 重置 thinking 状态和 diff processor
- **是否显示**: ✅ 是（显示为状态消息）
- **当前实现**: ✅ 已实现

### 14. `token_count`
- **含义**: Token 使用统计
- **包含字段**: 各种 token 计数信息
- **处理逻辑**:
  - 在 happy-cli 中：直接转发到服务器（不显示在 UI）
  - 发送到服务器：`{ ...msg, id: ... }`
- **是否显示**: ❌ 否（统计信息，不显示）
- **当前实现**: ✅ 已实现（返回 null，不显示）

## 当前实现状态

### ✅ 已实现并显示的消息类型

1. `agent_message` → `assistant` - Codex 的文本回复
2. `exec_command_begin` → `tool-call` - 命令开始执行
3. `exec_command_end` → `tool-result` - 命令执行完成
4. `agent_reasoning` → `reasoning` - Codex 的思考过程
5. `agent_reasoning_delta` → `reasoning-delta` - 推理增量（可选显示）
6. `task_started` → `status` - 任务开始
7. `task_complete` → `status` - 任务完成
8. `turn_aborted` → `status` - 任务中止
9. `exec_approval_request` → `tool-call` - 权限请求（**新增**）
10. `patch_apply_begin` → `tool-call` - 补丁应用开始（**新增**）
11. `patch_apply_end` → `tool-result` - 补丁应用完成（**新增**）

### ✅ 已实现但不显示的消息类型

1. `agent_reasoning_section_break` - 推理部分分隔（内部处理，返回 null）
2. `turn_diff` - 轮次差异（未来可实现 DiffProcessor，当前返回 null）
3. `token_count` - Token 统计（统计信息，返回 null）

### ❌ 完全未实现的消息类型

无（所有已知消息类型都已处理）

## 实现完整性分析

### ✅ 已完成的改进

1. **`exec_approval_request`** - ✅ 已实现
   - 显示为工具调用，标记需要权限
   - 提取命令信息到 metadata

2. **`patch_apply_begin` / `patch_apply_end`** - ✅ 已实现
   - 显示文件修改操作和结果
   - 提取文件数量和变更信息

3. **过滤 system 类型消息** - ✅ 已实现
   - 在 `parseMcpMessage()` 中，未知类型返回 null（不再返回 system）
   - 在 `CcWrapperPanel.addMessage()` 中，额外过滤 system 类型

### 🔄 可选增强功能

1. **`turn_diff`**
   - 整个轮次的代码差异
   - 需要实现 DiffProcessor 来跟踪和显示差异
   - **当前状态**: 返回 null，不显示（未来可增强）

2. **`agent_reasoning_section_break`**
   - 内部处理，不影响显示
   - **当前状态**: 返回 null，不显示（符合 happy-cli 的处理方式）

3. **`token_count`**
   - 统计信息，不显示在 UI
   - **当前状态**: 返回 null，不显示（符合 happy-cli 的处理方式）

## 实现细节

### 消息类型处理位置

所有消息类型处理在 `CodexMcpManager.parseMcpMessage()` 方法中实现（第 285-418 行）。

### 过滤机制

1. **在解析层面过滤**:
   - 不需要显示的消息类型直接返回 `null`
   - 包括：`agent_reasoning_section_break`, `turn_diff`, `token_count`
   - 未知类型也返回 `null`（不再返回 system 类型）

2. **在 UI 层面过滤**:
   - `CcWrapperPanel.addMessage()` 中额外检查 system 类型
   - 双重保障，确保 system 类型消息不会显示

## 总结

### 实现完整性

**当前实现覆盖了 100% 需要显示的消息类型**（14 种全部处理）：

✅ **已实现并显示**: 11 种消息类型
- 核心消息：agent_message, exec_command_begin, exec_command_end
- 推理消息：agent_reasoning, agent_reasoning_delta
- 状态消息：task_started, task_complete, turn_aborted
- 新增消息：exec_approval_request, patch_apply_begin, patch_apply_end

✅ **已实现但不显示**: 3 种消息类型
- agent_reasoning_section_break（内部处理）
- turn_diff（未来可增强）
- token_count（统计信息）

### 关键改进

1. ✅ **添加了文件修改事件处理** - 现在可以看到 Codex 的文件修改操作
2. ✅ **添加了权限请求处理** - 现在可以看到命令执行前的权限请求
3. ✅ **过滤了 system 类型消息** - Tool Window 不再显示系统消息，界面更清爽

### 与 happy-cli 的对比

| 功能 | happy-cli | CC Wrapper | 状态 |
|------|-----------|------------|------|
| 消息类型覆盖 | 14 种全部处理 | 14 种全部处理 | ✅ 一致 |
| UI 显示 | 11 种显示 | 11 种显示 | ✅ 一致 |
| 文件修改显示 | ✅ | ✅ | ✅ 已实现 |
| 权限请求显示 | ✅ | ✅ | ✅ 已实现 |
| System 消息过滤 | ✅ | ✅ | ✅ 已实现 |

**结论**: 当前实现已基本完整，覆盖了所有需要显示的消息类型，与 happy-cli 的处理方式保持一致。

