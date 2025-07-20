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

import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.place.SubmoduleCrystalPlacer
import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.trigger.CrystalAuraTriggerer.player
import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.trigger.CrystalAuraTriggerer.runPlace
import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.trigger.CrystalAuraTriggerer.world
import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.trigger.PostPacketTrigger
import net.ccbluex.liquidbounce.utils.math.sq
import net.minecraft.network.packet.s2c.play.PlaySoundFromEntityS2CPacket
import net.minecraft.sound.SoundEvents

/**
 * Runs placing when an explosion sound is received.
 */
object ExplodeSoundTrigger : PostPacketTrigger<PlaySoundFromEntityS2CPacket>("ExplodeSound", true) {

    override fun postPacketHandler(packet: PlaySoundFromEntityS2CPacket) {
        if (packet.sound != SoundEvents.ENTITY_GENERIC_EXPLODE) {
            return
        }

        world.getEntityById(packet.entityId)?.let {
            // don't place if the sound is too far away
            val maxRangeSq = SubmoduleCrystalPlacer.getMaxRange().sq()
            if (it.pos.squaredDistanceTo(player.pos) > maxRangeSq) {
                return
            }
        }

        runPlace { SubmoduleCrystalPlacer.tick() }
    }

}
