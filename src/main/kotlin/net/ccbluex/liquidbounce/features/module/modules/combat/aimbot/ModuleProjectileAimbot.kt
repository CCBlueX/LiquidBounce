package net.ccbluex.liquidbounce.features.module.modules.combat.aimbot

import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsConfigurable
import net.ccbluex.liquidbounce.utils.aiming.projectiles.SituationalProjectileAngleCalculator
import net.ccbluex.liquidbounce.utils.combat.TargetSelector
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.render.trajectory.TrajectoryData

object ModuleProjectileAimbot : ClientModule("ProjectileAimbot", Category.COMBAT) {

    private val targetSelector = TargetSelector()
    private val rotations = RotationsConfigurable(this)

    init {
        tree(targetSelector)
        tree(rotations)
    }

    @Suppress("unused")
    private val tickHandler = tickHandler {
        val target = targetSelector.targets().firstOrNull() ?: return@tickHandler

        val rotation = player.handItems.firstNotNullOfOrNull {
            if (it.item == null) {
                return@firstNotNullOfOrNull null
            }

            val trajectory = TrajectoryData.getRenderedTrajectoryInfo(
                player,
                it.item,
                true
            ) ?: return@firstNotNullOfOrNull null

            SituationalProjectileAngleCalculator.calculateAngleForEntity(trajectory, target)
        } ?: return@tickHandler

        RotationManager.setRotationTarget(
            rotation,
            considerInventory = false,
            rotations,
            Priority.IMPORTANT_FOR_USAGE_1,
            ModuleProjectileAimbot
        )
    }



}
