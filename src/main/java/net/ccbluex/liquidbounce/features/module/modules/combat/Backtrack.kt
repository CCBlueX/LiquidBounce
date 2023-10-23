/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.utils.PacketUtils.handlePacket
import net.ccbluex.liquidbounce.utils.extensions.getDistanceToBox
import net.ccbluex.liquidbounce.utils.extensions.hitBox
import net.ccbluex.liquidbounce.utils.render.ColorUtils.rainbow
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawBacktrackBox
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.minecraft.entity.Entity
import net.minecraft.network.Packet
import net.minecraft.network.play.INetHandlerPlayClient
import net.minecraft.network.play.server.*
import net.minecraft.util.AxisAlignedBB
import java.awt.Color
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object Backtrack : Module("Backtrack", ModuleCategory.COMBAT) {

    private val delay by object : IntegerValue("Delay", 80, 0..700) {
        override fun onChange(oldValue: Int, newValue: Int): Int {
            clearPackets()
            packetQueue.clear()

            return newValue
        }
    }

    private val maxDistanceValue: FloatValue = object : FloatValue("MaxDistance", 3.0f, 0.0f..3.0f) {
        override fun onChange(oldValue: Float, newValue: Float) = newValue.coerceAtLeast(minDistance)
    }
    private val maxDistance by maxDistanceValue
    private val minDistance by object : FloatValue("MinDistance", 2.0f, 0.0f..3.0f) {
        override fun onChange(oldValue: Float, newValue: Float) = newValue.coerceIn(minimum, maxDistance)
    }

    private val pingSpoof by BoolValue("PingSpoof", false)
    private val delayVelocity by BoolValue("DelayVelocity", false)
    private val delayExplosion by BoolValue("DelayExplosion", false)
    private val allPackets by BoolValue("AllPackets", false)

    // ESP
    private val rainbow by BoolValue("Rainbow", true)
    private val red by IntegerValue("R", 0, 0..255) { !rainbow }
    private val green by IntegerValue("G", 255, 0..255) { !rainbow }
    private val blue by IntegerValue("B", 0, 0..255) { !rainbow }

    private val packetQueue = ConcurrentHashMap<Packet<*>, Pair<Long, Long>>()
    private var target: Entity? = null
    private var realX = 0.0
    private var realY = 0.0
    private var realZ = 0.0

    @EventTarget
    fun onPacket(event: PacketEvent) {
        val packet = event.packet
        
        if (mc.thePlayer == null) {
            return
        }

        if (!shouldBacktrack()) {
            return
        }

        when (packet) {
            is S32PacketConfirmTransaction -> {
                if (pingSpoof) {
                    event.cancelEvent()
                    packetQueue[packet] = System.currentTimeMillis() + delay to System.nanoTime()
                }
                return
            }

            is S12PacketEntityVelocity -> {
                if (delayVelocity) {
                    event.cancelEvent()
                    packetQueue[packet] = System.currentTimeMillis() + delay to System.nanoTime()
                }
                return
            }

            is S27PacketExplosion -> {
                if (delayExplosion) {
                    event.cancelEvent()
                    packetQueue[packet] = System.currentTimeMillis() + delay to System.nanoTime()
                }
                return
            }

            is S14PacketEntity -> {
                if (packet.getEntity(mc.theWorld) == target) {
                    realX += packet.func_149062_c().toDouble()
                    realY += packet.func_149061_d().toDouble()
                    realZ += packet.func_149064_e().toDouble()
                }
                event.cancelEvent()
                packetQueue[packet] = System.currentTimeMillis() + delay to System.nanoTime()
                return
            }

            is S19PacketEntityStatus -> {
                event.cancelEvent()
                packetQueue[packet] = System.currentTimeMillis() + delay to System.nanoTime()
                return
            }
        }

        if (event.eventType == EventState.RECEIVE && allPackets && packet !is S29PacketSoundEffect && packet !is S0CPacketSpawnPlayer) {
            event.cancelEvent()
            packetQueue[packet] = System.currentTimeMillis() + delay to System.nanoTime()
        }
    }

    @EventTarget
    fun onTick(event: TickEvent) {
        if (!shouldBacktrack()) {
            clearPackets()
        } else handlePackets()
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

        target?.let {
            val x = it.lastTickPosX + (it.posX - it.lastTickPosX) * timer.renderPartialTicks - renderManager.renderPosX
            val y = it.lastTickPosY + (it.posY - it.lastTickPosY) * timer.renderPartialTicks - renderManager.renderPosY
            val z = it.lastTickPosZ + (it.posZ - it.lastTickPosZ) * timer.renderPartialTicks - renderManager.renderPosZ
            val axisAlignedBB = it.entityBoundingBox.offset(-it.posX, -it.posY, -it.posZ).offset(x, y, z)

            drawBacktrackBox(
                AxisAlignedBB.fromBounds(
                    axisAlignedBB.minX,
                    axisAlignedBB.minY,
                    axisAlignedBB.minZ,
                    axisAlignedBB.maxX,
                    axisAlignedBB.maxY,
                    axisAlignedBB.maxZ
                ).offset(realX / 32.0, realY / 32.0, realZ / 32.0), color
            )
        }
    }

    @EventTarget
    fun onWorld(event: WorldEvent) {
        clearPackets(false)
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
        val filtered = packetQueue.filter { 
            it.value.first <= System.currentTimeMillis()
        }.entries.sortedBy { it.value.second }.map { it.key }

        for (packet in filtered) {
            handlePacket(packet)
            if (packet is S14PacketEntity) {
                realX -= timedPacket.packet.func_149062_c().toDouble()
                realY -= timedPacket.packet.func_149061_d().toDouble()
                realZ -= timedPacket.packet.func_149064_e().toDouble()
            }
            packetQueue.remove(packet)
        }
    }

    private fun clearPackets(handlePackets: Boolean = true) {
        target = null
        if (handlePackets) {
            val filtered = packetQueue.entries.sortedBy { it.value.second }.map { it.key }
    
            for (packet in filtered) {
                handlePacket(packet)
                packetQueue.remove(packet)
            }
        }
        else packetQueue.clear()
        realX = 0.0
        realY = 0.0
        realZ = 0.0
    }

    val color
        get() = if (rainbow) rainbow() else Color(red, green, blue)

    private fun shouldBacktrack(): Boolean {
        return (target != null) && (!target!!.isDead) && (mc.thePlayer.getDistanceToBox(target!!.hitBox) in minDistance..maxDistance) && (mc.thePlayer.ticksExisted > 20)
    }
}

