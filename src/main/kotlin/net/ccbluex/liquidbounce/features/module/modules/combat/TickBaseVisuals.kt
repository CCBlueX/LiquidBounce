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
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.event.events.PlayerPostTickEvent
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.render.ClientRenderPipelines
import net.ccbluex.liquidbounce.render.WorldRenderEnvironment
import net.ccbluex.liquidbounce.render.addVertex
import net.ccbluex.liquidbounce.render.drawCircleOutline
import net.ccbluex.liquidbounce.render.drawCustomMesh
import net.ccbluex.liquidbounce.render.drawGradientCircle
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.renderEnvironment
import net.ccbluex.liquidbounce.render.setColor
import net.ccbluex.liquidbounce.render.withPositionRelativeToCamera
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.client.Timer
import net.ccbluex.liquidbounce.utils.client.clientStartDurationMs
import net.ccbluex.liquidbounce.utils.entity.interpolateCurrentPosition
import net.ccbluex.liquidbounce.utils.math.minus
import net.ccbluex.liquidbounce.utils.math.plus
import net.ccbluex.liquidbounce.utils.math.toRadians
import net.ccbluex.liquidbounce.utils.render.WireframePlayer
import net.minecraft.util.Mth
import net.minecraft.world.entity.Pose
import net.minecraft.world.phys.Vec3
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * Shows what TickBase does in every mode: the coming ticks up to the hit, the player where the hit lands,
 * the ticks run ahead of time trailing behind, and the balance as a ring around the feet.
 */
internal object TickBaseVisuals : ToggleableValueGroup(ModuleTickBase, "Visuals", true) {

    private const val AFTERIMAGE_MILLIS = 450f
    private const val MAX_AFTERIMAGES = 24

    private const val CIRCLE_SEGMENTS = 48
    private const val RING_RADIUS = 0.6f
    private const val RING_WIDTH = 0.04f
    private const val LANDING_RADIUS = 0.55f

    private const val PATH_HALF_WIDTH = 0.14
    private const val BEAD_RADIUS = 0.09
    private const val GROUND_LIFT = 0.02

    // Ghosts close to the camera would fill the screen in first person
    private const val GHOST_HIDDEN_DISTANCE = 0.8
    private const val GHOST_FADE_DISTANCE = 1.2

    private val color by color("Color", Color4b.LIQUID_BOUNCE).doNotIncludeAlways()
    private val balanceRing by boolean("BalanceRing", true)

    private var path = emptyList<Vec3>()
    private var landing = -1
    private var hit: Vec3? = null

    private val afterimages = ArrayDeque<Afterimage>()
    private val ghost = WireframePlayer()

    private class Afterimage(val pos: Vec3, val yRot: Float, val pose: Pose, val time: Long)

    /**
     * Shows [path], the coming positions of the player from now, with the hit landing on [hit] after [landing] ticks.
     */
    fun show(path: List<Vec3>, landing: Int?, hit: Vec3?) {
        this.path = path
        this.landing = landing ?: -1
        this.hit = hit
    }

    fun clear() = show(emptyList(), null, null)

    @Suppress("unused")
    private val tickHandler = handler<PlayerPostTickEvent> {
        // A tick ahead of real time leaves an image behind
        if (!Timer.bursting && Timer.timerSpeed <= 1f) {
            return@handler
        }

        afterimages.addLast(Afterimage(player.position(), player.yRot, player.pose, clientStartDurationMs))
        if (afterimages.size > MAX_AFTERIMAGES) {
            afterimages.removeFirst()
        }
    }

    @Suppress("unused")
    private val renderHandler = handler<WorldRenderEvent> { event ->
        val time = clientStartDurationMs
        val origin = player.interpolateCurrentPosition(event.partialTicks)

        event.renderEnvironment {
            if (balanceRing) {
                renderBalance(origin, player.getViewYRot(event.partialTicks))
            }
            renderPath(origin)
            renderLanding()
            renderAfterimages(time)
        }
    }

    /**
     * A ring around the feet filled by the balance, centred on where the player looks.
     */
    private fun WorldRenderEnvironment.renderBalance(origin: Vec3, yRot: Float) {
        val range = ModuleTickBase.balance
        val fraction = if (range.last > range.first) {
            ((Timer.balance - range.first) / (range.last - range.first)).coerceIn(0f, 1f)
        } else {
            0f
        }

        withPositionRelativeToCamera(origin.add(0.0, 0.01, 0.0)) {
            drawCircleOutline(RING_RADIUS, color.fade(0.2f))
            if (fraction <= 0f) {
                return@withPositionRelativeToCamera
            }

            val sweep = fraction * Mth.TWO_PI
            val start = (yRot + 90f).toRadians() - sweep / 2f
            val segments = max(2, (CIRCLE_SEGMENTS * fraction).toInt())
            val inner = RING_RADIUS - RING_WIDTH
            val outer = RING_RADIUS + RING_WIDTH

            drawCustomMesh(ClientRenderPipelines.triangleStrip(noDepthTest = true)) { pose ->
                for (i in 0..segments) {
                    val angle = start + sweep * i / segments
                    val x = cos(angle)
                    val z = sin(angle)
                    addVertex(pose, x * inner, 0f, z * inner).setColor(color.fade(0.35f))
                    addVertex(pose, x * outer, 0f, z * outer).setColor(color)
                }
            }
        }
    }

    /**
     * The coming ticks as a ribbon on the ground with a bead for each, widening and brightening towards the landing
     * and fading past it. Without a hit to land, only a faint trace.
     */
    private fun WorldRenderEnvironment.renderPath(origin: Vec3) {
        if (path.size < 2) {
            return
        }

        val end = if (landing > 0) landing else 0

        // The ribbon starts where the player is drawn between ticks
        val points = path.mapIndexed { tick, pos -> if (tick == 0) Vec3.ZERO else pos - origin }

        withPositionRelativeToCamera(origin.add(0.0, GROUND_LIFT, 0.0)) {
            drawCustomMesh(ClientRenderPipelines.quads(noDepthTest = true)) { pose ->
                for (tick in 1..points.lastIndex) {
                    val from = points[tick - 1]
                    val to = points[tick]

                    // Zero for ticks standing still, which leaves their quad empty
                    val side = Vec3(to.z - from.z, 0.0, from.x - to.x).normalize()
                    val fromFade = pathFade(tick - 1, end)
                    val toFade = pathFade(tick, end)
                    val fromSide = side.scale(PATH_HALF_WIDTH * (0.3 + 0.7 * fromFade))
                    val toSide = side.scale(PATH_HALF_WIDTH * (0.3 + 0.7 * toFade))

                    addVertex(pose, from + fromSide).setColor(color.fade(0.6f * fromFade))
                    addVertex(pose, to + toSide).setColor(color.fade(0.6f * toFade))
                    addVertex(pose, to - toSide).setColor(color.fade(0.6f * toFade))
                    addVertex(pose, from - fromSide).setColor(color.fade(0.6f * fromFade))

                    if (tick <= end) {
                        val bead = color.fade(0.4f + 0.6f * toFade)
                        addVertex(pose, to.add(BEAD_RADIUS, 0.0, 0.0)).setColor(bead)
                        addVertex(pose, to.add(0.0, 0.0, BEAD_RADIUS)).setColor(bead)
                        addVertex(pose, to.add(-BEAD_RADIUS, 0.0, 0.0)).setColor(bead)
                        addVertex(pose, to.add(0.0, 0.0, -BEAD_RADIUS)).setColor(bead)
                    }
                }
            }
        }
    }

    private fun pathFade(tick: Int, end: Int) = if (tick <= end) {
        0.25f + 0.75f * tick / end
    } else {
        0.25f * (1f - (tick - end).toFloat() / (path.size - end))
    }

    /**
     * The player where the hit lands on a ring, and the strike from its eyes to the hit.
     */
    private fun WorldRenderEnvironment.renderLanding() {
        val hit = hit ?: return
        if (landing <= 0) {
            return
        }

        val pos = path[landing]
        val eyes = pos.add(0.0, player.eyeHeight.toDouble(), 0.0)
        val fade = ghostFade(eyes)

        if (fade > 0f) {
            ghost.pos = pos
            ghost.pose = player.pose
            ghost.setRotation(Rotation.lookingAt(hit, eyes))
            ghost.render(color.fade(0.12f * fade), color.fade(0.9f * fade))
        }

        withPositionRelativeToCamera(pos.add(0.0, GROUND_LIFT, 0.0)) {
            drawGradientCircle(LANDING_RADIUS, 0f, color.fade(0.45f), Color4b.TRANSPARENT)
            drawCircleOutline(LANDING_RADIUS, color)
        }

        withPositionRelativeToCamera(eyes) {
            drawCustomMesh(ClientRenderPipelines.lines(noDepthTest = true)) { pose ->
                addVertex(pose, Vec3.ZERO).setColor(color.fade(0.05f))
                addVertex(pose, hit - eyes).setColor(color)
            }
        }
    }

    /**
     * The ticks run ahead of real time, fading out from the oldest.
     */
    private fun WorldRenderEnvironment.renderAfterimages(time: Long) {
        while (afterimages.isNotEmpty() && time - afterimages.first().time > AFTERIMAGE_MILLIS) {
            afterimages.removeFirst()
        }

        afterimages.forEachIndexed { index, image ->
            val age = (time - image.time) / AFTERIMAGE_MILLIS
            val fade = (1f - age) * (index + 1f) / afterimages.size *
                ghostFade(image.pos.add(0.0, player.eyeHeight.toDouble(), 0.0))
            if (fade <= 0f) {
                return@forEachIndexed
            }

            ghost.pos = image.pos
            ghost.pose = image.pose
            ghost.yRot = image.yRot
            ghost.xRot = 0f
            ghost.render(color.fade(0.08f * fade), color.fade(0.6f * fade))
        }
    }

    private fun WorldRenderEnvironment.ghostFade(eyes: Vec3) =
        ((camera.position().distanceTo(eyes) - GHOST_HIDDEN_DISTANCE) / GHOST_FADE_DISTANCE).toFloat().coerceIn(0f, 1f)

}
