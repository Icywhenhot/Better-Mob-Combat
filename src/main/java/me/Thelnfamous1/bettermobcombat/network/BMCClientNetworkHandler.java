package me.Thelnfamous1.bettermobcombat.network;

import me.Thelnfamous1.bettermobcombat.BetterMobCombat;
import me.Thelnfamous1.bettermobcombat.config.BMCServerConfig;
import net.bettercombat.client.BetterCombatClientMod;
import net.bettercombat.client.animation.PlayerAttackAnimatable;
import net.bettercombat.logic.AnimatedHand;
import net.bettercombat.logic.PlayerAttackProperties;
import net.bettercombat.network.Packets;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

public class BMCClientNetworkHandler {
    static void handleAttackAnimation(int mobId, String animationName, float length, AnimatedHand animatedHand, float upswing) {
        Entity entity = Minecraft.getInstance().level.getEntity(mobId);
        if (entity instanceof LivingEntity living) {
            boolean stop = animationName.equals(Packets.AttackAnimation.StopSymbol);
            if (!BetterMobCombat.areMobAnimationsEnabled()) {
                // Vanilla animations selected for this client. Better Combat's attack pipeline suppresses
                // MeleeAttackGoal's own swing() call, so without this the mob would deal damage with no
                // animation whatsoever. Client-side swing() just sets the local swing state - it only
                // broadcasts a packet when called on a ServerLevel - which is exactly the vanilla arm swing,
                // and keeps the whole option a per-client choice the server never has to know about.
                if (!stop) {
                    living.swing(animatedHand.isOffHand() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
                }
                return;
            }
            if (stop) {
                ((PlayerAttackAnimatable)entity).stopAttackAnimation(length);
            } else {
                ((PlayerAttackAnimatable)entity).playAttackAnimation(animationName, animatedHand, length, upswing);
            }
        }
    }

    public static void handleConfigSync(String json) {
        BMCServerConfig config = BMCServerConfig.deserialize(json);
        BetterMobCombat.updateServerConfig(config, true);
    }

    public static void handlePlaySound(int mobId, double x, double y, double z, String soundId, float volume, float pitch, long seed) {
        try {
            if (BetterCombatClientMod.config.weaponSwingSoundVolume == 0) {
                return;
            }

            Entity entity = Minecraft.getInstance().level.getEntity(mobId);
            SoundEvent soundEvent = BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse(soundId));
            int configVolume = BetterCombatClientMod.config.weaponSwingSoundVolume;
            volume *= ((float)Math.min(Math.max(configVolume, 0), 100) / 100.0F);
            Minecraft.getInstance().level.playLocalSound(x, y, z, soundEvent, entity.getSoundSource(), volume, pitch, true);
        } catch (Exception var5) {
            var5.printStackTrace();
        }
    }

    public static void handleComboSync(int mobId, int comboCount) {
        Entity entity = Minecraft.getInstance().level.getEntity(mobId);
        if(entity instanceof Mob mob){
            ((PlayerAttackProperties)mob).setComboCount(comboCount);
        }
    }
}
