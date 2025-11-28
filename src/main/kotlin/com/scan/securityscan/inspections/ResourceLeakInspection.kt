package com.scan.securityscan.inspections

import com.scan.securityscan.rules.SecurityRule
import com.scan.securityscan.rules.impl.ResourceLeakRule

/**
 * 资源未释放检查
 */
class ResourceLeakInspection : SecurityInspectionBase() {
    
    override fun getSecurityRules(): List<SecurityRule> {
        return listOf(ResourceLeakRule())
    }
    
    override fun getDisplayName(): String {
        return "资源未正确释放检测"
    }
    
    override fun getShortName(): String {
        return "ResourceLeak"
    }
    
    override fun getStaticDescription(): String {
        return """
            检测资源对象是否正确释放，防止资源泄漏。
            
            资源泄漏是 Java 应用中常见的问题，会导致：
            - 文件句柄泄漏，无法打开新文件
            - 数据库连接池耗尽，服务不可用
            - Socket 连接泄漏，端口耗尽
            - 内存泄漏，最终导致 OOM
            
            【检测范围】
            1. 资源对象未在 finally 中关闭
               - InputStream, OutputStream
               - Reader, Writer
               - Connection, Statement, ResultSet
               - Socket, ServerSocket
            
            2. 使用 IOUtils.closeQuietly() 关闭资源（不推荐）
               - closeQuietly() 会吞掉异常
               - 可能隐藏真正的问题
               - 不符合"快速失败"原则
            
            【推荐做法】
            1. 使用 try-with-resources（Java 7+）
               try (InputStream in = new FileInputStream(file)) {
                   // 使用资源
               } // 自动关闭
            
            2. 在 finally 中显式关闭
               InputStream in = null;
               try {
                   in = new FileInputStream(file);
                   // 使用资源
               } finally {
                   if (in != null) {
                       in.close(); // 显式关闭
                   }
               }
            
            【不推荐】
            - IOUtils.closeQuietly(in) // 会吞掉异常
            - 不在 finally 中关闭 // 异常时无法关闭
            
            【国内审计要求】
            - 资源管理是代码质量的基本要求
            - 长时间运行的服务必须正确管理资源
            - 避免生产环境资源泄漏导致服务故障
            
            【修复优先级】
            ⚠️ 中等优先级 - 建议及时修复
        """.trimIndent()
    }
}

