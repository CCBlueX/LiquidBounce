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
package net.ccbluex.liquidbounce.features.module.modules.world.autofarm

import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.render.*
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.engine.type.Vec3
import net.ccbluex.liquidbounce.render.utils.rainbow
import net.ccbluex.liquidbounce.utils.entity.interpolateCurrentPosition
import net.ccbluex.liquidbounce.utils.math.sq
import net.ccbluex.liquidbounce.utils.math.toVec3
import net.ccbluex.liquidbounce.utils.math.toVec3d
import net.minecraft.util.math.Direction

object AutoFarmVisualizer : ToggleableConfigurable(ModuleAutoFarm, "Visualize", true) {
    private object Path : ToggleableConfigurable(this, "Path", true) {
        val color by color("PathColor", Color4b(36, 237, 0, 255))

        val renderHandler = handler<WorldRenderEvent> { event ->
            renderEnvironmentForWorld(event.matrixStack) {
                withColor(color) {
                    AutoFarmAutoWalk.walkTarget?.let { target ->
                        drawLines(
                            relativeToCamera(player.interpolateCurrentPosition(event.partialTicks)).toVec3(),
                            relativeToCamera(target).toVec3()
                        )
                    }
                }
            }
        }
    }

    private object Blocks : ToggleableConfigurable(this, "Blocks", true) {
        val outline by boolean("Outline", true)

        private val readyColor by color("ReadyColor", Color4b(36, 237, 0, 255))
        private val placeColor by color("PlaceColor", Color4b(191, 245, 66, 100))
        private val range by int("Range", 50, 10..128).onChange {
            rangeSquared = it.sq()
            it
        }

        private var rangeSquared: Int = range * range

        private val colorRainbow by boolean("Rainbow", false)

        private object CurrentTarget : ToggleableConfigurable(this.parent, "CurrentTarget", true) {
            private val color by color("Color", Color4b(66, 120, 245, 255))
            private val colorRainbow by boolean("Rainbow", false)

            fun render(renderEnvironment: RenderEnvironment) {
                if (!this.enabled) return
                val target = ModuleAutoFarm.currentTarget ?: return
                with(renderEnvironment) {
                    withPosition(Vec3(target)) {
                        withColor((if (colorRainbow) rainbow() else color).with(a = 50)) {
                            drawSolidBox(FULL_BOX)
                        }
                    }
                }
            }
        }


        val renderHandler = handler<WorldRenderEvent> { event ->
            val matrixStack = event.matrixStack
            val baseColor = if (colorRainbow) rainbow() else readyColor

            val fillColor = baseColor.with(a = 50)
            val outlineColor = baseColor.with(a = 100)

            renderEnvironmentForWorld(matrixStack) {
                CurrentTarget.render(this)
                for ((pos, type) in AutoFarmBlockTracker.iterate()) {
                    if ((pos.x - player.x).sq() + (pos.z - player.z).sq() > rangeSquared) continue

                    withPositionRelativeToCamera(pos.toVec3d()) {
                        if (type == AutoFarmTrackedStates.Destroy) {
                            withColor(fillColor) {
                                drawSolidBox(FULL_BOX)
                            }
                        } else {
                            withColor(placeColor) {
                                drawSideBox(FULL_BOX, Direction.UP)
                            }

                        }

                        if (outline && type == AutoFarmTrackedStates.Destroy) {
                            withColor(outlineColor) {
                                drawOutlinedBox(FULL_BOX)
                            }
                        }
                    }
                }
            }
        }
    }

    init {
        tree(Path)
        tree(Blocks)
    }
}
