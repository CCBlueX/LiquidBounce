/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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
package net.ccbluex.liquidbounce.features.module.modules.world.autobuild

import net.ccbluex.liquidbounce.event.events.SimulatedTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.HotbarItemSlot
import net.ccbluex.liquidbounce.features.module.modules.world.autobuild.ModuleAutoBuild.placer
import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.client.Filter
import net.ccbluex.liquidbounce.utils.inventory.HOTBAR_SLOTS
import net.ccbluex.liquidbounce.utils.item.getBlock
import net.minecraft.block.Blocks
import net.minecraft.util.math.BlockPos

object PlatformMode : ModuleAutoBuild.AutoBuildMode("Platform") {

    private val filter by enumChoice("Filter", Filter.WHITELIST)
    private val blocks by blocks("Blocks", mutableSetOf(Blocks.OBSIDIAN))
    private val platformSize by int("Size", 3, 1..6)

    private var startY = 0.0

    override fun enabled() {
        startY = player.pos.y
    }

    @Suppress("unused")
    private val repeatable = repeatable {
        if (player.pos.y != startY) {
            ModuleAutoBuild.enabled = false
        }
    }

    @Suppress("unused")
    private val targetUpdater = handler<SimulatedTickEvent> {
        val blocks1 = mutableSetOf<BlockPos>()
        val center = BlockPos.ofFloored(player.pos).down()
        for (x in center.x - platformSize..center.x + platformSize) {
            for (z in center.z - platformSize..center.z + platformSize) {
                val pos = BlockPos(x, center.y, z)
                if (pos.getState()!!.isReplaceable) {
                    blocks1.add(pos)
                }
            }
        }

        placer.update(blocks1)
    }

    override fun getSlot(): HotbarItemSlot? {
        HOTBAR_SLOTS.forEach {
            val block = it.itemStack.getBlock() ?: return@forEach
            if (filter(block, blocks)) {
                return it
            }
        }

        return null
    }

}
