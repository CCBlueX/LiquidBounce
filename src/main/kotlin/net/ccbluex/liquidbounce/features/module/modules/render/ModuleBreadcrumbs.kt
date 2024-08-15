/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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

import com.mojang.blaze3d.systems.RenderSystem
import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.render.utils.rainbow
import net.minecraft.client.render.*
import net.minecraft.client.render.VertexFormat.DrawMode
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.entity.Entity
import org.apache.commons.lang3.tuple.MutablePair
import org.apache.commons.lang3.tuple.Pair
import org.joml.Matrix4f
import org.joml.Vector3f
import org.joml.Vector4f
import java.util.*

/**
 * Breadcrumbs module
 *
 * Leaves traces behind players.
 */
object ModuleBreadcrumbs : Module("Breadcrumbs", Category.RENDER, aliases = arrayOf("PlayerTrails")) {

    private val onlyOwn by boolean("OnlyOwn", true)
    private val color by color("Color", Color4b(70, 119, 255, 120))
    private val colorRainbow by boolean("Rainbow", false)
    private val height by float("Height", 0.5f, 0f..2f)

    internal object TemporaryConfigurable : ToggleableConfigurable(this, "Temporary", true) {
        val alive by int("Alive", 900, 10..10000, "ms")
        val fade by boolean("Fade", true)
    }

    init {
        tree(TemporaryConfigurable)
    }

    private val trails = IdentityHashMap<Entity, Trail>()
    private val lastPositions = IdentityHashMap<Entity, DoubleArray>()

    override fun disable() {
        clear()
    }

    val renderHandler = handler<WorldRenderEvent> { event ->
        val matrixStack = event.matrixStack
        val color = if (colorRainbow) rainbow() else color

        renderEnvironmentForWorld(matrixStack) {
            draw(matrixStack, color)
        }
    }

    private fun draw(matrixStack: MatrixStack, color: Color4b) {
        if (trails.isEmpty()) {
            return
        }

        if (height > 0) {
            RenderSystem.disableCull()
        }

        val matrix = matrixStack.peek().positionMatrix

        @Suppress("SpellCheckingInspection")
        val tessellator = RenderSystem.renderThreadTesselator()
        val camera = mc.entityRenderDispatcher.camera ?: return
        val time = System.currentTimeMillis()
        val colorF = Vector4f(color.r / 255f, color.g / 255f, color.b / 255f, color.a / 255f)
        val lines = height == 0f
        val buffer = tessellator.begin(if (lines) DrawMode.DEBUG_LINES else DrawMode.QUADS,
            VertexFormats.POSITION_COLOR)
        val renderData = RenderData(matrix, buffer, colorF, lines)

        RenderSystem.setShader { GameRenderer.getPositionColorProgram() }

        trails.forEach { (entity, trail) ->
            trail.verifyAndRenderTrail(renderData, camera, entity, time)
        }

        BufferRenderer.drawWithGlobalProgram(buffer.endNullable() ?: return)

        if (height > 0) {
            RenderSystem.enableCull()
        }
    }

    /**
     * Updates all trails.
     */
    @Suppress("unused")
    val updateHandler = handler<GameTickEvent> {
        val time = System.currentTimeMillis()

        if (onlyOwn) {
            updateEntityTrail(time, player)
            trails.keys.retainAll { it === player || !it.isAlive }
            return@handler
        }

        val actualPresent = world.players
        actualPresent.forEach { player -> updateEntityTrail(time, player) }
        trails.keys.removeIf { key ->
            actualPresent.none { it === key } || !key.isAlive
        }
    }

    private fun updateEntityTrail(time: Long, entity: Entity) {
        val last = lastPositions[entity]
        if (last != null && entity.x == last[0] && entity.y == last[1] && entity.z == last[2]) {
            return
        }

        lastPositions[entity] = doubleArrayOf(entity.x, entity.y, entity.z)
        trails.computeIfAbsent(entity) { Trail() }.positions.add(TrailPart(entity.x, entity.y, entity.z, time))
    }

    @Suppress("unused")
    val worldChangeHandler = handler<WorldChangeEvent>(ignoreCondition = true) {
        clear()
    }

    private fun clear() {
        lastPositions.clear()
        trails.clear()
    }

    @JvmRecord
    private data class TrailPart(val x: Double, val y: Double, val z: Double, val creationTime: Long)

    private class RenderData(
        val matrix: Matrix4f,
        val bufferBuilder: BufferBuilder,
        val color: Vector4f,
        val lines: Boolean
    )

    private class Trail {

        var positions = ArrayDeque<TrailPart>()

        fun verifyAndRenderTrail(renderData: RenderData, camera: Camera, entity: Entity, time: Long) {
            val aliveDurationF = TemporaryConfigurable.alive.toFloat()
            val initialAlpha = renderData.color.w

            if (TemporaryConfigurable.enabled) {
                val aliveDuration = TemporaryConfigurable.alive.toLong()
                val expirationTime = time - aliveDuration

                // Remove outdated positions, the positions are ordered by time (ascending)
                while (positions.isNotEmpty() && positions.peekFirst().creationTime < expirationTime) {
                    positions.removeFirst()
                }
            }

            if (positions.isEmpty()) {
                return
            }

            val shouldFade = TemporaryConfigurable.fade && TemporaryConfigurable.enabled
            val pointsWithAlpha = positions.map { position ->
                val alpha = if (shouldFade) {
                    val deltaTime = time - position.creationTime
                    val multiplier = (1F - deltaTime.toFloat() / aliveDurationF)
                    multiplier * initialAlpha
                } else {
                    initialAlpha
                }

                val point = calculatePoint(camera, position.x, position.y, position.z)
                MutablePair(point, alpha)
            }

            val interpolatedPos = entity.getLerpedPos(mc.renderTickCounter.getTickDelta(true))
            val point = calculatePoint(camera, interpolatedPos.x, interpolatedPos.y, interpolatedPos.z)
            pointsWithAlpha.last().left = point

            addVerticesToBuffer(renderData, pointsWithAlpha)
        }

        private fun calculatePoint(camera: Camera, x: Double, y: Double, z: Double): Vector3f {
            val point = Vector3f(x.toFloat(), y.toFloat(), z.toFloat())
            point.sub(camera.pos.x.toFloat(), camera.pos.y.toFloat(), camera.pos.z.toFloat())
            return point
        }

        private fun addVerticesToBuffer(renderData: RenderData, list: List<Pair<Vector3f, Float>>) {
            val red = renderData.color.x
            val green = renderData.color.y
            val blue = renderData.color.z

            for (i in list.indices) {
                if (i - 1 < 0) {
                    continue
                }

                val v0 = list[i]
                val v2 = list[i - 1]

                renderData.bufferBuilder.vertex(renderData.matrix, v0.left.x, v0.left.y, v0.left.z)
                    .color(red, green, blue, v0.right)
                renderData.bufferBuilder.vertex(renderData.matrix, v2.left.x, v2.left.y, v2.left.z)
                    .color(red, green, blue, v2.right)
                if (!renderData.lines) {
                    renderData.bufferBuilder.vertex(renderData.matrix, v2.left.x, v2.left.y + height, v2.left.z)
                        .color(red, green, blue, v2.right)
                    renderData.bufferBuilder.vertex(renderData.matrix, v0.left.x, v0.left.y + height, v0.left.z)
                        .color(red, green, blue, v0.right)
                }
            }
        }

    }

}
