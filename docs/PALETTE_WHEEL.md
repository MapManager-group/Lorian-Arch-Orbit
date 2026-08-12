# 双层物品色轮

## 使用方式

- 仅在创造模式世界中替换物品，默认按键为 `R`，可在按键设置中改绑。
- 按住一次 `R` 超过 180 ms 打开第一层；快速松开后在 250 ms 内再次按住 `R` 打开第二层。
- 松开按键会释放滚轮并播放收拢动画。轮盘实际打开时，滚轮循环选择并立即替换当前快捷栏槽；未匹配物品、空组、单项组不会占用滚轮。
- 编辑器默认按键为 `P`，也可从 YACL 的“行为 → 色轮编辑器”进入。

## 编辑器

- 左侧选择轮组，中间通过搜索、命名空间分类和分页浏览物品，右侧编辑成员。
- 左键物品添加注册 ID 规则；`Shift + 左键`把该物品设为轮组图标。
- “添加手持物品（完整组件）”会保存当前手持物品的 `DataComponentPatch`，适合自定义名称、损伤、方块状态和附魔等变体。
- 右键成员移除；拖动成员到另一行排序。支持新建、复制、删除、恢复空默认值，以及逐步撤销当前层尚未保存的修改。
- “取消”不写文件；“保存”分别通过临时文件和原子替换写入两层，并立即更新内存快照。外部修改后仍可使用 `/lorian_arch_orbit reload`。
- 24 是显示建议上限，不是数据硬限制。超过 24 项会显示警告，成员不会被截断或删除。

## JSON schema

第一层与第二层分别位于 `primary_wheel.json`、`secondary_wheel.json`：

```json
{
  "config_version": 1,
  "groups": [
    {
      "id": "stone_variants",
      "display_name": "Stone variants",
      "icon": "minecraft:stone",
      "members": [
        {
          "item": "minecraft:stone",
          "match": "item"
        },
        {
          "item": "minecraft:diamond_pickaxe",
          "match": "exact_components",
          "components": {
            "minecraft:damage": 7
          }
        }
      ]
    }
  ]
}
```

`item` 按注册 ID 匹配；`exact_components` 同时要求完整组件补丁相等。查找时完整组件规则优先于注册 ID 规则，同一优先级按文件中的轮组和成员顺序选择第一个匹配。显示候选按完整物品及组件去重，但不会重写用户配置。
