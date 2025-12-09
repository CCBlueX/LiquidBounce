/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 *
 */

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
