# SecureWatch - 代码安全监视器

<div align="center">

**🛡️ 专为国内安全审计需求设计的 IDEA 插件**

[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)
[![IntelliJ Platform](https://img.shields.io/badge/IntelliJ%20Platform-2022.2+-orange.svg)](https://www.jetbrains.com/idea/)
[![Java](https://img.shields.io/badge/Java-17+-green.svg)](https://www.oracle.com/java/)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.22-purple.svg)](https://kotlinlang.org/)

*实时监控代码安全，在编码阶段立即发现并提示安全隐患*

[快速开始](#-快速开始) • [功能特性](#-功能特性) • [检测规则](#-检测规则) • [使用方法](#-使用方法) • [开发文档](#-开发文档)

</div>

---

## 📖 项目简介

SecureWatch（代码安全监视器）是一个基于 IntelliJ IDEA 的代码安全检测插件，可以在**编写代码时实时监控**常见的安全漏洞，并提供**详细的安全提示和修复建议**。

### 🎯 解决的痛点

- ✅ **奇安信代码审计** - 提前发现审计问题，减少整改成本
- ✅ **等保 2.0 测评** - 满足等保安全编码要求
- ✅ **安全扫描整改** - 避免上线后的紧急修复
- ✅ **团队代码规范** - 统一安全编码标准

### ⭐ 核心特性

- 🔍 **实时检测** - 编码时后台自动扫描，立即标记问题
- 💡 **详细提示** - 鼠标悬停显示风险说明、攻击示例、修复建议
- 🎨 **柔和标记** - 统一黄色波浪线，不干扰正常编码
- 🛠️ **实用建议** - 推荐 Hutool 等国内常用工具库
- 📚 **完整文档** - 包含详细的使用指南和开发文档

---

## 🔥 功能特性

### 支持的检测类型（共9种）

| # | 检测规则 | 类别 | 风险级别 | 说明 |
|---|---------|------|---------|------|
| 1 | **Fastjson 反序列化** | 安全 | 🔥 高危 | 检测 JSON.parseObject/parse/parseArray，特别关注 AutoType |
| 2 | **SQL 注入** | 安全 | 🔥 高危 | 检测字符串拼接SQL、MyBatis ${} 用法 |
| 3 | **Java 原生反序列化** | 安全 | 🔥 高危 | 检测 ObjectInputStream.readObject() |
| 4 | **命令注入** | 安全 | 🔥 高危 | 检测 Runtime.exec()、ProcessBuilder 字符串拼接 |
| 5 | **路径遍历** | 安全 | ⚠️ 警告 | 检测 new File()、Paths.get() 外部输入 |
| 6 | **SSRF** | 安全 | ⚠️ 警告 | 检测 new URL() 外部输入 |
| 7 | **资源未释放** | 质量 | ⚠️ 警告 | 检测 InputStream 等资源未在 finally 中关闭 |
| 8 | **系统信息泄露** | 质量 | ⚠️ 警告 | 检测 return e.getMessage() 泄露内部信息 |
| 9 | **不安全的随机数** | 密码 | ⚠️ 警告 | 检测 new Random() 用于安全场景 |

### 特色功能

#### 1. MyBatis SQL 注入检测 ✨

专门检测 MyBatis 注解中的 `${}` 用法：

```java
// ❌ 不安全 - 显示黄色波浪线
@Select("SELECT * FROM users WHERE username = '${username}'")
User findUser(String username);

// ✅ 安全
@Select("SELECT * FROM users WHERE username = #{username}")
User findUser(String username);
```

#### 2. 推荐 Hutool 工具库 🛠️

针对国内常用的 Hutool 工具库提供专门的安全建议：

- **路径遍历** → 推荐使用 `FileUtil.file()`
- **SSRF** → 推荐使用 `URLUtil.url()`
- **资源管理** → 推荐使用 `IoUtil.close()`

#### 3. 详细的安全提示 📚

每个问题都提供：
- 🚨 **风险说明** - 为什么不安全
- 💣 **攻击示例** - 实际的攻击场景
- 🔧 **修复建议** - 多种修复方案供选择
- 📝 **示例代码** - 完整的安全代码示例
- 📊 **审计标准** - 符合奇安信、等保等国内标准

#### 4. 智能检测 🧠

- 区分安全和不安全的场景
- 识别常量和变量
- 检测 try-with-resources
- 理解业务异常和系统异常
- 识别开发和生产环境

---

## 🚀 快速开始

### 环境要求

- **JDK**: 17+（已配置为 `D:\jdk17`）
- **IntelliJ IDEA**: 2023.2+
- **Gradle**: 8.0+（已包含 Wrapper）

### 构建插件

```cmd
# Windows
gradlew.bat buildPlugin

# 插件生成位置
build\distributions\SecureWatch-1.0-SNAPSHOT.zip
```

### 运行测试

```cmd
# Windows
gradlew.bat runIde

# 会自动启动一个带插件的 IDEA 实例
```

### 安装插件

**方法 1：手动安装**
1. 打开 IDEA
2. `File` → `Settings` → `Plugins`
3. 点击 ⚙️ → `Install Plugin from Disk...`
4. 选择 `build\distributions\SecureWatch-1.0-SNAPSHOT.zip`
5. 重启 IDEA

**方法 2：开发模式运行**
```cmd
gradlew.bat runIde
```

---

## 📝 使用方法

### 第一步：打开项目

在安装了插件的 IDEA 中打开任意 Java 项目。

### 第二步：查看检测结果

编写代码时，不安全的代码会自动显示**黄色波浪线**：

```java
// 自动检测并标记
JSONObject obj = JSON.parseObject(userInput);  // ⚠️ 黄色波浪线
String sql = "SELECT * FROM users WHERE id=" + id;  // ⚠️ 黄色波浪线
Random random = new Random();  // ⚠️ 黄色波浪线
```

### 第三步：查看详细提示

将鼠标悬停在黄色波浪线上，查看详细的安全提示：

```
🚨 严重安全风险：Fastjson 反序列化漏洞

【问题】使用Fastjson反序列化可能导致远程代码执行

【修复建议】
1. 【推荐】替换为 Jackson
   ObjectMapper mapper = new ObjectMapper();
   MyClass obj = mapper.readValue(jsonString, MyClass.class);

2. 替换为 Gson
   ...

【为什么危险】
Fastjson 的 AutoType 功能允许攻击者通过 @type 字段指定类名，
可以实例化恶意类（如 JdbcRowSetImpl），执行任意代码。
```

### 第四步：手动修复

根据提示信息，手动修改代码：

```java
// 修改前
JSONObject obj = JSON.parseObject(userInput);

// 修改后
ObjectMapper mapper = new ObjectMapper();
MyClass obj = mapper.readValue(userInput, MyClass.class);
```

### 查看所有问题

打开 Problems 窗口查看所有检测到的问题：

1. `View` → `Tool Windows` → `Problems`
2. 筛选 `Security` 分组
3. 查看所有安全问题

---

## 🧪 测试示例

项目包含完整的测试示例文件：

| 测试文件 | 说明 |
|---------|------|
| `SecurityTestExamples.java` | 通用安全测试（6种漏洞） |
| `MyBatisSecurityExamples.java` | MyBatis SQL 注入专项测试 |
| `ResourceLeakExamples.java` | 资源泄漏测试 |
| `InformationLeakExamples.java` | 信息泄露测试 |
| `InsecureRandomExamples.java` | 不安全随机数测试 |

### 快速测试

```cmd
# 1. 运行插件
gradlew.bat runIde

# 2. 在新窗口中创建 Java 项目

# 3. 复制测试文件
copy src\main\resources\examples\*.java <你的项目>\src\main\java\

# 4. 打开测试文件，观察黄色波浪线
```

---

## 📊 符合的安全标准

### ✅ 国内标准

| 标准 | 覆盖情况 |
|------|---------|
| **奇安信代码审计** | ✅ Fastjson、SQL注入、反序列化、命令注入、信息泄露 |
| **等保 2.0 测评** | ✅ 应用安全、数据安全、代码安全性 |

### ✅ 国际标准

| 标准 | 覆盖情况 |
|------|---------|
| **OWASP Top 10 2021** | ✅ A01（路径遍历）、A03（注入）、A08（反序列化）、A10（SSRF） |
| **CWE** | ✅ CWE-89、CWE-78、CWE-502、CWE-22、CWE-918 |

---

## 📚 开发文档

### 项目结构

```
SecureWatch/
├── src/main/kotlin/com/scan/securityscan/
│   ├── rules/                          # 规则层
│   │   ├── SecurityRule.kt            # 规则接口
│   │   ├── AbstractSecurityRule.kt    # 规则基类
│   │   └── impl/                      # 9个具体规则
│   │       ├── FastjsonDeserializationRule.kt
│   │       ├── SqlInjectionRule.kt
│   │       ├── JavaDeserializationRule.kt
│   │       ├── CommandInjectionRule.kt
│   │       ├── PathTraversalRule.kt
│   │       ├── UnsafeUrlCreationRule.kt
│   │       ├── ResourceLeakRule.kt
│   │       ├── InformationLeakRule.kt
│   │       └── InsecureRandomRule.kt
│   │
│   └── inspections/                    # 检查器层
│       ├── SecurityInspectionBase.kt  # 检查器基类
│       └── [9个具体的 Inspection]
│
├── src/main/resources/
│   ├── META-INF/plugin.xml            # 插件配置
│   └── examples/                       # 测试示例
│
├── build.gradle.kts                    # Gradle 配置
├── gradlew.bat                         # Windows 构建脚本
└── README.md                           # 本文档
```

### 添加新规则

详细步骤请参考：[DEVELOPMENT.md](DEVELOPMENT.md)

简要步骤：

1. **创建规则类** - 实现 `SecurityRule` 接口
2. **创建检查器** - 继承 `SecurityInspectionBase`
3. **注册到 plugin.xml** - 添加 `localInspection` 配置
4. **测试规则** - 运行 `gradlew.bat runIde`

---

## 🔧 配置说明

### 启用/禁用规则

1. `File` → `Settings` → `Editor` → `Inspections`
2. 展开 `Security` 分组
3. 勾选或取消勾选需要的规则

### 调整严重级别

虽然代码中设置了不同的风险级别，但所有问题统一显示为**黄色波浪线**，视觉更柔和。

提示信息中仍会标注实际的风险级别（🔥 高危 / ⚠️ 警告）。

---

## 📖 文档索引

| 文档 | 说明 |
|------|------|
| [README.md](README.md) | 项目概述（本文档） |
| [快速入门.md](快速入门.md) | 5分钟快速上手指南 |
| [PLUGIN_USAGE.md](PLUGIN_USAGE.md) | 详细使用说明 |
| [DEVELOPMENT.md](DEVELOPMENT.md) | 开发者指南 |
| [Hutool集成说明.md](Hutool集成说明.md) | Hutool 工具库使用 |
| [显示效果说明.md](显示效果说明.md) | 界面显示效果 |
| [更新说明.md](更新说明.md) | 版本更新记录 |

---

## 💡 最佳实践

### 开发阶段

```java
// ✅ 边写代码边检查
public void saveUser(String username) {
    // 看到黄色波浪线 → 鼠标悬停查看 → 根据建议修复
    String sql = "INSERT INTO users VALUES('" + username + "')";
}
```

### 代码审查

1. 打开 `Problems` 窗口
2. 筛选 `Security` 分组
3. 逐个检查所有问题
4. 确保修复或记录原因

### 安全测评前

1. 全项目扫描安全问题
2. 优先修复高危问题（🔥）
3. 评估警告级别问题（⚠️）
4. 准备修复说明文档

---

## 🎯 使用场景

### 1. 日常开发

- 实时发现安全问题
- 学习安全编码规范
- 养成安全编码习惯

### 2. 代码审查

- 提交前自查
- 减少审查成本
- 提高代码质量

### 3. 安全测评

- 奇安信审计准备
- 等保测评自查
- 漏洞扫描整改

### 4. 团队培训

- 详细的安全说明
- 实际的攻击示例
- 完整的修复方案

---

## ⚙️ 构建说明

### 构建命令

```cmd
# 构建插件
gradlew.bat buildPlugin

# 清理构建
gradlew.bat clean

# 运行测试
gradlew.bat runIde

# 验证插件
gradlew.bat verifyPlugin
```

### 构建产物

```
build/
├── distributions/
│   └── SecureWatch-1.0-SNAPSHOT.zip  ← 插件安装包
├── libs/
│   └── SecureWatch-1.0-SNAPSHOT.jar  ← 编译后的 JAR
└── idea-sandbox/                      ← 测试沙箱
```

### JDK 配置

项目已配置使用 `D:\jdk17`，修改请编辑 `gradlew.bat`：

```batch
@rem 写死 JAVA_HOME 路径
set JAVA_HOME=D:\jdk17
```

---

## 🤝 贡献指南

欢迎贡献代码、提出建议或报告问题！

### 贡献方式

1. Fork 本项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交改动 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

### 报告问题

如果发现问题或有建议，请：
1. 查看 [Issues](../../issues) 是否已存在
2. 创建新 Issue，详细描述问题
3. 包含复现步骤和环境信息

---

## 📄 许可证

本项目采用 Apache License 2.0 许可证。详见 [LICENSE](LICENSE) 文件。

```
Copyright 2024 SecureWatch Team

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

---

## 📞 联系方式

- **Email**: 1724188078@qq.com
- **Issues**: [GitHub Issues](../../issues)
- **文档**: [项目 Wiki](../../wiki)

---

## 🌟 致谢

感谢以下项目和工具：

- [IntelliJ Platform SDK](https://plugins.jetbrains.com/docs/intellij/welcome.html)
- [Kotlin](https://kotlinlang.org/)
- [Gradle](https://gradle.org/)
- [Hutool](https://hutool.cn/)

---

## 📈 版本历史

- **v1.2** (当前) - 新增不安全随机数检测、系统信息泄露检测、资源未释放检测
- **v1.1** - 新增 MyBatis 注解检测、统一黄色波浪线显示
- **v1.0** - 初始版本，包含 6 种基础安全检测

---

<div align="center">

**让安全编码成为习惯，而不是事后补救 🛡️**

Made with ❤️ by pengfei-t

如果这个项目对你有帮助，请给个 Star ⭐️

</div>
