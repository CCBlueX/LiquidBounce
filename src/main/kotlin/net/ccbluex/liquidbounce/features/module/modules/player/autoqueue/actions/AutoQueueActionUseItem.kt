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

package net.ccbluex.liquidbounce.features.module.modules.player.autoqueue.actions

import net.ccbluex.liquidbounce.event.waitTicks
import net.ccbluex.liquidbounce.features.module.modules.player.autoqueue.ModuleAutoQueue
import net.ccbluex.liquidbounce.utils.client.SilentHotbar
import net.ccbluex.liquidbounce.utils.inventory.SingleItemStackPickMode
import net.ccbluex.liquidbounce.utils.inventory.Slots

object AutoQueueActionUseItem : AutoQueueAction("UseItem") {

    private val mode = modes("Mode", 0) {
        arrayOf(SingleItemStackPickMode.ByName(it), SingleItemStackPickMode.ByItem(it))
    }

    override suspend fun execute() {
        val slot = Slots.OffhandWithHotbar.findSlot(mode.activeMode::test) ?: return

        SilentHotbar.selectSlotSilently(ModuleAutoQueue, slot, 20)
        waitTicks(1)
        interaction.useItem(player, slot.useHand)
    }

}
