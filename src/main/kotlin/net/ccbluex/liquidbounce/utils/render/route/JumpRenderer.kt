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

package net.ccbluex.liquidbounce.utils.render.route

import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.render.WorldRenderEnvironment
import net.ccbluex.liquidbounce.render.drawBox
import net.ccbluex.liquidbounce.render.drawLines
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.renderEnvironment
import net.ccbluex.liquidbounce.render.utils.MutableVertexList
import net.ccbluex.liquidbounce.render.withPositionRelativeToCamera
import net.ccbluex.liquidbounce.utils.math.fma
import net.ccbluex.liquidbounce.utils.navigation.walk.AssistedJump
import net.minecraft.world.entity.Avatar.DEFAULT_EYE_HEIGHT
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3

/**
 * Draws the jump [assisted] goes for: the run to the take-off and the flight, a bar where the jump starts, the block
 * it lands on and a post where to look. Everything is predicted again every tick from where the player is, so it
 * follows the player steering by hand.
 */
internal class JumpRenderer(
    owner: EventListener,
    private val assisted: AssistedJump,
) : ToggleableValueGroup(owner, "Render", true) {

    private val canBeCovered by boolean("CanBeCovered", false)
    private val arcColor by color("Arc", Color4b.WHITE.alpha(ARC_ALPHA))
    private val takeoffColor by color("TakeOff", Color4b(0xFF, 0xC8, 0x3C))
    private val landingColor by color("Landing", Color4b(0x55, 0xFF, 0x7F))
    private val aimColor by color("Aim", Color4b.LIQUID_BOUNCE)

    @Suppress("unused")
    private val renderHandler = handler<WorldRenderEvent> { event ->
        val target = assisted.target ?: return@handler
        val path = assisted.path
        val noDepthTest = !canBeCovered

        event.renderEnvironment {
            drawLanding(target, path.lastOrNull(), noDepthTest)
            drawAim(target.aim, noDepthTest)
            if (path.size < 2) {
                return@renderEnvironment
            }

            if (target.step != null) {
                drawRunUp(path.first(), noDepthTest)
            }
            drawArc(path, assisted.takeoff, noDepthTest)
        }
    }

    /** The top of the block landed on, and the spot inside it the feet come down on. */
    private fun WorldRenderEnvironment.drawLanding(
        target: AssistedJump.Target,
        touchdown: Vec3?,
        noDepthTest: Boolean,
    ) {
        withPositionRelativeToCamera(Vec3(target.pos.x.toDouble(), target.feet, target.pos.z.toDouble())) {
            drawBox(PLATE, landingColor.alpha(PLATE_ALPHA), landingColor, noDepthTest = noDepthTest)
        }
        if (touchdown != null) {
            withPositionRelativeToCamera(touchdown) {
                drawBox(FOOTPRINT, null, landingColor, noDepthTest = noDepthTest)
            }
        }
    }

    /** A post at the point the jump is aimed at, with a cross at eye height that the crosshair lines up with. */
    private fun WorldRenderEnvironment.drawAim(aim: Vec3, noDepthTest: Boolean) {
        val origin = camera.position()
        val eye = aim.add(0.0, DEFAULT_EYE_HEIGHT.toDouble(), 0.0)
        val lines = MutableVertexList(POST_VERTICES)
        lines.addRelative(aim, origin)
        lines.addRelative(eye, origin)
        lines.addRelative(eye.add(-CROSS_SIZE, 0.0, 0.0), origin)
        lines.addRelative(eye.add(CROSS_SIZE, 0.0, 0.0), origin)
        lines.addRelative(eye.add(0.0, 0.0, -CROSS_SIZE), origin)
        lines.addRelative(eye.add(0.0, 0.0, CROSS_SIZE), origin)
        drawLines(aimColor.argb, lines, noDepthTest)
    }

    /** Where the player has to stand first, as the jump needs a run from there. */
    private fun WorldRenderEnvironment.drawRunUp(runUp: Vec3, noDepthTest: Boolean) {
        val lines = MutableVertexList()
        addPolyline(lines, listOf(player.position(), runUp), camera.position())
        drawLines(takeoffColor.alpha(takeoffColor.a / 2).argb, lines, noDepthTest)
        withPositionRelativeToCamera(runUp) {
            drawBox(SPOT, takeoffColor.alpha(PLATE_ALPHA), takeoffColor, noDepthTest = noDepthTest)
        }
    }

    /** The run with arrow heads, a bar across it at the take-off, and the flight. */
    private fun WorldRenderEnvironment.drawArc(path: List<Vec3>, takeoff: Int, noDepthTest: Boolean) {
        val origin = camera.position()
        val lines = MutableVertexList()
        addPolyline(lines, path, origin)
        if (takeoff > 0) {
            addChevrons(lines, path.subList(0, takeoff + 1), origin)
        }
        drawLines(arcColor.argb, lines, noDepthTest)

        if (takeoff < 0 || takeoff + 1 >= path.size) {
            return
        }
        val spot = path[takeoff]
        val along = path[takeoff + 1].subtract(spot)
        val across = Vec3(-along.z, 0.0, along.x)
        if (across.lengthSqr() < 1.0E-8) {
            return
        }

        val side = across.normalize()
        val bar = MutableVertexList(2)
        bar.addRelative(spot.fma(BAR_HALF_LENGTH, side).add(0.0, BAR_LIFT, 0.0), origin)
        bar.addRelative(spot.fma(-BAR_HALF_LENGTH, side).add(0.0, BAR_LIFT, 0.0), origin)
        drawLines(takeoffColor.argb, bar, noDepthTest)
        withPositionRelativeToCamera(spot) {
            drawBox(SPOT, takeoffColor.alpha(PLATE_ALPHA), takeoffColor, noDepthTest = noDepthTest)
        }
    }

    private companion object {
        const val ARC_ALPHA = 220
        const val PLATE_ALPHA = 60
        const val POST_VERTICES = 6
        const val CROSS_SIZE = 0.15
        const val BAR_HALF_LENGTH = 0.5
        const val BAR_LIFT = 0.05

        val PLATE = AABB(0.0, 0.0, 0.0, 1.0, 0.02, 1.0)
        val FOOTPRINT = AABB(-0.3, 0.0, -0.3, 0.3, 0.02, 0.3)
        val SPOT = AABB(-0.15, 0.0, -0.15, 0.15, 0.02, 0.15)
    }

}
