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
 */
package net.ccbluex.liquidbounce.features.module.modules.player.offhand

import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.KeyEvent
import net.ccbluex.liquidbounce.event.events.RefreshArrayListEvent
import net.ccbluex.liquidbounce.event.events.ScheduleInventoryActionEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.ModuleCrystalAura
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.HotbarItemSlot
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.ItemSlot
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.OffHandSlot
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.ccbluex.liquidbounce.utils.inventory.*
import net.ccbluex.liquidbounce.utils.item.findInventorySlot
import net.minecraft.item.Item
import net.minecraft.item.Items
import net.minecraft.item.SwordItem
import org.lwjgl.glfw.GLFW

/**
 * Scaffold module
 *
 * Places blocks under you.
 */
object ModuleOffhand : Module("Offhand", Category.PLAYER, aliases = arrayOf("AutoTotem")) {

    private val inventoryConstraints = tree(PlayerInventoryConstraints())
    private val switchDelay by int("SwitchDelay", 0, 0..500, "ms")
    private val cycleSlots by key("Cycle", GLFW.GLFW_KEY_H)
    private val totem = tree(Totem())

    private object Gapple : ToggleableConfigurable(this, "Gapple", true) {
        object WhileHoldingSword : ToggleableConfigurable(this, "WhileHoldingSword", true) {
            val onlyWhileKa by boolean("OnlyWhileKillAura", true)
        }

        val gappleBind by key("GappleBind", GLFW.GLFW_KEY_UNKNOWN)

        init {
            tree(WhileHoldingSword)
        }

    }

    private object Crystal : ToggleableConfigurable(this, "Crystal", true) {
        val onlyWhileCa by boolean("OnlyWhileCrystalAura", true)
        val whenNoTotems by boolean("WhenNoTotems", true)
        val crystalBind by key("CrystalBind", GLFW.GLFW_KEY_UNKNOWN)
    }

    init {
        tree(Crystal)
        tree(Gapple)
    }

    private val INVENTORY_MAIN_PRIORITY = INVENTORY_SLOTS + HOTBAR_SLOTS
    private val INVENTORY_HOTBAR_PRIORITY = HOTBAR_SLOTS + INVENTORY_SLOTS
    private val chronometer = Chronometer()
    private var activeMode: Mode = Mode.NONE
    private var lastMode: Mode? = null
    private var lastTagMode: Mode = Mode.NONE
    private var staticMode = Mode.NONE
    private var last: Pair<Item, ItemSlot>? = null

    override val tag: String
        get() = activeMode.modeName

    override fun enable() {
        staticMode = when {
            Crystal.enabled && Mode.CRYSTAL.canCycleTo() -> Mode.CRYSTAL
            Gapple.enabled -> Mode.GAPPLE
            totem.enabled && !Totem.Health.enabled -> Mode.TOTEM
            else -> Mode.NONE
        }
    }

    @Suppress("unused")
    val keyHandler = handler<KeyEvent> {
        if (it.action != GLFW.GLFW_PRESS) {
            return@handler
        }

        when (it.key.keyCode) {
            Gapple.gappleBind -> {
                if (Mode.GAPPLE.canCycleTo()) {
                    activeMode = Mode.GAPPLE
                }
            }
            Crystal.crystalBind -> {
                if (Mode.CRYSTAL.canCycleTo()) {
                    activeMode = Mode.CRYSTAL
                }
            }
            cycleSlots -> {
                val entries = Mode.entries
                val startIndex = staticMode.ordinal
                var index = (startIndex + 1) % entries.size

                while (index != startIndex) {
                    val mode = entries[index]
                    if (mode.canCycleTo()) {
                        staticMode = mode
                        return@handler
                    }

                    index = (index + 1) % entries.size
                }
            }
        }
    }

    @Suppress("unused")
    private val autoTotemHandler = handler<ScheduleInventoryActionEvent> {
        activeMode = Mode.entries.firstOrNull(Mode::shouldEquip) ?: staticMode
        if (activeMode == Mode.NONE && Totem.Health.switchBack && lastMode == Mode.TOTEM) {
            activeMode = Mode.BACK
        }

        if (activeMode != lastTagMode) {
            EventManager.callEvent(RefreshArrayListEvent())
            lastTagMode = activeMode
        }

        if (!chronometer.hasElapsed(activeMode.getDelay().toLong())) {
            return@handler
        }

        val slot = activeMode.getSlot() ?: return@handler
        lastMode = activeMode

        // the item is already located in Off-hand slot
        if (slot == OFFHAND_SLOT) {
            return@handler
        }

        if (Totem.Health.switchBack) {
            last = slot.itemStack.item to slot
        }

        val actions = ArrayList<ClickInventoryAction>(3)

        if (slot is HotbarItemSlot) {
            actions += ClickInventoryAction.performSwap(from = slot, to = OffHandSlot)
        } else {
            actions += ClickInventoryAction.performPickup(slot = slot)
            actions += ClickInventoryAction.performPickup(slot = OffHandSlot)
            if (!OffHandSlot.itemStack.isEmpty) {
                actions += ClickInventoryAction.performPickup(slot = slot)
            }
        }

        it.schedule(inventoryConstraints, actions)
        chronometer.reset()
    }

    fun isOperating() = enabled && activeMode != Mode.NONE

    // TODO MoreCarry slots
    private enum class Mode(val modeName: String, private val item: Item, private val fallBackItem: Item? = null) {
        TOTEM("Totem", Items.TOTEM_OF_UNDYING) {
            override fun shouldEquip() = totem.shouldEquip()

            override fun getDelay() = totem.switchDelay

            override fun getPrioritizedInventoryPart() = 1

            override fun getSlot(): ItemSlot? {
                val slot = super.getSlot()
                if (slot == null && Crystal.enabled && Crystal.whenNoTotems) {
                    return CRYSTAL.getSlot()
                }

                return slot
            }

            override fun canCycleTo() = totem.enabled
        },
        GAPPLE("Gapple", Items.ENCHANTED_GOLDEN_APPLE, Items.GOLDEN_APPLE) {
            override fun shouldEquip(): Boolean {
                if (!Gapple.enabled) {
                    return false
                }

                if (player.mainHandStack.item is SwordItem && Gapple.WhileHoldingSword.enabled) {
                    return if (Gapple.WhileHoldingSword.onlyWhileKa) {
                        ModuleKillAura.enabled
                    } else {
                        true
                    }
                }

                return false
            }

            override fun canCycleTo() = Gapple.enabled
        },
        CRYSTAL("Crystal", Items.END_CRYSTAL) {
            override fun canCycleTo() = Crystal.enabled && (!Crystal.onlyWhileCa || ModuleCrystalAura.enabled)
        },
        BACK("Back", Items.AIR) {
            override fun getSlot(): ItemSlot? {
                return last?.let {
                    if (it.first == it.second.itemStack.item) it.second else null
                }
            }

            override fun canCycleTo() = false
        },
        NONE("None", Items.AIR) {
            override fun canCycleTo() = false
        };

        open fun shouldEquip() = false

        open fun getDelay() = switchDelay

        abstract fun canCycleTo(): Boolean

        /**
         * 0 = Main inventory
         * 1 = Hotbar
         */
        open fun getPrioritizedInventoryPart() = 0

        open fun getSlot(): ItemSlot? {
            if (item == Items.AIR) {
                return null
            }

            if (player.offHandStack.item == item) {
                return OFFHAND_SLOT
            }

            val slots = if (getPrioritizedInventoryPart() == 0) {
                INVENTORY_MAIN_PRIORITY
            } else {
                INVENTORY_HOTBAR_PRIORITY
            }

            var itemSlot = findInventorySlot(slots) { it.item == item }
            if (itemSlot == null && fallBackItem != null) {
                if (player.offHandStack.item == fallBackItem) {
                    return OFFHAND_SLOT
                }

                itemSlot = findInventorySlot(slots) { it.item == fallBackItem }
            }

            return itemSlot
        }
    }

}
