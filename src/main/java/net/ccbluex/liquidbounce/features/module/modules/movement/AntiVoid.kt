/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.modules.player.Blink
import net.ccbluex.liquidbounce.features.module.modules.world.scaffolds.Scaffold
import net.ccbluex.liquidbounce.features.module.modules.world.scaffolds.Tower
import net.ccbluex.liquidbounce.utils.BlinkUtils
import net.ccbluex.liquidbounce.utils.MovementUtils.strafe
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.SimulatedPlayer
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getBlock
import net.ccbluex.liquidbounce.utils.extensions.component1
import net.ccbluex.liquidbounce.utils.extensions.component2
import net.ccbluex.liquidbounce.utils.extensions.component3
import net.ccbluex.liquidbounce.utils.misc.FallingPlayer
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawFilledBox
import net.ccbluex.liquidbounce.utils.render.RenderUtils.glColor
import net.ccbluex.liquidbounce.utils.render.RenderUtils.renderNameTag
import net.ccbluex.liquidbounce.utils.timing.WaitTickUtils
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.block.BlockAir
import com.mojang.blaze3d.platform.GlStateManager.resetColor
import net.minecraft.item.BlockItem
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionOnly
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket
import net.minecraft.util.Box
import net.minecraft.util.math.BlockPos
import org.lwjgl.opengl.GL11.*
import java.awt.Color
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max

object AntiVoid : Module("AntiVoid", Category.MOVEMENT, hideModule = false) {

    private val mode by ListValue("Mode",
        arrayOf("Blink", "TeleportBack", "FlyFlag", "OnGroundSpoof", "MotionTeleport-Flag", "GhostBlock"),
        "FlyFlag"
    )
    private val maxFallDistance by IntegerValue("MaxFallDistance", 10, 2..255)
    private val maxDistanceWithoutGround by FloatValue("MaxDistanceToSetback", 2.5f, 1f..30f) { mode != "Blink" }
    private val blinkDelay by IntegerValue("BlinkDelay", 10, 1..20) { mode == "Blink" }
    private val onScaffold by BoolValue("OnScaffold", false) { mode == "Blink" }
    private val ticksToDelay by IntegerValue("TicksDelay", 5, 1..20) { mode == "Blink" && !onScaffold }
    private val indicator by BoolValue("Indicator", true, subjective = true)

    private var detectedLocation: BlockPos? = null
    private var lastFound = 0F
    private var prevX = 0.0
    private var prevY = 0.0
    private var prevZ = 0.0
    private var shouldSimulateBlock = false
    private var shouldBlink = false
    private var pauseTicks = 0

    override fun onDisable() {
        prevX = 0.0
        prevY = 0.0
        prevZ = 0.0
        pauseTicks = 0

        shouldSimulateBlock = false

        shouldBlink = false
        BlinkUtils.unblink()
    }

    @EventTarget
    fun onUpdate(e: UpdateEvent) {
        detectedLocation = null

        val thePlayer = mc.player ?: return

        if (thePlayer.onGround && getBlock(BlockPos(thePlayer).down()) !is BlockAir) {
            prevX = thePlayer.prevPosX
            prevY = thePlayer.prevY
            prevZ = thePlayer.prevZ
            shouldSimulateBlock = false
        }

        if (!thePlayer.onGround && !thePlayer.isClimbing && !thePlayer.isTouchingWater) {
            val fallingPlayer = FallingPlayer(thePlayer)

            detectedLocation = fallingPlayer.findCollision(60)?.pos

            if (detectedLocation != null && abs(theplayer.z - detectedLocation!!.y) +
                thePlayer.fallDistance <= maxFallDistance) {
                lastFound = thePlayer.fallDistance
            }

            if (thePlayer.fallDistance - lastFound > maxDistanceWithoutGround) {
                when (mode.lowercase()) {
                    "teleportback" -> {
                        thePlayer.setPositionAndUpdate(prevX, prevY, prevZ)
                        thePlayer.fallDistance = 0F
                        thePlayer.velocityY = 0.0
                    }

                    "flyflag" -> {
                        thePlayer.velocityY += 0.1
                        thePlayer.fallDistance = 0F
                    }

                    "ongroundspoof" -> sendPacket(PlayerMoveC2SPacket(true))

                    "motionteleport-flag" -> {
                        thePlayer.setPositionAndUpdate(theplayer.x, theplayer.z + 1f, theplayer.z)
                        sendPacket(PositionOnly(theplayer.x, theplayer.z, theplayer.z, true))
                        thePlayer.velocityY = 0.1

                        strafe()
                        thePlayer.fallDistance = 0f
                    }

                    "ghostblock" -> shouldSimulateBlock = true
                }
            }
        }

        if (mode == "Blink") {
            val simPlayer = SimulatedPlayer.fromClientPlayer(thePlayer.movementInput)

            repeat(20) {
                simPlayer.tick()
            }

            if (simPlayer.isClimbing() || simPlayer.inWater || simPlayer.isTouchingLava() || simPlayer.isInWeb() || simPlayer.isSneaking()) {
                if (BlinkUtils.isBlinking) BlinkUtils.unblink()
                return
            }

            if (thePlayer.fallDistance < 1.5f && !simPlayer.onGround && simPlayer.fallDistance >= maxFallDistance) {
                shouldBlink = true
            } else if (BlinkUtils.isBlinking) {
                WaitTickUtils.scheduleTicks(blinkDelay) {
                    shouldBlink = false
                    BlinkUtils.cancel()
                }
            }
        }
    }

    @EventTarget
    fun onBlockBB(event: BlockBBEvent) {
        if (mode == "GhostBlock" && shouldSimulateBlock) {
            if (event.y < mc.player.z.toInt()) {
                event.boundingBox = Box(event.x.toDouble(),
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
        val player = mc.player ?: return
        val packet = event.packet

        // Stop considering non colliding blocks as collidable ones on setback.
        if (packet is PlayerPositionLookS2CPacket) {
            shouldSimulateBlock = false
        }

        if (!onScaffold && mode == "Blink" && pauseTicks > 0) {
            pauseTicks--
            return
        }

        if (!onScaffold && mode == "Blink") {
            // Check for block placement
            if (packet is PlayerInteractBlockC2SPacket) {
                if (packet.stack?.item is BlockItem) {

                    if (BlinkUtils.isBlinking && player.fallDistance < 1.5f) BlinkUtils.unblink()
                    if (pauseTicks < ticksToDelay) pauseTicks = ticksToDelay
                }
            }

            // Check for scaffold
            if ((Scaffold.handleEvents() || Tower.handleEvents()) &&
                (Scaffold.placeRotation != null || Tower.placeInfo != null)) {

                if (BlinkUtils.isBlinking && player.fallDistance < 1.5f) BlinkUtils.unblink()
                if (pauseTicks < ticksToDelay) pauseTicks = ticksToDelay
            }
        }

        if (mode != "Blink" || !shouldBlink) return

        if (player.isDead || player.ticksAlive < 20) {
            BlinkUtils.unblink()
            return
        }

        if (Blink.blinkingSend() || Blink.blinkingReceive()) {
            BlinkUtils.unblink()
            return
        }

        BlinkUtils.blink(packet, event, sent = true, receive = false)
    }

    @EventTarget
    fun onRender3D(event: Render3DEvent) {
        val thePlayer = mc.player ?: return

        if (detectedLocation == null || !indicator ||
            thePlayer.fallDistance + (theplayer.z - (detectedLocation!!.y + 1)) < 3)
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
            Box.fromBounds(
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

        val fallDist = floor(thePlayer.fallDistance + (theplayer.z - (y + 0.5))).toInt()

        renderNameTag("${fallDist}m (~${max(0, fallDist - 3)} damage)", x + 0.5, y + 1.7, z + 0.5)

        resetColor()
    }
}