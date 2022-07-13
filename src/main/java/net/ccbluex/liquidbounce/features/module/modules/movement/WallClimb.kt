/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.extensions.collideBlockIntersects
import net.ccbluex.liquidbounce.utils.extensions.cos
import net.ccbluex.liquidbounce.utils.extensions.moveDirectionRadians
import net.ccbluex.liquidbounce.utils.extensions.sin
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.block.BlockAir
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.client.multiplayer.WorldClient
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.util.AxisAlignedBB

@ModuleInfo(name = "WallClimb", description = "Allows you to climb up walls like a spider. (a.k.a. Spider)", category = ModuleCategory.MOVEMENT)
class WallClimb : Module()
{
    private val modeValue = ListValue("Mode", arrayOf("Simple", "CheckerClimb", "Clip", "AAC3.3.8-Glide", "AAC3.3.12"), "Simple")
    private val clipMode = ListValue("ClipMode", arrayOf("Jump", "Fast"), "Fast")

    private val checkerClimbMotionValue = FloatValue("CheckerClimbMotion", 0f, 0f, 1f)
    private val simpleSpeedValue = FloatValue("Simple-Speed", 0.2F, 0.01F, 1F)

    private var glitch = false
    private var aac3_3_12_steps = 0

    @EventTarget
    fun onMove(event: MoveEvent)
    {
        val thePlayer = mc.thePlayer ?: return

        if (!thePlayer.isCollidedHorizontally || thePlayer.isOnLadder || thePlayer.isInWater || thePlayer.isInLava) return

        if (modeValue.get().equals("Simple", ignoreCase = true))
        {
            event.y = simpleSpeedValue.get().toDouble()
            thePlayer.motionY = 0.0
        }
    }

    @EventTarget
    fun onUpdate(event: MotionEvent)
    {
        val theWorld = mc.theWorld ?: return
        val thePlayer = mc.thePlayer ?: return

        if (event.eventState != EventState.POST) return

        val onGround = thePlayer.onGround

        when (modeValue.get().lowercase())
        {
            "clip" -> clipClimb(thePlayer, onGround)
            "checkerclimb" -> checkerClimb(theWorld, thePlayer)
            "aac3.3.12" -> aac3_3_12Climb(thePlayer, onGround)
            "aac3.3.8-glide" -> aac3_3_8Glide(thePlayer)
        }
    }

    private fun clipClimb(thePlayer: EntityPlayerSP, onGround: Boolean)
    {
        if (thePlayer.motionY < 0) glitch = true

        if (thePlayer.isCollidedHorizontally)
        {
            when (clipMode.get().lowercase())
            {
                "jump" -> if (onGround) thePlayer.jump()

                "fast" -> if (onGround) thePlayer.motionY = 0.42
                else if (thePlayer.motionY < 0) thePlayer.motionY = -0.3
            }
        }
    }

    private fun checkerClimb(theWorld: WorldClient, thePlayer: EntityPlayerSP)
    {
        val isInsideBlock = theWorld.collideBlockIntersects(thePlayer.entityBoundingBox) { it !is BlockAir }
        val motion = checkerClimbMotionValue.get()

        if (isInsideBlock && motion != 0f) thePlayer.motionY = motion.toDouble()
    }

    private fun aac3_3_12Climb(thePlayer: EntityPlayerSP, onGround: Boolean)
    {
        if (thePlayer.isCollidedHorizontally && !thePlayer.isOnLadder)
        {
            aac3_3_12_steps++
            if (aac3_3_12_steps < 30) when (aac3_3_12_steps)
            {
                1 -> thePlayer.motionY = 0.43
                12 -> thePlayer.motionY = 0.43
                23 -> thePlayer.motionY = 0.43
                29 -> thePlayer.setPosition(thePlayer.posX, thePlayer.posY + 0.5, thePlayer.posZ)
            }
            else aac3_3_12_steps = 0
        }
        else if (onGround) aac3_3_12_steps = 0
    }

    private fun aac3_3_8Glide(thePlayer: EntityPlayerSP)
    {
        if (!thePlayer.isCollidedHorizontally || thePlayer.isOnLadder) return

        thePlayer.motionY = -0.19
    }

    @EventTarget
    fun onPacket(event: PacketEvent)
    {
        if (event.packet is C03PacketPlayer && glitch)
        {
            val thePlayer = mc.thePlayer ?: return

            val dir = thePlayer.moveDirectionRadians
            event.packet.x = event.packet.x - dir.sin * 0.00000001
            event.packet.z = event.packet.z + dir.cos * 0.00000001

            glitch = false
        }
    }

    @EventTarget
    fun onBlockBB(event: BlockBBEvent)
    {
        val thePlayer = mc.thePlayer ?: return

        val mode = modeValue.get()

        when (mode.lowercase())
        {
            "checkerclimb" -> if (event.y > thePlayer.posY) event.boundingBox = null

            "clip" ->
            {
                if (mc.thePlayer != null && event.block is BlockAir && event.y < thePlayer.posY && thePlayer.isCollidedHorizontally && !thePlayer.isOnLadder && !thePlayer.isInWater && !thePlayer.isInLava) event.boundingBox = AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0).offset(thePlayer.posX, thePlayer.posY.toInt() - 1.0, thePlayer.posZ)
            }
        }
    }

    override val tag: String
        get() = modeValue.get()
}
