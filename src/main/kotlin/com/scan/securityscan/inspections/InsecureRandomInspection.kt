package com.scan.securityscan.inspections

import com.scan.securityscan.rules.SecurityRule
import com.scan.securityscan.rules.impl.InsecureRandomRule

/**
 * 不安全的随机数检查
 */
class InsecureRandomInspection : SecurityInspectionBase() {
    
    override fun getSecurityRules(): List<SecurityRule> {
        return listOf(InsecureRandomRule())
    }
    
    override fun getDisplayName(): String {
        return "不安全的随机数（密码管理）"
    }
    
    override fun getShortName(): String {
        return "InsecureRandom"
    }
    
    override fun getStaticDescription(): String {
        return """
            检测使用 java.util.Random 而不是 SecureRandom 的情况。
            
            java.util.Random 不是密码学安全的随机数生成器：
            - 使用线性同余算法，输出可预测
            - 攻击者可以通过观察部分输出推测后续随机数
            - 不适用于生成密码、令牌、密钥等安全敏感数据
            
            【检测范围】
            new Random() 的使用
            
            【推荐做法】
            1. 使用 SecureRandom（密码学安全）
               SecureRandom secureRandom = new SecureRandom();
               byte[] tokenBytes = new byte[32];
               secureRandom.nextBytes(tokenBytes);
            
            2. 使用 UUID（简单场景）
               String token = UUID.randomUUID().toString();
            
            【使用场景对比】
            
            ✅ Random 适用于：
            - 游戏随机数
            - UI 动画效果
            - 非安全相关的随机选择
            
            ❌ Random 不适用于：
            - 生成密码
            - 生成会话令牌（Session Token）
            - 生成 CSRF Token
            - 生成 API Key
            - 生成加密密钥
            - 生成验证码
            
            【为什么 Random 不安全】
            Random 使用线性同余公式，攻击者如果观察到连续几个随机数，
            可以推算出种子值，从而预测所有后续的随机数。
            
            【国内审计要求】
            - 奇安信：密码、令牌等必须使用密码学安全的随机数
            - 等保测评：敏感数据生成需要使用安全的随机数生成器
            
            【修复优先级】
            ⚠️ 中等优先级 - 建议及时修复（取决于使用场景）
        """.trimIndent()
    }
}

