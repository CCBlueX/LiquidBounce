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
 */
package net.ccbluex.liquidbounce.features.itemgroup

import com.mojang.blaze3d.systems.RenderSystem
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.types.nesting.Configurable
import net.ccbluex.liquidbounce.features.itemgroup.groups.ContainerItemGroup
import net.ccbluex.liquidbounce.features.itemgroup.groups.ExploitsItemGroup
import net.ccbluex.liquidbounce.features.itemgroup.groups.HeadsItemGroup
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.item.createItem
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound

/**
 * LiquidBounce Creative Item Groups with useful items and blocks
 *
 * @author kawaiinekololis (@team CCBlueX)
 * @depends FabricAPI (for page buttons)
 */
object ClientItemGroups : Configurable("tabs") {

    private var beenSetup = false
    val containers by textList("Containers", mutableListOf())

    fun storeAsContainerItem(compound: NbtCompound) {
        val compoundString = compound.toString()

        if (compoundString in containers) {
            error("container is already stored")
        }

        containers.add(compoundString)
        ConfigSystem.storeConfigurable(this)

        RenderSystem.recordRenderCall {
            chat("§aAdded container to creative inventory")
        }
    }

    fun containersAsItemStacks(): List<ItemStack> {
        return containers.map { createItem("minecraft:chest$it") }
    }

    fun clearContainers() {
        containers.clear()
        ConfigSystem.storeConfigurable(this)
    }

    fun removeContainer(index: Int) {
        containers.removeAt(index)
        ConfigSystem.storeConfigurable(this)
    }

    /**
     * Since 1.20 we need to set this up at a more precise timing than just when the client starts.
     */
    fun setup() {
        if (beenSetup) {
            return
        }

        // Check if FabricAPI is installed, otherwise we can't use the page buttons
        // Use net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup
        runCatching {
            Class.forName("net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup")
        }.onFailure {
            logger.error("FabricAPI is not installed, please install it to use the page buttons " +
                "in the creative inventory")
        }.onSuccess {
            runCatching {
                // Create item groups
                val itemGroups = arrayOf(
                    HeadsItemGroup(),
                    ExploitsItemGroup(),
                    ContainerItemGroup(),
                )

                for (itemGroup in itemGroups) {
                    itemGroup.setup()
                }

                beenSetup = true
                itemGroups
            }.onFailure { exception ->
                logger.error("Unable to setup item groups", exception)
            }.onSuccess { itemGroups ->
                logger.info("Item Groups: [ ${itemGroups.joinToString { group -> group.plainName }} ]")
            }
        }
    }

}
