package com.scan.securityscan.rules.impl

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.psi.*
import com.scan.securityscan.rules.AbstractSecurityRule
import com.scan.securityscan.rules.RiskLevel

/**
 * 命令注入风险规则
 * 检测 Runtime.exec()、ProcessBuilder 等可能导致命令注入的危险方法
 */
class CommandInjectionRule : AbstractSecurityRule() {
    
    override val ruleId = "COMMAND_INJECTION"
    override val ruleName = "命令注入风险"
    override val description = "执行包含外部输入的系统命令可能导致命令注入攻击"
    override val severity = RiskLevel.CRITICAL
    
    // 危险的命令执行方法
    private val dangerousMethods = setOf(
        "java.lang.Runtime.exec",
        "java.lang.ProcessBuilder.command",
        "java.lang.ProcessBuilder.<init>"
    )
    
    override fun matches(element: PsiElement): Boolean {
        return when (element) {
            is PsiMethodCallExpression -> checkMethodCall(element)
            is PsiNewExpression -> checkNewExpression(element)
            else -> false
        }
    }
    
    /**
     * 检查方法调用
     */
    private fun checkMethodCall(expression: PsiMethodCallExpression): Boolean {
        val method = expression.resolveMethod() ?: return false
        val containingClass = method.containingClass ?: return false
        val className = containingClass.qualifiedName ?: return false
        val methodName = method.name
        
        val fullMethodName = "$className.$methodName"
        
        // 不是危险方法，不检查
        if (!dangerousMethods.any { fullMethodName == it }) {
            return false
        }
        
        // 检查参数是否包含变量（可能来自外部输入）
        val arguments = expression.argumentList.expressions
        if (arguments.isEmpty()) {
            return false
        }
        
        // 检查是否使用了字符串拼接或变量
        return containsUnsafeInput(arguments)
    }
    
    /**
     * 检查 new ProcessBuilder(...) 表达式
     */
    private fun checkNewExpression(expression: PsiNewExpression): Boolean {
        val classReference = expression.classReference ?: return false
        val className = classReference.qualifiedName ?: return false
        
        if (className != "java.lang.ProcessBuilder") {
            return false
        }
        
        val argumentList = expression.argumentList ?: return false
        val arguments = argumentList.expressions
        
        if (arguments.isEmpty()) {
            return false
        }
        
        return containsUnsafeInput(arguments)
    }
    
    /**
     * 检查参数是否包含不安全的输入
     */
    private fun containsUnsafeInput(arguments: Array<PsiExpression>): Boolean {
        for (arg in arguments) {
            // 如果是字面量，认为安全
            if (arg is PsiLiteralExpression) {
                continue
            }
            
            // 如果包含字符串拼接
            if (arg is PsiBinaryExpression && arg.operationTokenType == JavaTokenType.PLUS) {
                return true
            }
            
            // 如果是变量引用
            if (arg is PsiReferenceExpression) {
                val resolved = arg.resolve()
                // 如果不是 final 常量，认为可能不安全
                if (resolved is PsiVariable && !isConstant(resolved)) {
                    return true
                }
            }
            
            // 如果是数组或列表初始化
            if (arg is PsiNewExpression || arg is PsiArrayInitializerExpression) {
                // 递归检查数组元素
                val innerElements = when (arg) {
                    is PsiNewExpression -> arg.arrayInitializer?.initializers
                    is PsiArrayInitializerExpression -> arg.initializers
                    else -> null
                }
                if (innerElements != null && containsUnsafeInput(innerElements)) {
                    return true
                }
            }
        }
        
        return false
    }
    
    /**
     * 判断是否是常量
     */
    private fun isConstant(variable: PsiVariable): Boolean {
        return variable.hasModifierProperty(PsiModifier.FINAL) && 
               variable.hasModifierProperty(PsiModifier.STATIC)
    }
    
    override fun getProblemDescription(element: PsiElement): String {
        return """
            🚨 严重安全风险：命令注入攻击
            
            【问题】执行包含外部输入的系统命令，攻击者可以注入额外的命令
            
            【修复建议】
            1. 【最佳】避免执行外部命令，使用 Java API
               - 文件操作: Files.readAllBytes() 代替 cat
               - 压缩: ZipInputStream 代替 unzip
               - 图片: ImageIO 代替 convert
            
            2. 使用参数数组形式（不是字符串拼接）
               // ❌ 不安全
               Runtime.getRuntime().exec("cat " + fileName);
               
               // ✅ 安全
               Runtime.getRuntime().exec(new String[]{"cat", fileName});
               
               // ✅ 安全
               new ProcessBuilder("cat", fileName).start();
            
            3. 输入白名单校验
               if (!fileName.matches("^[a-zA-Z0-9._-]+$")) {
                   throw new SecurityException("Invalid input");
               }
            
            【攻击示例】
            输入: test.txt; rm -rf /
            执行: cat test.txt; rm -rf /
            结果: 删除系统所有文件
            
            【危险字符】; | & ${'$'} ` \n > < >> && ||
        """.trimIndent()
    }
    
    override fun getQuickFixes(element: PsiElement): Array<LocalQuickFix> {
        // 不提供自动修复，避免类型不匹配等问题
        return emptyArray()
    }
    
    override fun getSecurityAdvice(): String {
        return """
            【修复建议】
            
            1. 【最佳方案】避免执行外部命令
               - 使用 Java API 替代系统命令
               - 例如：文件操作用 Files 类，压缩用 java.util.zip
            
            2. 【如果必须执行】使用参数数组形式，不要使用字符串拼接
            
               // ❌ 不安全：字符串拼接
               String cmd = "cat " + fileName;
               Runtime.getRuntime().exec(cmd);
               
               // ✅ 安全：使用参数数组
               String[] cmd = {"cat", fileName};
               Runtime.getRuntime().exec(cmd);
               
               // ✅ 安全：使用 ProcessBuilder
               ProcessBuilder pb = new ProcessBuilder("cat", fileName);
               Process p = pb.start();
            
            3. 【输入验证】严格的白名单校验
            
               private boolean isValidFileName(String fileName) {
                   // 只允许字母、数字、下划线、点
                   return fileName.matches("^[a-zA-Z0-9._-]+$");
               }
               
               if (!isValidFileName(userInput)) {
                   throw new SecurityException("Invalid file name");
               }
            
            4. 【权限最小化】
               - 使用最低权限账户运行应用
               - 在容器中运行，限制可执行的命令
               - 使用 chroot 或 seccomp 限制系统调用
            
            【为什么参数数组更安全】
            
            使用字符串：
            - Runtime.exec("sh -c 'cat " + fileName + "'")
            - 攻击者输入：test; rm -rf /
            - 实际执行：sh -c 'cat test; rm -rf /'
            
            使用参数数组：
            - Runtime.exec(new String[]{"cat", fileName})
            - 攻击者输入：test; rm -rf /
            - 实际执行：cat "test; rm -rf /"（作为文件名参数）
            
            【常见危险场景】
            
            1. 文件处理
               // 不要这样：exec("cat " + file)
               // 应该用：Files.readAllBytes(path)
            
            2. 压缩解压
               // 不要这样：exec("unzip " + file)
               // 应该用：ZipInputStream
            
            3. 图片处理
               // 不要这样：exec("convert " + image)
               // 应该用：ImageIO、Thumbnailator
            
            4. 网络工具
               // 不要这样：exec("ping " + host)
               // 应该用：InetAddress.isReachable()
            
            【国内审计要求】
            - 奇安信：高危漏洞，必须修复
            - 等保测评：命令注入是必查项
            - OWASP Top 10: A03:2021 – Injection
        """.trimIndent()
    }
}
