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

import com.mojang.blaze3d.vertex.VertexConsumer
import it.unimi.dsi.fastutil.objects.ObjectFloatMutablePair
import it.unimi.dsi.fastutil.objects.ObjectFloatPair
import net.ccbluex.fastutil.component1
import net.ccbluex.fastutil.component2
import net.ccbluex.fastutil.mapToArray
import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.render.ClientRenderPipelines
import net.ccbluex.liquidbounce.render.addVertex
import net.ccbluex.liquidbounce.render.drawCustomMesh
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.render.utils.rainbow
import net.ccbluex.liquidbounce.utils.math.copy
import net.minecraft.client.Camera
import net.minecraft.world.entity.Entity
import net.minecraft.world.phys.Vec3
import org.joml.Matrix4fc
import org.joml.Vector3f
import org.joml.Vector4f
import java.util.ArrayDeque
import java.util.IdentityHashMap

/**
 * Breadcrumbs module
 *
 * Leaves traces behind players.
 */
object ModuleBreadcrumbs : ClientModule("Breadcrumbs", ModuleCategories.RENDER, aliases = listOf("PlayerTrails")) {

    private val onlyOwn by boolean("OnlyOwn", true)
    private val color by color("Color", Color4b(70, 119, 255, 120))
    private val colorRainbow by boolean("Rainbow", false)
    private val height by float("Height", 0.5f, 0f..2f)

    private object TemporaryValueGroup : ToggleableValueGroup(this, "Temporary", true) {
        val alive by int("Alive", 900, 10..10000, "ms")
        val fade by boolean("Fade", true)
    }

    init {
        tree(TemporaryValueGroup)
    }

    private val trails = IdentityHashMap<Entity, Trail>()
    private val lastPositions = IdentityHashMap<Entity, Vec3>()

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
            val camera = mc.entityRenderDispatcher.camera ?: return@handler
            val time = System.currentTimeMillis()
            val colorF = Vector4f(color.r / 255f, color.g / 255f, color.b / 255f, color.a / 255f)
            val lines = height == 0f
            drawCustomMesh(
                if (lines) ClientRenderPipelines.Lines else ClientRenderPipelines.Quads
            ) { pose ->
                val renderData = RenderData(pose.pose(), this, colorF, lines)
                trails.forEach { (entity, trail) ->
                    trail.verifyAndRenderTrail(renderData, camera, entity, time)
                }
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

        val actualPresent = world.players()
        actualPresent.forEach { player -> updateEntityTrail(time, player) }
        trails.keys.removeIf { key ->
            actualPresent.none { it === key } || !key.isAlive
        }
    }

    private fun updateEntityTrail(time: Long, entity: Entity) {
        val last = lastPositions[entity]
        if (last != null && entity.position() == last) {
            return
        }

        lastPositions[entity] = entity.position().copy()
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
        val pose: Matrix4fc,
        val bufferBuilder: VertexConsumer,
        val color: Vector4f,
        val lines: Boolean
    )

    private class Trail {

        val positions = ArrayDeque<TrailPart>()

        fun verifyAndRenderTrail(renderData: RenderData, camera: Camera, entity: Entity, time: Long) {
            val aliveDurationF = TemporaryValueGroup.alive.toFloat()
            val initialAlpha = renderData.color.w

            if (TemporaryValueGroup.enabled) {
                val aliveDuration = TemporaryValueGroup.alive.toLong()
                val expirationTime = time - aliveDuration

                // Remove outdated positions, the positions are ordered by time (ascending)
                while (positions.isNotEmpty() && positions.peekFirst().creationTime < expirationTime) {
                    positions.removeFirst()
                }
            }

            if (positions.isEmpty()) {
                return
            }

            val shouldFade = TemporaryValueGroup.fade && TemporaryValueGroup.enabled
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

            val interpolatedPos = entity.getPosition(mc.deltaTracker.getGameTimeDeltaPartialTick(true))
            val point = calculatePoint(camera, interpolatedPos.x, interpolatedPos.y, interpolatedPos.z)
            pointsWithAlpha.last().left(point)

            addVerticesToBuffer(renderData, pointsWithAlpha)
        }

        private fun calculatePoint(camera: Camera, x: Double, y: Double, z: Double): Vector3f {
            val point = Vector3f(x.toFloat(), y.toFloat(), z.toFloat())
            point.sub(camera.position().x.toFloat(), camera.position().y.toFloat(), camera.position().z.toFloat())
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

                    addVertex(renderData.pose, v0).setColor(red, green, blue, alpha0)
                    addVertex(renderData.pose, v2).setColor(red, green, blue, alpha2)
                    if (!renderData.lines) {
                        addVertex(renderData.pose, v2.x, v2.y + height, v2.z).setColor(red, green, blue, alpha2)
                        addVertex(renderData.pose, v0.x, v0.y + height, v0.z).setColor(red, green, blue, alpha0)
                    }
                }
            }
        }

    }

}
