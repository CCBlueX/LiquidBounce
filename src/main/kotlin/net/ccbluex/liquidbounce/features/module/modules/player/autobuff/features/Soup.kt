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

package net.ccbluex.liquidbounce.features.module.modules.player.autobuff.features

import net.ccbluex.liquidbounce.config.types.group.Mode
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.config.types.group.NoneMode
import net.ccbluex.liquidbounce.event.events.ScheduleInventoryActionEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.waitTicks
import net.ccbluex.liquidbounce.features.module.modules.player.autobuff.HealthBasedBuff
import net.ccbluex.liquidbounce.utils.inventory.HotbarItemSlot
import net.ccbluex.liquidbounce.utils.inventory.InventoryAction
import net.ccbluex.liquidbounce.utils.inventory.PlayerInventoryConstraints
import net.ccbluex.liquidbounce.utils.inventory.hasInventorySpace
import net.ccbluex.liquidbounce.utils.inventory.useHotbarSlotOrOffhand
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

internal object Soup : HealthBasedBuff("Soup") {

    private val afterUse = modes<Mode>(this, "AfterUse", activeIndex = 1) {
        arrayOf(NoneMode(it), Drop, Stack)
    }

    private abstract class BowlMode(name: String) : Mode(name) {

        override val parent: ModeValueGroup<*>
            get() = afterUse

        private val assumeEmptyBowl by boolean("AssumeEmptyBowl", true)
        val wait by intRange("Wait", 1..2, 1..20, "ticks")

        fun holdsBowl(slot: HotbarItemSlot) =
            (assumeEmptyBowl || slot.itemStack.`is`(Items.BOWL)) && slot != HotbarItemSlot.OFFHAND

        abstract fun handleBowl(slot: HotbarItemSlot)

    }

    private object Drop : BowlMode("Drop") {

        override fun handleBowl(slot: HotbarItemSlot) {
            interaction.dropItem(player, true)
        }

    }

    /**
     * Shift-clicks the bowl out of the hotbar, so the bowls stack up in the inventory.
     */
    private object Stack : BowlMode("Stack") {

        private val ignoreInventoryFull by boolean("IgnoreInventoryFull", true)
        private val inventoryConstraints = tree(PlayerInventoryConstraints())

        private var bowlSlot: HotbarItemSlot? = null

        override fun handleBowl(slot: HotbarItemSlot) {
            bowlSlot = slot
        }

        @Suppress("unused")
        private val scheduleHandler = handler<ScheduleInventoryActionEvent> { event ->
            val slot = bowlSlot ?: return@handler
            bowlSlot = null

            if (!holdsBowl(slot) || !ignoreInventoryFull && !hasInventorySpace()) {
                return@handler
            }

            event.schedule(
                inventoryConstraints,
                InventoryAction.Click.performQuickMove(slot = slot),
                Priority.IMPORTANT_FOR_USAGE_2
            )
        }

        override fun disable() {
            bowlSlot = null
        }

    }

    override fun isValidItem(stack: ItemStack, forUse: Boolean): Boolean {
        return stack.`is`(Items.MUSHROOM_STEW)
    }

    override suspend fun execute(slot: HotbarItemSlot) {
        val mode = afterUse.activeMode as? BowlMode
        val ticksToWait = mode?.wait?.random() ?: 0

        // Use item (be aware, it will always return false in this case)
        useHotbarSlotOrOffhand(slot, ticksUntilReset = ticksToWait + 1)

        if (mode != null) {
            waitTicks(ticksToWait)

            if (mode.holdsBowl(slot)) {
                mode.handleBowl(slot)
            }
        }
    }

}
