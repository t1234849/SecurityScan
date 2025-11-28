# 测试示例代码

本文档包含各种不安全代码示例，用于测试插件的检测能力。

## 1. Fastjson 反序列化测试

### 测试文件：FastjsonTest.java

```java
package test;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.parser.Feature;

public class FastjsonTest {
    
    // ❌ 应该被检测：基本的 parseObject
    public void test1(String jsonString) {
        User user = JSON.parseObject(jsonString, User.class);
    }
    
    // ❌ 应该被检测：parse 方法
    public void test2(String jsonString) {
        Object obj = JSON.parse(jsonString);
    }
    
    // 🔥 应该被检测为高危：启用了 AutoType
    public void test3(String jsonString) {
        User user = JSON.parseObject(jsonString, User.class, Feature.SupportAutoType);
    }
    
    // ❌ 应该被检测：parseArray
    public void test4(String jsonString) {
        List<User> users = JSON.parseArray(jsonString, User.class);
    }
    
    // ✅ 不应该被检测：使用常量
    public void test5() {
        String json = "{\"name\":\"test\"}";
        User user = JSON.parseObject(json, User.class);
    }
}
```

## 2. SQL 注入测试

### 测试文件：SqlInjectionTest.java

```java
package test;

import java.sql.*;

public class SqlInjectionTest {
    
    // ❌ 应该被检测：字符串拼接构造 SQL
    public void test1(Connection conn, String userName) throws SQLException {
        String sql = "SELECT * FROM users WHERE name = '" + userName + "'";
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(sql);
    }
    
    // ❌ 应该被检测：使用变量拼接
    public void test2(Connection conn, String userId) throws SQLException {
        String sql = "DELETE FROM users WHERE id = " + userId;
        Statement stmt = conn.createStatement();
        stmt.executeUpdate(sql);
    }
    
    // ❌ 应该被检测：复杂的拼接
    public void test3(Connection conn, String name, String email) throws SQLException {
        StringBuilder sql = new StringBuilder("INSERT INTO users (name, email) VALUES ('");
        sql.append(name).append("', '").append(email).append("')");
        Statement stmt = conn.createStatement();
        stmt.execute(sql.toString());
    }
    
    // ✅ 不应该被检测：使用 PreparedStatement
    public void test4(Connection conn, String userName) throws SQLException {
        String sql = "SELECT * FROM users WHERE name = ?";
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setString(1, userName);
        ResultSet rs = pstmt.executeQuery();
    }
    
    // ✅ 不应该被检测：使用常量
    public void test5(Connection conn) throws SQLException {
        String sql = "SELECT * FROM users WHERE role = 'admin'";
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(sql);
    }
}
```

## 3. 路径遍历测试

### 测试文件：PathTraversalTest.java

```java
package test;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PathTraversalTest {
    
    // ❌ 应该被检测：使用外部输入创建 File
    public void test1(String fileName) {
        File file = new File("/uploads/" + fileName);
    }
    
    // ❌ 应该被检测：使用 Paths.get
    public void test2(String fileName) {
        Path path = Paths.get("/data", fileName);
    }
    
    // ❌ 应该被检测：使用 Path.of
    public void test3(String fileName) {
        Path path = Path.of("/tmp", fileName);
    }
    
    // ❌ 应该被检测：构造器中使用变量
    public void test4(String userPath) {
        File file = new File(userPath);
    }
    
    // ✅ 不应该被检测：使用常量
    public void test5() {
        File file = new File("/etc/config.properties");
    }
    
    // ✅ 不应该被检测：使用 final 常量
    public void test6() {
        final String CONFIG_PATH = "/etc/config.properties";
        File file = new File(CONFIG_PATH);
    }
}
```

## 4. SSRF 测试

### 测试文件：SsrfTest.java

```java
package test;

import java.net.URL;
import java.net.MalformedURLException;

public class SsrfTest {
    
    // ❌ 应该被检测：使用外部输入创建 URL
    public void test1(String urlString) throws MalformedURLException {
        URL url = new URL(urlString);
        url.openConnection();
    }
    
    // ❌ 应该被检测：从请求参数获取
    public void test2(HttpServletRequest request) throws MalformedURLException {
        String targetUrl = request.getParameter("url");
        URL url = new URL(targetUrl);
    }
    
    // ❌ 应该被检测：从变量构造
    public void test3(String host, String path) throws MalformedURLException {
        String fullUrl = "http://" + host + path;
        URL url = new URL(fullUrl);
    }
    
    // ✅ 不应该被检测：使用常量
    public void test4() throws MalformedURLException {
        URL url = new URL("https://api.example.com/data");
    }
    
    // ✅ 不应该被检测：使用 final 常量
    public void test5() throws MalformedURLException {
        final String API_URL = "https://api.example.com";
        URL url = new URL(API_URL);
    }
}
```

## 测试步骤

### 1. 准备测试环境

```bash
# 构建插件
gradlew.bat buildPlugin

# 运行测试 IDE
gradlew.bat runIde
```

### 2. 在测试 IDE 中创建项目

1. 创建新的 Java 项目
2. 创建 `test` 包
3. 复制上述测试文件到项目中

### 3. 验证检测结果

检查每个测试方法：
- ❌ 标记的代码应该显示红色或黄色下划线
- ✅ 标记的代码不应该有警告
- 鼠标悬停查看详细说明
- 按 `Alt+Enter` 测试快速修复

### 4. 预期结果

| 测试类 | 应检测数量 | 不应检测数量 |
|--------|-----------|-------------|
| FastjsonTest | 4 | 1 |
| SqlInjectionTest | 3 | 2 |
| PathTraversalTest | 4 | 2 |
| SsrfTest | 3 | 2 |
| **总计** | **14** | **7** |

## 边界情况测试

### 1. 复杂表达式

```java
// 应该被检测：三元运算符
String sql = isAdmin ? "SELECT * FROM admin" : "SELECT * FROM users WHERE id = " + userId;
stmt.executeQuery(sql);

// 应该被检测：方法返回值
String path = getUserInputPath();
File file = new File(path);
```

### 2. 嵌套调用

```java
// 应该被检测：嵌套的 JSON 解析
List<User> users = JSON.parseObject(
    JSON.toJSONString(rawData), 
    new TypeReference<List<User>>() {}
);
```

### 3. Lambda 和 Stream

```java
// 应该被检测
List<File> files = fileNames.stream()
    .map(name -> new File(basePath + name))
    .collect(Collectors.toList());
```

## 性能测试

### 大文件测试

创建包含 1000+ 行代码的文件，验证：
- 扫描速度是否可接受（< 1秒）
- 不会导致 IDE 卡顿
- 内存占用正常

### 多文件测试

创建项目包含 100+ 个 Java 文件，验证：
- 全项目扫描完成时间
- 后台扫描不影响编码
- 准确率保持稳定

## 误报测试

以下代码**不应该**被检测为问题：

```java
// 1. 使用常量
private static final String SQL = "SELECT * FROM users";
stmt.executeQuery(SQL);

// 2. 注释中的代码
// String sql = "SELECT * FROM users WHERE id = " + userId;

// 3. 字符串字面量
String message = "User id = " + userId;  // 不是 SQL

// 4. 安全的 API
PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM users WHERE id = ?");
pstmt.setInt(1, userId);
```

## 修复测试

对每个检测出的问题：
1. 按 `Alt+Enter`
2. 选择快速修复选项
3. 验证修复后的代码是否正确
4. 验证警告是否消失

