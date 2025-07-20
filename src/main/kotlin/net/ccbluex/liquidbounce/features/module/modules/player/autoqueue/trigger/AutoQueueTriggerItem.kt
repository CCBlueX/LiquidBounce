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
import net.ccbluex.liquidbounce.utils.inventory.Slots

/**
 * Can be used for different server that use paper to join a game
 */
object AutoQueueTriggerItem : AutoQueueTrigger("Item") {

    /**
     * The [itemName] of the item when to trigger the queue,
     * which can be a different item than we use in the [AutoQueueTriggerItem] action.
     *
     * The name also can be a custom name of the item and does not have to be matching,
     * and only contains the text.
     */
    private val itemName by text("Name", "Paper")

    override val isTriggered: Boolean
        get() = Slots.Hotbar.findSlot { itemStack ->
            itemStack.name.convertToString().contains(itemName)
        } != null

}
