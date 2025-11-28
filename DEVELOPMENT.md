# 开发指南

本文档面向想要理解和扩展本插件的开发者。

---

## 📐 架构详解

### 1. 三层架构

```
┌─────────────────────────────────────────┐
│          Plugin Extension Point          │
│         (plugin.xml 注册)                │
└──────────────┬──────────────────────────┘
               │
               ↓
┌─────────────────────────────────────────┐
│         Inspection Layer                 │  ← 检查器层
│  (SecurityInspectionBase)                │
│  - 构建 PSI Visitor                      │
│  - 遍历代码结构                          │
│  - 调用规则检查                          │
└──────────────┬──────────────────────────┘
               │
               ↓
┌─────────────────────────────────────────┐
│           Rule Layer                     │  ← 规则层
│  (SecurityRule 接口)                     │
│  - 定义匹配逻辑                          │
│  - 定义风险描述                          │
│  - 提供修复方案                          │
└──────────────┬──────────────────────────┘
               │
               ↓
┌─────────────────────────────────────────┐
│        QuickFix Layer                    │  ← 修复层
│  (LocalQuickFix 实现)                    │
│  - 自动修改代码                          │
│  - 插入注释提示                          │
└─────────────────────────────────────────┘
```

### 2. 执行流程

```
用户编写代码
    ↓
IntelliJ PSI 解析代码 → 生成 PSI 树
    ↓
触发 Inspection
    ↓
buildVisitor() 返回 JavaElementVisitor
    ↓
遍历 PSI 节点
    ├─ visitMethodCallExpression()   → 方法调用
    ├─ visitNewExpression()          → new 表达式
    ├─ visitBinaryExpression()       → 二元表达式（字符串拼接）
    └─ ... 其他节点类型
    ↓
对每个节点调用 SecurityRule.matches()
    ↓
如果匹配 → registerProblem()
    ├─ 显示警告下划线
    ├─ 显示悬浮提示
    └─ 提供 QuickFix 列表
    ↓
用户按 Alt+Enter 选择修复
    ↓
执行 QuickFix.applyFix()
    ↓
代码自动修改完成
```

### 3. PSI（Program Structure Interface）详解

PSI 是 IntelliJ 平台的核心概念，表示代码的抽象语法树（AST）。

#### 常用 PSI 元素类型

| PSI 类型 | 对应代码 | 用途 |
|----------|---------|------|
| `PsiMethodCallExpression` | `obj.method()` | 检测方法调用 |
| `PsiNewExpression` | `new Class()` | 检测对象创建 |
| `PsiBinaryExpression` | `a + b` | 检测二元运算（如字符串拼接） |
| `PsiLiteralExpression` | `"string"`, `123` | 字面量 |
| `PsiReferenceExpression` | `variable` | 变量引用 |
| `PsiField` | 类字段 | 字段声明 |
| `PsiMethod` | 方法声明 | 方法定义 |
| `PsiClass` | 类声明 | 类定义 |

#### PSI 遍历示例

```kotlin
// 遍历方法调用
override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
    super.visitMethodCallExpression(expression)
    
    // 1. 解析方法
    val method = expression.resolveMethod() ?: return
    
    // 2. 获取类名
    val className = method.containingClass?.qualifiedName ?: return
    
    // 3. 获取方法名
    val methodName = method.name
    
    // 4. 获取参数
    val arguments = expression.argumentList.expressions
    
    // 5. 检查是否匹配危险模式
    if (className == "com.alibaba.fastjson.JSON" && methodName == "parseObject") {
        // 发现问题！
        holder.registerProblem(expression, "发现 Fastjson 反序列化")
    }
}
```

---

## 🔨 实现新规则的完整示例

### 场景：检测不安全的 Random 使用

我们要检测代码中使用 `new Random()` 生成随机数（不安全），建议使用 `SecureRandom`。

### 步骤 1：创建规则

```kotlin
// src/main/kotlin/com/scan/securityscan/rules/impl/InsecureRandomRule.kt
package com.scan.securityscan.rules.impl

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.scan.securityscan.rules.AbstractSecurityRule
import com.scan.securityscan.rules.RiskLevel

/**
 * 不安全的随机数生成规则
 */
class InsecureRandomRule : AbstractSecurityRule() {
    
    override val ruleId = "INSECURE_RANDOM"
    override val ruleName = "不安全的随机数生成"
    override val description = "使用 java.util.Random 生成随机数不适用于安全场景"
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
            ⚠️ 安全风险：java.util.Random 不是密码学安全的随机数生成器
            
            【风险说明】
            - Random 使用线性同余算法，可预测
            - 不适用于生成密码、令牌、密钥等安全敏感场景
            
            【建议】
            使用 java.security.SecureRandom
        """.trimIndent()
    }
    
    override fun getQuickFixes(element: PsiElement): Array<LocalQuickFix> {
        return arrayOf(ReplaceWithSecureRandomQuickFix())
    }
    
    override fun getSecurityAdvice(): String {
        return """
            【修复建议】
            
            // ❌ 不安全：可预测的随机数
            Random random = new Random();
            String token = String.valueOf(random.nextInt());
            
            // ✅ 安全：密码学安全的随机数
            SecureRandom random = new SecureRandom();
            byte[] tokenBytes = new byte[16];
            random.nextBytes(tokenBytes);
            String token = Base64.getEncoder().encodeToString(tokenBytes);
            
            【使用场景】
            - 生成会话令牌：必须用 SecureRandom
            - 生成密码：必须用 SecureRandom
            - 生成 CSRF token：必须用 SecureRandom
            - 游戏随机数：可以用 Random
            - UI 动画：可以用 Random
        """.trimIndent()
    }
}

/**
 * QuickFix：替换为 SecureRandom
 */
class ReplaceWithSecureRandomQuickFix : LocalQuickFix {
    
    override fun getFamilyName(): String {
        return "替换为 SecureRandom"
    }
    
    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val element = descriptor.psiElement as? PsiNewExpression ?: return
        val factory = JavaPsiFacade.getInstance(project).elementFactory
        
        try {
            val newCode = "new java.security.SecureRandom()"
            val newExpression = factory.createExpressionFromText(newCode, element)
            element.replace(newExpression)
        } catch (e: Exception) {
            val comment = factory.createCommentFromText(
                "// TODO: 替换为 SecureRandom", 
                element
            )
            element.parent?.addBefore(comment, element)
        }
    }
}
```

### 步骤 2：创建 Inspection

```kotlin
// src/main/kotlin/com/scan/securityscan/inspections/InsecureRandomInspection.kt
package com.scan.securityscan.inspections

import com.scan.securityscan.rules.SecurityRule
import com.scan.securityscan.rules.impl.InsecureRandomRule

class InsecureRandomInspection : SecurityInspectionBase() {
    
    override fun getSecurityRules(): List<SecurityRule> {
        return listOf(InsecureRandomRule())
    }
    
    override fun getDisplayName(): String {
        return "不安全的随机数生成"
    }
    
    override fun getShortName(): String {
        return "InsecureRandom"
    }
    
    override fun getStaticDescription(): String {
        return """
            检测使用 java.util.Random 生成安全敏感数据的情况。
            
            java.util.Random 使用线性同余算法，其输出是可预测的，
            不应用于生成密码、令牌、密钥等安全敏感数据。
            
            【建议使用】
            java.security.SecureRandom
        """.trimIndent()
    }
}
```

### 步骤 3：注册到 plugin.xml

```xml
<localInspection 
    language="JAVA"
    shortName="InsecureRandom"
    displayName="不安全的随机数生成"
    groupName="Security"
    enabledByDefault="true"
    level="WARNING"
    implementationClass="com.scan.securityscan.inspections.InsecureRandomInspection"/>
```

### 步骤 4：测试

```java
// 测试代码
public class Test {
    public void generateToken() {
        // 应该显示警告
        Random random = new Random();
        String token = String.valueOf(random.nextInt());
    }
}
```

运行插件后，`new Random()` 会显示黄色波浪线，按 `Alt+Enter` 可以选择"替换为 SecureRandom"。

---

## 🧪 调试技巧

### 1. 使用 println 调试

```kotlin
override fun matches(element: PsiElement): Boolean {
    println("检查元素: ${element.text}")
    
    if (element !is PsiMethodCallExpression) {
        println("不是方法调用")
        return false
    }
    
    val method = element.resolveMethod()
    println("方法: ${method?.name}, 类: ${method?.containingClass?.qualifiedName}")
    
    return true
}
```

运行 `./gradlew runIde`，输出会显示在 IDE 的控制台中。

### 2. 查看 PSI 结构

在开发时的 IDEA 中：
1. View → Tool Windows → PsiViewer
2. 打开一个 Java 文件
3. 在 PsiViewer 中查看 PSI 树结构

### 3. 断点调试

在 IDEA 中打开插件项目，设置断点，然后：
1. Run → Debug 'Run Plugin'
2. 在调试的 IDEA 实例中触发检查
3. 断点会在开发 IDEA 中停住

---

## 📚 常用 API 参考

### PSI 操作

```kotlin
// 创建元素
val factory = JavaPsiFacade.getInstance(project).elementFactory
val expression = factory.createExpressionFromText("new SecureRandom()", context)
val comment = factory.createCommentFromText("// TODO: fix", context)

// 替换元素
oldElement.replace(newElement)

// 添加元素
parent.addBefore(newElement, anchor)
parent.addAfter(newElement, anchor)

// 删除元素
element.delete()

// 遍历子元素
element.accept(object : JavaRecursiveElementVisitor() {
    override fun visitMethod(method: PsiMethod) {
        // 处理方法
    }
})
```

### 注册问题

```kotlin
// 注册问题
holder.registerProblem(
    element,                           // 问题所在的元素
    "问题描述",                         // 描述文本
    ProblemHighlightType.WARNING,     // 高亮类型
    *quickFixes                        // QuickFix 数组
)

// 高亮类型
ProblemHighlightType.ERROR                    // 红色波浪线
ProblemHighlightType.GENERIC_ERROR_OR_WARNING // 黄色波浪线
ProblemHighlightType.WARNING                  // 黄色波浪线
ProblemHighlightType.WEAK_WARNING             // 浅灰色波浪线
ProblemHighlightType.INFORMATION              // 信息提示
```

---

## 🎯 最佳实践

### 1. 规则匹配要精确

```kotlin
// ❌ 不好：匹配所有 parseObject
if (methodName == "parseObject") {
    return true
}

// ✅ 好：匹配特定类的方法
if (className == "com.alibaba.fastjson.JSON" && methodName == "parseObject") {
    return true
}
```

### 2. 减少误报

```kotlin
// 检查是否是字面量（常量）
private fun isLiteralOrConstant(expression: PsiExpression): Boolean {
    return when (expression) {
        is PsiLiteralExpression -> true
        is PsiReferenceExpression -> {
            val resolved = expression.resolve()
            resolved is PsiField && 
            resolved.hasModifierProperty(PsiModifier.FINAL) &&
            resolved.hasModifierProperty(PsiModifier.STATIC)
        }
        else -> false
    }
}
```

### 3. 提供详细的提示

```kotlin
override fun getProblemDescription(element: PsiElement): String {
    return """
        🚨 严重安全风险：SQL 注入
        
        【风险说明】
        字符串拼接构造 SQL 语句，攻击者可以注入恶意 SQL 代码
        
        【攻击示例】
        输入: ' OR '1'='1
        结果: 绕过身份验证
        
        【修复建议】
        使用 PreparedStatement 参数化查询
    """.trimIndent()
}
```

### 4. QuickFix 要安全

```kotlin
override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
    try {
        // 尝试自动修复
        val newExpression = factory.createExpressionFromText(newCode, element)
        element.replace(newExpression)
    } catch (e: Exception) {
        // 失败时添加 TODO 注释
        val comment = factory.createCommentFromText("// TODO: 手动修复", element)
        element.parent?.addBefore(comment, element)
    }
}
```

---

## 🔍 常见问题

### Q1: 为什么我的规则没有触发？

**可能原因：**
1. `matches()` 方法返回了 `false`
2. PSI 元素类型不匹配
3. Inspection 没有正确注册到 plugin.xml

**调试方法：**
在 `matches()` 方法中添加 `println` 查看是否被调用。

### Q2: QuickFix 没有显示？

**可能原因：**
1. `getQuickFixes()` 返回了空数组
2. QuickFix 的 `getFamilyName()` 返回了空字符串

### Q3: 代码替换失败？

**可能原因：**
1. 创建的代码语法错误
2. 上下文不正确
3. 缺少必要的导入

**解决方法：**
使用 try-catch 捕获异常，失败时添加注释提示。

---

## 📖 参考资料

### IntelliJ Platform SDK

- [Plugin DevKit](https://plugins.jetbrains.com/docs/intellij/welcome.html)
- [PSI Cook Book](https://plugins.jetbrains.com/docs/intellij/psi-cookbook.html)
- [Code Inspections](https://plugins.jetbrains.com/docs/intellij/code-inspections.html)

### Java Security

- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [CWE（Common Weakness Enumeration）](https://cwe.mitre.org/)
- [OWASP Code Review Guide](https://owasp.org/www-project-code-review-guide/)

---

## 🤝 贡献

欢迎提交 Pull Request！在提交之前，请确保：

1. 代码通过编译：`./gradlew build`
2. 规则有完整的文档说明
3. 提供测试示例
4. 更新 README.md

---

**Happy Coding! 🚀**

