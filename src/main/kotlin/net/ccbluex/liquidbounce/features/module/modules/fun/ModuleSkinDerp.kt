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

import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.repeatableSequence
import net.minecraft.client.render.entity.PlayerModelPart
import kotlin.random.Random

object ModuleSkinDerp : Module("SkinDerp", Category.FUN) {

    val ticks by int("Ticks", 0, 0..20)
    private val hat by boolean("Hat", true)
    private val jacket by boolean("Jacket", true)
    private val leftpants by boolean("LeftPants", true)
    private val rightpants by boolean("RightPants", true)
    private val leftsleeve by boolean("LeftSleeve", true)
    private val rightsleeve by boolean("RightSleeve", true)
    private val cape by boolean("Cape", true)
    private var prevmodelparts = emptySet<PlayerModelPart>()

    private var tick = 0
    override fun enable() {

        prevmodelparts = mc.options.enabledPlayerModelParts
    }

    override fun disable() {

        for(modelpart in PlayerModelPart.values()) {
            mc.options.setPlayerModelPart(modelpart, false)
        }
        for(modelpart in prevmodelparts) {
            mc.options.setPlayerModelPart(modelpart, true)
        }
    }

    val repeatable = repeatableSequence {
        if(tick >= ticks) {
            if (hat)
                mc.options.setPlayerModelPart(PlayerModelPart.HAT, Random.nextBoolean())
            if (jacket)
                mc.options.setPlayerModelPart(PlayerModelPart.JACKET, Random.nextBoolean())
            if (leftpants)
                mc.options.setPlayerModelPart(PlayerModelPart.LEFT_PANTS_LEG, Random.nextBoolean())
            if (rightpants)
                mc.options.setPlayerModelPart(PlayerModelPart.RIGHT_PANTS_LEG, Random.nextBoolean())
            if (leftsleeve)
                mc.options.setPlayerModelPart(PlayerModelPart.LEFT_SLEEVE, Random.nextBoolean())
            if (rightsleeve)
                mc.options.setPlayerModelPart(PlayerModelPart.RIGHT_SLEEVE, Random.nextBoolean())
            if(cape)
                mc.options.setPlayerModelPart(PlayerModelPart.CAPE, Random.nextBoolean())
            tick = 0
        }
    }
}
