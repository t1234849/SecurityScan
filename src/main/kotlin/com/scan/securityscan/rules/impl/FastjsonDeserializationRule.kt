package com.scan.securityscan.rules.impl

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.psi.*
import com.scan.securityscan.rules.AbstractSecurityRule
import com.scan.securityscan.rules.RiskLevel

/**
 * Fastjson 反序列化风险规则
 * 检测使用 Fastjson 的危险方法，特别是启用了 AutoType 的情况
 */
class FastjsonDeserializationRule : AbstractSecurityRule() {
    
    override val ruleId = "FASTJSON_DESERIALIZATION"
    override val ruleName = "Fastjson反序列化风险"
    override val description = "使用Fastjson反序列化可能导致远程代码执行"
    override val severity = RiskLevel.CRITICAL
    
    // 危险的 Fastjson 方法
    private val dangerousMethods = mapOf(
        "com.alibaba.fastjson.JSON.parseObject" to "使用 parseObject 可能触发反序列化漏洞",
        "com.alibaba.fastjson.JSON.parse" to "使用 parse 可能触发反序列化漏洞",
        "com.alibaba.fastjson.JSON.parseArray" to "使用 parseArray 可能触发反序列化漏洞",
        "com.alibaba.fastjson2.JSON.parseObject" to "Fastjson2 也存在反序列化风险",
        "com.alibaba.fastjson2.JSON.parse" to "Fastjson2 也存在反序列化风险"
    )
    
    override fun matches(element: PsiElement): Boolean {
        if (element !is PsiMethodCallExpression) {
            return false
        }
        
        val method = element.resolveMethod() ?: return false
        val containingClass = method.containingClass ?: return false
        val className = containingClass.qualifiedName ?: return false
        val methodName = method.name
        
        val fullMethodName = "$className.$methodName"
        
        return dangerousMethods.keys.any { fullMethodName == it }
    }
    
    override fun getProblemDescription(element: PsiElement): String {
        if (element !is PsiMethodCallExpression) {
            return description
        }
        
        val method = element.resolveMethod()
        val containingClass = method?.containingClass
        val className = containingClass?.qualifiedName ?: ""
        val methodName = method?.name ?: ""
        
        val fullMethodName = "$className.$methodName"
        val specificMsg = dangerousMethods[fullMethodName] ?: description
        
        // 检查是否使用了 Feature.SupportAutoType
        val hasAutoType = checkAutoTypeEnabled(element)
        val autoTypeWarning = if (hasAutoType) {
            "🔥🔥🔥 检测到启用了 AutoType，风险极高！"
        } else {
            ""
        }
        
        return """
            🚨 严重安全风险：Fastjson 反序列化漏洞
            $autoTypeWarning
            
            【问题】$specificMsg
            
            【修复建议】
            1. 【推荐】替换为 Jackson
               ObjectMapper mapper = new ObjectMapper();
               MyClass obj = mapper.readValue(jsonString, MyClass.class);
            
            2. 替换为 Gson
               Gson gson = new Gson();
               MyClass obj = gson.fromJson(jsonString, MyClass.class);
            
            3. 如必须使用 Fastjson，请：
               - 升级到最新版本
               - 启用 SafeMode: ParserConfig.getGlobalInstance().setSafeMode(true);
               - 使用白名单: ParserConfig.getGlobalInstance().addAccept("com.yourcompany.");
            
            【为什么危险】
            Fastjson 的 AutoType 功能允许攻击者通过 @type 字段指定类名，
            可以实例化恶意类（如 JdbcRowSetImpl），执行任意代码。
        """.trimIndent()
    }
    
    /**
     * 检查是否启用了 AutoType
     */
    private fun checkAutoTypeEnabled(expression: PsiMethodCallExpression): Boolean {
        val arguments = expression.argumentList.expressions
        
        for (arg in arguments) {
            val text = arg.text
            // 检查是否包含 Feature.SupportAutoType 或 ParserConfig.global.setAutoTypeSupport
            if (text.contains("SupportAutoType") || 
                text.contains("setAutoTypeSupport(true)") ||
                text.contains("autoTypeSupport = true")) {
                return true
            }
        }
        
        return false
    }
    
    override fun getQuickFixes(element: PsiElement): Array<LocalQuickFix> {
        // 不提供自动修复，避免类型不匹配等问题
        // 只提供详细的修复建议供用户参考
        return emptyArray()
    }
    
    override fun getSecurityAdvice(): String {
        return """
            【修复建议】
            1. 【强烈推荐】替换为安全的 JSON 库：Jackson 或 Gson
            2. 如果必须使用 Fastjson，请升级到最新版本并禁用 AutoType
            3. 使用白名单模式，只允许反序列化特定的类
            4. 对输入的 JSON 数据进行严格验证
            
            【为什么 Fastjson 不安全】
            - AutoType 功能允许通过 @type 字段指定类名进行实例化
            - 攻击者可以利用此特性实例化恶意类（如 JdbcRowSetImpl）
            - 即使禁用 AutoType，仍可能通过绕过手段触发漏洞
            - Fastjson 历史上爆出多个高危 RCE 漏洞
            
            【安全替代方案】
            
            // 方案1：使用 Jackson（推荐）
            ObjectMapper mapper = new ObjectMapper();
            MyClass obj = mapper.readValue(jsonString, MyClass.class);
            
            // 方案2：使用 Gson
            Gson gson = new Gson();
            MyClass obj = gson.fromJson(jsonString, MyClass.class);
            
            // 方案3：如果必须用 Fastjson，务必配置安全策略
            ParserConfig.getGlobalInstance().setSafeMode(true);  // Fastjson 1.2.68+
            // 或使用白名单
            ParserConfig.getGlobalInstance().addAccept("com.yourcompany.safepkg.");
            
            【国内审计要求】
            奇安信、等保测评都会重点检查 Fastjson 使用情况，建议全面替换。
        """.trimIndent()
    }
}


