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
 */
package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.BlockBreakingProgressEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.utils.inventory.HotbarItemSlot
import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.client.SilentHotbar
import net.ccbluex.liquidbounce.utils.collection.Filter
import net.ccbluex.liquidbounce.utils.inventory.ItemSlot
import net.ccbluex.liquidbounce.utils.inventory.SlotGroup
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.ccbluex.liquidbounce.utils.item.isNothing
import net.minecraft.block.BlockState
import net.minecraft.util.math.BlockPos

/**
 * AutoTool module
 *
 * Automatically chooses the best tool in your inventory to mine a block.
 */
object ModuleAutoTool : ClientModule("AutoTool", Category.WORLD) {
    val toolSelector =
        choices(
            "ToolSelector",
            DynamicSelectMode,
            arrayOf(DynamicSelectMode, StaticSelectMode)
        )

    private val filter by enumChoice("Filter", Filter.BLACKLIST)
    private val blocks by blocks("Blocks", hashSetOf())

    sealed class ToolSelectorMode(name: String) : Choice(name) {
        fun getTool(blockState: BlockState): HotbarItemSlot? =
            if (filter(blockState.block, blocks)) {
                getToolSlot(blockState)
            } else {
                null
            }

        protected abstract fun getToolSlot(blockState: BlockState): HotbarItemSlot?
    }

    private object DynamicSelectMode : ToolSelectorMode("Dynamic") {
        override val parent: ChoiceConfigurable<*>
            get() = toolSelector

        private val ignoreDurability by boolean("IgnoreDurability", false)

        override fun getToolSlot(blockState: BlockState) =
            Slots.Hotbar.findBestToolToMineBlock(blockState, ignoreDurability)
    }

    private object StaticSelectMode : ToolSelectorMode("Static") {
        override val parent: ChoiceConfigurable<*>
            get() = toolSelector

        private val slot by int("Slot", 0, 0..8)

        override fun getToolSlot(blockState: BlockState) = Slots.Hotbar[slot]
    }

    private val swapPreviousDelay by int("SwapPreviousDelay", 20, 1..100, "ticks")

    private val requireSneaking by boolean("RequireSneaking", false)

    @Suppress("unused")
    private val handleBlockBreakingProgress = handler<BlockBreakingProgressEvent> { event ->
        switchToBreakBlock(event.pos)
    }

    fun switchToBreakBlock(pos: BlockPos) {
        if (requireSneaking && !player.isSneaking) {
            return
        }

        val blockState = pos.getState()!!
        val slot = toolSelector.activeChoice.getTool(blockState) ?: return
        SilentHotbar.selectSlotSilently(this, slot, swapPreviousDelay)
    }

    fun <T : ItemSlot> SlotGroup<T>.findBestToolToMineBlock(
        blockState: BlockState,
        ignoreDurability: Boolean = true
    ): T? {
        val player = mc.player ?: return null

        val slot = filter {
            val stack = it.itemStack
            val durabilityCheck = (ignoreDurability || stack.damage < (stack.maxDamage - 2))
            stack.isNothing() || (!player.isCreative && durabilityCheck)
        }.maxByOrNull {
            it.itemStack.getMiningSpeedMultiplier(blockState)
        } ?: return null

        val miningSpeedMultiplier = slot.itemStack.getMiningSpeedMultiplier(blockState)

        // The current slot already matches the best
        if (miningSpeedMultiplier == player.inventory.mainHandStack.getMiningSpeedMultiplier(blockState)) {
            return null
        }

        return slot
    }

}
