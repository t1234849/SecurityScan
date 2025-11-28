package com.scan.securityscan.rules

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.psi.PsiElement

/**
 * 安全规则抽象基类
 * 提供通用实现，简化具体规则的编写
 */
abstract class AbstractSecurityRule : SecurityRule {
    
    override fun getProblemDescription(element: PsiElement): String {
        return description
    }
    
    override fun getQuickFixes(element: PsiElement): Array<LocalQuickFix> {
        return emptyArray()
    }
    
    override fun getSecurityAdvice(): String {
        return "请参考国内安全审计标准进行修复"
    }
}

