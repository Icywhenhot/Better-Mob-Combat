package me.Thelnfamous1.bettermobcombat.compatibility;

import me.Thelnfamous1.bettermobcombat.platform.BMCPlatform;

public class BMCCompatibilityFlags {
    private static boolean geckolibLoaded;

    public static void initialize() {
        if (BMCPlatform.isModLoaded("pehkui")) {
            BMCPehkuiHelper.load();
        }
        if(BMCPlatform.isModLoaded("geckolib")){
            geckolibLoaded = true;
        }
    }

    public static boolean isGeckolibLoaded() {
        return geckolibLoaded;
    }
}