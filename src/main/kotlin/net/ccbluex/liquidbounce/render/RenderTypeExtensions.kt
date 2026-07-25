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
package net.ccbluex.liquidbounce.render

import net.ccbluex.liquidbounce.injection.mixins.minecraft.render.MixinRenderSetupAccessor
import net.minecraft.client.renderer.rendertype.OutputTarget
import net.minecraft.client.renderer.rendertype.RenderSetup

fun RenderSetup.withOutputTarget(outputTarget: OutputTarget): RenderSetup {
    this as MixinRenderSetupAccessor

    return MixinRenderSetupAccessor.`liquid_bounce$invokeInit`(
        this.getPipeline(),
        this.getTextures(),
        this.getUseLightmap(),
        this.getUseOverlay(),
        this.getLayeringTransform(),
        outputTarget,
        this.getTextureTransform(),
        this.getOutlineProperty(),
        this.getAffectsCrumbling(),
        this.getSortOnUpload()
    )
}
