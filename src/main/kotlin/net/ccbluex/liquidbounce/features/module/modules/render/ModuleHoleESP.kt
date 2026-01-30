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
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.config.types.group.Mode
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.render.drawBox
import net.ccbluex.liquidbounce.render.drawBoxSide
import net.ccbluex.liquidbounce.render.drawGradientSides
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.render.withPositionRelativeToCamera
import net.ccbluex.liquidbounce.utils.block.hole.Hole
import net.ccbluex.liquidbounce.utils.block.hole.HoleManager
import net.ccbluex.liquidbounce.utils.block.hole.HoleManagerSubscriber
import net.ccbluex.liquidbounce.utils.block.hole.HoleTracker
import net.ccbluex.liquidbounce.utils.math.box
import net.ccbluex.liquidbounce.utils.math.from
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.phys.Vec3
import kotlin.math.abs
import kotlin.math.max

/**
 * HoleESP module
 *
 * Detects and displays safe spots for Crystal PvP.
 */
object ModuleHoleESP : ClientModule("HoleESP", ModuleCategories.RENDER), HoleManagerSubscriber {

    private val modes = choices("Mode", GlowingPlane, arrayOf(BoxMode, GlowingPlane))

    private val horizontalDistance by int("HorizontalScanDistance", 32, 4..128)
    private val verticalDistance by int("VerticalScanDistance", 8, 4..128)

    private val distanceFade by float("DistanceFade", 0.3f, 0f..1f)

    private val colorBedrock by color("1x1Bedrock", Color4b.fullAlpha(0x19c15c))
    private val color1by1 by color("1x1", Color4b.fullAlpha(0xf7381b))
    private val color1by2 by color("1x2", Color4b.fullAlpha(0x35bacc))
    private val color2by2 by color("2x2", Color4b.fullAlpha(0xf7cf1b))

    override fun horizontalDistance(): Int = horizontalDistance
    override fun verticalDistance(): Int = verticalDistance

    override fun onEnabled() {
        HoleManager.subscribe(this)
    }

    override fun onDisabled() {
        HoleManager.unsubscribe(this)
    }

    private object BoxMode : Mode("Box") {

        override val parent: ModeValueGroup<Mode>
            get() = modes

        private val outline by boolean("Outline", true)

        @Suppress("unused")
        val renderHandler = handler<WorldRenderEvent> { event ->
            val pos = player.blockPosition()
            val vDistance = verticalDistance
            val hDistance = horizontalDistance

            renderEnvironmentForWorld(event.matrixStack) {
                startBatch()
                HoleTracker.holes.forEach {
                    val positions = it.positions

                    val valOutOfRange = abs(pos.y - positions.minY()) > vDistance
                    val xzOutOfRange = abs(pos.x - positions.minX()) > hDistance ||
                        abs(pos.z - positions.minZ()) > hDistance
                    if (valOutOfRange || xzOutOfRange) {
                        return@forEach
                    }

                    val fade = calculateFade(positions.from)
                    val baseColor = it.color().with(a = 50).fade(fade)
                    withPositionRelativeToCamera(positions.from) {
                        drawBox(
                            positions.box,
                            baseColor,
                            if (outline) baseColor.with(a = 100).fade(fade) else null,
                        )
                    }
                }
                commitBatch()
            }
        }
    }

    private object GlowingPlane : Mode("GlowingPlane") {

        override val parent: ModeValueGroup<Mode>
            get() = modes

        private val outline by boolean("Outline", true)

        private val glowHeightSetting by float("GlowHeight", 0.7f, 0f..1f)

        @Suppress("unused")
        val renderHandler = handler<WorldRenderEvent> { event ->
            val glowHeight = glowHeightSetting.toDouble()
            val pos = player.blockPosition()
            val vDistance = verticalDistance
            val hDistance = horizontalDistance

            renderEnvironmentForWorld(event.matrixStack) {
                HoleTracker.holes.forEach {
                    val positions = it.positions

                    val valOutOfRange = abs(pos.y - positions.minY()) > vDistance
                    val xzOutOfRange = abs(pos.x - positions.minX()) > hDistance ||
                        abs(pos.z - positions.minZ()) > hDistance
                    if (valOutOfRange || xzOutOfRange) {
                        return@forEach
                    }

                    val fade = calculateFade(positions.from)
                    val baseColor = it.color().with(a = 50).fade(fade)
                    val transparentColor = baseColor.with(a = 0)
                    val box = positions.box
                    withPositionRelativeToCamera(positions.from) {
                        drawBoxSide(
                            box,
                            Direction.DOWN,
                            baseColor,
                            if (outline) baseColor.with(a = 100).fade(fade) else null,
                        )
                        drawGradientSides(glowHeight, baseColor, transparentColor, box)
                    }
                }
            }
        }
    }

    private fun Hole.color() = when (type) {
        Hole.Type.ONE_ONE if bedrockOnly -> colorBedrock
        Hole.Type.ONE_TWO -> color1by2
        Hole.Type.TWO_TWO -> color2by2
        else -> color1by1
    }

    private fun calculateFade(pos: BlockPos): Float {
        if (distanceFade == 0f) {
            return 1f
        }

        val verticalDistanceFraction = (player.position().y - pos.y) / verticalDistance
        val horizontalDistanceFraction =
            Vec3(player.position().x - pos.x, 0.0, player.position().z - pos.z).length() / horizontalDistance

        val fade = (1 - max(verticalDistanceFraction, horizontalDistanceFraction)) / distanceFade

        return fade.coerceIn(0.0, 1.0).toFloat()
    }

}
