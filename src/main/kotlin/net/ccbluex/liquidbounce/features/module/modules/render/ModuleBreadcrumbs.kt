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

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import it.unimi.dsi.fastutil.objects.ObjectFloatMutablePair
import it.unimi.dsi.fastutil.objects.ObjectFloatPair
import net.ccbluex.fastutil.component1
import net.ccbluex.fastutil.component2
import net.ccbluex.fastutil.mapToArray
import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.render.BuiltinParticle
import net.ccbluex.liquidbounce.render.ClientRenderPipelines
import net.ccbluex.liquidbounce.render.WorldRenderEnvironment
import net.ccbluex.liquidbounce.render.addVertex
import net.ccbluex.liquidbounce.render.drawCustomMesh
import net.ccbluex.liquidbounce.render.drawCustomMeshTextured
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.renderEnvironment
import net.ccbluex.liquidbounce.render.utils.UnitCircle
import net.ccbluex.liquidbounce.render.utils.rainbow
import net.ccbluex.liquidbounce.utils.math.copy
import net.minecraft.util.ARGB
import net.minecraft.world.entity.Entity
import net.minecraft.world.phys.Vec3
import org.joml.Matrix4fc
import org.joml.Vector3f
import org.joml.Vector4f
import java.util.ArrayDeque
import java.util.IdentityHashMap
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Breadcrumbs module
 *
 * Leaves traces behind players.
 */
object ModuleBreadcrumbs : ClientModule("Breadcrumbs", ModuleCategories.RENDER, aliases = listOf("PlayerTrails")) {

    private val mode by enumChoice("Mode", TraceMode.TRAIL).onChanged {
        clear()
    }
    private val onlyOwn by boolean("OnlyOwn", true)
    private val color by color("Color", Color4b.LIQUID_BOUNCE.alpha(120))
    private val colorRainbow by boolean("Rainbow", false)

    /**
     * The height of the [TraceMode.TRAIL] ribbon.
     */
    private val height by float("Height", 0.5f, 0f..2f)

    /**
     * The size of a single footstep, ring or particle.
     */
    private val size by float("Size", 0.4f, 0.05f..2f)

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

        val color = if (colorRainbow) rainbow() else color
        val time = System.currentTimeMillis()

        if (mode != TraceMode.TRAIL) {
            event.renderEnvironment {
                drawMarks(color, time)
            }
            return@handler
        }

        val colorF = color.toVector4f()
        val lines = height == 0f

        event.renderEnvironment {
            drawCustomMesh(if (lines) {
                ClientRenderPipelines.lines(noDepthTest = true)
            } else {
                ClientRenderPipelines.quads(noDepthTest = true)
            }) {
                val renderData = RenderData(poseStack.last().pose(), this, colorF, lines)
                trails.forEach { (entity, trail) ->
                    trail.verifyAndRenderTrail(renderData, event.camera.position(), entity, time)
                }
            }
        }
    }

    private fun WorldRenderEnvironment.drawMarks(color: Color4b, time: Long) {
        trails.values.forEach { it.removeExpiredMarks(time) }

        when (mode) {
            TraceMode.FOOTSTEPS -> drawCustomMesh(ClientRenderPipelines.quads(noDepthTest = false)) { pose ->
                forEachMark(color, time) { mark, x, y, z, argb ->
                    addFootstep(pose, mark, x, y, z, argb)
                }
            }

            TraceMode.RINGS -> drawCustomMesh(ClientRenderPipelines.quads(noDepthTest = false)) { pose ->
                forEachMark(color, time) { _, x, y, z, argb ->
                    addRing(pose, x, y, z, argb)
                }
            }

            TraceMode.PARTICLES -> {
                val left = camera.leftVector()
                val up = camera.upVector()
                val pipeline = ClientRenderPipelines.texQuads(noDepthTest = false)
                drawCustomMeshTextured(BuiltinParticle.ORBIZ.texture, pipeline) { pose ->
                    forEachMark(color, time) { mark, x, y, z, argb ->
                        // Particles drift upwards while they fade
                        val rise = if (TemporaryValueGroup.enabled) {
                            (time - mark.creationTime).toFloat() / TemporaryValueGroup.alive * size
                        } else {
                            0f
                        }
                        val half = size * 0.5f
                        val cy = y + half + rise
                        val lx = left.x() * half
                        val ly = left.y() * half
                        val lz = left.z() * half
                        val ux = up.x() * half
                        val uy = up.y() * half
                        val uz = up.z() * half

                        addVertex(pose, x + lx + ux, cy + ly + uy, z + lz + uz).setUv(0f, 0f).setColor(argb)
                        addVertex(pose, x + lx - ux, cy + ly - uy, z + lz - uz).setUv(0f, 1f).setColor(argb)
                        addVertex(pose, x - lx - ux, cy - ly - uy, z - lz - uz).setUv(1f, 1f).setColor(argb)
                        addVertex(pose, x - lx + ux, cy - ly + uy, z - lz + uz).setUv(1f, 0f).setColor(argb)
                    }
                }
            }

            TraceMode.TRAIL -> error("a trail has no marks")
        }
    }

    /**
     * Calls [action] with every mark relative to the camera and its faded color.
     */
    private inline fun WorldRenderEnvironment.forEachMark(
        color: Color4b,
        time: Long,
        action: (Mark, Float, Float, Float, Int) -> Unit,
    ) {
        val cameraPos = camera.position()
        val shouldFade = TemporaryValueGroup.enabled && TemporaryValueGroup.fade
        val alive = TemporaryValueGroup.alive.toFloat()

        for (trail in trails.values) {
            for (mark in trail.marks) {
                val alpha = if (shouldFade) {
                    (1f - (time - mark.creationTime) / alive).coerceIn(0f, 1f)
                } else {
                    1f
                }

                action(
                    mark,
                    (mark.pos.x - cameraPos.x).toFloat(),
                    (mark.pos.y - cameraPos.y).toFloat() + GROUND_OFFSET,
                    (mark.pos.z - cameraPos.z).toFloat(),
                    ARGB.color((color.a * alpha).toInt(), color.r, color.g, color.b),
                )
            }
        }
    }

    private fun VertexConsumer.addFootstep(
        pose: PoseStack.Pose,
        mark: Mark,
        x: Float,
        y: Float,
        z: Float,
        argb: Int,
    ) {
        val forwardX = cos(mark.direction)
        val forwardZ = sin(mark.direction)
        // Perpendicular to the walking direction, towards the foot of this step
        val sideX = -forwardZ * mark.side
        val sideZ = forwardX * mark.side

        val centerX = x + sideX * size * FOOT_SPREAD
        val centerZ = z + sideZ * size * FOOT_SPREAD
        val halfLength = size * 0.5f
        val halfWidth = size * FOOT_WIDTH * 0.5f

        addVertex(pose, centerX + forwardX * halfLength + sideX * halfWidth, y,
            centerZ + forwardZ * halfLength + sideZ * halfWidth).setColor(argb)
        addVertex(pose, centerX + forwardX * halfLength - sideX * halfWidth, y,
            centerZ + forwardZ * halfLength - sideZ * halfWidth).setColor(argb)
        addVertex(pose, centerX - forwardX * halfLength - sideX * halfWidth, y,
            centerZ - forwardZ * halfLength - sideZ * halfWidth).setColor(argb)
        addVertex(pose, centerX - forwardX * halfLength + sideX * halfWidth, y,
            centerZ - forwardZ * halfLength + sideZ * halfWidth).setColor(argb)
    }

    private fun VertexConsumer.addRing(
        pose: PoseStack.Pose,
        x: Float,
        y: Float,
        z: Float,
        argb: Int,
    ) {
        val outer = size * 0.5f
        val inner = outer * (1f - RING_WIDTH)
        val points = UnitCircle.POINTS

        repeat(UnitCircle.CIRCLE_RES) {
            val cos0 = points[it * 2]
            val sin0 = points[it * 2 + 1]
            val cos1 = points[it * 2 + 2]
            val sin1 = points[it * 2 + 3]

            addVertex(pose, x + cos0 * outer, y, z + sin0 * outer).setColor(argb)
            addVertex(pose, x + cos1 * outer, y, z + sin1 * outer).setColor(argb)
            addVertex(pose, x + cos1 * inner, y, z + sin1 * inner).setColor(argb)
            addVertex(pose, x + cos0 * inner, y, z + sin0 * inner).setColor(argb)
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
            trails.keys.removeIf { it !== player && it.isAlive }
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
        val trail = trails.getOrPut(entity, ::Trail)

        if (mode == TraceMode.TRAIL) {
            trail.positions.add(TrailPart(entity.position(), time))
        } else if (entity.onGround()) {
            // Marks lie on the ground, so jumping or falling leaves none in the air
            trail.updateMarks(entity.position(), time)
        }
    }

    @Suppress("unused")
    private val worldChangeHandler = handler<WorldChangeEvent> {
        clear()
    }

    private fun clear() {
        lastPositions.clear()
        trails.clear()
    }

    /**
     * Read by the game tests.
     */
    val markCount: Int
        get() = trails.values.sumOf { it.marks.size }

    @JvmRecord
    private data class TrailPart(val pos: Vec3, val creationTime: Long)

    /**
     * A footstep, ring or particle left at [pos], heading in [direction] (radians, around Y).
     */
    @JvmRecord
    private data class Mark(val pos: Vec3, val creationTime: Long, val direction: Float, val side: Int)

    private class RenderData(
        val pose: Matrix4fc,
        val bufferBuilder: VertexConsumer,
        val color: Vector4f,
        val lines: Boolean
    )

    private class Trail {

        val positions = ArrayDeque<TrailPart>()
        val marks = ArrayDeque<Mark>()

        private var lastMarkPos: Vec3? = null
        private var nextSide = 1

        fun updateMarks(pos: Vec3, time: Long) {
            val lastPos = lastMarkPos
            if (lastPos == null) {
                lastMarkPos = pos
                return
            }

            val dx = pos.x - lastPos.x
            val dz = pos.z - lastPos.z
            val spacing = size * mode.spacing
            if (dx * dx + dz * dz < spacing * spacing) {
                return
            }

            marks.add(Mark(pos, time, atan2(dz, dx).toFloat(), nextSide))
            nextSide = -nextSide
            lastMarkPos = pos
        }

        fun removeExpiredMarks(time: Long) {
            if (!TemporaryValueGroup.enabled) {
                return
            }

            val expirationTime = time - TemporaryValueGroup.alive
            while (marks.isNotEmpty() && marks.peekFirst().creationTime < expirationTime) {
                marks.removeFirst()
            }
        }

        fun verifyAndRenderTrail(renderData: RenderData, cameraPos: Vec3, entity: Entity, time: Long) {
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

                val point = calculateRelativePos(cameraPos, position.pos)
                ObjectFloatMutablePair.of(point, alpha)
            }

            val interpolatedPos = entity.getPosition(mc.deltaTracker.getGameTimeDeltaPartialTick(true))
            val point = calculateRelativePos(cameraPos, interpolatedPos)
            pointsWithAlpha.last().left(point)

            addVerticesToBuffer(renderData, pointsWithAlpha)
        }

        private fun calculateRelativePos(cameraPos: Vec3, pos: Vec3): Vector3f {
            return Vector3f().set(
                pos.x - cameraPos.x,
                pos.y - cameraPos.y,
                pos.z - cameraPos.z,
            )
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

    /**
     * @param spacing the distance between two marks, in multiples of [size]
     */
    private enum class TraceMode(override val tag: String, val spacing: Float) : Tagged {
        TRAIL("Trail", 0f),
        FOOTSTEPS("Footsteps", 0.9f),
        RINGS("Rings", 0.8f),
        PARTICLES("Particles", 0.5f),
    }

    private const val GROUND_OFFSET = 0.01f
    private const val FOOT_SPREAD = 0.25f
    private const val FOOT_WIDTH = 0.4f
    private const val RING_WIDTH = 0.2f

}
