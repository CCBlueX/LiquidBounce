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

import com.mojang.blaze3d.systems.RenderSystem
import it.unimi.dsi.fastutil.objects.ObjectFloatMutablePair
import it.unimi.dsi.fastutil.objects.ObjectFloatPair
import net.ccbluex.fastutil.component1
import net.ccbluex.fastutil.component2
import net.ccbluex.fastutil.mapToArray
import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.render.VertexInputType
import net.ccbluex.liquidbounce.render.drawCustomMesh
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.render.utils.rainbow
import net.minecraft.client.render.Camera
import net.minecraft.client.render.VertexConsumer
import net.minecraft.client.render.VertexFormat.DrawMode
import net.minecraft.entity.Entity
import net.minecraft.util.math.Vec3d
import org.joml.Matrix4f
import org.joml.Vector3f
import org.joml.Vector4f
import java.util.*

/**
 * Breadcrumbs module
 *
 * Leaves traces behind players.
 */
object ModuleBreadcrumbs : ClientModule("Breadcrumbs", Category.RENDER, aliases = listOf("PlayerTrails")) {

    private val onlyOwn by boolean("OnlyOwn", true)
    private val color by color("Color", Color4b(70, 119, 255, 120))
    private val colorRainbow by boolean("Rainbow", false)
    private val height by float("Height", 0.5f, 0f..2f)

    private object TemporaryConfigurable : ToggleableConfigurable(this, "Temporary", true) {
        val alive by int("Alive", 900, 10..10000, "ms")
        val fade by boolean("Fade", true)
    }

    init {
        tree(TemporaryConfigurable)
    }

    private val trails = IdentityHashMap<Entity, Trail>()
    private val lastPositions = IdentityHashMap<Entity, Vec3d>()

    override fun onDisabled() {
        clear()
    }

    val renderHandler = handler<WorldRenderEvent> { event ->
        if (trails.isEmpty()) {
            return@handler
        }

        val matrixStack = event.matrixStack
        val color = if (colorRainbow) rainbow() else color

        renderEnvironmentForWorld(matrixStack) {
            if (height > 0) {
                RenderSystem.disableCull()
            }

            val camera = mc.entityRenderDispatcher.camera ?: return@handler
            val time = System.currentTimeMillis()
            val colorF = Vector4f(color.r / 255f, color.g / 255f, color.b / 255f, color.a / 255f)
            val lines = height == 0f
            drawCustomMesh(
                if (lines) DrawMode.DEBUG_LINES else DrawMode.QUADS,
                VertexInputType.PosColor,
            ) { matrix ->
                val renderData = RenderData(matrix, this, colorF, lines)
                trails.forEach { (entity, trail) ->
                    trail.verifyAndRenderTrail(renderData, camera, entity, time)
                }
            }

            if (height > 0) {
                RenderSystem.enableCull()
            }
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
        if (last != null && entity.x == last.x && entity.y == last.y && entity.z == last.z) {
            return
        }

        lastPositions[entity] = Vec3d(entity.x, entity.y, entity.z)
        trails.getOrPut(entity, ::Trail).positions.add(TrailPart(entity.x, entity.y, entity.z, time))
    }

    @Suppress("unused")
    private val worldChangeHandler = handler<WorldChangeEvent> {
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
        val bufferBuilder: VertexConsumer,
        val color: Vector4f,
        val lines: Boolean
    )

    private class Trail {

        val positions = ArrayDeque<TrailPart>()

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
            val pointsWithAlpha = positions.mapToArray { position ->
                val alpha = if (shouldFade) {
                    val deltaTime = time - position.creationTime
                    val multiplier = (1F - deltaTime.toFloat() / aliveDurationF)
                    multiplier * initialAlpha
                } else {
                    initialAlpha
                }

                val point = calculatePoint(camera, position.x, position.y, position.z)
                ObjectFloatMutablePair.of(point, alpha)
            }

            val interpolatedPos = entity.getLerpedPos(mc.renderTickCounter.getTickDelta(true))
            val point = calculatePoint(camera, interpolatedPos.x, interpolatedPos.y, interpolatedPos.z)
            pointsWithAlpha.last().left(point)

            addVerticesToBuffer(renderData, pointsWithAlpha)
        }

        private fun calculatePoint(camera: Camera, x: Double, y: Double, z: Double): Vector3f {
            val point = Vector3f(x.toFloat(), y.toFloat(), z.toFloat())
            point.sub(camera.pos.x.toFloat(), camera.pos.y.toFloat(), camera.pos.z.toFloat())
            return point
        }

        private fun addVerticesToBuffer(renderData: RenderData, list: Array<out ObjectFloatPair<Vector3f>>) {
            val red = renderData.color.x
            val green = renderData.color.y
            val blue = renderData.color.z

            with(renderData.bufferBuilder) {
                for (i in 1..<list.size) {
                    val (v0, alpha0) = list[i]
                    val (v2, alpha2) = list[i - 1]

                    vertex(renderData.matrix, v0.x, v0.y, v0.z).color(red, green, blue, alpha0)
                    vertex(renderData.matrix, v2.x, v2.y, v2.z).color(red, green, blue, alpha2)
                    if (!renderData.lines) {
                        vertex(renderData.matrix, v2.x, v2.y + height, v2.z).color(red, green, blue, alpha2)
                        vertex(renderData.matrix, v0.x, v0.y + height, v0.z).color(red, green, blue, alpha0)
                    }
                }
            }
        }

    }

}
