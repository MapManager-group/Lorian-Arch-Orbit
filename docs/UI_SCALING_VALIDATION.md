# 配色界面动态尺寸验证

本轮仅修改 `26.2`。保留 Minecraft 26.2、JDK 25、Gradle 9.5.1、Loom no-remap、Fabric Loader 0.19.3 / API 0.157.0+26.2 和 NeoForge 26.2.0.52-beta 的现有配置。

## 自动验证

- `PaletteViewportTest`：参考画布、可读性下限、坐标往返、居中留白边界、GUI 档位无关的像素位置，以及原版鼠标坐标向上取整和实际渲染投影之间的修正。
- 窗口矩阵：854×480、1280×720、1920×1080、2560×1440、3840×2036、3840×2160、3440×1440、5120×1440、1024×768，并增加 480×270、320×180；几何测试覆盖 GUI 1、2、3、5 和自动档计算值。
- `PaletteEditorLayoutTest`、`PaletteDialogLayoutTest`、`HueGradientLayoutTest`：紧凑断点、最小画布可操作行、状态区和操作区不重叠、按钮换行。
- `PaletteRadialLayoutTest`：空列表、少量成员、34 项、容量边界、100/1000 项、图标间距、快捷栏避让；重复值保留位置语义，首尾循环仍能选中全部条目。
- 使用本机 JDK 25 运行 `./gradlew.bat build --offline`，通过仓库现有 `common:unitTest` 执行 JUnit；未新增依赖或测试框架。`common:test` 的跳过属于仓库已有 Windows 非 ASCII 路径处理，不是跳过单元测试。
- 最终结果：`BUILD SUCCESSFUL`，134 / 134 tests successful，0 skipped / 0 failed；Fabric 与 NeoForge 发布 jar 均已生成。`git diff --check`、中英文 JSON 解析、Mixin 注册唯一性检查通过。

## 已执行的客户端检查

- Fabric：独立开发客户端，854×480 小窗口的 GUI 自动档和 GUI 1 均能打开并点击编辑器页签；物品网格保持相同布局尺寸。最大化后恢复四栏；全屏切换后保留渐变结果。
- Fabric：游戏内物品浏览、成员拖拽排序、成员提示、分享名称输入（含中文字符）、缩小窗口后名称保持、渐变生成和方块选择分页、返回后保留草稿与生成结果。
- Fabric 最终代码复测：GUI 1 下预览滚轮切换橡木至云杉、Tab 焦点和 Enter 页签切换正常；从小窗口最大化后保留当前分组及预览选择，恢复四栏布局。
- NeoForge：启动到主菜单，经模组配置打开编辑器；紧凑页签、渐变和导入页面可打开。未进入世界时物品浏览保持空列表并显示提示，避免 26.2 的 `Components not bound yet` 崩溃。
- 两个平台已检查客户端日志，没有新增 Mixin 注入失败。Fabric 开发账户的 Realms / 认证请求存在 401，与本次 UI 无关。

## 未完成的人工矩阵与环境限制

- 没有逐一实机切换全部分辨率与 GUI 组合，也没有进行截图逐像素差分；完整矩阵目前是几何单元测试覆盖。
- HUD 持续按键、一级/二级/临时轮动画、连续滚动和极端 GUI 档位快捷栏避让尚未逐项实机复核；容量、顺序、循环和底部选择位置已由测试覆盖。
- 中文字符输入已检查，真实输入法预编辑、候选窗口、全部键盘导航路径尚未完整人工验证。
- NeoForge 未重复 Fabric 的完整游戏内操作矩阵；导入选择的缩放保持已检查实现路径，尚未用多个实际导入源做人工回归。
- NeoForge 开发客户端关闭窗口后出现 `ClientShutdownWatchdog` / `Module minecraft has been closed`，残留测试进程锁住 common 开发 jar，曾造成构建失败。确认窗口已关闭并结束该测试进程后，重新构建成功。未改动加载器版本或关闭校验来掩盖此问题。
- Fabric 最终 `:fabric:runClient --offline` 已完成上述游戏内检查，但窗口关闭、世界保存完毕后也触发 `Client shutdown from post-main` 看门狗，任务以 -8 退出。因此客户端交互检查通过不代表 runClient 进程退出通过。仓库开发目录内 2026-08-11、2026-08-13 的历史报告已有同类退出异常；本轮报告中残留固定线程池处于等待状态，没有配色页面调用栈。没有在本次尺寸适配中修改开发加载器或线程生命周期。

## 自检

`Code Review: PASS WITH RISKS`。审查新增文件和完整差异，未发现本轮改动的剩余 BLOCKER / MAJOR；配置 schema、色轮文件格式、快捷键与保存语义不变。修正了复核中发现的非配色 HUD 动画分支、窄栏长文本、重复 Mixin 注册和鼠标取整问题。剩余风险为上述未执行的人工矩阵及开发客户端退出异常。

`UPDATE_NOTES.md` 已按 Gitmoji + Conventional Commits 格式追加尺寸适配与主菜单安全浏览说明；没有提交或推送 Git。

## 后续：上下安全区与目标方向

- `PaletteRadialLayout` 扩大上下 HUD 留白；已用本机 26.2 `Hud.extractSelectedItemName` 核实物品名称为窗口高度减 59（创造模式减 45）。保留顶部常规多行 HUD 空间，不引入 Jade 依赖。
- `PaletteTargetPosition` 与配置 codec/draft/snapshot、配置页面、中英文翻译接通 `features.palette_wheel.target_position`。默认 bottom，支持 top/left/right；新增可选字段，不提升配置版本，不更改色轮成员格式。旧文件、无效值回退、枚举往返、恢复默认及配置变更通知均有测试。
- `PaletteTargetIndicator`、`RadialWheelVisuals` 在配色 HUD 与编辑器预览绘制绿色像素箭头，随选中槽位移动并轻微往复；原版/智能选取等其他轮盘不受影响。
- 最终 `./gradlew.bat build --offline`：**PASS，138/138 测试成功**，两平台发布 jar 已生成；`git diff --check` 与新增中英文翻译检查通过。本轮没有新增或修改 Mixin。
- Fabric 独立开发客户端：实际检查预览的下、上、左、右四个方向；底部默认箭头、小窗口与最大化显示正常；右侧模式滚动后目标由橡木切换为云杉并停在右侧。通过开发目录配置热重载切换方向，测试后恢复原文件，SHA-256 与备份一致。
- 检测到用户操作桌面后停止界面自动化，未继续检查配置控件点击/保存，也未主动关闭测试客户端。本轮未执行 NeoForge 游戏内回归或带 Jade 的 HUD 持续按键实测，相关方向和安全区由共享几何测试覆盖；Jade 的自定义位置、过高面板不在通用留白的保证范围内。
- 按 `CODE_REVIEW.md` 自检：**PASS WITH RISKS**，无本轮 BLOCKER / MAJOR；剩余风险为上述实机未覆盖项。`UPDATE_NOTES.md` 已追加安全区修复和目标箭头/方向配置两条记录；未提交或推送 Git。

## 后续：箭头动效解耦与三角形外观

- `PaletteTargetIndicator` 改为由目标方向、半径和当前时间计算位置，不再读取旋转中的选中槽位坐标。保留箭头自身 900 毫秒周期、3 逻辑单位行程的径向往复，滚动不改变朝向或重置相位；箭尖与目标图标中心保持至少 16 逻辑单位距离。
- `RadialWheelVisuals` 使用无尾杆、低饱和灰绿色三角形和深色像素描边。HUD 与编辑器预览均在槽位循环之外绘制一次箭头，四个配置方向共用实现。没有新增配置字段或修改 Mixin、依赖。
- `PaletteTargetIndicatorTest` 覆盖四方向、极小半径隐藏、正反向连续滚动与重复物品，以及往复运动的轴线、范围和完整周期。使用 JDK 25 执行 `.\gradlew.bat build --offline`：**PASS，140/140 测试成功，0 skipped / 0 failed**；Fabric 与 NeoForge 发布 jar 均已生成。`git diff --check`、中英文 JSON 解析通过。
- 本次没有重新操作游戏客户端，未进行最终三角形外观和连续滚动的实机视觉复测；此前检测到用户使用桌面后已停止界面自动化，不将上一轮客户端检查记作本次复测。
- 按 `CODE_REVIEW.md` 自检：**PASS WITH RISKS**，无 BLOCKER / MAJOR / MINOR；剩余风险为上述未执行的视觉复测。`UPDATE_NOTES.md` 已追加箭头解耦和外观修复记录；没有提交或推送 Git。

## 后续：深绿色箭标重绘

- 本次仅调整 `RadialWheelVisuals` 的像素外观，以及中英文说明、`CONFIGURATION.md`、`DESIGN.md` 和验证/更新记录；保留此前未提交的功能改动。采用 11×8 像素内凹箭头，森林深绿主体、深色描边、克制的亮边和阴影。每个像素仅绘制一次，避免重叠半透明描边使轮廓粗细不均。
- 从最终代码的图案和颜色生成离线预览，检查上下左右四方向在浅色、深色背景及 3×、7× 像素倍率下的轮廓。箭尖仍锚定既有目标位置，往复动效及滚动解耦逻辑未改动。
- JDK 25 执行 `.\gradlew.bat build --offline`：**PASS，140/140 测试成功，0 skipped / 0 failed**；Fabric / NeoForge 发布 jar 已生成。中英文 JSON 解析与 `git diff --check` 通过。
- 按 `CODE_REVIEW.md` 自检：**PASS WITH RISKS**，无 BLOCKER / MAJOR / MINOR；未新增依赖或修改 Mixin、配置格式。本轮使用离线预览检查外观，没有接管用户桌面进行游戏内复测；实际场景中的视觉效果仍待实机确认。`UPDATE_NOTES.md` 已追加深绿色箭标重绘记录；未提交或推送 Git。

## 后续：Pointer / Shift 样式切换

- 按两张参考图分别绘制手形指针和短尖头宽底座的 Shift 箭头，保留深绿色、像素描边和独立往复动效。`RadialWheelVisuals` 为两种图案分别指定指尖/箭尖锚点，避免手形偏心导致四方向指向错位；HUD 与编辑器预览读取同一配置。
- 新增 `PaletteArrowStyle` 和 `features.palette_wheel.arrow_style` 可选字段；配置页面“行为 → 指示箭头样式”提供 Pointer / Shift，默认 Shift。同步 codec、snapshot、draft、恢复默认和变更通知、中英文说明；旧配置缺失或非法值回退 Shift，schema 仍为 3，未知字段和目标方向保留。
- 新增 `PaletteArrowStyleConfigTest`：默认值、旧文件、非法值、未知字段保留、两个样式在四方向下的序列化往返、通知范围及恢复默认。使用 JDK 25 执行 `.\gradlew.bat build --offline`：**PASS，142/142 测试成功，0 skipped / 0 failed**，Fabric / NeoForge 发布 jar 已生成。中英文 JSON 解析和 `git diff --check` 通过。
- 从实际代码图案和颜色生成离线预览，检查两种样式在四个方向、深浅背景下的外观。未接管用户桌面进行游戏内配置点击/保存或最终视觉复测；不将离线预览记为客户端检查。
- 按 `CODE_REVIEW.md` 自检：**PASS WITH RISKS**，无 BLOCKER / MAJOR / MINOR；剩余风险为上述实机未验证项。只修改 `26.2`，保留前序未提交改动；未修改依赖、Mixin 或色轮文件格式，未提交或推送 Git。`UPDATE_NOTES.md` 已追加两种指示样式及默认值说明。

## 后续：Vanilla / Arrow / Pointer / None（2026-10-04）

- 从目标 26.2 客户端 JAR 读取 `textures/map/decorations/frame.png`，核对 8×8 像素形状、黑色描边及 `#00BC38`／`#00E043`／`#00FF4C` 色阶。Vanilla 使用对应图案并成为新默认值；Arrow、Pointer 共用同一色阶，Pointer 削减指尖两角及一个关节边角像素。中英文配置均直接显示 Vanilla、Arrow、Pointer、None。
- None 不提交箭头绘制，不改变方向、物品强调或文本。旧 shift 字符串（含大小写）迁移为 arrow，保留原样式选择；缺失／无效配置回退 vanilla，已有 pointer／none 读写保持。配置 schema、色轮文件格式及依赖版本未改动。
- HUD 与色轮编辑器预览共用按可见位置计算的余弦放大，最大为原尺寸的 1.1 倍；滚动重定向与首尾循环保持连续，重复物品按索引独立处理。配色色轮安全边界按半尺寸 11 计算，非配色 HUD 保留半尺寸 10；箭标独立 900 毫秒往复时钟未改变。
- 一级、二级和临时 HUD 色轮共用两行信息：物品名称；当前位置／总数与滚轮提示。临时名称不再拼接模式前缀及数量，即使所有物品均在环内也保留第二行。渐变配色器实现保持冻结。
- JDK 25.0.3 在 `26.2` 执行 `.\gradlew.bat build --offline`：**PASS，188/188 JUnit 成功，零失败／跳过**。样式／方向配置往返、旧字段迁移、原版色阶、四方向箭尖、None 空图案、放大连续性和安全距离检查通过；既有箭标独立动画测试通过。Fabric 与 NeoForge 安装包包含新增类及 None 翻译。
- `git diff --check`、翻译 JSON／重复键／键集合和四个不汉化名称检查：PASS。按 `CODE_REVIEW.md` 自检：**PASS WITH RISKS**，无未解决 BLOCKER／MAJOR；保留此前未提交改动，仅修改 26.2，未提交或推送。
- 遵循用户要求，未启动客户端或使用 computer-use。待用户手测：四种样式切换／保存、旧配置升级后选择 Vanilla、四方向指向、快速正反滚动及放大观感、临时色轮两行文本、不同 GUI 倍率及全屏／窗口缩放。自动测试与资源核对不代替实机视觉验收。

## 后续：Vanilla 放大与工作区提交审查（2026-10-04）

- Vanilla 保留 8×8 源图案，每个源像素绘制为 2×2 逻辑像素；以原箭尖锚点沿四方向展开，颜色、轮廓与独立动效不变。Arrow／Pointer 仍为 1 倍。更长的内侧轮廓在半径不足 55 时隐藏，避免展开／收拢时压住名称。
- 审查当前全部未提交源码、资源、测试、脚本和文档，按面映射、渐变编辑工作流、色轮指示三个功能批次整理。构建／运行／测试数据目录已在既有忽略规则内，没有发现需要删除的无关未跟踪文件。保留渐变冻结状态，只审查和提交已有实现。
- JDK 25 下 `.\gradlew.bat build --offline`：PASS，188 项 JUnit 全部成功，Fabric／NeoForge 构建成功。`python -B -m unittest discover -s scripts -p test_extract_gradient_faces.py`：PASS，3 项；从目标客户端 JAR 内存重新提取，面映射与待提交资源逐字节一致。
- Code Review: PASS WITH RISKS，无未解决 BLOCKER／MAJOR；客户端仍按用户要求留待手动验证，未操作游戏界面。等比例放大的最终观感及全屏／窗口切换未作实机复测。
