package net.ccbluex.liquidbounce.utils.client.modmenu;

import com.terraformersmc.modmenu.ModMenu;
import com.terraformersmc.modmenu.util.mod.Mod;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public enum ModMenuCompatibility {
    INSTANCE;

    /**
     * SAFETY: The method doesn't check if {@link ModMenu} is present and loaded
     *
     * @param id the modid to remove
     * @return mod container in the {@link ModMenu} system
     */
    public final @Nullable Mod removeModUnchecked(@NotNull String id) {
        var mod = ModMenu.MODS.remove(id);
        var rootMod = ModMenu.ROOT_MODS.remove(id);

        return mod == null ? rootMod : mod;
    }

    /**
     * SAFETY: The method doesn't check if {@link ModMenu} is present and loaded
     *
     * @param id modid to associate the mod container
     * @param mod mod container in the {@link ModMenu} system to add
     */
    public final void addModUnchecked(@NotNull String id, @NotNull Mod mod) {
        ModMenu.MODS.put(id, mod);
        ModMenu.ROOT_MODS.put(id, mod);
    }
}
