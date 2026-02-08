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

package net.ccbluex.liquidbounce.features.module.modules.render.hitfx

import net.ccbluex.fastutil.mapToArray
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.utils.client.logger
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.sounds.SoundEvent

object HitFXRegistry {

    val BONK = register("bonk")
    val POP = register("pop")
    val UWU = register("uwu")
    val NYA = register("nya")
    val TUNG = register("tung")
    val MEOW = register("meow")
    val BRING = register("bring")
    val SOFT = register("soft")
    val SQUASH = register("squash")
    val MAGICSQUASH = register("magicsquash")

    val CLICK = register("click-1", "click-2", "click-3")
    val BOYKISSER = register(
        "boykisser-1",
        "boykisser-2",
        "boykisser-3",
        "boykisser-4",
        "boykisser-5",
        "boykisser-6"
    )
    val GLASS = register("glass-1", "glass-2", "glass-3")
    val MOAN = register("moan-1", "moan-2", "moan-3", "moan-4")

    fun init() {
        logger.info("HitFXRegistry initialized")
    }

    private fun register(vararg ids: String) = ids.mapToArray { id ->
        val soundId = LiquidBounce.identifier(id)

        Registry.register(
            BuiltInRegistries.SOUND_EVENT,
            soundId,
            SoundEvent.createVariableRangeEvent(soundId)
        )
    }

}
