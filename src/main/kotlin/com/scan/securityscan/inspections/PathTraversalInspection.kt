package com.scan.securityscan.inspections

import com.scan.securityscan.rules.SecurityRule
import com.scan.securityscan.rules.impl.PathTraversalRule

/**
 * 路径遍历风险检查
 */
class PathTraversalInspection : SecurityInspectionBase() {
    
    override fun getSecurityRules(): List<SecurityRule> {
        return listOf(PathTraversalRule())
    }
    
    override fun getDisplayName(): String {
        return "路径遍历风险检测"
    }
    
    override fun getShortName(): String {
        return "PathTraversal"
    }
    
    override fun getStaticDescription(): String {
        return """
            检测可能导致路径遍历攻击的不安全文件操作。
            
            当直接使用外部输入创建文件路径时，攻击者可能通过 ../ 等特殊字符访问系统中的任意文件。
            
            【检测范围】
            - new File(userInput)
            - Paths.get(userInput)
            - Path.of(userInput)
            
            【常见攻击手法】
            - 使用 ../ 穿越到上级目录
            - 使用绝对路径访问敏感文件
            - 利用符号链接绕过检查
            
            【修复建议】
            - 使用 FileUtil.file() 等安全工具类
            - 对路径进行规范化处理
            - 限制文件访问范围
            
            【符合国内审计标准】
            - 奇安信代码审计
            - 等保2.0测评要求
            - OWASP Top 10 (A01:2021)
        """.trimIndent()
    }
}

