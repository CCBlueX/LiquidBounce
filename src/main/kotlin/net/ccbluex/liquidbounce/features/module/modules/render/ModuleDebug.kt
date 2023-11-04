/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2023 CCBlueX
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

import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.MovementInputEvent
import net.ccbluex.liquidbounce.event.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleBlink
import net.ccbluex.liquidbounce.render.*
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.render.engine.Vec3
import net.ccbluex.liquidbounce.utils.entity.SimulatedPlayer
import net.ccbluex.liquidbounce.utils.math.geometry.Line
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d
import java.awt.Color

/**
 * Rotations module
 *
 * Allows you to see server-sided rotations.
 */

object ModuleDebug : Module("Debug", Category.RENDER) {

    object RenderSimulatedPlayer: ToggleableConfigurable(this, "SimulatedPlayer", false) {
        private val ticksToPredict by int("TicksToPredict", 20, 5..100)
        private val simLines = mutableListOf<Vec3>()
        val tickRep =
            handler<MovementInputEvent> { event ->
                // We aren't actually where we are because of blink.
                // So this module shall not cause any disturbance in that case.
                if (ModuleBlink.enabled) {
                    return@handler
                }

                simLines.clear()

                val world = world

                val input =
                    SimulatedPlayer.SimulatedPlayerInput(
                        event.directionalInput,
                        player.input.jumping,
                        player.isSprinting
                    )

                val simulatedPlayer = SimulatedPlayer.fromClientPlayer(input)

                repeat(ticksToPredict) {
                    simulatedPlayer.tick()
                    simLines.add(Vec3(simulatedPlayer.pos))
                }
            }
        val renderHandler = handler<WorldRenderEvent> { event ->
            renderEnvironmentForWorld(event.matrixStack) {
                withColor(Color4b.BLUE) {
                    drawLineStrip(lines = simLines.toTypedArray())
                }
            }
        }
    }
    init {
        tree(RenderSimulatedPlayer)
    }

    private val debuggedGeometry = hashMapOf<DebuggedGeometryOwner, DebuggedGeometry>()


    val renderHandler = handler<WorldRenderEvent> { event ->
        val matrixStack = event.matrixStack

        renderEnvironmentForWorld(matrixStack) {
            debuggedGeometry.values.forEach {
                it.render(this)
            }
        }
    }

    fun debugGeometry(owner: Any, name: String, geometry: DebuggedGeometry) {
        debuggedGeometry[DebuggedGeometryOwner(owner, name)] = geometry
    }

    data class DebuggedGeometryOwner(val owner: Any, val name: String)

    fun getArrayEntryColor(idx: Int, length: Int): Color4b {
        val hue = idx.toFloat() / length.toFloat()
        return Color4b(Color.getHSBColor(hue, 1f, 1f)).alpha(32)
    }

    abstract class DebuggedGeometry(val color: Color4b) {
        abstract fun render(env: RenderEnvironment)
    }

    class DebuggedLine(line: Line, color: Color4b) : DebuggedGeometry(color) {
        val from: Vec3
        val to: Vec3

        init {
            val normalizedDirection = line.direction.normalize()

            this.from = Vec3(line.position.subtract(normalizedDirection.multiply(100.0)))
            this.to = Vec3(line.position.add(normalizedDirection.multiply(100.0)))
        }

        override fun render(env: RenderEnvironment) {
            env.withColor(color) {
                this.drawLineStrip(from, to)
            }
        }
    }

    class DebuggedLineSegment(val from: Vec3, val to: Vec3, color: Color4b) : DebuggedGeometry(color) {
        override fun render(env: RenderEnvironment) {
            env.withColor(color) {
                this.drawLineStrip(from, to)
            }
        }
    }

    open class DebuggedBox(val box: Box, color: Color4b) : DebuggedGeometry(color) {
        override fun render(env: RenderEnvironment) {
            env.withColor(color) {
                this.drawSolidBox(box)
            }
        }
    }

    class DebuggedPoint(point: Vec3d, color: Color4b, size: Double = 0.2) : DebuggedBox(
        Box.of(point, size, size, size),
        color
    )

    class DebugCollection(val geometry: List<DebuggedGeometry>) : DebuggedGeometry(Color4b.WHITE) {
        override fun render(env: RenderEnvironment) {
            this.geometry.forEach { it.render(env) }
        }
    }
}
