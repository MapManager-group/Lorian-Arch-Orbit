# 26.3 迁移记录

日期：2026-10-18。源基线为 `26.2/` 的 `c150a19`，目标为用户提供的 `26.3/` Architectury Template。

Migration Result: SUCCESS

Code Review: PASS WITH RISKS

本文记录版本迁移完成时的状态；后续配色已改为本地资源计算，见 [本地资源配色实现与验证](LOCAL_RESOURCE_COLORS.md)。

## 版本与工程

| 项目 | 最终版本 |
|---|---|
| Minecraft / Java | 26.3 / 25（本机 JDK 25.0.3） |
| Gradle Wrapper | 9.5.1，保留新模板版本 |
| Architectury Loom / Plugin | 1.17.493 / 3.5.170，固定为基线实际解析版本 |
| 映射 | `dev.architectury.loom-no-remap`，无混淆官方名称，无 Yarn mappings |
| Fabric Loader / API | 0.20.0 / 0.163.0+26.3 |
| Architectury API / NeoForge | 22.0.3 / 26.3.0.41-beta |
| YACL / Mod Menu / Radial | 3.9.7+26.3（双平台）/ 21.0.0 / 3.2.2+26.3 |

`26.3/` 已建立独立 Git 仓库，以无父历史的 `26.3` 孤儿分支承载迁移结果；origin 与 26.2 相同。26.2 工作区保持干净，具体提交与推送状态以 Git 记录为准。

保留 `lorian_arch_orbit` Mod ID、`com.davidblackcn.lorianarchorbit` 主包、`com.davidblackcn.mixin` Mixin 包、作者、MIT 许可证、图标、描述及 Mod 版本 `1.1.0`。发行文件仍采用 `lorian-arch-orbit-<loader>-1.1.0+26.3.jar`。

配置代码、距离协议代码、分享代码和默认色轮预设均与 26.2 逐文件比较一致：配置 schema 为 3，网络协议为 1，分享格式为 1。CI 保留原有任务、权限、触发条件和产物筛选，仅更新版本标签。未扩展或重排默认预设；全部已有成员通过 26.3 注册表校验。

## 必要适配

- `ChestRendererMixin`：按 26.3 原版渲染路径，将箱子连接补面与破坏覆盖层分别提交，保留原有动画及覆盖层。
- `ClientLevelBreakingEffectMixin`：目标改为已核实描述符的 `addBreakingParticles`，避免取消新版外层方法时连同原版破坏声音一起抑制。
- `PickItemSlotMode`：匹配 Radial 3.2 的 `Screen` 编辑器参数。已核对 `OPEN_RADIAL`、`lockKey` 和图标编辑接口。
- 逐项核对 9 个客户端 Mixin 的目标签名；OBE 1.1.53 的目标方法、调用点及 `List` 局部变量匹配现有兼容注入。没有 Access Widener。
- 可选客户端开发依赖与主运行时一致解析，解决不同配置引入两个 Fabric Loader 版本的问题；服务端不引入 YACL 或 Mod Menu。
- 从 26.3 原版 JAR 重新提取 `block_faces.json`：974 个方块模型、8957 条状态/面记录。新增 85 个方块映射，已有映射未移除或改变。
- README、迁移指南、HueBlocks 文档和平台元数据更新到目标版本；历史开发记录、协议和配置说明保留。

## 经用户批准的 HueBlocks 回退

上游索引当前没有 Minecraft 26.3 数据。26.3 优先选择同版本数据，仅在索引缺少该版本时使用 26.2；原生数据下载失败时不悄悄改用其他版本。

缓存仍位于 `config/lorian_arch_orbit/hueblocks/26.3.json`，增加可选的 `source_version` 字段用于记录来源，原有缓存可读取。不同来源的数据不共享 ETag；完整下载和验证成功后才原子替换缓存。26.2 的缓存文件不会被改写。界面显示回退数据来源。Mod 配置、网络协议、色轮分享格式均未改变。

实际使用 Java 下载器连续刷新两次，均成功解析 26.2 的 452 种纹理和 5 个调色板。新增方块是否可参与渐变，仍取决于上游是否提供其纹理颜色。

## 验证

以下命令均在 `26.3/` 执行。运行时验证以启动及日志为边界，不代表已完成全部交互和视觉回归。

| 命令或检查 | 结果 |
|---|---|
| 未修改模板的 `./gradlew.bat build`（JDK 25） | PASS |
| 最终 `./gradlew.bat -Dorg.gradle.java.home="C:/Program Files/Java/jdk-25.0.3" clean build --console=plain` | PASS；205 项测试成功，0 失败，0 跳过；两个发行 JAR 生成 |
| `python -m unittest discover -s scripts -p "test_*.py"` | PASS；3 项测试 |
| `:fabric:runClient`、`:neoforge:runClient` | 启动及资源加载 PASS；无本 Mod 注入失败 |
| `:fabric:runServer --args=nogui`、`:neoforge:runServer --args=nogui` | 本地测试世界启动 PASS；网络接收器注册；发送 `stop` 后保存世界 |
| Fabric 客户端同时加载 OBE 1.1.53、Radial 3.2.2 | 启动及资源加载 PASS WITH RISKS；见下方第三方警告 |
| HueBlocks 回退、离线缓存、旧缓存读取、原生数据优先、来源切换及部分更新失败 | 自动测试 PASS |
| 真实上游连续刷新 | PASS；452 种纹理、5 个调色板 |
| 发行包元数据、入口、图标、9 个 Mixin、无模板类 | PASS |
| 26.2 工作区、目标分支、配置/协议/分享一致性、JSON 和翻译键 | PASS |

开发运行任务在完成启动检查后主动结束；专服先发送 `stop`，其后结束仍等待的 Gradle 运行进程。启动结果不以被终止的 Gradle 任务退出码判定。日志保留于目标目录的 `migration-*.log`，由既有 `.gitignore` 排除。

早期验证遇到沙箱临时目录访问失败及一次 JDK 21 环境误选，均已在正常用户环境、明确指定 JDK 25 后通过重跑。首次专服测试的超平坦夹具缺少 `layers`，修正测试配置后的复验已成功。未修改测试来绕过这些环境问题。

## 自检与剩余风险

- BLOCKER：无。MAJOR：无。更新记录已按 Gitmoji + Conventional Commits 格式追加。
- MINOR：NeoForge 提示沿用的 `logoFile` 元数据已弃用，但目前仍有效；YACL 也有同类提示。
- OBE 组合存在重复图集精灵及未安装可选兼容 Mod 的警告；Radial 存在 `font/LICENSE-phosphor` 非法资源路径日志。未改写第三方包，启动和图集加载仍完成。
- 未手动验证入世界后的按键竞争、各 GUI 缩放、箱子破坏覆盖层和连接面视觉效果，也未验证跨进程距离请求的完整交互、Sodium/Embeddium、光影及第三方资源包矩阵。
- 新增方块的 HueBlocks 配色覆盖受上游数据限制；同版本数据可用后自动优先使用。
