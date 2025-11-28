#!/bin/bash

# ==========================================
# 代码安全扫描助手 - Linux/Mac 构建运行脚本
# ==========================================

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 显示菜单
show_menu() {
    clear
    echo -e "${BLUE}===============================================${NC}"
    echo -e "${BLUE}   代码安全扫描助手 - 构建和运行${NC}"
    echo -e "${BLUE}===============================================${NC}"
    echo ""
    echo "请选择操作："
    echo ""
    echo "[1] 构建插件"
    echo "[2] 运行插件（开发模式）"
    echo "[3] 构建并运行"
    echo "[4] 清理构建"
    echo "[5] 查看帮助"
    echo "[0] 退出"
    echo ""
}

# 构建插件
build_plugin() {
    echo ""
    echo -e "${BLUE}==========================================${NC}"
    echo -e "${BLUE} 开始构建插件...${NC}"
    echo -e "${BLUE}==========================================${NC}"
    echo ""
    
    ./gradlew buildPlugin
    
    if [ $? -eq 0 ]; then
        echo ""
        echo -e "${GREEN}==========================================${NC}"
        echo -e "${GREEN} ✅ 构建成功！${NC}"
        echo -e "${GREEN} 插件文件位于: build/distributions/${NC}"
        echo -e "${GREEN}==========================================${NC}"
        echo ""
    else
        echo ""
        echo -e "${RED}==========================================${NC}"
        echo -e "${RED} ❌ 构建失败！请检查错误信息${NC}"
        echo -e "${RED}==========================================${NC}"
        echo ""
    fi
}

# 运行插件
run_plugin() {
    echo ""
    echo -e "${BLUE}==========================================${NC}"
    echo -e "${BLUE} 启动插件开发环境...${NC}"
    echo -e "${BLUE} 这将打开一个新的 IDEA 实例，插件已安装${NC}"
    echo -e "${BLUE}==========================================${NC}"
    echo ""
    
    ./gradlew runIde
}

# 构建并运行
build_and_run() {
    echo ""
    echo -e "${BLUE}==========================================${NC}"
    echo -e "${BLUE} 构建并运行插件...${NC}"
    echo -e "${BLUE}==========================================${NC}"
    echo ""
    
    ./gradlew buildPlugin
    
    if [ $? -eq 0 ]; then
        echo ""
        echo "构建成功！正在启动插件..."
        echo ""
        ./gradlew runIde
    fi
}

# 清理构建
clean_build() {
    echo ""
    echo -e "${BLUE}==========================================${NC}"
    echo -e "${BLUE} 清理构建文件...${NC}"
    echo -e "${BLUE}==========================================${NC}"
    echo ""
    
    ./gradlew clean
    
    if [ $? -eq 0 ]; then
        echo ""
        echo -e "${GREEN}✅ 清理完成！${NC}"
        echo ""
    else
        echo ""
        echo -e "${RED}❌ 清理失败！${NC}"
        echo ""
    fi
}

# 显示帮助
show_help() {
    clear
    echo -e "${BLUE}==========================================${NC}"
    echo -e "${BLUE} 帮助信息${NC}"
    echo -e "${BLUE}==========================================${NC}"
    echo ""
    echo "本脚本用于快速构建和测试插件。"
    echo ""
    echo -e "${YELLOW}[构建插件]${NC}"
    echo "  将插件编译打包为 .zip 文件"
    echo "  位置: build/distributions/SecurityScan-1.0-SNAPSHOT.zip"
    echo ""
    echo -e "${YELLOW}[运行插件（开发模式）]${NC}"
    echo "  启动一个新的 IDEA 实例"
    echo "  插件自动安装并启用"
    echo "  可以实时测试插件功能"
    echo ""
    echo -e "${YELLOW}[构建并运行]${NC}"
    echo "  先构建，再运行"
    echo ""
    echo -e "${YELLOW}[清理构建]${NC}"
    echo "  删除所有构建产物"
    echo ""
    echo -e "${BLUE}==========================================${NC}"
    echo -e "${BLUE} 测试插件的步骤${NC}"
    echo -e "${BLUE}==========================================${NC}"
    echo ""
    echo "1. 选择 [2] 运行插件"
    echo "2. 等待新的 IDEA 窗口打开"
    echo "3. 在新窗口中创建或打开 Java 项目"
    echo "4. 复制测试文件："
    echo "   src/main/resources/examples/SecurityTestExamples.java"
    echo "5. 打开测试文件，观察安全警告"
    echo "6. 将鼠标悬停在波浪线上查看详情"
    echo "7. 按 Alt+Enter (或 Option+Enter) 查看修复选项"
    echo ""
    echo -e "${BLUE}==========================================${NC}"
    echo ""
}

# 主循环
while true; do
    show_menu
    read -p "请输入选项 (0-5): " choice
    
    case $choice in
        1)
            build_plugin
            read -p "按回车键继续..."
            ;;
        2)
            run_plugin
            read -p "按回车键继续..."
            ;;
        3)
            build_and_run
            read -p "按回车键继续..."
            ;;
        4)
            clean_build
            read -p "按回车键继续..."
            ;;
        5)
            show_help
            read -p "按回车键继续..."
            ;;
        0)
            echo ""
            echo "再见！"
            echo ""
            exit 0
            ;;
        *)
            echo ""
            echo -e "${RED}无效选项，请重新选择${NC}"
            sleep 2
            ;;
    esac
done

