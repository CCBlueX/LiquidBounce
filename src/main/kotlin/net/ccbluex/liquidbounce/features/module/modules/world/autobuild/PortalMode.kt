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
package net.ccbluex.liquidbounce.features.module.modules.world.autobuild

import net.ccbluex.liquidbounce.event.events.RotationUpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.utils.inventory.HotbarItemSlot
import net.ccbluex.liquidbounce.features.module.modules.world.autobuild.ModuleAutoBuild.placer
import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.markAsError
import net.ccbluex.liquidbounce.utils.inventory.Slots
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
        placer.update(portal!!.frameBlocks.filter { it.getState()!!.block != Blocks.OBSIDIAN }.toSet())
        placer.support.blockedPositions.addAll(portal!!.enclosedBlocks)
    }

    @Suppress("unused")
    private val targetUpdater = handler<RotationUpdateEvent> {
        if (!placer.isDone()) {
            return@handler
        }

        if (phase == Phase.BUILD) {
            val blocks = portal!!.confirmPlacements()
            if (blocks.isNotEmpty()) {
                placer.update(blocks.toSet())
                return@handler
            }

            phase = Phase.IGNITE
            placer.addToQueue(portal!!.ignitePos)
        } else if (phase == Phase.IGNITE) {
            ModuleAutoBuild.enabled = false
        }
    }

    override fun disabled() {
        placer.support.blockedPositions.clear()
        portal = null
    }

    @Suppress("NestedBlockDepth", "CognitiveComplexMethod")
    private fun getPortal(): NetherPortal? {
        var result: NetherPortal? = null
        val pos = BlockPos.ofFloored(player.pos)
        for (direction in Direction.HORIZONTAL) {
            for (yOffset in -1..0) {
                for (dirOffset in 0 downTo  -1) {
                    val portalOrigin = pos.mutableCopy().move(direction)
                    val rotated = direction.rotateYClockwise()
                    if (dirOffset == -1) {
                        portalOrigin.move(rotated.opposite)
                    }
                    if (yOffset == -1) {
                        portalOrigin.move(Direction.DOWN)
                    }

                    val portal = NetherPortal(portalOrigin, yOffset == -1, direction, rotated)
                    portal.calculateScore()
                    if (!portal.isValid()) continue

                    if (result == null || result.score < portal.score) {
                        result = portal
                    }
                }
            }
        }

        return result
    }

    override fun getSlot(): HotbarItemSlot? {
        for (it in Slots.OffhandWithHotbar) {
            val item = it.itemStack.item
            if (phase == Phase.IGNITE) {
                if (item == Items.FLINT_AND_STEEL) {
                    return it
                }

                continue
            }

            // build phase...

            if (item !is BlockItem) {
                continue
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
