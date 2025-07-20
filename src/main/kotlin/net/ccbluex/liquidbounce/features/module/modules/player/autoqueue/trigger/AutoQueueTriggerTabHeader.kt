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
 *
 *
 */

package net.ccbluex.liquidbounce.features.module.modules.player.autoqueue.trigger

import net.ccbluex.liquidbounce.utils.client.convertToString

/**
 * Can be used for different server that use paper to join a game
 */
object AutoQueueTriggerTabHeader : AutoQueueTrigger("TabHeader") {

    private val text by text("Text", "Duels")

    override val isTriggered: Boolean
        get() {
            val playerListHeader = mc.inGameHud?.playerListHud?.header ?: return false
            return playerListHeader.convertToString().contains(text)
        }

}
