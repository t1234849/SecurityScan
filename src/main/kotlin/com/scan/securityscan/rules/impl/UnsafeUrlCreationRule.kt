package com.scan.securityscan.rules.impl

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.psi.*
import com.scan.securityscan.rules.AbstractSecurityRule
import com.scan.securityscan.rules.RiskLevel

/**
 * 不安全的URL创建规则
 * 检测 new URL(userInput) 这种可能导致 SSRF 的写法
 */
class UnsafeUrlCreationRule : AbstractSecurityRule() {
    
    override val ruleId = "UNSAFE_URL_CREATION"
    override val ruleName = "不安全的URL创建"
    override val description = "直接使用外部输入创建URL可能导致SSRF攻击"
    override val severity = RiskLevel.ERROR
    
    override fun matches(element: PsiElement): Boolean {
        // 只检查 new 表达式
        if (element !is PsiNewExpression) {
            return false
        }
        
        // 检查是否是 new URL(...)
        val classReference = element.classReference ?: return false
        val className = classReference.qualifiedName ?: return false
        
        if (className != "java.net.URL") {
            return false
        }
        
        // 检查参数
        val argumentList = element.argumentList ?: return false
        val arguments = argumentList.expressions
        
        if (arguments.isEmpty()) {
            return false
        }
        
        // 如果第一个参数不是字面量（即可能来自外部输入），则认为不安全
        val firstArg = arguments[0]
        return !isLiteralOrConstant(firstArg)
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
            ⚠️ 安全风险：SSRF（服务端请求伪造）攻击
            
            【问题】直接使用外部输入创建 URL，攻击者可能访问内网资源或发起攻击
            
            【修复建议】
            1. 【推荐】使用 Hutool 的 URLUtil（自动校验）
               // 需要依赖：cn.hutool:hutool-core
               URL url = cn.hutool.core.util.URLUtil.url(userInput);
               // URLUtil 会自动进行安全校验和格式化
            
            2. URL 白名单校验
               String[] allowedDomains = {"example.com", "api.example.com"};
               if (!isValidUrl(userInput, allowedDomains)) {
                   throw new SecurityException("Invalid URL");
               }
            
            3. 协议限制（只允许 http/https）
               URL url = new URL(userInput);
               if (!url.getProtocol().matches("^https?$")) {
                   throw new SecurityException("Invalid protocol");
               }
            
            4. 禁止访问内网地址
               - 127.0.0.1, localhost
               - 10.0.0.0/8, 172.16.0.0/12, 192.168.0.0/16
               - 169.254.169.254 (云服务器元数据)
            
            【攻击示例】
            输入: file:///etc/passwd 或 http://169.254.169.254/latest/meta-data/
            结果: 读取本地文件或云服务器敏感信息
        """.trimIndent()
    }
    
    override fun getQuickFixes(element: PsiElement): Array<LocalQuickFix> {
        // 不提供自动修复，避免类型不匹配等问题
        return emptyArray()
    }
    
    override fun getSecurityAdvice(): String {
        return """
            【修复建议】
            1. 【推荐】使用 Hutool 的 URLUtil 工具类
            2. 对 URL 进行白名单校验，只允许访问特定的域名或IP
            3. 禁止使用 file://、gopher:// 等危险协议
            4. 禁止访问内网地址和云服务器元数据接口
            
            【示例代码】
            
            // ❌ 不安全的写法
            String urlStr = request.getParameter("url");
            URL url = new URL(urlStr);  // 危险！可能导致 SSRF
            
            // ✅ 安全的写法 1：使用 Hutool URLUtil（推荐）
            import cn.hutool.core.util.URLUtil;
            
            String urlStr = request.getParameter("url");
            try {
                // URLUtil 会进行安全校验和格式化
                URL url = URLUtil.url(urlStr);
                
                // 额外检查：确保是 http/https 协议
                if (!url.getProtocol().matches("^https?$")) {
                    throw new SecurityException("只允许 http/https 协议");
                }
                
                // 额外检查：域名白名单（根据业务需要）
                if (!isAllowedDomain(url.getHost())) {
                    throw new SecurityException("域名不在白名单中");
                }
                
                // 使用该 URL
                URLConnection conn = url.openConnection();
                
            } catch (Exception e) {
                throw new SecurityException("无效的 URL", e);
            }
            
            // ✅ 安全的写法 2：手动白名单校验
            String urlStr = request.getParameter("url");
            if (!isValidUrl(urlStr)) {
                throw new SecurityException("Invalid URL");
            }
            URL url = new URL(urlStr);
            
            private boolean isValidUrl(String urlStr) {
                // 白名单：只允许特定域名
                String[] allowedDomains = {"example.com", "api.example.com"};
                
                try {
                    URL url = new URL(urlStr);
                    String protocol = url.getProtocol();
                    String host = url.getHost();
                    
                    // 1. 只允许 http 和 https 协议
                    if (!protocol.equals("http") && !protocol.equals("https")) {
                        return false;
                    }
                    
                    // 2. 禁止访问内网地址
                    if (isInternalAddress(host)) {
                        return false;
                    }
                    
                    // 3. 域名白名单检查
                    for (String domain : allowedDomains) {
                        if (host.equals(domain) || host.endsWith("." + domain)) {
                            return true;
                        }
                    }
                    
                    return false;
                    
                } catch (Exception e) {
                    return false;
                }
            }
            
            private boolean isInternalAddress(String host) {
                // 检查是否是内网地址
                if (host.equals("localhost") || host.equals("127.0.0.1")) {
                    return true;
                }
                
                // 检查私有网段
                if (host.startsWith("10.") || 
                    host.startsWith("192.168.") ||
                    host.startsWith("172.")) {
                    return true;
                }
                
                // 检查云服务器元数据地址
                if (host.equals("169.254.169.254")) {
                    return true;
                }
                
                return false;
            }
            
            【Maven 依赖】
            <!-- Hutool 工具库 -->
            <dependency>
                <groupId>cn.hutool</groupId>
                <artifactId>hutool-core</artifactId>
                <version>5.8.23</version>
            </dependency>
            
            【Gradle 依赖】
            implementation 'cn.hutool:hutool-core:5.8.23'
            
            【国内审计要求】
            - 奇安信：SSRF 是重点检查项
            - 等保测评：需要严格限制外部请求
            - OWASP Top 10: A10:2021 – SSRF
        """.trimIndent()
    }
}
