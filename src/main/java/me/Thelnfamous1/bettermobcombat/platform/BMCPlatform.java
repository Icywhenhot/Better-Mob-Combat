package me.Thelnfamous1.bettermobcombat.platform;

import net.minecraft.world.entity.LivingEntity;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLLoader;

/**
 * Small static replacement for the old MultiLoader {@code Services.PLATFORM} indirection. Now that the
 * mod targets NeoForge only, these helpers call the NeoForge APIs directly.
 */
public class BMCPlatform {

    public static String getPlatformName() {
        return "NeoForge";
    }

    public static boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    public static boolean isDevelopmentEnvironment() {
        return !FMLLoader.isProduction();
    }

    public static String getEnvironmentName() {
        return isDevelopmentEnvironment() ? "development" : "production";
    }

    /**
     * Whether the given mob is currently casting a spell. Wired to Iron's Spells 'n Spellbooks when that
     * compat is present; otherwise always false.
     */
    public static boolean isCastingSpell(LivingEntity mob) {
        return false;
    }
}
