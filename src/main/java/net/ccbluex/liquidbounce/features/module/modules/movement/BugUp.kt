/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.ClientUtils
import net.ccbluex.liquidbounce.utils.extensions.*
import net.ccbluex.liquidbounce.utils.misc.FallingPlayer
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.timer.TickTimer
import net.ccbluex.liquidbounce.value.*
import net.minecraft.block.BlockAir
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.BlockPos
import org.lwjgl.opengl.GL11
import java.awt.Color
import java.util.*
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max

@ModuleInfo(name = "BugUp", description = "Automatically setbacks you after falling a certain distance. (a.k.a. AntiFall)", category = ModuleCategory.MOVEMENT)
class BugUp : Module()
{
    private val modeValue = ListValue("Mode", arrayOf("TeleportBack", "FlyFlag", "OnGroundSpoof", "MotionTeleport-Flag", "Packet", "SpeedFlag"), "FlyFlag")
    private val maxFallDistance = IntegerValue("MaxFallDistance", 10, 2, 255)
    private val maxVoidFallDistance = IntegerValue("MaxVoidFallDistance", 3, 1, 255)
    private val maxDistanceWithoutGround = FloatValue("MaxDistanceToSetback", 2.5f, 1f, 30f)

    private val flagTryTicks = IntegerValue("FlagTryTicks", 10, 5, 20)

    private val flyFlagYMotionValue = object : FloatValue("YMotion", 1f, -10f, 10f)
    {
        override fun showCondition() = modeValue.get().equals("FlyFlag", ignoreCase = true)
    }

    private val motionTeleportFlagYTeleportValue = object : FloatValue("YTeleport", 1f, -10f, 10f)
    {
        override fun showCondition() = modeValue.get().equals("MotionTeleport-Flag", ignoreCase = true)
    }

    private val speedFlagMotionValue = object : FloatValue("XZMotion", 1F, -10F, 10F)
    {
        override fun showCondition() = modeValue.get().equals("SpeedFlag", ignoreCase = true)
    }

    private val packetGroup = object : ValueGroup("Packet")
    {
        override fun showCondition() = modeValue.get().equals("Packet", ignoreCase = true)
    }
    private val packetHValue = FloatValue("Horizontal", 1F, -10F, 10F, "Packet-H")
    private val packetHBobValue = BoolValue("Horizontal-Shake", true, "Packet-H-Bob")
    private val packetVValue = FloatValue("Vertical", 11F, -10F, 10F, "Packet-V")
    private val packetVBobValue = BoolValue("Vertical-Shake", false, "Packet-V-Bob")

    private val onlyCatchVoid = BoolValue("OnlyVoid", true)
    private val indicator = BoolValue("Indicator", true)

    private var detectedLocation: BlockPos? = null
    private var lastFound = 0F
    private var prevX = 0.0
    private var prevY = 0.0
    private var prevZ = 0.0

    private val flagTimer = TickTimer()
    private var tryingFlag = false
    private var alreadyTriedFlag = false
    private var packetHBob = false
    private var packetVBob = false

    init
    {
        packetGroup.addAll(packetHValue, packetHBobValue, packetVValue, packetVBobValue)
    }

    override fun onDisable()
    {
        prevX = 0.0
        prevY = 0.0
        prevZ = 0.0
    }

    @EventTarget
    fun onUpdate(@Suppress("UNUSED_PARAMETER") event: UpdateEvent)
    {
        detectedLocation = null

        val theWorld = mc.theWorld ?: return
        val thePlayer = mc.thePlayer ?: return

        val posX = thePlayer.posX
        val posY = thePlayer.posY
        val posZ = thePlayer.posZ

        if (thePlayer.onGround && theWorld.getBlock(BlockPos(posX, posY, posZ).down()) !is BlockAir)
        {
            prevX = thePlayer.prevPosX
            prevY = thePlayer.prevPosY
            prevZ = thePlayer.prevPosZ
        }

        val networkManager = mc.netHandler.networkManager

        if (!thePlayer.onGround && !thePlayer.isOnLadder && !thePlayer.isInWater)
        {
            val fallingPlayer = FallingPlayer(theWorld, thePlayer, posX, posY, posZ, thePlayer.motionX, thePlayer.motionY, thePlayer.motionZ, thePlayer.rotationYaw, thePlayer.moveStrafing, thePlayer.moveForward)

            val fallDistance = thePlayer.fallDistance

            val detectedLocation = fallingPlayer.findCollision(60)?.pos

            this.detectedLocation = detectedLocation

            if (detectedLocation != null && (onlyCatchVoid.get() || abs(posY - detectedLocation.y) + fallDistance <= maxFallDistance.get())) lastFound = fallDistance

            if (detectedLocation == null && fallDistance <= maxVoidFallDistance.get()) lastFound = fallDistance

            if (fallDistance - lastFound > maxDistanceWithoutGround.get())
            {
                when (modeValue.get().lowercase(Locale.getDefault()))
                {
                    "teleportback" ->
                    {
                        thePlayer.setPositionAndUpdate(prevX, prevY, prevZ)
                        thePlayer.fallDistance = 0F
                        thePlayer.motionY = 0.0
                    }

                    else -> if (!alreadyTriedFlag)
                    {
                        tryingFlag = true
                        alreadyTriedFlag = true
                    }
                }
            }
        }
        else alreadyTriedFlag = false

        if (tryingFlag)
        {
            if (!flagTimer.hasTimePassed(flagTryTicks.get()))
            {
                when (modeValue.get().lowercase(Locale.getDefault()))
                {
                    "flyflag" ->
                    {
                        thePlayer.motionY = flyFlagYMotionValue.get().toDouble()
                        thePlayer.fallDistance = 0F
                    }

                    "ongroundspoof" -> networkManager.sendPacketWithoutEvent(C03PacketPlayer(true))

                    "motionteleport-flag" ->
                    {
                        thePlayer.setPositionAndUpdate(posX, posY + motionTeleportFlagYTeleportValue.get(), posZ)
                        networkManager.sendPacketWithoutEvent(C04PacketPlayerPosition(posX, posY, posZ, true))
                        thePlayer.motionY = flyFlagYMotionValue.get().toDouble()

                        thePlayer.strafe()
                        thePlayer.fallDistance = 0f
                    }

                    "speedflag" -> thePlayer.boost(speedFlagMotionValue.get(), thePlayer.rotationYaw)

                    "packet" ->
                    {
                        val dir = thePlayer.rotationYaw.toRadians
                        val length = if (packetHBob) packetHValue.get() else -packetHValue.get()
                        val x = dir.sin * length
                        val z = dir.cos * length

                        networkManager.sendPacketWithoutEvent(C04PacketPlayerPosition(posX - x, posY + if (packetVBob) packetVValue.get() else -packetVValue.get(), posZ + z, true))

                        if (packetHBobValue.get()) packetHBob = !packetHBob
                        if (packetVBobValue.get()) packetVBob = !packetVBob
                    }
                }
            }
            else
            {
                tryingFlag = false
                flagTimer.reset()
            }
            flagTimer.update()
        }
        else flagTimer.reset()
    }

    @EventTarget
    fun onRender3D(@Suppress("UNUSED_PARAMETER") event: Render3DEvent)
    {
        val thePlayer = mc.thePlayer ?: return

        val detectedLocation = detectedLocation ?: return
        if (!indicator.get() || thePlayer.fallDistance + (thePlayer.posY - (detectedLocation.y + 1)) < 3) return

        val x = detectedLocation.x
        val y = detectedLocation.y
        val z = detectedLocation.z

        val renderManager = mc.renderManager
        val renderPosX = renderManager.renderPosX
        val renderPosY = renderManager.renderPosY
        val renderPosZ = renderManager.renderPosZ

        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        GL11.glEnable(GL11.GL_BLEND)
        GL11.glLineWidth(2f)
        GL11.glDisable(GL11.GL_TEXTURE_2D)
        GL11.glDisable(GL11.GL_DEPTH_TEST)
        GL11.glDepthMask(false)

        RenderUtils.glColor(Color(255, 0, 0, 90))

        RenderUtils.drawFilledBox(AxisAlignedBB(x - renderPosX, y + 1 - renderPosY, z - renderPosZ, x - renderPosX + 1.0, y + 1.2 - renderPosY, z - renderPosZ + 1.0))

        GL11.glEnable(GL11.GL_TEXTURE_2D)
        GL11.glEnable(GL11.GL_DEPTH_TEST)
        GL11.glDepthMask(true)
        GL11.glDisable(GL11.GL_BLEND)

        val fallDistance = floor(thePlayer.fallDistance + (thePlayer.posY - (y + 0.5))).toInt()

        RenderUtils.renderNameTag("${fallDistance}m (~${max(0, fallDistance - 3)} damage)", x + 0.5, y + 1.7, z + 0.5)

        GlStateManager.resetColor()
    }

    @EventTarget
    fun onPacket(event: PacketEvent)
    {
        val packet = event.packet
        if (packet is S08PacketPlayerPosLook)
        {
            val mode = modeValue.get()

            if (!mode.equals("TeleportBack", true) && tryingFlag)
            {
                // Automatically stop to try flag after teleported back.
                ClientUtils.displayChatMessage(mc.thePlayer, "\u00A78[\u00A7c\u00A7lBugUp\u00A78] \u00A7cTeleported.")
                tryingFlag = false
            }
        }
    }

    override val tag: String
        get() = modeValue.get()
}
