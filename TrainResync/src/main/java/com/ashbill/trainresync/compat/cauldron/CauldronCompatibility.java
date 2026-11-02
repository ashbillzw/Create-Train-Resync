package com.ashbill.trainresync.compat.cauldron;

import net.minecraftforge.fml.loading.LoadingModList;

public final class CauldronCompatibility {
    // Containers, entity drops and comparator callbacks share types from these mods.
    public static boolean available() {
        var mods = LoadingModList.get();
        return mods.getModFileById("amendments") != null && mods.getModFileById("moonlight") != null
                && mods.getModFileById("natures_spirit") != null && mods.getModFileById("fruitsdelight") != null
                && mods.getModFileById("farmersdelight") != null;
    }
}
