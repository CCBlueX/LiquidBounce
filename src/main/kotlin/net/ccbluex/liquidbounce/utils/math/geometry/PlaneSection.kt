package net.ccbluex.liquidbounce.utils.math.geometry

import it.unimi.dsi.fastutil.doubles.DoubleDoublePair
import net.ccbluex.liquidbounce.utils.kotlin.component1
import net.ccbluex.liquidbounce.utils.kotlin.component2
import net.ccbluex.liquidbounce.utils.kotlin.step
import net.ccbluex.liquidbounce.utils.math.plus
import net.ccbluex.liquidbounce.utils.math.times
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d
import kotlin.math.sqrt

class PlaneSection(
    val originPoint: Vec3d,
    val dirVec1: Vec3d,
    val dirVec2: Vec3d
) {

    inline fun castPointsOnUniformly(maxPoints: Int, consumer: (Vec3d) -> Unit) {
        val (dz, dy) = getFairStepSide(maxPoints)

        for (y in 0.0..1.0 step dy) {
            for (z in 0.0..1.0 step dz) {
                val point = this.originPoint + this.dirVec1 * y + this.dirVec2 * z

                consumer(point)
            }
        }
    }

    fun getFairStepSide(nPoints: Int): DoubleDoublePair {
        val aspectRatio = this.dirVec2.length() / this.dirVec1.length()

        val vec1zero = MathHelper.approximatelyEquals(this.dirVec1.length(), 0.0)
        val vec2zero = MathHelper.approximatelyEquals(this.dirVec2.length(), 0.0)

        return when {
            !vec1zero && !vec2zero -> {
                val dz = sqrt(1 / (aspectRatio * nPoints))
                val dy = sqrt(aspectRatio / nPoints)

                DoubleDoublePair.of(dz, dy)
            }
            vec1zero && vec2zero -> DoubleDoublePair.of(1.0, 1.0)
            vec1zero -> DoubleDoublePair.of(1.0, 2.0 / nPoints)
            else -> DoubleDoublePair.of(2.0 / nPoints, 1.0)
        }
    }

}
