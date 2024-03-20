package net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.grim

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.PlayerTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.ModuleSpeed
import net.ccbluex.liquidbounce.utils.entity.directionYaw
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.decoration.ArmorStandEntity
import kotlin.math.cos
import kotlin.math.sin

object SpeedGrimCollide : Choice("GrimCollide") {

    override val parent: ChoiceConfigurable<Choice>
        get() = ModuleSpeed.modes

    /**
     * Grim Collide mode for the Speed module.
     * The simulation when colliding with another player basically gives lenience.
     *
     * We can exploit this by increasing our speed by
     * 0.08 when we collide with any entity.
     *
     * This only works on client version being 1.9+.
     */
    val tickHandler = handler<PlayerTickEvent> {
        if (player.input.movementForward == 0.0f && player.input.movementSideways == 0.0f) { return@handler }
        var collisions = 0
        val box = player.boundingBox.expand(1.0)
        for (entity in world.entities) {
            val entityBox = entity.boundingBox
            if (canCauseSpeed(entity) && box.intersects(entityBox)) {
                collisions++
            }
        }

        // Grim gives 0.08 leniency per entity.
        val yaw = Math.toRadians(player.directionYaw.toDouble())
        val boost = 0.08 * collisions
        player.addVelocity(-sin(yaw) * boost, 0.0, cos(yaw) * boost)
    }

    private fun canCauseSpeed(entity: Entity) =
        entity != player && entity is LivingEntity && entity !is ArmorStandEntity

}
