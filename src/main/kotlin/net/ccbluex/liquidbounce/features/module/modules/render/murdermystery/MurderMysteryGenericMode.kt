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
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.utils.client.chat
import net.minecraft.client.network.AbstractClientPlayerEntity
import net.minecraft.item.BowItem
import net.minecraft.item.Items
import net.minecraft.util.Identifier

abstract class MurderMysteryGenericMode(name: String) : Choice(name), MurderMysteryMode {
    protected val bowSkins = HashSet<String>()
    protected val murdererSkins = HashSet<String>()

    /**
     * What is our current player doing? Is he murderer?
     */
    protected var currentPlayerType = MurderMysteryMode.PlayerType.NEUTRAL

    val repeatable =
        repeatable {
            currentPlayerType = player.handItems.firstNotNullOfOrNull {
                when {
                    it.item is BowItem || it.item == Items.ARROW -> MurderMysteryMode.PlayerType.DETECTIVE_LIKE
                    MurderMysterySwordDetection.isSword(it.item) -> MurderMysteryMode.PlayerType.MURDERER
                    else -> null
                }
            } ?: MurderMysteryMode.PlayerType.NEUTRAL
        }

    override fun reset() {
        this.bowSkins.clear()
        this.murdererSkins.clear()
    }

    override fun handleHasBow(
        entity: AbstractClientPlayerEntity,
        locationSkin: Identifier,
    ) {
        if (bowSkins.add(locationSkin.path)) {
            chat(entity.gameProfile.name + " has a bow.")

            ModuleMurderMystery.playBow = true
        }
    }

    override fun getPlayerType(player: AbstractClientPlayerEntity): MurderMysteryMode.PlayerType {
        return when (player.skinTextures.texture.path) {
            in murdererSkins -> MurderMysteryMode.PlayerType.MURDERER
            in bowSkins -> MurderMysteryMode.PlayerType.DETECTIVE_LIKE
            else -> MurderMysteryMode.PlayerType.NEUTRAL
        }
    }

    override fun shouldAttack(entity: AbstractClientPlayerEntity): Boolean {
        val targetPlayerType = getPlayerType(entity)

        return when (currentPlayerType) {
            MurderMysteryMode.PlayerType.MURDERER -> targetPlayerType != MurderMysteryMode.PlayerType.MURDERER
            else -> targetPlayerType == MurderMysteryMode.PlayerType.MURDERER
        }
    }
}
