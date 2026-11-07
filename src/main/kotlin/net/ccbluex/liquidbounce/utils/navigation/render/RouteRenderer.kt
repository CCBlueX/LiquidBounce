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

package net.ccbluex.liquidbounce.utils.navigation.render

import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.render.FULL_BOX
import net.ccbluex.liquidbounce.render.WorldRenderEnvironment
import net.ccbluex.liquidbounce.render.drawBox
import net.ccbluex.liquidbounce.render.drawLines
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.renderEnvironment
import net.ccbluex.liquidbounce.render.utils.MutableVertexList
import net.ccbluex.liquidbounce.render.withPositionRelativeToCamera
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.entity.interpolateCurrentPosition
import net.ccbluex.liquidbounce.utils.entity.rotation
import net.ccbluex.liquidbounce.utils.navigation.terrain.NavigationTerrain
import net.ccbluex.liquidbounce.utils.render.WireframePlayer
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.Entity
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import kotlin.math.PI
import kotlin.math.sin

/**
 * Draws the route of [view] with its direction and progress, the blocks it breaks and places, the destination,
 * where the server has the player, and the target with its attack position.
 */
class RouteRenderer(
    owner: EventListener,
    private val view: RouteView,
    enabled: Boolean = true,
) : ToggleableValueGroup(owner, "Render", enabled) {

    private val canBeCovered by boolean("CanBeCovered", false)
    private val routeColor by color("Route", Color4b.LIQUID_BOUNCE)
    private val searchColor by color("Search", Color4b(0xFF, 0xC8, 0x3C))
    private val destinationColor by color("Destination", Color4b.LIQUID_BOUNCE.alpha(60))
    private val breakColor by color("Break", Color4b(0xFF, 0x55, 0x55, 0xC8))
    private val placeColor by color("Place", Color4b(0x55, 0xFF, 0x7F, 0xC8))
    private val targetColor by color("Target", Color4b(0xFF, 0x8C, 0x1A))
    private val invalidColor by color("Invalid", Color4b(0xD7, 0x09, 0x09))
    private val ghost by boolean("Ghost", true)
    private val fadeOut by int("FadeOut", 800, 0..3000, "ms")

    private val wireframe = WireframePlayer()

    @Suppress("unused")
    private val renderHandler = handler<WorldRenderEvent> { event ->
        val ghostPosition = view.ghost?.invoke(event.partialTicks)
        if (view.state == RouteState.NONE && view.target == null && view.preview == null && ghostPosition == null) {
            return@handler
        }

        val fade = fade()
        val noDepthTest = !canBeCovered

        event.renderEnvironment {
            if (view.state != RouteState.NONE && fade > 0f) {
                val color = if (view.state == RouteState.INVALID) invalidColor else routeColor
                when (view.state) {
                    RouteState.PENDING -> drawPending(noDepthTest)
                    else -> drawRoute(event.partialTicks, color.fade(fade), noDepthTest)
                }

                view.destination?.let { destination ->
                    val markerColor = if (view.state == RouteState.INVALID) invalidColor else destinationColor
                    drawMarker(destination, markerColor.fade(fade), noDepthTest)
                }

                if (view.state == RouteState.ACTIVE) {
                    drawBlocks(view.breaks, breakColor, noDepthTest)
                    drawBlocks(view.places, placeColor, noDepthTest)
                }
            } else {
                view.preview?.let { preview ->
                    val color = if (view.isPreviewValid) destinationColor else invalidColor.alpha(PREVIEW_INVALID_ALPHA)
                    drawMarker(preview, color, noDepthTest)
                }
            }

            view.target?.let { target -> drawTarget(target, ghostPosition, event.partialTicks, noDepthTest) }

            if (ghost && ghostPosition != null) {
                wireframe.pos = ghostPosition
                wireframe.setRotation(RotationManager.currentRotation ?: player.rotation)
                val color = if (view.engaged) targetColor else routeColor
                wireframe.render(color.alpha(GHOST_ALPHA), color, noDepthTest)
            }
        }
    }

    private fun fade(): Float {
        if (view.state != RouteState.DONE && view.state != RouteState.INVALID) {
            return 1f
        }
        if (fadeOut == 0) {
            return 0f
        }
        return 1f - (System.currentTimeMillis() - view.since).toFloat() / fadeOut
    }

    /**
     * The best route the search has so far, a line on to the destination, and a pulsing marker where the search got.
     */
    private fun WorldRenderEnvironment.drawPending(noDepthTest: Boolean) {
        val start = view.start ?: return
        val destination = view.destination ?: return
        val origin = camera.position()
        val searched = view.path?.takeIf { it.size >= 2 }
        val reached = searched?.last() ?: start

        val gap = MutableVertexList(2)
        gap.addRelative(reached.add(0.0, LIFT, 0.0), origin)
        gap.addRelative(destination.add(0.0, LIFT, 0.0), origin)
        drawLines(searchColor.alpha(PENDING_ALPHA).argb, gap, noDepthTest)

        if (searched != null) {
            val lines = MutableVertexList()
            addPolyline(lines, searched, origin)
            addChevrons(lines, searched, origin)
            drawLines(searchColor.argb, lines, noDepthTest)
        }

        val phase = (System.currentTimeMillis() % PULSE_MILLIS).toDouble() / PULSE_MILLIS
        val pulse = (sin(phase * 2.0 * PI) + 1.0) / 2.0
        withPositionRelativeToCamera(reached) {
            drawBox(TIP, searchColor.alpha((TIP_ALPHA * pulse).toInt()), searchColor, noDepthTest = noDepthTest)
        }
    }

    private fun WorldRenderEnvironment.drawRoute(partialTicks: Float, color: Color4b, noDepthTest: Boolean) {
        val path = view.path ?: return
        val origin = camera.position()

        val travelled = MutableVertexList()
        addPolyline(travelled, path, origin)
        drawLines(color.alpha(color.a / 3).argb, travelled, noDepthTest)

        val progress = view.progress
        val ahead = when {
            progress != null && progress.isMoving -> listOf(progress.head(partialTicks)) + progress.ahead
            view.state == RouteState.ACTIVE -> path
            else -> return
        }

        val lines = MutableVertexList()
        addPolyline(lines, ahead, origin)
        addChevrons(lines, ahead, origin)
        drawLines(color.argb, lines, noDepthTest)
    }

    private fun WorldRenderEnvironment.drawBlocks(blocks: Collection<BlockPos>, color: Color4b, noDepthTest: Boolean) {
        for (pos in blocks) {
            withPositionRelativeToCamera(pos) {
                drawBox(FULL_BOX, color.alpha(color.a / 4), color, noDepthTest = noDepthTest)
            }
        }
    }

    private fun WorldRenderEnvironment.drawMarker(feet: Vec3, color: Color4b, noDepthTest: Boolean) {
        withPositionRelativeToCamera(feet) {
            drawBox(PLATE, color, color.alpha(255), noDepthTest = noDepthTest)
            drawBox(BODY, null, color.alpha(OUTLINE_ALPHA), noDepthTest = noDepthTest)
        }
    }

    private fun WorldRenderEnvironment.drawTarget(
        target: Entity,
        server: Vec3?,
        partialTicks: Float,
        noDepthTest: Boolean,
    ) {
        val position = target.interpolateCurrentPosition(partialTicks)
        val box = target.boundingBox.move(position.subtract(target.position()))
        withPositionRelativeToCamera {
            drawBox(box, null, targetColor, noDepthTest = noDepthTest)
        }

        view.attackPosition?.let { drawMarker(it, targetColor.alpha(destinationColor.a), noDepthTest) }

        if (view.engaged && server != null) {
            val lines = MutableVertexList(2)
            lines.addRelative(server.add(0.0, EYE_HEIGHT, 0.0), camera.position())
            lines.addRelative(box.center, camera.position())
            drawLines(targetColor.argb, lines, noDepthTest)
        }
    }

    private fun addPolyline(lines: MutableVertexList, points: List<Vec3>, origin: Vec3) {
        for (index in 0 until points.lastIndex) {
            lines.addRelative(points[index].add(0.0, LIFT, 0.0), origin)
            lines.addRelative(points[index + 1].add(0.0, LIFT, 0.0), origin)
        }
    }

    /**
     * Arrow heads along [points], pointing the way the route goes.
     */
    private fun addChevrons(lines: MutableVertexList, points: List<Vec3>, origin: Vec3) {
        var offset = CHEVRON_SPACING / 2
        for (index in 0 until points.lastIndex) {
            val from = points[index].add(0.0, LIFT, 0.0)
            val segment = points[index + 1].add(0.0, LIFT, 0.0).subtract(from)
            val length = segment.length()
            if (length < 1.0E-4) {
                continue
            }

            val direction = segment.scale(1.0 / length)
            val side = if (direction.horizontalDistanceSqr() < 1.0E-4) {
                Vec3(1.0, 0.0, 0.0)
            } else {
                Vec3(-direction.z, 0.0, direction.x).normalize()
            }

            while (offset < length) {
                val tip = from.add(direction.scale(offset + CHEVRON_SIZE))
                val back = from.add(direction.scale(offset - CHEVRON_SIZE))
                lines.addRelative(back.add(side.scale(CHEVRON_SIZE)), origin)
                lines.addRelative(tip, origin)
                lines.addRelative(back.subtract(side.scale(CHEVRON_SIZE)), origin)
                lines.addRelative(tip, origin)
                offset += CHEVRON_SPACING
            }
            offset -= length
        }
    }

    private companion object {
        const val LIFT = 0.05
        const val EYE_HEIGHT = 1.62
        const val CHEVRON_SPACING = 1.5
        const val CHEVRON_SIZE = 0.2
        const val GHOST_ALPHA = 50
        const val PENDING_ALPHA = 90
        const val OUTLINE_ALPHA = 160
        const val PREVIEW_INVALID_ALPHA = 60
        const val TIP_ALPHA = 160
        const val PULSE_MILLIS = 800L

        val TIP = AABB(-0.15, 0.0, -0.15, 0.15, 0.3, 0.15)

        val PLATE = AABB(-0.5, 0.0, -0.5, 0.5, 0.02, 0.5)
        val BODY = AABB(
            -NavigationTerrain.PLAYER_WIDTH / 2, 0.0, -NavigationTerrain.PLAYER_WIDTH / 2,
            NavigationTerrain.PLAYER_WIDTH / 2, NavigationTerrain.PLAYER_HEIGHT, NavigationTerrain.PLAYER_WIDTH / 2,
        )
    }

}
