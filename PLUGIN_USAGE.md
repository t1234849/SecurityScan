# 代码安全扫描助手 - 使用说明

## 🎯 插件介绍

这是一个专为国内安全审计需求设计的 IDEA 插件，可以在编码阶段实时检测并修复常见的安全漏洞。

## ✨ 核心功能

### 1. **实时安全扫描**
在你编写代码时，插件会自动在后台使用 PSI 扫描代码，发现安全风险会立即显示下划线提示。

### 2. **智能提示**
详细的安全风险说明，包括：
- 风险描述
- 攻击场景
- 修复建议
- 示例代码

### 3. **一键修复**
提供 QuickFix 功能，可以自动修复部分安全问题。

## 🔍 支持检测的安全问题

### 1. Fastjson 反序列化漏洞（🔥 高危）

**检测内容：**
- `JSON.parseObject()`
- `JSON.parse()`
- `JSON.parseArray()`
- 启用了 `AutoType` 的情况

**危险示例：**
```java
// ❌ 危险：可能导致 RCE
String json = request.getParameter("data");
JSONObject obj = JSON.parseObject(json);
```

**修复方案：**
- 替换为 Jackson（推荐）
- 替换为 Gson
- 添加安全配置（治标不治本）

**一键修复：**
- `Alt+Enter` → 选择"替换为 Jackson（推荐）"
- `Alt+Enter` → 选择"替换为 Gson"

---

### 2. SQL 注入漏洞（🔥 高危）

**检测内容：**
- 字符串拼接构造 SQL 语句
- `Statement.executeQuery()` 使用拼接的 SQL
- 包含 SQL 关键字的字符串拼接

**危险示例：**
```java
// ❌ 危险：SQL 注入
String sql = "SELECT * FROM users WHERE username = '" + username + "'";
Statement stmt = conn.createStatement();
ResultSet rs = stmt.executeQuery(sql);
```

**修复方案：**
- 使用 `PreparedStatement` 参数化查询
- 使用 MyBatis 的 `#{}` 而不是 `${}`

**一键修复：**
- `Alt+Enter` → 选择"转换为 PreparedStatement 参数化查询"

---

### 3. 路径遍历风险（⚠️ 警告）

**检测内容：**
- `new File(userInput)`
- `Paths.get(userInput)`
- `Path.of(userInput)`

**危险示例：**
```java
// ❌ 危险：路径遍历
String fileName = request.getParameter("file");
File file = new File("/uploads/" + fileName);
// 攻击者输入：../../etc/passwd
```

**修复方案：**
- 使用 `FileUtil.file()` 进行安全处理
- 对路径进行规范化和白名单校验

**一键修复：**
- `Alt+Enter` → 选择"使用 FileUtil.file() 替代"
- `Alt+Enter` → 选择"添加路径安全校验"

---

### 4. 不安全的 URL 创建（⚠️ SSRF 风险）

**检测内容：**
- `new URL(userInput)` 使用外部输入

**危险示例：**
```java
// ❌ 危险：SSRF 攻击
String url = request.getParameter("url");
URL urlObj = new URL(url);
// 攻击者输入：file:///etc/passwd 或 http://internal-server/
```

**修复方案：**
- 对 URL 进行白名单校验
- 限制允许的协议（只允许 http/https）
- 限制允许的域名

**一键修复：**
- `Alt+Enter` → 选择"添加URL安全校验"

---

## 📦 插件架构

```
SecurityScan/
├── rules/                          # 规则层
│   ├── SecurityRule.kt            # 规则接口
│   ├── AbstractSecurityRule.kt    # 规则基类
│   ├── SecurityRuleRegistry.kt    # 规则注册中心
│   └── impl/                      # 具体规则实现
│       ├── FastjsonDeserializationRule.kt
│       ├── SqlInjectionRule.kt
│       ├── PathTraversalRule.kt
│       └── UnsafeUrlCreationRule.kt
│
├── inspections/                    # 检查器层
│   ├── SecurityInspectionBase.kt  # 检查器基类
│   ├── FastjsonDeserializationInspection.kt
│   ├── SqlInjectionInspection.kt
│   ├── PathTraversalInspection.kt
│   └── UnsafeUrlCreationInspection.kt
│
└── quickfixes/                     # QuickFix 层（集成在规则中）
    ├── ReplaceWithJacksonQuickFix
    ├── UsePreparedStatementQuickFix
    ├── UseFileUtilQuickFix
    └── AddUrlValidationQuickFix
```

### 架构优势

1. **规则可扩展**：新增规则只需实现 `SecurityRule` 接口
2. **检查器可复用**：统一的 `SecurityInspectionBase` 基类
3. **配置灵活**：风险级别、提示文案都可自定义

---

## 🚀 如何添加新规则

### 步骤 1：创建规则类

```kotlin
class MySecurityRule : AbstractSecurityRule() {
    override val ruleId = "MY_RULE"
    override val ruleName = "我的规则"
    override val description = "规则描述"
    override val severity = RiskLevel.WARNING
    
    override fun matches(element: PsiElement): Boolean {
        // 实现匹配逻辑
        return false
    }
    
    override fun getQuickFixes(element: PsiElement): Array<LocalQuickFix> {
        return arrayOf(MyQuickFix())
    }
}
```

### 步骤 2：创建 Inspection

```kotlin
class MySecurityInspection : SecurityInspectionBase() {
    override fun getSecurityRules(): List<SecurityRule> {
        return listOf(MySecurityRule())
    }
}
```

### 步骤 3：注册到 plugin.xml

```xml
<localInspection 
    language="JAVA"
    shortName="MySecurityRule"
    displayName="我的安全规则"
    groupName="Security"
    enabledByDefault="true"
    level="WARNING"
    implementationClass="com.scan.securityscan.inspections.MySecurityInspection"/>
```

---

## 🔧 开发和测试

### 运行插件
```bash
./gradlew runIde
```

### 构建插件
```bash
./gradlew buildPlugin
```

### 测试示例

创建测试文件 `SecurityTestExamples.java`：

```java
import com.alibaba.fastjson.JSON;
import java.io.File;
import java.net.URL;
import java.sql.*;

public class SecurityTestExamples {
    
    // 测试1：Fastjson 反序列化（应该报错）
    public void testFastjson(String jsonData) {
        Object obj = JSON.parseObject(jsonData);  // 🔥 这里会高亮提示
    }
    
    // 测试2：SQL 注入（应该报错）
    public void testSqlInjection(Connection conn, String username) throws Exception {
        String sql = "SELECT * FROM users WHERE name = '" + username + "'";  // 🔥 这里会高亮提示
        Statement stmt = conn.createStatement();
        stmt.executeQuery(sql);
    }
    
    // 测试3：路径遍历（应该报警告）
    public void testPathTraversal(String fileName) {
        File file = new File("/uploads/" + fileName);  // ⚠️ 这里会高亮提示
    }
    
    // 测试4：SSRF（应该报警告）
    public void testSSRF(String urlString) throws Exception {
        URL url = new URL(urlString);  // ⚠️ 这里会高亮提示
    }
}
```

打开这个文件，你应该能看到：
- 红色波浪线（高危问题）
- 黄色波浪线（警告）
- 把光标放上去会显示详细提示
- 按 `Alt+Enter` 可以看到修复选项

---

## 📊 符合的审计标准

### ✅ 奇安信代码审计
- Fastjson 使用检测
- SQL 注入检测
- 路径遍历检测
- SSRF 检测

### ✅ 等保 2.0 测评
- 应用安全要求
- 数据安全要求
- 代码安全性

### ✅ OWASP Top 10 2021
- A01:2021 – Broken Access Control（路径遍历）
- A03:2021 – Injection（SQL 注入）
- A08:2021 – Software and Data Integrity Failures（反序列化）
- A10:2021 – Server-Side Request Forgery (SSRF)

---

## 💡 最佳实践

1. **开发阶段启用插件**：在编码时就发现问题
2. **提交前检查**：确保没有高危安全问题
3. **团队规范**：将安全编码作为团队标准
4. **持续改进**：根据审计反馈扩展规则

---

## 🎓 扩展方向

### 可以添加的规则：

1. **反序列化漏洞**
   - `ObjectInputStream.readObject()`
   - Java 原生反序列化

2. **命令注入**
   - `Runtime.getRuntime().exec()`
   - `ProcessBuilder`

3. **XXE（XML 外部实体注入）**
   - `DocumentBuilder`
   - `SAXParser`

4. **不安全的加密**
   - 弱加密算法（DES、MD5）
   - 硬编码密钥

5. **敏感信息泄露**
   - 硬编码密码
   - 日志输出敏感信息

6. **CSRF 防护缺失**
   - Spring 接口缺少 CSRF token

7. **不安全的随机数**
   - 使用 `Random` 而不是 `SecureRandom`

---

## 📞 技术支持

如有问题或建议，请联系：
- Email: support@securityscan.com
- GitHub: [项目地址]

---

## 📄 许可证

本插件遵循 Apache 2.0 许可证。

