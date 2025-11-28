package com.scan.securityscan.inspections

import com.intellij.codeInspection.*
import com.intellij.psi.*
import com.scan.securityscan.rules.RiskLevel
import com.scan.securityscan.rules.SecurityRule

/**
 * 安全检查基类
 * 封装通用的 Inspection 逻辑
 */
abstract class SecurityInspectionBase : LocalInspectionTool() {
    
    /**
     * 获取此 Inspection 关联的安全规则
     */
    abstract fun getSecurityRules(): List<SecurityRule>
    
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : JavaElementVisitor() {
            
            override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
                super.visitMethodCallExpression(expression)
                checkElement(expression, holder)
            }
            
            override fun visitNewExpression(expression: PsiNewExpression) {
                super.visitNewExpression(expression)
                checkElement(expression, holder)
            }
            
            override fun visitBinaryExpression(expression: PsiBinaryExpression) {
                super.visitBinaryExpression(expression)
                checkElement(expression, holder)
            }
            
            override fun visitAnnotation(annotation: PsiAnnotation) {
                super.visitAnnotation(annotation)
                checkElement(annotation, holder)
            }
            
            override fun visitDeclarationStatement(statement: PsiDeclarationStatement) {
                super.visitDeclarationStatement(statement)
                checkElement(statement, holder)
            }
            
            override fun visitReturnStatement(statement: PsiReturnStatement) {
                super.visitReturnStatement(statement)
                checkElement(statement, holder)
            }
        }
    }
    
    /**
     * 检查元素是否违反安全规则
     */
    private fun checkElement(element: PsiElement, holder: ProblemsHolder) {
        for (rule in getSecurityRules()) {
            if (rule.matches(element)) {
                val description = rule.getProblemDescription(element)
                val quickFixes = rule.getQuickFixes(element)
                val highlightType = mapSeverityToHighlightType(rule.severity)
                
                holder.registerProblem(
                    element,
                    description,
                    highlightType,
                    *quickFixes
                )
            }
        }
    }
    
    /**
     * 将风险级别映射到 IDEA 的高亮类型
     * 所有安全问题统一显示为黄色波浪线，避免过于刺眼
     */
    private fun mapSeverityToHighlightType(severity: RiskLevel): ProblemHighlightType {
        // 所有级别都使用黄色波浪线（WARNING）
        // 这样更柔和，不会让代码看起来满屏都是错误
        return ProblemHighlightType.WARNING
    }
}

