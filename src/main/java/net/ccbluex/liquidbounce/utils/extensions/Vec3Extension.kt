package net.ccbluex.liquidbounce.utils.extensions

import net.minecraft.util.Vec3

/* Addition */

operator fun Vec3.plus(vec: Vec3): Vec3 = plus(vec.xCoord, vec.yCoord, vec.zCoord)

operator fun Vec3.plus(offset: Double): Vec3 = plus(offset, offset, offset)

fun Vec3.plus(x: Double, y: Double, z: Double): Vec3 = Vec3(xCoord + x, yCoord + y, zCoord + z)

/* Multiplication */

operator fun Vec3.times(vec: Vec3): Vec3 = Vec3(xCoord * vec.xCoord, yCoord * vec.yCoord, zCoord * vec.zCoord)

operator fun Vec3.times(multiplier: Double): Vec3 = Vec3(xCoord * multiplier, yCoord * multiplier, zCoord * multiplier)

fun Vec3.floor(): Vec3 = Vec3(kotlin.math.floor(xCoord), kotlin.math.floor(yCoord), kotlin.math.floor(zCoord))
