package com.scan.securityscan.rules

/**
 * 安全规则注册中心
 * 管理所有的安全扫描规则
 */
object SecurityRuleRegistry {
    
    private val rules = mutableListOf<SecurityRule>()
    
    /**
     * 注册规则
     */
    fun registerRule(rule: SecurityRule) {
        rules.add(rule)
    }
    
    /**
     * 获取所有规则
     */
    fun getAllRules(): List<SecurityRule> {
        return rules.toList()
    }
    
    /**
     * 根据ID获取规则
     */
    fun getRuleById(ruleId: String): SecurityRule? {
        return rules.find { it.ruleId == ruleId }
    }
    
    /**
     * 清空所有规则（主要用于测试）
     */
    fun clear() {
        rules.clear()
    }
}

