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
package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.render.drawOutlinedBox
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.render.withColor
import net.ccbluex.liquidbounce.render.withPositionRelativeToCamera
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsConfigurable
import net.ccbluex.liquidbounce.utils.aiming.raytraceBlock
import net.ccbluex.liquidbounce.utils.entity.eyes
import net.ccbluex.liquidbounce.utils.item.findHotbarSlot
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.math.toVec3d
import net.minecraft.block.BlockState
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.Direction

/**
 * CivBreak module
 *
 * Allows you to break the same block faster.
 */
object ModuleCivBreak : Module("CivBreak", Category.WORLD) {

    private val rotate by boolean("Rotate", false)
    private val ignoreOpenInventory by boolean("IgnoreOpenInventory", true)
    private val rotationsConfigurable = tree(RotationsConfigurable(this))
    private val switch by boolean("Switch", false)
    private val color by color("Color", Color4b(0, 100, 255))

    var pos: BlockPos? = null
    var dir: Direction? = null

    val repeatable = repeatable {
        if (pos == null || dir == null) {
            return@repeatable
        }

        // some blocks only break when holding a certain tool
        val oldSlot = player.inventory.selectedSlot
        val state = world.getBlockState(pos)
        var shouldSwitch = switch && state.isToolRequired
        if (shouldSwitch && ModuleAutoTool.enabled) {
            ModuleAutoTool.switchToBreakBlock(pos!!)
            shouldSwitch = false
        } else if (shouldSwitch) {
            val slot = findHotbarSlot { stack -> stack.isSuitableFor(state) } ?: -1
            if (slot != -1 && slot != oldSlot) {
                network.sendPacket(UpdateSelectedSlotC2SPacket(slot))
            } else {
                shouldSwitch = false
            }
        }

        rotateToTargetBlock(state)

        // Alright, for some reason when we spam STOP_DESTROY_BLOCK
        // server accepts us to destroy the same block instantly over and over.
        network.sendPacket(PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, pos, dir))

        if (shouldSwitch) {
            network.sendPacket(UpdateSelectedSlotC2SPacket(oldSlot))
        }
    }

    // some anti-cheats have interact checks
    private fun rotateToTargetBlock(state: BlockState) {
        if (!rotate) {
            return
        }

        val raytrace = raytraceBlock(
            player.eyes,
            pos!!,
            state,
            range = 25.0,
            wallsRange = 25.0
        )

        if (raytrace == null) {
            return // still send the packet, so we don't lose the block
        }

        val (rotation, _) = raytrace
        RotationManager.aimAt(
            rotation,
            considerInventory = !ignoreOpenInventory,
            configurable = rotationsConfigurable,
            Priority.IMPORTANT_FOR_USAGE_2,
            ModuleCivBreak
        )
    }

    // could (should!) listen directly for mouse clicks in the future to provide better compatibility with other modules
    val packetHandler = handler<PacketEvent> { event ->
        val packet = event.packet

        if (packet is PlayerActionC2SPacket && packet.action == PlayerActionC2SPacket.Action.START_DESTROY_BLOCK) {
            if (world.getBlockState(packet.pos)
                    .getHardness(
                        mc.world,
                        packet.pos
                    ) <= 0F || // mining unbreakable (-1) or instant breaking (0) blocks with this doesn't make sense
                packet.pos.equals(pos) // stop when the block is clicked again
            ) {
                pos = null
                dir = null
            } else {
                pos = packet.pos
                dir = packet.direction
            }
        }
    }

    // render

    private val fullBox = Box(0.0, 0.0, 0.0, 1.0, 1.0, 1.0)

    @Suppress("unused")
    val renderHandler = handler<WorldRenderEvent> { event ->
        val matrixStack = event.matrixStack
        if (pos == null || dir == null) return@handler

        renderEnvironmentForWorld(matrixStack) {
            withPositionRelativeToCamera(pos!!.toVec3d()) {
                withColor(color) {
                    drawOutlinedBox(fullBox)
                }
            }
        }
    }

    override fun disable() {
        pos = null
        dir = null
    }

}
