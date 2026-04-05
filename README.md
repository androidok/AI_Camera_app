# CameraX-AI 驱动的智能摄影助手

> **让每一拍都成为佳作**

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com/)
[![Language](https://img.shields.io/badge/Language-Kotlin-blue.svg)](https://kotlinlang.org/)
[![UI](https://img.shields.io/badge/UI-Jetpack%20Compose-blue.svg)](https://developer.android.com/jetpack/compose)

大学生软件创新赛事参赛作品 - 基于 Android 平台的智能摄影辅助应用

---

## 📱 项目简介

CameraX-AI 是一款聚焦"**拍前构图辅助**"核心需求的智能摄影应用，通过 AI 技术为摄影爱好者提供：

- 🎯 **实时构图辅助** - AI 实时检测主体位置，提供调整建议
- 🏞️ **场景智能识别** - 自动识别人像、风景、美食、夜景等场景
- ✂️ **智能裁剪优化** - 基于美学模型的自动裁剪建议
- 🎨 **AI 色彩增强** - 一键智能调色，保持自然观感
- 🔒 **隐私保护** - 纯本地化处理，照片数据不出设备

---

## 🎯 核心亮点

| 特性 | 说明 | 状态 |
|------|------|------|
| **拍前辅助** | 区别于传统后期修图，从源头提升质量 | ✅ 架构完成 |
| **边缘 AI** | 所有 AI 推理在设备端完成，低延迟 | ⏳ 待接入 |
| **多模型融合** | 场景识别 + 人脸检测 + 构图分析 + 色彩分析 | ⏳ 待接入 |
| **隐私保护** | 无需网络，照片数据不出设备 | ✅ 架构保证 |

---

## 📊 开发进度

### 总体进度：50%

| 模块 | 进度 | 状态 |
|------|------|------|
| **UI 界面** | 100% | ✅ 完成 |
| **组件库** | 100% | ✅ 完成 |
| **相机预览** | 100% | ✅ 完成 |
| **导航系统** | 100% | ✅ 完成 |
| **AI 场景识别** | 0% | ⏳ 待开发 |
| **AI 构图分析** | 0% | ⏳ 待开发 |
| **AI 智能裁剪** | 0% | ⏳ 待开发 |
| **AI 色彩增强** | 0% | ⏳ 待开发 |
| **图片处理** | 0% | ⏳ 待开发 |

---

## 🏗️ 技术架构

```
┌─────────────────────────────────────┐
│      UI 层 (Jetpack Compose)       │
│  Splash | Camera | Edit | Crop     │
│  ColorAdjust | Settings            │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│    业务逻辑层 (Kotlin + ViewModel)  │
│  CameraManager | AIAnalyzer         │
│  ImageProcessor | DataManager       │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│      AI 与图像处理层                │
│  ML Kit | ONNX | OpenCV | GPUImage  │
└─────────────────────────────────────┘
```

### 技术栈

| 类别 | 技术 | 版本 |
|------|------|------|
| **语言** | Kotlin | 2.0.21 |
| **UI 框架** | Jetpack Compose | 1.5.4 |
| **设计系统** | Material 3 | - |
| **相机 API** | CameraX | 1.1.0-beta01 |
| **图片加载** | Coil | 2.4.0 |
| **权限管理** | Accompanist | 0.32.0 |
| **AI 推理** | ONNX Runtime | Latest (规划) |
| **场景识别** | ML Kit | 17.0.7 (规划) |

---

## 📦 功能模块

### 1. 启动界面 (SplashScreen)
- 应用启动欢迎页面
- 三大核心功能展示
- 点击启动进入主界面

### 2. 相机界面 (CameraScreen)
- 实时相机预览
- 三分法构图辅助线
- 场景识别显示
- AI 构图提示气泡
- 相机参数显示（ISO、快门、光圈）
- 闪光灯控制、前后摄像头切换

### 3. 编辑界面 (EditScreen)
- 图片预览
- 工具选择（裁剪、调色、旋转）
- 导航到子功能

### 4. 智能裁剪 (CropScreen)
- AI 自动识别主体并建议裁剪框
- 用户手动调整
- 九宫格辅助线
- 多种宽高比选择

### 5. 调色界面 (ColorAdjustScreen)
- 6 个调色参数滑块
- AI 智能增强一键应用
- 实时预览效果

### 6. 设置界面 (SettingsScreen)
- EV 曝光补偿
- ISO 感光度
- 快门速度
- HDR 模式

---

## 📂 项目结构

```
AI_Camera_app-main/
├── app/
│   ├── src/main/
│   │   ├── java/com/aicamera/app/
│   │   │   ├── MainActivity.kt              # 主活动
│   │   │   └── ui/
│   │   │       ├── screens/                 # 6 个界面
│   │   │       ├── components/              # 10 个组件
│   │   │       └── theme/                   # 主题系统
│   │   ├── res/                             # 资源文件
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── gradle/
│   └── libs.versions.toml                   # 版本目录
├── licenses/                                # 许可证文本
├── docs/                                    # 文档（如有）
├── LICENSE                                  # 开源许可证
├── NOTICE                                   # 第三方声明
├── README.md                                # 本文件
├── PROJECT_SUMMARY.md                       # 作品简介
├── PROJECT_DOCUMENTATION.md                 # 完整项目文档
├── OPEN_SOURCE_USAGE.md                     # 开源组件说明
├── BACKEND_API_DOCUMENTATION.md             # API 接口文档
└── DOCUMENTATION_INDEX.md                   # 文档索引
```

---

## 🚀 快速开始

### 环境要求

- Android Studio Hedgehog | 2023.1.1 或更高版本
- JDK 11 或更高版本
- Android SDK API 36
- Gradle 8.14

### 安装步骤

1. **克隆项目**
```bash
git clone https://github.com/your-username/ai-camera.git
cd ai-camera
```

2. **使用 Android Studio 打开项目**

3. **同步 Gradle 项目**
   - 点击 "Sync Project with Gradle Files"

4. **运行应用**
   - 连接 Android 设备或启动模拟器
   - 点击 "Run" 按钮

### 构建配置

编辑 `app/build.gradle.kts` 可自定义配置：

```kotlin
android {
    namespace = "com.aicamera.app"
    compileSdk = 36
    
    defaultConfig {
        applicationId = "com.aicamera.app"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }
}
```

---

## 📖 文档导航

### 核心文档

| 文档 | 说明 | 用途 |
|------|------|------|
| [作品简介](./PROJECT_SUMMARY.md) | 精简版项目介绍 | 比赛申报、快速了解 |
| [完整文档](./PROJECT_DOCUMENTATION.md) | 详细项目文档 | 技术评审、开发参考 |
| [API 文档](./BACKEND_API_DOCUMENTATION.md) | 接口定义与规范 | 前后端对接 |
| [开源说明](./OPEN_SOURCE_USAGE.md) | 依赖与许可证说明 | 合规性审查 |
| [文档索引](./DOCUMENTATION_INDEX.md) | 所有文档的导航 | 快速查找 |

### 推荐阅读顺序

1. **初次了解**: README.md → PROJECT_SUMMARY.md
2. **技术评审**: PROJECT_DOCUMENTATION.md → BACKEND_API_DOCUMENTATION.md
3. **开发参考**: BACKEND_API_DOCUMENTATION.md → 源代码
4. **合规审查**: OPEN_SOURCE_USAGE.md → LICENSE → NOTICE

---

## 🎨 界面预览

### 已实现界面

- ✅ **启动页** - Material 3 设计，功能卡片展示
- ✅ **相机页** - 实时预览，构图辅助线，场景识别
- ✅ **编辑页** - 图片预览，工具选择
- ✅ **裁剪页** - 智能裁剪框，九宫格辅助线
- ✅ **调色页** - 6 参数滑块，AI 增强卡片
- ✅ **设置页** - 相机参数调节

### 组件库

已封装 10 个高质量可复用组件：
- CircleIconButton - 圆形图标按钮
- PrimaryButton - 主按钮
- FeatureCard - 功能卡片
- TopBarWithActions - 顶部操作栏
- ToolButton - 工具按钮
- LabeledSlider - 带标签滑块
- AIEnhanceCard - AI 增强卡片
- StatusLabel - 状态标签
- CaptureButton - 拍摄按钮
- CameraParams - 相机参数显示

---

## 🔧 待开发功能

### P0 核心功能
- [ ] 拍照并保存图片
- [ ] 场景识别（ML Kit 接入）
- [ ] AI 构图分析（模型集成）
- [ ] AI 智能裁剪识别
- [ ] 图片裁剪执行
- [ ] AI 色彩增强
- [ ] 滤镜应用（GPUImage）

### P1 重要功能
- [ ] 相机参数获取（ISO、快门、光圈）
- [ ] 闪光灯控制
- [ ] 前后摄像头切换
- [ ] 图片保存到相册
- [ ] 缓存管理

### P2 增强功能
- [ ] HDR 模式控制
- [ ] 定时器功能
- [ ] 对焦控制
- [ ] 变焦控制

---

## 🏆 比赛信息

### 参赛类别
大学生软件创新赛事 - 移动应用开发

### 创新点

1. **技术创新**
   - 边缘 AI 推理优化（<200ms 延迟）
   - 多模型融合架构
   - 性能优化策略（内存、CPU）

2. **产品创新**
   - "拍前辅助"理念（区别于后期修图）
   - 自然增强美学（反对过度美颜）
   - 隐私保护架构（纯本地化）

3. **商业模式创新**
   - 免费 + 增值服务
   - 技术授权可能性

### 预期成果
- ✅ 完整的 Android 应用（APK）
- ✅ 开源代码（Apache 2.0）
- ✅ 技术文档（完整体系）
- ⏳ 性能优化方案（论文/专利）

---

## 👥 团队分工（建议）

| 角色 | 人数 | 职责 | 状态 |
|------|------|------|------|
| **项目负责人** | 1 | 整体规划、进度管理 | ✅ |
| **前端开发** | 2 | UI 界面、CameraX 集成 | ✅ 已完成 |
| **AI 算法** | 2 | 模型训练、优化、部署 | ⏳ 待加入 |
| **后端开发** | 1 | 图片处理、数据存储 | ⏳ 待加入 |
| **测试工程师** | 1 | 功能测试、性能测试 | ⏳ 待加入 |

---

## 📅 开发计划

### 短期（1-2 周）
- [ ] 实现拍照功能
- [ ] 接入 ML Kit 场景识别
- [ ] 接入 ML Kit 人脸检测
- [ ] 实现基础构图分析
- [ ] 实现图片裁剪
- [ ] 实现基础滤镜

### 中期（3-4 周）
- [ ] 训练构图分析模型
- [ ] 训练色彩增强模型
- [ ] 优化模型性能（量化、剪枝）
- [ ] 实现智能帧率控制
- [ ] 性能优化（内存、CPU）

### 长期（5-8 周）
- [ ] 实现高级相机功能（HDR、定时器）
- [ ] 优化 UI 细节和动画
- [ ] 用户测试和反馈收集
- [ ] 准备比赛材料（PPT、视频、文档）

---

## 📊 代码统计

| 类别 | 文件数 | 代码行数 |
|------|--------|----------|
| UI 界面 | 6 | ~1800 |
| 组件库 | 1 | ~500 |
| 主题系统 | 3 | ~150 |
| 导航系统 | 1 | ~150 |
| **总计** | **11** | **~2600** |

---

## 🔐 开源协议

本项目采用 **Apache License 2.0** 开源协议

- ✅ 允许商业使用
- ✅ 允许修改和分发
- ✅ 允许专利使用
- ⚠️ 需保留许可证声明
- ⚠️ 需声明修改内容

详见 [LICENSE](LICENSE) 和 [NOTICE](NOTICE) 文件。

---

## 🙏 致谢

感谢以下开源项目的贡献：

- [AndroidX](https://developer.android.com/jetpack/androidx) - 现代化 Android 开发库
- [Jetpack Compose](https://developer.android.com/jetpack/compose) - 声明式 UI 框架
- [CameraX](https://developer.android.com/training/camerax) - 相机 API 封装
- [ML Kit](https://developers.google.com/ml-kit) - 移动端 AI 能力（规划）
- [Coil](https://github.com/coil-kt/coil) - 图片加载库
- [Accompanist](https://github.com/google/accompanist) - Compose 扩展
- [Material Components](https://github.com/material-components/material-components-android) - 设计系统

详见 [OPEN_SOURCE_USAGE.md](OPEN_SOURCE_USAGE.md)

---

## 📧 联系方式

- **项目名称**: CameraX-AI 驱动的智能摄影助手
- **参赛类别**: 大学生软件创新赛事
- **指导老师**: [待填写]
- **团队名称**: [待填写]
- **联系邮箱**: [待填写]
- **项目仓库**: [待填写]

---

## 📝 更新日志

### v1.0.0 (2026-03-21)
- ✅ 完成所有 UI 界面（6 个）
- ✅ 封装 10 个可复用组件
- ✅ 实现相机预览功能
- ✅ 建立完整导航系统
- ✅ 创建项目文档体系
- ✅ 创建 API 接口文档
- ✅ 创建开源合规文档

---

## 📄 相关文档

- [作品简介](PROJECT_SUMMARY.md) - 精简版项目介绍
- [完整文档](PROJECT_DOCUMENTATION.md) - 详细技术文档
- [API 文档](BACKEND_API_DOCUMENTATION.md) - 接口定义
- [开源说明](OPEN_SOURCE_USAGE.md) - 依赖与许可证
- [文档索引](DOCUMENTATION_INDEX.md) - 文档导航

---

**最后更新**: 2026-03-21  
**版本**: v1.0.0  
**状态**: 🚧 开发中

---

*CameraX-AI 驱动的智能摄影助手 - 大学生软件创新赛事参赛作品*

*让每一拍都成为佳作*
