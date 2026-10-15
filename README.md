# SmartNews

SmartNews 是一个用于学习与求职展示的智能资讯阅读 Android App。本项目基于 Google 官方 JetNews Compose 示例进行二次开发；当前阶段专注于建立稳定的品牌化工程基线。

项目改造方案见：[SmartNews Android 项目方案](SmartNews_Android_Project_Plan.md)。

## 当前功能

- Guardian Content API 资讯流与文章阅读
- 手机与平板自适应布局
- 首页、文章详情与兴趣偏好的可运行导航
- 浅色与深色 Material 3 主题

当前正式运行时数据源为 The Guardian Content API。

## 技术栈

- Kotlin
- Jetpack Compose
- Material 3
- Navigation 3

## 后续路线

- MVVM + StateFlow + Retrofit
- 搜索与分类浏览
- Room 收藏与阅读历史
- 缓存与分页加载
- 通知深链接
- 可选 AI 摘要

## 开发说明

### Guardian API Key 配置

1. 在仓库根目录的 `local.properties` 中添加：
   `GUARDIAN_API_KEY=你的GuardianKey`
2. `local.properties` 已被 Git 忽略，不会进入版本控制。
3. App 会通过 `BuildConfig.GUARDIAN_API_KEY` 在运行时读取该值；未配置时会在界面显示清晰错误，而不是崩溃。

本阶段不迁移 Kotlin 包名，仍保留 `com.example.jetnews`，以避免在品牌化基线阶段影响 Manifest、深链接、Glance Widget 和既有导航路径；包名迁移将在后续单独规划与验证。

本项目保留原 JetNews 示例所需的 Apache License 与版权声明。原始示例版权归 The Android Open Source Project 所有；本仓库的改造仅用于学习和二次开发。

## License

```
Copyright 2021 The Android Open Source Project

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
