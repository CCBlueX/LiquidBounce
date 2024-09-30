/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.extensions

import net.minecraft.block.Block
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.entity.Entity
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.Vec3
import net.minecraft.util.Vec3i

/**
 * Provides:
 * ```
 * val (x, y, z) = blockPos
 */
operator fun Vec3i.component1() = x
operator fun Vec3i.component2() = y
operator fun Vec3i.component3() = z

/**
 * Provides:
 * ```
 * val (x, y, z) = vec
 */
operator fun Vec3.component1() = xCoord
operator fun Vec3.component2() = yCoord
operator fun Vec3.component3() = zCoord

/**
 * Provides:
 * ```
 * val (x, y, z) = mc.thePlayer
 */
operator fun Entity.component1() = posX
operator fun Entity.component2() = posY
operator fun Entity.component3() = posZ

/**
 * Provides:
 * ```
 * val (width, height) = ScaledResolution(mc)
 */
operator fun ScaledResolution.component1() = this.scaledWidth
operator fun ScaledResolution.component2() = this.scaledHeight

/**
 * Provides:
 * `vec + othervec`, `vec - othervec`, `vec * number`, `vec / number`
 * */
operator fun Vec3.plus(vec: Vec3): Vec3 = add(vec)
operator fun Vec3.minus(vec: Vec3): Vec3 = subtract(vec)
operator fun Vec3.times(number: Double) = Vec3(xCoord * number, yCoord * number, zCoord * number)
operator fun Vec3.div(number: Double) = times(1 / number)

val Vec3_ZERO: Vec3
    get() = Vec3(0.0, 0.0, 0.0)
fun Vec3.toFloatTriple() = Triple(xCoord.toFloat(), yCoord.toFloat(), zCoord.toFloat())

fun Float.toRadians() = this * 0.017453292f
fun Float.toRadiansD() = toRadians().toDouble()
fun Float.toDegrees() = this * 57.29578f
fun Float.toDegreesD() = toDegrees().toDouble()

/**
 * Prevents possible NaN / (-) Infinity results.
 */
infix fun Int.safeDiv(b: Int) = if (b == 0) 0f else this.toFloat() / b.toFloat()
infix fun Float.safeDiv(b: Float) = if (b == 0f) 0f else this / b

fun Double.toRadians() = this * 0.017453292
fun Double.toRadiansF() = toRadians().toFloat()
fun Double.toDegrees() = this * 57.295779513
fun Double.toDegreesF() = toDegrees().toFloat()

/**
 * Provides: (step is 0.1 by default)
 * ```
 *      for (x in 0.1..0.9 step 0.05) {}
 *      for (y in 0.1..0.9) {}
 */
class RangeIterator(
    private val range: ClosedFloatingPointRange<Double>, private val step: Double = 0.1,
) : Iterator<Double> {
    private var value = range.start

    override fun hasNext() = value < range.endInclusive

    override fun next(): Double {
        val returned = value
        value = (value + step).coerceAtMost(range.endInclusive)
        return returned
    }
}

operator fun ClosedFloatingPointRange<Double>.iterator() = RangeIterator(this)
infix fun ClosedFloatingPointRange<Double>.step(step: Double) = RangeIterator(this, step)

fun ClosedFloatingPointRange<Float>.random(): Float {
    require(start.isFinite())
    require(endInclusive.isFinite())
    return (start + (endInclusive - start) * Math.random()).toFloat()
}

/**
 * Conditionally shuffles an `Iterable`
 * @param shuffle determines if the returned `Iterable` is shuffled
 */
fun <T> Iterable<T>.shuffled(shuffle: Boolean) = toMutableList().apply { if (shuffle) shuffle() }

fun AxisAlignedBB.lerpWith(x: Double, y: Double, z: Double) =
    Vec3(minX + (maxX - minX) * x, minY + (maxY - minY) * y, minZ + (maxZ - minZ) * z)

fun AxisAlignedBB.lerpWith(point: Vec3) = lerpWith(point.xCoord, point.yCoord, point.zCoord)
fun AxisAlignedBB.lerpWith(value: Double) = lerpWith(value, value, value)

val AxisAlignedBB.center
    get() = lerpWith(0.5)

fun Block.lerpWith(x: Double, y: Double, z: Double) = Vec3(
    blockBoundsMinX + (blockBoundsMaxX - blockBoundsMinX) * x,
    blockBoundsMinY + (blockBoundsMaxY - blockBoundsMinY) * y,
    blockBoundsMinZ + (blockBoundsMaxZ - blockBoundsMinZ) * z
)
