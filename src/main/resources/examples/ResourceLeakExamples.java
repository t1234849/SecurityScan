import java.io.*;
import java.sql.*;
import java.net.Socket;

/**
 * 资源管理测试示例
 * 
 * 演示正确和错误的资源管理方式
 */
public class ResourceLeakExamples {
    
    // ============================================
    // 错误示例 1：资源未关闭
    // ============================================
    
    /**
     * ❌ 错误：InputStream 未关闭
     * 问题：资源泄漏，文件句柄耗尽
     */
    public void resourceNotClosed1(String filePath) throws IOException {
        // ⚠️ 这里会显示黄色波浪线
        InputStream in = new FileInputStream(filePath);
        byte[] buffer = new byte[1024];
        in.read(buffer);
        // 忘记关闭！
    }
    
    /**
     * ❌ 错误：数据库连接未关闭
     * 问题：连接池耗尽，服务不可用
     */
    public void resourceNotClosed2(Connection conn, String sql) throws SQLException {
        // ⚠️ 这些都会显示黄色波浪线
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(sql);
        
        while (rs.next()) {
            System.out.println(rs.getString(1));
        }
        // 忘记关闭！连接泄漏
    }
    
    /**
     * ❌ 错误：只在 try 中关闭，异常时无法关闭
     */
    public void resourceClosedInTry(String filePath) throws IOException {
        InputStream in = new FileInputStream(filePath);
        try {
            byte[] buffer = new byte[1024];
            in.read(buffer);
            in.close(); // 如果 read() 抛异常，这行不会执行
        } catch (IOException e) {
            // 异常处理
        }
        // 异常时资源未关闭！
    }
    
    // ============================================
    // 错误示例 2：使用 closeQuietly（不推荐）
    // ============================================
    
    /**
     * ❌ 不推荐：使用 IOUtils.closeQuietly()
     * 问题：吞掉异常，可能隐藏真正的问题
     */
    public void usingCloseQuietly(String filePath) {
        InputStream in = null;
        try {
            in = new FileInputStream(filePath);
            byte[] buffer = new byte[1024];
            in.read(buffer);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // ⚠️ 这里会显示黄色波浪线：不推荐使用 closeQuietly
            org.apache.commons.io.IOUtils.closeQuietly(in);
            // 问题：closeQuietly 会吞掉 close() 时的异常
            // 可能隐藏磁盘满、网络断开等问题
        }
    }
    
    // ============================================
    // 正确示例 1：try-with-resources（推荐）
    // ============================================
    
    /**
     * ✅ 推荐：使用 try-with-resources（Java 7+）
     * 优点：自动关闭，异常安全
     */
    public void correctWithTryWithResources1(String filePath) throws IOException {
        // 自动关闭，无需手动 close()
        try (InputStream in = new FileInputStream(filePath)) {
            byte[] buffer = new byte[1024];
            in.read(buffer);
        } // 自动调用 in.close()，即使发生异常
    }
    
    /**
     * ✅ 推荐：多个资源的 try-with-resources
     * 自动按相反顺序关闭
     */
    public void correctWithTryWithResources2(String srcPath, String destPath) throws IOException {
        try (InputStream in = new FileInputStream(srcPath);
             OutputStream out = new FileOutputStream(destPath)) {
            
            byte[] buffer = new byte[1024];
            int len;
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
        } // 先关闭 out，再关闭 in
    }
    
    /**
     * ✅ 推荐：数据库资源的 try-with-resources
     */
    public void correctWithTryWithResources3(Connection conn, String sql) throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                System.out.println(rs.getString(1));
            }
        } // 自动关闭：先 rs，再 pstmt
    }
    
    // ============================================
    // 正确示例 2：在 finally 中显式关闭
    // ============================================
    
    /**
     * ✅ 正确：在 finally 中显式关闭
     * 传统方式，适用于 Java 6 及以下
     */
    public void correctWithFinally1(String filePath) throws IOException {
        InputStream in = null;
        try {
            in = new FileInputStream(filePath);
            byte[] buffer = new byte[1024];
            in.read(buffer);
        } finally {
            // 在 finally 中关闭，确保异常时也能关闭
            if (in != null) {
                in.close(); // 显式关闭，异常会被抛出
            }
        }
    }
    
    /**
     * ✅ 正确：多个资源在 finally 中关闭
     * 注意按相反顺序关闭
     */
    public void correctWithFinally2(String srcPath, String destPath) throws IOException {
        InputStream in = null;
        OutputStream out = null;
        try {
            in = new FileInputStream(srcPath);
            out = new FileOutputStream(destPath);
            
            byte[] buffer = new byte[1024];
            int len;
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
        } finally {
            // 按相反顺序关闭
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    // 记录日志
                    System.err.println("关闭输出流失败: " + e.getMessage());
                }
            }
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    // 记录日志
                    System.err.println("关闭输入流失败: " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * ✅ 正确：数据库资源在 finally 中关闭
     */
    public void correctWithFinally3(Connection conn, String sql) throws SQLException {
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = conn.prepareStatement(sql);
            rs = pstmt.executeQuery();
            
            while (rs.next()) {
                System.out.println(rs.getString(1));
            }
        } finally {
            // 按相反顺序关闭
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (pstmt != null) {
                try {
                    pstmt.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    // ============================================
    // 正确示例 3：使用 Hutool 工具类
    // ============================================
    
    /**
     * ✅ 推荐：使用 Hutool 的 IoUtil
     * Hutool 会正确处理资源关闭
     */
    public void correctWithHutool1(String filePath) {
        InputStream in = null;
        try {
            in = new FileInputStream(filePath);
            // 使用 Hutool 读取
            String content = cn.hutool.core.io.IoUtil.readUtf8(in);
            System.out.println(content);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // Hutool 的 close 会正确处理异常
            cn.hutool.core.io.IoUtil.close(in);
        }
    }
    
    /**
     * ✅ 推荐：直接使用 Hutool 的 FileUtil
     * 完全自动管理资源，无需手动关闭
     */
    public void correctWithHutool2(String filePath) {
        // FileUtil 自动管理资源
        String content = cn.hutool.core.io.FileUtil.readUtf8String(filePath);
        System.out.println(content);
        
        // 写文件也是自动管理
        cn.hutool.core.io.FileUtil.writeUtf8String("Hello", "output.txt");
    }
    
    /**
     * ✅ 推荐：Hutool 文件复制
     * 完全自动管理资源
     */
    public void correctWithHutool3(String srcPath, String destPath) {
        // 一行代码，自动管理所有资源
        cn.hutool.core.io.FileUtil.copy(srcPath, destPath, true);
    }
    
    // ============================================
    // 复杂场景示例
    // ============================================
    
    /**
     * ✅ 正确：复杂的文件处理（使用 try-with-resources）
     */
    public void complexFileProcessing(String inputFile, String outputFile) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(inputFile));
             BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            
            String line;
            while ((line = reader.readLine()) != null) {
                // 处理每一行
                String processed = line.toUpperCase();
                writer.write(processed);
                writer.newLine();
            }
        } // 自动关闭所有资源
    }
    
    /**
     * ✅ 正确：网络资源管理
     */
    public void networkResourceManagement(String host, int port) throws IOException {
        try (Socket socket = new Socket(host, port);
             InputStream in = socket.getInputStream();
             OutputStream out = socket.getOutputStream()) {
            
            // 发送数据
            out.write("Hello".getBytes());
            
            // 接收数据
            byte[] buffer = new byte[1024];
            int len = in.read(buffer);
            System.out.println(new String(buffer, 0, len));
            
        } // 自动关闭：in -> out -> socket
    }
    
    // ============================================
    // 总结
    // ============================================
    
    /**
     * 资源管理最佳实践：
     * 
     * 1. 【首选】使用 try-with-resources（Java 7+）
     *    - 代码简洁
     *    - 自动关闭
     *    - 异常安全
     *    - 按相反顺序关闭
     * 
     * 2. 【传统】在 finally 中显式关闭
     *    - 适用于 Java 6 及以下
     *    - 必须检查 null
     *    - 需要捕获关闭时的异常
     *    - 按相反顺序关闭
     * 
     * 3. 【推荐】使用 Hutool 等工具库
     *    - 自动管理资源
     *    - 减少模板代码
     *    - 更简洁易用
     * 
     * 4. 【禁止】使用 IOUtils.closeQuietly()
     *    - 吞掉异常
     *    - 隐藏问题
     *    - 不符合快速失败原则
     * 
     * 需要关闭的资源：
     * - InputStream / OutputStream
     * - Reader / Writer
     * - Connection / Statement / ResultSet
     * - Socket / ServerSocket
     * - Scanner
     * - RandomAccessFile
     * - ZipFile / ZipInputStream
     * - FileChannel
     */
}

