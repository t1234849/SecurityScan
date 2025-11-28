import org.apache.ibatis.annotations.*;

/**
 * MyBatis SQL 注入安全示例
 * 
 * 本文件展示 MyBatis 中安全和不安全的 SQL 写法
 */
public interface MyBatisSecurityExamples {
    
    // ============================================
    // 危险写法：使用 ${} 字符串替换
    // ============================================
    
    /**
     * ❌ 危险：使用 ${} 进行字符串替换
     * 风险：SQL 注入攻击
     * 
     * ${} 会直接将参数值拼接到 SQL 中，不做任何处理
     * 攻击者可以注入任意 SQL 代码
     */
    @Select("SELECT * FROM users WHERE username = '${username}'")
    User findByUsernameDangerous(String username);
    
    /**
     * ❌ 危险：在 WHERE 条件中使用 ${}
     */
    @Select("SELECT * FROM orders WHERE order_id = ${orderId}")
    Order findByOrderIdDangerous(int orderId);
    
    /**
     * ❌ 危险：在 LIKE 查询中使用 ${}
     */
    @Select("SELECT * FROM products WHERE name LIKE '%${keyword}%'")
    List<Product> searchProductsDangerous(String keyword);
    
    /**
     * ❌ 危险：在 ORDER BY 中使用 ${}
     * 注意：ORDER BY 后面的列名不能用参数绑定，但也不应直接使用 ${}
     * 应该使用白名单校验
     */
    @Select("SELECT * FROM users ORDER BY ${sortColumn}")
    List<User> listUsersDangerous(String sortColumn);
    
    /**
     * ❌ 危险：在 INSERT 语句中使用 ${}
     */
    @Insert("INSERT INTO users (username, email) VALUES ('${username}', '${email}')")
    void insertUserDangerous(String username, String email);
    
    /**
     * ❌ 危险：在 UPDATE 语句中使用 ${}
     */
    @Update("UPDATE users SET email = '${email}' WHERE id = ${id}")
    void updateUserDangerous(int id, String email);
    
    /**
     * ❌ 极度危险：在 DELETE 语句中使用 ${}
     */
    @Delete("DELETE FROM users WHERE username = '${username}'")
    void deleteUserDangerous(String username);
    
    // ============================================
    // 安全写法：使用 #{} 参数绑定
    // ============================================
    
    /**
     * ✅ 安全：使用 #{} 参数绑定
     * 
     * #{} 会使用 PreparedStatement 的参数绑定
     * 自动进行参数转义，防止 SQL 注入
     */
    @Select("SELECT * FROM users WHERE username = #{username}")
    User findByUsernameSafe(String username);
    
    /**
     * ✅ 安全：在 WHERE 条件中使用 #{}
     */
    @Select("SELECT * FROM orders WHERE order_id = #{orderId}")
    Order findByOrderIdSafe(int orderId);
    
    /**
     * ✅ 安全：在 LIKE 查询中使用 #{}
     * 方式1：使用 CONCAT 函数
     */
    @Select("SELECT * FROM products WHERE name LIKE CONCAT('%', #{keyword}, '%')")
    List<Product> searchProductsSafe1(String keyword);
    
    /**
     * ✅ 安全：在 LIKE 查询中使用 #{}
     * 方式2：在代码中拼接通配符
     */
    @Select("SELECT * FROM products WHERE name LIKE #{keyword}")
    List<Product> searchProductsSafe2(String keyword);
    // 调用时：searchProductsSafe2("%" + keyword + "%")
    
    /**
     * ✅ 安全：在 INSERT 语句中使用 #{}
     */
    @Insert("INSERT INTO users (username, email) VALUES (#{username}, #{email})")
    void insertUserSafe(String username, String email);
    
    /**
     * ✅ 安全：在 UPDATE 语句中使用 #{}
     */
    @Update("UPDATE users SET email = #{email} WHERE id = #{id}")
    void updateUserSafe(@Param("id") int id, @Param("email") String email);
    
    /**
     * ✅ 安全：在 DELETE 语句中使用 #{}
     */
    @Delete("DELETE FROM users WHERE id = #{id}")
    void deleteUserSafe(int id);
    
    /**
     * ✅ 安全：多个参数使用 @Param 注解
     */
    @Select("SELECT * FROM users WHERE username = #{username} AND status = #{status}")
    User findByUsernameAndStatus(@Param("username") String username, 
                                   @Param("status") int status);
    
    // ============================================
    // 特殊场景：ORDER BY 和 表名/列名
    // ============================================
    
    /**
     * ⚠️ 特殊场景：ORDER BY 不能直接使用 #{}
     * 
     * 问题：ORDER BY 后面的列名不能用参数绑定（会被当作字符串）
     * 解决方案：使用白名单校验
     */
    // 这样不行：ORDER BY #{sortColumn} 会变成 ORDER BY 'username'（有引号）
    
    /**
     * ✅ 正确做法：在代码中使用白名单校验
     */
    default List<User> listUsersSafe(String sortColumn) {
        // 白名单校验
        if (!sortColumn.matches("^(id|username|email|create_time)$")) {
            throw new IllegalArgumentException("Invalid sort column");
        }
        // 然后调用实际的查询方法（这里需要在 XML 中使用 ${}，但已经做了白名单校验）
        return listUsersInternal(sortColumn);
    }
    
    @Select("SELECT * FROM users ORDER BY ${sortColumn}")
    List<User> listUsersInternal(String sortColumn);
    
    /**
     * ✅ 更好的做法：使用枚举限制排序字段
     */
    enum SortColumn {
        ID("id"),
        USERNAME("username"),
        EMAIL("email"),
        CREATE_TIME("create_time");
        
        private final String columnName;
        
        SortColumn(String columnName) {
            this.columnName = columnName;
        }
        
        public String getColumnName() {
            return columnName;
        }
    }
    
    // ============================================
    // 攻击示例
    // ============================================
    
    /**
     * 【攻击示例1：绕过身份认证】
     * 
     * 不安全的代码：
     * @Select("SELECT * FROM users WHERE username = '${username}' AND password = '${password}'")
     * User login(String username, String password);
     * 
     * 攻击者输入：
     * username = "admin' --"
     * password = "任意值"
     * 
     * 实际执行的 SQL：
     * SELECT * FROM users WHERE username = 'admin' --' AND password = '任意值'
     * 
     * 结果：注释掉了密码验证，成功以 admin 身份登录
     */
    
    /**
     * 【攻击示例2：数据泄露】
     * 
     * 不安全的代码：
     * @Select("SELECT * FROM users WHERE id = ${id}")
     * User findById(String id);
     * 
     * 攻击者输入：
     * id = "1 UNION SELECT username, password, null, null FROM admin_users"
     * 
     * 实际执行的 SQL：
     * SELECT * FROM users WHERE id = 1 UNION SELECT username, password, null, null FROM admin_users
     * 
     * 结果：获取了管理员表的用户名和密码
     */
    
    /**
     * 【攻击示例3：删除数据】
     * 
     * 不安全的代码：
     * @Delete("DELETE FROM comments WHERE id = ${id}")
     * void deleteComment(String id);
     * 
     * 攻击者输入：
     * id = "1; DELETE FROM users; --"
     * 
     * 实际执行的 SQL：
     * DELETE FROM comments WHERE id = 1; DELETE FROM users; --
     * 
     * 结果：删除了所有用户数据
     */
    
    // ============================================
    // 总结
    // ============================================
    
    /**
     * 【规则总结】
     * 
     * 1. 默认使用 #{} 参数绑定
     *    - SELECT、INSERT、UPDATE、DELETE 中的所有参数
     *    - WHERE 条件、VALUES、SET 子句
     * 
     * 2. 避免使用 ${}
     *    - ${} 会直接拼接字符串，容易导致 SQL 注入
     *    - 只在极少数场景下使用（如动态表名、列名）
     * 
     * 3. 必须使用 ${} 的场景（需要额外校验）
     *    - ORDER BY 后的列名
     *    - 动态表名
     *    - 动态列名
     *    解决方案：使用白名单或枚举限制可选值
     * 
     * 4. LIKE 查询
     *    - 不要：LIKE '%${keyword}%'
     *    - 使用：LIKE CONCAT('%', #{keyword}, '%')
     *    - 或在代码中拼接：LIKE #{keyword}，传入 "%" + keyword + "%"
     * 
     * 【国内审计标准】
     * - 奇安信：严禁使用 ${}，除非有明确的白名单校验
     * - 等保测评：所有 SQL 参数必须使用参数化查询
     */
}

// 辅助类定义
class User {
    private int id;
    private String username;
    private String email;
}

class Order {
    private int orderId;
    private String orderNo;
}

class Product {
    private int id;
    private String name;
}

