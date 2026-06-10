/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
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
 */
package net.ccbluex.liquidbounce.platform.fabric

import com.terraformersmc.modmenu.util.mod.Mod
import net.ccbluex.liquidbounce.platform.Platform
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.modmenu.ModMenuCompatibility
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab
import net.fabricmc.loader.api.FabricLoader
import net.fabricmc.loader.impl.FabricLoaderImpl
import net.minecraft.network.chat.Component
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.ItemStack
import java.util.function.Supplier

/**
 * [Platform] implementation for the Fabric loader.
 */
class FabricPlatform : Platform {

    override val loaderName = "fabric"

    private val modMenuPresent = runCatching {
        Class.forName("com.terraformersmc.modmenu.ModMenu")
        true
    }.getOrDefault(false)

    private val fabricApiCreativeTabPresent = runCatching {
        Class.forName("net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab")
        true
    }.getOrDefault(false)

    /**
     * Mods removed from ModMenu by [hideModsFromModList], with their [Mod] containers
     * so [restoreModsInModList] can add them back.
     *
     * Because we don't know about the [Mod] container of each mod in advance,
     * the value is provided after first removing the mod.
     */
    private val hiddenMods = mutableMapOf<String, Mod?>()

    override fun isModLoaded(id: String) = FabricLoader.getInstance().isModLoaded(id)

    override fun hideModsFromModList(ids: Collection<String>): Boolean {
        if (!modMenuPresent) {
            return false
        }

        for (id in ids) {
            hiddenMods[id] = ModMenuCompatibility.INSTANCE.removeModUnchecked(id)
        }

        return true
    }

    override fun restoreModsInModList(): Boolean {
        if (!modMenuPresent) {
            return false
        }

        for ((id, container) in hiddenMods) {
            container?.let {
                ModMenuCompatibility.INSTANCE.addModUnchecked(id, it)
            }
        }

        return true
    }

    override fun removeModAndDeleteJars(id: String): Boolean {
        val mod = FabricLoaderImpl.INSTANCE.allMods.find {
            it.metadata.id == id
        } ?: return false

        // Delete JAR file(s)
        runCatching {
            val origin = mod.origin

            for (path in origin.paths) {
                runCatching {
                    path.toFile().delete()
                }
            }
        }

        // Remove from Fabric Loader Impl
        runCatching {
            FabricLoaderImpl.INSTANCE.modsInternal.remove(mod)
        }

        return true
    }

    override fun buildCreativeTab(
        title: Component,
        icon: Supplier<ItemStack>,
        displayItems: CreativeModeTab.DisplayItemsGenerator
    ): CreativeModeTab? {
        // Check if FabricAPI is installed, otherwise we can't use the page buttons
        if (!fabricApiCreativeTabPresent) {
            logger.error("FabricAPI is not installed, please install it to use the page buttons " +
                "in the creative inventory")
            return null
        }

        return FabricCreativeModeTab.builder()
            .title(title)
            .icon(icon)
            .displayItems(displayItems)
            .build()
    }

}
