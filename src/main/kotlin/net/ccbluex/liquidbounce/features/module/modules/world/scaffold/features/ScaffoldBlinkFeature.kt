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
package net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features

import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.fakelag.FakeLag
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.ccbluex.liquidbounce.utils.client.Chronometer

object ScaffoldBlinkFeature : ToggleableConfigurable(ModuleScaffold, "Blink", false) {

    private val time by intRange("Time", 50..250, 0..3000, "ms")
    private val fallCancel by boolean("FallCancel", true)

    private var pulseTime = 0L
    private val pulseTimer = Chronometer()

    val shouldBlink
        get() = handleEvents() && (!player.isOnGround || !pulseTimer.hasElapsed(pulseTime))

    fun onBlockPlacement() {
        pulseTime = time.random().toLong()
    }

    val repeatable = repeatable {
        if (fallCancel && player.fallDistance > 0.5f) {
            FakeLag.cancel()
            onBlockPlacement()
        }

        if (pulseTimer.hasElapsed(pulseTime)) {
            pulseTimer.reset()
        }
    }


}
