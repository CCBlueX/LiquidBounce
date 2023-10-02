/*
 * liquidbounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/liquidbounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.AttackEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.utils.extensions.getDistanceToEntityBox
import net.ccbluex.liquidbounce.utils.render.RenderUtils.glColor
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.BoolValue
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.network.Packet
import net.minecraft.network.play.INetHandlerPlayClient
import net.minecraft.network.play.server.S12PacketEntityVelocity
import net.minecraft.network.play.server.S27PacketExplosion
import net.minecraft.network.play.server.S32PacketConfirmTransaction
import net.minecraft.network.play.server.S00PacketKeepAlive
import net.minecraft.network.play.server.S14PacketEntity 
import net.minecraft.network.play.server.S18PacketEntityTeleport
import net.minecraft.network.play.server.S03PacketTimeUpdate
import org.lwjgl.opengl.GL11.*
import java.awt.Color
import java.util.*
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawBacktrackBox
import net.ccbluex.liquidbounce.utils.PacketUtils.handlePacket
import net.minecraft.util.AxisAlignedBB
import net.ccbluex.liquidbounce.utils.render.ColorUtils.rainbow
import net.ccbluex.liquidbounce.utils.ClientUtils.displayChatMessage

object Backtrack : Module("Backtrack", ModuleCategory.COMBAT) {

    private val maxDelay by IntegerValue("Delay", 50, 50..500)
    private val delayTimer = MSTimer()

    private val maxDistanceValue: FloatValue = object : FloatValue("Max-Distance",  2.9f, 2.5f..3.0f) {
        override fun onChange(oldValue: Float, newValue: Float) = newValue.coerceAtLeast(minDistance)
    }
    private val maxDistance by maxDistanceValue
    private val minDistance by object : FloatValue("Min-Distance", 1f, 2.5f..2.9f) {
        override fun onChange(oldValue: Float, newValue: Float) = newValue.coerceIn(minimum, maxDistance)
    }

    private val pingSpoof by BoolValue("Ping-Spoof", false)
    private val delayVelocity by BoolValue("Delay-Velocity", false)
    private val delayExplosion by BoolValue("Delay-Explosion", false)

    // ESP
    private val colorRainbow by BoolValue("Rainbow", true)
    private val colorRed by IntegerValue("R", 0, 0..255) { !colorRainbow }
    private val colorGreen by IntegerValue("G", 255, 0..255) { !colorRainbow }
    private val colorBlue by IntegerValue("B", 0, 0..255) { !colorRainbow }

    private val packets = LinkedList<TimedPacket>()
    private var target: Entity? = null
    private var realX = 0.0
    private var realY = 0.0
    private var realZ = 0.0
    private var ticks = 0


    @EventTarget
    fun onPacket(event: PacketEvent) {
        val packet = event.packet
        
        if (!shouldBacktrack()) {
            return
        }

        when (packet) {
            is S32PacketConfirmTransaction -> {
                if (pingSpoof) {
                    packets.add(TimedPacket(packet, System.currentTimeMillis()))
                    event.cancelEvent()
                } 
                return
            }
            is S12PacketEntityVelocity -> {
                if (delayVelocity) {
                    packets.add(TimedPacket(packet, System.currentTimeMillis()))
                    event.cancelEvent()
                }
                return
            }
            is S27PacketExplosion -> {
                if (delayExplosion) {
                    packets.add(TimedPacket(packet, System.currentTimeMillis()))
                    event.cancelEvent()
                }
                return
            }
            is S14PacketEntity -> {
                if (packet.getEntity(mc.theWorld) == target) {
                    realX += packet.func_149062_c().toDouble()
                    realY += packet.func_149061_d().toDouble()
                    realZ += packet.func_149064_e().toDouble()
                    packets.add(TimedPacket(packet, System.currentTimeMillis()))
                    event.cancelEvent()
                }
                return
            }
        }

        if (packet.javaClass.simpleName.startsWith("S", ignoreCase = true)) {
            packets.add(TimedPacket(packet as Packet<INetHandlerPlayClient>, System.currentTimeMillis()))
            event.cancelEvent()
        }
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        ticks++

        if (!shouldBacktrack()) {
            clearPackets()
        }
        else handlePackets()
    }

    @EventTarget
    fun onAttack(event: AttackEvent) {
        target = event.targetEntity
    }

    @EventTarget
    fun onRender3D(event: Render3DEvent) {
        if (!shouldBacktrack()) {
            return
        }

        val renderManager = mc.renderManager
        val timer = mc.timer

        val x = target!!.lastTickPosX + (target!!.posX - target!!.lastTickPosX) * timer.renderPartialTicks - renderManager.renderPosX
        val y = target!!.lastTickPosY + (target!!.posY - target!!.lastTickPosY) * timer.renderPartialTicks - renderManager.renderPosY
        val z = target!!.lastTickPosZ + (target!!.posZ - target!!.lastTickPosZ) * timer.renderPartialTicks - renderManager.renderPosZ
        val axisAlignedBB = target!!.entityBoundingBox
            .offset(-target!!.posX, -target!!.posY, -target!!.posZ)
            .offset(x, y, z)

        drawBacktrackBox(
            AxisAlignedBB.fromBounds(
                axisAlignedBB.minX,
                axisAlignedBB.minY,
                axisAlignedBB.minZ,
                axisAlignedBB.maxX,
                axisAlignedBB.maxY,
                axisAlignedBB.maxZ
            ).offset(realX/32.0, realY/32.0, realZ/32.0), getColor()
        )                
    }

    @EventTarget
    override fun onEnable() {
        target = null
        realX = 0.0
        realY = 0.0
        realZ = 0.0
    }

    @EventTarget
    override fun onDisable() {
        clearPackets()         
    }

    private fun handlePackets() {
        val packetsToRemove = mutableListOf<TimedPacket>()
        for (timedPacket in packets) {
            if (timedPacket.timestamp + maxDelay <= System.currentTimeMillis()) {
                handlePacket(timedPacket.packet)
                if (timedPacket.packet is S14PacketEntity) {
                    realX -= timedPacket.packet.func_149062_c().toDouble()
                    realY -= timedPacket.packet.func_149061_d().toDouble()
                    realZ -= timedPacket.packet.func_149064_e().toDouble()
                }
                packetsToRemove.add(timedPacket)
            }
        }
        packets.removeAll(packetsToRemove)         
    }

    private fun clearPackets() {
        target = null
        for (timedPacket in packets) {
            handlePacket(timedPacket.packet)
        }
        packets.clear()  
        realX = 0.0
        realY = 0.0
        realZ = 0.0
    }

    private fun getColor():Color{
        return if (colorRainbow) rainbow() else Color(colorRed, colorGreen, colorBlue)
    }

    private fun shouldBacktrack(): Boolean {
        return mc.theplayer.getdistancetobox(target.hitbox) in mindistance..maxdistance && target != null && mc.thePlayer.ticksExisted > 20
        
    }

    
}

data class TimedPacket(val packet: Packet<INetHandlerPlayClient>, val timestamp: Long)