# Lorian’s Arch Orbit

面向创造模式建筑师、地图作者和整合包团队的建筑辅助 Mod。支持 Minecraft 26.2、Fabric 与 NeoForge，主要功能均可独立关闭。

## 基本功能

- 一级/二级物品色轮：按住 `R` 打开，滚轮选择；按 `P` 打开可视化编辑器，并支持 JSON/分享码导入导出。
- 智能选取：中键短按保持原版选取，按住约 100 ms 后打开候选轮盘。
- 交互距离：按住 `G` 并滚动调节 5–128 格；该功能必须在客户端和服务端同时安装本 Mod，并由服务端配置授权。
- 连接材质修复：补齐标准墙、床和门模型的连接截面；箱子尚未支持。
- 不可见方块显示：按 `V` 同时切换屏障和光源方块，可在配置中筛选类型。

默认还提供 `O` 打开配置界面。所有按键都可在 Minecraft 按键设置中修改。

## 安装

选择与加载器匹配的 JAR，并安装 Architectury API。Fabric 还需要 Fabric API。建议客户端安装 YACL；Fabric 可选装 Mod Menu。NeoForge 可直接从模组列表进入配置界面。

除“交互距离”外，其余功能均为客户端功能。服务端仅需 Architectury API 和本 Mod，不应安装 YACL、Mod Menu 等客户端组件。

## 文档

- [配置文件与故障排查](CONFIGURATION.md)
- [向上迁移指南](docs/UPWARD_MIGRATION.md)
- [向下迁移指南](docs/DOWNWARD_MIGRATION.md)
- [更新记录](UPDATE_NOTES.md)

## 整合包说明

仓库提供 [Preliminary Art Form 26.2 默认配置](integration/preliminary-art-form-26.2/README.md)。该整合包原有 LotTweaks 和 Visual Barriers 与本 Mod 功能重叠，集成时应先移除；默认 `V` 还与 WorldEdit CUI 冲突，需要在整合包 `options.txt` 中改绑其中一个。

## 鸣谢

- [LotTweaks](https://github.com/aruma256/LotTweaks)：色轮、智能选取和交互距离的行为参考。
- [Visible Barriers](https://github.com/AmyMialeeMods/visiblebarriers)：不可见方块显示行为参考。
- [Preliminary Art Form](https://github.com/MapManager-group/Preliminary-Art-Form-Modpack)：最初的目标整合包和兼容测试环境。
- 根目录开发资料中的方块分组与连接面复现资源仅用于行为分析；本项目代码、模型定义和图集配置均为独立实现，方块图标及基础材质来自 Minecraft 运行时资源。

## 协议

Copyright © 2026 DavidBlackCN。项目采用 [MIT License](LICENSE)。Minecraft 名称、图标与游戏资源归其权利人所有。
