package dev.dong4j.ccwrapper.util

/**
 * 日志工具类
 * 用于在开发时输出调试信息到 IDEA 控制台
 * 上线时可以通过设置 DEBUG = false 来禁用所有日志输出
 */
object LogUtil {
    
    /**
     * 调试开关
     * 设置为 false 时，所有日志输出将被禁用
     * 上线时设置为 false 即可
     */
    private const val DEBUG = true
    
    /**
     * 是否启用详细日志（包括堆栈跟踪等）
     */
    private const val VERBOSE = true
    
    /**
     * 输出信息日志
     */
    fun info(message: String, vararg args: Any?) {
        if (!DEBUG) return
        val formatted = formatMessage("INFO", message, args)
        System.out.println(formatted)
    }
    
    /**
     * 输出调试日志
     */
    fun debug(message: String, vararg args: Any?) {
        if (!DEBUG) return
        val formatted = formatMessage("DEBUG", message, args)
        System.out.println(formatted)
    }
    
    /**
     * 输出警告日志
     */
    fun warn(message: String, vararg args: Any?) {
        if (!DEBUG) return
        val formatted = formatMessage("WARN", message, args)
        System.out.println(formatted)
    }
    
    /**
     * 输出错误日志
     */
    fun error(message: String, throwable: Throwable? = null, vararg args: Any?) {
        if (!DEBUG) return
        val formatted = formatMessage("ERROR", message, args)
        System.out.println(formatted)
        
        if (throwable != null && VERBOSE) {
            System.out.println("  Exception: ${throwable.javaClass.simpleName}: ${throwable.message}")
            throwable.printStackTrace(System.out)
        }
    }
    
    /**
     * 输出详细调试日志（仅在 VERBOSE 模式下输出）
     */
    fun verbose(message: String, vararg args: Any?) {
        if (!DEBUG || !VERBOSE) return
        val formatted = formatMessage("VERBOSE", message, args)
        System.out.println(formatted)
    }
    
    /**
     * 格式化日志消息
     */
    private fun formatMessage(level: String, message: String, args: Array<out Any?>): String {
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss.SSS").format(java.util.Date())
        val threadName = Thread.currentThread().name
        
        // 格式化消息，支持参数替换
        val formattedMessage = if (args.isNotEmpty()) {
            try {
                // 如果消息包含 %s 占位符，使用 String.format
                if (message.contains("%s") || message.contains("%d") || message.contains("%f")) {
                    String.format(message, *args.map { it?.toString() ?: "null" }.toTypedArray())
                } else {
                    // 否则将参数追加到消息后面
                    "$message [${args.joinToString(", ") { it?.toString() ?: "null" }}]"
                }
            } catch (e: Exception) {
                "$message [${args.joinToString(", ") { it?.toString() ?: "null" }}]"
            }
        } else {
            message
        }
        
        return "[$timestamp] [$level] [$threadName] $formattedMessage"
    }
    
    /**
     * 输出大对象（JSON 等）的调试信息
     */
    fun debugLargeJson(label: String, obj: Any?) {
        if (!DEBUG) return
        debug("$label: ${formatObject(obj)}")
    }
    
    /**
     * 格式化对象为字符串
     */
    private fun formatObject(obj: Any?): String {
        if (obj == null) return "null"
        
        return try {
            // 尝试使用 Gson 格式化 JSON
            val gson = com.google.gson.GsonBuilder()
                .setPrettyPrinting()
                .create()
            gson.toJson(obj)
        } catch (e: Exception) {
            // 如果格式化失败，使用 toString
            obj.toString()
        }
    }
}

