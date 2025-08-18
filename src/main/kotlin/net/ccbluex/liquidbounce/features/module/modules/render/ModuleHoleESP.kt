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

import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.render.*
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.block.Region.Companion.getBox
import net.ccbluex.liquidbounce.utils.block.hole.Hole
import net.ccbluex.liquidbounce.utils.block.hole.HoleManager
import net.ccbluex.liquidbounce.utils.block.hole.HoleManagerSubscriber
import net.ccbluex.liquidbounce.utils.block.hole.HoleTracker
import net.ccbluex.liquidbounce.utils.math.toVec3d
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Vec3d
import kotlin.math.abs
import kotlin.math.max

/**
 * HoleESP module
 *
 * Detects and displays safe spots for Crystal PvP.
 */
object ModuleHoleESP : ClientModule("HoleESP", Category.RENDER), HoleManagerSubscriber {

    private val modes = choices("Mode", GlowingPlane, arrayOf(BoxChoice, GlowingPlane))

    private val horizontalDistance by int("HorizontalScanDistance", 32, 4..128)
    private val verticalDistance by int("VerticalScanDistance", 8, 4..128)

    private val distanceFade by float("DistanceFade", 0.3f, 0f..1f)

    private val colorBedrock by color("1x1Bedrock", Color4b(0x19c15c))
    private val color1by1 by color("1x1", Color4b(0xf7381b))
    private val color1by2 by color("1x2", Color4b(0x35bacc))
    private val color2by2 by color("2x2", Color4b(0xf7cf1b))

    override fun horizontalDistance(): Int = horizontalDistance
    override fun verticalDistance(): Int = verticalDistance

    override fun onEnabled() {
        HoleManager.subscribe(this)
    }

    override fun onDisabled() {
        HoleManager.unsubscribe(this)
    }

    private object BoxChoice : Choice("Box") {

        override val parent: ChoiceConfigurable<Choice>
            get() = modes

        private val outline by boolean("Outline", true)

        @Suppress("unused")
        val renderHandler = handler<WorldRenderEvent> { event ->
            val pos = player.blockPos
            val vDistance = verticalDistance
            val hDistance = horizontalDistance

            renderEnvironmentForWorld(event.matrixStack) {
                HoleTracker.holes.forEach {
                    val positions = it.positions

                    val valOutOfRange = abs(pos.y - positions.from.y) > vDistance
                    val xzOutOfRange = abs(pos.x - positions.from.x) > hDistance ||
                        abs(pos.z - positions.from.z) > hDistance
                    if (valOutOfRange || xzOutOfRange) {
                        return@forEach
                    }

                    val fade = calculateFade(positions.from)
                    val baseColor = it.color().with(a = 50).fade(fade)
                    val box = positions.getBox()
                    withPositionRelativeToCamera(positions.from.toVec3d()) {
                        withColor(baseColor) {
                            drawSolidBox(box)
                        }

                        if (outline) {
                            val outlineColor = it.color().with(a = 100).fade(fade)
                            withColor(outlineColor) {
                                drawOutlinedBox(box)
                            }
                        }
                    }
                }
            }
        }
    }

    private object GlowingPlane : Choice("GlowingPlane") {

        override val parent: ChoiceConfigurable<Choice>
            get() = modes

        private val outline by boolean("Outline", true)

        private val glowHeightSetting by float("GlowHeight", 0.7f, 0f..1f)

        @Suppress("unused")
        val renderHandler = handler<WorldRenderEvent> { event ->
            val glowHeight = glowHeightSetting.toDouble()
            val pos = player.blockPos
            val vDistance = verticalDistance
            val hDistance = horizontalDistance

            renderEnvironmentForWorld(event.matrixStack) {
                withDisabledCull {
                    HoleTracker.holes.forEach {
                        val positions = it.positions

                        val valOutOfRange = abs(pos.y - positions.from.y) > vDistance
                        val xzOutOfRange = abs(pos.x - positions.from.x) > hDistance ||
                            abs(pos.z - positions.from.z) > hDistance
                        if (valOutOfRange || xzOutOfRange) {
                            return@forEach
                        }

                        val fade = calculateFade(positions.from)
                        val baseColor = it.color().with(a = 50).fade(fade)
                        val transparentColor = baseColor.with(a = 0)
                        val box = positions.getBox()
                        withPositionRelativeToCamera(positions.from.toVec3d()) {
                            withColor(baseColor) {
                                drawSideBox(box, Direction.DOWN)
                            }

                            if (outline) {
                                val outlineColor = it.color().with(a = 100).fade(fade)
                                withColor(outlineColor) {
                                    drawSideBox(box, Direction.DOWN, onlyOutline = true)
                                }
                            }

                            drawGradientSides(glowHeight, baseColor, transparentColor, box)
                        }
                    }
                }
            }
        }
    }

    private fun Hole.color() = when {
        type == Hole.Type.ONE_ONE && bedrockOnly -> colorBedrock
        type == Hole.Type.ONE_TWO -> color1by2
        type == Hole.Type.TWO_TWO -> color2by2
        else -> color1by1
    }

    private fun calculateFade(pos: BlockPos): Float {
        if (distanceFade == 0f) {
            return 1f
        }

        val verticalDistanceFraction = (player.pos.y - pos.y) / verticalDistance
        val horizontalDistanceFraction =
            Vec3d(player.pos.x - pos.x, 0.0, player.pos.z - pos.z).length() / horizontalDistance

        val fade = (1 - max(verticalDistanceFraction, horizontalDistanceFraction)) / distanceFade

        return fade.coerceIn(0.0, 1.0).toFloat()
    }

}
