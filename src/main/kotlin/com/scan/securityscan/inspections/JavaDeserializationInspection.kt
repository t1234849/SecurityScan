package com.scan.securityscan.inspections

import com.scan.securityscan.rules.SecurityRule
import com.scan.securityscan.rules.impl.JavaDeserializationRule

/**
 * Java 原生反序列化风险检查
 */
class JavaDeserializationInspection : SecurityInspectionBase() {
    
    override fun getSecurityRules(): List<SecurityRule> {
        return listOf(JavaDeserializationRule())
    }
    
    override fun getDisplayName(): String {
        return "Java原生反序列化漏洞（高危）"
    }
    
    override fun getShortName(): String {
        return "JavaDeserialization"
    }
    
    override fun getStaticDescription(): String {
        return """
            检测 Java 原生反序列化相关的安全风险。
            
            Java 原生序列化是一个历史遗留的严重安全问题。攻击者可以通过构造恶意的序列化数据，
            在反序列化时执行任意代码。这是导致多个企业级应用（WebLogic、JBoss 等）严重漏洞的根源。
            
            【高危方法】
            - ObjectInputStream.readObject()
            - ObjectInputStream.readUnshared()
            - XMLDecoder.readObject()
            
            【著名漏洞】
            - WebLogic CVE-2015-4852 (CVSS 10.0)
            - JBoss CVE-2017-12149
            - Apache Commons Collections RCE
            
            【为什么危险】
            1. 反序列化会自动调用类的构造函数和特殊方法
            2. 攻击者可以构造 gadget chain 执行任意代码
            3. 即使不导入恶意类，也可能利用系统已有的类
            
            【国内审计重点】
            奇安信、等保测评都会重点关注 Java 反序列化的使用情况，建议：
            1. 完全避免反序列化不受信任的数据
            2. 使用 JSON、Protocol Buffers 等安全的序列化格式
            3. 如必须使用，添加严格的类白名单过滤
            
            【修复优先级】
            🔥 最高优先级 - 必须立即修复
        """.trimIndent()
    }
}

