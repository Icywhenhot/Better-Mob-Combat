package me.Thelnfamous1.bettermobcombat;

import me.Thelnfamous1.bettermobcombat.config.BMCConfigWrapper;
import me.Thelnfamous1.bettermobcombat.network.BMCNetwork;
import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@Mod(Constants.MOD_ID)
public class BetterMobCombatNeoForge {

    public BetterMobCombatNeoForge(IEventBus modBus, ModContainer modContainer) {
        // Bootstrap the shared mod logic.
        BetterMobCombat.init();
        if (FMLEnvironment.dist == Dist.CLIENT) {
            BetterMobCombatClient.init();

            // Wire the Cloth AutoConfig screen into NeoForge's mod-list "Config" button. Client-only: the screen
            // classes do not exist on a dedicated server.
            modContainer.registerExtensionPoint(IConfigScreenFactory.class,
                    (container, modListScreen) -> AutoConfig.getConfigScreen(BMCConfigWrapper.class, modListScreen).get());
        }

        // Register the S2C payloads on the mod event bus.
        modBus.addListener(BMCNetwork::register);

        // Sync the server config to players when they log in.
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer serverPlayer) {
                BMCNetwork.syncServerConfig(serverPlayer);
            }
        });
    }
}
