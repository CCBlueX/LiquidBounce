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
package net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.trigger.triggers

import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.place.SubmoduleCrystalPlacer
import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.trigger.CrystalAuraTriggerer.player
import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.trigger.CrystalAuraTriggerer.runPlace
import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.trigger.CrystalAuraTriggerer.world
import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.trigger.PostPacketTrigger
import net.ccbluex.liquidbounce.interfaces.EntitiesDestroyS2CPacketAddition
import net.ccbluex.liquidbounce.utils.math.sq
import net.minecraft.entity.decoration.EndCrystalEntity
import net.minecraft.network.packet.s2c.play.EntitiesDestroyS2CPacket

/**
 * Runs placing when the information, that a crystal is removed is received.
 */
object CrystalDestroyTrigger : PostPacketTrigger<EntitiesDestroyS2CPacket>("CrystalDestroy", true) {

    @Suppress("unused")
    private val packetListener = handler<PacketEvent>(-1) { event ->
        val packet = event.packet
        if (packet !is EntitiesDestroyS2CPacket) {
            return@handler
        }

        val maxRangeSq = SubmoduleCrystalPlacer.getMaxRange().sq()
        val containsRelevantCrystal = packet.entityIds.any {
            val entity = world.getEntityById(it)

            // is the entity a crystal and in range?
            entity is EndCrystalEntity && entity.pos.squaredDistanceTo(player.pos) <= maxRangeSq
        }

        // mark the packet
        if (containsRelevantCrystal) {
            (packet as EntitiesDestroyS2CPacketAddition).`liquid_bounce$setContainsCrystal`()
        }
    }

    override fun postPacketHandler(packet: EntitiesDestroyS2CPacket) {
        val packetNotRelevant = !(packet as EntitiesDestroyS2CPacketAddition).`liquid_bounce$containsCrystal`()
        if (packetNotRelevant) {
            return
        }

        runPlace { SubmoduleCrystalPlacer.tick() }
    }

}
