package dev.dong4j.ccwrapper.ui

import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBScrollPane
import dev.dong4j.ccwrapper.mcp.CodexMcpManager
import dev.dong4j.ccwrapper.model.CodexMessage
import dev.dong4j.ccwrapper.ui.components.CodexMessageRenderer
import java.awt.BorderLayout
import javax.swing.*
import javax.swing.border.EmptyBorder

/**
 * Main panel for CC Wrapper Tool Window
 * Displays Codex messages and allows user interaction
 * 参考 happy 的展示方式，提供更好的可视化效果
 */
class CcWrapperPanel(private val project: Project) : JPanel(BorderLayout()) {
    
    private val messagePanel: JPanel
    private val inputField: JTextField
    private val sendButton: JButton
    private val statusLabel: JLabel
    
    private val mcpManager: CodexMcpManager = CodexMcpManager(project)
    
    init {
        // Create message panel with vertical box layout
        messagePanel = JPanel()
        messagePanel.layout = BoxLayout(messagePanel, BoxLayout.Y_AXIS)
        messagePanel.border = EmptyBorder(8, 8, 8, 8)
        
        val scrollPane = JBScrollPane(messagePanel)
        scrollPane.verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
        scrollPane.horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
        
        // Create input panel
        val inputPanel = JPanel(BorderLayout())
        inputField = JTextField()
        sendButton = JButton("Send")
        statusLabel = JLabel("Ready")
        
        inputPanel.add(JLabel("Input: "), BorderLayout.WEST)
        inputPanel.add(inputField, BorderLayout.CENTER)
        inputPanel.add(sendButton, BorderLayout.EAST)
        inputPanel.add(statusLabel, BorderLayout.SOUTH)
        
        // Layout
        add(scrollPane, BorderLayout.CENTER)
        add(inputPanel, BorderLayout.SOUTH)
        
        // Setup event handlers
        sendButton.addActionListener { sendMessage() }
        inputField.addActionListener { sendMessage() }
        
        // Setup MCP manager callbacks
        mcpManager.onMessage { message ->
            SwingUtilities.invokeLater {
                addMessage(message)
            }
        }
        
        mcpManager.onStatusChange { status ->
            SwingUtilities.invokeLater {
                statusLabel.text = status
            }
        }
    }
    
    private fun sendMessage() {
        val text = inputField.text.trim()
        if (text.isBlank()) return
        
        // Add user message to table
        addMessage(CodexMessage(
            type = "user",
            content = text,
            timestamp = System.currentTimeMillis()
        ))
        
        // Send to Codex
        mcpManager.sendMessage(text)
        
        // Clear input
        inputField.text = ""
    }
    
    private fun addMessage(message: CodexMessage) {
        // 过滤 system 类型消息（不显示）
        if (message.type == "system") {
            return
        }
        
        // 使用 CodexMessageRenderer 创建消息组件
        val messageComponent = CodexMessageRenderer.createMessageComponent(message)
        
        // 添加时间戳（可选）
        val timeLabel = JLabel(java.text.SimpleDateFormat("HH:mm:ss").format(java.util.Date(message.timestamp)))
        timeLabel.font = timeLabel.font.deriveFont(10f)
        timeLabel.foreground = java.awt.Color.GRAY
        timeLabel.border = EmptyBorder(0, 12, 2, 12)
        
        // 创建容器
        val container = JPanel()
        container.layout = BoxLayout(container, BoxLayout.Y_AXIS)
        container.add(timeLabel)
        container.add(messageComponent)
        container.add(Box.createVerticalStrut(4)) // 消息间距
        
        messagePanel.add(container)
        messagePanel.revalidate()
        messagePanel.repaint()
        
        // Auto scroll to bottom
        SwingUtilities.invokeLater {
            val scrollPane = messagePanel.parent as? JScrollPane
            scrollPane?.verticalScrollBar?.value = scrollPane?.verticalScrollBar?.maximum ?: 0
        }
    }
}

