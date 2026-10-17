package com.davidblackcn.lorianarchorbit.client.invisible;

import com.davidblackcn.lorianarchorbit.client.ClientConfigRuntime;
import com.davidblackcn.lorianarchorbit.config.ClientConfigDraft;
import com.davidblackcn.lorianarchorbit.config.ClientConfigSnapshot;
import com.davidblackcn.lorianarchorbit.feature.builtin.InvisibleBlocksFeature;
import com.mojang.blaze3d.platform.InputConstants;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class InvisibleBlocksRuntime {
    private static final SectionRebuildCoalescer REBUILDS = new SectionRebuildCoalescer();
    private static KeyMapping toggleKey;

    private InvisibleBlocksRuntime() {
    }

    public static synchronized void initialize(KeyMapping.Category category) {
        if (toggleKey != null) return;
        toggleKey = new KeyMapping(
                "key.lorian_arch_orbit.toggle_invisible_blocks", InputConstants.KEY_V, category
        );
        KeyMappingRegistry.register(toggleKey);
    }

    public static void tick(Minecraft minecraft) {
        while (toggleKey != null && toggleKey.consumeClick()) toggle(minecraft);
        if (REBUILDS.consume()) rebuildLoadedView(minecraft);
    }

    public static boolean visible(InvisibleBlockKind kind) {
        if (!ClientConfigRuntime.initialized()) return false;
        ClientConfigSnapshot config = ClientConfigRuntime.configManager().client();
        if (!config.featureEnabled(InvisibleBlocksFeature.ID) || !config.invisibleBlocksVisible()) return false;
        return switch (kind) {
            case BARRIER -> config.showBarriers();
            case LIGHT -> config.showLightBlocks();
        };
    }

    public static boolean shouldRender(BlockState state) {
        InvisibleBlockKind kind = kind(state);
        return kind != null && visible(kind);
    }

    static InvisibleBlockKind kind(BlockState state) {
        if (state.is(Blocks.BARRIER)) return InvisibleBlockKind.BARRIER;
        if (state.is(Blocks.LIGHT)) return InvisibleBlockKind.LIGHT;
        return null;
    }

    public static void configsChanged() {
        REBUILDS.request();
    }

    public static void closeRuntime() {
        REBUILDS.request();
    }

    static int completedRebuilds() {
        return REBUILDS.completedRebuilds();
    }

    private static void toggle(Minecraft minecraft) {
        ClientConfigSnapshot config = ClientConfigRuntime.configManager().client();
        if (!config.featureEnabled(InvisibleBlocksFeature.ID)) {
            showActionBar(minecraft, "message.lorian_arch_orbit.invisible_blocks.feature_disabled");
            return;
        }
        boolean visible = !config.invisibleBlocksVisible();
        ClientConfigDraft draft = new ClientConfigDraft(config);
        draft.setInvisibleBlocksVisible(visible);
        if (!ClientConfigRuntime.save(draft).successful()) {
            showActionBar(minecraft, "message.lorian_arch_orbit.invisible_blocks.save_failed");
            return;
        }
        if (!visible) {
            showActionBar(minecraft, "message.lorian_arch_orbit.invisible_blocks.disabled");
        } else if (config.showBarriers() && config.showLightBlocks()) {
            showActionBar(minecraft, "message.lorian_arch_orbit.invisible_blocks.enabled_both");
        } else if (config.showBarriers()) {
            showActionBar(minecraft, "message.lorian_arch_orbit.invisible_blocks.enabled_barriers");
        } else if (config.showLightBlocks()) {
            showActionBar(minecraft, "message.lorian_arch_orbit.invisible_blocks.enabled_lights");
        } else {
            showActionBar(minecraft, "message.lorian_arch_orbit.invisible_blocks.enabled_none");
        }
    }

    private static void showActionBar(Minecraft minecraft, String translationKey) {
        minecraft.gui.hud.setOverlayMessage(Component.translatable(translationKey), false);
    }

    private static void rebuildLoadedView(Minecraft minecraft) {
        if (minecraft.level == null) return;
        minecraft.levelExtractor.allChanged();
    }

    public static boolean isSupportedState(BlockState state) {
        return state.is(Blocks.BARRIER) || state.is(Blocks.LIGHT);
    }

}
