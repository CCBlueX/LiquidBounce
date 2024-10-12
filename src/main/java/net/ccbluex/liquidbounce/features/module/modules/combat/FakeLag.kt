/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.player.Blink
import net.ccbluex.liquidbounce.features.module.modules.world.scaffolds.Scaffold
import net.ccbluex.liquidbounce.injection.implementations.IMixinEntity
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.extensions.*
import net.ccbluex.liquidbounce.utils.render.ColorUtils.rainbow
import net.ccbluex.liquidbounce.utils.render.RenderUtils.glColor
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.network.Packet
import net.minecraft.network.handshake.client.C00Handshake
import net.minecraft.network.play.client.*
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import net.minecraft.network.play.server.S12PacketEntityVelocity
import net.minecraft.network.play.server.S27PacketExplosion
import net.minecraft.network.status.client.C00PacketServerQuery
import net.minecraft.network.status.client.C01PacketPing
import net.minecraft.network.status.server.S01PacketPong
import net.minecraft.util.Vec3
import org.lwjgl.opengl.GL11.*
import java.awt.Color

object FakeLag : Module("FakeLag", Category.COMBAT, gameDetecting = false, hideModule = false) {

    private val delay by IntegerValue("Delay", 550, 0..1000)
    private val recoilTime by IntegerValue("RecoilTime", 750, 0..2000)
    private val distanceToPlayers by FloatValue("AllowedDistanceToPlayers", 3.5f, 0.0f..6.0f)

    private val blinkOnAction by BoolValue("BlinkOnAction", true)

    private val pauseOnNoMove by BoolValue("PauseOnNoMove", true)
    private val pauseOnChest by BoolValue("PauseOnChest", false)

    private val line by BoolValue("Line", true, subjective = true)
    private val rainbow by BoolValue("Rainbow", false, subjective = true) { line }
    private val red by IntegerValue("R",
        0,
        0..255,
        subjective = true
    ) { !rainbow && line }
    private val green by IntegerValue("G",
        255,
        0..255,
        subjective = true
    ) { !rainbow && line }
    private val blue by IntegerValue("B",
        0,
        0..255,
        subjective = true
    ) { !rainbow && line }

    private val packetQueue = LinkedHashMap<Packet<*>, Long>()
    private val positions = LinkedHashMap<Vec3, Long>()
    private val resetTimer = MSTimer()
    private var wasNearPlayer = false
    private var ignoreWholeTick = false

    override fun onDisable() {
        if (mc.thePlayer == null)
            return

        blink()
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        val player = mc.thePlayer ?: return
        val packet = event.packet

        if (!handleEvents())
            return

        if (player.isDead)
            return

        if (event.isCancelled)
            return

        if (distanceToPlayers > 0.0 && wasNearPlayer)
            return

        if (ignoreWholeTick)
            return

        if (pauseOnNoMove && !isMoving) {
            blink()
            return
        }

        // Flush on damaged received
        if (player.health < player.maxHealth) {
            if (player.hurtTime != 0) {
                blink()
                return
            }
        }

        // Flush on scaffold/tower usage
        if (Scaffold.handleEvents() && Scaffold.placeRotation != null) {
            blink()
            return
        }

        // Flush on attack/interact
        if (blinkOnAction && packet is C02PacketUseEntity) {
            blink()
            return
        }

        if (pauseOnChest && mc.currentScreen is GuiContainer) {
            blink()
            return
        }

        when (packet) {
            is C00Handshake, is C00PacketServerQuery, is C01PacketPing, is C01PacketChatMessage, is S01PacketPong -> return

            // Flush on window clicked (Inventory)
            is C0EPacketClickWindow, is C0DPacketCloseWindow -> {
                blink()
                return
            }

            // Flush on doing action/getting action
            is S08PacketPlayerPosLook, is C08PacketPlayerBlockPlacement, is C07PacketPlayerDigging, is C12PacketUpdateSign, is C19PacketResourcePackStatus -> {
                blink()
                return
            }

            // Flush on knockback
            is S12PacketEntityVelocity -> {
                if (player.entityId == packet.entityID) {
                    blink()
                    return
                }
            }

            is S27PacketExplosion -> {
                if (packet.field_149153_g != 0f || packet.field_149152_f != 0f || packet.field_149159_h != 0f) {
                    blink()
                    return
                }
            }
        }

        if (!resetTimer.hasTimePassed(recoilTime))
            return

        if (event.eventType == EventState.SEND) {
            event.cancelEvent()
            if (packet is C03PacketPlayer && packet.isMoving) {
                val packetPos = Vec3(packet.x, packet.y, packet.z)
                synchronized(positions) {
                    positions[packetPos] = System.currentTimeMillis()
                }
            }
            synchronized(packetQueue) {
                packetQueue[packet] = System.currentTimeMillis()
            }
        }
    }

    @EventTarget
    fun onWorld(event: WorldEvent) {
        // Clear packets on disconnect only
        if (event.worldClient == null)
            blink(false)
    }

    private fun getTruePositionEyes(player: EntityPlayer): Vec3 {
        val mixinPlayer = player as? IMixinEntity
        return Vec3(mixinPlayer!!.trueX, mixinPlayer.trueY + player.getEyeHeight().toDouble(), mixinPlayer.trueZ)
    }

    @EventTarget
    fun onGameLoop(event: GameLoopEvent) {
        val player = mc.thePlayer ?: return

        if (distanceToPlayers > 0) {
            val playerPos = player.positionVector
            val serverPos = positions.keys.firstOrNull() ?: playerPos

            val otherPlayers = mc.theWorld.playerEntities.filter { it != player }

            val (dx, dy, dz) = serverPos - playerPos
            val playerBox = player.hitBox.offset(dx, dy, dz)

            wasNearPlayer = false

            for (otherPlayer in otherPlayers) {
                val entityMixin = otherPlayer as? IMixinEntity
                if (entityMixin != null) {
                    val eyes = getTruePositionEyes(otherPlayer)
                    if (eyes.distanceTo(getNearestPointBB(eyes, playerBox)) <= distanceToPlayers.toDouble()) {
                        blink()
                        wasNearPlayer = true
                        return
                    }
                }
            }
        }

        if (Blink.blinkingSend() || player.isDead || player.isUsingItem) {
            blink()
            return
        }

        if (!resetTimer.hasTimePassed(recoilTime))
            return

        handlePackets()
        ignoreWholeTick = false
    }

    @EventTarget
    fun onRender3D(event: Render3DEvent) {
        if (!line) return

        val color = if (rainbow) rainbow() else Color(red, green, blue)

        if (Blink.blinkingSend())
            return

        synchronized(positions.keys) {
            glPushMatrix()
            glDisable(GL_TEXTURE_2D)
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
            glEnable(GL_LINE_SMOOTH)
            glEnable(GL_BLEND)
            glDisable(GL_DEPTH_TEST)
            mc.entityRenderer.disableLightmap()
            glBegin(GL_LINE_STRIP)
            glColor(color)

            val renderPosX = mc.renderManager.viewerPosX
            val renderPosY = mc.renderManager.viewerPosY
            val renderPosZ = mc.renderManager.viewerPosZ

            for (pos in positions.keys)
                glVertex3d(pos.xCoord - renderPosX, pos.yCoord - renderPosY, pos.zCoord - renderPosZ)

            glColor4d(1.0, 1.0, 1.0, 1.0)
            glEnd()
            glEnable(GL_DEPTH_TEST)
            glDisable(GL_LINE_SMOOTH)
            glDisable(GL_BLEND)
            glEnable(GL_TEXTURE_2D)
            glPopMatrix()
        }
    }

    override val tag
        get() = packetQueue.size.toString()

    private fun blink(handlePackets: Boolean = true) {
        synchronized(packetQueue) {
            if (handlePackets) {
                resetTimer.reset()

                packetQueue.forEach { (packet) -> sendPacket(packet, false) }
            }
        }

        packetQueue.clear()
        positions.clear()
        ignoreWholeTick = true
    }

    private fun handlePackets() {
        synchronized(packetQueue) {
            packetQueue.entries.removeAll { (packet, timestamp) ->
                if (timestamp <= System.currentTimeMillis() - delay) {
                    sendPacket(packet, false)
                    true
                } else false
            }
        }

        synchronized(positions) {
            positions.entries.removeAll { (_, timestamp) -> timestamp <= System.currentTimeMillis() - delay }
        }
    }

}
