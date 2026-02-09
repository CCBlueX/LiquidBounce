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

package net.ccbluex.liquidbounce.render.utils

import com.mojang.blaze3d.systems.RenderPass
import net.ccbluex.liquidbounce.config.types.Value
import net.ccbluex.liquidbounce.config.types.group.ValueGroup
import net.ccbluex.liquidbounce.render.ClientUniformDefine
import net.ccbluex.liquidbounce.utils.render.writeStd140

class DistanceFadeUniformValueGroup : ValueGroup("DistanceFade") {

    val nearStart: Float by float("NearStart", 0F, 0F..512F).onChange {
        minOf(it, nearEnd)
    }.markDirtyOnChanged()
    val nearEnd: Float by float("NearEnd", 0F, 0F..512F).onChange {
        it.coerceIn(nearStart, farStart)
    }.markDirtyOnChanged()
    val farStart: Float by float("FarStart", 512F, 0F..512F).onChange {
        it.coerceIn(nearEnd, farEnd)
    }.markDirtyOnChanged()
    val farEnd: Float by float("FarEnd", 512F, 0F..512F).onChange {
        maxOf(farStart, it)
    }.markDirtyOnChanged()

    private val ubo = ClientUniformDefine.DISTANCE_FADE.createSingleBuffer()

    private var uboDirty = true
    private fun <T : Any> Value<T>.markDirtyOnChanged() = onChanged { uboDirty = true }

    fun updateIfDirty() {
        if (uboDirty) {
            ubo.writeStd140 {
                putVec4(nearStart, nearEnd, farStart, farEnd)
            }
            uboDirty = false
        }
    }

    fun bindUniform(pass: RenderPass) {
        pass.setUniform(ClientUniformDefine.DISTANCE_FADE.uboName, ubo)
    }

}
