/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.api.minecraft.util

import kotlin.math.sqrt

class WVec3(
        val xCoord: Double,
        val yCoord: Double,
        val zCoord: Double
) {
    inline fun addVector(x: Double, y: Double, z: Double): WVec3 = WVec3(xCoord + x, yCoord + y, zCoord + z)

    fun distanceTo(vec: WVec3): Double {
        val d0: Double = vec.xCoord - xCoord
        val d1: Double = vec.yCoord - yCoord
        val d2: Double = vec.zCoord - zCoord

        return sqrt(d0 * d0 + d1 * d1 + d2 * d2)
    }

    inline fun add(vec: WVec3): WVec3 = addVector(vec.xCoord, vec.yCoord, vec.zCoord)

    constructor(blockPos: WVec3i) : this(blockPos.x.toDouble(), blockPos.y.toDouble(), blockPos.z.toDouble())
}