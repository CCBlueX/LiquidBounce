package net.ccbluex.liquidbounce.features.module.modules.combat.aimbot

import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.utils.aiming.*
import net.ccbluex.liquidbounce.utils.client.world
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
import kotlin.jvm.optionals.getOrNull

/**
 * Find the best spot of a box to aim at.
 */
@Suppress("detekt:complexity.LongParameterList")
fun raytraceFromVirtualEye(
    virtualEyes: Vec3d,
    box: Box,
    rangeToTest: Double,
    visibilityPredicate: VisibilityPredicate = ArrowVisibilityPredicate,
): Vec3d? {
    val points = projectPointsOnBox(virtualEyes, box) ?: return null

    val debugCollection = ModuleDebug.DebugCollection(points.map { ModuleDebug.DebuggedPoint(it, Color4b.BLUE, 0.01) })

    ModuleDebug.debugGeometry(ModuleProjectileAimbot, "points", debugCollection)

    val rays = ArrayList<ModuleDebug.DebuggedGeometry>()

    val center = box.center
    val sortedPoints = points.sortedBy { it.distanceTo(center) }

    for (spot in sortedPoints) {
        val vecFromEyes = spot - virtualEyes
        val raycastTarget = vecFromEyes * 2.0 + virtualEyes
        val spotOnBox = box.raycast(virtualEyes, raycastTarget).getOrNull() ?: continue

        val rayStart = spotOnBox.subtract(vecFromEyes.normalize().multiply(rangeToTest))

        val visible = visibilityPredicate.isVisible(rayStart, spotOnBox)

        rays.add(ModuleDebug.DebuggedLineSegment(rayStart, spotOnBox, if (visible) Color4b.GREEN else Color4b.RED))

        if (visible) {
            ModuleDebug.debugGeometry(ModuleProjectileAimbot, "rays", ModuleDebug.DebugCollection(rays))
            return spotOnBox
        }
    }

    ModuleDebug.debugGeometry(ModuleProjectileAimbot, "rays", ModuleDebug.DebugCollection(rays))

    return null
}

object ArrowVisibilityPredicate : VisibilityPredicate {
    override fun isVisible(eyesPos: Vec3d, targetSpot: Vec3d): Boolean {
        val arrowEntity = ArrowEntity(world, eyesPos.x, targetSpot.y, targetSpot.z, ItemStack(Items.ARROW),
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

//@Suppress("detekt:complexity.LongParameterList")
//private fun considerSpot(
//    preferredSpot: Vec3d,
//    box: Box,
//    eyes: Vec3d,
//    visibilityPredicate: VisibilityPredicate,
//    rangeSquared: Double,
//    wallsRangeSquared: Double,
//    spot: Vec3d,
//    bestRotationTracker: BestRotationTracker,
//) {
//    // Elongate the line so we have no issues with fp-precision
//    val raycastTarget = (preferredSpot - eyes) * 2.0 + eyes
//    val spotOnBox = box.raycast(eyes, raycastTarget).getOrNull() ?: return
//    val distance = eyes.squaredDistanceTo(spotOnBox)
//
//    val visible = visibilityPredicate.isVisible(eyes, raycastTarget)
//
//    // Is either spot visible or distance within wall range?
//    if ((!visible || distance >= rangeSquared) && distance >= wallsRangeSquared) {
//        return
//    }
//
//    val rotation = RotationManager.makeRotation(spot, eyes)
//
//    bestRotationTracker.considerRotation(VecRotation(rotation, spot), visible)
//}
