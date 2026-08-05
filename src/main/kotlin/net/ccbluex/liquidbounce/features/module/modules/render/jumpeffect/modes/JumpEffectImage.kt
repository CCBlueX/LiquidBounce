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

package net.ccbluex.liquidbounce.features.module.modules.render.jumpeffect.modes

import com.mojang.math.Axis
import net.ccbluex.fastutil.toEnumSet
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.config.utils.TextureMode
import net.ccbluex.liquidbounce.features.module.modules.render.jumpeffect.JumpEffectColorSettings
import net.ccbluex.liquidbounce.features.module.modules.render.jumpeffect.JumpEffectMode
import net.ccbluex.liquidbounce.render.AnchorPoint
import net.ccbluex.liquidbounce.render.WorldRenderEnvironment
import net.ccbluex.liquidbounce.render.drawSquareTexture
import net.ccbluex.liquidbounce.render.withPush
import net.ccbluex.liquidbounce.utils.render.asTexture
import net.ccbluex.liquidbounce.utils.render.readNativeImage

internal object JumpEffectImage : JumpEffectMode("Image") {

    private val colors = JumpEffectColorSettings()

    private val textureMode = modes("Source", 0) {
        arrayOf(
            TextureMode.Builtin(it, PresetTexture.LIQUIDBOUNCE, PresetTexture.entries.toEnumSet()),
            TextureMode.Custom(it),
        )
    }

    init {
        tree(colors)
    }

    override fun WorldRenderEnvironment.drawJumpEffect(progress: Float) {
        val texture = textureMode.activeMode.texture ?: return
        poseStack.withPush {
            mulPose(Axis.XP.rotationDegrees(90f))
            drawSquareTexture(texture, endRadius.endInclusive * progress, colors.innerColor.argb, AnchorPoint.CENTER)
        }
    }

    private enum class PresetTexture(override val tag: String, val path: String) : TextureMode.Builtin.Preset {
        LIQUIDBOUNCE("LiquidBounce", "jump_effect/liquidbounce.png");

        override val texture = LiquidBounce.resource(this.path)
            .readNativeImage().asTexture { "JumpEffect Image $tag" }
    }
}
