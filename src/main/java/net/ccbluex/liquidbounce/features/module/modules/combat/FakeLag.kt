/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.modules.player.Blink
import net.ccbluex.liquidbounce.features.module.modules.world.scaffolds.*
import net.ccbluex.liquidbounce.injection.implementations.IMixinEntity
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.Rotation
import net.ccbluex.liquidbounce.utils.RotationUtils
import net.ccbluex.liquidbounce.utils.extensions.*
import net.ccbluex.liquidbounce.utils.render.ColorUtils.rainbow
import net.ccbluex.liquidbounce.utils.render.RenderUtils.glColor
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.network.Packet
import net.minecraft.network.packet.c2s.handshake.HandshakeC2SPacket
import net.minecraft.network.packet.c2s.play.*
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket
import net.minecraft.network.packet.c2s.query.QueryRequestC2SPacket
import net.minecraft.network.packet.c2s.query.QueryPingC2SPacket
import net.minecraft.network.packet.s2c.query.QueryPongS2CPacket
import net.minecraft.util.math.Vec3d
import org.lwjgl.opengl.GL11.*
import java.awt.Color

object FakeLag : Module("FakeLag", Category.COMBAT, gameDetecting = false, hideModule = false) {

    private val delay by IntegerValue("Delay", 550, 0..1000)
    private val recoilTime by IntegerValue("RecoilTime", 750, 0..2000)
    private val distanceToPlayers by FloatValue("AllowedDistanceToPlayers", 3.5f, 0.0f..6.0f)

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
    private val positions = LinkedHashMap<Vec3d, Long>()
    private val resetTimer = MSTimer()
    private var wasNearPlayer = false
    private var ignoreWholeTick = false

    override fun onDisable() {
        if (mc.player == null)
            return

        blink()
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        val player = mc.player ?: return
        val packet = event.packet

        if (!handleEvents())
            return

        if (!player.isAlive)
            return

        if (event.isCancelled)
            return

        if (distanceToPlayers > 0.0 && wasNearPlayer)
            return

        if (ignoreWholeTick)
            return

        // Check if player got damaged
        if (player.health < player.maxHealth) {
            if (player.hurtTime != 0) {
                blink()
                return
            }
        }

        // Proper check to prevent FakeLag while using Scaffold
        if (Scaffold.handleEvents() && (Tower.placeInfo != null || Scaffold.placeRotation != null)) {
            blink()
            return
        }

        when (packet) {
            is HandshakeC2SPacket, is QueryRequestC2SPacket, is QueryPingC2SPacket, is ChatMessageC2SPacket, is QueryPongS2CPacket -> return

            // Flush on window clicked (Inventory)
            is ClickWindowC2SPacket, is GuiCloseC2SPacket -> {
                blink()
                return
            }

            // Flush on doing action, getting action
            is PlayerPositionLookS2CPacket, is PlayerInteractBlockC2SPacket, is PlayerActionC2SPacket, is UpdateSignC2SPacket, is PlayerInteractEntityC2SPacket, is ResourcePackStatusC2SPacket -> {
                blink()
                return
            }

            // Flush on knockback
            is EntityVelocityUpdateS2CPacket -> {
                if (player.entityId == packet.id) {
                    blink()
                    return
                }
            }

            is ExplosionS2CPacket -> {
                if (packet.playerVelocityY != 0f || packet.playerVelocityX != 0f || packet.playerVelocityZ != 0f) {
                    blink()
                    return
                }
            }

            /*
             * Temporarily disabled (It seems like it only detects when player is healing??)
             * And "packet.health < player.health" check doesn't really work.
             */

            // Flush on damage
//            is HealthUpdateS2CPacket -> {
//                if (packet.health < mc.player.health) {
//                    blink()
//                    return
//                }
//            }
        }

        if (!resetTimer.hasTimePassed(recoilTime))
            return

        if (event.eventType == EventState.SEND) {
            event.cancelEvent()
            if (packet is PlayerMoveC2SPacket && packet.isMoving) {
                val packetPos = Vec3d(packet.x, packet.y, packet.z)
                synchronized(positions) {
                    positions[packetPos] = System.currentTimeMillis()
                }
                if (packet.rotating) {
                    RotationUtils.serverRotation = Rotation(packet.yaw, packet.pitch)
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

    private fun getTruePositionEyes(player: PlayerEntity): Vec3d {
        val mixinPlayer = player as? IMixinEntity
        return Vec3d(mixinPlayer!!.trueX, mixinPlayer.trueY + player.getEyeHeight().toDouble(), mixinPlayer.trueZ)
    }

    @EventTarget
    fun onGameLoop(event: GameLoopEvent) {
        val player = mc.player ?: return

        if (distanceToPlayers > 0) {
            val playerPos = player.positionVector
            val serverPos = positions.keys.firstOrNull() ?: playerPos

            val otherPlayers = mc.world.playerEntities.filter { it != player }

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

        if (Blink.blinkingSend() || !player.isAlive || player.isUsingItem) {
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
            mc.entityRenderDispatcher.disableLightmap()
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
