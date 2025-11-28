package com.scan.securityscan.rules.impl

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.psi.*
import com.scan.securityscan.rules.AbstractSecurityRule
import com.scan.securityscan.rules.RiskLevel

/**
 * Java 原生反序列化风险规则
 * 检测 ObjectInputStream.readObject() 等危险的反序列化操作
 */
class JavaDeserializationRule : AbstractSecurityRule() {
    
    override val ruleId = "JAVA_DESERIALIZATION"
    override val ruleName = "Java原生反序列化漏洞"
    override val description = "使用 ObjectInputStream 反序列化不受信任的数据可能导致远程代码执行"
    override val severity = RiskLevel.CRITICAL
    
    // 危险的反序列化方法
    private val dangerousMethods = setOf(
        "java.io.ObjectInputStream.readObject",
        "java.io.ObjectInputStream.readUnshared",
        "java.beans.XMLDecoder.readObject"
    )
    
    override fun matches(element: PsiElement): Boolean {
        if (element !is PsiMethodCallExpression) {
            return false
        }
        
        val method = element.resolveMethod() ?: return false
        val containingClass = method.containingClass ?: return false
        val className = containingClass.qualifiedName ?: return false
        val methodName = method.name
        
        val fullMethodName = "$className.$methodName"
        
        return dangerousMethods.any { fullMethodName == it }
    }
    
    override fun getProblemDescription(element: PsiElement): String {
        return """
            🚨 严重安全风险：Java 原生反序列化漏洞
            
            【问题】反序列化不受信任的数据可能导致远程代码执行（RCE）
            攻击者可以构造恶意的序列化数据，利用 gadget chain 执行任意代码
            
            【修复建议】
            1. 【最佳方案】使用 JSON 替代 Java 序列化
               ObjectMapper mapper = new ObjectMapper();
               MyClass obj = mapper.readValue(jsonString, MyClass.class);
            
            2. 【如果必须使用】添加反序列化过滤器（JDK 9+）
               ObjectInputFilter filter = ObjectInputFilter.Config.createFilter(
                   "com.yourpackage.*;!*"  // 只允许特定包的类
               );
               ObjectInputStream ois = new ObjectInputStream(inputStream);
               ois.setObjectInputFilter(filter);
            
            3. 重写 resolveClass 实现白名单
               class SecureObjectInputStream extends ObjectInputStream {
                   private static final Set<String> ALLOWED_CLASSES = 
                       Set.of("com.example.SafeClass1", "com.example.SafeClass2");
                   
                   @Override
                   protected Class<?> resolveClass(ObjectStreamClass desc) 
                           throws IOException, ClassNotFoundException {
                       if (!ALLOWED_CLASSES.contains(desc.getName())) {
                           throw new InvalidClassException("Unauthorized: " + desc.getName());
                       }
                       return super.resolveClass(desc);
                   }
               }
            
            【已知利用链】
            - Apache Commons Collections
            - Apache Commons BeanUtils
            - Spring Framework
            - Hibernate
            
            【著名漏洞】
            - WebLogic CVE-2015-4852 (CVSS 10.0)
            - JBoss CVE-2017-12149
        """.trimIndent()
    }
    
    override fun getQuickFixes(element: PsiElement): Array<LocalQuickFix> {
        // 不提供自动修复，避免类型不匹配等问题
        return emptyArray()
    }
    
    override fun getSecurityAdvice(): String {
        return """
            【修复建议】
            
            1. 【最佳方案】避免反序列化不受信任的数据
               - 使用 JSON、Protocol Buffers 等安全的序列化格式
               - 使用数字签名验证数据来源
            
            2. 【如果必须使用】添加反序列化过滤器（JDK 9+）
               
               ObjectInputFilter filter = ObjectInputFilter.Config.createFilter(
                   "com.yourpackage.*;!*"  // 只允许特定包的类
               );
               ObjectInputStream ois = new ObjectInputStream(inputStream);
               ois.setObjectInputFilter(filter);
            
            3. 【传统方案】重写 resolveClass 方法
            
               class SecureObjectInputStream extends ObjectInputStream {
                   private static final Set<String> ALLOWED_CLASSES = 
                       Set.of("com.example.SafeClass1", "com.example.SafeClass2");
                   
                   @Override
                   protected Class<?> resolveClass(ObjectStreamClass desc) 
                           throws IOException, ClassNotFoundException {
                       if (!ALLOWED_CLASSES.contains(desc.getName())) {
                           throw new InvalidClassException("Unauthorized class: " + desc.getName());
                       }
                       return super.resolveClass(desc);
                   }
               }
            
            4. 【防御措施】
               - 禁用 Java 反序列化（在容器/JVM 层面）
               - 使用安全管理器限制权限
               - 移除危险的依赖库
            
            【国内审计要求】
            - 奇安信：重点检查项，必须修复
            - 等保测评：高风险项
            - 历史上多个严重漏洞（WebLogic、JBoss 等）
            
            【安全替代方案】
            
            // ❌ 不安全：Java 原生序列化
            ObjectInputStream ois = new ObjectInputStream(inputStream);
            Object obj = ois.readObject();
            
            // ✅ 安全：使用 JSON
            ObjectMapper mapper = new ObjectMapper();
            MyClass obj = mapper.readValue(jsonString, MyClass.class);
            
            // ✅ 安全：使用 Protocol Buffers
            MyProto.MyMessage obj = MyProto.MyMessage.parseFrom(bytes);
        """.trimIndent()
    }
}
