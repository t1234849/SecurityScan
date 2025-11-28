package com.scan.securityscan.inspections

import com.scan.securityscan.rules.SecurityRule
import com.scan.securityscan.rules.impl.SqlInjectionRule

/**
 * SQL 注入风险检查
 */
class SqlInjectionInspection : SecurityInspectionBase() {
    
    override fun getSecurityRules(): List<SecurityRule> {
        return listOf(SqlInjectionRule())
    }
    
    override fun getDisplayName(): String {
        return "SQL注入漏洞检测（高危）"
    }
    
    override fun getShortName(): String {
        return "SqlInjection"
    }
    
    override fun getStaticDescription(): String {
        return """
            检测可能导致 SQL 注入攻击的不安全代码。
            
            SQL 注入是最常见也是危害最大的 Web 安全漏洞之一。
            当使用字符串拼接构造 SQL 语句时，攻击者可以通过精心构造的输入来改变 SQL 语句的语义，
            从而执行未授权的数据库操作。
            
            【检测范围】
            - 字符串拼接构造 SQL 语句
            - Statement.executeQuery() 使用拼接的 SQL
            - Statement.execute() 使用拼接的 SQL
            - MyBatis 中使用 ${'$'}{} 而不是 #{}
            
            【常见攻击场景】
            1. 绕过身份认证
               输入: admin' --
               结果: 注释掉密码验证部分
            
            2. 数据泄露
               输入: ' UNION SELECT credit_card FROM payments --
               结果: 获取敏感数据
            
            3. 数据篡改
               输入: '; DELETE FROM users; --
               结果: 删除所有用户数据
            
            【修复方案】
            - 使用 PreparedStatement 参数化查询
            - 使用 ORM 框架的参数绑定
            - 对输入进行严格验证
            
            【国内审计要求】
            - 奇安信代码审计：必查项
            - 等保2.0测评：高风险项
            - OWASP Top 10: A03:2021
            
            【修复优先级】
            🔥 最高优先级 - 必须立即修复
        """.trimIndent()
    }
}

