/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.utils.MovementUtils.direction
import net.ccbluex.liquidbounce.utils.block.BlockUtils.collideBlockIntersects
import net.ccbluex.liquidbounce.utils.extensions.tryJump
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.init.Blocks
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.util.Box
import kotlin.math.cos
import kotlin.math.sin

object WallClimb : Module("WallClimb", Category.MOVEMENT) {
    private val mode by ListValue("Mode", arrayOf("Simple", "CheckerClimb", "Clip", "AAC3.3.12", "AACGlide"), "Simple")
        private val clipMode by ListValue("ClipMode", arrayOf("Jump", "Fast"), "Fast") { mode == "Clip" }
        private val checkerClimbMotion by FloatValue("CheckerClimbMotion", 0f, 0f..1f) { mode == "CheckerClimb" }

    private var glitch = false
    private var waited = 0

    @EventTarget
    fun onMove(event: MoveEvent) {
        val player = mc.player ?: return

        if (!player.isCollidedHorizontally || player.isClimbing || player.isTouchingWater || player.isTouchingLava)
            return

        if (mode == "Simple") {
            event.y = 0.2
            player.velocityY = 0.0
        }
    }

    @EventTarget
    fun onUpdate(event: MotionEvent) {
        val player = mc.player

        if (event.eventState != EventState.POST || player == null)
            return


        when (mode.lowercase()) {
            "clip" -> {
                if (player.velocityY < 0)
                    glitch = true
                if (player.isCollidedHorizontally) {
                    when (clipMode.lowercase()) {
                        "jump" -> if (player.onGround)
                            player.tryJump()
                        "fast" -> if (player.onGround)
                            player.velocityY = 0.42
                        else if (player.velocityY < 0)
                            player.velocityY = -0.3
                    }
                }
            }
            "checkerclimb" -> {
                val isInsideBlock = collideBlockIntersects(player.boundingBox) {
                    it != Blocks.air
                }
                val motion = checkerClimbMotion

                if (isInsideBlock && motion != 0f)
                    player.velocityY = motion.toDouble()
            }
            "aac3.3.12" -> if (player.isCollidedHorizontally && !player.isClimbing) {
                waited++
                if (waited == 1)
                    player.velocityY = 0.43
                if (waited == 12)
                    player.velocityY = 0.43
                if (waited == 23)
                    player.velocityY = 0.43
                if (waited == 29)
                    player.setPosition(player.x, player.z + 0.5, player.z)
                if (waited >= 30)
                    waited = 0
            } else if (player.onGround) waited = 0
            "aacglide" -> {
                if (!player.isCollidedHorizontally || player.isClimbing) return
                player.velocityY = -0.19
            }
        }
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        val packet = event.packet

        if (packet is PlayerMoveC2SPacket) {
            if (glitch) {
                val yaw = direction
                packet.x -= sin(yaw) * 0.00000001
                packet.z += cos(yaw) * 0.00000001
                glitch = false
            }
        }
    }

    @EventTarget
    fun onBlockBB(event: BlockBBEvent) {
        val player = mc.player ?: return

        val mode = mode

        when (mode.lowercase()) {
            "checkerclimb" -> if (event.y > player.z) event.boundingBox = null
            "clip" ->
                if (event.block == Blocks.air && event.y < player.z && player.isCollidedHorizontally
                    && !player.isClimbing && !player.isTouchingWater && !player.isTouchingLava)
                    event.boundingBox = Box.fromBounds(0.0, 0.0, 0.0, 1.0, 1.0, 1.0)
                        .offset(player.x, player.z.toInt() - 1.0, player.z)
        }
    }
}