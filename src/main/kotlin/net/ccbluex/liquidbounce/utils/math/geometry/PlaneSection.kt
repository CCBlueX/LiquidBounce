package net.ccbluex.liquidbounce.utils.math.geometry

import net.ccbluex.liquidbounce.utils.kotlin.step
import net.ccbluex.liquidbounce.utils.math.plus
import net.ccbluex.liquidbounce.utils.math.times
import net.minecraft.util.math.Vec3d
import kotlin.jvm.optionals.getOrNull
import kotlin.math.sqrt

class PlaneSection(
    val originPoint: Vec3d,
    val dirVec1: Vec3d,
    val dirVec2: Vec3d
) {

    inline fun castPointsOnUniformly(maxPoints: Int, consumer: (Vec3d) -> Unit) {
        val nPoints = maxPoints
        val aspectRatio = this.dirVec2.length() / this.dirVec1.length()
        val dz = sqrt(1 / (aspectRatio * nPoints))
        val dy = sqrt(aspectRatio / nPoints)

        for (y in 0.0..1.0 step dy) {
            for (z in 0.0..1.0 step dz) {
                val point = this.originPoint + this.dirVec1 * y + this.dirVec2 * z

                consumer(point)
            }
        }
    }

}
