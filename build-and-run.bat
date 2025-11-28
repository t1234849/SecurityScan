@echo off
REM ==========================================
REM 代码安全扫描助手 - Windows 构建运行脚本
REM ==========================================

echo.
echo ===============================================
echo   代码安全扫描助手 - 构建和运行
echo ===============================================
echo.

:menu
echo 请选择操作：
echo.
echo [1] 构建插件
echo [2] 运行插件（开发模式）
echo [3] 构建并运行
echo [4] 清理构建
echo [5] 查看帮助
echo [0] 退出
echo.

set /p choice=请输入选项 (0-5): 

if "%choice%"=="1" goto build
if "%choice%"=="2" goto run
if "%choice%"=="3" goto build_and_run
if "%choice%"=="4" goto clean
if "%choice%"=="5" goto help
if "%choice%"=="0" goto end
goto menu

:build
echo.
echo ==========================================
echo  开始构建插件...
echo ==========================================
echo.
call gradlew.bat buildPlugin
if %errorlevel% equ 0 (
    echo.
    echo ==========================================
    echo  ✅ 构建成功！
    echo  插件文件位于: build\distributions\
    echo ==========================================
    echo.
) else (
    echo.
    echo ==========================================
    echo  ❌ 构建失败！请检查错误信息
    echo ==========================================
    echo.
)
pause
goto menu

:run
echo.
echo ==========================================
echo  启动插件开发环境...
echo  这将打开一个新的 IDEA 实例，插件已安装
echo ==========================================
echo.
call gradlew.bat runIde
pause
goto menu

:build_and_run
echo.
echo ==========================================
echo  构建并运行插件...
echo ==========================================
echo.
call gradlew.bat buildPlugin
if %errorlevel% equ 0 (
    echo.
    echo 构建成功！正在启动插件...
    echo.
    call gradlew.bat runIde
)
pause
goto menu

:clean
echo.
echo ==========================================
echo  清理构建文件...
echo ==========================================
echo.
call gradlew.bat clean
if %errorlevel% equ 0 (
    echo.
    echo ✅ 清理完成！
    echo.
) else (
    echo.
    echo ❌ 清理失败！
    echo.
)
pause
goto menu

:help
echo.
echo ==========================================
echo  帮助信息
echo ==========================================
echo.
echo 本脚本用于快速构建和测试插件。
echo.
echo [构建插件]
echo   将插件编译打包为 .zip 文件
echo   位置: build\distributions\SecurityScan-1.0-SNAPSHOT.zip
echo.
echo [运行插件（开发模式）]
echo   启动一个新的 IDEA 实例
echo   插件自动安装并启用
echo   可以实时测试插件功能
echo.
echo [构建并运行]
echo   先构建，再运行
echo.
echo [清理构建]
echo   删除所有构建产物
echo.
echo ==========================================
echo  测试插件的步骤
echo ==========================================
echo.
echo 1. 选择 [2] 运行插件
echo 2. 等待新的 IDEA 窗口打开
echo 3. 在新窗口中创建或打开 Java 项目
echo 4. 复制测试文件：
echo    src\main\resources\examples\SecurityTestExamples.java
echo 5. 打开测试文件，观察安全警告
echo 6. 将鼠标悬停在波浪线上查看详情
echo 7. 按 Alt+Enter 查看修复选项
echo.
echo ==========================================
echo.
pause
goto menu

:end
echo.
echo 再见！
echo.
exit /b 0

