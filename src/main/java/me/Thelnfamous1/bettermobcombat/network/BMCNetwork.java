package me.Thelnfamous1.bettermobcombat.network;

import me.Thelnfamous1.bettermobcombat.BetterMobCombat;
import me.Thelnfamous1.bettermobcombat.Constants;
import net.bettercombat.logic.AnimatedHand;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * NeoForge networking for Better Mob Combat. Replaces the old Forge {@code SimpleChannel} and the
 * loader-specific {@code ForgePlatformHelper} send helpers. All packets are server -> client only.
 */
public class BMCNetwork {

    private static final String PROTOCOL_VERSION = "1.0";

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(Constants.MOD_ID).versioned(PROTOCOL_VERSION).optional();
        registrar.playToClient(S2CAttackAnimation.TYPE, S2CAttackAnimation.STREAM_CODEC, BMCNetwork::handleAttackAnimation);
        registrar.playToClient(S2CConfigSync.TYPE, S2CConfigSync.STREAM_CODEC, BMCNetwork::handleConfigSync);
        registrar.playToClient(S2CAttackSound.TYPE, S2CAttackSound.STREAM_CODEC, BMCNetwork::handleAttackSound);
        registrar.playToClient(S2CComboCountSync.TYPE, S2CComboCountSync.STREAM_CODEC, BMCNetwork::handleComboCount);
    }

    // ---- client-side handlers (only ever invoked on the physical client) ----

    private static void handleAttackAnimation(S2CAttackAnimation msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> BMCClientNetworkHandler.handleAttackAnimation(msg.mobId(), msg.animationName(), msg.length(), msg.animatedHand(), msg.upswing()));
    }

    private static void handleConfigSync(S2CConfigSync msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> BMCClientNetworkHandler.handleConfigSync(msg.json()));
    }

    private static void handleAttackSound(S2CAttackSound msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> BMCClientNetworkHandler.handlePlaySound(msg.mobId(), msg.x(), msg.y(), msg.z(), msg.soundId(), msg.volume(), msg.pitch(), msg.seed()));
    }

    private static void handleComboCount(S2CComboCountSync msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> BMCClientNetworkHandler.handleComboSync(msg.mobId(), msg.comboCount()));
    }

    // ---- server-side send helpers ----

    public static void playMobAttackAnimation(LivingEntity mob, AnimatedHand animatedHand, String animationName, float length, float upswing) {
        PacketDistributor.sendToPlayersTrackingEntity(mob, new S2CAttackAnimation(mob.getId(), animatedHand, animationName, length, upswing));
    }

    public static void stopMobAttackAnimation(LivingEntity mob, int downWind) {
        PacketDistributor.sendToPlayersTrackingEntity(mob, S2CAttackAnimation.stop(mob.getId(), downWind));
    }

    public static void syncServerConfig(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new S2CConfigSync(BetterMobCombat.getServerConfigSerialized()));
    }

    public static void syncServerConfig() {
        if (ServerLifecycleHooks.getCurrentServer() != null) {
            PacketDistributor.sendToAllPlayers(new S2CConfigSync(BetterMobCombat.getServerConfigSerialized()));
        }
    }

    public static void playMobAttackSound(ServerLevel world, int mobId, double x, double y, double z, String soundId, float volume, float pitch, long seed, float distance, ResourceKey<Level> dimension) {
        PacketDistributor.sendToPlayersNear(world, null, x, y, z, distance, new S2CAttackSound(mobId, x, y, z, soundId, volume, pitch, seed));
    }

    public static void syncMobComboCount(LivingEntity mob, int comboCount) {
        PacketDistributor.sendToPlayersTrackingEntity(mob, new S2CComboCountSync(mob.getId(), comboCount));
    }
}
