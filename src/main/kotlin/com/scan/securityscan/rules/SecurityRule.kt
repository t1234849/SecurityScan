package com.scan.securityscan.rules

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.psi.PsiElement

/**
 * 安全规则接口
 * 每个安全规则都需要实现此接口
 */
interface SecurityRule {
    /**
     * 规则唯一标识
     */
    val ruleId: String
    
    /**
     * 规则名称
     */
    val ruleName: String
    
    /**
     * 规则描述
     */
    val description: String
    
    /**
     * 风险级别
     */
    val severity: RiskLevel
    
    /**
     * 检查元素是否匹配此规则
     * @param element PSI元素
     * @return 如果匹配返回true，否则返回false
     */
    fun matches(element: PsiElement): Boolean
    
    /**
     * 获取问题描述（可以根据具体元素定制）
     * @param element 匹配的PSI元素
     * @return 问题描述文本
     */
    fun getProblemDescription(element: PsiElement): String = description
    
    /**
     * 获取修复建议
     * @param element 匹配的PSI元素
     * @return QuickFix列表，如果没有返回空数组
     */
    fun getQuickFixes(element: PsiElement): Array<LocalQuickFix> = emptyArray()
    
    /**
     * 获取详细的安全建议
     */
    fun getSecurityAdvice(): String = ""
}

/**
 * 风险级别枚举
 */
enum class RiskLevel {
    /** 信息提示 */
    INFO,
    /** 警告 */
    WARNING,
    /** 严重 */
    ERROR,
    /** 致命 */
    CRITICAL
}

