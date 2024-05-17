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
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.util.AxisAlignedBB
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
        val thePlayer = mc.thePlayer ?: return

        if (!thePlayer.isCollidedHorizontally || thePlayer.isOnLadder || thePlayer.isInWater || thePlayer.isInLava)
            return

        if (mode == "Simple") {
            event.y = 0.2
            thePlayer.motionY = 0.0
        }
    }

    @EventTarget
    fun onUpdate(event: MotionEvent) {
        val thePlayer = mc.thePlayer

        if (event.eventState != EventState.POST || thePlayer == null)
            return


        when (mode.lowercase()) {
            "clip" -> {
                if (thePlayer.motionY < 0)
                    glitch = true
                if (thePlayer.isCollidedHorizontally) {
                    when (clipMode.lowercase()) {
                        "jump" -> if (thePlayer.onGround)
                            thePlayer.tryJump()
                        "fast" -> if (thePlayer.onGround)
                            thePlayer.motionY = 0.42
                        else if (thePlayer.motionY < 0)
                            thePlayer.motionY = -0.3
                    }
                }
            }
            "checkerclimb" -> {
                val isInsideBlock = collideBlockIntersects(thePlayer.entityBoundingBox) {
                    it != Blocks.air
                }
                val motion = checkerClimbMotion

                if (isInsideBlock && motion != 0f)
                    thePlayer.motionY = motion.toDouble()
            }
            "aac3.3.12" -> if (thePlayer.isCollidedHorizontally && !thePlayer.isOnLadder) {
                waited++
                if (waited == 1)
                    thePlayer.motionY = 0.43
                if (waited == 12)
                    thePlayer.motionY = 0.43
                if (waited == 23)
                    thePlayer.motionY = 0.43
                if (waited == 29)
                    thePlayer.setPosition(thePlayer.posX, thePlayer.posY + 0.5, thePlayer.posZ)
                if (waited >= 30)
                    waited = 0
            } else if (thePlayer.onGround) waited = 0
            "aacglide" -> {
                if (!thePlayer.isCollidedHorizontally || thePlayer.isOnLadder) return
                thePlayer.motionY = -0.19
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
        val thePlayer = mc.thePlayer ?: return

        val mode = mode

        when (mode.lowercase()) {
            "checkerclimb" -> if (event.y > thePlayer.posY) event.boundingBox = null
            "clip" ->
                if (event.block == Blocks.air && event.y < thePlayer.posY && thePlayer.isCollidedHorizontally
                    && !thePlayer.isOnLadder && !thePlayer.isInWater && !thePlayer.isInLava)
                    event.boundingBox = AxisAlignedBB.fromBounds(0.0, 0.0, 0.0, 1.0, 1.0, 1.0)
                        .offset(thePlayer.posX, thePlayer.posY.toInt() - 1.0, thePlayer.posZ)
        }
    }
}