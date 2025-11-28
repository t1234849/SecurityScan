package com.scan.securityscan.rules.impl

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.psi.*
import com.scan.securityscan.rules.AbstractSecurityRule
import com.scan.securityscan.rules.RiskLevel

/**
 * 系统信息泄露检测规则
 * 检测是否直接返回异常信息给外部用户
 */
class InformationLeakRule : AbstractSecurityRule() {
    
    override val ruleId = "INFORMATION_LEAK"
    override val ruleName = "系统信息泄露"
    override val description = "直接返回异常详细信息可能泄露系统内部信息"
    override val severity = RiskLevel.WARNING
    
    override fun matches(element: PsiElement): Boolean {
        // 检查返回语句
        if (element is PsiReturnStatement) {
            return checkReturnStatement(element)
        }
        
        // 检查方法调用中的 getMessage()
        if (element is PsiMethodCallExpression) {
            return checkMethodCallInReturn(element)
        }
        
        return false
    }
    
    /**
     * 检查 return 语句是否包含 e.getMessage()
     */
    private fun checkReturnStatement(returnStatement: PsiReturnStatement): Boolean {
        val returnValue = returnStatement.returnValue ?: return false
        
        // 递归检查返回值表达式
        return containsGetMessage(returnValue)
    }
    
    /**
     * 检查方法调用是否在返回语句中
     */
    private fun checkMethodCallInReturn(methodCall: PsiMethodCallExpression): Boolean {
        // 检查是否是 getMessage() 调用
        val method = methodCall.resolveMethod() ?: return false
        val methodName = method.name
        
        if (methodName != "getMessage" && methodName != "getLocalizedMessage") {
            return false
        }
        
        // 检查是否在异常对象上调用
        val qualifier = methodCall.methodExpression.qualifierExpression
        if (qualifier == null || !isExceptionType(qualifier)) {
            return false
        }
        
        // 检查是否在返回语句或返回表达式中
        return isInReturnContext(methodCall)
    }
    
    /**
     * 检查表达式是否包含 getMessage() 调用
     */
    private fun containsGetMessage(expression: PsiExpression): Boolean {
        var found = false
        
        expression.accept(object : JavaRecursiveElementVisitor() {
            override fun visitMethodCallExpression(methodCall: PsiMethodCallExpression) {
                super.visitMethodCallExpression(methodCall)
                
                val method = methodCall.resolveMethod()
                val methodName = method?.name
                
                if (methodName == "getMessage" || methodName == "getLocalizedMessage") {
                    val qualifier = methodCall.methodExpression.qualifierExpression
                    if (qualifier != null && isExceptionType(qualifier)) {
                        found = true
                    }
                }
            }
        })
        
        return found
    }
    
    /**
     * 检查表达式是否是异常类型
     */
    private fun isExceptionType(expression: PsiExpression): Boolean {
        val type = expression.type ?: return false
        val className = type.canonicalText
        
        // 检查是否是 Throwable 或其子类
        return className.contains("Exception") || 
               className.contains("Error") || 
               className.contains("Throwable") ||
               className == "java.lang.Throwable"
    }
    
    /**
     * 检查方法调用是否在返回上下文中
     */
    private fun isInReturnContext(element: PsiElement): Boolean {
        var parent: PsiElement? = element.parent
        
        while (parent != null) {
            if (parent is PsiReturnStatement) {
                return true
            }
            
            // 如果到达方法级别还没找到 return，说明不在返回上下文
            if (parent is PsiMethod) {
                break
            }
            
            parent = parent.parent
        }
        
        return false
    }
    
    override fun getProblemDescription(element: PsiElement): String {
        return """
            ⚠️ 系统信息泄露：直接返回异常详细信息
            
            【问题】返回 e.getMessage() 可能暴露：
            - 系统内部路径和文件结构
            - 数据库表结构和字段名
            - 第三方服务地址和配置
            - SQL 语句和参数
            - 堆栈信息和代码位置
            
            【修复建议】
            1. 返回通用的错误消息
               // ❌ 不安全
               return AjaxResult.error(e.getMessage());
               
               // ✅ 安全
               return AjaxResult.error("操作失败，请稍后重试");
            
            2. 返回业务相关的错误信息
               // ✅ 安全：不暴露技术细节
               return AjaxResult.error("请求地址" + requestURI + ",不支持" + method + "请求");
               
            3. 记录详细日志，但只返回通用消息
               // ✅ 安全：日志记录详细信息，用户看到通用消息
               log.error("操作失败: {}", e.getMessage(), e);
               return AjaxResult.error("系统繁忙，请稍后重试");
            
            【攻击者可能获取的信息】
            - 数据库类型和版本
            - 应用服务器路径
            - 业务逻辑细节
            - 用于进一步攻击的技术信息
            
            【国内审计要求】
            - 奇安信：不允许向外暴露系统内部信息
            - 等保测评：错误信息应该通用化
            - 不应暴露技术实现细节
        """.trimIndent()
    }
    
    override fun getQuickFixes(element: PsiElement): Array<LocalQuickFix> {
        return emptyArray()
    }
    
    override fun getSecurityAdvice(): String {
        return """
            【系统信息泄露防护最佳实践】
            
            1. 【原则】对外接口不返回异常详细信息
            
            // ❌ 危险的写法
            @ExceptionHandler(Exception.class)
            public AjaxResult handleException(Exception e) {
                return AjaxResult.error(e.getMessage());  // 暴露内部信息
            }
            
            // ✅ 安全的写法 1：返回通用错误
            @ExceptionHandler(Exception.class)
            public AjaxResult handleException(Exception e) {
                // 记录详细日志供运维排查
                log.error("系统异常", e);
                
                // 返回通用错误消息
                return AjaxResult.error("系统繁忙，请稍后重试");
            }
            
            // ✅ 安全的写法 2：返回业务相关错误
            @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
            public AjaxResult handleMethodNotSupported(
                    HttpRequestMethodNotSupportedException e,
                    HttpServletRequest request) {
                
                // 记录详细日志
                log.warn("不支持的请求方法: {} {}", request.getMethod(), request.getRequestURI());
                
                // 返回业务错误，不暴露技术细节
                String message = "请求地址" + request.getRequestURI() + 
                               ",不支持" + e.getMethod() + "请求";
                return AjaxResult.error(message);
            }
            
            2. 【分类处理】不同异常返回不同消息
            
            // ✅ 业务异常：可以返回具体消息
            @ExceptionHandler(BusinessException.class)
            public AjaxResult handleBusinessException(BusinessException e) {
                // 业务异常消息是安全的，可以返回
                return AjaxResult.error(e.getMessage());
            }
            
            // ✅ 参数校验异常：返回校验信息
            @ExceptionHandler(MethodArgumentNotValidException.class)
            public AjaxResult handleValidException(MethodArgumentNotValidException e) {
                // 校验错误信息是安全的
                String message = e.getBindingResult().getFieldError().getDefaultMessage();
                return AjaxResult.error(message);
            }
            
            // ✅ 系统异常：返回通用消息
            @ExceptionHandler(Exception.class)
            public AjaxResult handleSystemException(Exception e) {
                log.error("系统异常", e);
                return AjaxResult.error("系统繁忙，请稍后重试");
            }
            
            3. 【日志记录】详细信息记录到日志
            
            try {
                // 业务处理
                userService.updateUser(user);
            } catch (SQLException e) {
                // ✅ 详细信息记录到日志
                log.error("更新用户失败, userId={}, error={}", 
                         user.getId(), e.getMessage(), e);
                
                // ✅ 返回通用消息
                return AjaxResult.error("更新失败，请稍后重试");
            }
            
            4. 【开发环境特殊处理】
            
            @ExceptionHandler(Exception.class)
            public AjaxResult handleException(Exception e, HttpServletRequest request) {
                log.error("系统异常", e);
                
                // 开发环境返回详细信息，便于调试
                if (isDevelopmentMode()) {
                    return AjaxResult.error(e.getMessage());
                }
                
                // 生产环境返回通用消息
                return AjaxResult.error("系统繁忙，请稍后重试");
            }
            
            private boolean isDevelopmentMode() {
                String env = System.getProperty("spring.profiles.active");
                return "dev".equals(env) || "local".equals(env);
            }
            
            5. 【错误码机制】使用错误码而非详细消息
            
            // ✅ 使用错误码
            public class ErrorCode {
                public static final String SYSTEM_ERROR = "SYS001";
                public static final String DB_ERROR = "SYS002";
                public static final String NETWORK_ERROR = "SYS003";
            }
            
            @ExceptionHandler(SQLException.class)
            public AjaxResult handleSQLException(SQLException e) {
                log.error("数据库异常", e);
                
                // 返回错误码和通用消息
                return AjaxResult.error(ErrorCode.DB_ERROR, "数据库操作失败");
            }
            
            6. 【敏感信息过滤】
            
            // ✅ 过滤敏感信息
            private String sanitizeErrorMessage(String message) {
                if (message == null) return "操作失败";
                
                // 移除可能的敏感信息
                message = message.replaceAll("password=\\w+", "password=***");
                message = message.replaceAll("/[a-zA-Z]:/.*", "");  // 移除路径
                message = message.replaceAll("jdbc:.*", "");  // 移除数据库连接串
                
                return message;
            }
            
            【可能泄露的敏感信息示例】
            
            1. 数据库异常
               SQLException: Table 'user_payment_info' doesn't exist
               → 暴露了数据库表名
            
            2. 文件路径
               FileNotFoundException: /opt/app/config/database.properties
               → 暴露了服务器路径结构
            
            3. SQL 语句
               SQLSyntaxErrorException: near "admin_password": syntax error
               → 暴露了表字段名
            
            4. 第三方服务
               ConnectException: Connection refused: http://internal-api.company.com
               → 暴露了内网服务地址
            
            【检查清单】
            ✓ 所有对外接口不直接返回 e.getMessage()
            ✓ 系统异常返回通用错误消息
            ✓ 业务异常可以返回具体消息（确保安全）
            ✓ 详细错误信息记录到日志
            ✓ 使用错误码机制
            ✓ 开发/生产环境区分处理
            
            【国内审计标准】
            - 奇安信：不允许向外暴露系统技术细节
            - 等保测评：错误信息应通用化，不暴露实现
            - 漏洞扫描：信息泄露是常见检查项
        """.trimIndent()
    }
}

