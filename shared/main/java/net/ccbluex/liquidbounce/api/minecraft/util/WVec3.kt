/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.api.minecraft.util

import kotlin.math.cos
import kotlin.math.sin
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

    inline fun squareDistanceTo(vec: WVec3): Double {
        val d0 = vec.xCoord - xCoord
        val d1 = vec.yCoord - yCoord
        val d2 = vec.zCoord - zCoord

        return d0 * d0 + d1 * d1 + d2 * d2
    }

    inline fun add(vec: WVec3): WVec3 = addVector(vec.xCoord, vec.yCoord, vec.zCoord)

    fun rotatePitch(pitch: Float): WVec3 {
        val f: Float = cos(pitch)
        val f1: Float = sin(pitch)
        val d0 = xCoord
        val d1 = yCoord * f.toDouble() + zCoord * f1.toDouble()
        val d2 = zCoord * f.toDouble() - yCoord * f1.toDouble()
        return WVec3(d0, d1, d2)
    }

    fun rotateYaw(yaw: Float): WVec3 {
        val f: Float = cos(yaw)
        val f1: Float = sin(yaw)
        val d0 = xCoord * f.toDouble() + zCoord * f1.toDouble()
        val d1 = yCoord
        val d2 = zCoord * f.toDouble() - xCoord * f1.toDouble()
        return WVec3(d0, d1, d2)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as WVec3

        if (xCoord != other.xCoord) return false
        if (yCoord != other.yCoord) return false
        if (zCoord != other.zCoord) return false

        return true
    }

    override fun hashCode(): Int {
        var result = xCoord.hashCode()
        result = 31 * result + yCoord.hashCode()
        result = 31 * result + zCoord.hashCode()
        return result
    }

    constructor(blockPos: WVec3i) : this(blockPos.x.toDouble(), blockPos.y.toDouble(), blockPos.z.toDouble())


}