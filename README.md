# Picall — 专业胶片风格图片编辑 App

**v1.0.0** | Kotlin + Jetpack Compose | Material 3 暗色主题 | Android 8.0+

## 功能

- **胶片预设列** — 10 个内置胶片风格 + 自定义预设，拟物化胶片卷轴卡片，点击即时预览
- **色彩配方** — 亮度/颜色/RGB原色/LCH/效果 多组滑块调节，每组独立强度控制
- **LUT 导入** — 支持 .cube 格式 3D LUT，强度可调，与配方组合使用
- **曲线编辑** — Catmull-Rom 样条曲线，4 通道 (W/R/G/B)，5 节点拖拽，S 曲线预设
- **直方图** — RGB + 亮度 4 通道实时直方图
- **预设管理** — 2 列胶片卡片网格，搜索、保存、删除、一键加载
- **一键导出** — JPEG 95% 质量保存到相册

## 构建

```bash
./gradlew assembleDebug
```

APK 输出：`app/build/outputs/apk/debug/app-debug.apk`

## 技术栈

Kotlin 2.1.0 · Jetpack Compose · Room 2.6.1 · Gson · Coroutines · Coil · Navigation Compose · KSP · Gradle 8.9 · AGP 8.7.3
