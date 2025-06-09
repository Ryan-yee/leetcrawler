# LeetCode Crawler Plugin

[![Build](https://img.shields.io/badge/build-passing-brightgreen)](https://github.com/leetcrawler/leetcrawler)
[![Version](https://img.shields.io/badge/version-1.0.0-blue)](https://github.com/leetcrawler/leetcrawler/releases)
[![License](https://img.shields.io/badge/license-MIT-green)](LICENSE)

一个强大的IntelliJ平台插件，用于浏览、搜索和管理LeetCode题目，支持多种编程语言的代码模板插入。

## ✨ 特性

- 🔍 **智能搜索** - 快速搜索LeetCode题目，支持关键词匹配
- 📖 **题目浏览** - 分页浏览热门题目，支持加载更多
- 🌐 **双站点支持** - 支持LeetCode国际站和中文站
- 💻 **多语言支持** - 支持Java、Python、C++、Go、JavaScript等多种编程语言
- 🎯 **智能语言选择** - 根据当前IDE自动推荐最适合的编程语言
- ⚡ **延迟加载** - 优化性能，按需加载题目详情
- 📝 **代码模板插入** - 一键插入带题目注释的代码模板到编辑器

## 🚀 安装

### 方式1：从文件安装
1. 下载最新的 `leetcrawler-1.0.0.zip` 文件
2. 打开IDE，进入 `File` -> `Settings` -> `Plugins`
3. 点击齿轮图标，选择 `Install Plugin from Disk...`
4. 选择下载的zip文件并安装
5. 重启IDE

### 方式2：从源码构建
```bash
git clone https://github.com/yourusername/leetcrawler.git
cd leetcrawler
./gradlew buildPlugin
```

## 🎮 使用方法

### 1. 打开工具窗口
- 在IDE右侧工具栏找到 `LeetCode Crawler` 窗口
- 或使用快捷键 `Ctrl+Shift+L` 搜索题目

### 2. 浏览题目
- 点击 `刷新热门` 按钮加载热门题目
- 使用 `加载更多` 按钮获取更多题目
- 切换 `使用中文站点` 复选框选择不同的LeetCode站点

### 3. 搜索题目
- 在搜索框输入关键词
- 点击 `搜索` 按钮查找相关题目
- 支持题目标题和内容的模糊匹配

### 4. 查看题目详情
- 在题目列表中选择任意题目
- 详情会在右侧面板显示
- 支持 `题目详情` 和 `代码模板` 两个标签页

### 5. 插入代码模板
- 选择合适的编程语言
- 点击 `插入代码模板` 按钮
- 代码会自动插入到当前编辑器光标位置

## 🛠️ 开发环境

- **IntelliJ Platform SDK**: 2024.2+
- **Kotlin**: 1.9+
- **Gradle**: 8.0+
- **依赖库**:
  - OkHttp 4.12.0 (HTTP客户端)
  - Gson 2.10.1 (JSON解析)
  - Jsoup 1.17.2 (HTML解析)

## 🏗️ 项目结构

```
src/
├── main/
│   ├── kotlin/
│   │   └── com/leetcrawler/
│   │       ├── actions/           # IDE动作定义
│   │       ├── model/             # 数据模型
│   │       ├── service/           # 核心服务
│   │       └── toolwindow/        # UI组件
│   └── resources/
│       └── META-INF/
│           └── plugin.xml         # 插件配置
```

## 🤝 贡献

欢迎提交Pull Request或Issue！

1. Fork本项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送分支 (`git push origin feature/AmazingFeature`)
5. 创建Pull Request

## 📄 许可证

本项目采用MIT许可证 - 查看 [LICENSE](LICENSE) 文件了解详情。

## 🙏 致谢

- 感谢 [LeetCode](https://leetcode.com) 提供优秀的编程练习平台
- 感谢 [JetBrains](https://www.jetbrains.com) 提供强大的IDE开发平台
