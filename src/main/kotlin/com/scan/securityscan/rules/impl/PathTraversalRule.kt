package com.scan.securityscan.rules.impl

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.psi.*
import com.scan.securityscan.rules.AbstractSecurityRule
import com.scan.securityscan.rules.RiskLevel

/**
 * 路径遍历风险规则
 * 检测 new File(path) 和 Paths.get(path) 等可能导致路径遍历的写法
 */
class PathTraversalRule : AbstractSecurityRule() {
    
    override val ruleId = "PATH_TRAVERSAL"
    override val ruleName = "路径遍历风险"
    override val description = "直接使用外部输入创建文件路径可能导致路径遍历攻击"
    override val severity = RiskLevel.ERROR
    
    // 危险的方法列表
    private val dangerousMethods = setOf(
        "java.io.File.<init>",
        "java.nio.file.Paths.get",
        "java.nio.file.Path.of"
    )
    
    override fun matches(element: PsiElement): Boolean {
        return when (element) {
            is PsiNewExpression -> checkNewExpression(element)
            is PsiMethodCallExpression -> checkMethodCall(element)
            else -> false
        }
    }
    
    /**
     * 检查 new File(...) 表达式
     */
    private fun checkNewExpression(expression: PsiNewExpression): Boolean {
        val classReference = expression.classReference ?: return false
        val className = classReference.qualifiedName ?: return false
        
        if (className != "java.io.File") {
            return false
        }
        
        val argumentList = expression.argumentList ?: return false
        val arguments = argumentList.expressions
        
        if (arguments.isEmpty()) {
            return false
        }
        
        // 如果参数不是字面量，则可能存在风险
        return !isLiteralOrConstant(arguments[0])
    }
    
    /**
     * 检查 Paths.get(...) 或 Path.of(...) 方法调用
     */
    private fun checkMethodCall(expression: PsiMethodCallExpression): Boolean {
        val method = expression.resolveMethod() ?: return false
        val containingClass = method.containingClass ?: return false
        val className = containingClass.qualifiedName ?: return false
        val methodName = method.name
        
        // 检查是否是 Paths.get 或 Path.of
        val isPathMethod = (className == "java.nio.file.Paths" && methodName == "get") ||
                          (className == "java.nio.file.Path" && methodName == "of")
        
        if (!isPathMethod) {
            return false
        }
        
        val arguments = expression.argumentList.expressions
        if (arguments.isEmpty()) {
            return false
        }
        
        // 如果第一个参数不是字面量，则可能存在风险
        return !isLiteralOrConstant(arguments[0])
    }
    
    /**
     * 判断表达式是否是字面量或常量
     */
    private fun isLiteralOrConstant(expression: PsiExpression): Boolean {
        return when (expression) {
            is PsiLiteralExpression -> true
            is PsiReferenceExpression -> {
                val resolved = expression.resolve()
                resolved is PsiField && resolved.hasModifierProperty(PsiModifier.FINAL)
            }
            else -> false
        }
    }
    
    override fun getProblemDescription(element: PsiElement): String {
        return """
            ⚠️ 安全风险：路径遍历攻击
            
            【问题】直接使用外部输入创建文件路径，攻击者可能通过 ../ 访问系统任意文件
            
            【修复建议】
            1. 使用 FileUtil.file() 等安全工具类
               File file = cn.hutool.core.io.FileUtil.file(basePath, fileName);
            
            2. 路径规范化 + 白名单校验
               Path path = Paths.get(basePath, fileName).normalize();
               if (!path.startsWith(Paths.get(basePath).normalize())) {
                   throw new SecurityException("Path traversal detected");
               }
            
            3. 文件名白名单验证
               if (!fileName.matches("^[a-zA-Z0-9._-]+$")) {
                   throw new SecurityException("Invalid file name");
               }
            
            【攻击示例】
            输入: ../../etc/passwd
            结果: 访问系统敏感文件
        """.trimIndent()
    }
    
    override fun getQuickFixes(element: PsiElement): Array<LocalQuickFix> {
        // 不提供自动修复，避免类型不匹配等问题
        return emptyArray()
    }
    
    override fun getSecurityAdvice(): String {
        return """
            【修复建议】
            1. 使用 FileUtil.file() 等安全工具类创建文件对象
            2. 对文件路径进行规范化处理，防止 ../ 等路径穿越
            3. 限制文件访问在特定目录范围内
            4. 使用白名单方式验证文件名
            
            【示例代码】
            // 不安全的写法
            String fileName = request.getParameter("file");
            File file = new File(basePath + fileName);
            
            // 安全的写法 1：使用工具类
            File file = FileUtil.file(basePath, fileName);
            
            // 安全的写法 2：路径校验
            String fileName = request.getParameter("file");
            // 规范化路径
            Path path = Paths.get(basePath, fileName).normalize();
            // 确保在允许的目录内
            if (!path.startsWith(Paths.get(basePath).normalize())) {
                throw new SecurityException("Path traversal detected");
            }
            File file = path.toFile();
            
            // 安全的写法 3：文件名白名单
            if (!isValidFileName(fileName)) {
                throw new SecurityException("Invalid file name");
            }
        """.trimIndent()
    }
}
