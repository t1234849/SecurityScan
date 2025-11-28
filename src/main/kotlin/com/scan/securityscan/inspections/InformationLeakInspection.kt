package com.scan.securityscan.inspections

import com.scan.securityscan.rules.SecurityRule
import com.scan.securityscan.rules.impl.InformationLeakRule

/**
 * 系统信息泄露检查
 */
class InformationLeakInspection : SecurityInspectionBase() {
    
    override fun getSecurityRules(): List<SecurityRule> {
        return listOf(InformationLeakRule())
    }
    
    override fun getDisplayName(): String {
        return "系统信息泄露检测（代码质量）"
    }
    
    override fun getShortName(): String {
        return "InformationLeak"
    }
    
    override fun getStaticDescription(): String {
        return """
            检测是否直接返回异常详细信息给外部用户，防止系统信息泄露。
            
            系统信息泄露是常见的安全问题，会暴露：
            - 系统内部路径和文件结构
            - 数据库表结构和字段名
            - 第三方服务地址和配置
            - SQL 语句和参数
            - 堆栈信息和代码位置
            
            【检测范围】
            1. return 语句中使用 e.getMessage()
               return AjaxResult.error(e.getMessage());  // 危险
            
            2. return 语句中使用 e.getLocalizedMessage()
               return Response.error(e.getLocalizedMessage());  // 危险
            
            3. 直接返回异常对象的消息
               return new Result(false, exception.getMessage());  // 危险
            
            【推荐做法】
            1. 返回通用错误消息
               return AjaxResult.error("操作失败，请稍后重试");
            
            2. 返回业务相关错误（不含技术细节）
               return AjaxResult.error("请求地址" + requestURI + ",不支持" + method + "请求");
            
            3. 详细信息记录日志
               log.error("操作失败: {}", e.getMessage(), e);
               return AjaxResult.error("系统繁忙，请稍后重试");
            
            【可能泄露的信息示例】
            - "Table 'admin_user' doesn't exist" → 数据库表名
            - "/opt/app/config/database.properties not found" → 服务器路径
            - "Column 'admin_password' not found" → 字段名
            - "Connection refused: http://internal-api:8080" → 内网地址
            
            【国内审计要求】
            - 奇安信：不允许向外暴露系统内部信息
            - 等保测评：错误信息应该通用化
            - 不应暴露技术实现细节
            
            【修复优先级】
            ⚠️ 中等优先级 - 建议及时修复
        """.trimIndent()
    }
}

