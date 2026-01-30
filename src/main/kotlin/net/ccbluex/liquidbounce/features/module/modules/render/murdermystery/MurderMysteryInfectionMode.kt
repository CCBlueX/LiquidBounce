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

package net.ccbluex.liquidbounce.features.module.modules.render.murdermystery

import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.entity.handItems
import net.minecraft.client.player.AbstractClientPlayer
import net.minecraft.resources.Identifier
import net.minecraft.world.item.BowItem
import net.minecraft.world.item.Items

object MurderMysteryInfectionMode : SkinBasedMurderMysteryMode("Infection") {

    val tickHandler = handler<GameTickEvent> {
        world.players()
            .filter {
                it.isUsingItem && player.handItems.any { stack -> stack.item is BowItem } ||
                    player.handItems.any { stack -> stack.item == Items.ARROW }
            }
            .forEach { playerEntity ->
                handleHasBow(playerEntity, playerEntity.skin.body.texturePath())
            }
    }

    override fun handleHasSword(
        entity: AbstractClientPlayer,
        locationSkin: Identifier,
    ) {
        if (murdererSkins.add(locationSkin.path) && murdererSkins.size == 1) {
            chat(entity.gameProfile.name + " is the first infected.")

            ModuleMurderMystery.playHurt = true
        }
    }

    override fun disallowsArrowDodge(): Boolean {
        // Don't dodge if we are not dead yet.
        return currentPlayerType == MurderMysteryMode.PlayerType.DETECTIVE_LIKE
    }

}
