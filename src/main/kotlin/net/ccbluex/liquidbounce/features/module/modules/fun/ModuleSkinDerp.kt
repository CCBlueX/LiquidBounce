/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2021 CCBlueX
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

import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.minecraft.client.render.entity.PlayerModelPart
import kotlin.random.Random

/**
 * Skin Derp module
 *
 * Makes your skin blink (Requires multi-layer skin).
 */
object ModuleSkinDerp : Module("SkinDerp", Category.FUN) {

    private val delay by int("Delay", 0, 0..20)
    private val hat by boolean("Hat", true)
    private val jacket by boolean("Jacket", true)
    private val leftPants by boolean("LeftPants", true)
    private val rightPants by boolean("RightPants", true)
    private val leftSleeve by boolean("LeftSleeve", true)
    private val rightSleeve by boolean("RightSleeve", true)
    private val cape by boolean("Cape", true)

    private var prevModelParts = emptySet<PlayerModelPart>()

    override fun enable() {
        prevModelParts = mc.options.enabledPlayerModelParts
    }

    override fun disable() {
        // Disable all current model parts
        for (modelPart in PlayerModelPart.values()) {
            mc.options.togglePlayerModelPart(modelPart, false)
        }

        // Enable all old model parts
        for (modelPart in prevModelParts) {
            mc.options.togglePlayerModelPart(modelPart, true)
        }
    }

    val repeatable = repeatable {
        wait(delay)
        if (hat) {
            mc.options.togglePlayerModelPart(PlayerModelPart.HAT, Random.nextBoolean())
        }
        if (jacket) {
            mc.options.togglePlayerModelPart(PlayerModelPart.JACKET, Random.nextBoolean())
        }
        if (leftPants) {
            mc.options.togglePlayerModelPart(PlayerModelPart.LEFT_PANTS_LEG, Random.nextBoolean())
        }
        if (rightPants) {
            mc.options.togglePlayerModelPart(PlayerModelPart.RIGHT_PANTS_LEG, Random.nextBoolean())
        }
        if (leftSleeve) {
            mc.options.togglePlayerModelPart(PlayerModelPart.LEFT_SLEEVE, Random.nextBoolean())
        }
        if (rightSleeve) {
            mc.options.togglePlayerModelPart(PlayerModelPart.RIGHT_SLEEVE, Random.nextBoolean())
        }
        if (cape) {
            mc.options.togglePlayerModelPart(PlayerModelPart.CAPE, Random.nextBoolean())
        }
    }
}
