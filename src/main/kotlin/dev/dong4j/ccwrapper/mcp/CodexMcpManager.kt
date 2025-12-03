package dev.dong4j.ccwrapper.mcp

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import dev.dong4j.ccwrapper.model.CodexMessage
import dev.dong4j.ccwrapper.util.LogUtil
import dev.dong4j.ccwrapper.util.ProjectInfoUtil
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Manages Codex MCP connection
 * Based on happy-cli's CodexMcpClient implementation
 */
@Service(Service.Level.PROJECT)
class CodexMcpManager(private val project: Project) {

    // 使用 LogUtil 替代 logger

    private var process: Process? = null
    private var readerThread: Thread? = null
    private var writer: OutputStreamWriter? = null
    private val isConnected = AtomicBoolean(false)

    private val gson = Gson()
    private val jsonParser = JsonParser()

    private val messageCallbacks = mutableListOf<(CodexMessage) -> Unit>()
    private val statusCallbacks = mutableListOf<(String) -> Unit>()

    /**
     * Register callback for messages
     */
    fun onMessage(callback: (CodexMessage) -> Unit) {
        messageCallbacks.add(callback)
    }

    /**
     * Register callback for status changes
     */
    fun onStatusChange(callback: (String) -> Unit) {
        statusCallbacks.add(callback)
    }

    private fun notifyMessage(message: CodexMessage) {
        messageCallbacks.forEach { it(message) }
    }

    private fun notifyStatus(status: String) {
        statusCallbacks.forEach { it(status) }
    }

    /**
     * Connect to Codex MCP server
     */
    @Suppress("D")
    fun connect(): CompletableFuture<Boolean> {
        val future = CompletableFuture<Boolean>()

        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                if (isConnected.get()) {
                    future.complete(true)
                    return@executeOnPooledThread
                }

                notifyStatus("Connecting to Codex...")
                LogUtil.info("Starting Codex MCP server")

                // Start codex mcp-server process
                val processBuilder = ProcessBuilder("codex", "mcp-server")
                processBuilder.redirectErrorStream(true)

                // Set working directory to project root
                val projectBasePath = ProjectInfoUtil.getProjectBasePath(project)
                if (projectBasePath != null) {
                    processBuilder.directory(java.io.File(projectBasePath))
                    LogUtil.info("Setting Codex working directory to: $projectBasePath")
                }

                process = processBuilder.start()
                val inputStream = process!!.inputStream
                val outputStream = process!!.outputStream

                writer = OutputStreamWriter(outputStream, Charsets.UTF_8)

                // Start reader thread
                readerThread = Thread {
                    try {
                        val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
                        var line: String?

                        while (reader.readLine().also { line = it } != null && !Thread.currentThread().isInterrupted) {
                            if (line.isNullOrBlank()) continue

                            try {
                                // Parse JSON-RPC message
                                val message = parseMcpMessage(line!!)
                                if (message != null) {
                                    ApplicationManager.getApplication().invokeLater {
                                        notifyMessage(message)
                                    }
                                }
                                // message 为 null 表示不需要显示的消息类型（如 system, token_count 等）
                            } catch (e: Exception) {
                                LogUtil.warn("Failed to parse MCP message: $line", e)
                            }
                        }
                    } catch (e: Exception) {
                        if (!Thread.currentThread().isInterrupted) {
                            LogUtil.error("Error reading from Codex MCP", e)
                            ApplicationManager.getApplication().invokeLater {
                                notifyStatus("Error: ${e.message}")
                                notifyMessage(CodexMessage("error", "Connection error: ${e.message}"))
                            }
                        }
                    }
                }
                readerThread!!.isDaemon = true
                readerThread!!.start()

                isConnected.set(true)
                notifyStatus("Connected to Codex")
                future.complete(true)

            } catch (e: Exception) {
                LogUtil.error("Failed to connect to Codex MCP", e)
                notifyStatus("Failed to connect: ${e.message}")
                future.completeExceptionally(e)
            }
        }

        return future
    }

    private var sessionId: String? = null
    private var conversationId: String? = null
    private var isSessionStarted = false

    /**
     * Send a message to Codex
     * If no session exists, starts a new one. Otherwise continues the existing session.
     */
    fun sendMessage(text: String) {
        if (!isConnected.get()) {
            connect().thenAccept { connected ->
                if (connected) {
                    if (!isSessionStarted) {
                        startSession(text)
                    } else {
                        continueSession(text)
                    }
                }
            }
            return
        }

        if (!isSessionStarted) {
            startSession(text)
        } else {
            continueSession(text)
        }
    }

    /**
     * Start a new Codex session
     */
    private fun startSession(prompt: String) {
        try {
            val writer = this.writer ?: return

            notifyStatus("Starting Codex session...")

            // Build enhanced prompt with project context
            val projectContext = ProjectInfoUtil.buildProjectContext(project)
            val enhancedPrompt = if (projectContext.isNotBlank()) {
                "$projectContext\n\n$prompt"
            } else {
                prompt
            }

            // Get project base path for cwd
            val projectBasePath = ProjectInfoUtil.getProjectBasePath(project)

            // Create JSON-RPC request for codex tool (startSession)
            val request = JsonObject().apply {
                addProperty("jsonrpc", "2.0")
                addProperty("id", System.currentTimeMillis())
                addProperty("method", "tools/call")
                add("params", JsonObject().apply {
                    addProperty("name", "codex")
                    add("arguments", JsonObject().apply {
                        addProperty("prompt", enhancedPrompt)
                        addProperty("sandbox", "workspace-write")
                        addProperty("approval-policy", "untrusted")

                        // Set working directory if available
                        if (projectBasePath != null) {
                            addProperty("cwd", projectBasePath)
                            LogUtil.info("Setting Codex cwd to: $projectBasePath")
                        }
                    })
                })
            }

            writer.write(gson.toJson(request))
            writer.write("\n")
            writer.flush()

            isSessionStarted = true
            LogUtil.info("Started Codex session with prompt: $prompt")

        } catch (e: Exception) {
            LogUtil.error("Failed to start Codex session", e)
            notifyStatus("Failed to start session: ${e.message}")
        }
    }

    /**
     * Continue an existing Codex session
     */
    private fun continueSession(prompt: String) {
        try {
            val writer = this.writer ?: return

            if (sessionId == null) {
                LogUtil.warn("No session ID available, starting new session")
                startSession(prompt)
                return
            }

            // Create JSON-RPC request for codex-reply tool
            val request = JsonObject().apply {
                addProperty("jsonrpc", "2.0")
                addProperty("id", System.currentTimeMillis())
                addProperty("method", "tools/call")
                add("params", JsonObject().apply {
                    addProperty("name", "codex-reply")
                    add("arguments", JsonObject().apply {
                        addProperty("sessionId", sessionId)
                        addProperty("conversationId", conversationId ?: sessionId)
                        addProperty("prompt", prompt)
                    })
                })
            }

            writer.write(gson.toJson(request))
            writer.write("\n")
            writer.flush()

            LogUtil.info("Continued Codex session with prompt: $prompt")

        } catch (e: Exception) {
            LogUtil.error("Failed to continue Codex session", e)
            notifyStatus("Failed to continue session: ${e.message}")
        }
    }

    /**
     * Parse MCP message from JSON-RPC format
     */
    @Suppress("D")
    private fun parseMcpMessage(line: String): CodexMessage? {
        try {
            val json = jsonParser.parse(line) as? JsonObject ?: return null

            LogUtil.info("Received MCP message: $line")

            // Check if it's a codex/event notification
            val method = json.get("method")?.asString
            if (method == "codex/event") {
                val params = json.getAsJsonObject("params")
                val msg = params.getAsJsonObject("msg")
                val type = msg.get("type")?.asString ?: return null

                // Extract session/conversation IDs from event
                updateIdentifiersFromEvent(msg)

                return when (type) {
                    "agent_message" -> {
                        val message = msg.get("message")?.asString ?: "Message received"
                        CodexMessage("assistant", message)
                    }

                    "exec_command_begin" -> {
                        val command = msg.get("command")?.asString ?: "Unknown command"
                        val callId = msg.get("call_id")?.asString
                        val cwd = msg.get("cwd")?.asString

                        // 提取更多信息到 metadata
                        val metadata = mutableMapOf<String, Any>()
                        metadata["command"] = command
                        callId?.let { metadata["callId"] = it }
                        cwd?.let { metadata["cwd"] = it }

                        CodexMessage("tool-call", "Executing: $command", metadata = metadata)
                    }

                    "exec_command_end" -> {
                        val output = msg.get("output")?.asString
                        val error = msg.get("error")?.asString
                        val callId = msg.get("call_id")?.asString
                        val success = msg.get("success")?.asBoolean ?: (error == null)

                        val displayText = output ?: error ?: "Command completed"

                        // 提取更多信息到 metadata
                        val metadata = mutableMapOf<String, Any>()
                        output?.let { metadata["output"] = it }
                        error?.let { metadata["error"] = it }
                        metadata["success"] = success
                        callId?.let { metadata["callId"] = it }

                        CodexMessage("tool-result", displayText, metadata = metadata)
                    }

                    "agent_reasoning" -> {
                        val text = msg.get("text")?.asString ?: "Thinking..."
                        CodexMessage("reasoning", text.take(100) + if (text.length > 100) "..." else "")
                    }

                    "agent_reasoning_delta" -> {
                        val delta = msg.get("delta")?.asString ?: ""
                        CodexMessage("reasoning-delta", delta)
                    }

                    "task_started" -> {
                        CodexMessage("status", "Task started")
                    }

                    "task_complete" -> {
                        CodexMessage("status", "Task completed")
                    }

                    "turn_aborted" -> {
                        CodexMessage("status", "Turn aborted")
                    }

                    "exec_approval_request" -> {
                        // 权限请求事件
                        val command = msg.get("codex_command")?.asJsonArray?.joinToString(" ") 
                            ?: msg.get("command")?.asString 
                            ?: "Unknown command"
                        val callId = msg.get("codex_call_id")?.asString ?: msg.get("call_id")?.asString
                        
                        val metadata = mutableMapOf<String, Any>()
                        metadata["command"] = command
                        callId?.let { metadata["callId"] = it }
                        metadata["needsApproval"] = true
                        
                        CodexMessage("tool-call", "Requesting permission: $command", metadata = metadata)
                    }

                    "patch_apply_begin" -> {
                        // 补丁应用开始
                        val changes = msg.get("changes")?.asJsonObject
                        val callId = msg.get("call_id")?.asString
                        val fileCount = changes?.size() ?: 0
                        val filesMsg = if (fileCount == 1) "1 file" else "$fileCount files"
                        
                        val metadata = mutableMapOf<String, Any>()
                        metadata["fileCount"] = fileCount
                        callId?.let { metadata["callId"] = it }
                        changes?.let { metadata["changes"] = it.toString() }
                        
                        CodexMessage("tool-call", "Modifying $filesMsg...", metadata = metadata)
                    }

                    "patch_apply_end" -> {
                        // 补丁应用完成
                        val success = msg.get("success")?.asBoolean ?: false
                        val stdout = msg.get("stdout")?.asString
                        val stderr = msg.get("stderr")?.asString
                        val callId = msg.get("call_id")?.asString
                        
                        val displayText = when {
                            success && stdout != null -> stdout
                            !success && stderr != null -> stderr
                            success -> "Files modified successfully"
                            else -> "Failed to modify files"
                        }
                        
                        val metadata = mutableMapOf<String, Any>()
                        metadata["success"] = success
                        stdout?.let { metadata["output"] = it }
                        stderr?.let { metadata["error"] = it }
                        callId?.let { metadata["callId"] = it }
                        
                        CodexMessage("tool-result", displayText, metadata = metadata)
                    }

                    "agent_reasoning_section_break" -> {
                        // 推理部分分隔，内部处理，不显示
                        null
                    }

                    "turn_diff" -> {
                        // 轮次差异，当前不显示（未来可以实现 DiffProcessor）
                        null
                    }

                    "token_count" -> {
                        // Token 统计，不显示
                        null
                    }

                    else -> {
                        // 未知类型，不显示（避免 system 类型消息）
                        LogUtil.debug("Unknown Codex event type: $type")
                        null
                    }
                }
            }

            // Check if it's a response to our request
            if (json.has("result")) {
                val result = json.getAsJsonObject("result")

                // Extract session/conversation IDs from response
                if (result.has("meta")) {
                    val meta = result.getAsJsonObject("meta")
                    meta.get("sessionId")?.asString?.let { sessionId = it }
                    meta.get("conversationId")?.asString?.let { conversationId = it }
                } else {
                    result.get("sessionId")?.asString?.let { sessionId = it }
                    result.get("conversationId")?.asString?.let { conversationId = it }
                }

                // Extract content from response
                if (result.has("content")) {
                    val content = result.get("content")
                    when {
                        content.isJsonArray -> {
                            val contentArray = content.asJsonArray
                            if (contentArray.size() > 0) {
                                val firstItem = contentArray[0].asJsonObject
                                val text = firstItem.get("text")?.asString ?: "Response received"
                                return CodexMessage("assistant", text)
                            }
                        }

                        content.isJsonPrimitive -> {
                            val text = content.asString
                            return CodexMessage("assistant", text)
                        }
                    }
                }
            }

            // Fallback: 未知消息类型，不显示（避免 system 类型消息）
            LogUtil.debug("Unhandled message type or format: ${line.take(200)}")
            return null

        } catch (e: Exception) {
            LogUtil.warn("Failed to parse message: $line", e)
            return null
        }
    }

    /**
     * Update session and conversation IDs from event data
     */
    private fun updateIdentifiersFromEvent(event: JsonObject) {
        // Check event itself
        event.get("session_id")?.asString?.let { sessionId = it }
        event.get("sessionId")?.asString?.let { sessionId = it }
        event.get("conversation_id")?.asString?.let { conversationId = it }
        event.get("conversationId")?.asString?.let { conversationId = it }

        // Check event.data if present
        event.get("data")?.asJsonObject?.let { data ->
            data.get("session_id")?.asString?.let { sessionId = it }
            data.get("sessionId")?.asString?.let { sessionId = it }
            data.get("conversation_id")?.asString?.let { conversationId = it }
            data.get("conversationId")?.asString?.let { conversationId = it }
        }
    }

    /**
     * Disconnect from Codex
     */
    fun disconnect() {
        try {
            isConnected.set(false)
            isSessionStarted = false
            sessionId = null
            conversationId = null

            readerThread?.interrupt()
            readerThread = null

            writer?.close()
            writer = null

            process?.destroy()
            process = null

            notifyStatus("Disconnected")
            LogUtil.info("Disconnected from Codex MCP")

        } catch (e: Exception) {
            LogUtil.error("Error disconnecting from Codex", e)
        }
    }
}

