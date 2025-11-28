package com.scan.securityscan.rules.impl

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.psi.*
import com.scan.securityscan.rules.AbstractSecurityRule
import com.scan.securityscan.rules.RiskLevel

/**
 * 资源未释放检测规则
 * 检测 InputStream, OutputStream, Connection 等资源是否正确释放
 */
class ResourceLeakRule : AbstractSecurityRule() {
    
    override val ruleId = "RESOURCE_LEAK"
    override val ruleName = "资源未正确释放"
    override val description = "资源对象未在 finally 中正确关闭可能导致资源泄漏"
    override val severity = RiskLevel.WARNING
    
    // 需要关闭的资源类型
    private val resourceTypes = setOf(
        "java.io.InputStream",
        "java.io.OutputStream",
        "java.io.Reader",
        "java.io.Writer",
        "java.io.FileInputStream",
        "java.io.FileOutputStream",
        "java.io.BufferedReader",
        "java.io.BufferedWriter",
        "java.sql.Connection",
        "java.sql.Statement",
        "java.sql.PreparedStatement",
        "java.sql.ResultSet",
        "java.net.Socket",
        "java.net.ServerSocket"
    )
    
    override fun matches(element: PsiElement): Boolean {
        // 检查方法调用：IOUtils.closeQuietly()
        if (element is PsiMethodCallExpression) {
            return checkCloseQuietlyUsage(element)
        }
        
        // 检查变量声明：资源对象
        if (element is PsiDeclarationStatement) {
            return checkResourceDeclaration(element)
        }
        
        return false
    }
    
    /**
     * 检查是否使用了 IOUtils.closeQuietly()（不推荐）
     */
    private fun checkCloseQuietlyUsage(expression: PsiMethodCallExpression): Boolean {
        val method = expression.resolveMethod() ?: return false
        val containingClass = method.containingClass ?: return false
        val className = containingClass.qualifiedName ?: return false
        val methodName = method.name
        
        // 检查是否是 IOUtils.closeQuietly 或类似方法
        return (className.contains("IOUtils") || className.contains("IOUtil")) && 
               methodName.equals("closeQuietly", ignoreCase = true)
    }
    
    /**
     * 检查资源声明是否正确关闭
     */
    private fun checkResourceDeclaration(statement: PsiDeclarationStatement): Boolean {
        val elements = statement.declaredElements
        
        for (element in elements) {
            if (element is PsiVariable) {
                val type = element.type
                val typeText = type.canonicalText
                
                // 检查是否是需要关闭的资源类型
                val isResource = resourceTypes.any { typeText.contains(it) }
                if (!isResource) continue
                
                // 检查是否在 try-with-resources 中
                if (isInTryWithResources(element)) {
                    continue // try-with-resources 是安全的
                }
                
                // 检查是否在 finally 中正确关闭
                if (!isProperlyClosedInFinally(element)) {
                    return true // 发现问题
                }
            }
        }
        
        return false
    }
    
    /**
     * 检查变量是否在 try-with-resources 中声明
     */
    private fun isInTryWithResources(variable: PsiVariable): Boolean {
        var parent = variable.parent
        while (parent != null) {
            if (parent is PsiResourceList) {
                return true
            }
            if (parent is PsiMethod || parent is PsiClass) {
                break
            }
            parent = parent.parent
        }
        return false
    }
    
    /**
     * 检查变量是否在 finally 块中正确关闭
     */
    private fun isProperlyClosedInFinally(variable: PsiVariable): Boolean {
        // 查找包含该变量的方法
        var parent: PsiElement? = variable.parent
        while (parent != null && parent !is PsiMethod) {
            parent = parent.parent
        }
        
        if (parent !is PsiMethod) {
            return false
        }
        
        val method = parent
        val methodBody = method.body ?: return false
        
        // 查找 try-finally 语句
        var hasFinallyBlock = false
        var closedInFinally = false
        
        methodBody.accept(object : JavaRecursiveElementVisitor() {
            override fun visitTryStatement(statement: PsiTryStatement) {
                super.visitTryStatement(statement)
                
                val finallyBlock = statement.finallyBlock
                if (finallyBlock != null) {
                    hasFinallyBlock = true
                    
                    // 检查 finally 块中是否调用了 close()
                    finallyBlock.accept(object : JavaRecursiveElementVisitor() {
                        override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
                            super.visitMethodCallExpression(expression)
                            
                            val methodExpr = expression.methodExpression
                            val qualifier = methodExpr.qualifierExpression
                            
                            if (qualifier is PsiReferenceExpression) {
                                val resolved = qualifier.resolve()
                                if (resolved == variable && methodExpr.referenceName == "close") {
                                    closedInFinally = true
                                }
                            }
                        }
                    })
                }
            }
        })
        
        // 如果没有 finally 块，或者 finally 块中没有 close()，返回 false
        return hasFinallyBlock && closedInFinally
    }
    
    override fun getProblemDescription(element: PsiElement): String {
        // 区分两种情况
        if (element is PsiMethodCallExpression) {
            // 使用了 closeQuietly
            return """
                ⚠️ 不推荐：使用 IOUtils.closeQuietly() 关闭资源
                
                【问题】closeQuietly() 会吞掉异常，可能隐藏真正的问题
                
                【推荐做法】
                1. 使用 try-with-resources（Java 7+）
                   try (InputStream in = new FileInputStream(file)) {
                       // 使用资源
                   } // 自动关闭
                
                2. 在 finally 中显式关闭
                   InputStream in = null;
                   try {
                       in = new FileInputStream(file);
                       // 使用资源
                   } finally {
                       if (in != null) {
                           in.close(); // 显式关闭，异常会被抛出
                       }
                   }
                
                【为什么不用 closeQuietly】
                - 吞掉了 close() 时的异常
                - 可能隐藏磁盘满、网络断开等问题
                - 不符合"快速失败"原则
            """.trimIndent()
        } else {
            // 资源未正确关闭
            return """
                ⚠️ 资源泄漏风险：资源未在 finally 中正确关闭
                
                【问题】资源对象未正确关闭可能导致：
                - 文件句柄泄漏
                - 数据库连接池耗尽
                - 内存泄漏
                - Socket 连接泄漏
                
                【推荐做法】
                1. 【最推荐】使用 try-with-resources（Java 7+）
                   try (InputStream in = new FileInputStream(file)) {
                       // 使用资源
                   } // 自动关闭，异常安全
                
                2. 在 finally 中显式关闭
                   InputStream in = null;
                   try {
                       in = new FileInputStream(file);
                       // 使用资源
                   } finally {
                       if (in != null) {
                           in.close();
                       }
                   }
                
                【注意】
                - 不要使用 IOUtils.closeQuietly()（会吞掉异常）
                - 必须在 finally 中关闭（确保异常时也能关闭）
                - 推荐使用 try-with-resources（自动管理）
            """.trimIndent()
        }
    }
    
    override fun getQuickFixes(element: PsiElement): Array<LocalQuickFix> {
        return emptyArray()
    }
    
    override fun getSecurityAdvice(): String {
        return """
            【资源管理最佳实践】
            
            1. 【强烈推荐】使用 try-with-resources（Java 7+）
            
            // ✅ 最佳实践
            try (InputStream in = new FileInputStream(file);
                 OutputStream out = new FileOutputStream(targetFile)) {
                // 使用资源
                byte[] buffer = new byte[1024];
                int len;
                while ((len = in.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                }
            } // 自动按声明相反顺序关闭，异常安全
            
            2. 【传统方式】在 finally 中显式关闭
            
            // ✅ 正确的传统写法
            InputStream in = null;
            try {
                in = new FileInputStream(file);
                // 使用资源
            } catch (IOException e) {
                // 处理异常
                throw e;
            } finally {
                // 必须在 finally 中关闭
                if (in != null) {
                    try {
                        in.close(); // 显式关闭，不吞异常
                    } catch (IOException e) {
                        // 记录日志
                        log.error("关闭资源失败", e);
                    }
                }
            }
            
            3. 【不推荐】使用 closeQuietly
            
            // ❌ 不推荐：吞掉异常
            InputStream in = null;
            try {
                in = new FileInputStream(file);
                // 使用资源
            } finally {
                IOUtils.closeQuietly(in); // 不好：异常被吞掉了
            }
            
            问题：
            - closeQuietly() 会吞掉 close() 时的异常
            - 可能隐藏磁盘满、网络断开等严重问题
            - 不符合"快速失败"原则
            
            4. 【数据库资源管理】
            
            // ✅ 正确写法：使用 try-with-resources
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql);
                 ResultSet rs = pstmt.executeQuery()) {
                
                while (rs.next()) {
                    // 处理结果
                }
            } // 自动关闭，按相反顺序：rs -> pstmt -> conn
            
            // ❌ 错误写法：没有关闭
            Connection conn = dataSource.getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            // 忘记关闭，导致连接泄漏
            
            5. 【Hutool 工具类】
            
            // 使用 Hutool 的 IoUtil
            import cn.hutool.core.io.IoUtil;
            
            InputStream in = null;
            try {
                in = new FileInputStream(file);
                String content = IoUtil.readUtf8(in);
            } finally {
                IoUtil.close(in); // Hutool 的 close 会正确处理异常
            }
            
            // 或者更简单
            String content = FileUtil.readUtf8String(file); // Hutool 自动管理资源
            
            【常见需要关闭的资源】
            - InputStream / OutputStream
            - Reader / Writer
            - Connection / Statement / ResultSet
            - Socket / ServerSocket
            - Scanner
            - RandomAccessFile
            - ZipFile
            - FileChannel
            
            【资源泄漏的危害】
            1. 文件句柄泄漏 → 无法打开新文件
            2. 数据库连接泄漏 → 连接池耗尽，服务不可用
            3. Socket 泄漏 → 端口耗尽
            4. 内存泄漏 → OOM
            
            【检查清单】
            ✓ 所有资源对象都用 try-with-resources
            ✓ 或者在 finally 中显式 close()
            ✓ 不使用 closeQuietly()
            ✓ 数据库资源按相反顺序关闭
            ✓ 异常处理正确
        """.trimIndent()
    }
}

