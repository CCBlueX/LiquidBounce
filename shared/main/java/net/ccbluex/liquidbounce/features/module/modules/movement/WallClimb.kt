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
import net.ccbluex.liquidbounce.utils.extensions.moveDirectionRadians
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.ListValue

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

        when (modeValue.get().toLowerCase())
        {
            "clip" ->
            {
                if (thePlayer.motionY < 0) glitch = true

                if (thePlayer.isCollidedHorizontally)
                {
                    when (clipMode.get().toLowerCase())
                    {
                        "jump" -> if (onGround) thePlayer.jump()

                        "fast" -> if (onGround) thePlayer.motionY = 0.42
                        else if (thePlayer.motionY < 0) thePlayer.motionY = -0.3
                    }
                }
            }

            "checkerclimb" ->
            {
                val isInsideBlock = theWorld.collideBlockIntersects(thePlayer.entityBoundingBox) { !classProvider.isBlockAir(it) }
                val motion = checkerClimbMotionValue.get()

                if (isInsideBlock && motion != 0f) thePlayer.motionY = motion.toDouble()
            }

            "aac3.3.12" -> if (thePlayer.isCollidedHorizontally && !thePlayer.isOnLadder)
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

            "aac3.3.8-glide" ->
            {
                if (!thePlayer.isCollidedHorizontally || thePlayer.isOnLadder) return

                thePlayer.motionY = -0.19
            }
        }
    }

    @EventTarget
    fun onPacket(event: PacketEvent)
    {
        if (classProvider.isCPacketPlayer(event.packet))
        {
            val packetPlayer = event.packet.asCPacketPlayer()

            if (glitch)
            {
                val thePlayer = mc.thePlayer ?: return

                val func = functions

                val dir = thePlayer.moveDirectionRadians
                packetPlayer.x = packetPlayer.x - func.sin(dir) * 0.00000001
                packetPlayer.z = packetPlayer.z + func.cos(dir) * 0.00000001

                glitch = false
            }
        }
    }

    @EventTarget
    fun onBlockBB(event: BlockBBEvent)
    {
        val thePlayer = mc.thePlayer ?: return

        val mode = modeValue.get()

        when (mode.toLowerCase())
        {
            "checkerclimb" -> if (event.y > thePlayer.posY) event.boundingBox = null

            "clip" ->
            {
                val provider = classProvider

                if (mc.thePlayer != null && provider.isBlockAir(event.block) && event.y < thePlayer.posY && thePlayer.isCollidedHorizontally && !thePlayer.isOnLadder && !thePlayer.isInWater && !thePlayer.isInLava) event.boundingBox = provider.createAxisAlignedBB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0).offset(thePlayer.posX, thePlayer.posY.toInt() - 1.0, thePlayer.posZ)
            }
        }
    }

    override val tag: String
        get() = modeValue.get()
}
