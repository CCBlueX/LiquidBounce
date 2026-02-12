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

package net.ccbluex.liquidbounce.features.module.modules.misc

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.ccbluex.liquidbounce.config.types.group.ValueGroup
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.config.utils.asRefreshable
import net.ccbluex.liquidbounce.event.events.KeyEvent
import net.ccbluex.liquidbounce.event.suspendHandler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.utils.block.SwingMode
import net.ccbluex.liquidbounce.utils.block.doPlacement
import net.ccbluex.liquidbounce.utils.client.SilentHotbar
import net.ccbluex.liquidbounce.utils.client.clientStartDurationMs
import net.ccbluex.liquidbounce.utils.client.sendChatOrCommand
import net.ccbluex.liquidbounce.utils.inventory.SingleItemStackPickMode
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.ccbluex.liquidbounce.utils.inventory.useItem
import net.minecraft.world.phys.BlockHitResult

/**
 * Macros module
 *
 * Lets you execute chat messages or item actions using custom keybinds.
 */
object ModuleMacros : ClientModule("Macros", ModuleCategories.MISC) {

    /** We don't have mutable value group list yet */
    private const val COUNT = 5

    private val macros: List<Macro>

    init {
        val macros = ArrayList<Macro>()
        repeat(COUNT) {
            macros += tree(Macro.Chat("Chat-${it + 1}"))
        }
        repeat(COUNT) {
            macros += tree(Macro.UseItem("Item-${it + 1}"))
        }
        this.macros = macros
    }

    @Suppress("unused")
    private val keyboardKeyHandler = suspendHandler<KeyEvent> { event ->
        val now = clientStartDurationMs
        for (macro in macros) {
            if (macro.trigger == event.key && now - macro.lastTriggerTime > macro.triggerDelayMs.current) {
                macro.lastTriggerTime = now
                macro.triggerDelayMs.refresh()
                launch {
                    macro.execute()
                }
            }
        }
    }

    private sealed class Macro(name: String) : ValueGroup(name) {

        var lastTriggerTime = 0L

        val triggerDelayMs = intRange("TriggerDelay", 0..0, 0..15000, "ms").asRefreshable()

        val trigger by key("Trigger")

        abstract suspend fun execute()

        class Chat(name: String) : Macro(name) {

            private val delay by intRange("Delay", 0..0, 0..5000, "ms")

            private val messages by textList("Messages", ArrayList())

            override suspend fun execute() {
                for (message in messages) {
                    network.sendChatOrCommand(message)
                    delay(delay.random().toLong())
                }
            }

        }

        class UseItem(name: String) : Macro(name) {

            private val action by enumChoice("Action", Action.USE_ONLY)

            private val pickMode = modes(ModuleMacros, "PickMode", 0) {
                arrayOf(
                    SingleItemStackPickMode.ByName(it),
                    SingleItemStackPickMode.ByItem(it),
                )
            }

            private val swingMode by enumChoice("SwingMode", SwingMode.DO_NOT_HIDE)
            private val holdTime by intRange("HoldTime", 1..1, 1..20, "ticks")

            private enum class Action(override val tag: String) : Tagged {
                USE_ONLY("UseOnly"),
                PLACE_OR_USE("PlaceOrUse"),
            }

            override suspend fun execute() {
                val slot = Slots.OffhandWithHotbar.findSlot { pickMode.activeMode.test(it) } ?: return

                SilentHotbar.selectSlotSilently(ModuleMacros, slot, ticksUntilReset = holdTime.random())
                when (action) {
                    Action.USE_ONLY -> useItem(slot.useHand, swingMode = swingMode)

                    Action.PLACE_OR_USE -> {
                        val hitResult = mc.hitResult as? BlockHitResult ?: return
                        doPlacement(hitResult, hand = slot.useHand, swingMode = swingMode)
                    }
                }
            }

        }
    }

    override fun onDisabled() {
        SilentHotbar.resetSlot(this)
        super.onDisabled()
    }

}
