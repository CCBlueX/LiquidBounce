/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils

import javax.vecmath.Vector3d
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.sqrt

object PathUtils : MinecraftInstance() {
    fun findBlinkPath(tpX: Double, tpY: Double, tpZ: Double): List<Vector3d> {
        val positions = mutableListOf<Vector3d>()

        var curX = player.posX
        var curY = player.posY
        var curZ = player.posZ
        var distance = abs(curX - tpX) + abs(curY - tpY) + abs(curZ - tpZ)

        var count = 0
        while (distance > 0) {
            distance = abs(curX - tpX) + abs(curY - tpY) + abs(curZ - tpZ)

            val diffX = curX - tpX
            val diffY = curY - tpY
            val diffZ = curZ - tpZ
            val offset = if (count and 1 == 0) 0.4 else 0.1

            val minX = diffX.coerceIn(-offset, offset)
            curX -= minX

            val minY = diffY.coerceIn(-0.25, 0.25)
            curY -= minY

            val minZ = diffZ.coerceIn(-offset, offset)
            curZ -= minZ

            positions += Vector3d(curX, curY, curZ)
            count++
        }

        return positions
    }

    fun findPath(tpX: Double, tpY: Double, tpZ: Double, offset: Double): List<Vector3d> {
        val positions = mutableListOf<Vector3d>()
        val steps = ceil(getDistance(player.posX, player.posY, player.posZ, tpX, tpY, tpZ) / offset)

        val dX = tpX - player.posX
        val dY = tpY - player.posY
        val dZ = tpZ - player.posZ

        var d = 1.0
        while (d <= steps) {
            positions += Vector3d(player.posX + dX * d / steps, player.posY + dY * d / steps, player.posZ + dZ * d / steps)
            ++d
        }

        return positions
    }

    private fun getDistance(x1: Double, y1: Double, z1: Double, x2: Double, y2: Double, z2: Double): Double {
        val xDiff = x1 - x2
        val yDiff = y1 - y2
        val zDiff = z1 - z2

        return sqrt(xDiff * xDiff + yDiff * yDiff + zDiff * zDiff)
    }
}