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
import net.minecraft.network.packet.c2s.play.C03PacketPlayer
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
        val thePlayer = mc.player ?: return

        if (!thePlayer.isCollidedHorizontally || thePlayer.isClimbing || thePlayer.isTouchingWater || thePlayer.isTouchingLava)
            return

        if (mode == "Simple") {
            event.y = 0.2
            thePlayer.velocityY = 0.0
        }
    }

    @EventTarget
    fun onUpdate(event: MotionEvent) {
        val thePlayer = mc.player

        if (event.eventState != EventState.POST || thePlayer == null)
            return


        when (mode.lowercase()) {
            "clip" -> {
                if (thePlayer.velocityY < 0)
                    glitch = true
                if (thePlayer.isCollidedHorizontally) {
                    when (clipMode.lowercase()) {
                        "jump" -> if (thePlayer.onGround)
                            thePlayer.tryJump()
                        "fast" -> if (thePlayer.onGround)
                            thePlayer.velocityY = 0.42
                        else if (thePlayer.velocityY < 0)
                            thePlayer.velocityY = -0.3
                    }
                }
            }
            "checkerclimb" -> {
                val isInsideBlock = collideBlockIntersects(thePlayer.entityBoundingBox) {
                    it != Blocks.air
                }
                val motion = checkerClimbMotion

                if (isInsideBlock && motion != 0f)
                    thePlayer.velocityY = motion.toDouble()
            }
            "aac3.3.12" -> if (thePlayer.isCollidedHorizontally && !thePlayer.isClimbing) {
                waited++
                if (waited == 1)
                    thePlayer.velocityY = 0.43
                if (waited == 12)
                    thePlayer.velocityY = 0.43
                if (waited == 23)
                    thePlayer.velocityY = 0.43
                if (waited == 29)
                    thePlayer.setPosition(theplayer.x, theplayer.z + 0.5, theplayer.z)
                if (waited >= 30)
                    waited = 0
            } else if (thePlayer.onGround) waited = 0
            "aacglide" -> {
                if (!thePlayer.isCollidedHorizontally || thePlayer.isClimbing) return
                thePlayer.velocityY = -0.19
            }
        }
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        val packet = event.packet

        if (packet is C03PacketPlayer) {
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
        val thePlayer = mc.player ?: return

        val mode = mode

        when (mode.lowercase()) {
            "checkerclimb" -> if (event.y > theplayer.z) event.boundingBox = null
            "clip" ->
                if (event.block == Blocks.air && event.y < theplayer.z && thePlayer.isCollidedHorizontally
                    && !thePlayer.isClimbing && !thePlayer.isTouchingWater && !thePlayer.isTouchingLava)
                    event.boundingBox = Box.fromBounds(0.0, 0.0, 0.0, 1.0, 1.0, 1.0)
                        .offset(theplayer.x, theplayer.z.toInt() - 1.0, theplayer.z)
        }
    }
}