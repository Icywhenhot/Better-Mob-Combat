package me.Thelnfamous1.bettermobcombat.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.Comment;

/**
 * Client-side, per-player display options. These are deliberately not synced from the server: they only change how
 * mobs are drawn on this client, never how much damage they deal or when they may attack, so each player can pick
 * their own preference without the server having to agree.
 */
@Config(
    name = "client"
)
public class BMCClientConfig implements ConfigData {

    // count must match the number of ".@Tooltip[n]" lang keys, or Cloth Config looks up a plain ".@Tooltip"
    // instead and the tooltip silently goes missing.
    @ConfigEntry.Gui.Tooltip(count = 4)
    @Comment("""
            Whether mobs play Better Combat's weapon animations.

            When enabled, a mob holding a Better Combat weapon uses that weapon's attack animation and idle stance.
            When disabled, mobs fall back to their vanilla arm swing and vanilla idle pose instead.

            This is a visual setting only - mob attack timing, reach and damage are unchanged either way.
            """)
    public boolean better_combat_mob_animations = true;

    public BMCClientConfig() {
    }
}
