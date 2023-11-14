/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2023 CCBlueX
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
package net.ccbluex.liquidbounce.script.bindings.api

import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.combat.attack
import net.minecraft.entity.Entity
import net.minecraft.network.packet.c2s.*
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.util.Hand

object JsInteractionUtil {

    fun attackEntity(entity: Entity, swing: Boolean) {
        // Safety check
        if (entity == mc.player) {
            return
        }

        entity.attack(swing)
    }

    fun useEntity(entity: Entity, hand: Hand) {
        // Safety check
        if (entity == mc.player) {
            return
        }

        mc.interactionManager?.interactEntity(mc.player, entity, hand)
    }

    fun useItem(hand: Hand) {
        mc.interactionManager?.interactItem(mc.player, hand)
    }

}
