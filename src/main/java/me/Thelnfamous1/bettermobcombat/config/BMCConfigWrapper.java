package me.Thelnfamous1.bettermobcombat.config;

import me.Thelnfamous1.bettermobcombat.Constants;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.autoconfig.serializer.PartitioningSerializer;

/**
 * Holds every config category. {@link PartitioningSerializer} writes each category to its own file under
 * {@code config/bettermobcombat/}, so {@code server} keeps living in {@code server.json5} exactly as before and
 * {@code client} gets its own new {@code client.json5}.
 */
@Config(
    name = Constants.MOD_ID
)
public class BMCConfigWrapper extends PartitioningSerializer.GlobalData {

    /**
     * The only category shown in the mod-list config screen. The server category stays excluded: it is the
     * server's authoritative copy and is pushed to clients on login, so anything edited here client-side would
     * just be overwritten by the next sync.
     */
    @ConfigEntry.Category("client")
    @ConfigEntry.Gui.TransitiveObject
    public BMCClientConfig client = new BMCClientConfig();

    @ConfigEntry.Category("server")
    @ConfigEntry.Gui.Excluded
    public BMCServerConfig server = new BMCServerConfig();

    public BMCConfigWrapper() {
    }
}
