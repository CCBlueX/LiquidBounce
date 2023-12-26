package net.ccbluex.liquidbounce.features.module.modules.movement.speed

import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.entity.SimulatedPlayer
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.util.math.Vec3d

object SpeedPreventDeadlyJump {

    fun wouldJumpToDeath(maxFallDistance: Double = 10.0): Boolean {
        val player = mc.player!!

        val simulatedPlayer = createSimulatedPlayer(player)

        simulatedPlayer.jump()

        var groundPos: Vec3d? = null

        for (ignored in 0..40) {
            simulatedPlayer.tick()

            if (simulatedPlayer.onGround) {
                groundPos = simulatedPlayer.pos

                break
            }
        }

        if (groundPos == null)
            return true

        simulatedPlayer.input = SimulatedPlayer.SimulatedPlayerInput(
            DirectionalInput.NONE, jumping = false,
            sprinting = false, sneaking = false
        )

        return wouldFallToDeath(simulatedPlayer, ticksToWaitForFall = 5, maxFallDistance = maxFallDistance)
    }

    fun createSimulatedPlayer(player: ClientPlayerEntity): SimulatedPlayer {
        val input = SimulatedPlayer.SimulatedPlayerInput(
            DirectionalInput(player.input),
            jumping = false,
            sprinting = true,
            sneaking = false
        )

        return SimulatedPlayer.fromClientPlayer(input)
    }

    fun wouldFallToDeath(
        simulatedPlayer: SimulatedPlayer,
        ticksToWaitForFall: Int = 5,
        maxFallDistance: Double = 10.0
    ): Boolean {
        var groundPos: Vec3d? = null

        for (ignored in 0 until ticksToWaitForFall) {
            simulatedPlayer.tick()
        }

        for (ignored in 0..40) {
            simulatedPlayer.tick()

            if (simulatedPlayer.onGround) {
                groundPos = simulatedPlayer.pos

                break
            }
        }

        if (groundPos != null) {
            simulatedPlayer.input = SimulatedPlayer.SimulatedPlayerInput(
                DirectionalInput.NONE,
                jumping = false,
                sprinting = false,
                sneaking = false
            )

            for (ignored in 0..40) {
                simulatedPlayer.tick()

                groundPos = if (simulatedPlayer.onGround) {
                    simulatedPlayer.pos
                } else {
                    null;
                }
            }
        }

        return groundPos == null || mc.player!!.y - groundPos.y > maxFallDistance
    }

}
