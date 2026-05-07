# 🚀 快速开始指南

## 一键启动

### Windows用户
双击以下任一文件：

- **`start-frontend.bat`** - 启动开发服务器
- **`build.bat`** - 构建生产版本

### 手动启动
```bash
cd frontend
npm install    # 首次运行需要
npm run dev    # 启动开发服务器
```

## 访问地址

**开发服务器**: http://localhost:3000

## 主要功能

### 🎯 对话页面
- 6个快捷操作入口
- 热门问题推荐
- 实时AI对话
- 流式输出效果
- 智能卡片展示

### 📊 知识库管理
- 6种知识库类型
- 数据增删改查
- 批量导入导出
- 图片上传预览
- 搜索筛选功能

## 技术栈

- Vue 3.4 + Vite 5.0
- Element Plus 2.5
- Pinia + Vue Router
- Axios + Sass

## 项目结构

```
frontend/
├── src/
│   ├── views/          # 页面组件
│   ├── components/     # 公共组件
│   ├── api/           # API接口
│   ├── router/       # 路由配置
│   ├── styles/       # 全局样式
│   └── utils/        # 工具函数
├── dist/             # 构建产物
└── package.json      # 项目配置
```

## 常用命令

```bash
npm install          # 安装依赖
npm run dev          # 启动开发服务器
npm run build        # 构建生产版本
npm run preview      # 预览构建结果
```

## 注意事项

⚠️ **当前状态**:
- ✅ 前端界面已完成
- ⏳ 需要对接后端API
- ⏳ 需要配置真实数据

📝 **下一步**:
1. 启动SpringBoot后端服务
2. 配置API地址
3. 测试前后端联调

## 获取帮助

- 📖 详细文档: `frontend/README.md`
- 📊 项目状态: `PROJECT_STATUS.md`
- 🎨 设计说明: `PROJECT_README.md`

---

**现在就开始体验吧！** 🎉
