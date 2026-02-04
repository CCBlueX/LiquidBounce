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

package net.ccbluex.liquidbounce.features.module.modules.render.hats.modes

import net.ccbluex.liquidbounce.config.types.toTextureProperty
import net.ccbluex.liquidbounce.features.module.modules.render.hats.HatsMode
import net.ccbluex.liquidbounce.render.WorldRenderEnvironment
import net.ccbluex.liquidbounce.render.drawTexQuad
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.withPush
import net.minecraft.util.Mth
import org.joml.Quaternionf
import org.joml.Vector2f

internal object HatsImage : HatsMode("Image") {

    private val image by file("Image").toTextureProperty(this, printErrorToChat = true)
    private val colorModulator by color("ColorModulator", Color4b.WHITE)
    private val scale by vec2f("Scale", Vector2f(1f, 1f))
    private val spinSpeed by float("SpinSpeed", 1f, -10f..10f)

    private val ROTATION = Quaternionf()

    override fun WorldRenderEnvironment.drawHat(isHurt: Boolean) {
        val texture = image ?: return

        matrixStack.withPush {
            mulPose(
                ROTATION.scaling(1f)
                    .rotateX(Mth.HALF_PI)
                    .rotateZ(getRotationAngle(spinSpeed))
            )
            scale(scale.x(), scale.y(), 1f)

            drawTexQuad(texture, colorModulator.argb)
        }
    }
}
