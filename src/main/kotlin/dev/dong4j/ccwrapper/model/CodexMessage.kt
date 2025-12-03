package dev.dong4j.ccwrapper.model

/**
 * Represents a message from Codex
 */
data class CodexMessage(
    val type: String,  // "user", "assistant", "tool-call", "tool-result", "error", etc.
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val metadata: Map<String, Any>? = null
)

