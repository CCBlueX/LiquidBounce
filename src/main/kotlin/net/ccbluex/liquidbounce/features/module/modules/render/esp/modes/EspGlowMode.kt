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
package net.ccbluex.liquidbounce.features.module.modules.render.esp.modes

import net.ccbluex.liquidbounce.features.module.modules.render.esp.glow.AuraGlow
import net.ccbluex.liquidbounce.features.module.modules.render.esp.glow.ChromaticGlow
import net.ccbluex.liquidbounce.features.module.modules.render.esp.glow.GlowPostEffect
import net.ccbluex.liquidbounce.features.module.modules.render.esp.glow.GlowStyle
import net.ccbluex.liquidbounce.features.module.modules.render.esp.glow.HalftoneGlow
import net.ccbluex.liquidbounce.features.module.modules.render.esp.glow.HologramGlow
import net.ccbluex.liquidbounce.features.module.modules.render.esp.glow.NeonGlow
import net.ccbluex.liquidbounce.features.module.modules.render.esp.glow.OutlineGlow
import net.ccbluex.liquidbounce.features.module.modules.render.esp.glow.RainbowGlow
import net.ccbluex.liquidbounce.features.module.modules.render.esp.glow.RippleGlow
import net.ccbluex.liquidbounce.features.module.modules.render.esp.glow.SmoothGlow
import net.ccbluex.liquidbounce.features.module.modules.render.esp.glow.VanillaGlow

object EspGlowMode : EspMode("Glow", requiresTrueSight = true) {

    val styles = modes("Style", SmoothGlow, arrayOf<GlowStyle>(
        VanillaGlow,
        SmoothGlow,
        OutlineGlow,
        NeonGlow,
        RainbowGlow,
        RippleGlow,
        HologramGlow,
        HalftoneGlow,
        ChromaticGlow,
        AuraGlow,
    ))

    init {
        for (style in styles.modes) {
            for (value in style.containedValues) {
                value.onChanged { GlowPostEffect.invalidate() }
            }
        }
    }

}
