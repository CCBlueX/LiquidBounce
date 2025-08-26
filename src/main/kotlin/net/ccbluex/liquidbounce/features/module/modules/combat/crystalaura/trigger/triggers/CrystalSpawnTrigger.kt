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

import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.destroy.SubmoduleCrystalDestroyer
import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.place.SubmoduleCrystalPlacer
import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.post.SubmoduleSetDead
import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.trigger.CrystalAuraTriggerer.runDestroy
import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.trigger.CrystalAuraTriggerer.runPlace
import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.trigger.CrystalAuraTriggerer.world
import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.trigger.PostPacketTrigger
import net.minecraft.entity.EntityType
import net.minecraft.entity.decoration.EndCrystalEntity
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket

/**
 * Runs destroying when the information, that a crystal is spawned is received.
 *
 * When Set-Dead is enabled, this will also run placing.
 */
object CrystalSpawnTrigger : PostPacketTrigger<EntitySpawnS2CPacket>("CrystalSpawn", true) {

    override fun postPacketHandler(packet: EntitySpawnS2CPacket) {
        if (packet.entityType != EntityType.END_CRYSTAL) {
            return
        }

        runDestroy {
            val entity = world.getEntityById(packet.entityId)
            if (entity !is EndCrystalEntity) {
                return@runDestroy
            }

            SubmoduleCrystalDestroyer.tick()
        }

        if (SubmoduleSetDead.enabled) {
            runPlace { SubmoduleCrystalPlacer.tick() }
        }
    }

}
