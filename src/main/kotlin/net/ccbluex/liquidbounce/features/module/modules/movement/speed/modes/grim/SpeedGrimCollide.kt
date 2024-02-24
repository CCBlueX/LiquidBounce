package net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.grim

import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.PlayerTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.ModuleSpeed
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.SpeedBHopBase
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.decoration.ArmorStandEntity
import kotlin.math.sqrt

object SpeedGrimCollide : SpeedBHopBase("GrimCollide") {

    /**
     * The factor to multiply the player's velocity with when colliding with another player.
     *
     * 1.19f seems decent enough. Rarely flags for Simulation. 1.3f is the maximum that works.
     */
    private val factor by float("Factor", 1.19f, 1.1f..1.3f)

    override val parent: ChoiceConfigurable
        get() = ModuleSpeed.modes

    /**
     * Grim Collide mode for the Speed module.
     * The simulation when colliding with another player basically gives up.
     *
     * We can exploit this by increasing our speed when
     * we collide with another player.
     *
     * This only works on servers running more recent versions of Minecraft and the player being on the same version.
     */
    val tickHandler = handler<PlayerTickEvent> {
        val collidesWithAnother = world.entities.any {
            canCauseSpeed(it) && sqrt(player.squaredDistanceTo(it)) <= 1.5
        }

        if (collidesWithAnother) {
            val velocityX = player.velocity.x * factor
            val velocityZ = player.velocity.z * factor
            player.setVelocity(velocityX, player.velocity.y, velocityZ)
        }
    }

    private fun canCauseSpeed(entity: Entity) =
        entity != player && entity is LivingEntity && entity !is ArmorStandEntity

}
