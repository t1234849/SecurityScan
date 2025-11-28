import javax.servlet.http.HttpServletRequest;
import java.sql.SQLException;
import java.io.IOException;

/**
 * 系统信息泄露测试示例
 * 
 * 演示正确和错误的异常信息返回方式
 */
public class InformationLeakExamples {
    
    // ============================================
    // 错误示例：直接返回异常详细信息
    // ============================================
    
    /**
     * ❌ 错误：直接返回 e.getMessage()
     * 问题：可能暴露数据库表名、字段名、SQL 语句等
     */
    public AjaxResult dangerousReturn1(Exception e) {
        // ⚠️ 这里会显示黄色波浪线
        return AjaxResult.error(e.getMessage());
        // 可能返回：Table 'admin_user' doesn't exist
        // 暴露了数据库表名！
    }
    
    /**
     * ❌ 错误：返回 SQLException 的消息
     * 问题：暴露数据库结构和SQL语句
     */
    public AjaxResult dangerousReturn2(SQLException e) {
        // ⚠️ 这里会显示黄色波浪线
        return AjaxResult.error(e.getMessage());
        // 可能返回：Column 'admin_password' not found in table 'sys_user'
        // 暴露了表名和字段名！
    }
    
    /**
     * ❌ 错误：返回 IOException 的消息
     * 问题：暴露服务器路径和文件结构
     */
    public AjaxResult dangerousReturn3(IOException e) {
        // ⚠️ 这里会显示黄色波浪线
        return AjaxResult.error(e.getMessage());
        // 可能返回：/opt/app/config/database.properties (No such file or directory)
        // 暴露了服务器路径！
    }
    
    /**
     * ❌ 错误：在异常处理器中直接返回消息
     */
    public AjaxResult handleException(Exception e) {
        // ⚠️ 这里会显示黄色波浪线
        return AjaxResult.error(e.getMessage());
    }
    
    /**
     * ❌ 错误：使用 getLocalizedMessage()
     */
    public AjaxResult dangerousReturn4(Exception e) {
        // ⚠️ 这里会显示黄色波浪线
        return AjaxResult.error(e.getLocalizedMessage());
    }
    
    /**
     * ❌ 错误：在拼接字符串中使用
     */
    public AjaxResult dangerousReturn5(Exception e) {
        // ⚠️ 这里会显示黄色波浪线
        return AjaxResult.error("操作失败: " + e.getMessage());
        // 虽然添加了前缀，但仍然暴露了详细信息
    }
    
    // ============================================
    // 正确示例 1：返回通用错误消息
    // ============================================
    
    /**
     * ✅ 正确：返回通用错误消息
     * 不暴露任何技术细节
     */
    public AjaxResult safeReturn1(Exception e) {
        // 记录详细日志供运维排查
        log.error("操作失败", e);
        
        // 返回通用消息
        return AjaxResult.error("操作失败，请稍后重试");
    }
    
    /**
     * ✅ 正确：返回业务相关错误
     * 不含技术实现细节
     */
    public AjaxResult safeReturn2(String userId) {
        try {
            userService.deleteUser(userId);
            return AjaxResult.success("删除成功");
        } catch (Exception e) {
            log.error("删除用户失败, userId={}", userId, e);
            return AjaxResult.error("删除失败，该用户可能正在使用中");
        }
    }
    
    /**
     * ✅ 正确：针对特定异常返回业务消息
     */
    public AjaxResult safeReturn3(HttpServletRequest request, 
                                   HttpRequestMethodNotSupportedException e) {
        // 记录日志
        log.warn("不支持的请求方法: {} {}", 
                request.getMethod(), request.getRequestURI());
        
        // 返回业务相关错误，不暴露技术细节
        String message = "请求地址" + request.getRequestURI() + 
                       ",不支持" + e.getMethod() + "请求";
        return AjaxResult.error(message);
    }
    
    // ============================================
    // 正确示例 2：分类处理异常
    // ============================================
    
    /**
     * ✅ 正确：业务异常可以返回具体消息
     * 因为业务异常消息是设计好的，不含技术细节
     */
    public AjaxResult handleBusinessException(BusinessException e) {
        // 业务异常的消息是安全的
        return AjaxResult.error(e.getMessage());
        // 例如："用户名已存在"、"库存不足" 等
    }
    
    /**
     * ✅ 正确：参数校验异常返回校验信息
     */
    public AjaxResult handleValidException(MethodArgumentNotValidException e) {
        // 参数校验错误信息是安全的
        String message = e.getBindingResult()
                         .getFieldError()
                         .getDefaultMessage();
        return AjaxResult.error(message);
        // 例如："手机号格式不正确"、"密码长度必须在6-20位之间"
    }
    
    /**
     * ✅ 正确：系统异常返回通用消息
     */
    public AjaxResult handleSystemException(Exception e) {
        // 详细信息记录到日志
        log.error("系统异常", e);
        
        // 返回通用消息
        return AjaxResult.error("系统繁忙，请稍后重试");
    }
    
    // ============================================
    // 正确示例 3：使用错误码机制
    // ============================================
    
    /**
     * ✅ 正确：使用错误码
     */
    public AjaxResult safeReturnWithCode(SQLException e) {
        // 记录详细日志
        log.error("数据库操作失败", e);
        
        // 返回错误码和通用消息
        return AjaxResult.error(ErrorCode.DB_ERROR, "数据库操作失败");
    }
    
    /**
     * ✅ 正确：根据不同数据库异常返回不同错误码
     */
    public AjaxResult handleSQLException(SQLException e) {
        log.error("SQL异常, errorCode={}, sqlState={}", 
                 e.getErrorCode(), e.getSQLState(), e);
        
        // 根据 SQL 状态码返回业务错误
        String sqlState = e.getSQLState();
        if ("23000".equals(sqlState)) {
            // 唯一约束冲突
            return AjaxResult.error("数据已存在，请勿重复提交");
        } else if ("42000".equals(sqlState)) {
            // 语法错误
            return AjaxResult.error("操作失败，请检查输入参数");
        } else {
            // 其他数据库错误
            return AjaxResult.error("数据库操作失败");
        }
    }
    
    // ============================================
    // 正确示例 4：开发/生产环境区分
    // ============================================
    
    /**
     * ✅ 正确：开发环境返回详细信息，生产环境返回通用消息
     */
    public AjaxResult handleExceptionWithEnv(Exception e) {
        log.error("系统异常", e);
        
        // 开发环境返回详细信息便于调试
        if (isDevelopmentMode()) {
            return AjaxResult.error("开发环境错误: " + e.getMessage());
        }
        
        // 生产环境返回通用消息
        return AjaxResult.error("系统繁忙，请稍后重试");
    }
    
    private boolean isDevelopmentMode() {
        String env = System.getProperty("spring.profiles.active");
        return "dev".equals(env) || "local".equals(env);
    }
    
    // ============================================
    // 正确示例 5：全局异常处理器
    // ============================================
    
    /**
     * ✅ 正确：Spring 全局异常处理器示例
     */
    @RestControllerAdvice
    public class GlobalExceptionHandler {
        
        /**
         * 业务异常：可以返回具体消息
         */
        @ExceptionHandler(BusinessException.class)
        public AjaxResult handleBusinessException(BusinessException e) {
            log.info("业务异常: {}", e.getMessage());
            return AjaxResult.error(e.getMessage());
        }
        
        /**
         * 参数校验异常：返回校验信息
         */
        @ExceptionHandler(MethodArgumentNotValidException.class)
        public AjaxResult handleValidException(MethodArgumentNotValidException e) {
            String message = e.getBindingResult()
                             .getFieldError()
                             .getDefaultMessage();
            return AjaxResult.error(message);
        }
        
        /**
         * 请求方法不支持：返回业务错误
         */
        @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
        public AjaxResult handleMethodNotSupported(
                HttpRequestMethodNotSupportedException e,
                HttpServletRequest request) {
            
            log.warn("不支持的请求方法: {} {}", 
                    request.getMethod(), request.getRequestURI());
            
            String message = "请求地址" + request.getRequestURI() + 
                           ",不支持" + e.getMethod() + "请求";
            return AjaxResult.error(message);
        }
        
        /**
         * 数据库异常：返回通用消息
         */
        @ExceptionHandler(SQLException.class)
        public AjaxResult handleSQLException(SQLException e) {
            log.error("数据库异常", e);
            return AjaxResult.error("数据库操作失败");
        }
        
        /**
         * 系统异常：返回通用消息
         */
        @ExceptionHandler(Exception.class)
        public AjaxResult handleException(Exception e) {
            log.error("系统异常", e);
            return AjaxResult.error("系统繁忙，请稍后重试");
        }
    }
    
    // ============================================
    // 可能泄露的敏感信息示例
    // ============================================
    
    /**
     * 以下是一些可能泄露的敏感信息示例：
     * 
     * 1. 数据库异常
     *    "Table 'sys_admin_user' doesn't exist"
     *    → 暴露表名：sys_admin_user
     * 
     * 2. 字段异常
     *    "Column 'admin_password' not found"
     *    → 暴露字段名：admin_password
     * 
     * 3. 文件路径
     *    "/opt/tomcat/webapps/app/WEB-INF/config/jdbc.properties not found"
     *    → 暴露服务器路径和文件结构
     * 
     * 4. SQL 语句
     *    "You have an error in your SQL syntax near 'SELECT * FROM admin_user WHERE username='"
     *    → 暴露表名和 SQL 语句结构
     * 
     * 5. 连接信息
     *    "Connection refused: connect to http://192.168.1.100:8080"
     *    → 暴露内网 IP 和端口
     * 
     * 6. 版本信息
     *    "MySQLSyntaxErrorException: You have an error in your SQL syntax; check the manual that corresponds to your MySQL server version 5.7.28"
     *    → 暴露数据库类型和版本
     * 
     * 7. 框架信息
     *    "No serializer found for class org.hibernate.proxy.pojo.bytebuddy.ByteBuddyInterceptor"
     *    → 暴露使用的框架和技术栈
     */
}

// 辅助类定义
class AjaxResult {
    private boolean success;
    private String message;
    private Object data;
    
    public static AjaxResult success(String message) {
        AjaxResult result = new AjaxResult();
        result.success = true;
        result.message = message;
        return result;
    }
    
    public static AjaxResult error(String message) {
        AjaxResult result = new AjaxResult();
        result.success = false;
        result.message = message;
        return result;
    }
    
    public static AjaxResult error(String code, String message) {
        AjaxResult result = new AjaxResult();
        result.success = false;
        result.message = message;
        return result;
    }
}

class ErrorCode {
    public static final String DB_ERROR = "DB_001";
    public static final String NETWORK_ERROR = "NET_001";
    public static final String SYSTEM_ERROR = "SYS_001";
}

class BusinessException extends Exception {
    public BusinessException(String message) {
        super(message);
    }
}

class HttpRequestMethodNotSupportedException extends Exception {
    private String method;
    
    public String getMethod() {
        return method;
    }
}

// 占位符
class log {
    public static void error(String msg, Object... args) {}
    public static void warn(String msg, Object... args) {}
    public static void info(String msg, Object... args) {}
}

