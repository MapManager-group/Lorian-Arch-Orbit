package com.davidblackcn.lorianarchorbit.client;

import com.davidblackcn.lorianarchorbit.config.ClientConfigDraft;
import com.davidblackcn.lorianarchorbit.interaction.ScrollAccumulator;
import com.davidblackcn.lorianarchorbit.interaction.WheelLease;
import com.davidblackcn.lorianarchorbit.interaction.WheelPriority;
import com.davidblackcn.lorianarchorbit.reach.ReachDecision;
import com.davidblackcn.lorianarchorbit.reach.ReachPackets;
import com.davidblackcn.lorianarchorbit.reach.ReachProtocol;
import com.mojang.blaze3d.platform.InputConstants;
import dev.architectury.event.events.client.ClientPlayerEvent;
import dev.architectury.networking.NetworkManager;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class ClientReachRuntime {
    private static final String OWNER = "reach_extension";
    private static final ScrollAccumulator SCROLL = new ScrollAccumulator();
    private static KeyMapping adjustKey;
    private static WheelLease lease;
    private static int requestedDistance = ReachProtocol.MINIMUM_DISTANCE;
    private static int confirmedDistance;
    private static int serverMaximum = ReachProtocol.MAXIMUM_DISTANCE;
    private static boolean supported;
    private static boolean previousDown;
    private static boolean dirty;
    private static boolean warned;
    private static ReachDecision availabilityDecision = ReachDecision.DISABLED;

    private ClientReachRuntime() {
    }

    public static synchronized void initialize(KeyMapping.Category category) {
        if (adjustKey != null) return;
        adjustKey = new KeyMapping("key.lorian_arch_orbit.adjust_reach", InputConstants.KEY_G, category);
        KeyMappingRegistry.register(adjustKey);
        requestedDistance = ClientConfigRuntime.configManager().client().reachDistance();
        NetworkManager.registerReceiver(NetworkManager.s2c(), ReachPackets.Capabilities.TYPE,
                ReachPackets.Capabilities.CODEC, (payload, context) -> context.queue(() -> capabilities(payload)));
        NetworkManager.registerReceiver(NetworkManager.s2c(), ReachPackets.Result.TYPE,
                ReachPackets.Result.CODEC, (payload, context) -> context.queue(() -> result(payload)));
        ClientPlayerEvent.CLIENT_PLAYER_JOIN.register(player -> handshake());
        ClientPlayerEvent.CLIENT_PLAYER_QUIT.register(player -> resetConnection());
        ClientPlayerEvent.CLIENT_PLAYER_RESPAWN.register((oldPlayer, newPlayer) -> handshake());
    }

    public static synchronized void tick(Minecraft minecraft) {
        if (adjustKey == null) return;
        boolean down = adjustKey.isDown() && enabled() && minecraft.gui.screen() == null
                && minecraft.player != null && minecraft.player.isCreative();
        if (down && !previousDown) open(minecraft);
        if (!down && previousDown) close(true);
        previousDown = down;
        if (down && lease != null && lease.active()) showConfirmed(minecraft);
    }

    public static synchronized void configsChanged() {
        requestedDistance = ClientConfigRuntime.configManager().client().reachDistance();
        if (!enabled()) {
            if (supported && NetworkManager.canServerReceive(ReachPackets.SetDistance.TYPE)) {
                NetworkManager.sendToServer(new ReachPackets.SetDistance(ReachProtocol.VERSION, 0));
            }
            close(false);
        }
    }

    public static synchronized void closeRuntime() {
        close(false);
        resetConnection();
        adjustKey = null;
    }

    private static void handshake() {
        resetConnection();
        if (NetworkManager.canServerReceive(ReachPackets.Hello.TYPE)) {
            NetworkManager.sendToServer(new ReachPackets.Hello(ReachProtocol.VERSION));
        }
    }

    private static void capabilities(ReachPackets.Capabilities payload) {
        supported = payload.version() == ReachProtocol.VERSION && payload.available();
        serverMaximum = Math.max(ReachProtocol.MINIMUM_DISTANCE,
                Math.min(ReachProtocol.MAXIMUM_DISTANCE, payload.maximumDistance()));
        confirmedDistance = payload.confirmedDistance();
        availabilityDecision = payload.decision();
        requestedDistance = Math.min(requestedDistance, serverMaximum);
        if (supported) warned = false;
    }

    private static void result(ReachPackets.Result payload) {
        if (payload.version() != ReachProtocol.VERSION) {
            supported = false;
            warn(ReachDecision.INCOMPATIBLE);
            return;
        }
        confirmedDistance = payload.confirmedDistance();
        if (!payload.accepted()) {
            requestedDistance = Math.max(ReachProtocol.MINIMUM_DISTANCE,
                    Math.min(serverMaximum, confirmedDistance));
            warn(payload.decision());
        }
    }

    private static void open(Minecraft minecraft) {
        confirmedDistance = (int) Math.round(minecraft.player.blockInteractionRange());
        requestedDistance = Math.max(ReachProtocol.MINIMUM_DISTANCE,
                Math.min(serverMaximum, confirmedDistance));
        dirty = false;
        lease = ClientInteractionRuntime.wheel().claim(
                OWNER, WheelPriority.REACH_ADJUSTMENT, ClientReachRuntime::onScroll, ClientReachRuntime::revoked
        ).orElse(null);
        if (lease != null) showConfirmed(minecraft);
    }

    private static boolean onScroll(double amountX, double amountY) {
        if (lease == null || !lease.active()) return false;
        double amount = amountY != 0.0 ? amountY : amountX;
        int steps = SCROLL.add(amount);
        if (steps == 0) return true;
        Minecraft minecraft = Minecraft.getInstance();
        if (!supported || !NetworkManager.canServerReceive(ReachPackets.SetDistance.TYPE)) {
            warn(availabilityDecision);
            return true;
        }
        int increment = minecraft.options.keyShift.isDown() ? 8 : 1;
        requestedDistance = Math.max(ReachProtocol.MINIMUM_DISTANCE,
                Math.min(serverMaximum, requestedDistance + steps * increment));
        dirty = true;
        NetworkManager.sendToServer(new ReachPackets.SetDistance(ReachProtocol.VERSION, requestedDistance));
        return true;
    }

    private static void showConfirmed(Minecraft minecraft) {
        int current = confirmedDistance > 0 ? confirmedDistance
                : (int) Math.round(minecraft.player.blockInteractionRange());
        ClientInteractionRuntime.hud().showTransparentNumericAbove(
                OWNER, Component.translatable("hud.lorian_arch_orbit.reach", current)
        );
    }

    private static void warn(ReachDecision decision) {
        Minecraft minecraft = Minecraft.getInstance();
        if (warned || minecraft.player == null) return;
        warned = true;
        minecraft.player.sendOverlayMessage(Component.translatable(
                "message.lorian_arch_orbit.reach." + decision.name().toLowerCase(java.util.Locale.ROOT)
        ));
    }

    private static void persistRequested() {
        if (!dirty) return;
        dirty = false;
        ClientConfigDraft draft = new ClientConfigDraft(ClientConfigRuntime.configManager().client());
        draft.setReachDistance(requestedDistance);
        ClientConfigRuntime.save(draft);
    }

    private static void close(boolean persist) {
        WheelLease previous = lease;
        lease = null;
        if (previous != null) previous.close();
        SCROLL.reset();
        previousDown = false;
        ClientInteractionRuntime.hud().hideNumeric(OWNER);
        if (persist) persistRequested();
    }

    private static void revoked() {
        lease = null;
        SCROLL.reset();
        ClientInteractionRuntime.hud().hideNumeric(OWNER);
        persistRequested();
    }

    private static void resetConnection() {
        close(false);
        supported = false;
        confirmedDistance = 0;
        serverMaximum = ReachProtocol.MAXIMUM_DISTANCE;
        warned = false;
        availabilityDecision = ReachDecision.DISABLED;
    }

    private static boolean enabled() {
        return ClientConfigRuntime.configManager().client().featureEnabled(OWNER);
    }
}
