import java.security.SecureRandom;
import java.util.Base64;
import java.util.Random;
import java.util.UUID;

/**
 * 不安全的随机数测试示例
 * 
 * 演示正确和错误的随机数使用方式
 */
public class InsecureRandomExamples {
    
    // ============================================
    // 错误示例：使用 Random 生成安全敏感数据
    // ============================================
    
    /**
     * ❌ 错误：使用 Random 生成密码
     * 问题：可预测，攻击者可以推测出密码
     */
    public String generatePasswordInsecure() {
        // ⚠️ 这里会显示黄色波浪线
        Random random = new Random();
        return String.valueOf(random.nextInt(999999));
    }
    
    /**
     * ❌ 错误：使用 Random 生成 Session ID
     * 问题：攻击者可以预测其他用户的 Session ID
     */
    public String generateSessionIdInsecure() {
        // ⚠️ 这里会显示黄色波浪线
        Random random = new Random();
        return String.valueOf(random.nextLong());
    }
    
    /**
     * ❌ 错误：使用 Random 生成令牌
     * 问题：攻击者可以伪造令牌
     */
    public String generateTokenInsecure() {
        // ⚠️ 这里会显示黄色波浪线
        Random random = new Random();
        byte[] tokenBytes = new byte[32];
        random.nextBytes(tokenBytes);
        return Base64.getEncoder().encodeToString(tokenBytes);
    }
    
    /**
     * ❌ 错误：使用时间戳作为种子
     * 问题：时间戳容易被猜测
     */
    public String generateKeyInsecure() {
        // ⚠️ 这里会显示黄色波浪线
        Random random = new Random(System.currentTimeMillis());
        return String.valueOf(random.nextLong());
    }
    
    /**
     * ❌ 错误：使用 Random 生成验证码
     * 问题：攻击者可以暴力破解或预测
     */
    public String generateVerificationCodeInsecure() {
        // ⚠️ 这里会显示黄色波浪线
        Random random = new Random();
        int code = random.nextInt(900000) + 100000;  // 100000-999999
        return String.valueOf(code);
    }
    
    /**
     * ❌ 错误：使用 Math.random()
     * 问题：Math.random() 内部使用 Random，也不安全
     */
    public int generateCodeWithMathRandom() {
        // Math.random() 也不安全，但插件暂不检测
        return (int)(Math.random() * 999999);
    }
    
    // ============================================
    // 正确示例 1：使用 SecureRandom
    // ============================================
    
    /**
     * ✅ 正确：使用 SecureRandom 生成密码
     */
    public String generatePasswordSecure() {
        SecureRandom secureRandom = new SecureRandom();
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
        StringBuilder password = new StringBuilder(12);
        
        for (int i = 0; i < 12; i++) {
            int index = secureRandom.nextInt(chars.length());
            password.append(chars.charAt(index));
        }
        
        return password.toString();
    }
    
    /**
     * ✅ 正确：使用 SecureRandom 生成 Session ID
     */
    public String generateSessionIdSecure() {
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
    
    /**
     * ✅ 正确：使用 SecureRandom 生成令牌
     */
    public String generateTokenSecure() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] tokenBytes = new byte[32];  // 256 bits
        secureRandom.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }
    
    /**
     * ✅ 正确：使用 SecureRandom 生成 CSRF Token
     */
    public String generateCsrfToken() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] tokenBytes = new byte[32];
        secureRandom.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }
    
    /**
     * ✅ 正确：使用 SecureRandom 生成验证码
     */
    public String generateVerificationCodeSecure() {
        SecureRandom secureRandom = new SecureRandom();
        int code = secureRandom.nextInt(900000) + 100000;  // 100000-999999
        return String.valueOf(code);
    }
    
    /**
     * ✅ 正确：使用 SecureRandom 生成 API Key
     */
    public String generateApiKey() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] keyBytes = new byte[32];
        secureRandom.nextBytes(keyBytes);
        
        // 转换为十六进制
        StringBuilder apiKey = new StringBuilder();
        for (byte b : keyBytes) {
            apiKey.append(String.format("%02x", b));
        }
        
        return apiKey.toString();
    }
    
    /**
     * ✅ 正确：使用 SecureRandom 生成加密密钥
     */
    public byte[] generateEncryptionKey() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] key = new byte[32];  // 256-bit key
        secureRandom.nextBytes(key);
        return key;
    }
    
    /**
     * ✅ 正确：使用 SecureRandom 生成盐值（用于密码哈希）
     */
    public byte[] generateSalt() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] salt = new byte[16];
        secureRandom.nextBytes(salt);
        return salt;
    }
    
    // ============================================
    // 正确示例 2：使用 UUID
    // ============================================
    
    /**
     * ✅ 正确：使用 UUID 生成唯一标识符
     * 适用于不需要高度安全性的场景
     */
    public String generateUniqueId() {
        return UUID.randomUUID().toString();
    }
    
    /**
     * ✅ 正确：使用 UUID 生成短ID（去掉横线）
     */
    public String generateShortId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
    
    // ============================================
    // 正确示例 3：Random 的正确使用场景
    // ============================================
    
    /**
     * ✅ 正确：Random 用于游戏逻辑
     * Random 适用于非安全场景
     */
    public int rollDice() {
        Random random = new Random();  // 游戏场景可以使用 Random
        return random.nextInt(6) + 1;  // 1-6
    }
    
    /**
     * ✅ 正确：Random 用于随机选择（非安全）
     */
    public String getRandomColor() {
        Random random = new Random();
        String[] colors = {"Red", "Green", "Blue", "Yellow"};
        return colors[random.nextInt(colors.length)];
    }
    
    /**
     * ✅ 正确：Random 用于 UI 动画
     */
    public int getRandomDelay() {
        Random random = new Random();
        return random.nextInt(1000) + 500;  // 500-1500ms
    }
    
    // ============================================
    // 实际应用示例
    // ============================================
    
    /**
     * ✅ 完整示例：用户注册时生成密码重置令牌
     */
    public String generatePasswordResetToken(String userId) {
        SecureRandom secureRandom = new SecureRandom();
        
        // 生成 32 字节随机数据
        byte[] tokenBytes = new byte[32];
        secureRandom.nextBytes(tokenBytes);
        
        // 转换为 URL 安全的 Base64 字符串
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
        
        // 存储到数据库，关联用户ID和过期时间
        // saveResetToken(userId, token, expiryTime);
        
        return token;
    }
    
    /**
     * ✅ 完整示例：生成邮箱验证码
     */
    public String generateEmailVerificationCode() {
        SecureRandom secureRandom = new SecureRandom();
        
        // 生成 6 位数字验证码
        int code = secureRandom.nextInt(900000) + 100000;
        
        // 存储到缓存，设置 5 分钟过期
        // cache.put("email_code:" + email, code, 5 * 60);
        
        return String.valueOf(code);
    }
    
    /**
     * ✅ 完整示例：生成用户初始密码
     */
    public String generateInitialPassword() {
        SecureRandom secureRandom = new SecureRandom();
        
        // 密码字符集
        String uppercase = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lowercase = "abcdefghijklmnopqrstuvwxyz";
        String numbers = "0123456789";
        String special = "!@#$%^&*";
        String allChars = uppercase + lowercase + numbers + special;
        
        StringBuilder password = new StringBuilder(12);
        
        // 确保包含各种类型的字符
        password.append(uppercase.charAt(secureRandom.nextInt(uppercase.length())));
        password.append(lowercase.charAt(secureRandom.nextInt(lowercase.length())));
        password.append(numbers.charAt(secureRandom.nextInt(numbers.length())));
        password.append(special.charAt(secureRandom.nextInt(special.length())));
        
        // 填充剩余字符
        for (int i = 4; i < 12; i++) {
            password.append(allChars.charAt(secureRandom.nextInt(allChars.length())));
        }
        
        // 打乱顺序
        for (int i = password.length() - 1; i > 0; i--) {
            int j = secureRandom.nextInt(i + 1);
            char temp = password.charAt(i);
            password.setCharAt(i, password.charAt(j));
            password.setCharAt(j, temp);
        }
        
        return password.toString();
    }
    
    /**
     * ✅ 完整示例：生成 JWT Secret Key
     */
    public String generateJwtSecretKey() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] keyBytes = new byte[64];  // 512 bits
        secureRandom.nextBytes(keyBytes);
        return Base64.getEncoder().encodeToString(keyBytes);
    }
    
    // ============================================
    // 攻击示例说明
    // ============================================
    
    /**
     * 【攻击示例】预测 Random 生成的值
     * 
     * 假设攻击者观察到以下连续的随机数：
     * 1234567890
     * 9876543210
     * 5555555555
     * 
     * 由于 Random 使用线性同余算法：
     * seed(n+1) = (seed(n) * 25214903917 + 11) mod 2^48
     * 
     * 攻击者可以通过这几个观察值反推出种子值，
     * 然后预测所有后续的随机数。
     * 
     * 这就是为什么不能用 Random 生成密码、令牌等安全数据。
     */
    
    /**
     * 【场景对比】
     * 
     * 场景                  使用 Random    使用 SecureRandom
     * --------------------------------------------------------
     * 生成密码              ❌ 危险        ✅ 安全
     * 生成 Session ID       ❌ 危险        ✅ 安全
     * 生成 CSRF Token       ❌ 危险        ✅ 安全
     * 生成 API Key          ❌ 危险        ✅ 安全
     * 生成验证码            ❌ 危险        ✅ 安全
     * 生成加密密钥          ❌ 危险        ✅ 安全
     * 生成盐值              ❌ 危险        ✅ 安全
     * 游戏随机数            ✅ 可以        ✅ 可以（但无必要）
     * UI 动画               ✅ 可以        ✅ 可以（但无必要）
     * 随机选择（非安全）    ✅ 可以        ✅ 可以（但无必要）
     * 
     * 总结：
     * - 与安全相关的场景：必须使用 SecureRandom
     * - 非安全相关的场景：可以使用 Random（性能更好）
     */
}

