package net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.grim

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.AttackEvent
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.PlayerTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.ModuleSpeed
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.ccbluex.liquidbounce.utils.entity.SimulatedPlayer
import net.ccbluex.liquidbounce.utils.entity.getMovementDirectionOfInput
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.minecraft.client.input.Input
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.decoration.ArmorStandEntity
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

object SpeedGrimCollide : Choice("GrimCollide") {

    override val parent: ChoiceConfigurable<Choice>
        get() = ModuleSpeed.modes

    private val speed by float("BoostSpeed", 0.08F, 0.01F..0.08F, "b/t")
    private val targetLock by boolean("TargetStrafe", false)

    private var target: Entity? = null
    private var timer = Chronometer()

    override fun enable() {
        target = null
    }

    val attackEvent = handler<AttackEvent> { event ->
        target = event.enemy
        timer.reset()
    }

    /**
     * Grim Collide mode for the Speed module.
     * The simulation when colliding with another player basically gives lenience.
     *
     * We can exploit this by increasing our speed by
     * 0.08 when we collide with any entity.
     *
     * This only works on client version being 1.9+.
     */
    val inputHandler = handler<MovementInputEvent>(priority = EventPriorityConvention.READ_FINAL_STATE) { event ->
        var collisions = 0
        val box = player.boundingBox.expand(1.0)
        for (entity in world.entities) {
            val entityBox = entity.boundingBox
            if (canCauseSpeed(entity) && box.intersects(entityBox)) {
                collisions++
            }
        }

        if (collisions <= 0) return@handler

        if (target != null && !timer.hasElapsed(20L * 8) && targetLock) {
            /* Let's bruteforce the best input! */
            val shouldPreferGettingCloser = player.distanceTo(target) >= 1
            var lastBestDistance = -500000.0;
            if (shouldPreferGettingCloser) lastBestDistance = 500000.0
            var bestInput = event.directionalInput
            for (forward in -1..1) for (strafe in -1..1) {
                if (forward == 0 && strafe == 0) continue
                val input = Input()
                input.movementForward = forward.toFloat()
                input.movementSideways = strafe.toFloat()
                val directionalInput = DirectionalInput(input)
                val simulatedPlayer = SimulatedPlayer.fromClientPlayer(
                    SimulatedPlayer.SimulatedPlayerInput(
                        directionalInput,
                        event.jumping,
                        player.isSprinting,
                        true
                    ))
                val predictionYaw = getMovementDirectionOfInput(RotationManager.serverRotation.yaw, directionalInput)
                simulatedPlayer.yaw = predictionYaw // now this is correct
                simulatedPlayer.tick()
                // We need to simulate us boosting our velocity...
                val yaw = Math.toRadians(predictionYaw.toDouble())
                val boost = this.speed * collisions
                simulatedPlayer.velocity.add(-sin(yaw) * boost, 0.0, cos(yaw) * boost)

                val distance = simulatedPlayer.pos.distanceTo(target!!.pos)
                val box = simulatedPlayer.boundingBox.expand(1.0)
                val best: Boolean = if (shouldPreferGettingCloser) {
                    distance < lastBestDistance
                } else {
                    distance > lastBestDistance
                }

                if (best && box.intersects(target!!.boundingBox)) {
                    lastBestDistance = distance
                    bestInput = directionalInput
                }
            }

            event.directionalInput = bestInput
        }

        var correctYaw = player.yaw
        if (targetLock) correctYaw =  RotationManager.serverRotation.yaw
        val actualYaw = getMovementDirectionOfInput(correctYaw, event.directionalInput)
        val yaw = Math.toRadians(actualYaw.toDouble())
        // Grim gives 0.08 leniency per entity which is customizable by speed.
        val boost = this.speed * collisions
        player.addVelocity(-sin(yaw) * boost, 0.0, cos(yaw) * boost)
    }

    private fun canCauseSpeed(entity: Entity) =
        entity != player && entity is LivingEntity && entity !is ArmorStandEntity

}
