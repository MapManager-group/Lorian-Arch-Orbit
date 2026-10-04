# Color Wheel Guide

[Back to README](../README_en.md) · [Gradient Guide](GRADIENT_GUIDE_en.md) · [中文](PALETTE_GUIDE.md)

Put blocks you often use together in one group, then switch between them with the scroll wheel while building instead of reopening your inventory.

## Try It First

1. Enter **Creative mode** and hold a block included in a wheel group, such as an oak log
2. Hold `R` to open the primary wheel, then scroll to switch items
3. Release `R` when you reach the block you want, and keep building

Scrolling immediately replaces the item in the selected hotbar slot. No extra click is needed. All keys below are defaults and can be changed in Minecraft's Controls settings.

![Color wheel](image/物品色轮示意.png)

## Primary, Secondary, and Temporary Wheels

| Wheel | How to open it | Use |
| --- | --- | --- |
| Primary | Hold `R` | Your usual group of blocks |
| Secondary | Quickly press `R` twice, holding the second press | Another grouping for the same held block |
| Temporary | Quickly press `R` three times, holding the third press | The complete result from the gradient editor |

- Primary and secondary wheels find a group using the **currently held item**; the group needs at least two valid, distinct members
- Each layer can use a **block type, block family, or color** preset, or your own groups from the editor
- Generate and apply a temporary wheel in the gradient editor first. It can then open even with an empty hand, preserves repeated positions, and clears when the game restarts

If the wheel cannot fit every member, a “position / total” counter appears. Keep scrolling to access the rest; a smaller window does not remove any members.

## Edit Your Own Wheel

Press `P` to open the editor home, then choose **Color Wheel Editor**.

At normal window sizes, the columns are **groups, item browser, wheel preview, and members**, from left to right. Small windows use matching tabs with the same functions.

![Color wheel editor](image/色轮编辑器示例.png)

### Create a Group

1. Choose **Primary** or **Secondary** at the top left
2. Click **New** above the group list and enter a group name on the right
3. Click items in the browser to add them; use categories or search to find what you need
4. Reorder the members on the right and check the wheel preview in the middle
5. Click **Save** at the bottom right; the editor stays open after a successful save

**Drag to reorder** members and **right-click to remove** them. To preserve a held item's custom name and other components, use **Add held item (full components)**.

### Manage Groups

- Use the **Actions** menu above the group list to copy or delete the current group
- **Undo** at the bottom left reverses draft edits
- **File → Restore default groups** affects every group in both layers and asks for confirmation

Changes remain in the draft until you click **Save**. Restoring defaults also needs a save before it takes effect.

## Share and Import

- Open **File → Share**, select the groups, enter a name, then copy a share code or export JSON
- Copy a share code to the clipboard or place a JSON file in the share folder, then open **File → Import**. Select a bundle and check its group count and missing-member information
- Choose a conflict policy: **Keep both**, **Replace existing**, or **Skip import**, then confirm the import
- Imported groups enter the draft first; review them and click **Save**

Share files live in `config/lorian_arch_orbit/palette-shares/` inside the game directory. These contain wheel groups; gradient stops and filters are exported separately from the gradient editor.

## Adjust the Display

Press `O` to open the configuration screen and adjust wheel preferences:

- **Target item position**: bottom, top, left, or right; bottom by default
- **Arrow style**: `Vanilla`, `Arrow`, `Pointer`, or `None`; defaults to `Vanilla`, while `None` hides the arrow
- Wheels and editor screens adapt to window size and GUI scale. Crowded wheels show fewer members at once, with all members still accessible by scrolling

The indicator points to the target item, and the selected block enlarges slightly. The indicator's own animation is independent of wheel rotation.

## Back, Switch Pages, and Exit

- The page menu at the top right switches to the **Gradient Editor**; **Back** at the bottom right returns to the editor home
- Switching pages or returning home retains the draft, search, and editing state without saving automatically
- Press `Esc` in either editor to exit directly. An open menu closes first; unsaved or unapplied changes trigger a confirmation

Unsaved drafts are not retained after leaving the editor entirely. The temporary wheel and gradient inventory backup have separate lifetimes and are not cleared just by closing the editor.

## If the Wheel Does Not Open

- Check that you are in Creative mode and have enabled the mod and the relevant wheel feature
- Primary and secondary wheels require a held item that matches a group with at least two valid, distinct members; a temporary wheel needs an applied gradient result first
- Double- and triple-taps must be quick, with the final press held down
- Check for key conflicts. On Fabric, when sharing a key with Radial, this mod takes priority if its wheel can open

See [Configuration and Troubleshooting](../CONFIGURATION.md) (Chinese) for more details.
