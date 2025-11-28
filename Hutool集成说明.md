 🛠️ Hutool 工具库集成说明

## ✅ 已集成 Hutool 安全工具推荐

### 📋 更新内容

插件现在**推荐使用 Hutool 工具库**来解决安全问题，特别是：

1. **路径遍历** → 使用 `FileUtil.file()`
2. **SSRF 风险** → 使用 `URLUtil.url()`

---

## 🔧 具体更新

### 1️⃣ 路径遍历安全（PathTraversalRule）

**检测到的问题：**
```java
File file = new File(userInput);  // ⚠️ 黄色波浪线
```

**推荐的修复方案（已更新）：**
```java
// 【推荐】使用 Hutool 的 FileUtil
import cn.hutool.core.io.FileUtil;

File file = FileUtil.file(basePath, fileName);
// FileUtil 会自动进行路径规范化和安全检查
```

**为什么推荐 FileUtil？**
- ✅ 自动规范化路径（处理 ../ 等）
- ✅ 自动检测路径遍历攻击
- ✅ 更简洁的 API
- ✅ 国内项目广泛使用

---

### 2️⃣ SSRF 防护（UnsafeUrlCreationRule）✨ 新增

**检测到的问题：**
```java
URL url = new URL(userInput);  // ⚠️ 黄色波浪线
```

**推荐的修复方案（新增）：**
```java
// 【推荐】使用 Hutool 的 URLUtil
import cn.hutool.core.util.URLUtil;

try {
    // URLUtil 会进行安全校验和格式化
    URL url = URLUtil.url(userInput);
    
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
```

**为什么推荐 URLUtil？**
- ✅ 自动校验 URL 格式
- ✅ 自动处理编码问题
- ✅ 更安全的 URL 解析
- ✅ 减少手动校验代码

---

## 📦 如何添加 Hutool 依赖

### Maven 项目

在 `pom.xml` 中添加：

```xml
<dependency>
    <groupId>cn.hutool</groupId>
    <artifactId>hutool-core</artifactId>
    <version>5.8.23</version>
</dependency>
```

### Gradle 项目

在 `build.gradle` 中添加：

```groovy
implementation 'cn.hutool:hutool-core:5.8.23'
```

或 Kotlin DSL (`build.gradle.kts`)：

```kotlin
implementation("cn.hutool:hutool-core:5.8.23")
```

---

## 🎯 完整的安全修复示例

### 场景 1：文件上传处理

```java
@PostMapping("/upload")
public void uploadFile(MultipartFile file, String targetDir) {
    String fileName = file.getOriginalFilename();
    
    // ❌ 不安全的写法
    File dest = new File(targetDir + fileName);
    
    // ✅ 安全的写法：使用 Hutool FileUtil
    File dest = FileUtil.file(targetDir, fileName);
    // FileUtil 会自动处理路径遍历攻击
    
    file.transferTo(dest);
}
```

### 场景 2：远程资源下载

```java
@GetMapping("/download")
public void downloadResource(String url) throws Exception {
    // ❌ 不安全的写法
    URL resourceUrl = new URL(url);
    
    // ✅ 安全的写法：使用 Hutool URLUtil
    URL resourceUrl = URLUtil.url(url);
    
    // 额外安全检查
    if (!resourceUrl.getProtocol().matches("^https?$")) {
        throw new SecurityException("只允许 http/https 协议");
    }
    
    if (isInternalAddress(resourceUrl.getHost())) {
        throw new SecurityException("禁止访问内网地址");
    }
    
    // 下载资源
    InputStream in = resourceUrl.openStream();
    // ... 处理下载
}

private boolean isInternalAddress(String host) {
    return host.equals("localhost") || 
           host.equals("127.0.0.1") ||
           host.startsWith("10.") || 
           host.startsWith("192.168.") ||
           host.equals("169.254.169.254");
}
```

---

## 📊 Hutool 安全工具对照表

| 不安全的 API | Hutool 安全替代 | 说明 |
|-------------|----------------|------|
| `new File(path)` | `FileUtil.file(path)` | 防止路径遍历 |
| `Paths.get(path)` | `FileUtil.file(path)` | 防止路径遍历 |
| `new URL(url)` | `URLUtil.url(url)` | 更安全的 URL 解析 |
| `Files.readAllBytes()` | `FileUtil.readBytes()` | 带安全检查的文件读取 |
| `Files.write()` | `FileUtil.writeBytes()` | 带安全检查的文件写入 |

---

## 🎨 插件中的显示效果

当你写下不安全的代码时：

```java
// 1. 路径遍历风险
File file = new File(userInput);
            ~~~~~~~~~~~~~~~~~~~~ ⚠️ 黄色波浪线

鼠标悬停显示：
⚠️ 安全风险：路径遍历攻击

【修复建议】
1. 【推荐】使用 Hutool 的 FileUtil
   File file = cn.hutool.core.io.FileUtil.file(basePath, fileName);

2. 路径规范化 + 白名单校验
   Path path = Paths.get(basePath, fileName).normalize();
   ...
```

```java
// 2. SSRF 风险
URL url = new URL(userInput);
          ~~~~~~~~~~~~~~~~~~~ ⚠️ 黄色波浪线

鼠标悬停显示：
⚠️ 安全风险：SSRF（服务端请求伪造）攻击

【修复建议】
1. 【推荐】使用 Hutool 的 URLUtil（自动校验）
   URL url = cn.hutool.core.util.URLUtil.url(userInput);

2. URL 白名单校验
   ...
```

---

## 🌟 Hutool 的优势

### 1. 国内广泛使用
- GitHub Star 30k+
- 国内大量企业项目在使用
- 社区活跃，文档完善

### 2. 开箱即用
- 无需自己编写复杂的校验逻辑
- API 简洁易用
- 减少代码量

### 3. 安全可靠
- 内置安全检查
- 自动处理常见安全问题
- 持续更新维护

### 4. 功能丰富
```java
// 文件操作
FileUtil.file()      // 安全创建文件
FileUtil.copy()      // 安全复制文件
FileUtil.del()       // 安全删除文件

// URL 操作
URLUtil.url()        // 安全创建 URL
URLUtil.normalize()  // URL 规范化
URLUtil.getHost()    // 获取主机名

// 还有更多工具...
```

---

## 📚 相关资源

### Hutool 官方文档
- **官网**: https://hutool.cn/
- **GitHub**: https://github.com/dromara/hutool
- **文档**: https://hutool.cn/docs/

### 相关工具文档
- **FileUtil**: https://hutool.cn/docs/#/core/IO/文件工具类-FileUtil
- **URLUtil**: https://hutool.cn/docs/#/core/网络/URL工具-URLUtil

---

## ✅ 更新总结

### 已更新的文件

1. ✅ **UnsafeUrlCreationRule.kt** - 添加 URLUtil 推荐
2. ✅ **PathTraversalRule.kt** - FileUtil 已存在推荐
3. ✅ **SecurityTestExamples.java** - 添加 URLUtil 使用示例

### 插件提供的帮助

- ✅ **检测**：发现不安全的 File 和 URL 创建
- ✅ **提示**：详细的安全风险说明
- ✅ **建议**：推荐使用 Hutool 工具类
- ✅ **示例**：完整的安全代码示例
- ✅ **依赖**：提供 Maven/Gradle 依赖配置

---

## 🚀 开始使用

1. **添加 Hutool 依赖**
   ```xml
   <dependency>
       <groupId>cn.hutool</groupId>
       <artifactId>hutool-core</artifactId>
       <version>5.8.23</version>
   </dependency>
   ```

2. **运行插件测试**
   ```bash
   ./gradlew runIde
   ```

3. **查看测试示例**
   - 打开 `SecurityTestExamples.java`
   - 查看 Hutool 的使用示例
   - 体验插件的提示功能

4. **应用到项目**
   - 根据插件提示修复代码
   - 使用 Hutool 工具类替代不安全的 API
   - 享受更安全、更简洁的代码

---

<div align="center">

**推荐使用 Hutool，让代码更安全、更简洁！🛡️**

</div>

