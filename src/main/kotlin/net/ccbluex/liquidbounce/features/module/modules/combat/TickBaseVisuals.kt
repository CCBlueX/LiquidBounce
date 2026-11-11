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
import net.ccbluex.liquidbounce.render.drawLinesWithWidth
import net.ccbluex.liquidbounce.render.effect.ShaderLayer
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.renderEnvironment
import net.ccbluex.liquidbounce.render.utils.MutableVertexList
import net.ccbluex.liquidbounce.render.utils.lineStripAsLines
import net.ccbluex.liquidbounce.render.setColor
import net.ccbluex.liquidbounce.render.withPositionRelativeToCamera
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.client.Timer
import net.ccbluex.liquidbounce.utils.client.clientStartDurationMs
import net.ccbluex.liquidbounce.utils.entity.interpolateCurrentPosition
import net.ccbluex.liquidbounce.utils.math.minus
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
 * where the player was when it ran ahead of time, and the balance as a ring around the feet.
 */
internal object TickBaseVisuals : ToggleableValueGroup(ModuleTickBase, "Visuals", true) {

    private val color by color("Color", Color4b.LIQUID_BOUNCE).doNotIncludeAlways()
    private val balanceRing by boolean("BalanceRing", true)

    private val shader = tree(ShaderLayer())
    private val lineShader = tree(ShaderLayer(solid = true, name = "LineShader"))

    /**
     * The coming positions of the player from now up to where the hit on [hit] lands. Empty without a hit.
     */
    private var path = emptyList<Vec3>()
    private var hit: Vec3? = null

    /**
     * Where the player was when the ticks started to run ahead of real time.
     */
    private var afterimage: Afterimage? = null
    private var ahead = false

    private val ghost = WireframePlayer()

    private class Afterimage(val pos: Vec3, val yRot: Float, val pose: Pose, val time: Long)

    /**
     * Shows [path], the coming positions of the player from now, with the hit landing on [hit] after [landing] ticks.
     */
    fun show(path: List<Vec3>, landing: Int?, hit: Vec3?) {
        this.path = if (landing != null && landing > 0 && hit != null) path.subList(0, landing + 1) else emptyList()
        this.hit = hit
    }

    fun clear() = show(emptyList(), null, null)

    @Suppress("unused")
    private val tickHandler = handler<PlayerPostTickEvent> {
        val wasAhead = ahead
        ahead = Timer.bursting || Timer.timerSpeed > 1f
        if (ahead && !wasAhead) {
            afterimage = Afterimage(player.oldPosition(), player.yRot, player.pose, clientStartDurationMs)
        }
    }

    @Suppress("unused")
    private val renderHandler = handler<WorldRenderEvent> { event ->
        val time = clientStartDurationMs
        val origin = player.interpolateCurrentPosition(event.partialTicks)

        event.renderEnvironment(shader) {
            if (balanceRing) {
                renderBalance(origin, player.getViewYRot(event.partialTicks))
            }
            hit?.takeIf { path.isNotEmpty() }?.let { hit ->
                renderLanding(hit)
            }
            renderAfterimage(time)
        }

        event.renderEnvironment(lineShader) {
            if (balanceRing) {
                withPositionRelativeToCamera(origin.add(0.0, 0.01, 0.0)) {
                    drawCircleOutline(0.6f, color.fade(0.2f))
                }
            }
            hit?.takeIf { path.isNotEmpty() }?.let { hit ->
                renderPath(origin)
                renderStrike(hit)
            }
        }
    }

    /**
     * The balance as a part of the ring around the feet, centred on where the player looks.
     */
    private fun WorldRenderEnvironment.renderBalance(origin: Vec3, yRot: Float) {
        val range = ModuleTickBase.balance
        val fraction = if (range.last > range.first) {
            ((Timer.balance - range.first) / (range.last - range.first)).coerceIn(0f, 1f)
        } else {
            0f
        }

        if (fraction <= 0f) {
            return
        }

        withPositionRelativeToCamera(origin.add(0.0, 0.01, 0.0)) {
            val sweep = fraction * Mth.TWO_PI
            val start = (yRot + 90f).toRadians() - sweep / 2f
            val segments = max(2, (48 * fraction).toInt())
            val inner = 0.56f
            val outer = 0.64f

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
     * The coming ticks up to the landing as a line on the ground.
     */
    private fun WorldRenderEnvironment.renderPath(origin: Vec3) {
        // The line starts where the player is drawn between ticks
        val line = MutableVertexList(path.size).add(Vec3.ZERO).addAllRelative(path.subList(1, path.size), origin)
        withPositionRelativeToCamera(origin.add(0.0, 0.02, 0.0)) {
            drawLinesWithWidth(color.argb, 3f, line.lineStripAsLines())
        }
    }

    /**
     * The player where the hit lands.
     */
    private fun WorldRenderEnvironment.renderLanding(hit: Vec3) {
        val pos = path.last()
        val eyes = pos.add(0.0, player.eyeHeight.toDouble(), 0.0)
        val fade = ghostFade(eyes)

        if (fade > 0f) {
            ghost.pos = pos
            ghost.pose = player.pose
            ghost.setRotation(Rotation.lookingAt(hit, eyes))
            ghost.render(color.fade(fade))
        }
    }

    /**
     * The strike from the eyes of the player where the hit lands to the hit.
     */
    private fun WorldRenderEnvironment.renderStrike(hit: Vec3) {
        val eyes = path.last().add(0.0, player.eyeHeight.toDouble(), 0.0)

        withPositionRelativeToCamera(eyes) {
            drawCustomMesh(ClientRenderPipelines.lines(noDepthTest = true)) { pose ->
                addVertex(pose, Vec3.ZERO).setColor(color.fade(0.05f))
                addVertex(pose, hit - eyes).setColor(color)
            }
        }
    }

    /**
     * The player where the ticks started to run ahead of real time, fading out.
     */
    private fun WorldRenderEnvironment.renderAfterimage(time: Long) {
        val image = afterimage ?: return
        val fade = (1f - (time - image.time) / 450f) * ghostFade(image.pos.add(0.0, player.eyeHeight.toDouble(), 0.0))
        if (fade <= 0f) {
            return
        }

        ghost.pos = image.pos
        ghost.pose = image.pose
        ghost.yRot = image.yRot
        ghost.xRot = 0f
        ghost.render(color.fade(fade))
    }

    // Ghosts close to the camera would fill the screen in first person
    private fun WorldRenderEnvironment.ghostFade(eyes: Vec3) =
        ((camera.position().distanceTo(eyes) - 0.8) / 1.2).toFloat().coerceIn(0f, 1f)

}
