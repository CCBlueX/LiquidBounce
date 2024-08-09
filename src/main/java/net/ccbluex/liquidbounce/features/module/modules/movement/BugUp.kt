/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.utils.MovementUtils.strafe
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getBlock
import net.ccbluex.liquidbounce.utils.extensions.component1
import net.ccbluex.liquidbounce.utils.extensions.component2
import net.ccbluex.liquidbounce.utils.extensions.component3
import net.ccbluex.liquidbounce.utils.misc.FallingPlayer
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawFilledBox
import net.ccbluex.liquidbounce.utils.render.RenderUtils.glColor
import net.ccbluex.liquidbounce.utils.render.RenderUtils.renderNameTag
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.block.BlockAir
import net.minecraft.client.renderer.GlStateManager.resetColor
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.BlockPos
import org.lwjgl.opengl.GL11.*
import java.awt.Color
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max

object BugUp : Module("BugUp", Category.MOVEMENT, hideModule = false) {

    private val mode by ListValue("Mode",
        arrayOf("TeleportBack", "FlyFlag", "OnGroundSpoof", "MotionTeleport-Flag", "GhostBlock"),
        "FlyFlag"
    )
    private val maxFallDistance by IntegerValue("MaxFallDistance", 10, 2..255)
    private val maxDistanceWithoutGround by FloatValue("MaxDistanceToSetback", 2.5f, 1f..30f)
    private val indicator by BoolValue("Indicator", true, subjective = true)

    private var detectedLocation: BlockPos? = null
    private var lastFound = 0F
    private var prevX = 0.0
    private var prevY = 0.0
    private var prevZ = 0.0
    private var shouldSimulateBlock = false

    override fun onDisable() {
        prevX = 0.0
        prevY = 0.0
        prevZ = 0.0

        shouldSimulateBlock = false
    }

    @EventTarget
    fun onUpdate(e: UpdateEvent) {
        detectedLocation = null

        val player = mc.thePlayer ?: return

        if (player.onGround && getBlock(BlockPos(player).down()) !is BlockAir) {
            prevX = player.prevPosX
            prevY = player.prevPosY
            prevZ = player.prevPosZ
            shouldSimulateBlock = false
        }

        if (!player.onGround && !player.isOnLadder && !player.isInWater) {
            val fallingPlayer = FallingPlayer(player)

            detectedLocation = fallingPlayer.findCollision(60)?.pos

            if (detectedLocation != null && abs(player.posY - detectedLocation!!.y) +
                player.fallDistance <= maxFallDistance) {
                lastFound = player.fallDistance
            }

            if (player.fallDistance - lastFound > maxDistanceWithoutGround) {
                val mode = mode

                when (mode.lowercase()) {
                    "teleportback" -> {
                        player.setPositionAndUpdate(prevX, prevY, prevZ)
                        player.fallDistance = 0F
                        player.motionY = 0.0
                    }

                    "flyflag" -> {
                        player.motionY += 0.1
                        player.fallDistance = 0F
                    }

                    "ongroundspoof" -> sendPacket(C03PacketPlayer(true))

                    "motionteleport-flag" -> {
                        player.setPositionAndUpdate(player.posX, player.posY + 1f, player.posZ)
                        sendPacket(C04PacketPlayerPosition(player.posX, player.posY, player.posZ, true))
                        player.motionY = 0.1

                        strafe()
                        player.fallDistance = 0f
                    }

                    "ghostblock" -> shouldSimulateBlock = true
                }
            }
        }
    }

    @EventTarget
    fun onBlockBB(event: BlockBBEvent) {
        if (mode == "GhostBlock" && shouldSimulateBlock) {
            if (event.y < mc.thePlayer.posY.toInt()) {
                event.boundingBox = AxisAlignedBB(event.x.toDouble(),
                    event.y.toDouble(),
                    event.z.toDouble(),
                    event.x + 1.0,
                    event.y + 1.0,
                    event.z + 1.0
                )
            }
        }
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        // Stop considering non colliding blocks as collidable ones on setback.
        if (event.packet is S08PacketPlayerPosLook) {
            shouldSimulateBlock = false
        }
    }

    @EventTarget
    fun onRender3D(event: Render3DEvent) {
        val player = mc.thePlayer ?: return

        if (detectedLocation == null || !indicator ||
            player.fallDistance + (player.posY - (detectedLocation!!.y + 1)) < 3)
            return

        val (x, y, z) = detectedLocation ?: return

        val renderManager = mc.renderManager

        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        glEnable(GL_BLEND)
        glLineWidth(2f)
        glDisable(GL_TEXTURE_2D)
        glDisable(GL_DEPTH_TEST)
        glDepthMask(false)

        glColor(Color(255, 0, 0, 90))
        drawFilledBox(
            AxisAlignedBB.fromBounds(
                x - renderManager.renderPosX,
                y + 1 - renderManager.renderPosY,
                z - renderManager.renderPosZ,
                x - renderManager.renderPosX + 1.0,
                y + 1.2 - renderManager.renderPosY,
                z - renderManager.renderPosZ + 1.0
            )
        )

        glEnable(GL_TEXTURE_2D)
        glEnable(GL_DEPTH_TEST)
        glDepthMask(true)
        glDisable(GL_BLEND)

        val fallDist = floor(player.fallDistance + (player.posY - (y + 0.5))).toInt()

        renderNameTag("${fallDist}m (~${max(0, fallDist - 3)} damage)", x + 0.5, y + 1.7, z + 0.5)

        resetColor()
    }
}