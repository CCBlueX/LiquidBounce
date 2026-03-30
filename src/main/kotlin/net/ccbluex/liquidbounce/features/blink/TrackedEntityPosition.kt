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

package net.ccbluex.liquidbounce.features.blink

import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientboundEntityPositionSyncPacket
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket
import net.minecraft.network.protocol.game.VecDeltaCodec
import net.minecraft.world.entity.Entity
import net.minecraft.world.phys.Vec3

/**
 * Tracks delayed entity positions from vanilla movement packets.
 *
 * @see net.minecraft.network.protocol.game.ClientboundMoveEntityPacket
 * @see net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket
 * @see net.minecraft.network.protocol.game.ClientboundEntityPositionSyncPacket
 * @see net.minecraft.network.protocol.game.VecDeltaCodec
 */
class TrackedEntityPosition(initialPos: Vec3 = Vec3.ZERO) {

    constructor(entity: Entity) : this(entity.positionCodec.base)

    private val codec = VecDeltaCodec().apply {
        setBase(initialPos)
    }

    var base: Vec3
        get() = codec.base
        set(value) {
            codec.setBase(value)
        }

    fun setBaseFrom(entity: Entity) {
        codec.setBase(entity.positionCodec.base)
    }

    fun handlePacket(packet: Packet<*>, level: ClientLevel, target: Entity): Vec3? {
        val trackedPos = when (packet) {
            is ClientboundMoveEntityPacket if packet.getEntity(level) == target ->
                codec.decode(packet.xa.toLong(), packet.ya.toLong(), packet.za.toLong())

            is ClientboundTeleportEntityPacket if packet.id == target.id ->
                packet.change.position

            is ClientboundEntityPositionSyncPacket if packet.id == target.id ->
                packet.values.position

            else -> return null
        } ?: return null

        base = trackedPos
        return trackedPos
    }

}
