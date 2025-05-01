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
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.injection.mixins.minecraft.render.MixinGameRenderer
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.shader.shaders.OutlineEffectShaderData

/**
 * Module ItemChams
 *
 * Applies visual effects to your held items.
 *
 * [MixinGameRenderer]
 *
 * @author ccetl
 */
object ModuleItemChams : ClientModule("ItemChams", Category.RENDER) {

    private val blendColor by color("BlendColor", Color4b(0, 64, 255, 186))
    private val alpha by int("Alpha", 95, 1..255)
    private val glowColor by color("GlowColor", Color4b(0, 64, 255, 15))
    private val layers by int("Layers", 3, 1..10)
    private val layerSize by float("LayerSize", 1.91f, 1f..5f)
    private val falloff by float("Falloff", 6.83f, 0f..20f)

    var active = false

    fun setData() {
        active = true
        with(OutlineEffectShaderData) {
            falloff = ModuleItemChams.falloff
            sampleMul = layerSize
            layerCount = layers
            glowColor = ModuleItemChams.glowColor
            blendColor = ModuleItemChams.blendColor
            alpha = ModuleItemChams.alpha / 255f
        }
    }

}
