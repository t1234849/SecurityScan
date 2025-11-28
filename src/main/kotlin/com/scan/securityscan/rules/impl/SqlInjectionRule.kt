package com.scan.securityscan.rules.impl

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.psi.*
import com.scan.securityscan.rules.AbstractSecurityRule
import com.scan.securityscan.rules.RiskLevel

/**
 * SQL 注入风险规则
 * 检测字符串拼接构造 SQL 语句的危险写法
 */
class SqlInjectionRule : AbstractSecurityRule() {
    
    override val ruleId = "SQL_INJECTION"
    override val ruleName = "SQL注入风险"
    override val description = "使用字符串拼接构造SQL语句可能导致SQL注入攻击"
    override val severity = RiskLevel.CRITICAL
    
    // SQL 关键字
    private val sqlKeywords = setOf(
        "SELECT", "INSERT", "UPDATE", "DELETE", "DROP", "CREATE", 
        "ALTER", "FROM", "WHERE", "JOIN", "UNION", "ORDER", "GROUP"
    )
    
    // JDBC 执行 SQL 的方法
    private val jdbcExecuteMethods = setOf(
        "java.sql.Statement.executeQuery",
        "java.sql.Statement.execute",
        "java.sql.Statement.executeUpdate",
        "java.sql.Connection.prepareStatement",
        "java.sql.Connection.createStatement"
    )
    
    // MyBatis SQL 注解
    private val mybatisSqlAnnotations = setOf(
        "org.apache.ibatis.annotations.Select",
        "org.apache.ibatis.annotations.Insert",
        "org.apache.ibatis.annotations.Update",
        "org.apache.ibatis.annotations.Delete"
    )
    
    override fun matches(element: PsiElement): Boolean {
        // 检查方法调用
        if (element is PsiMethodCallExpression) {
            return checkMethodCall(element)
        }
        
        // 检查二元表达式（字符串拼接）
        if (element is PsiBinaryExpression) {
            return checkStringConcatenation(element)
        }
        
        // 检查 MyBatis 注解中的 ${} 用法
        if (element is PsiAnnotation) {
            return checkMyBatisAnnotation(element)
        }
        
        return false
    }
    
    /**
     * 检查是否是危险的 SQL 执行方法调用
     */
    private fun checkMethodCall(expression: PsiMethodCallExpression): Boolean {
        val method = expression.resolveMethod() ?: return false
        val containingClass = method.containingClass ?: return false
        val className = containingClass.qualifiedName ?: return false
        val methodName = method.name
        
        val fullMethodName = "$className.$methodName"
        
        // 不是 JDBC 方法，不检查
        if (!jdbcExecuteMethods.any { fullMethodName == it }) {
            return false
        }
        
        // 检查参数是否是字符串拼接
        val arguments = expression.argumentList.expressions
        if (arguments.isEmpty()) {
            return false
        }
        
        val sqlArgument = arguments[0]
        return isSqlStringConcatenation(sqlArgument)
    }
    
    /**
     * 检查字符串拼接中是否包含 SQL 语句
     */
    private fun checkStringConcatenation(expression: PsiBinaryExpression): Boolean {
        // 必须是加号操作
        if (expression.operationTokenType != JavaTokenType.PLUS) {
            return false
        }
        
        // 检查是否包含 SQL 关键字
        val text = expression.text.uppercase()
        val hasSqlKeyword = sqlKeywords.any { text.contains(it) }
        
        if (!hasSqlKeyword) {
            return false
        }
        
        // 检查是否包含变量拼接（不是纯字面量）
        return containsVariableReference(expression)
    }
    
    /**
     * 判断表达式是否是 SQL 字符串拼接
     */
    private fun isSqlStringConcatenation(expression: PsiExpression): Boolean {
        // 如果是字面量，安全
        if (expression is PsiLiteralExpression) {
            return false
        }
        
        // 如果是引用，检查是否是拼接
        if (expression is PsiReferenceExpression) {
            val resolved = expression.resolve()
            if (resolved is PsiVariable) {
                val initializer = (resolved as? PsiLocalVariable)?.initializer
                    ?: (resolved as? PsiField)?.initializer
                
                if (initializer is PsiBinaryExpression) {
                    return checkStringConcatenation(initializer)
                }
            }
        }
        
        // 如果是二元表达式，检查是否是拼接
        if (expression is PsiBinaryExpression) {
            return checkStringConcatenation(expression)
        }
        
        // 如果是多态表达式（三元运算符等），递归检查
        if (expression is PsiConditionalExpression) {
            return isSqlStringConcatenation(expression.thenExpression ?: return false) ||
                   isSqlStringConcatenation(expression.elseExpression ?: return false)
        }
        
        return false
    }
    
    /**
     * 检查表达式是否包含变量引用
     */
    private fun containsVariableReference(expression: PsiElement): Boolean {
        var hasVariable = false
        
        expression.accept(object : JavaRecursiveElementVisitor() {
            override fun visitReferenceExpression(expression: PsiReferenceExpression) {
                super.visitReferenceExpression(expression)
                val resolved = expression.resolve()
                if (resolved is PsiVariable && !isConstant(resolved)) {
                    hasVariable = true
                }
            }
        })
        
        return hasVariable
    }
    
    /**
     * 判断是否是常量
     */
    private fun isConstant(variable: PsiVariable): Boolean {
        return variable.hasModifierProperty(PsiModifier.FINAL) && 
               variable.hasModifierProperty(PsiModifier.STATIC)
    }
    
    /**
     * 检查 MyBatis 注解中是否使用了 ${} 
     */
    private fun checkMyBatisAnnotation(annotation: PsiAnnotation): Boolean {
        val qualifiedName = annotation.qualifiedName ?: return false
        
        // 检查是否是 MyBatis SQL 注解
        if (!mybatisSqlAnnotations.contains(qualifiedName)) {
            return false
        }
        
        // 获取注解的 value 参数（SQL 语句）
        val valueAttribute = annotation.findAttributeValue("value")
        if (valueAttribute == null) {
            return false
        }
        
        // 获取 SQL 字符串内容
        val sqlText = when (valueAttribute) {
            is PsiLiteralExpression -> valueAttribute.value as? String
            else -> valueAttribute.text?.trim('"')
        } ?: return false
        
        // 检查是否包含 ${}（字符串替换，不安全）
        // 同时排除 #{}（参数绑定，安全）
        return sqlText.contains("\${") || sqlText.contains("${'$'}{")
    }
    
    override fun getProblemDescription(element: PsiElement): String {
        // 区分 MyBatis 注解和普通 SQL 拼接
        if (element is PsiAnnotation) {
            return """
                🚨 严重安全风险：MyBatis SQL 注入
                
                【问题】使用 ${'$'}{} 进行字符串替换，攻击者可以注入恶意 SQL 代码
                
                【修复建议】
                1. 使用 #{} 参数绑定（推荐）
                   // ❌ 不安全
                   @Select("SELECT * FROM users WHERE username = '${'$'}{username}'")
                   
                   // ✅ 安全
                   @Select("SELECT * FROM users WHERE username = #{username}")
                
                2. 如果必须使用 ${'$'}{}（如 ORDER BY），需要白名单校验
                   default List<User> listUsers(String sortColumn) {
                       // 白名单校验
                       if (!sortColumn.matches("^(id|username|email)${'$'}")) {
                           throw new IllegalArgumentException("Invalid column");
                       }
                       return listUsersInternal(sortColumn);
                   }
                
                【为什么 ${'$'}{} 不安全】
                - ${'$'}{} 直接拼接字符串，不做任何转义
                - #{} 使用 PreparedStatement，自动转义
                
                【攻击示例】
                输入: admin' --
                SQL: SELECT * FROM users WHERE username = 'admin' --'
                结果: 注释掉后面的条件，绕过验证
            """.trimIndent()
        } else {
            return """
                🚨 严重安全风险：SQL 注入攻击
                
                【问题】使用字符串拼接构造 SQL 语句，攻击者可以注入恶意 SQL 代码
                
                【修复建议】
                1. 使用 PreparedStatement 参数化查询
                   String sql = "SELECT * FROM users WHERE id = ?";
                   PreparedStatement pstmt = conn.prepareStatement(sql);
                   pstmt.setInt(1, userId);
                
                2. 使用 MyBatis 的 #{} 参数绑定
                   @Select("SELECT * FROM users WHERE username = #{username}")
                
                【危险示例】
                输入: ' OR '1'='1
                结果: 绕过身份验证，返回所有数据
            """.trimIndent()
        }
    }
    
    override fun getQuickFixes(element: PsiElement): Array<LocalQuickFix> {
        // 不提供自动修复，避免类型不匹配等问题
        // 只提供详细的修复建议供用户参考
        return emptyArray()
    }
    
    override fun getSecurityAdvice(): String {
        return """
            【修复建议】
            1. 【强烈推荐】使用 PreparedStatement 参数化查询
            2. 使用 ORM 框架（MyBatis、Hibernate）的参数绑定功能
            3. 对用户输入进行严格的白名单验证
            4. 避免将用户输入直接拼接到 SQL 语句中
            
            【为什么字符串拼接不安全】
            攻击者可以通过精心构造的输入来改变 SQL 语句的语义：
            - 输入: ' OR '1'='1
            - 原 SQL: SELECT * FROM users WHERE name = 'input'
            - 注入后: SELECT * FROM users WHERE name = '' OR '1'='1'
            结果：绕过认证，返回所有用户数据
            
            【安全写法示例】
            
            // ❌ 不安全：字符串拼接
            String sql = "SELECT * FROM users WHERE username = '" + username + "'";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            
            // ✅ 安全：PreparedStatement
            String sql = "SELECT * FROM users WHERE username = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            
            // ✅ 安全：MyBatis 参数绑定
            @Select("SELECT * FROM users WHERE username = #{username}")
            User findByUsername(String username);
            
            // ❌ 不安全：MyBatis 使用 ${'$'}{}
            @Select("SELECT * FROM users WHERE username = '${'$'}{username}'")  // 危险！
            
            【国内审计要求】
            SQL 注入是奇安信、等保测评的必查项，属于高危漏洞。
            所有数据库操作都需要使用参数化查询，不允许字符串拼接。
        """.trimIndent()
    }
}


