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
package net.ccbluex.liquidbounce.utils.block.bed

import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
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
import net.ccbluex.liquidbounce.utils.inventory.getArmorColor
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.FIRST_PRIORITY
import net.ccbluex.liquidbounce.utils.math.component1
import net.ccbluex.liquidbounce.utils.math.component2
import net.ccbluex.liquidbounce.utils.math.component3
import net.ccbluex.liquidbounce.utils.math.sq
import net.minecraft.block.BedBlock
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket
import net.minecraft.util.math.BlockPos
import org.joml.Vector3d
import org.lwjgl.glfw.GLFW

fun isSelfBedChoices(choice: ChoiceConfigurable<IsSelfBedChoice>): Array<IsSelfBedChoice> {
    return arrayOf(
        IsSelfBedChoice.None(choice),
        IsSelfBedChoice.Color(choice),
        IsSelfBedChoice.SpawnLocation(choice),
        IsSelfBedChoice.Manual(choice),
    )
}

sealed class IsSelfBedChoice(name: String, final override val parent: ChoiceConfigurable<*>) : Choice(name) {
    abstract fun isSelfBed(block: BedBlock, pos: BlockPos): Boolean
    open fun shouldDefend(block: BedBlock, pos: BlockPos): Boolean = isSelfBed(block, pos)

    class None(parent: ChoiceConfigurable<*>) : IsSelfBedChoice("None", parent) {
        override fun isSelfBed(block: BedBlock, pos: BlockPos) = false
        override fun shouldDefend(block: BedBlock, pos: BlockPos) = true
    }

    class Color(parent: ChoiceConfigurable<*>) : IsSelfBedChoice("Color", parent) {
        override fun isSelfBed(block: BedBlock, pos: BlockPos): Boolean {
            val color = block.color
            val colorRgb = color.entityColor
            val (_, armorColor) = getArmorColor() ?: return false

            return armorColor == colorRgb
        }
    }

    class SpawnLocation(parent: ChoiceConfigurable<*>) : IsSelfBedChoice("SpawnLocation", parent) {

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

            if (packet is PlayerPositionLookS2CPacket) {
                val pos = packet.change.position
                val distSq = player.pos.squaredDistanceTo(pos.x, pos.y, pos.z)

                if (distSq > 16.0 * 16.0) {
                    trackedSpawnLocation.set(pos.x, pos.y, pos.z)
                }
            }
        }

    }

    class Manual(parent: ChoiceConfigurable<*>) : IsSelfBedChoice("Manual", parent) {

        private val trackKey by key("Track", GLFW.GLFW_KEY_KP_ADD)
        private val untrackKey by key("Untrack", GLFW.GLFW_KEY_KP_SUBTRACT)

        private val trackedPos = BlockPos.Mutable()

        override fun disable() {
            trackedPos.set(BlockPos.ORIGIN)
            super.disable()
        }

        override fun isSelfBed(
            block: BedBlock,
            pos: BlockPos,
        ): Boolean = pos == trackedPos || pos.offset(pos.getState().anotherBedPartDirection()!!) == trackedPos

        @Suppress("unused")
        private val keyHandler = handler<KeyboardKeyEvent> { event ->
            if (event.action != GLFW.GLFW_PRESS) return@handler

            when (event.key) {
                trackKey -> {
                    val center = player.eyePos
                    val (bedPos, _) = center.searchBlocksInCuboid(16.0F) { _, state -> state.isBed }
                        .minByOrNull { it.first.getSquaredDistance(center) } ?: run {
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

                untrackKey if trackedPos != BlockPos.ORIGIN -> {
                    val (x, y, z) = trackedPos
                    notification(
                        title = "SelfBed-$name",
                        message = "Bed position ($x, $y, $z) has been untracked.",
                        NotificationEvent.Severity.INFO,
                    )
                    trackedPos.set(BlockPos.ORIGIN)
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
            trackedPos.set(BlockPos.ORIGIN)
        }

    }
}
