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
package net.ccbluex.liquidbounce.features.module.modules.world.fucker

import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.utils.inventory.getArmorColor
import net.ccbluex.liquidbounce.utils.math.toVec3d
import net.minecraft.block.BedBlock
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d

fun isSelfBedChoices(choice: ChoiceConfigurable<IsSelfBedChoice>): Array<IsSelfBedChoice> {
    return arrayOf(
        IsSelfBedNoneChoice(choice),
        IsSelfBedColorChoice(choice),
        IsSelfBedSpawnLocationChoice(choice)
    )
}

sealed class IsSelfBedChoice(name: String, override val parent: ChoiceConfigurable<*>) : Choice(name) {
    abstract fun isSelfBed(block: BedBlock, pos: BlockPos): Boolean
    open fun shouldDefend(block: BedBlock, pos: BlockPos): Boolean = isSelfBed(block, pos)
}

class IsSelfBedNoneChoice(parent: ChoiceConfigurable<*>) : IsSelfBedChoice("None", parent) {
    override fun isSelfBed(block: BedBlock, pos: BlockPos) = false
    override fun shouldDefend(block: BedBlock, pos: BlockPos) = true
}

class IsSelfBedSpawnLocationChoice(parent: ChoiceConfigurable<*>) : IsSelfBedChoice("SpawnLocation", parent) {

    private val bedDistance by float("BedDistance", 24.0f, 16.0f..48.0f)
    private var spawnLocation: Vec3d? = null

    override fun isSelfBed(block: BedBlock, pos: BlockPos) =
        spawnLocation?.isInRange(pos.toVec3d(), bedDistance.toDouble()) ?: false

    @Suppress("unused")
    private val gameStartHandler = handler<PacketEvent> {
        val packet = it.packet

        if (packet is PlayerPositionLookS2CPacket) {
            val pos = packet.change.position
            val packetPos = Vec3d(pos.x, pos.y, pos.z)
            val dist = player.pos.distanceTo(packetPos)

            if (dist > 16.0) {
                spawnLocation = packetPos
            }
        }
    }

}

class IsSelfBedColorChoice(parent: ChoiceConfigurable<*>) : IsSelfBedChoice("Color", parent) {
    override fun isSelfBed(block: BedBlock, pos: BlockPos): Boolean {
        val color = block.color
        val colorRgb = color.mapColor.color
        val (_, armorColor) = getArmorColor() ?: return false

        return armorColor == colorRgb
    }
}
