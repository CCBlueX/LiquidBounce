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
package net.ccbluex.liquidbounce.features.module.modules.movement.noslow.modes.blocking

import it.unimi.dsi.fastutil.floats.FloatFloatPair
import net.ccbluex.liquidbounce.config.types.nesting.NoneChoice
import net.ccbluex.liquidbounce.features.module.modules.movement.noslow.NoSlowUseActionHandler
import net.ccbluex.liquidbounce.features.module.modules.movement.noslow.modes.shared.NoSlowSharedGrim2360
import net.ccbluex.liquidbounce.features.module.modules.movement.noslow.modes.shared.NoSlowSharedGrim2364MC18
import net.ccbluex.liquidbounce.features.module.modules.movement.noslow.modes.shared.NoSlowSharedInvalidHand
import net.ccbluex.liquidbounce.utils.client.InteractionTracker.isBlocking
import net.ccbluex.liquidbounce.utils.client.inGame
import net.minecraft.item.consume.UseAction

internal object NoSlowBlock : NoSlowUseActionHandler("Blocking") {

    private val onlySlowOnServerSide by boolean("OnlySlowOnServerSide", false)

    val modes = choices(this, "Choice") {
        arrayOf(
            NoneChoice(it),
            NoSlowBlockingReuse,
            NoSlowBlockingSwitch,
            NoSlowBlockingBlink,
            NoSlowBlockingInteract,
            NoSlowSharedGrim2360(it),
            NoSlowSharedGrim2364MC18(it),
            NoSlowSharedInvalidHand(it),
            NoSlowBlockIntave14(it)
        )
    }

    override fun getMultiplier(): FloatFloatPair {
        if (onlySlowOnServerSide && isBlocking) {
            return DEFAULT_USE_MUL
        }

        return super.getMultiplier()
    }

    override val running: Boolean
        get() {
            if (!super.running || !inGame) {
                return false
            }

            // Check if we are using a block item
            return (player.isUsingItem && player.activeItem.useAction == UseAction.BLOCK) || isBlocking
        }

}

