package com.scan.securityscan.inspections

import com.scan.securityscan.rules.SecurityRule
import com.scan.securityscan.rules.impl.UnsafeUrlCreationRule

/**
 * 不安全的URL创建检查
 */
class UnsafeUrlCreationInspection : SecurityInspectionBase() {
    
    override fun getSecurityRules(): List<SecurityRule> {
        return listOf(UnsafeUrlCreationRule())
    }
    
    override fun getDisplayName(): String {
        return "不安全的URL创建（SSRF风险）"
    }
    
    override fun getShortName(): String {
        return "UnsafeUrlCreation"
    }
    
    override fun getStaticDescription(): String {
        return """
            检测可能导致SSRF（服务端请求伪造）攻击的不安全URL创建方式。
            
            当使用外部输入直接创建URL对象时，攻击者可能通过构造特殊的URL来访问内网资源或发起攻击。
            
            【常见问题】
            - 直接使用用户输入创建URL对象
            - 未对URL协议进行限制
            - 未对目标域名进行白名单校验
            
            【符合国内审计标准】
            - 奇安信代码审计
            - 等保2.0测评要求
            - OWASP Top 10
        """.trimIndent()
    }
}

