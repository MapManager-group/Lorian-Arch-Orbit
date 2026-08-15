# Lorian’s Arch Orbit

> [!NOTE]
> This is a project by AI. If you mind this, please do not use this mod.

<img src="https://cdn.jsdelivr.net/gh/MapManager-group/Lorian-Arch-Orbit@main/docs/image/icon.png" width="400" alt="色轮在自己动w~">

面向创造模式建筑师、地图作者和整合包团队的建筑辅助 Mod。支持 Minecraft 26.2、Fabric 与 NeoForge，主要功能均可独立开关并热重载。

## 基本功能

默认提供 `O` 打开配置界面。所有按键都可在 Minecraft 按键设置中修改。  
除 **交互距离修改** 外，其余功能均为客户端功能。

### 一级/二级物品色轮

- 默认按住 `R` 打开**一级色轮**，连续按下两次 `R` 打开**二级色轮**，使用滚轮选择物品
- 默认按 `P` 打开**可视化色轮编辑器**，并支持 **JSON/分享码导入导出**

![](./docs/image/1.png)

![](./docs/image/2.png)

### 智能中键选取

- 中键短按保持原版选取，按住约 100 ms 后打开**候选轮盘**，选择周围方块
- 候选轮盘提供三种模式：**相邻模式、范围模式、上下文模式**，默认启用上下文模式

![](./docs/image/3.png)

### 交互距离修改

- 默认按住 `G` 并使用鼠标滚轮动态调节 5–128 格交互距离
- 该功能必须在客户端和服务端同时安装本 Mod，并由服务端配置授权
- 该功能默认关闭，原因是在Fabric侧可以使用Axiom的 `无限触距` 以取代此功能

![](./docs/image/4.png)

### 连接材质修复

- 补齐标准墙、床、门、伸出活塞、地狱传送门四个边缘面及末地传送门五个非顶面的材质
- 所有修复均可在配置中动态更改并重载

![](./docs/image/5.png)

### 不可见方块显示

- 默认按 `V` 同时切换屏障和光源方块，可在配置中筛选类型

![](./docs/image/6.png)


## 依赖

### Fabric

- [Fabric API](https://modrinth.com/mod/fabric-api)
- [Mod Menu](https://modrinth.com/mod/modmenu)
- [Architectury API](https://modrinth.com/mod/architectury-api)
- [YetAnotherConfigLib (YACL)](https://modrinth.com/mod/yacl)

### Neoforge

- [Architectury API](https://modrinth.com/mod/architectury-api)
- [YetAnotherConfigLib (YACL)](https://modrinth.com/mod/yacl)


## 文档

- [配置文件与故障排查](CONFIGURATION.md)
- [向上迁移指南](docs/UPWARD_MIGRATION.md)
- [向下迁移指南](docs/DOWNWARD_MIGRATION.md)
- [更新记录](UPDATE_NOTES.md)


## TODO

1. 箱子连接材质修复（暂未修复：关闭方块更新后移除双箱一半时，另一半的裸露连接面仍需开箱后才会更新）
2. 结构方块显示
3. 自定义工具&命令轮盘


## 鸣谢

- [LotTweaks](https://github.com/aruma256/LotTweaks)：色轮、智能选取和交互距离的行为参考。
- [Visible Barriers](https://github.com/AmyMialeeMods/visiblebarriers)：不可见方块显示行为参考。
- 可爱的酒狐：贡献了该项目的Icon。

## 协议

[MIT License](LICENSE).
