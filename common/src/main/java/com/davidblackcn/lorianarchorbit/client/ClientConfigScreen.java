package com.davidblackcn.lorianarchorbit.client;

import com.davidblackcn.lorianarchorbit.config.ClientConfigDraft;
import com.davidblackcn.lorianarchorbit.config.ClientConfigSnapshot;
import com.davidblackcn.lorianarchorbit.config.PaletteAnimation;
import com.davidblackcn.lorianarchorbit.config.PalettePreset;
import com.davidblackcn.lorianarchorbit.config.SmartPickMode;
import dev.isxander.yacl3.api.ButtonOption;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import dev.isxander.yacl3.api.controller.EnumControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class ClientConfigScreen {
    private ClientConfigScreen() {
    }

    public static Screen create(Screen parent) {
        ClientConfigSnapshot current = ClientConfigRuntime.configManager().client();
        ClientConfigDraft draft = new ClientConfigDraft(current);
        List<Option<?>> resettable = new ArrayList<>();

        ConfigCategory.Builder features = ConfigCategory.createBuilder()
                .name(text("category.features"));
        addBoolean(features, resettable, "reach_extension.enabled", false,
                () -> draft.featureEnabled("reach_extension"),
                value -> draft.setFeatureEnabled("reach_extension", value));
        addBoolean(features, resettable, "palette_wheel.enabled", true,
                () -> draft.featureEnabled("palette_wheel"),
                value -> draft.setFeatureEnabled("palette_wheel", value));
        addBoolean(features, resettable, "smart_pick.enabled", true,
                () -> draft.featureEnabled("smart_pick"),
                value -> draft.setFeatureEnabled("smart_pick", value));
        addBoolean(features, resettable, "connected_texture_fix.enabled", true,
                () -> draft.featureEnabled("connected_texture_fix"),
                value -> draft.setFeatureEnabled("connected_texture_fix", value));
        addBoolean(features, resettable, "invisible_blocks.enabled", true,
                () -> draft.featureEnabled("invisible_blocks"),
                value -> draft.setFeatureEnabled("invisible_blocks", value));

        ConfigCategory.Builder behavior = ConfigCategory.createBuilder()
                .name(text("category.behavior"));
        addInteger(behavior, resettable, "reach_extension.distance", 5, 5, 128, 1,
                draft::reachDistance, draft::setReachDistance);
        addBoolean(behavior, resettable, "connected_texture_fix.walls", true,
                draft::fixWalls, draft::setFixWalls);
        addBoolean(behavior, resettable, "connected_texture_fix.beds", true,
                draft::fixBeds, draft::setFixBeds);
        addBoolean(behavior, resettable, "connected_texture_fix.doors", true,
                draft::fixDoors, draft::setFixDoors);
        addBoolean(behavior, resettable, "connected_texture_fix.pistons", true,
                draft::fixPistons, draft::setFixPistons);
        addBoolean(behavior, resettable, "connected_texture_fix.nether_portals", true,
                draft::fixNetherPortals, draft::setFixNetherPortals);
        addBoolean(behavior, resettable, "connected_texture_fix.end_portals", true,
                draft::fixEndPortals, draft::setFixEndPortals);
        addEnum(behavior, resettable, "palette_wheel.animation", PaletteAnimation.CLOCKWISE,
                PaletteAnimation.class, draft::paletteAnimation, draft::setPaletteAnimation);
        addPreset(behavior, resettable, "palette_wheel.primary_default_preset", PalettePreset.ITEM_TAG_A,
                draft::primaryPalettePreset, draft::setPrimaryPalettePreset);
        addPreset(behavior, resettable, "palette_wheel.secondary_default_preset", PalettePreset.ITEM_TAG_B,
                draft::secondaryPalettePreset, draft::setSecondaryPalettePreset);
        behavior.option(ButtonOption.createBuilder()
                .name(text("palette_wheel.editor.name"))
                .text(text("palette_wheel.editor.button"))
                .description(description("palette_wheel.editor"))
                .action((screen, option) -> net.minecraft.client.Minecraft.getInstance()
                        .setScreenAndShow(new PaletteEditorScreen(screen)))
                .build());
        addEnum(behavior, resettable, "smart_pick.mode", SmartPickMode.CONTEXT,
                SmartPickMode.class, draft::smartPickMode, draft::setSmartPickMode);
        addInteger(behavior, resettable, "smart_pick.scan_radius", 3, 1, 3, 1,
                draft::smartPickRadius, draft::setSmartPickRadius);
        addInteger(behavior, resettable, "smart_pick.candidate_limit", 12, 8, 24, 1,
                draft::smartPickCandidateLimit, draft::setSmartPickCandidateLimit);
        addBoolean(behavior, resettable, "smart_pick.history_weight", true,
                draft::smartPickHistoryWeight, draft::setSmartPickHistoryWeight);
        addBoolean(behavior, resettable, "smart_pick.debug_stats", false,
                draft::smartPickDebugStats, draft::setSmartPickDebugStats);
        addBoolean(behavior, resettable, "invisible_blocks.currently_visible", false,
                draft::invisibleBlocksVisible, draft::setInvisibleBlocksVisible);
        addBoolean(behavior, resettable, "invisible_blocks.show_barriers", true,
                draft::showBarriers, draft::setShowBarriers);
        addBoolean(behavior, resettable, "invisible_blocks.show_light_blocks", true,
                draft::showLightBlocks, draft::setShowLightBlocks);

        ConfigCategory.Builder interfaceCategory = ConfigCategory.createBuilder()
                .name(text("category.interface"));
        addBoolean(interfaceCategory, resettable, "ui.hud_enabled", true,
                draft::hudEnabled, draft::setHudEnabled);
        interfaceCategory.option(ButtonOption.createBuilder()
                .name(text("restore_defaults.name"))
                .text(text("restore_defaults.button"))
                .description(description("restore_defaults"))
                .action((screen, option) -> resettable.forEach(Option::requestSetDefault))
                .build());

        return YetAnotherConfigLib.createBuilder()
                .title(text("title"))
                .category(features.build())
                .category(behavior.build())
                .category(interfaceCategory.build())
                .save(() -> ClientConfigRuntime.save(draft))
                .build()
                .generateScreen(parent);
    }

    private static void addBoolean(
            ConfigCategory.Builder category,
            List<Option<?>> resettable,
            String key,
            boolean defaultValue,
            Supplier<Boolean> getter,
            Consumer<Boolean> setter
    ) {
        Option<Boolean> option = Option.<Boolean>createBuilder()
                .name(text(key + ".name"))
                .description(description(key))
                .binding(defaultValue, getter, setter)
                .controller(BooleanControllerBuilder::create)
                .build();
        resettable.add(option);
        category.option(option);
    }

    private static void addInteger(
            ConfigCategory.Builder category,
            List<Option<?>> resettable,
            String key,
            int defaultValue,
            int minimum,
            int maximum,
            int step,
            Supplier<Integer> getter,
            Consumer<Integer> setter
    ) {
        Option<Integer> option = Option.<Integer>createBuilder()
                .name(text(key + ".name"))
                .description(description(key))
                .binding(defaultValue, getter, setter)
                .controller(value -> IntegerSliderControllerBuilder.create(value)
                        .range(minimum, maximum)
                        .step(step))
                .build();
        resettable.add(option);
        category.option(option);
    }

    private static <E extends Enum<E>> void addEnum(
            ConfigCategory.Builder category,
            List<Option<?>> resettable,
            String key,
            E defaultValue,
            Class<E> type,
            Supplier<E> getter,
            Consumer<E> setter
    ) {
        Option<E> option = Option.<E>createBuilder()
                .name(text(key + ".name"))
                .description(description(key))
                .binding(defaultValue, getter, setter)
                .controller(value -> EnumControllerBuilder.create(value).enumClass(type))
                .build();
        resettable.add(option);
        category.option(option);
    }

    private static void addPreset(
            ConfigCategory.Builder category,
            List<Option<?>> resettable,
            String key,
            PalettePreset defaultValue,
            Supplier<PalettePreset> getter,
            Consumer<PalettePreset> setter
    ) {
        Option<PalettePreset> option = Option.<PalettePreset>createBuilder()
                .name(text(key + ".name"))
                .description(description(key))
                .binding(defaultValue, getter, setter)
                .controller(value -> EnumControllerBuilder.create(value)
                        .enumClass(PalettePreset.class)
                        .valueFormatter(preset -> text("palette_wheel.preset."
                                + preset.name().toLowerCase(java.util.Locale.ROOT))))
                .build();
        resettable.add(option);
        category.option(option);
    }

    private static OptionDescription description(String key) {
        return OptionDescription.of(text(key + ".description"));
    }

    private static Component text(String suffix) {
        return Component.translatable("config.lorian_arch_orbit." + suffix);
    }
}
