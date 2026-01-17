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

import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.entity.handItems
import net.minecraft.client.player.AbstractClientPlayer
import net.minecraft.resources.Identifier
import net.minecraft.world.item.BowItem
import net.minecraft.world.item.Items

sealed class SkinBasedMurderMysteryMode(name: String) : MurderMysteryMode(name) {

    protected val bowSkins = HashSet<String>()
    protected val murdererSkins = HashSet<String>()

    /**
     * What is our current player doing? Is he murderer?
     */
    protected var currentPlayerType = PlayerType.NEUTRAL

    val repeatable =
        tickHandler {
            currentPlayerType = player.handItems.firstNotNullOfOrNull {
                when {
                    it.item is BowItem || it.item == Items.ARROW -> PlayerType.DETECTIVE_LIKE
                    MurderMysterySwordDetection.isSword(it) -> PlayerType.MURDERER
                    else -> null
                }
            } ?: PlayerType.NEUTRAL
        }

    override fun reset() {
        this.bowSkins.clear()
        this.murdererSkins.clear()
    }

    override fun handleHasBow(
        entity: AbstractClientPlayer,
        locationSkin: Identifier,
    ) {
        if (bowSkins.add(locationSkin.path)) {
            chat(entity.gameProfile.name + " has a bow.")

            ModuleMurderMystery.playBow = true
        }
    }

    override fun getPlayerType(player: AbstractClientPlayer): PlayerType {
        return when (player.skin.body.texturePath().path) {
            in murdererSkins -> PlayerType.MURDERER
            in bowSkins -> PlayerType.DETECTIVE_LIKE
            else -> PlayerType.NEUTRAL
        }
    }

    override fun shouldAttack(entity: AbstractClientPlayer): Boolean {
        val targetPlayerType = getPlayerType(entity)

        return when (currentPlayerType) {
            PlayerType.MURDERER -> targetPlayerType != PlayerType.MURDERER
            else -> targetPlayerType == PlayerType.MURDERER
        }
    }
}
