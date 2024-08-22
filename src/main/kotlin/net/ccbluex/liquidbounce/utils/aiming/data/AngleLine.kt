package net.ccbluex.liquidbounce.utils.aiming.data

import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.features.FightBot.player
import net.ccbluex.liquidbounce.utils.math.component1
import net.ccbluex.liquidbounce.utils.math.component2
import net.ccbluex.liquidbounce.utils.math.component3
import net.minecraft.util.math.Box
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * Represents an angle line from one point to another.
 */
data class AngleLine(val fromPoint: Vec3d = player.eyePos, val toPoint: Vec3d) {

    constructor(fromPoint: Vec3d = player.eyePos, orientation: Orientation)
        : this(fromPoint, Vec3d.fromPolar(orientation.pitch, orientation.yaw))

    val direction: Vec3d
        get() = toPoint.subtract(fromPoint)

    val length: Double
        get() = direction.length()

    val orientation: Orientation
        get() {
            val (diffX, diffY, diffZ) = direction

            return Orientation(
                MathHelper.wrapDegrees(Math.toDegrees(atan2(diffZ, diffX)).toFloat() - 90f),
                MathHelper.wrapDegrees((-Math.toDegrees(atan2(diffY, sqrt(diffX * diffX + diffZ * diffZ)))).toFloat())
            )
        }

    fun differenceTo(rotation: Orientation) = rotation.differenceTo(this.orientation)

}

/**
 * Represents a point in the target box.
 */
data class BoxedAnglePoint(val fromPoint: Vec3d, val toPoint: Vec3d, val box: Box, val cutOffBox: Box) {
    val angleLine
        get() = AngleLine(fromPoint, toPoint)
}
