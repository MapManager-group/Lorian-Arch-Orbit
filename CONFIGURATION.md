# 配置文件与故障排查

配置目录为 `.minecraft/config/lorian_arch_orbit/`。当前配置 schema 为 `3`；未知字段会保留，非法字段会回退到安全默认值并写入日志。

## 文件

- `client.json`：客户端功能、行为和 HUD。
- `server.json`：服务端交互距离授权策略。
- `lorian_arch_orbit-wheel-primary.json`：一级色轮自定义覆盖。
- `lorian_arch_orbit-wheel-secondary.json`：二级色轮自定义覆盖。
- `palette-shares/`：色轮分享 JSON 的导入导出目录。

首次启动会自动创建文件。外部编辑采用约 250 ms 防抖热重载，也可执行客户端命令 `/lorian_arch_orbit reload`。编辑器使用原子写入；迁移旧 schema 前会生成同目录 `.bak` 备份。

## 客户端配置

`client.json` 的主要字段：

- `features.<功能>.enabled`：独立开关。
- `features.reach_extension.distance`：请求距离，范围 5–128，最终值由服务端确认。
- `features.palette_wheel.animation`：`clockwise`、`expand` 或 `none`。
- `features.palette_wheel.primary_default_preset` / `secondary_default_preset`：`item_tag_a`（同类方块）、`item_tag_b`（同系列方块）或 `color_categories`（颜色分类）。这些内部枚举名为兼容已有配置而保留，界面不显示开发代号。
- `features.smart_pick.mode`：`adjacent`、`range` 或 `context`。
- `features.connected_texture_fix.walls|beds|doors|pistons|nether_portals|end_portals`：各类连接面修复开关；活塞修复伸出的普通与粘性活塞底座和活塞头之间的连接截面，两个传送门修复可独立开关且默认开启。旧配置中的 `chests` 字段会保留但不再使用；此前的单一 `portals` 字段会作为两个新开关的默认值。
- `features.invisible_blocks.currently_visible`：上次显示状态；`show_barriers` 与 `show_light_blocks` 控制类型。
- `ui.hud_enabled`：是否允许功能显示 HUD。

色轮文件的 `groups` 数组仅保存用户覆盖。删除某个内置组的覆盖后会自动回退到当前选择的内置预设。

## 服务端配置

`server.json` 只控制交互距离：

- `enabled`：默认 `false`，管理员必须明确启用。
- `maximum_distance`：服务端允许的最大值，范围 5–128。
- `creative_only`：默认仅允许创造模式。
- `required_permission_level`：0–4。
- `requests_per_second`：每名玩家每秒 1–40 次，默认 10。

客户端没有本 Mod 时可正常加入，且不会获得扩展距离。服务端没有本 Mod、功能关闭或协议版本不同，客户端会保持原版 5 格并显示原因。断线、切换模式或功能关闭时，服务端清除已应用的距离修改。

## 升级与兼容

- schema `0`、`1`、`2` 会自动迁移到 `3`，同时保留未知字段和原文件备份。
- 高于当前版本的 schema 会被拒绝，不会用旧程序覆盖新配置。
- 当前交互距离协议版本为 `1`。协议不一致会安全拒绝距离请求；修改数据包结构或语义时必须提升协议版本。
- 色轮分享格式单独版本化；不支持的分享版本只拒绝导入，不修改现有色轮。

## 故障排查

- 无法打开配置：安装与加载器匹配的 YACL；Fabric 可通过 Mod Menu 或 `O` 打开，NeoForge 可从模组列表进入。
- 修改未生效：确认 JSON 可解析，执行 `/lorian_arch_orbit reload`，再查看日志中的 `lorian_arch_orbit` 警告。
- 配置损坏：Mod 会保留损坏文件并继续使用上一份有效值或内存默认值。修正文件或移走它后重新加载；不要先删除 `.bak`。
- 交互距离始终为 5：确认服务端也安装本 Mod 与 Architectury API，并在 `server.json` 开启功能；同时检查模式、权限和协议提示。
- 按键无反应：在“控制”中搜索 `Lorian’s Arch Orbit` 并检查冲突。整合包常见冲突见集成说明。
- 模型或透明显示异常：先关闭资源包、Sodium/Embeddium 与光影定位组合；连接材质修复开关会触发资源重载，不要连续快速切换。
