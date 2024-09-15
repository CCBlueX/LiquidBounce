package net.ccbluex.liquidbounce.features.module.modules.combat.aimbot

import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.render.trajectories.TrajectoryInfo
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsConfigurable
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.minecraft.client.option.KeyBinding
import net.minecraft.entity.Entity
import net.minecraft.util.Hand
import net.minecraft.util.math.Vec3d
import org.lwjgl.glfw.GLFW

object ModuleDroneControl : Module("DroneControl", Category.COMBAT) {

    private val rotationsConfigurable = tree(RotationsConfigurable(this))

    var screen: DroneControlScreen? = null

    override fun enable() {
        screen = DroneControlScreen()

        mc.setScreen(screen)
    }

    override fun disable() {
        if (mc.currentScreen == screen) {
            mc.setScreen(null)
        }

        screen = null
    }

    var currentTarget: Pair<Entity, Vec3d>? = null
    var mayShoot = false

    private val repeatable = repeatable {
        val currentRotation = currentTarget?.let { (entity, pos) ->
            ModuleProjectileAimbot.aimFor(pos, entity.dimensions, TrajectoryInfo.BOW_FULL_PULL)
        }

        if (currentRotation != null) {
            RotationManager.aimAt(
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
