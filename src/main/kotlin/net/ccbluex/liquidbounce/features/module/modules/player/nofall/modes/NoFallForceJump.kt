package net.ccbluex.liquidbounce.features.module.modules.player.nofall.modes

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.player.nofall.ModuleNoFall
import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.entity.FallingPlayer
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.util.math.Vec3d

/**
 * NoFallForceJump mode for the NoFall module.
 * This mode forces the player to jump just when his about to land,
 * preventing fall damage.
 */
internal object NoFallForceJump : Choice("ForceJump") {
    override val parent: ChoiceConfigurable<*>
        get() = ModuleNoFall.modes

    private val blockDistance by int("Block Distance", 1, 0..5)
    private val fallDistance by float("Fall Distance", 3.35f, 3.35f..10.0f)
    private val jumpHeight by float("Jump Height", 0.42f, 0.1f..0.42f)
    private var jumpTriggered: Boolean = false

    /**
     * Handles the packet event to check if a force jump should be triggered.
     * This is done by checking if the player's fall distance is higher than the specific (fallDistance)
     * and if the player is above a nonair block by the specific block distance.
     */
    val packetHandler = handler<PacketEvent> {
        val packet = it.packet
        if (packet is PlayerMoveC2SPacket && player.fallDistance > fallDistance) {
            val collision = FallingPlayer.fromPlayer(player).findCollision(20)?.pos ?: return@handler

            if (!jumpTriggered && collision.getState()?.isAir == true) {
                forceJump()
            }
        }
        resetJumpTrigger()
    }

    /**
     * Reseting the jump triggered flag if the player is on the ground.
     */
    private fun resetJumpTrigger() {
        if (player.isOnGround) {
            jumpTriggered = false
        }
    }

    /**
     * Forces the player to jump by setting their velocity.
     */
    private fun forceJump() {
        val velocity = player.velocity
        val jumpVelocity = jumpHeight.toDouble()
        val newVelocity = Vec3d(velocity.x, jumpVelocity, velocity.z)
        player.velocity = newVelocity
        jumpTriggered = true
    }
}
