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
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.HotbarItemSlot
import net.ccbluex.liquidbounce.features.module.modules.world.autobuild.ModuleAutoBuild.placer
import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.markAsError
import net.ccbluex.liquidbounce.utils.inventory.HOTBAR_SLOTS
import net.minecraft.block.Blocks
import net.minecraft.item.BlockItem
import net.minecraft.item.Items
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction

object PortalMode : ModuleAutoBuild.AutoBuildMode("Portal") {

    private var phase = Phase.BUILD
    private var portal: NetherPortal? = null

    override fun enabled() {
        phase = Phase.BUILD
        portal = getPortal()
        if (portal == null) {
            chat(markAsError(ModuleAutoBuild.message("noPosition")), ModuleAutoBuild)
            ModuleAutoBuild.enabled = false
        }
        placer.update(portal!!.blocks.filter { it.getState()!!.block != Blocks.OBSIDIAN }.toSet())
    }

    @Suppress("unused")
    private val targetUpdater = handler<SimulatedTickEvent> {
        if (!placer.isDone()) {
            return@handler
        }

        if (phase == Phase.BUILD) {
            phase = Phase.IGNITE
            placer.addToQueue(portal!!.ignitePos)
        } else if (phase == Phase.IGNITE) {
            ModuleAutoBuild.enabled = false
        }
    }

    override fun disabled() {
        portal = null
    }

    private fun getPortal(): NetherPortal? {
        val portals = mutableListOf<NetherPortal>()
        val pos = BlockPos.ofFloored(player.pos)
        for (direction in Direction.HORIZONTAL) {
            for (yOffset in -1 until 1) {
                for (dirOffset in 0 downTo  -1) {
                    var portalOrigin = pos.offset(direction)
                    val rotated = direction.rotateYClockwise()
                    if (dirOffset == -1) {
                        portalOrigin = portalOrigin.offset(rotated.opposite)
                    }
                    if (yOffset == -1) {
                        portalOrigin = portalOrigin.down()
                    }

                    val portal = NetherPortal(portalOrigin, yOffset == -1, direction, rotated)
                    portal.calculateScore()
                    portals.add(portal)
                }
            }
        }

        return portals.filter { it.isValid() }.maxByOrNull { it.score }
    }

    override fun getSlot(): HotbarItemSlot? {
        HOTBAR_SLOTS.forEach {
            val item = it.itemStack.item
            if (phase == Phase.IGNITE) {
                if (item == Items.FLINT_AND_STEEL) {
                    return it
                }

                return@forEach
            }

            // build phase...

            if (item !is BlockItem) {
                return@forEach
            }

            if (item.block == Blocks.OBSIDIAN) {
                return it
            }
        }

        if (phase == Phase.IGNITE) {
            chat(markAsError(ModuleAutoBuild.message("noFlintAndSteel")), ModuleAutoBuild)
        } else {
            chat(markAsError(ModuleAutoBuild.message("noObsidian")), ModuleAutoBuild)
        }
        ModuleAutoBuild.enabled = false
        return null
    }

    enum class Phase {
        BUILD,
        IGNITE
    }

}
