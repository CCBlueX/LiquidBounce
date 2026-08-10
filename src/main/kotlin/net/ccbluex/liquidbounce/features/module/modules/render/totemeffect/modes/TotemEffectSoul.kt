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

package net.ccbluex.liquidbounce.features.module.modules.render.totemeffect.modes

import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.features.module.modules.render.hats.modes.HatsHalo.addTorusQuad
import net.ccbluex.liquidbounce.features.module.modules.render.hats.modes.HatsHalo.getAngle
import net.ccbluex.liquidbounce.features.module.modules.render.hats.modes.HatsHalo.getNextAngle
import net.ccbluex.liquidbounce.features.module.modules.render.totemeffect.TotemEffectColorSettings
import net.ccbluex.liquidbounce.features.module.modules.render.totemeffect.TotemEffectMode
import net.ccbluex.liquidbounce.render.ClientRenderPipelines
import net.ccbluex.liquidbounce.render.WorldRenderEnvironment
import net.ccbluex.liquidbounce.render.drawCustomMesh
import net.ccbluex.liquidbounce.render.withPositionRelativeToCamera
import net.ccbluex.liquidbounce.render.withPush
import net.ccbluex.liquidbounce.utils.render.WireframePlayer
import net.minecraft.world.phys.Vec3

internal object TotemEffectSoul : TotemEffectMode("Soul") {

    private val colors = TotemEffectColorSettings()
    private val soulYmotion by float("SoulYMotion", 5f, 0.1f..10f)

    private val wireframePlayer = WireframePlayer()

    init {
        tree(colors)
    }

    override fun WorldRenderEnvironment.drawTotemEffect(progress: Float, pos: Vec3, event: WorldRenderEvent) {
        val fadeProgress = if (progress >= fade) (progress - fade) / (1f - fade).coerceAtLeast(0.001f) else 0f
        val alpha = ((1f - fadeProgress).coerceIn(0f, 1f) * 255f).toInt()

        if (alpha <= 0) return

        val inner = colors.innerColor.alpha(alpha)
        val outer = if (colors.sync) inner else colors.outerColor.alpha(alpha)

        val targetPos = pos.add(0.0, (progress * soulYmotion).toDouble(), -0.1)

        wireframePlayer.pos = targetPos
        wireframePlayer.render(event, color = inner, outlineColor = outer)

        withPositionRelativeToCamera(targetPos.add(0.0, 2.1, 0.125)) {
            poseStack.withPush {

                // Pasted from HatsHalo.kt
                drawCustomMesh(ClientRenderPipelines.triangles(noDepthTest = true)) { matrix ->
                    val outerSegments = 144
                    val innerSegments = 12

                    for (outerI in 0 until outerSegments) {

                        val outerCurAngleTorus = getAngle(outerI, outerSegments)
                        val outerNextAngleTorus = getNextAngle(outerI, outerSegments)

                        for (innerI in 0 until innerSegments) {
                            addTorusQuad(
                                matrix,
                                innerSegments,
                                outerCurAngleTorus,
                                outerNextAngleTorus,
                                0.3f,
                                0.3f,
                                0.0375f,
                                innerI,
                                inner,
                            )
                        }
                    }
                }
            }
        }
    }
}
