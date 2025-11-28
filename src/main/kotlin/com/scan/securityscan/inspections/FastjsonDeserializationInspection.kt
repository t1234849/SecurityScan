package com.scan.securityscan.inspections

import com.scan.securityscan.rules.SecurityRule
import com.scan.securityscan.rules.impl.FastjsonDeserializationRule

/**
 * Fastjson 反序列化风险检查
 */
class FastjsonDeserializationInspection : SecurityInspectionBase() {
    
    override fun getSecurityRules(): List<SecurityRule> {
        return listOf(FastjsonDeserializationRule())
    }
    
    override fun getDisplayName(): String {
        return "Fastjson反序列化漏洞（高危）"
    }
    
    override fun getShortName(): String {
        return "FastjsonDeserialization"
    }
    
    override fun getStaticDescription(): String {
        return """
            检测 Fastjson 反序列化相关的安全风险。
            
            Fastjson 是国内广泛使用的 JSON 库，但历史上爆出多个严重的远程代码执行漏洞。
            攻击者可以通过构造恶意的 JSON 数据，利用 AutoType 功能实例化恶意类，从而执行任意代码。
            
            【高危方法】
            - JSON.parseObject()
            - JSON.parse()
            - JSON.parseArray()
            
            【已知漏洞】
            - CVE-2017-18349 (1.2.24)
            - CVE-2022-25845 (1.2.80)
            - 以及大量未分配 CVE 的绕过漏洞
            
            【国内审计重点】
            奇安信、等保测评都会重点关注 Fastjson 的使用情况，建议：
            1. 全面替换为 Jackson 或 Gson
            2. 如必须使用，升级到最新版本并启用 SafeMode
            3. 记录所有 Fastjson 使用位置，便于后续应急响应
            
            【修复优先级】
            🔥 最高优先级 - 建议立即修复
        """.trimIndent()
    }
}

