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

import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.event.events.ScheduleInventoryActionEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.waitTicks
import net.ccbluex.liquidbounce.features.module.modules.player.autobuff.HealthBasedBuff
import net.ccbluex.liquidbounce.features.module.modules.player.autobuff.features.Soup.DropAfterUse.assumeEmptyBowl
import net.ccbluex.liquidbounce.features.module.modules.player.autobuff.features.Soup.DropAfterUse.wait
import net.ccbluex.liquidbounce.utils.inventory.HotbarItemSlot
import net.ccbluex.liquidbounce.utils.inventory.InventoryAction
import net.ccbluex.liquidbounce.utils.inventory.OffHandSlot
import net.ccbluex.liquidbounce.utils.inventory.PlayerInventoryConstraints
import net.ccbluex.liquidbounce.utils.inventory.hasInventorySpace
import net.ccbluex.liquidbounce.utils.inventory.useHotbarSlotOrOffhand
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

internal object Soup : HealthBasedBuff("Soup") {

    private object DropAfterUse : ToggleableValueGroup(this, "DropAfterUse", true) {
        val assumeEmptyBowl by boolean("AssumeEmptyBowl", true)
        val wait by intRange("Wait", 1..2, 1..20, "ticks")
    }

    private object SoupStacker : ToggleableValueGroup(this, "SoupStacker", false) {
        val assumeEmptyBowl by boolean("AssumeEmptyBowl", true)
        val wait by intRange("Wait", 1..2, 1..20, "ticks")
        val ignoreInventoryFull by boolean("IgnoreInventoryFull", true)
        val inventoryConstraints = tree(PlayerInventoryConstraints())
    }

    private var pendingBowlSlot: HotbarItemSlot? = null

    init {
        tree(DropAfterUse)
        tree(SoupStacker)
    }

    @Suppress("unused")
    private val stackerHandler = handler<ScheduleInventoryActionEvent> { event ->
        val slot = pendingBowlSlot ?: return@handler
        pendingBowlSlot = null

        if (slot is OffHandSlot) return@handler

        val shouldStack = SoupStacker.assumeEmptyBowl || slot.itemStack.`is`(Items.BOWL)
        if (!shouldStack) return@handler

        if (!SoupStacker.ignoreInventoryFull && !hasInventorySpace()) return@handler

        event.schedule(
            SoupStacker.inventoryConstraints,
            InventoryAction.Click.performQuickMove(screen = null, slot = slot),
            Priority.IMPORTANT_FOR_USAGE_2
        )
    }

    override fun isValidItem(stack: ItemStack, forUse: Boolean): Boolean {
        return stack.`is`(Items.MUSHROOM_STEW)
    }

    override suspend fun execute(slot: HotbarItemSlot) {
        useHotbarSlotOrOffhand(slot)

        when {
            DropAfterUse.enabled -> handleDropAfterUse(slot)
            SoupStacker.enabled -> handleSoupStacker(slot)
        }
    }

    private suspend fun handleDropAfterUse(slot: HotbarItemSlot) {
        waitTicks(wait.random())

        if (assumeEmptyBowl || slot.itemStack.`is`(Items.BOWL) && slot !is OffHandSlot) {
            if (player.drop(true)) {
                player.swing(InteractionHand.MAIN_HAND)
            }
        }
    }

    private suspend fun handleSoupStacker(slot: HotbarItemSlot) {
        waitTicks(SoupStacker.wait.random())
        pendingBowlSlot = slot
    }


}
