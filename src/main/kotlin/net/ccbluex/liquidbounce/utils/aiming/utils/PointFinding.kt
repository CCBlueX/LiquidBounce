package net.ccbluex.liquidbounce.utils.aiming.utils

import net.ccbluex.liquidbounce.features.module.modules.combat.aimbot.ModuleProjectileAimbot
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug.debugGeometry
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.client.world
import net.ccbluex.liquidbounce.utils.kotlin.mapArray
import net.ccbluex.liquidbounce.utils.math.geometry.Line
import net.ccbluex.liquidbounce.utils.math.geometry.NormalizedPlane
import net.ccbluex.liquidbounce.utils.math.geometry.PlaneSection
import net.ccbluex.liquidbounce.utils.math.minus
import net.ccbluex.liquidbounce.utils.math.plus
import net.ccbluex.liquidbounce.utils.math.times
import net.minecraft.entity.projectile.ArrowEntity
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d
import net.minecraft.world.RaycastContext
import org.joml.Matrix3f
import org.joml.Vector3f
import kotlin.jvm.optionals.getOrNull
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

val Box.edgePoints: Array<Vec3d>
    get() = arrayOf(
        Vec3d(minX, minY, minZ),
        Vec3d(minX, minY, maxZ),
        Vec3d(minX, maxY, minZ),
        Vec3d(minX, maxY, maxZ),
        Vec3d(maxX, minY, minZ),
        Vec3d(maxX, minY, maxZ),
        Vec3d(maxX, maxY, minZ),
        Vec3d(maxX, maxY, maxZ),
    )

fun Vec3d.moveTowards(otherPoint: Vec3d, fraction: Double): Vec3d {
    val direction = otherPoint - this

    return this + direction.multiply(fraction)
}

/**
 * Creates rotation matrices: The first allows to turn the vec (1.0, 0.0, 0.0) into the given [vec].
 * The second allows to turn the given vec into (1.0, 0.0, 0.0).
 */
fun getRotationMatricesForVec(vec: Vec3d): Pair<Matrix3f, Matrix3f> {
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
fun projectPointsOnBox(virtualEye: Vec3d, targetBox: Box, maxPoints: Int = 128): ArrayList<Vec3d>? {
    val list = ArrayList<Vec3d>()

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
    virtualEye: Vec3d,
    targetBox: Box,
    maxPoints: Int = 128,
    consumer: (Vec3d) -> Unit
): Boolean {
    if (targetBox.contains(virtualEye)) {
        return false
    }

    val playerToBoxLine = Line(position = virtualEye, direction = targetBox.center - virtualEye)

    // Find a point between the virtual eye and the target box such that every edge point of the box is behind it
    // (from the perspective of the virtual eye). This position is used to craft a the targeting frame
    val targetFrameOrigin = targetBox.edgePoints
        .mapArray { playerToBoxLine.getNearestPointTo(it) }
        .minBy { it.squaredDistanceTo(virtualEye) }
        .moveTowards(virtualEye, 0.1)

    val plane = NormalizedPlane(targetFrameOrigin, playerToBoxLine.direction)
    val (toMatrix, backMatrix) = getRotationMatricesForVec(plane.normalVec)

    val projectedAndRotatedPoints = targetBox.edgePoints.mapArray {
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

    val posVec = Vector3f(0f, minY, minZ).mul(toMatrix).toVec3d().add(targetFrameOrigin)
    val dirVecY = Vector3f(0f, maxY - minY, 0f).mul(toMatrix).toVec3d()
    val dirVecZ = Vector3f(0f, 0f, maxZ - minZ).mul(toMatrix).toVec3d()

    val planeSection = PlaneSection(posVec, dirVecY, dirVecZ)

    planeSection.castPointsOnUniformly(maxPoints) { point ->
        // Extent the point from the face on.
        val pointExtended = point.moveTowards(virtualEye, -100.0)

        val pos = targetBox.raycast(virtualEye, pointExtended).getOrNull() ?: return@castPointsOnUniformly

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
    virtualEyes: Vec3d,
    box: Box,
    rangeToTest: Double,
    visibilityPredicate: VisibilityPredicate = ArrowVisibilityPredicate,
): Vec3d? {
    val points = projectPointsOnBox(virtualEyes, box) ?: return null

    ModuleProjectileAimbot.debugGeometry("points") {
        ModuleDebug.DebugCollection(points.map { ModuleDebug.DebuggedPoint(it, Color4b.BLUE, 0.01) })
    }

    val rays = ArrayList<ModuleDebug.DebuggedGeometry>()

    val center = box.center
    points.sortBy { it.squaredDistanceTo(center) }

    for (spot in points) {
        val vecFromEyes = spot - virtualEyes
        val raycastTarget = vecFromEyes * 2.0 + virtualEyes
        val spotOnBox = box.raycast(virtualEyes, raycastTarget).getOrNull() ?: continue

        val rayStart = spotOnBox.subtract(vecFromEyes.normalize().multiply(rangeToTest))

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
    override fun isVisible(eyesPos: Vec3d, targetSpot: Vec3d): Boolean {
        val arrowEntity = ArrowEntity(
            world, eyesPos.x, targetSpot.y, targetSpot.z, ItemStack(Items.ARROW),
            null)

        return world.raycast(
            RaycastContext(
                eyesPos,
                targetSpot,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                arrowEntity
            )
        )?.let { it.type == HitResult.Type.MISS } ?: true
    }
}

fun Vector3f.toVec3d(): Vec3d = Vec3d(this.x.toDouble(), this.y.toDouble(), this.z.toDouble())
