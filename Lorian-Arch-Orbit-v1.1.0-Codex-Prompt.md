# Lorian’s Arch Orbit v1.1.0 — Codex 实施任务

仓库：`MapManager-group/Lorian-Arch-Orbit`

请直接基于当前 `main` 分支代码实施 **v1.1.0**。先阅读现有架构和相关类，再以“最小改动、复用现有逻辑、不破坏双加载器构建”为原则完成下面两个功能。

参考对象：Radial `velolib/radial`，当前 MC 26.2 版本。Radial 只能作为 **Fabric 客户端选装依赖**，不得成为 Lorian 的硬依赖；NeoForge 不应引入任何 Radial 类。

---

## 目标 1：Lorian 色轮与 Radial 共用 R 键时智能仲裁

当前两者默认都使用 `R`。需要实现以下行为：

| 当前状态 | 按 R 的结果 |
|---|---|
| 空手 | Radial |
| 手持物品，但 Lorian 当前没有任何可用色板可处理它 | Radial |
| 手持物品，且 Lorian 主/副色板至少有一个能够有效处理它 | **Lorian 色轮优先**，Radial 本次按键不得打开 |
| Lorian 色轮功能关闭 / 非创造模式 | Radial |
| 两个 Mod 已被用户改成不同快捷键 | 完全不仲裁，各自正常工作 |
| 未安装 Radial | Lorian 行为与现在完全一致 |

### 实现要求

1. 不要简单按“是否空手”判断。必须判断 **Lorian 实际能否处理当前物品**，否则会造成普通手持物品时 R 变死键。
2. 主色轮和副色轮都要参与 eligibility 判断。尤其要保证“物品只存在于副色轮时”的双击 R 仍可正常进入 Lorian，而不会先被 Radial 打开。
3. 重构 `ClientPaletteRuntime` 中现有查找逻辑，提供一个**无副作用**的 eligibility/resolve 方法给兼容层复用；避免 `open()` 与兼容判断各复制一套 PaletteLookup 逻辑。
4. Fabric 侧新增独立 Radial compat 类。只在 `FabricLoader.isModLoaded("radial")` 时初始化。
5. Radial 当前在 `END_CLIENT_TICK` 检查 `OPEN_RADIAL.isDown()` 并使用公开的 `RadialClient.lockKey()` 阻止本次轮盘打开。Lorian 兼容层应在更早的 Fabric client tick（优先 `START_CLIENT_TICK`）完成仲裁：
   - Radial 已安装；
   - Lorian 与 Radial 当前实际绑定键相同；
   - Lorian 色轮键当前按下；
   - `ClientPaletteRuntime` 判断当前输入可被主/副色轮处理；
   - 满足时才锁住 Radial 本次按键周期。
6. 对 Radial 的内部实现依赖要隔离在 Fabric compat 包内。`lockKey()` / `OPEN_RADIAL` 不属于 Radial 正式 API，优先采用可失败、可降级的封装（反射并缓存也可以）；兼容初始化失败时只记录一次警告并禁用此项兼容，**不得影响 Lorian 启动**。
7. 不要用跨 Mod Mixin，除非现有公开入口完全无法实现；当前方案应无需 Mixin。
8. 不要改变两边默认快捷键。

建议结构（可根据现有包结构微调）：

```text
common/
  client/
    ClientPaletteRuntime.java
    CreativeInventoryHelper.java   # 目标 2 也会复用

fabric/
  client/
    compat/
      radial/
        RadialInputCompat.java
        ...
```

---

## 目标 2：为 Radial 增加 “Pick Item / 拿出物品” 槽位模式

Radial 安装时，由 Lorian 为它额外注册一个 SlotMode，例如：

```text
lorian_arch_orbit:pick_item
```

显示名称：

- `en_us`: `Pick Item`
- `zh_cn`: `拿出物品`

用途：创造模式下从 Radial 直接把配置好的物品放到玩家**当前选中的快捷栏槽位**。典型场景：快速拿出 WorldEdit 的 `minecraft:wooden_axe`。

### 核心限制

**绝对不要使用 `/give`、聊天命令或服务端命令权限。**

必须复用 Lorian 当前色轮已经使用的创造模式物品栏同步逻辑：

```java
player.getInventory().setSelectedItem(stack);
gameMode.handleCreativeModeItemAdd(stack, 36 + selectedSlot);
```

请把这段能力从 `ClientPaletteRuntime.replaceSelectedSlot()` 抽成一个通用 helper（如 `CreativeInventoryHelper`），让：

- Lorian 原有 Palette Wheel；
- Radial 的 Pick Item SlotMode；

共同调用同一实现，避免重复代码。

### Radial API 集成

Radial 当前支持第三方 entrypoint：

```java
RadialApiEntrypoint#registerSlotModes()
```

并通过：

```java
SlotModeRegistry.register(...)
```

注册自定义模式。

请使用 **Radial 正式提供的这个扩展机制**，不要 fork Radial，不要修改 Radial 源码，不要用 Mixin 注入 SlotModeRegistry。

实现一个 Fabric-only entrypoint，例如：

```text
LorianRadialEntrypoint implements RadialApiEntrypoint
```

在 `fabric.mod.json` 中增加 Radial 自定义 entrypoint。Radial 未安装时该类不得被正常 Lorian 启动路径加载。

### 物品配置与显示

第一版保持简单：

- `slot.itemId` 同时作为“要拿出的物品”和槽位图标来源；
- 尽量复用 Radial 的 `IconEnabledSlotMode` / 现有 icon editor UI；
- 支持普通物品 ID，例如 `minecraft:wooden_axe`；
- 支持 Radial 现有 Hand 按钮序列化出来的 component 数据；
- 执行动作时正确解析为 `ItemStack`；
- 不要因为 `RadialSlot#getRenderStack()` 在解析失败时返回 Barrier 而把“Invalid Item ID”屏障真的塞进玩家物品栏；请做可靠的解析失败判断或自行用同等 `ItemParser` 逻辑返回 `Optional<ItemStack>`。
- `AIR` / 空栈 / 无效 ID 不执行。
- 仅创造模式允许执行；生存/冒险模式不得发送 creative inventory 操作。
- 可以用客户端 actionbar/translatable message 简短提示“仅创造模式可用”或“无效物品”，不要刷聊天栏。

Radial 选择该槽位后的预期：

```text
R → Radial → Pick Item: minecraft:wooden_axe → 松开
→ 关闭 Radial
→ 当前快捷栏选中槽直接变成木斧
```

服务器即使禁止 `/give` 权限，也不影响该功能；但仍以服务器是否允许玩家正常创造模式物品栏操作为准。

---

## 依赖与元数据

1. 将项目版本从当前版本更新为：

```properties
mod_version = 1.1.0
```

2. Radial 仅作为 Fabric **compile-only / optional** 依赖：
   - 不打包进 Lorian jar；
   - 不设为 `depends`；
   - 在 Fabric metadata 中用 `suggests`（或项目当前约定的等价弱依赖）表达；
   - 只声明与当前 MC 26.2 Radial API 兼容的版本范围。
3. 如果需要增加 Radial Maven 仓库/坐标，请采用其公开可解析来源；不要提交本地 jar，不要把 Radial shadow 进产物。
4. NeoForge 模块不得新增 Radial 依赖或类引用。
5. 保持“只安装 Lorian”时 Fabric 与 NeoForge 均能独立构建和运行。

---

## 文档与更新说明

更新 `UPDATE_NOTES.md`（以及确有必要时 README），增加 v1.1.0：

- 新增 Radial 可选兼容；
- R 键冲突时按当前手持物品和 Lorian 色板可用性自动仲裁；
- Radial 新增 `Pick Item / 拿出物品` 槽位类型；
- 该功能通过创造物品栏同步拿物品，不依赖 `/give` 权限。

不要大规模改写现有文档。

---

## 验收标准

完成后至少执行项目现有测试与完整构建，优先：

```bash
./gradlew test
./gradlew build
```

并检查以下场景，能自动测试的尽量补测试，无法自动化的在最终总结中明确列为手工验证：

1. **未安装 Radial**：Fabric Lorian 正常启动，R 色轮行为无回归。
2. **NeoForge**：正常构建，无 Radial 类加载/依赖。
3. **Lorian + Radial，空手**：R 打开 Radial。
4. **Lorian + Radial，持普通非 Palette 物品**：R 打开 Radial。
5. **持 Primary Palette 可处理物品**：R 只打开 Lorian 色轮。
6. **物品仅 Secondary Palette 可处理**：双击 R 能正常进入 Lorian 副色轮，第一次按键不会先打开 Radial。
7. **用户把两个快捷键改成不同键**：兼容层不干预。
8. **Lorian 色轮关闭 / 非创造模式**：R 交给 Radial。
9. Radial 编辑器中能看到 `Pick Item / 拿出物品`。
10. 创造模式配置 `minecraft:wooden_axe` 后选择该槽位：木斧进入当前选中快捷栏槽。
11. Pick Item 不发送 `/give` 或任何聊天命令。
12. 生存模式、无效 Item ID、AIR 均安全拒绝，不生成错误物品。
13. 带 components 的有效物品配置可以正确恢复。
14. 最终 jar 中不应打包 Radial 本体。

---

## 实施原则

- 先阅读现有代码再修改，不要重写已经工作的输入、HUD、Palette 或配置架构。
- 尽量小范围改动，复用现有 `ClientInputCoordinator`、Palette 查找和创造模式换物逻辑。
- Radial 相关代码全部隔离到 Fabric optional compat 路径；common 只保留可复用、与 Radial 无关的能力。
- 不引入不必要的新依赖。
- 不改变现有配置文件格式，除非功能确实需要。
- 保持现有代码风格、命名、国际化与构建方式。
- 对 Radial 内部 API 的调用必须能优雅失效，不能因 Radial 小版本变化导致 Lorian 整体崩溃。

完成后请输出：

1. 修改文件清单；
2. 两个功能的实现摘要；
3. Radial optional dependency / entrypoint 的实现方式；
4. 测试与构建结果；
5. 尚需人工游戏内验证的项目；
6. 如发现当前设计与 Radial 3.1.0+26.2 实际 API 有差异，说明差异及你采用的最小兼容调整。
