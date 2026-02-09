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
package net.ccbluex.liquidbounce.utils.block.bed

import net.ccbluex.fastutil.enumSetOf
import net.ccbluex.liquidbounce.config.types.group.Mode
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.event.events.KeyboardKeyEvent
import net.ccbluex.liquidbounce.event.events.NotificationEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.utils.block.anotherBedPartDirection
import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.block.isBed
import net.ccbluex.liquidbounce.utils.block.searchBlocksInCuboid
import net.ccbluex.liquidbounce.utils.client.notification
import net.ccbluex.liquidbounce.utils.inventory.EquipmentSlotChoice
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.FIRST_PRIORITY
import net.ccbluex.liquidbounce.utils.math.component1
import net.ccbluex.liquidbounce.utils.math.component2
import net.ccbluex.liquidbounce.utils.math.component3
import net.ccbluex.liquidbounce.utils.math.sq
import net.minecraft.core.BlockPos
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket
import net.minecraft.world.level.block.BedBlock
import org.joml.Vector3d
import org.lwjgl.glfw.GLFW

fun isSelfBedChoices(choice: ModeValueGroup<IsSelfBedMode>): Array<IsSelfBedMode> {
    return arrayOf(
        IsSelfBedMode.None(choice),
        IsSelfBedMode.Color(choice),
        IsSelfBedMode.SpawnLocation(choice),
        IsSelfBedMode.Manual(choice),
    )
}

sealed class IsSelfBedMode(name: String, final override val parent: ModeValueGroup<*>) : Mode(name) {
    abstract fun isSelfBed(block: BedBlock, pos: BlockPos): Boolean
    open fun shouldDefend(block: BedBlock, pos: BlockPos): Boolean = isSelfBed(block, pos)

    class None(parent: ModeValueGroup<*>) : IsSelfBedMode("None", parent) {
        override fun isSelfBed(block: BedBlock, pos: BlockPos) = false
        override fun shouldDefend(block: BedBlock, pos: BlockPos) = true
    }

    class Color(parent: ModeValueGroup<*>) : IsSelfBedMode("Color", parent) {

        private val slots by multiEnumChoice(
            "Slots",
            enumSetOf(EquipmentSlotChoice.HEAD),
            EquipmentSlotChoice.allHumanoidArmor(),
            canBeNone = false,
        )

        override fun isSelfBed(block: BedBlock, pos: BlockPos): Boolean {
            val color = block.color
            val colorRgb = color.textureDiffuseColor
            return slots.any { it.getArmorColor(player) == colorRgb }
        }
    }

    class SpawnLocation(parent: ModeValueGroup<*>) : IsSelfBedMode("SpawnLocation", parent) {

        private val bedDistance by float("BedDistance", 24.0f, 16.0f..48.0f)
        private val trackedSpawnLocation = Vector3d(Double.MAX_VALUE)

        override fun isSelfBed(block: BedBlock, pos: BlockPos) =
            trackedSpawnLocation.distanceSquared(
                pos.x.toDouble(),
                pos.y.toDouble(),
                pos.z.toDouble(),
            ) <= bedDistance.sq()

        override fun disable() {
            trackedSpawnLocation.set(Double.MAX_VALUE)
            super.disable()
        }

        @Suppress("unused")
        private val gameStartHandler = handler<PacketEvent>(FIRST_PRIORITY) {
            val packet = it.packet

            if (packet is ClientboundPlayerPositionPacket) {
                val pos = packet.change.position
                val distSq = player.position().distanceToSqr(pos.x, pos.y, pos.z)

                if (distSq > 16.0 * 16.0) {
                    trackedSpawnLocation.set(pos.x, pos.y, pos.z)
                }
            }
        }

    }

    class Manual(parent: ModeValueGroup<*>) : IsSelfBedMode("Manual", parent) {

        private val trackKey by key("Track", GLFW.GLFW_KEY_KP_ADD)
        private val untrackKey by key("Untrack", GLFW.GLFW_KEY_KP_SUBTRACT)

        private val trackedPos = BlockPos.MutableBlockPos()

        override fun disable() {
            trackedPos.set(BlockPos.ZERO)
            super.disable()
        }

        override fun isSelfBed(
            block: BedBlock,
            pos: BlockPos,
        ): Boolean = pos == trackedPos || pos.relative(pos.getState().anotherBedPartDirection()!!) == trackedPos

        @Suppress("unused")
        private val keyHandler = handler<KeyboardKeyEvent> { event ->
            if (event.action != GLFW.GLFW_PRESS) return@handler

            when (event.key) {
                trackKey -> {
                    val center = player.eyePosition
                    val (bedPos, _) = center.searchBlocksInCuboid(16.0F) { _, state -> state.isBed }
                        .minByOrNull { it.first.distToCenterSqr(center) } ?: run {
                        notification(
                            title = "SelfBed-$name",
                            message = "Cannot find any bed around you! Please get close to your bed.",
                            NotificationEvent.Severity.ERROR,
                        )
                        return@handler
                    }

                    trackedPos.set(bedPos)
                    val (x, y, z) = bedPos
                    notification(
                        title = "SelfBed-$name",
                        message = "Tracked bed position ($x, $y, $z).",
                        NotificationEvent.Severity.SUCCESS,
                    )
                }

                untrackKey if trackedPos != BlockPos.ZERO -> {
                    val (x, y, z) = trackedPos
                    notification(
                        title = "SelfBed-$name",
                        message = "Bed position ($x, $y, $z) has been untracked.",
                        NotificationEvent.Severity.INFO,
                    )
                    trackedPos.set(BlockPos.ZERO)
                }
            }
        }

        @Suppress("unused")
        private val worldHandler = handler<WorldChangeEvent> {
            val (x, y, z) = trackedPos
            notification(
                title = "SelfBed-$name",
                message = "Bed position ($x, $y, $z) has been untracked due to world change.",
                NotificationEvent.Severity.INFO,
            )
            trackedPos.set(BlockPos.ZERO)
        }

    }
}
