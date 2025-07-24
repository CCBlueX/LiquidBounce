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
 *
 */

package net.ccbluex.liquidbounce.features.module.modules.movement.fly.modes.fireball

import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.ModuleFly
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.modes.fireball.techniques.FlyFireballCustomTechnique
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.modes.fireball.techniques.FlyFireballLegitTechnique
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.modes.fireball.trigger.FlyFireballInstantTrigger
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.modes.fireball.trigger.FlyFireballOnEdgeTrigger
import net.ccbluex.liquidbounce.utils.client.SilentHotbar
import net.ccbluex.liquidbounce.utils.inventory.interactItem
import net.minecraft.item.FireChargeItem
import net.minecraft.util.Hand

internal object FlyFireball : Choice("Fireball") {

    override val parent: ChoiceConfigurable<*>
        get() = ModuleFly.modes

    val technique = choices("Technique", FlyFireballLegitTechnique,
        arrayOf(FlyFireballLegitTechnique, FlyFireballCustomTechnique))

    val trigger = choices("Trigger", FlyFireballInstantTrigger,
        arrayOf(FlyFireballInstantTrigger, FlyFireballOnEdgeTrigger))

    // Silent fireball selection
    object AutoFireball : ToggleableConfigurable(this, "AutoFireball", true) {
        val slotResetDelay by int("SlotResetDelay", 5, 0..40, "ticks")
    }

    var wasTriggered = false

    init {
        tree(AutoFireball)
    }

    private fun findFireballSlot(): Int? {
        return (0..8).firstOrNull {
            val stack = player.inventory.getStack(it)
            stack.item is FireChargeItem
        }
    }

    fun holdsFireball() = player.inventory.mainHandStack.item is FireChargeItem

    fun throwFireball() {
        interactItem(Hand.MAIN_HAND)
    }

    @Suppress("unused")
    val handleSilentFireballSelection = tickHandler {
        if (AutoFireball.enabled) {
            val bestMainHandSlot = findFireballSlot()
            if (bestMainHandSlot != null) {
                SilentHotbar.selectSlotSilently(this, bestMainHandSlot, AutoFireball.slotResetDelay)
            } else {
                SilentHotbar.resetSlot(this)
            }
        } else {
            SilentHotbar.resetSlot(this)
        }
    }

}
