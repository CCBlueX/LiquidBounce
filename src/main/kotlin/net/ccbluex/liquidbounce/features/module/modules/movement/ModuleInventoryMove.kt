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
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.NotificationEvent
import net.ccbluex.liquidbounce.event.events.ScreenEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.ccbluex.liquidbounce.utils.client.formatAsTime
import net.ccbluex.liquidbounce.utils.client.notification
import net.minecraft.client.gui.screen.ChatScreen
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.client.option.KeyBinding
import net.minecraft.item.ItemGroups

/**
 * InventoryMove module
 *
 * Allows you to walk while an inventory is opened.
 */

object ModuleInventoryMove : Module("InventoryMove", Category.MOVEMENT) {

    private val undetectable by boolean("Undetectable", false)
    private val passthroughSneak by boolean("PassthroughSneak", false)

    object Blink : ToggleableConfigurable(this,"Blink", false) {

        /**
         * After reaching this time, we will close the inventory and blink.
         */
        private val maximumTime by int("MaximumTime", 10000, 0..30000, "ms")

        private val chronometer = Chronometer()

        fun shouldLag() = ModuleInventoryMove.enabled && this.enabled && mc.currentScreen is HandledScreen<*>

        val screenHandler = handler<ScreenEvent> {
            if (it.screen is HandledScreen<*>) {
                chronometer.reset()

                notification("InventoryMove", message("blinkStart", maximumTime.formatAsTime()),
                    NotificationEvent.Severity.INFO)
            }
        }

        val repeatable = repeatable {
            if (shouldLag() && chronometer.hasElapsed(maximumTime.toLong())) {
                player.closeHandledScreen()
                notification("InventoryMove", message("blinkEnd"), NotificationEvent.Severity.INFO)
            }
        }

    }

    init {
        tree(Blink)
    }

    fun shouldHandleInputs(keyBinding: KeyBinding): Boolean {
        if (!enabled || mc.currentScreen is ChatScreen || isInCreativeSearchField()) {
            return false
        }

        if (keyBinding == mc.options.sneakKey && !passthroughSneak) {
            return false
        }

        // If we are in a handled screen, we should handle the inputs only if the undetectable option is not enabled
        return !undetectable || mc.currentScreen !is HandledScreen<*>
    }

    /**
     * Checks if the player is in the creative search field
     */
    private fun isInCreativeSearchField() =
        mc.currentScreen is CreativeInventoryScreen &&
            CreativeInventoryScreen.selectedTab == ItemGroups.getSearchGroup()

}
