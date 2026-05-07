@echo off
chcp 65001 >nul
echo ========================================
echo 洛阳景区问答智能体 - 前端项目启动
echo ========================================
echo.

cd /d "%~dp0frontend"

if not exist "node_modules" (
    echo [提示] 首次运行，正在安装依赖...
    call npm install
    if errorlevel 1 (
        echo [错误] 依赖安装失败！
        pause
        exit /b 1
    )
    echo [成功] 依赖安装完成！
    echo.
)

echo [信息] 正在启动开发服务器...
echo [信息] 服务器地址: http://localhost:3000
echo.
echo [提示] 按 Ctrl+C 可停止服务器
echo.

call npm run dev

pause
