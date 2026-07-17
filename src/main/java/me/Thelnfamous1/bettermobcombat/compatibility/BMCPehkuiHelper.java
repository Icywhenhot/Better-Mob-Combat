package me.Thelnfamous1.bettermobcombat.compatibility;

/**
 * Pehkui compatibility hook.
 *
 * Better Combat 2.x (1.21) removed its internal {@code net.bettercombat.compatibility.PehkuiHelper},
 * and Pehkui is not a dependency of this NeoForge port, so this integration is currently a no-op.
 * The hook is retained so it can be re-implemented if Pehkui support is added back later.
 */
public class BMCPehkuiHelper {

    public BMCPehkuiHelper() {
    }

    public static void load() {
        // No-op: Pehkui attack-range scaling is not wired on this NeoForge 1.21.1 port.
    }
}
