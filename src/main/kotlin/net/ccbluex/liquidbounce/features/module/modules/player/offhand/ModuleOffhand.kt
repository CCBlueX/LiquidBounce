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

import com.google.common.base.Predicate
import net.ccbluex.liquidbounce.config.NamedChoice
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
import net.ccbluex.liquidbounce.utils.client.hasProtocolTranslator
import net.ccbluex.liquidbounce.utils.client.isNewerThanOrEquals1_16
import net.ccbluex.liquidbounce.utils.inventory.*
import net.ccbluex.liquidbounce.utils.item.findInventorySlot
import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.PotionContentsComponent
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.item.SwordItem
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import org.lwjgl.glfw.GLFW

/**
 * Offhand module
 *
 * Manages your offhand.
 */
object ModuleOffhand : Module("Offhand", Category.PLAYER, aliases = arrayOf("AutoTotem")) {

    private val inventoryConstraints = tree(PlayerInventoryConstraints())
    private var switchMode = enumChoice("SwitchMode", SwitchMode.AUTOMATIC)
    private val switchDelay by int("SwitchDelay", 0, 0..500, "ms")
    private val cycleSlots by bind("Cycle", GLFW.GLFW_KEY_H)
    private val totem = tree(Totem())

    private object Gapple : ToggleableConfigurable(this, "Gapple", true) {
        object WhileHoldingSword : ToggleableConfigurable(this, "WhileHoldingSword", true) {
            val onlyWhileKa by boolean("OnlyWhileKillAura", true)
        }

        val gappleBind by bind("GappleBind", GLFW.GLFW_KEY_UNKNOWN)

        init {
            tree(WhileHoldingSword)
        }
    }

    private object Crystal : ToggleableConfigurable(this, "Crystal", true) {
        val onlyWhileCa by boolean("OnlyWhileCrystalAura", false)
        val whenNoTotems by boolean("WhenNoTotems", true)
        val crystalBind by bind("CrystalBind", GLFW.GLFW_KEY_UNKNOWN)
    }

    private object Strength : ToggleableConfigurable(this, "StrengthPotion", false) {
        val onlyWhileHoldingSword by boolean("OnlyWhileHoldingSword", true)
        val onlyWhileKa by boolean("OnlyWhileKillAura", true)
        val strengthBind by bind("StrengthBind", GLFW.GLFW_KEY_UNKNOWN)
    }

    init {
        tree(Crystal)
        tree(Gapple)
        tree(Strength)

        if (!hasProtocolTranslator) {
            switchMode = enumChoice("SwitchMode", SwitchMode.SWITCH)
        }
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

        when (it.key.code) {
            Gapple.gappleBind.boundKey.code -> Mode.GAPPLE.onBindPress()
            Crystal.crystalBind.boundKey.code -> Mode.CRYSTAL.onBindPress()
            Strength.strengthBind.boundKey.code -> {
                // since we can't cycle to strength, its status has to be checked here
                if (Strength.enabled) {
                    Mode.STRENGTH.onBindPress()
                }
            }

            cycleSlots.boundKey.code -> {
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
    private val autoTotemHandler = handler<ScheduleInventoryActionEvent>(priority = 100) {
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

        val actions = switchMode.get().performSwitch(slot)
        if (actions.isEmpty()) {
            chronometer.reset()
            return@handler
        }

        if (activeMode != Mode.TOTEM || !totem.send(actions)) {
            it.schedule(inventoryConstraints, actions)
        }

        chronometer.reset()
    }

    fun performSwitch(from: ItemSlot, smart: Boolean): List<ClickInventoryAction> {
        val actions = ArrayList<ClickInventoryAction>(3)

        if (smart && from is HotbarItemSlot) {
            if (!player.isSpectator) {
                network.sendPacket(
                    PlayerActionC2SPacket(
                        PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND,
                        BlockPos.ORIGIN,
                        Direction.DOWN
                    )
                )
            }
        } else {
            actions += ClickInventoryAction.performPickup(slot = from)
            actions += ClickInventoryAction.performPickup(slot = OffHandSlot)
            if (!OffHandSlot.itemStack.isEmpty) {
                actions += ClickInventoryAction.performPickup(slot = from)
            }
        }

        return actions
    }

    fun isOperating() = enabled && activeMode != Mode.NONE

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
        STRENGTH("Strength", Items.POTION) {
            val isStrengthPotion = Predicate<ItemStack> { stack ->
                if (stack.item != Items.POTION) {
                    return@Predicate false
                }

                val content = stack.getOrDefault(DataComponentTypes.POTION_CONTENTS, PotionContentsComponent.DEFAULT)
                content.effects.forEach { effect ->
                    if (effect.effectType == StatusEffects.STRENGTH) {
                        return@Predicate true
                    }
                }

                return@Predicate false
            }

            override fun shouldEquip(): Boolean {
                val killAura = Strength.onlyWhileKa && !ModuleKillAura.enabled
                if (!Strength.enabled || killAura || player.hasStatusEffect(StatusEffects.STRENGTH)) {
                    return false
                }

                return player.mainHandStack.item is SwordItem || !Strength.onlyWhileHoldingSword
            }

            override fun getSlot(): ItemSlot? {
                if (isStrengthPotion.test(player.offHandStack)) {
                    return OFFHAND_SLOT
                }

                return findInventorySlot(INVENTORY_MAIN_PRIORITY) { isStrengthPotion.test(it) }
            }
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
        },
        NONE("None", Items.AIR);

        private var modeBeforeDirectSwitch: Mode? = null

        open fun shouldEquip() = false

        open fun getDelay() = switchDelay

        open fun canCycleTo() = false

        /**
         * 0 = Main inventory
         * 1 = Hotbar
         */
        open fun getPrioritizedInventoryPart() = 0

        fun onBindPress() {
            if (activeMode == this && modeBeforeDirectSwitch != null && modeBeforeDirectSwitch!!.canCycleTo()) {
                staticMode = modeBeforeDirectSwitch!!
                modeBeforeDirectSwitch = null
            } else if (canCycleTo()) {
                modeBeforeDirectSwitch = staticMode
                staticMode = this
            } else {
                modeBeforeDirectSwitch = null
            }
        }

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

    @Suppress("unused")
    private enum class SwitchMode(override val choiceName: String) : NamedChoice {
        /**
         * Pickup, but it performs a SWAP_ITEM_WITH_OFFHAND action whenever possible to send fewer packets.
         * Works on all versions.
         *
         * It's not the default because some servers kick you when you perform a SWAP_ITEM_WITH_OFFHAND action
         * often and quickly.
         */
        SMART("Smart") {
            override fun performSwitch(from: ItemSlot) = performSwitch(from, true)
        },

        /**
         * Performs a switch action, works on 1.16.
         * The best method on newer servers.
         */
        SWITCH("Switch") {
            override fun performSwitch(from: ItemSlot) = listOf(ClickInventoryAction.performSwap(
                from = from,
                to = OffHandSlot
            ))
        },

        /**
         * Performs 2-3 a pickup actions.
         * Works on all versions.
         */
        PICKUP("PickUp") {
            override fun performSwitch(from: ItemSlot) = performSwitch(from, false)
        },

        /**
         * Chooses the switch action based on the version. Only works if vfp is installed.
         */
        AUTOMATIC("Automatic") {
            override fun performSwitch(from: ItemSlot): List<ClickInventoryAction> {
                return if (isNewerThanOrEquals1_16) {
                    SWITCH.performSwitch(from)
                } else {
                    PICKUP.performSwitch(from)
                }
            }
        };

        abstract fun performSwitch(from: ItemSlot): List<ClickInventoryAction>
    }

}
