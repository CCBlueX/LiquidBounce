package net.ccbluex.liquidbounce.features.module.modules.combat.aimbot

import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsConfigurable
import net.ccbluex.liquidbounce.utils.aiming.projectiles.SituationalProjectileAngleCalculator
import net.ccbluex.liquidbounce.utils.entity.ConstantPositionExtrapolation
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.render.trajectory.TrajectoryInfo
import net.minecraft.entity.Entity
import net.minecraft.util.Hand
import net.minecraft.util.math.Vec3d

object ModuleDroneControl : ClientModule("DroneControl", Category.COMBAT) {

    private val rotationsConfigurable = tree(RotationsConfigurable(this))

    var screen: DroneControlScreen? = null

    override fun onEnabled() {
        screen = DroneControlScreen()

        mc.setScreen(screen)
    }

    override fun onDisabled() {
        if (mc.currentScreen == screen) {
            mc.setScreen(null)
        }

        screen = null
    }

    var currentTarget: Pair<Entity, Vec3d>? = null
    var mayShoot = false

    private val repeatable = tickHandler {
        val currentRotation = currentTarget?.let { (entity, pos) ->
            SituationalProjectileAngleCalculator.calculateAngleFor(
                TrajectoryInfo.BOW_FULL_PULL,
                sourcePos = player.eyePos,
                targetPosFunction = ConstantPositionExtrapolation(pos),
                targetShape = entity.dimensions
            )
        }

        if (currentRotation != null) {
            RotationManager.setRotationTarget(
                rotation = currentRotation,
                configurable = rotationsConfigurable,
                priority = Priority.NORMAL,
                provider = ModuleDroneControl
            )
        }

        if (mayShoot) {
            interaction.stopUsingItem(player)

            mayShoot = false
        } else {
            interaction.interactItem(player, Hand.MAIN_HAND)
        }
    }

}
