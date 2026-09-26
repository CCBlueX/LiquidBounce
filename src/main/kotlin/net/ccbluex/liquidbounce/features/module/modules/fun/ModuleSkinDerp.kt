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
package net.ccbluex.liquidbounce.features.module.modules.`fun`

import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.event.waitTicks
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.minecraft.world.entity.player.PlayerModelPart
import kotlin.random.Random

/**
 * Skin Derp module
 *
 * Makes your skin blink (Requires multi-layer skin).
 */
@Suppress("MagicNumber")
object ModuleSkinDerp : ClientModule("SkinDerp", ModuleCategories.FUN) {
    private val sync by boolean("Sync", false)
    private val delay by int("Delay", 0, 0..20, "ticks")
    private val parts by multiEnumChoice("Parts", DerpParts.entries)

    private var prevModelParts = emptySet<PlayerModelPart>()

    override fun onEnabled() {
        prevModelParts = mc.options.modelParts.toSet()
    }

    override fun onDisabled() {
        // Disable all current model parts
        for (modelPart in PlayerModelPart.entries) {
            mc.options.setModelPart(modelPart, false)
        }
        // Enable all old model parts
        for (modelPart in prevModelParts) {
            mc.options.setModelPart(modelPart, true)
        }
    }

    val repeatable = tickHandler {
        waitTicks(delay)

        parts.forEach {
            if (sync) {
                mc.options.setModelPart(it.part, !mc.options.isModelPartEnabled(it.part))
            } else {
                mc.options.setModelPart(it.part, Random.nextBoolean())
            }
        }
    }

    private enum class DerpParts(
        override val tag: String,
        val part: PlayerModelPart
    ) : Tagged {
        HAT("Hat", PlayerModelPart.HAT),
        JACKET("Jacket", PlayerModelPart.JACKET),
        LEFT_PANTS("LeftPants", PlayerModelPart.LEFT_PANTS_LEG),
        RIGHT_PANTS("RightPants", PlayerModelPart.RIGHT_PANTS_LEG),
        LEFT_SLEEVE("LeftSleeve", PlayerModelPart.LEFT_SLEEVE),
        RIGHT_SLEEVE("RightSleeve", PlayerModelPart.RIGHT_SLEEVE),
        CAPE("Cape", PlayerModelPart.CAPE)
    }
}
