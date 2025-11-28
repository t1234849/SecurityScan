import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;

/**
 * 安全扫描测试示例
 * 
 * 这个文件包含了各种不安全的代码写法，用于测试插件的检测能力。
 * 打开这个文件，你应该能看到多处安全警告。
 */
public class SecurityTestExamples {
    
    // ============================================
    // 测试1：Fastjson 反序列化漏洞（高危）
    // ============================================
    
    /**
     * 危险：直接使用 parseObject 反序列化用户输入
     * 风险：可能导致远程代码执行（RCE）
     */
    public void unsafeFastjsonParseObject(String userInput) {
        // 🔥 这里应该显示错误提示
        JSONObject obj = JSON.parseObject(userInput);
        System.out.println(obj);
    }
    
    /**
     * 危险：使用 parse 方法
     */
    public void unsafeFastjsonParse(String userInput) {
        // 🔥 这里应该显示错误提示
        Object obj = JSON.parse(userInput);
        System.out.println(obj);
    }
    
    /**
     * 极度危险：启用了 AutoType
     */
    public void unsafeFastjsonWithAutoType(String userInput) {
        // 🔥🔥🔥 这里应该显示严重错误提示
        JSONObject obj = JSON.parseObject(userInput, Feature.SupportAutoType);
        System.out.println(obj);
    }
    
    /**
     * 安全：使用 Jackson（推荐）
     */
    public void safeFastjsonAlternative(String jsonInput) throws Exception {
        // ✅ 安全的写法
        com.fasterxml.jackson.databind.ObjectMapper mapper = 
            new com.fasterxml.jackson.databind.ObjectMapper();
        Object obj = mapper.readValue(jsonInput, Object.class);
    }
    
    // ============================================
    // 测试2：SQL 注入漏洞（高危）
    // ============================================
    
    /**
     * 危险：字符串拼接构造 SQL
     * 风险：SQL 注入攻击
     */
    public void unsafeSqlConcatenation(Connection conn, String username, String password) 
            throws SQLException {
        // 🔥 这里应该显示错误提示
        String sql = "SELECT * FROM users WHERE username = '" + username + 
                     "' AND password = '" + password + "'";
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(sql);
    }
    
    /**
     * 危险：使用 executeUpdate 执行拼接的 SQL
     */
    public void unsafeSqlUpdate(Connection conn, String userId, String newEmail) 
            throws SQLException {
        // 🔥 这里应该显示错误提示
        String sql = "UPDATE users SET email = '" + newEmail + 
                     "' WHERE id = " + userId;
        Statement stmt = conn.createStatement();
        stmt.executeUpdate(sql);
    }
    
    /**
     * 安全：使用 PreparedStatement
     */
    public void safeSqlQuery(Connection conn, String username, String password) 
            throws SQLException {
        // ✅ 安全的写法
        String sql = "SELECT * FROM users WHERE username = ? AND password = ?";
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setString(1, username);
        pstmt.setString(2, password);
        ResultSet rs = pstmt.executeQuery();
    }
    
    // ============================================
    // 测试3：路径遍历漏洞
    // ============================================
    
    /**
     * 危险：直接使用用户输入创建文件对象
     * 风险：路径遍历攻击，攻击者可能访问系统任意文件
     */
    public void unsafeFileCreation(String userFileName) {
        // ⚠️ 这里应该显示警告提示
        File file = new File("/uploads/" + userFileName);
        // 如果 userFileName = "../../etc/passwd"，则可以访问系统敏感文件
    }
    
    /**
     * 危险：使用 Paths.get
     */
    public void unsafePathsGet(String userPath) {
        // ⚠️ 这里应该显示警告提示
        Path path = Paths.get("/data/" + userPath);
    }
    
    /**
     * 危险：使用 Path.of
     */
    public void unsafePathOf(String userPath) {
        // ⚠️ 这里应该显示警告提示
        Path path = Path.of("/data/" + userPath);
    }
    
    /**
     * 安全：使用 FileUtil（Hutool）
     */
    public void safeFileCreation(String userFileName) {
        // ✅ 安全的写法（需要添加 hutool-core 依赖）
        // File file = cn.hutool.core.io.FileUtil.file("/uploads/", userFileName);
    }
    
    /**
     * 安全：手动进行路径校验
     */
    public void safeFileCreationWithValidation(String userFileName) throws Exception {
        // ✅ 安全的写法
        String basePath = "/uploads/";
        Path path = Paths.get(basePath, userFileName).normalize();
        
        // 确保在允许的目录内
        if (!path.startsWith(Paths.get(basePath).normalize())) {
            throw new SecurityException("Path traversal detected");
        }
        
        File file = path.toFile();
    }
    
    // ============================================
    // 测试4：SSRF（服务端请求伪造）
    // ============================================
    
    /**
     * 危险：直接使用用户输入创建 URL
     * 风险：SSRF 攻击，攻击者可能访问内网资源
     */
    public void unsafeUrlCreation(String userUrl) throws Exception {
        // ⚠️ 这里应该显示警告提示
        URL url = new URL(userUrl);
        // 攻击者可能输入：
        // - http://localhost:6379/ (访问内网 Redis)
        // - file:///etc/passwd (读取本地文件)
        // - http://169.254.169.254/latest/meta-data/ (云服务器元数据)
    }
    
    /**
     * 安全：使用 Hutool URLUtil（推荐）
     */
    public void safeUrlCreationWithHutool(String userUrl) throws Exception {
        // ✅ 安全的写法：使用 Hutool URLUtil
        // 需要依赖：cn.hutool:hutool-core:5.8.23
        
        // URLUtil 会自动进行安全校验和格式化
        URL url = cn.hutool.core.util.URLUtil.url(userUrl);
        
        // 额外检查：确保是 http/https 协议
        if (!url.getProtocol().matches("^https?$")) {
            throw new SecurityException("只允许 http/https 协议");
        }
        
        // 额外检查：域名白名单（根据业务需要）
        if (!isAllowedDomain(url.getHost())) {
            throw new SecurityException("域名不在白名单中");
        }
    }
    
    /**
     * 安全：使用 URL 白名单校验（传统方式）
     */
    public void safeUrlCreationWithValidation(String userUrl) throws Exception {
        // ✅ 安全的写法：手动白名单校验
        if (!isValidUrl(userUrl)) {
            throw new SecurityException("Invalid URL");
        }
        URL url = new URL(userUrl);
    }
    
    /**
     * URL 白名单校验示例
     */
    private boolean isValidUrl(String urlStr) {
        String[] allowedDomains = {"example.com", "api.example.com"};
        try {
            URL url = new URL(urlStr);
            String protocol = url.getProtocol();
            String host = url.getHost();
            
            // 只允许 http 和 https
            if (!protocol.equals("http") && !protocol.equals("https")) {
                return false;
            }
            
            // 禁止访问内网地址
            if (isInternalAddress(host)) {
                return false;
            }
            
            // 域名白名单检查
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
    
    /**
     * 检查是否是内网地址
     */
    private boolean isInternalAddress(String host) {
        // localhost / 127.0.0.1
        if (host.equals("localhost") || host.equals("127.0.0.1")) {
            return true;
        }
        
        // 私有网段
        if (host.startsWith("10.") || 
            host.startsWith("192.168.") ||
            host.startsWith("172.")) {
            return true;
        }
        
        // 云服务器元数据地址
        if (host.equals("169.254.169.254")) {
            return true;
        }
        
        return false;
    }
    
    /**
     * 域名白名单检查
     */
    private boolean isAllowedDomain(String host) {
        String[] allowedDomains = {"example.com", "api.example.com"};
        for (String domain : allowedDomains) {
            if (host.equals(domain) || host.endsWith("." + domain)) {
                return true;
            }
        }
        return false;
    }
    
    // ============================================
    // 测试5：Java 原生反序列化漏洞
    // ============================================
    
    /**
     * 危险：使用 ObjectInputStream 反序列化不受信任的数据
     * 风险：远程代码执行（RCE）
     */
    public void unsafeJavaDeserialization(java.io.InputStream inputStream) throws Exception {
        // 🔥 这里应该显示错误提示
        java.io.ObjectInputStream ois = new java.io.ObjectInputStream(inputStream);
        Object obj = ois.readObject();
        // 攻击者可以通过 gadget chain 执行任意代码
    }
    
    /**
     * 危险：使用 XMLDecoder
     */
    public void unsafeXmlDecoder(java.io.InputStream inputStream) {
        // 🔥 这里应该显示错误提示
        java.beans.XMLDecoder decoder = new java.beans.XMLDecoder(inputStream);
        Object obj = decoder.readObject();
    }
    
    /**
     * 安全：使用 JSON 序列化替代
     */
    public void safeDeserialization(String jsonString) throws Exception {
        // ✅ 安全的写法
        com.fasterxml.jackson.databind.ObjectMapper mapper = 
            new com.fasterxml.jackson.databind.ObjectMapper();
        Object obj = mapper.readValue(jsonString, Object.class);
    }
    
    // ============================================
    // 测试6：命令注入漏洞
    // ============================================
    
    /**
     * 危险：使用字符串拼接执行系统命令
     * 风险：命令注入攻击
     */
    public void unsafeCommandExecution(String fileName) throws Exception {
        // 🔥 这里应该显示错误提示
        String command = "cat " + fileName;
        Process process = Runtime.getRuntime().exec(command);
        // 如果 fileName = "test.txt; rm -rf /"，将删除系统文件
    }
    
    /**
     * 危险：使用 ProcessBuilder 但参数是拼接的
     */
    public void unsafeProcessBuilder(String userInput) throws Exception {
        // 🔥 这里应该显示错误提示
        String command = "ls " + userInput;
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.start();
    }
    
    /**
     * 危险：通过 shell 执行拼接的命令
     */
    public void unsafeShellCommand(String fileName) throws Exception {
        // 🔥🔥 更危险：通过 shell 执行
        String[] cmd = {"sh", "-c", "cat " + fileName};
        Runtime.getRuntime().exec(cmd);
    }
    
    /**
     * 安全：使用参数数组形式
     */
    public void safeCommandExecution(String fileName) throws Exception {
        // ✅ 安全的写法：使用参数数组
        String[] cmd = {"cat", fileName};
        Process process = Runtime.getRuntime().exec(cmd);
    }
    
    /**
     * 安全：使用 ProcessBuilder 参数列表
     */
    public void safeProcessBuilder(String fileName) throws Exception {
        // ✅ 安全的写法
        ProcessBuilder pb = new ProcessBuilder("cat", fileName);
        Process process = pb.start();
    }
    
    /**
     * 最佳：使用 Java API 替代系统命令
     */
    public void bestPractice(String fileName) throws Exception {
        // ✅ 最佳实践：使用 Java API 而不是系统命令
        Path path = Paths.get(fileName);
        byte[] content = java.nio.file.Files.readAllBytes(path);
        // 完全避免了命令执行
    }
    
    // ============================================
    // 复杂场景测试
    // ============================================
    
    /**
     * 复杂场景：在 Web 应用中的典型不安全代码
     */
    public void webApplicationUnsafeExample(
            String username,      // 来自用户输入
            String fileName,      // 来自用户上传
            String jsonData,      // 来自 API 请求
            String redirectUrl,   // 来自请求参数
            Connection conn
    ) throws Exception {
        
        // 1. SQL 注入风险
        String sql = "SELECT * FROM users WHERE username = '" + username + "'";
        Statement stmt = conn.createStatement();
        stmt.executeQuery(sql);
        
        // 2. Fastjson 反序列化风险
        JSONObject config = JSON.parseObject(jsonData);
        
        // 3. 路径遍历风险
        File uploadFile = new File("/uploads/" + fileName);
        
        // 4. SSRF 风险
        URL callback = new URL(redirectUrl);
        
        // 以上所有代码都应该被标记为不安全！
    }
    
    /**
     * 安全的替代实现
     */
    public void webApplicationSafeExample(
            String username,
            String fileName,
            String jsonData,
            String redirectUrl,
            Connection conn
    ) throws Exception {
        
        // 1. 使用 PreparedStatement
        String sql = "SELECT * FROM users WHERE username = ?";
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setString(1, username);
        pstmt.executeQuery();
        
        // 2. 使用 Jackson 或 Gson
        com.fasterxml.jackson.databind.ObjectMapper mapper = 
            new com.fasterxml.jackson.databind.ObjectMapper();
        Object config = mapper.readValue(jsonData, Object.class);
        
        // 3. 使用路径校验
        Path uploadPath = Paths.get("/uploads/", fileName).normalize();
        if (!uploadPath.startsWith("/uploads/")) {
            throw new SecurityException("Invalid path");
        }
        File uploadFile = uploadPath.toFile();
        
        // 4. 使用 URL 白名单
        if (!isValidUrl(redirectUrl)) {
            throw new SecurityException("Invalid URL");
        }
        URL callback = new URL(redirectUrl);
    }
}

