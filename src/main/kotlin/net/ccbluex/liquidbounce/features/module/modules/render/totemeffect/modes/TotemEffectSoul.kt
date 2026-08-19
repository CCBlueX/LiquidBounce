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

import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.features.module.modules.render.totemeffect.ModuleTotemEffect.TotemPopSnapshot
import net.ccbluex.liquidbounce.features.module.modules.render.totemeffect.TotemEffectColorSettings
import net.ccbluex.liquidbounce.features.module.modules.render.totemeffect.TotemEffectMode
import net.ccbluex.liquidbounce.render.ClientRenderPipelines
import net.ccbluex.liquidbounce.render.WorldRenderEnvironment
import net.ccbluex.liquidbounce.render.addTorusQuad
import net.ccbluex.liquidbounce.render.drawCustomMesh
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.segmentAngle
import net.ccbluex.liquidbounce.render.withPositionRelativeToCamera
import net.ccbluex.liquidbounce.utils.render.WireframePlayer
import net.minecraft.world.phys.Vec3

internal object TotemEffectSoul : TotemEffectMode("Soul") {

    private val colors = TotemEffectColorSettings()
    private val toggleHalo by boolean("Halo", false)
    private object YMotion : ToggleableValueGroup(this, "YMotion", true) {
        val soulYMotion by float("SoulYMotion", 0.75f, 0.1f..10f)
        val animBy by enumChoice("AnimBy", AnimBy.AGE)
    }

    private val wireframePlayer = WireframePlayer()

    init {
        tree(YMotion)
        tree(colors)
    }

    override fun WorldRenderEnvironment.drawTotemEffect(
        progress: Float, age: Float, entity: TotemPopSnapshot, fade: Float
    ) {
        val alpha = ((1f - fade).coerceIn(0f, 1f) * 255f).toInt()

        if (alpha <= 0) return

        val inner = colors.innerColor.alpha(alpha)
        val outer = if (colors.sync) inner else colors.outerColor.alpha(alpha)

        val anim = if (YMotion.enabled) {
            when (YMotion.animBy) {
                AnimBy.PROGRESS -> progress.toDouble()
                AnimBy.AGE -> age.toDouble()
            }
        } else { 0.0 }

        val targetPos = entity.pos.add(0.0, (anim * YMotion.soulYMotion), -0.1)

        wireframePlayer.pos = targetPos
        wireframePlayer.xRot = entity.xRot
        wireframePlayer.yRot = entity.yRot
        wireframePlayer.render(color = inner, outlineColor = outer, noDepthTest = !canBeCovered)

        if (toggleHalo) drawHalo(targetPos, inner)
    }

    private fun WorldRenderEnvironment.drawHalo(targetPos: Vec3, color: Color4b) {
        withPositionRelativeToCamera(targetPos.add(0.0, 2.1, 0.125)) {
            drawCustomMesh(ClientRenderPipelines.quads(noDepthTest = !canBeCovered)) { matrix ->
                val outerSegments = 144
                val innerSegments = 12

                for (outerI in 0 until outerSegments) {
                    val outerCurAngleTorus = segmentAngle(outerI, outerSegments)
                    val outerNextAngleTorus = segmentAngle(outerI + 1, outerSegments)

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
                            color,
                        )
                    }
                }
            }
        }
    }

    private enum class AnimBy(
        override val tag: String,
    ) : Tagged {
        PROGRESS("Progress"),
        AGE("Age");
    }
}
