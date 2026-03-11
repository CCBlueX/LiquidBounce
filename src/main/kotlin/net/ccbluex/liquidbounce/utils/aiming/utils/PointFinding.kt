/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
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

package net.ccbluex.liquidbounce.utils.aiming.utils

import net.ccbluex.fastutil.mapToArray
import net.ccbluex.liquidbounce.features.module.modules.combat.aimbot.ModuleProjectileAimbot
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug.debugGeometry
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.client.world
import net.ccbluex.liquidbounce.utils.math.add
import net.ccbluex.liquidbounce.utils.math.firstHit
import net.ccbluex.liquidbounce.utils.math.geometry.Line
import net.ccbluex.liquidbounce.utils.math.geometry.NormalizedPlane
import net.ccbluex.liquidbounce.utils.math.geometry.PlaneSection
import net.ccbluex.liquidbounce.utils.math.minus
import net.ccbluex.liquidbounce.utils.math.plus
import net.ccbluex.liquidbounce.utils.math.times
import net.ccbluex.liquidbounce.utils.math.toVec3d
import net.ccbluex.liquidbounce.utils.math.withLength
import net.minecraft.world.entity.projectile.arrow.Arrow
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import org.joml.Matrix3f
import org.joml.Vector3f
import kotlin.jvm.optionals.getOrNull
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

val AABB.edgePoints: Array<Vec3>
    get() = arrayOf(
        Vec3(minX, minY, minZ),
        Vec3(minX, minY, maxZ),
        Vec3(minX, maxY, minZ),
        Vec3(minX, maxY, maxZ),
        Vec3(maxX, minY, minZ),
        Vec3(maxX, minY, maxZ),
        Vec3(maxX, maxY, minZ),
        Vec3(maxX, maxY, maxZ),
    )

/**
 * Creates rotation matrices: The first allows to turn the vec (1.0, 0.0, 0.0) into the given [vec].
 * The second allows to turn the given vec into (1.0, 0.0, 0.0).
 */
fun getRotationMatricesForVec(vec: Vec3): Pair<Matrix3f, Matrix3f> {
    val hypotenuse = hypot(vec.x, vec.z)

    val yawAtan = atan2(vec.z, vec.x).toFloat()
    val pitchAtan = atan2(vec.y, hypotenuse).toFloat()

    val toMatrix = Matrix3f().rotateY(-yawAtan).mul(Matrix3f().rotateZ(pitchAtan))
    val backMatrix = Matrix3f().rotateZ(-pitchAtan).mul(Matrix3f().rotateY(yawAtan))

    return toMatrix to backMatrix
}

/**
 * Projects points onto the [targetBox]. The points are uniformly distributed from the perspective of [virtualEye].
 *
 * @return a list of projected points, or null if the virtual eye is inside the target box.
 */
fun projectPointsOnBox(virtualEye: Vec3, targetBox: AABB, maxPoints: Int = 128): MutableList<Vec3>? {
    val list = ArrayList<Vec3>()

    val success = projectPointsOnBox(virtualEye, targetBox, maxPoints) {
        list.add(it)
    }

    if (!success) {
        return null
    }

    return list
}

/**
 * Projects points onto the [targetBox]. The points are uniformly distributed from the perspective of [virtualEye].
 *
 * @return `false` if the virtual eye is inside the target box.
 */
inline fun projectPointsOnBox(
    virtualEye: Vec3,
    targetBox: AABB,
    maxPoints: Int = 128,
    consumer: (Vec3) -> Unit
): Boolean {
    if (targetBox.contains(virtualEye)) {
        return false
    }

    val playerToBoxLine = Line(position = virtualEye, direction = targetBox.center - virtualEye)

    // Find a point between the virtual eye and the target box such that every edge point of the box is behind it
    // (from the perspective of the virtual eye). This position is used to craft the targeting frame
    val targetFrameOrigin = targetBox.edgePoints
        .mapToArray { playerToBoxLine.getNearestPointTo(it) }
        .minBy { it.distanceToSqr(virtualEye) }
        .lerp(virtualEye, 0.1)

    val plane = NormalizedPlane(targetFrameOrigin, playerToBoxLine.direction)
    val (toMatrix, backMatrix) = getRotationMatricesForVec(plane.normalVec)

    val projectedAndRotatedPoints = targetBox.edgePoints.mapToArray {
        plane.intersection(Line.fromPoints(virtualEye, it))!!.subtract(targetFrameOrigin).toVector3f().mul(backMatrix)
    }

    var minZ = 0.0F
    var maxZ = 0.0F
    var minY = 0.0F
    var maxY = 0.0F

    projectedAndRotatedPoints.forEach {
        minZ = min(minZ, it.z)
        maxZ = max(maxZ, it.z)
        minY = min(minY, it.y)
        maxY = max(maxY, it.y)
    }

    val posVec = Vector3f(0f, minY, minZ).mul(toMatrix).add(targetFrameOrigin).toVec3d()
    val dirVecY = Vector3f(0f, maxY - minY, 0f).mul(toMatrix).toVec3d()
    val dirVecZ = Vector3f(0f, 0f, maxZ - minZ).mul(toMatrix).toVec3d()

    val planeSection = PlaneSection(posVec, dirVecY, dirVecZ)

    planeSection.castPointsOnUniformly(maxPoints) { point ->
        // Extent the point from the face on.
        val pointExtended = point.lerp(virtualEye, -100.0)

        val pos = targetBox.clip(virtualEye, pointExtended).getOrNull() ?: return@castPointsOnUniformly

        consumer(pos)
    }

    return true
}

/**
 * Finds a point that is visible from the virtual eyes.
 *
 * ## Algorithm
 * 1. Projects points on the box from the virtual eyes.
 * 2. Sorts the points by distance to the box center.
 * 3. For each point:
 *      - Creates a ray starting from the point, extending for twice the range.
 *      - Raycasts the ray against the box to find the intersection point.
 *      - Checks if the intersection point is within the range and satisfies the [visibilityPredicate].
 * 4. Returns the first visible point found, or null if no point is visible.
 *
 * @param rangeToTest The maximum distance to test for visibility.
 * @param visibilityPredicate An optional predicate to determine if a given point is visible
 * @return the best visible spot found or `null`
 */
@Suppress("detekt:complexity.LongParameterList")
fun findVisiblePointFromVirtualEye(
    virtualEyes: Vec3,
    box: AABB,
    rangeToTest: Double,
    visibilityPredicate: VisibilityPredicate = ArrowVisibilityPredicate,
): Vec3? {
    val points = projectPointsOnBox(virtualEyes, box) ?: return null

    ModuleProjectileAimbot.debugGeometry("points") {
        ModuleDebug.DebugCollection(points.map { ModuleDebug.DebuggedPoint(it, Color4b.BLUE, 0.01) })
    }

    val rays = ArrayList<ModuleDebug.DebuggedGeometry>()

    val center = box.center
    points.sortBy { it.distanceToSqr(center) }

    for (spot in points) {
        val vecFromEyes = spot - virtualEyes
        val raycastTarget = vecFromEyes * 2.0 + virtualEyes
        val spotOnBox = box.firstHit(virtualEyes, raycastTarget) ?: continue

        val rayStart = spotOnBox - vecFromEyes.withLength(rangeToTest)

        val visible = visibilityPredicate.isVisible(rayStart, spotOnBox)

        rays.add(ModuleDebug.DebuggedLineSegment(rayStart, spotOnBox, if (visible) Color4b.GREEN else Color4b.RED))

        if (visible) {
            ModuleProjectileAimbot.debugGeometry("rays") { ModuleDebug.DebugCollection(rays) }
            return spotOnBox
        }
    }

    ModuleProjectileAimbot.debugGeometry("rays") { ModuleDebug.DebugCollection(rays) }

    return null
}

object ArrowVisibilityPredicate : VisibilityPredicate {
    override fun isVisible(eyesPos: Vec3, targetSpot: Vec3): Boolean {
        val arrowEntity = Arrow(
            world, eyesPos.x, targetSpot.y, targetSpot.z, ItemStack(Items.ARROW),
            null)

        return world.clip(
            ClipContext(
                eyesPos,
                targetSpot,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                arrowEntity
            )
        ).type == HitResult.Type.MISS
    }
}
