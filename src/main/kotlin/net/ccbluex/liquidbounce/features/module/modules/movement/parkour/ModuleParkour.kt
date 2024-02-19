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
package net.ccbluex.liquidbounce.features.module.modules.movement.parkour

import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.utils.client.toRadians
import net.ccbluex.liquidbounce.utils.entity.SimulatedPlayer
import net.ccbluex.liquidbounce.utils.entity.moving
import net.ccbluex.liquidbounce.utils.math.*
import net.ccbluex.liquidbounce.utils.math.geometry.LineSegment
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.minecraft.util.math.Vec3d
import org.joml.Vector2d
import java.text.DecimalFormat
import kotlin.math.abs
import kotlin.math.absoluteValue


/**
 * Parkour module
 *
 * Automatically jumps at the very edge of a block.
 */
object ModuleParkour : Module("Parkour", Category.MOVEMENT) {

//    private val edgeDistance by float("EdgeDistance", 0.01f, 0.01f..0.5f)

    fun findFallOffPos(line: LineSegment, directionalInput: DirectionalInput): Triple<Int, Vector2d, Vector2d>? {
        val simulatedPlayer = SimulatedPlayer.fromClientPlayer(
            SimulatedPlayer.SimulatedPlayerInput.fromClientPlayer(directionalInput)
        )

        var currPos = player.pos

        for (tick in 0..25) {
            simulatedPlayer.tick()

            val nextPos = simulatedPlayer.pos

            val linePoints = line.points

            val intersect = intersectLineSegmentLineSegment(
                currPos.horizontalComponent(),
                nextPos.horizontalComponent(),
                linePoints.first.horizontalComponent(),
                linePoints.second.horizontalComponent()
            )

            if (intersect != null)
                return Triple(tick, currPos.horizontalComponent(), intersect)

            currPos = nextPos
        }

        return null
    }

    var jumpOnNextTick = false

    val tickJumpHandler = handler<MovementInputEvent> {
        this.directionalInput = it.directionalInput

        if (it.jumping && player.isOnGround) {
            println("P: " + player.pos)
        }

        if (jumpOnNextTick) {
            it.jumping = true

            println("A: " + player.pos)

            this.jumpOnNextTick = false

            return@handler
        }

        val shouldJump = player.moving &&
            player.isOnGround &&
            !player.isSneaking &&
            !mc.options.sneakKey.isPressed &&
            !mc.options.jumpKey.isPressed

        val line = this.thresholdLine ?: return@handler

        val (fallOffTick, fallOffPos, lineIntersect) = findFallOffPos(line, it.directionalInput) ?: return@handler

//        println(fallOffPos.distance(lineIntersect))

        if (fallOffPos.distance(lineIntersect) > this.play + 0.05 && fallOffTick > 11 && !wasStalling) {
            it.directionalInput = it.directionalInput.copy(forwards = false)

            wasStalling = true
        }

        wasStalling = true

        if (shouldJump && fallOffTick == 0) {
            jumpOnNextTick = true
        }
    }
    private var wasStalling = false

    private var thresholdLine: LineSegment? = null
    private var play: Double = 1.0
    private var directionalInput: DirectionalInput? = null

    inline fun plotLine(from: Vec2i, to: Vec2i, callback: (Int, Int) -> Unit) {
        var x0 = from.x
        var y0 = from.y

        val dx = abs((to.x - x0).toDouble()).toInt()
        val dy = abs((to.y - y0).toDouble()).toInt()


        val sx = if (x0 < to.x) 1 else -1
        val sy = if (y0 < to.y) 1 else -1


        var err = dx - dy

        var e2: Int

        while (true) {
            callback(x0, y0)

            if (x0 == to.x && y0 == to.y)
                break

            e2 = 2 * err

            if (e2 > -dy) {
                err -= dy
                x0 += sx
            }

            if (e2 < dx) {
                err += dx
                y0 += sy
            }
        }
    }

    inline fun dilate(extendX: Boolean, crossinline otherFunction: (Int, Int) -> Unit): (Int, Int) -> Unit {
        return { x, y ->
            otherFunction(x, y)

            if (extendX) {
                otherFunction(x + 1, y)
                otherFunction(x - 1, y)
            } else {
                otherFunction(x, y + 1)
                otherFunction(x, y - 1)
            }
        }
    }

//    val distRepeatable = repeatable {
//        if (!player.isOnGround)
//            return@repeatable
//
//        var startPos = player.pos
//
//        val nTicks = 0
//
//        while (true) {
//            waitTicks(1)
//
//            if (player.isOnGround) {
//                val dist = startPos.subtract(player.pos).horizontalLength()
//
//                if (dist > 0.5) {
//                    println(dist)
//                }
//
//                startPos = player.pos
//            }
//
//        }
//    }

    val tick = repeatable {
        ModuleDebug.debugParameter(ModuleParkour, "Play [blocks]", "N/A")

        val rotVec = Vec3d(0.0, 0.0, 1.0).rotateY(-player.yaw.toRadians())

        val fromPoint = player.pos.add(rotVec.multiply(-1.0)).toBlockPos().horizontalComponent()
        val toPoint = player.pos.add(rotVec.multiply(8.0)).toBlockPos().horizontalComponent()

        val points = findInterestingPoints(fromPoint, toPoint)

        val platforms = discoverPlatforms(points, player.blockPos.y, fromPoint, toPoint)

        val nextGround = findHypotheticalPosOnGround()

        val currentPlatform = findNearestPlatform(platforms, nextGround) ?: return@repeatable

        val (jumpOffLine, jumpOffPoint) = findJumpOffPosition(nextGround, rotVec, currentPlatform) ?: return@repeatable

        ModuleDebug.debugGeometry(ModuleParkour, "XZ", ModuleDebug.DebuggedPoint(jumpOffPoint, Color4b.WHITE))

        tagReachable(platforms, jumpOffPoint, nextGround)

        val targetPlatform = findNearestPlatform(
            platforms.filter { it.reachable == true },
            Vec3d(toPoint.x + 0.5, player.pos.y + 1.0, toPoint.y + 0.5)
        )

        if (targetPlatform == null) {
            debugPlatforms(platforms, currentPlatform)

            return@repeatable
        }

        targetPlatform.isTarget = true

        debugPlatforms(platforms, currentPlatform)

        val targetLineSegment = LineSegment(nextGround, rotVec, 0.0..8.0)

        val eee = findIntersects(targetPlatform, targetLineSegment, targetPlatform.platformBlocks[0].y + 1.0)

        val sss = eee.minByOrNull { it.second.squaredDistanceTo(player.pos.add(0.0, 1.0, 0.0)) }

        if (sss == null) {
            return@repeatable
        }

        val targetPos = sss.second

        ModuleDebug.debugGeometry(ModuleParkour, "YYYZ", ModuleDebug.DebuggedPoint(targetPos, Color4b.BLACK))

        val horizontalLength = targetPos.subtract(jumpOffPoint).horizontalLength()
        val maxJumpLen = approxJumpDistance(jumpOffPoint.y - targetPos.y)!!

        play = maxJumpLen - horizontalLength

        ModuleDebug.debugParameter(
            ModuleParkour,
            "Play [blocks]",
            DecimalFormat("0.00").format(maxJumpLen - horizontalLength)
        )

        thresholdLine = jumpOffLine

        return@repeatable
    }

    private fun findHypotheticalPosOnGround(): Vec3d {
        val directionalInput = this.directionalInput

        if (player.isOnGround || directionalInput == null) {
            return player.pos
        }

        val simulatedPlayer = SimulatedPlayer.fromClientPlayer(
            SimulatedPlayer.SimulatedPlayerInput.fromClientPlayer(directionalInput)
        )

        for (ignored in 0..20) {
            simulatedPlayer.tick()

            if (simulatedPlayer.onGround) {
                return simulatedPlayer.pos
            }
        }

        // We failed... The current player pos might be the best approximation that we have in this situation
        return player.pos
    }

    private fun findInterestingPoints(fromPoint: Vec2i, toPoint: Vec2i): ArrayList<Vec2i> {
        val points = ArrayList<Vec2i>()

        val delta = toPoint - fromPoint

        val extendX = delta.x.absoluteValue < delta.y.absoluteValue

        val filter = dilate(extendX) { px, py ->
            points.add(Vec2i(px, py))
        }

        plotLine(fromPoint, toPoint, filter)

        return points
    }

}
