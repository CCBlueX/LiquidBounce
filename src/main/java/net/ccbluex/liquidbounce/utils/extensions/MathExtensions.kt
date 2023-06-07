package net.ccbluex.liquidbounce.utils.extensions

import net.minecraft.entity.Entity
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3

/**
 * Provides:
 * val (x, y, z) = vec
 */
operator fun Vec3.component1() = xCoord
operator fun Vec3.component2() = yCoord
operator fun Vec3.component3() = zCoord

/**
 * Provides:
 * val (x, y, z) = blockPos
 */
operator fun BlockPos.component1() = x
operator fun BlockPos.component2() = y
operator fun BlockPos.component3() = z

/**
 * Provides:
 * val (x, y, z) = mc.thePlayer
 */
operator fun Entity.component1() = posX
operator fun Entity.component2() = posY
operator fun Entity.component3() = posZ

fun Float.toRadians() = this * 0.017453292f
fun Float.toRadiansD() = toRadians().toDouble()
fun Float.toDegrees() = this * 57.29578f
fun Float.toDegreesD() = toDegrees().toDouble()

fun Double.toRadians() = this * 0.017453292
fun Double.toRadiansF() = toRadians().toFloat()
fun Double.toDegrees() = this * 57.295779513
fun Double.toDegreesF() = toDegrees().toFloat()