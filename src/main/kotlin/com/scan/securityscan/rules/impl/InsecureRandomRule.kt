package com.scan.securityscan.rules.impl

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.psi.*
import com.scan.securityscan.rules.AbstractSecurityRule
import com.scan.securityscan.rules.RiskLevel

/**
 * 不安全的随机数生成规则
 * 检测使用 java.util.Random 而不是 SecureRandom 的情况
 */
class InsecureRandomRule : AbstractSecurityRule() {
    
    override val ruleId = "INSECURE_RANDOM"
    override val ruleName = "不安全的随机数"
    override val description = "使用 Random 生成随机数不适用于安全场景"
    override val severity = RiskLevel.WARNING
    
    override fun matches(element: PsiElement): Boolean {
        // 只检查 new 表达式
        if (element !is PsiNewExpression) {
            return false
        }
        
        // 检查是否是 new Random()
        val classReference = element.classReference ?: return false
        val className = classReference.qualifiedName ?: return false
        
        return className == "java.util.Random"
    }
    
    override fun getProblemDescription(element: PsiElement): String {
        return """
            ⚠️ 密码管理风险：使用不安全的随机数生成器
            
            【问题】java.util.Random 不是密码学安全的随机数生成器
            - 使用线性同余算法，输出可预测
            - 攻击者可以通过观察部分输出推测后续随机数
            - 不适用于生成密码、令牌、密钥等安全敏感数据
            
            【修复建议】
            1. 使用 SecureRandom（推荐）
               // ❌ 不安全：可预测
               Random random = new Random();
               String token = String.valueOf(random.nextInt());
               
               // ✅ 安全：密码学安全
               SecureRandom secureRandom = new SecureRandom();
               byte[] tokenBytes = new byte[16];
               secureRandom.nextBytes(tokenBytes);
               String token = Base64.getEncoder().encodeToString(tokenBytes);
            
            2. 使用 UUID（简单场景）
               // ✅ 适用于生成唯一标识符
               String token = UUID.randomUUID().toString();
            
            【使用场景对比】
            ✅ Random 适用于：
               - 游戏随机数
               - UI 动画效果
               - 非安全相关的随机选择
            
            ❌ Random 不适用于：
               - 生成密码
               - 生成会话令牌（Session Token）
               - 生成 CSRF Token
               - 生成 API Key
               - 生成加密密钥
               - 生成验证码
            
            【为什么 Random 不安全】
            Random 使用线性同余公式：
            seed(n+1) = (seed(n) * multiplier + addend) mod 2^48
            
            攻击者如果观察到连续几个随机数，可以推算出种子值，
            从而预测所有后续的随机数。
        """.trimIndent()
    }
    
    override fun getQuickFixes(element: PsiElement): Array<LocalQuickFix> {
        return emptyArray()
    }
    
    override fun getSecurityAdvice(): String {
        return """
            【密码学安全随机数最佳实践】
            
            1. 【推荐】使用 SecureRandom 生成安全令牌
            
            import java.security.SecureRandom;
            import java.util.Base64;
            
            // ✅ 生成安全的随机令牌
            public String generateSecureToken() {
                SecureRandom secureRandom = new SecureRandom();
                byte[] tokenBytes = new byte[32];  // 256 bits
                secureRandom.nextBytes(tokenBytes);
                return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
            }
            
            2. 【推荐】生成随机密码
            
            public String generateRandomPassword(int length) {
                SecureRandom secureRandom = new SecureRandom();
                String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#${'$'}%^&*";
                StringBuilder password = new StringBuilder(length);
                
                for (int i = 0; i < length; i++) {
                    int index = secureRandom.nextInt(chars.length());
                    password.append(chars.charAt(index));
                }
                
                return password.toString();
            }
            
            3. 【推荐】生成 Session ID
            
            public String generateSessionId() {
                SecureRandom secureRandom = new SecureRandom();
                byte[] sessionBytes = new byte[32];
                secureRandom.nextBytes(sessionBytes);
                
                // 转换为十六进制字符串
                StringBuilder sessionId = new StringBuilder();
                for (byte b : sessionBytes) {
                    sessionId.append(String.format("%02x", b));
                }
                
                return sessionId.toString();
            }
            
            4. 【推荐】生成 CSRF Token
            
            public String generateCsrfToken() {
                SecureRandom secureRandom = new SecureRandom();
                byte[] tokenBytes = new byte[32];
                secureRandom.nextBytes(tokenBytes);
                return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
            }
            
            5. 【简单场景】使用 UUID
            
            import java.util.UUID;
            
            // ✅ 适用于生成唯一标识符
            public String generateUniqueId() {
                return UUID.randomUUID().toString();
            }
            
            // ✅ 去掉横线的 UUID
            public String generateShortId() {
                return UUID.randomUUID().toString().replace("-", "");
            }
            
            6. 【Spring Security】使用内置工具
            
            import org.springframework.security.crypto.keygen.KeyGenerators;
            
            // ✅ Spring Security 提供的安全随机数生成器
            public String generateToken() {
                // 生成 256 位随机数
                String token = KeyGenerators.string().generateKey();
                return token;
            }
            
            7. 【Apache Commons】使用 RandomStringUtils
            
            import org.apache.commons.lang3.RandomStringUtils;
            
            // ⚠️ 注意：默认使用 Random，不安全
            String insecure = RandomStringUtils.randomAlphanumeric(32);
            
            // ✅ 使用 SecureRandom 版本
            SecureRandom secureRandom = new SecureRandom();
            String secure = RandomStringUtils.random(32, 0, 0, true, true, null, secureRandom);
            
            8. 【Hutool】使用 RandomUtil
            
            import cn.hutool.core.util.RandomUtil;
            
            // ⚠️ 注意：RandomUtil 默认使用 Random
            String randomStr = RandomUtil.randomString(32);  // 不安全
            
            // ✅ 使用 SecureRandom
            SecureRandom secureRandom = new SecureRandom();
            String secureStr = RandomUtil.randomString(
                "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789", 
                32, 
                secureRandom
            );
            
            【错误示例】
            
            // ❌ 不安全：使用 Random 生成密码
            Random random = new Random();
            public String generatePassword() {
                return String.valueOf(random.nextInt(999999));  // 可预测！
            }
            
            // ❌ 不安全：使用时间戳作为种子
            Random random = new Random(System.currentTimeMillis());  // 容易被猜测
            
            // ❌ 不安全：使用 Math.random()
            int code = (int)(Math.random() * 999999);  // 内部使用 Random
            
            【正确示例】
            
            // ✅ 生成 6 位数字验证码
            public String generateVerificationCode() {
                SecureRandom secureRandom = new SecureRandom();
                int code = secureRandom.nextInt(900000) + 100000;  // 100000-999999
                return String.valueOf(code);
            }
            
            // ✅ 生成加密密钥
            public byte[] generateEncryptionKey() {
                SecureRandom secureRandom = new SecureRandom();
                byte[] key = new byte[32];  // 256-bit key
                secureRandom.nextBytes(key);
                return key;
            }
            
            // ✅ 生成盐值（用于密码哈希）
            public byte[] generateSalt() {
                SecureRandom secureRandom = new SecureRandom();
                byte[] salt = new byte[16];
                secureRandom.nextBytes(salt);
                return salt;
            }
            
            【性能考虑】
            
            SecureRandom 比 Random 慢，但对于安全场景，这点性能损失是值得的：
            - Random: ~10 纳秒/次
            - SecureRandom: ~50-100 微秒/次（首次）
            - 后续调用会使用缓存，性能提升
            
            如果需要大量随机数：
            1. 安全场景：仍然使用 SecureRandom
            2. 非安全场景：可以使用 Random
            
            【检查清单】
            ✓ 密码生成：使用 SecureRandom
            ✓ 令牌生成：使用 SecureRandom
            ✓ Session ID：使用 SecureRandom
            ✓ CSRF Token：使用 SecureRandom
            ✓ API Key：使用 SecureRandom
            ✓ 验证码：使用 SecureRandom
            ✓ 加密密钥：使用 SecureRandom
            ✓ 盐值：使用 SecureRandom
            ✓ 游戏随机：可以使用 Random
            ✓ UI 动画：可以使用 Random
            
            【国内审计要求】
            - 奇安信：密码、令牌等必须使用密码学安全的随机数
            - 等保测评：敏感数据生成需要使用安全的随机数生成器
            - 所有与安全相关的随机数都应使用 SecureRandom
        """.trimIndent()
    }
}

