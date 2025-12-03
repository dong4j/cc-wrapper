package dev.dong4j.ccwrapper.ui.components

import dev.dong4j.ccwrapper.model.CodexMessage
import java.awt.Color
import java.awt.Font
import javax.swing.*
import javax.swing.border.EmptyBorder

/**
 * 渲染 Codex 消息的组件
 * 参考 happy 的展示方式，提供更好的可视化效果
 */
object CodexMessageRenderer {
    
    /**
     * 根据消息类型创建对应的 UI 组件
     */
    fun createMessageComponent(message: CodexMessage): JComponent {
        return when (message.type) {
            "user" -> createUserMessageComponent(message)
            "assistant" -> createAssistantMessageComponent(message)
            "tool-call" -> createToolCallComponent(message)
            "tool-result" -> createToolResultComponent(message)
            "reasoning" -> createReasoningComponent(message)
            "status" -> createStatusComponent(message)
            "error" -> createErrorComponent(message)
            else -> createDefaultComponent(message)
        }
    }
    
    /**
     * 用户消息组件
     */
    private fun createUserMessageComponent(message: CodexMessage): JComponent {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        panel.border = EmptyBorder(8, 12, 8, 12)
        panel.background = Color(0xF0F0F0)
        
        val label = JLabel("<html><b>You:</b> ${escapeHtml(message.content)}</html>")
        label.font = Font(Font.SANS_SERIF, Font.PLAIN, 13)
        panel.add(label)
        
        return panel
    }
    
    /**
     * 助手消息组件
     */
    private fun createAssistantMessageComponent(message: CodexMessage): JComponent {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        panel.border = EmptyBorder(8, 12, 8, 12)
        panel.background = Color.WHITE
        
        val label = JLabel("<html><b>Codex:</b> ${escapeHtml(message.content)}</html>")
        label.font = Font(Font.SANS_SERIF, Font.PLAIN, 13)
        panel.add(label)
        
        return panel
    }
    
    /**
     * 工具调用组件（参考 CommandView）
     */
    private fun createToolCallComponent(message: CodexMessage): JComponent {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        panel.border = EmptyBorder(8, 12, 8, 12)
        panel.background = Color(0xF8F9FA)
        
        // 解析命令信息
        val command = extractCommand(message)
        val icon = JLabel("⚙️")
        icon.font = Font(Font.SANS_SERIF, Font.PLAIN, 14)
        
        val commandLabel = JLabel("<html><b>Executing:</b> <code style='font-family: monospace; background: #E9ECEF; padding: 2px 4px; border-radius: 3px;'>${escapeHtml(command)}</code></html>")
        commandLabel.font = Font(Font.SANS_SERIF, Font.PLAIN, 12)
        
        val row = JPanel()
        row.layout = BoxLayout(row, BoxLayout.X_AXIS)
        row.add(icon)
        row.add(Box.createHorizontalStrut(8))
        row.add(commandLabel)
        row.background = panel.background
        
        panel.add(row)
        
        return panel
    }
    
    /**
     * 工具结果组件（参考 CommandView 的 stdout/stderr 显示）
     */
    private fun createToolResultComponent(message: CodexMessage): JComponent {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        panel.border = EmptyBorder(8, 12, 8, 12)
        panel.background = Color(0xF8F9FA)
        
        // 解析输出信息
        val output = extractOutput(message)
        val error = message.metadata?.get("error") as? String
        val success = message.metadata?.get("success") as? Boolean ?: (error == null)
        val isError = !success || error != null
        
        val icon = JLabel(if (isError) "❌" else "✅")
        icon.font = Font(Font.SANS_SERIF, Font.PLAIN, 14)
        
        val outputLabel = JLabel("<html><pre style='font-family: monospace; font-size: 12px; margin: 0; white-space: pre-wrap; word-wrap: break-word; color: ${if (isError) "#DC3545" else "#28A745"};'>${escapeHtml(output)}</pre></html>")
        
        val row = JPanel()
        row.layout = BoxLayout(row, BoxLayout.X_AXIS)
        row.add(icon)
        row.add(Box.createHorizontalStrut(8))
        row.add(outputLabel)
        row.background = panel.background
        
        panel.add(row)
        
        return panel
    }
    
    /**
     * 推理/思考组件
     */
    private fun createReasoningComponent(message: CodexMessage): JComponent {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        panel.border = EmptyBorder(8, 12, 8, 12)
        panel.background = Color(0xFFF3CD)
        
        val label = JLabel("<html><b>💭 Thinking:</b> ${escapeHtml(message.content)}</html>")
        label.font = Font(Font.SANS_SERIF, Font.ITALIC, 12)
        panel.add(label)
        
        return panel
    }
    
    /**
     * 状态消息组件
     */
    private fun createStatusComponent(message: CodexMessage): JComponent {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        panel.border = EmptyBorder(4, 12, 4, 12)
        panel.background = Color(0xE7F3FF)
        
        val label = JLabel("<html><b>ℹ️</b> ${escapeHtml(message.content)}</html>")
        label.font = Font(Font.SANS_SERIF, Font.PLAIN, 11)
        panel.add(label)
        
        return panel
    }
    
    /**
     * 错误消息组件
     */
    private fun createErrorComponent(message: CodexMessage): JComponent {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        panel.border = EmptyBorder(8, 12, 8, 12)
        panel.background = Color(0xFFE6E6)
        
        val label = JLabel("<html><b>❌ Error:</b> <span style='color: #DC3545;'>${escapeHtml(message.content)}</span></html>")
        label.font = Font(Font.SANS_SERIF, Font.PLAIN, 12)
        panel.add(label)
        
        return panel
    }
    
    /**
     * 默认消息组件
     */
    private fun createDefaultComponent(message: CodexMessage): JComponent {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        panel.border = EmptyBorder(8, 12, 8, 12)
        panel.background = Color.WHITE
        
        val label = JLabel("<html><b>[${message.type}]</b> ${escapeHtml(message.content)}</html>")
        label.font = Font(Font.MONOSPACED, Font.PLAIN, 12)
        panel.add(label)
        
        return panel
    }
    
    /**
     * 从消息中提取命令
     */
    private fun extractCommand(message: CodexMessage): String {
        // 尝试从 metadata 中提取命令
        val command = message.metadata?.get("command") as? String
        if (command != null) return command
        
        // 尝试从 content 中解析
        if (message.content.startsWith("Executing: ")) {
            return message.content.substring(11)
        }
        
        return message.content
    }
    
    /**
     * 从消息中提取输出
     */
    private fun extractOutput(message: CodexMessage): String {
        // 优先从 metadata 中提取输出
        val output = message.metadata?.get("output") as? String
        if (output != null && output.isNotBlank()) return output
        
        val error = message.metadata?.get("error") as? String
        if (error != null && error.isNotBlank()) return error
        
        // 如果 content 包含 "Result:" 前缀，提取实际内容
        if (message.content.startsWith("Result: ")) {
            return message.content.substring(8)
        }
        
        return message.content
    }
    
    /**
     * HTML 转义
     */
    private fun escapeHtml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }
}

