package com.davidblackcn.lorianarchorbit.client;

import com.davidblackcn.lorianarchorbit.config.ClientConfigSnapshot;
import com.davidblackcn.lorianarchorbit.config.SmartPickMode;
import com.davidblackcn.lorianarchorbit.interaction.GestureRegistration;
import com.davidblackcn.lorianarchorbit.interaction.InputGesture;
import com.davidblackcn.lorianarchorbit.interaction.InputGestureEvent;
import com.davidblackcn.lorianarchorbit.interaction.PressTiming;
import com.davidblackcn.lorianarchorbit.interaction.RadialAnimationMode;
import com.davidblackcn.lorianarchorbit.interaction.RadialMenuSnapshot;
import com.davidblackcn.lorianarchorbit.interaction.RadialMenuWindow;
import com.davidblackcn.lorianarchorbit.interaction.ScrollAccumulator;
import com.davidblackcn.lorianarchorbit.interaction.WheelLease;
import com.davidblackcn.lorianarchorbit.interaction.WheelPriority;
import com.davidblackcn.lorianarchorbit.smartpick.SmartPickDirection;
import com.davidblackcn.lorianarchorbit.smartpick.SmartPickGestureState;
import com.davidblackcn.lorianarchorbit.smartpick.SmartPickSample;
import com.davidblackcn.lorianarchorbit.smartpick.SmartPickScanResult;
import com.davidblackcn.lorianarchorbit.smartpick.SmartPickScanner;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

public final class ClientSmartPickRuntime {
    private static final String OWNER = "smart_pick";
    private static final int ACTIVATION_DELAY_MS = 100;
    private static final int RANGE_VISIBLE = 12;
    private static final int HISTORY_SIZE = 16;
    private static final System.Logger LOGGER = System.getLogger("lorian_arch_orbit.smart_pick");
    private static final ScrollAccumulator SCROLL = new ScrollAccumulator();
    private static final Deque<StackIdentity> HISTORY = new ArrayDeque<>();
    private static final SmartPickGestureState GESTURE = new SmartPickGestureState();
    private static GestureRegistration registration;
    private static KeyMapping pickKey;
    private static WheelLease lease;
    private static List<SmartPickEntry> candidates = List.of();
    private static int selectedIndex;
    private static boolean initialized;

    private ClientSmartPickRuntime() {
    }

    public static synchronized void initialize() {
        if (initialized) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options == null) {
            throw new IllegalStateException("Smart pick must initialize after Minecraft options are available");
        }
        pickKey = minecraft.options.keyPickItem;
        registerGesture();
        initialized = true;
    }

    public static synchronized void configsChanged() {
        close(false);
        GESTURE.clear();
        if (registration != null) {
            registration.close();
            registration = null;
        }
        if (initialized) {
            registerGesture();
        }
    }

    public static synchronized void closeRuntime() {
        close(false);
        if (registration != null) {
            registration.close();
            registration = null;
        }
        HISTORY.clear();
        initialized = false;
        pickKey = null;
        GESTURE.clear();
    }

    public static synchronized void observeVanillaPick() {
        if (!initialized || !enabled()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gui.screen() != null || minecraft.player == null || !minecraft.player.isCreative()
                || pickKey == null || !pickKey.isDown()) {
            return;
        }
        GESTURE.armForVanillaPick();
    }

    private static void registerGesture() {
        registration = ClientInteractionRuntime.inputs().register(
                OWNER,
                new PressTiming(ACTIVATION_DELAY_MS, 250),
                pickKey::saveString,
                pickKey::isDown,
                ClientSmartPickRuntime::enabled,
                ClientSmartPickRuntime::onGesture
        );
    }

    private static boolean enabled() {
        return ClientConfigRuntime.configManager().client().featureEnabled(OWNER);
    }

    private static synchronized void onGesture(InputGestureEvent event) {
        for (SmartPickGestureState.Action action : GESTURE.accept(event.gesture())) {
            switch (action) {
                case OPEN_SMART_PICK -> GESTURE.smartOpened(open(event.timestampMillis()));
                case CONFIRM_SMART_PICK -> {
                    confirmSelection();
                    close(true);
                }
                case CANCEL -> close(false);
            }
        }
    }

    private static boolean open(long nowMillis) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || minecraft.gameMode == null
                || !minecraft.player.isCreative() || !(minecraft.hitResult instanceof BlockHitResult hit)
                || hit.getType() != HitResult.Type.BLOCK || minecraft.gui.screen() != null) {
            return false;
        }
        BlockPos center = hit.getBlockPos().immutable();
        ClientConfigSnapshot config = ClientConfigRuntime.configManager().client();
        SmartPickScanResult<SmartPickStack> result = SmartPickScanner.scan(
                config.smartPickMode(),
                config.smartPickRadius(),
                config.smartPickCandidateLimit(),
                direction(hit.getDirection()),
                (x, y, z) -> sample(minecraft, center.offset(x, y, z)),
                stack -> new StackIdentity(stack.stack()),
                SmartPickStack::registryId,
                stack -> config.smartPickHistoryWeight() ? historyWeight(new StackIdentity(stack.stack())) : 0
        );
        if (config.smartPickDebugStats()) {
            var stats = result.stats();
            LOGGER.log(System.Logger.Level.INFO,
                    "mode={0}, visited={1}, loaded={2}, valid={3}, unique={4}, scored={5}, elapsed_us={6}",
                    config.smartPickMode(), stats.visitedPositions(), stats.loadedPositions(), stats.validSamples(),
                    stats.uniqueCandidates(), stats.scoredCandidates(), stats.elapsedNanos() / 1_000L);
        }
        List<SmartPickEntry> scanned = result.candidates().stream()
                .map(candidate -> new SmartPickEntry(candidate.value().stack(), candidate.registryId()))
                .toList();
        if (scanned.size() < 2) {
            return false;
        }
        WheelLease claimed = ClientInteractionRuntime.wheel().claim(
                OWNER, WheelPriority.SMART_PICK, ClientSmartPickRuntime::onScroll, ClientSmartPickRuntime::revoked
        ).orElse(null);
        if (claimed == null) {
            return false;
        }
        lease = claimed;
        candidates = scanned;
        selectedIndex = 0;
        ClientInteractionRuntime.hud().showRadial(OWNER, hudSnapshot(config.smartPickMode()), animationMode(), nowMillis);
        showSelectedName();
        return true;
    }

    private static SmartPickSample<SmartPickStack> sample(Minecraft minecraft, BlockPos pos) {
        if (minecraft.level == null || minecraft.level.isOutsideBuildHeight(pos)) {
            return SmartPickSample.empty();
        }
        if (!minecraft.level.hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) {
            return SmartPickSample.unloaded();
        }
        var state = minecraft.level.getBlockState(pos);
        if (state.isAir()) {
            return SmartPickSample.empty();
        }
        ItemStack stack = state.getCloneItemStack(minecraft.level, pos, true);
        if (stack.isEmpty()) {
            return SmartPickSample.empty();
        }
        stack.setCount(1);
        return SmartPickSample.value(new SmartPickStack(
                stack, BuiltInRegistries.ITEM.getKey(stack.getItem()).toString()
        ));
    }

    private static synchronized boolean onScroll(double amountX, double amountY) {
        if (candidates.isEmpty() || lease == null || !lease.active()) {
            return false;
        }
        double amount = amountY != 0.0 ? amountY : amountX;
        int steps = SCROLL.add(amount);
        if (steps != 0) {
            int selectionSteps = -steps;
            selectedIndex = Math.floorMod(selectedIndex + selectionSteps, candidates.size());
            SmartPickMode mode = ClientConfigRuntime.configManager().client().smartPickMode();
            ClientInteractionRuntime.hud().rotateRadial(
                    OWNER, hudSnapshot(mode), selectionSteps, ClientInteractionRuntime.nowMillis()
            );
            showSelectedName();
        }
        return true;
    }

    private static RadialMenuSnapshot<RadialHudEntry> hudSnapshot(SmartPickMode mode) {
        List<RadialHudEntry> entries = candidates.stream()
                .map(entry -> new RadialHudEntry(entry.stack().getHoverName(), entry.stack()))
                .toList();
        return mode == SmartPickMode.RANGE
                ? RadialMenuWindow.from(entries, selectedIndex, RANGE_VISIBLE)
                : new RadialMenuSnapshot<>(entries, selectedIndex);
    }

    private static void showSelectedName() {
        if (candidates.isEmpty()) {
            return;
        }
        ClientInteractionRuntime.hud().showNumeric(OWNER, candidates.get(selectedIndex).stack().getHoverName());
    }

    private static void confirmSelection() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.gameMode == null || !minecraft.player.isCreative()
                || candidates.isEmpty()) {
            return;
        }
        ItemStack replacement = candidates.get(selectedIndex).stack().copy();
        int slot = minecraft.player.getInventory().getSelectedSlot();
        minecraft.player.getInventory().setSelectedItem(replacement);
        minecraft.gameMode.handleCreativeModeItemAdd(replacement, 36 + slot);
        remember(new StackIdentity(replacement));
    }

    private static void remember(StackIdentity identity) {
        HISTORY.remove(identity);
        HISTORY.addFirst(identity);
        while (HISTORY.size() > HISTORY_SIZE) {
            HISTORY.removeLast();
        }
    }

    private static int historyWeight(StackIdentity identity) {
        int index = 0;
        for (StackIdentity recent : HISTORY) {
            if (recent.equals(identity)) {
                return Math.max(1, 4 - index / 4);
            }
            index++;
        }
        return 0;
    }

    private static synchronized void close(boolean animate) {
        WheelLease previous = lease;
        lease = null;
        if (previous != null) {
            previous.close();
        }
        candidates = List.of();
        selectedIndex = 0;
        SCROLL.reset();
        ClientInteractionRuntime.hud().hideNumeric(OWNER);
        if (animate) {
            ClientInteractionRuntime.hud().closeRadial(OWNER, ClientInteractionRuntime.nowMillis());
        } else {
            ClientInteractionRuntime.hud().hideRadial(OWNER);
        }
    }

    private static synchronized void revoked() {
        lease = null;
        candidates = List.of();
        selectedIndex = 0;
        SCROLL.reset();
        ClientInteractionRuntime.hud().hideRadial(OWNER);
        ClientInteractionRuntime.hud().hideNumeric(OWNER);
    }

    private static SmartPickDirection direction(Direction direction) {
        return new SmartPickDirection(direction.getStepX(), direction.getStepY(), direction.getStepZ());
    }

    private static RadialAnimationMode animationMode() {
        return switch (ClientConfigRuntime.configManager().client().paletteAnimation()) {
            case CLOCKWISE -> RadialAnimationMode.CLOCKWISE;
            case EXPAND -> RadialAnimationMode.EXPAND;
            case OFF -> RadialAnimationMode.OFF;
        };
    }

    private record SmartPickStack(ItemStack stack, String registryId) {
        private SmartPickStack {
            stack = stack.copy();
        }

        @Override
        public ItemStack stack() {
            return stack.copy();
        }
    }

    private record SmartPickEntry(ItemStack stack, String registryId) {
        private SmartPickEntry {
            stack = stack.copy();
        }

        @Override
        public ItemStack stack() {
            return stack.copy();
        }
    }

    private static final class StackIdentity {
        private final ItemStack stack;
        private final int hash;

        private StackIdentity(ItemStack stack) {
            this.stack = stack.copy();
            this.hash = ItemStack.hashItemAndComponents(this.stack);
        }

        @Override
        public boolean equals(Object other) {
            return this == other || other instanceof StackIdentity that
                    && ItemStack.isSameItemSameComponents(stack, that.stack);
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }
}
