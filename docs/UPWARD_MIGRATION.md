# 向上迁移指南（面向 AI）

用于把 `26.2/` 迁移到更新版本，例如 `26.3/`。每次迁移从用户手动下载的对应版本 Architectury Mod Template 开始，不复制旧工程骨架。

## 强制流程

1. 读取仓库根 `AGENTS.md`、`CODE_REVIEW.md`、旧版与新版目录配置。若根目录存在 `VERSION_MIGRATION.md`，也必须完整遵守。
2. 锁定唯一目标目录，先输出 Migration Plan：基线版本、映射、Java、Gradle、Loom、Loader、Fabric API、Architectury、NeoForge、YACL、Mod Menu、预计 API 差异、验证矩阵。等待用户明确批准后再改代码。
3. 在新模板目录执行未修改的基线构建，记录最早的真实错误。不得把旧版 `build.gradle`、Wrapper、Mixin JSON 或依赖版本整份覆盖到新模板。
4. 按“纯 Java 逻辑 → 公共 Minecraft API → Fabric → NeoForge → Mixin/模型资源 → 测试与文档”的顺序迁移，每一层都先编译。

## 必查差异

- 映射以新版 Loom `mappings` 配置为准；26.2 使用无混淆命名并不保证后续版本仍相同。
- 核对所有事件、网络 payload/codec、玩家交互距离属性、按键、HUD、屏幕绘制、创造物品页和模型烘焙 API。
- 对每个 Mixin 重新确认目标类、完整方法描述符、注入点和 cancellable 语义；不得凭旧版签名猜测。
- 核对方块状态、墙/床/门模型模板、屏障与 0–15 光源模型、图集 source JSON 格式。资源能加载不代表 UV 和透明排序正确。
- 从新版注册表重新审计默认色轮预设：新增/移除/重命名方块必须落实到资源和测试，不能仅改显示名。
- 检查客户端类是否泄漏到专用服务器。YACL 与 Mod Menu 保持客户端可选依赖。

## 兼容策略

- 配置 schema 当前为 `2`，网络协议为 `1`，色轮分享格式独立版本化。仅当格式或语义改变时提升版本，并为所有已发布旧版本编写逐级迁移；不得静默重解释旧字段。
- 协议不一致必须保持安全拒绝，并回到原版 5 格。客户端缺 Mod、服务端缺 Mod均应可连接。
- 保留 `lorian_arch_orbit` mod id、配置目录、翻译键和资源 ID；仅发行文件名使用连字符。
- 未知配置字段应保留；损坏文件不得被默认值覆盖；迁移写入前生成 `.bak`。

## 验证与交付

- 使用新模板要求的 JDK 执行 Wrapper 完整构建，确认单元测试、数据生成、Fabric/NeoForge 发行 JAR及元数据。
- 启动 Fabric/NeoForge 客户端和专服，检查日志中的 Mixin、资源、网络与类加载错误。
- 手工回归两层色轮、编辑/分享、智能选取、服务端授权距离、三类连接面以及屏障/光源显示；再覆盖 Sodium/Embeddium、资源包和光影。
- 核对产物名为 `lorian-arch-orbit-<loader>-<modVersion>+<minecraftVersion>.jar`，更新 README、配置文档、迁移说明和 `UPDATE_NOTES.md`。
- 报告每个实际命令与结果；未运行的兼容组合写成 NOT RUN，不能推断为通过。
