package dev.dong4j.ccwrapper.util

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import java.io.File

/**
 * 项目信息工具类
 * 用于获取项目相关信息，以便传递给 Codex
 */
object ProjectInfoUtil {
    
    /**
     * 获取项目根路径
     */
    fun getProjectBasePath(project: Project): String? {
        return project.basePath
    }
    
    /**
     * 获取项目名称
     */
    fun getProjectName(project: Project): String {
        return project.name
    }
    
    /**
     * 获取项目类型信息
     */
    fun getProjectType(project: Project): String {
        val rootManager = ProjectRootManager.getInstance(project)
        val contentRoots = rootManager.contentRoots
        
        // 检测项目类型
        return when {
            contentRoots.any { File(it.path, "pom.xml").exists() } -> "Maven"
            contentRoots.any { 
                val path = it.path
                File(path, "build.gradle").exists() || File(path, "build.gradle.kts").exists() 
            } -> "Gradle"
            contentRoots.any { File(it.path, "package.json").exists() } -> "Node.js"
            contentRoots.any { File(it.path, "Cargo.toml").exists() } -> "Rust"
            contentRoots.any { File(it.path, "go.mod").exists() } -> "Go"
            contentRoots.any { File(it.path, "requirements.txt").exists() } -> "Python"
            contentRoots.any { File(it.path, "Gemfile").exists() } -> "Ruby"
            else -> "Unknown"
        }
    }
    
    /**
     * 构建项目上下文信息，用于添加到 Codex prompt 中
     * 参考 happy-cli 的做法，提供项目基本信息
     */
    fun buildProjectContext(project: Project): String {
        val basePath = getProjectBasePath(project)
        val projectName = getProjectName(project)
        val projectType = getProjectType(project)
        
        val context = StringBuilder()
        context.append("=== Project Context ===\n")
        context.append("Project Name: $projectName\n")
        context.append("Project Type: $projectType\n")
        
        if (basePath != null) {
            context.append("Project Path: $basePath\n")
            
            // 添加一些关键文件信息
            val keyFiles = detectKeyFiles(basePath)
            if (keyFiles.isNotEmpty()) {
                context.append("Key Files: ${keyFiles.joinToString(", ")}\n")
            }
        }
        
        context.append("======================\n\n")
        
        return context.toString()
    }
    
    /**
     * 检测项目中的关键文件
     */
    private fun detectKeyFiles(basePath: String): List<String> {
        val file = File(basePath)
        if (!file.exists() || !file.isDirectory) return emptyList()
        
        val keyFiles = mutableListOf<String>()
        
        // 检测常见的项目配置文件
        val commonFiles = listOf(
            "README.md", "README.txt",
            "package.json", "pom.xml", "build.gradle", "build.gradle.kts",
            "Cargo.toml", "go.mod", "requirements.txt", "Gemfile",
            ".gitignore", "Dockerfile"
        )
        
        commonFiles.forEach { fileName ->
            val filePath = java.io.File(file, fileName)
            if (filePath.exists()) {
                keyFiles.add(fileName)
            }
        }
        
        return keyFiles
    }
}

