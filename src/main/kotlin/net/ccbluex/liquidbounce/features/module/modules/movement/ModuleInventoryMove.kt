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
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.config.types.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.*
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleInventoryMove.Behaviour.NORMAL
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleInventoryMove.Behaviour.SAFE
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleClickGui.isInSearchBar
import net.ccbluex.liquidbounce.utils.client.*
import net.ccbluex.liquidbounce.utils.inventory.InventoryManager.isInventoryOpenServerSide
import net.ccbluex.liquidbounce.utils.inventory.closeInventorySilently
import net.ccbluex.liquidbounce.utils.inventory.isInInventoryScreen
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.minecraft.client.gui.screen.ChatScreen
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.client.gui.screen.ingame.InventoryScreen
import net.minecraft.client.option.KeyBinding
import net.minecraft.item.ItemGroups
import net.minecraft.network.packet.c2s.play.*
import org.lwjgl.glfw.GLFW.GLFW_RELEASE

/**
 * InventoryMove module
 *
 * Allows you to walk while an inventory is opened.
 */

object ModuleInventoryMove : ClientModule("InventoryMove", Category.MOVEMENT) {

    private val behavior by enumChoice("Behavior", NORMAL)

    enum class Behaviour(override val choiceName: String) : NamedChoice {
        NORMAL("Normal"),
        SAFE("Safe"), // disable clicks while moving
        UNDETECTABLE("Undetectable"), // stop in inventory
    }

    private val passthroughSneak by boolean("PassthroughSneak", false)

    // states of movement keys, using mc.options.<key>.isPressed doesn't work for some reason
    private val movementKeys = mc.options.run {
        arrayOf(forwardKey, leftKey, backKey, rightKey, jumpKey, sneakKey).associateWith { false }.toMutableMap()
    }

    val cancelClicks
        get() = behavior == SAFE && movementKeys.any { (key, pressed) -> pressed && shouldHandleInputs(key) }

    private object TimerFeature : ToggleableConfigurable(this, "Timer", false) {

        private val speed by float("Speed", 1.0f, 0.1f..2.0f)

        @Suppress("unused")
        private val tickHandler = tickHandler {
            if (mc.currentScreen is HandledScreen<*>) {
                Timer.requestTimerSpeed(speed, Priority.IMPORTANT_FOR_USAGE_2, ModuleInventoryMove)
            }
        }

    }

    private object BlinkFeature : ToggleableConfigurable(this,"Blink", false) {

        /**
         * After reaching this time, we will close the inventory and blink.
         */
        private val maximumTime by int("MaximumTime", 10000, 0..30000, "ms")

        private val chronometer = Chronometer()

        @Suppress("unused")
        private val fakeLagHandler = handler<QueuePacketEvent> { event ->
            val packet = event.packet

            if (mc.currentScreen is HandledScreen<*> && event.origin == TransferOrigin.OUTGOING) {
                event.action = when (packet) {
                    is ClickSlotC2SPacket,
                    is ButtonClickC2SPacket,
                    is CreativeInventoryActionC2SPacket,
                    is SlotChangedStateC2SPacket,
                    is CloseHandledScreenC2SPacket -> PacketQueueManager.Action.PASS
                    else -> PacketQueueManager.Action.QUEUE
                }
            }
        }

        @Suppress("unused")
        val screenHandler = handler<ScreenEvent> { event ->
            if (event.screen is HandledScreen<*>) {
                chronometer.reset()

                notification("InventoryMove", message("blinkStart", maximumTime.formatAsTime()),
                    NotificationEvent.Severity.INFO)
            }
        }

        @Suppress("unused")
        private val tickHandler = tickHandler {
            if (mc.currentScreen is HandledScreen<*> && chronometer.hasElapsed(maximumTime.toLong())) {
                player.closeHandledScreen()
                notification("InventoryMove", message("blinkEnd"), NotificationEvent.Severity.INFO)
            }
        }

    }

    init {
        tree(TimerFeature)
        tree(BlinkFeature)
    }

    fun shouldHandleInputs(keyBinding: KeyBinding): Boolean {
        val screen = mc.currentScreen ?: return true

        if (!running || screen is ChatScreen || isInCreativeSearchField() || isInSearchBar) {
            return false
        }

        if (keyBinding == mc.options.sneakKey && !passthroughSneak) {
            return false
        }

        // If we are in a handled screen, we should handle the inputs only if the undetectable option is not enabled
        return behavior == NORMAL || screen !is HandledScreen<*>
            || behavior == SAFE && screen is InventoryScreen
    }

    @Suppress("unused")
    val keyHandler = handler<KeyboardKeyEvent> { event ->
        val key = movementKeys.keys.find { it.matchesKey(event.keyCode, event.scanCode) } ?: return@handler
        val pressed = shouldHandleInputs(key) && event.action != GLFW_RELEASE
        movementKeys[key] = pressed

        if (behavior == SAFE && isInInventoryScreen && isInventoryOpenServerSide && pressed) {
            closeInventorySilently()
        }
    }

    /**
     * Checks if the player is in the creative search field
     */
    private fun isInCreativeSearchField() =
        mc.currentScreen is CreativeInventoryScreen &&
            CreativeInventoryScreen.selectedTab == ItemGroups.getSearchGroup()

}
