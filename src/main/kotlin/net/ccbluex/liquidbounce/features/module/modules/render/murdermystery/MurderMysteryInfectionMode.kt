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
 *
 */
package net.ccbluex.liquidbounce.features.module.modules.render.murdermystery

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.utils.client.chat
import net.minecraft.client.network.AbstractClientPlayerEntity
import net.minecraft.item.BowItem
import net.minecraft.item.Items
import net.minecraft.util.Identifier

object MurderMysteryInfectionMode : MurderMysteryGenericMode("Infection") {
    override val parent: ChoiceConfigurable<Choice>
        get() = ModuleMurderMystery.modes

    val rep =
        repeatable {
            world.players
                .filterIsInstance<AbstractClientPlayerEntity>()
                .filter {
                    it.isUsingItem && player.handItems.any { stack -> stack.item is BowItem } ||
                        player.handItems.any { stack -> stack.item == Items.ARROW }
                }
                .forEach { playerEntity ->
                    handleHasBow(playerEntity, playerEntity.skinTextures.texture)
                }
        }

    override fun handleHasSword(
        entity: AbstractClientPlayerEntity,
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
