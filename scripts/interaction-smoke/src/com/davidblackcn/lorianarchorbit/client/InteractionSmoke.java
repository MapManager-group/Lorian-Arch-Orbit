package com.davidblackcn.lorianarchorbit.client;

import com.davidblackcn.lorianarchorbit.fabric.client.compat.radial.PickItemSlotMode;
import com.davidblackcn.lorianarchorbit.fabric.client.compat.radial.RadialInputCompat;
import com.davidblackcn.lorianarchorbit.palette.PaletteGroup;
import com.mojang.blaze3d.platform.InputConstants;
import dev.velolib.radial.api.*;
import dev.velolib.radial.util.EncoderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import java.lang.reflect.*;
import java.util.*;

/** Opt-in real-client regression fixture; never included in release artifacts. */
public final class InteractionSmoke implements net.fabricmc.api.ClientModInitializer {
    private boolean ran;
    private int ticks;
    public void onInitializeClient() {
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (ran) return;
            if (++ticks > 2400) { ran = true; System.out.println("INTERACTION_SMOKE FAIL: world timeout"); client.stop(); return; }
            if (ticks == 100 && client.level == null) {
                System.out.println("INTERACTION_OPEN_WORLD from " + client.gui.screen());
                client.createWorldOpenFlows().openWorld("lorian-interaction-smoke", () -> client.stop());
                return;
            }
            if (ticks % 200 == 0) System.out.println("INTERACTION_WAIT " + client.gui.screen());
            if (client.player == null || client.level == null || client.gui.screen() != null || ticks < 120) return;
            if (!client.player.isCreative()) {
                var server = client.getSingleplayerServer();
                server.execute(() -> server.getPlayerList().getPlayers().forEach(player -> player.setGameMode(GameType.CREATIVE)));
                return;
            }
            ran = true;
            try { test(client); System.out.println("INTERACTION_SMOKE PASS"); }
            catch (Throwable ex) { System.out.println("INTERACTION_SMOKE FAIL"); ex.printStackTrace(); }
            finally { CreativeInventoryHelper.clearSession(); client.stop(); }
        });
    }
    private static void test(Minecraft c) throws Exception {
        require(c.player.isCreative(), "creative test world");
        var original = c.player.getMainHandItem().copy();
        var wheel = ClientPaletteRuntime.wheelKey();
        try {
            c.player.getInventory().setItem(c.player.getInventory().getSelectedSlot(), ItemStack.EMPTY);
            CreativeInventoryHelper.setTemporaryPalette(List.of("minecraft:stone", "minecraft:dirt"));
            require(!ClientPaletteRuntime.canHandleCurrentInput(c), "empty hand with temporary palette must yield");
            Class<?> radial = Class.forName("dev.velolib.radial.RadialClient");
            var locked = radial.getDeclaredField("keyLocked"); locked.setAccessible(true); locked.setBoolean(null, false);
            wheel.setDown(true);
            call(RadialInputCompat.class, null, "onStartClientTick", new Class<?>[]{Minecraft.class}, c);
            require(!locked.getBoolean(null), "empty hand must not lock Radial");
            wheel.setDown(false);
            require(ClientPaletteRuntime.temporaryWheelKey().isUnbound(), "temporary shortcut defaults unbound");
            var temporaryKey = ClientPaletteRuntime.temporaryWheelKey();
            temporaryKey.setKey(InputConstants.Type.KEYBOARD.getOrCreate(InputConstants.KEY_G));
            try {
                long now = ClientInteractionRuntime.nowMillis();
                ClientInteractionRuntime.inputs().tick(now - 1, true, c.level);
                temporaryKey.setDown(true);
                ClientInteractionRuntime.inputs().tick(now, true, c.level);
                var lease = ClientPaletteRuntime.class.getDeclaredField("lease"); lease.setAccessible(true);
                require(lease.get(null) != null, "dedicated temporary shortcut opens with empty hand");
                temporaryKey.setDown(false);
                ClientInteractionRuntime.inputs().tick(now + 50, true, c.level);
                require(lease.get(null) == null, "temporary shortcut release closes wheel");
            } finally {
                temporaryKey.setDown(false); temporaryKey.setKey(InputConstants.UNKNOWN); ClientPaletteRuntime.resetGesture();
            }
            var mode = new PickItemSlotMode();
            var slot = new RadialSlot("test", mode, "", "minecraft:stone");
            var context = new SlotActionContext() {
                public void closeScreen() { c.setScreenAndShow(null); }
                public void openSubmenu(List<RadialSlot> slots, int count) { }
                public boolean isRoot() { return true; }
            };
            mode.performAction(slot, context);
            require(c.player.getMainHandItem().is(Items.STONE), "legacy icon target");
            slot.mode = SlotModeRegistry.getDefaultMode();
            var parent = createSlotEditor(slot);
            c.setScreenAndShow(parent);
            var dropdown = (dev.velolib.radial.ui.widget.DropdownButtonWidget<?>) parent.children().stream()
                    .filter(w -> w instanceof dev.velolib.radial.ui.widget.DropdownButtonWidget).findFirst().orElseThrow();
            parent.mouseClicked(new MouseButtonEvent(dropdown.getX()+2, dropdown.getY()+2,
                    new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0)), false);
            var menu = dropdown.getActiveMenu();
            require(menu != null, "mode dropdown opens");
            var optionsField = menu.getClass().getDeclaredField("options"); optionsField.setAccessible(true);
            var options = (List<?>) optionsField.get(menu);
            int index = -1;
            for (int i=0; i<options.size(); i++) if (options.get(i) instanceof PickItemSlotMode) index=i;
            require(index >= 0, "pick-item mode registered in dropdown");
            var heightField = menu.getClass().getDeclaredField("itemHeight"); heightField.setAccessible(true);
            int rowHeight = heightField.getInt(menu);
            menu.mouseScrolled(menu.getX()+2, menu.getY()+2, 0, -100);
            var scrollField = menu.getClass().getDeclaredField("scrollAmount"); scrollField.setAccessible(true);
            double rowY = menu.getY()+index*rowHeight-scrollField.getDouble(menu)+rowHeight/2.0;
            parent.mouseClicked(new MouseButtonEvent(menu.getX()+2, rowY,
                    new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0)), false);
            require(slot.mode instanceof PickItemSlotMode, "mouse selection switches to pick-item mode");
            require(parent.children().size() > 5, "real Radial editor registers widgets");
            var select = (net.minecraft.client.gui.components.Button) parent.children().stream()
                    .filter(w -> w instanceof net.minecraft.client.gui.components.Button button
                            && button.getMessage().equals(Component.translatable("radial.mode.lorian_arch_orbit.select_target")))
                    .findFirst().orElseThrow();
            parent.mouseClicked(new MouseButtonEvent(select.getX()+2, select.getY()+2,
                    new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0)), false);
            require(c.gui.screen() instanceof dev.velolib.radial.ui.screen.iconpicker.IconPickerScreen, "target picker opens");
            c.gui.screen().onClose();
            require(c.gui.screen() == parent && parent.children().size() > 5, "picker returns to populated editor");
            var layout = LinearLayout.vertical();
            mode.buildEditorWidgets(parent, slot, 300, layout);
            var edits = new ArrayList<EditBox>();
            layout.visitWidgets(widget -> { if (widget instanceof EditBox box) edits.add(box); });
            require(edits.size() >= 2, "target and independent icon editors");
            require(slot.value.equals("minecraft:stone"), "editing freezes legacy target independently of icon");
            slot.itemId = "minecraft:dirt";
            mode.performAction(slot, context);
            require(c.player.getMainHandItem().is(Items.STONE), "icon-only change preserves legacy target");
            slot.itemId = "minecraft:stone";
            var named = new ItemStack(Items.DIRT);
            named.set(DataComponents.CUSTOM_NAME, Component.literal("Smoke target"));
            edits.getFirst().setValue(EncoderUtils.toGiveCommandString(named, c.level.registryAccess()));
            mode.performAction(slot, context);
            require(ItemStack.isSameItemSameComponents(named, c.player.getMainHandItem()), "explicit target with components");
            require(slot.itemId.equals("minecraft:stone"), "target editing preserves icon");
            edits.getFirst().setValue("invalid:missing");
            mode.performAction(slot, context);
            require(ItemStack.isSameItemSameComponents(named, c.player.getMainHandItem()), "invalid explicit target must not fall back");
            var sessionConstructor = EditorSession.class.getDeclaredConstructor(net.minecraft.client.gui.screens.Screen.class);
            sessionConstructor.setAccessible(true);
            var session = sessionConstructor.newInstance((Object)null);
            var screen = session.wheel(); c.setScreenAndShow(screen);
            var bounds = (PaletteEditorLayout)call(PaletteEditorScreen.class, screen, "layout", new Class<?>[]{});
            require(!bounds.compact(), "wide editor fixture");
            click(screen, bounds.groupLeft()+6, bounds.groupTop()+6, InputConstants.MOUSE_BUTTON_LEFT);
            require(screen.selectedRef() != null, "left group selection");
            var before = (PaletteGroup)call(PaletteEditorScreen.class, screen, "selected", new Class<?>[]{});
            int count = before.members().size();
            click(screen, bounds.memberLeft()+6, bounds.memberTop()+6, InputConstants.MOUSE_BUTTON_LEFT);
            require(selected(screen).members().size()==count, "left click must not delete");
            call(PaletteEditorScreen.class, screen, "updateDragFeedback", new Class<?>[]{PaletteEditorLayout.class,int.class,int.class}, bounds,bounds.memberLeft()+6,bounds.memberTop()+24);
            screen.mouseReleased(event(screen,bounds.memberLeft()+6,bounds.memberTop()+24,InputConstants.MOUSE_BUTTON_LEFT));
            require(selected(screen).members().get(1).equals(before.members().getFirst()), "drag reorder");
            click(screen,bounds.memberLeft()+6,bounds.memberTop()+6,InputConstants.MOUSE_BUTTON_RIGHT);
            require(selected(screen).members().size()==count-1,"right click deletes exactly one member");
            click(screen,bounds.memberLeft()+6,bounds.memberTop()+6,InputConstants.MOUSE_BUTTON_MIDDLE);
            require(selected(screen).members().size()==count-1,"middle click does not delete");
            c.setScreenAndShow(null); // Discard the in-memory draft; do not save user configuration.
        } finally {
            wheel.setDown(false);
            c.player.getInventory().setItem(c.player.getInventory().getSelectedSlot(),original);
        }
    }
    private static dev.velolib.radial.ui.screen.SlotEditorScreen createSlotEditor(RadialSlot slot) throws Exception {
        var type = dev.velolib.radial.ui.screen.SlotEditorScreen.class;
        try { return type.getConstructor(RadialSlot.class).newInstance(slot); }
        catch (NoSuchMethodException oldApi) { return type.getConstructor(RadialSlot.class, boolean.class).newInstance(slot, true); }
    }
    private static PaletteGroup selected(PaletteEditorScreen s) throws Exception { return (PaletteGroup)call(PaletteEditorScreen.class,s,"selected",new Class<?>[]{}); }
    private static void click(PaletteEditorScreen s,int x,int y,int button) throws Exception { s.mouseClicked(event(s,x,y,button),false); }
    private static MouseButtonEvent event(PaletteEditorScreen s,int x,int y,int button) throws Exception {
        var field=AdaptivePaletteScreen.class.getDeclaredField("viewport");field.setAccessible(true);
        var v=(PaletteViewport)field.get(s);
        return new MouseButtonEvent(v.guiX(x),v.guiY(y),new MouseButtonInfo(button,0));
    }
    private static Object call(Class<?> type,Object owner,String name,Class<?>[] params,Object... args) throws Exception {
        var method=type.getDeclaredMethod(name,params); method.setAccessible(true); return method.invoke(owner,args);
    }
    private static void require(boolean value,String label) { if (!value) throw new AssertionError(label); System.out.println("INTERACTION_CHECK " + label); }
}
