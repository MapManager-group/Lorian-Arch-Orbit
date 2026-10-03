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
