package com.davidblackcn.lorianarchorbit.reach;

import com.davidblackcn.lorianarchorbit.LorianArchOrbit;
import com.davidblackcn.lorianarchorbit.config.CommonConfigRuntime;
import com.davidblackcn.lorianarchorbit.config.ServerConfigSnapshot;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class ServerReachRuntime {
    private static final Identifier MODIFIER_ID = Identifier.fromNamespaceAndPath(
            LorianArchOrbit.MOD_ID, "block_interaction_range"
    );
    private static final ReachRateLimiter RATE_LIMITER = new ReachRateLimiter();
    private static final Set<UUID> MODIFIED = new HashSet<>();
    private static boolean initialized;

    private ServerReachRuntime() {
    }

    public static synchronized void initialize() {
        if (initialized) return;
        initialized = true;
        NetworkManager.registerC2S(ReachPackets.Hello.TYPE, ReachPackets.Hello.CODEC, ServerReachRuntime::receiveHello);
        NetworkManager.registerC2S(ReachPackets.SetDistance.TYPE, ReachPackets.SetDistance.CODEC,
                ServerReachRuntime::receiveSetDistance);
        if (Platform.getEnvironment() == Env.SERVER) {
            NetworkManager.registerS2CPayloadType(ReachPackets.Capabilities.TYPE, ReachPackets.Capabilities.CODEC);
            NetworkManager.registerS2CPayloadType(ReachPackets.Result.TYPE, ReachPackets.Result.CODEC);
        }
        PlayerEvent.PLAYER_QUIT.register(ServerReachRuntime::clear);
        PlayerEvent.PLAYER_CLONE.register((oldPlayer, newPlayer, wonGame) -> {
            clear(oldPlayer);
            clear(newPlayer);
        });
        TickEvent.PLAYER_POST.register(player -> {
            if (player instanceof ServerPlayer serverPlayer && MODIFIED.contains(serverPlayer.getUUID())) {
                ServerConfigSnapshot config = CommonConfigRuntime.configManager().current();
                if (!config.reachEnabled() || (config.creativeOnly() && !serverPlayer.isCreative())) {
                    resetReach(serverPlayer);
                    sendResult(serverPlayer, false,
                            config.reachEnabled() ? ReachDecision.NOT_CREATIVE : ReachDecision.DISABLED);
                }
            }
        });
        LifecycleEvent.SERVER_STOPPING.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) clear(player);
        });
    }

    private static void receiveHello(ReachPackets.Hello payload, NetworkManager.PacketContext context) {
        context.queue(() -> {
            if (!(context.getPlayer() instanceof ServerPlayer player)) return;
            ServerConfigSnapshot config = CommonConfigRuntime.configManager().current();
            ReachDecision decision = payload.version() != ReachProtocol.VERSION
                    ? ReachDecision.INCOMPATIBLE
                    : config.reachEnabled() ? ReachDecision.ACCEPTED : ReachDecision.DISABLED;
            sendCapabilities(player, config, decision);
        });
    }

    public static void configsChanged(MinecraftServer server) {
        ServerConfigSnapshot config = CommonConfigRuntime.configManager().current();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ReachDecision decision = validate(player, ReachProtocol.VERSION, config);
            if (decision != ReachDecision.ACCEPTED) {
                boolean wasModified = MODIFIED.contains(player.getUUID());
                resetReach(player);
                if (wasModified && NetworkManager.canPlayerReceive(player, ReachPackets.Result.TYPE)) {
                    sendResult(player, false, decision);
                }
            } else if (MODIFIED.contains(player.getUUID())
                    && player.blockInteractionRange() > config.maximumDistance()) {
                apply(player, config.maximumDistance());
            }
            if (NetworkManager.canPlayerReceive(player, ReachPackets.Capabilities.TYPE)) {
                ReachDecision availability = config.reachEnabled()
                        ? ReachDecision.ACCEPTED : ReachDecision.DISABLED;
                sendCapabilities(player, config, availability);
            }
        }
    }

    private static void receiveSetDistance(ReachPackets.SetDistance payload, NetworkManager.PacketContext context) {
        context.queue(() -> {
            if (!(context.getPlayer() instanceof ServerPlayer player)) return;
            ServerConfigSnapshot config = CommonConfigRuntime.configManager().current();
            if (payload.version() == ReachProtocol.VERSION && payload.requestedDistance() == 0) {
                resetReach(player);
                sendResult(player, true, ReachDecision.ACCEPTED);
                return;
            }
            ReachDecision decision = validate(player, payload.version(), config);
            if (decision == ReachDecision.ACCEPTED && !RATE_LIMITER.allow(
                    player.getUUID(), System.currentTimeMillis(), config.requestsPerSecond())) {
                decision = ReachDecision.RATE_LIMITED;
            }
            if (decision != ReachDecision.ACCEPTED) {
                if (decision != ReachDecision.RATE_LIMITED) {
                    resetReach(player);
                }
                sendResult(player, false, decision);
                return;
            }
            int confirmed = ReachProtocol.clamp(payload.requestedDistance(), config.maximumDistance());
            apply(player, confirmed);
            NetworkManager.sendToPlayer(player, new ReachPackets.Result(
                    ReachProtocol.VERSION, true, confirmed, ReachDecision.ACCEPTED
            ));
        });
    }

    private static ReachDecision validate(ServerPlayer player, int version, ServerConfigSnapshot config) {
        if (version != ReachProtocol.VERSION) return ReachDecision.INCOMPATIBLE;
        if (!config.reachEnabled()) return ReachDecision.DISABLED;
        if (config.creativeOnly() && !player.isCreative()) return ReachDecision.NOT_CREATIVE;
        if (!hasPermission(player, config.requiredPermissionLevel())) return ReachDecision.NO_PERMISSION;
        return ReachDecision.ACCEPTED;
    }

    private static boolean hasPermission(ServerPlayer player, int level) {
        if (level <= 0) return true;
        Permission permission = switch (level) {
            case 1 -> Permissions.COMMANDS_MODERATOR;
            case 2 -> Permissions.COMMANDS_GAMEMASTER;
            case 3 -> Permissions.COMMANDS_ADMIN;
            default -> Permissions.COMMANDS_OWNER;
        };
        return player.permissions().hasPermission(permission);
    }

    private static synchronized void apply(ServerPlayer player, int distance) {
        AttributeInstance attribute = player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE);
        if (attribute == null) return;
        if (attribute.hasModifier(MODIFIER_ID)
                && Math.abs(player.blockInteractionRange() - distance) <= 0.000_001) {
            return;
        }
        attribute.removeModifier(MODIFIER_ID);
        double current = attribute.getValue();
        double amount = distance - current;
        if (Math.abs(amount) > 0.000_001) {
            attribute.addOrUpdateTransientModifier(new AttributeModifier(
                    MODIFIER_ID, amount, AttributeModifier.Operation.ADD_VALUE
            ));
            MODIFIED.add(player.getUUID());
        } else {
            MODIFIED.remove(player.getUUID());
        }
    }

    private static void sendResult(ServerPlayer player, boolean accepted, ReachDecision decision) {
        NetworkManager.sendToPlayer(player, new ReachPackets.Result(
                ReachProtocol.VERSION, accepted, (int) Math.round(player.blockInteractionRange()), decision
        ));
    }

    private static void sendCapabilities(
            ServerPlayer player,
            ServerConfigSnapshot config,
            ReachDecision decision
    ) {
        NetworkManager.sendToPlayer(player, new ReachPackets.Capabilities(
                ReachProtocol.VERSION,
                decision == ReachDecision.ACCEPTED,
                config.maximumDistance(),
                (int) Math.round(player.blockInteractionRange()),
                decision
        ));
    }

    private static synchronized void clear(ServerPlayer player) {
        resetReach(player);
        RATE_LIMITER.remove(player.getUUID());
    }

    private static synchronized void resetReach(ServerPlayer player) {
        AttributeInstance attribute = player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE);
        if (attribute != null) attribute.removeModifier(MODIFIER_ID);
        MODIFIED.remove(player.getUUID());
    }
}
