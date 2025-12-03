# CC Wrapper

一个 IntelliJ IDEA 插件，用于在 IDE 中集成 Codex CLI，通过 MCP (Model Context Protocol) 协议与 Codex 通信。

## 功能特性

- 🚀 **一键启动 Codex**：通过工具栏按钮快速启动 Codex MCP 会话
- 💬 **实时消息展示**：在 Tool Window 中实时显示 Codex 的消息、命令执行结果等
- 🔄 **会话管理**：自动管理 Codex 会话的生命周期
- 📊 **消息分类**：支持用户消息、助手回复、工具调用、执行结果等多种消息类型

## 前置要求

- IntelliJ IDEA 2022.3 或更高版本
- OpenAI Codex CLI 已安装并在系统 PATH 中可用
  - 安装方法：参考 [OpenAI Codex CLI 文档](https://github.com/openai/codex)

## 安装

1. 克隆或下载此项目
2. 使用 IntelliJ IDEA 打开项目
3. 运行 `./gradlew buildPlugin` 构建插件
4. 运行 `./gradlew runIde` 在沙盒环境中测试插件

## 使用方法

1. **启动 Codex**：
   - 点击工具栏上的 "Launch Codex" 按钮
   - 或通过 `Tools` → `Launch Codex` 菜单

2. **发送消息**：
   - 在 Tool Window 底部的输入框中输入消息
   - 按 Enter 或点击 "Send" 按钮发送

3. **查看输出**：
   - 所有消息会显示在 Tool Window 的表格中
   - 包括用户消息、Codex 回复、命令执行结果等

## 技术架构

### 核心组件

- **CcWrapperToolWindowFactory**：创建 Tool Window
- **CcWrapperPanel**：主界面面板，显示消息列表和输入框
- **CodexMcpManager**：管理 Codex MCP 连接，处理消息收发
- **CodexMessage**：消息数据模型

### MCP 协议实现

插件通过 stdio 与 Codex MCP 服务器通信，使用 JSON-RPC 2.0 协议：

- **连接**：启动 `codex mcp-server` 进程
- **启动会话**：调用 `codex` 工具开始新会话
- **继续会话**：调用 `codex-reply` 工具继续现有会话
- **事件监听**：监听 `codex/event` 通知获取实时更新

## 开发

### 项目结构

```
src/main/kotlin/dev/dong4j/ccwrapper/
├── action/          # Actions (工具栏按钮等)
├── mcp/             # MCP 客户端实现
├── model/           # 数据模型
└── ui/              # UI 组件 (Tool Window, Panel 等)
```

### 构建

```bash
# 构建插件
./gradlew buildPlugin

# 运行测试
./gradlew test

# 在沙盒中运行
./gradlew runIde
```

## 参考

- [happy-cli](https://github.com/slopus/happy-cli)：参考了其 Codex MCP 客户端实现
- [codex-launcher](https://github.com/x0x0b/codex-launcher)：参考了其终端集成方式

## 许可证

MIT License

## 作者

dong4j <dong4j@gmail.com>

