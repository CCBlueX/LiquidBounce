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
package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.config.types.group.Mode
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.event.events.BlockBreakingProgressEvent
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.ScheduleInventoryActionEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.once
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.utils.block.bed.BedBlockTracker
import net.ccbluex.liquidbounce.utils.block.getCenterDistanceSquaredEyes
import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.client.SilentHotbar
import net.ccbluex.liquidbounce.utils.collection.Filter
import net.ccbluex.liquidbounce.utils.collection.blockSortedSetOf
import net.ccbluex.liquidbounce.utils.inventory.HotbarItemSlot
import net.ccbluex.liquidbounce.utils.inventory.InventoryAction
import net.ccbluex.liquidbounce.utils.inventory.InventoryConstraints
import net.ccbluex.liquidbounce.utils.inventory.ItemSlot
import net.ccbluex.liquidbounce.utils.inventory.SlotGroup
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.ccbluex.liquidbounce.utils.item.durability
import net.ccbluex.liquidbounce.utils.item.getEnchantment
import net.ccbluex.liquidbounce.utils.math.sq
import net.minecraft.core.BlockPos
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.enchantment.Enchantments
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState

/**
 * AutoTool module
 *
 * Automatically chooses the best tool in your inventory to mine a block.
 */
object ModuleAutoTool : ClientModule("AutoTool", ModuleCategories.WORLD) {
    val toolSelector =
        choices(
            "ToolSelector",
            DynamicSelectMode,
            arrayOf(DynamicSelectMode, StaticSelectMode)
        )

    sealed class ToolSelectorMode(name: String) : Mode(name) {
        final override val parent: ModeValueGroup<*>
            get() = toolSelector

        fun getTool(blockState: BlockState): HotbarItemSlot? =
            if (filter(blockState.block, blocks)) {
                getToolSlot(blockState)
            } else {
                null
            }

        protected abstract fun getToolSlot(blockState: BlockState): HotbarItemSlot?
    }

    private object DynamicSelectMode : ToolSelectorMode("Dynamic") {
        private val ignoreDurability by boolean("IgnoreDurability", false)

        object ConsiderInventory : ToggleableValueGroup(this, "ConsiderInventory", enabled = false) {
            private val inventoryConstraints = tree(InventoryConstraints())

            @JvmField var currentBestTool: ItemSlot? = null
            private var swapAction: InventoryAction? = null

            @Suppress("unused")
            private val inventoryActionHandler = handler<ScheduleInventoryActionEvent> { event ->
                val currentBestTool = currentBestTool ?: return@handler
                val slotToSwap = Slots.Hotbar.findSlot { it.isEmpty } ?: Slots.Hotbar[SilentHotbar.serversideSlot]

                event.schedule(
                    inventoryConstraints,
                    InventoryAction.Click.performSwap(
                        from = currentBestTool,
                        to = slotToSwap,
                    ).also { if (swapAction == null) swapAction = it }
                )
                this.currentBestTool = null
            }

            @JvmField var waitingTicks = 0

            @Suppress("unused")
            private val tickHandler = handler<GameTickEvent> {
                waitingTicks++
                if (waitingTicks <= swapPreviousDelay) return@handler

                waitingTicks = 0
                val swapAction = swapAction ?: return@handler
                this.swapAction = null
                once<ScheduleInventoryActionEvent> { event ->
                    event.schedule(inventoryConstraints, swapAction)
                }
            }

            override fun onDisabled() {
                waitingTicks = 0
                currentBestTool = null
                swapAction = null
                super.onDisabled()
            }
        }

        init {
            tree(ConsiderInventory)
        }

        override fun getToolSlot(blockState: BlockState): HotbarItemSlot? {
            if (!ConsiderInventory.running) {
                return Slots.Hotbar.findBestToolToMineBlock(blockState, ignoreDurability)
            } else {
                val slot = (Slots.Hotbar + Slots.Inventory).findBestToolToMineBlock(blockState, ignoreDurability)

                ConsiderInventory.waitingTicks = 0
                if (slot is HotbarItemSlot?) {
                    // We found the best tool in hotbar, don't need inventory action
                    ConsiderInventory.currentBestTool = null
                    return slot
                } else {
                    // Request inventory action
                    ConsiderInventory.currentBestTool = slot
                    return null
                }
            }
        }
    }

    private object StaticSelectMode : ToolSelectorMode("Static") {
        private val slot by int("Slot", 0, 0..8)

        override fun getToolSlot(blockState: BlockState) = Slots.Hotbar[slot]
    }

    private val filter by enumChoice("Filter", Filter.BLACKLIST)
    private val blocks by blocks("Blocks", blockSortedSetOf())

    private object SilkTouchHandler : ToggleableValueGroup(
        this, "SilkTouchHandler", enabled = false
    ) {
        private val filter by enumChoice("Filter", Filter.WHITELIST)
        private val blocks by blocks(
            "Blocks",
            blockSortedSetOf(Blocks.ENDER_CHEST, Blocks.GLOWSTONE, Blocks.SEA_LANTERN, Blocks.TURTLE_EGG),
        )

        fun test(blockState: BlockState, itemStack: ItemStack): Boolean =
            !running // If module AutoTool is disabled, this function returns true
                || blockState.block !in blocks
                || (filter == Filter.BLACKLIST) == (itemStack.getEnchantment(Enchantments.SILK_TOUCH) == 0)
    }

    init {
        tree(SilkTouchHandler)
    }

    private val swapPreviousDelay by int("SwapPreviousDelay", 20, 1..100, "ticks")

    private val requireSneaking by boolean("RequireSneaking", false)

    private object RequireNearBed : ToggleableValueGroup(
        this, "RequireNearBed", enabled = false
    ), BedBlockTracker.Subscriber {
        override val maxLayers: Int get() = 1

        override fun onEnabled() {
            BedBlockTracker.subscribe(this)
        }

        override fun onDisabled() {
            BedBlockTracker.unsubscribe(this)
        }

        private val distance by float("Distance", 10.0f, 3.0f..50.0f)

        fun matches(): Boolean {
            return BedBlockTracker.allPositions().any { it.getCenterDistanceSquaredEyes() <= distance.sq() }
        }
    }

    init {
        tree(RequireNearBed)
    }

    val isInventoryConsidered: Boolean
        get() = DynamicSelectMode.ConsiderInventory.running

    @Suppress("unused")
    private val handleBlockBreakingProgress = handler<BlockBreakingProgressEvent> { event ->
        switchToBreakBlock(event.pos)
    }

    fun switchToBreakBlock(pos: BlockPos) {
        if (requireSneaking && !player.isShiftKeyDown || RequireNearBed.enabled && !RequireNearBed.matches()) {
            return
        }

        val blockState = pos.getState()!!
        val slot = toolSelector.activeMode.getTool(blockState) ?: return
        SilentHotbar.selectSlotSilently(this, slot, swapPreviousDelay)
    }

    fun <T : ItemSlot> SlotGroup<T>.findBestToolToMineBlock(
        blockState: BlockState,
        ignoreDurability: Boolean = true
    ): T? {
        val player = mc.player ?: return null

        val slot = filter {
            val stack = it.itemStack
            val durabilityCheck = (ignoreDurability || (stack.durability > 2 || stack.maxDamage <= 0))
            !player.isCreative && durabilityCheck && SilkTouchHandler.test(blockState, stack)
        }.maxWithOrNull(
            Comparator.comparingDouble<T> {
                it.itemStack.getDestroySpeed(blockState).toDouble()
            }.thenDescending(ItemSlot.PREFER_NEARBY)
        ) ?: return null

        return slot
    }

}
