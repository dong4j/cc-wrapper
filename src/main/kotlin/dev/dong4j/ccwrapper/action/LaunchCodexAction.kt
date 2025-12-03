package dev.dong4j.ccwrapper.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import dev.dong4j.ccwrapper.mcp.CodexMcpManager

/**
 * Action to launch Codex MCP connection
 */
class LaunchCodexAction : AnAction("Launch Codex", "Start Codex MCP session", null) {
    
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        
        // Show tool window
        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("CC Wrapper")
        toolWindow?.show()
        
        // Connect to Codex
        val mcpManager = project.getService(CodexMcpManager::class.java)
        mcpManager.connect()
    }
    
    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = e.project != null
    }
    
    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}

