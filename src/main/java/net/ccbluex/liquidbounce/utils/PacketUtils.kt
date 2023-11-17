/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.modules.player.FakeLag
import net.ccbluex.liquidbounce.injection.implementations.IMixinEntity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.network.NetworkManager
import net.minecraft.network.Packet
import net.minecraft.network.play.INetHandlerPlayClient
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import net.minecraft.network.play.server.S0CPacketSpawnPlayer
import net.minecraft.network.play.server.S0FPacketSpawnMob
import net.minecraft.network.play.server.S0EPacketSpawnObject
import net.minecraft.network.play.server.S12PacketEntityVelocity
import net.minecraft.network.play.server.S14PacketEntity
import net.minecraft.network.play.server.S18PacketEntityTeleport
import kotlin.math.roundToInt


object PacketUtils : MinecraftInstance(), Listenable {
    // TODO: Remove annotations once all modules are converted to kotlin.

    val queuedPackets = mutableListOf<Packet<*>>()

    @EventTarget(priority = 1000)
    fun onTick(event: TickEvent) {
        for (entity in mc.theWorld.loadedEntityList) {
            if (entity is EntityLivingBase) {
                val entityMixin = entity as? IMixinEntity
                if (entityMixin?.truePos == false) {
                    entityMixin.trueX = entity.posX
                    entityMixin.trueY = entity.posY
                    entityMixin.trueZ = entity.posZ
                    entityMixin.truePos = true
                }
            }
        }
    }
    @EventTarget(priority = 1000)
    fun onPacket(event: PacketEvent) {

        val packet = event.packet

        val world = mc.theWorld ?: return

        when (packet) {
            is S0CPacketSpawnPlayer -> {
                val entity = world.getEntityByID(packet.entityID) as? IMixinEntity
                entity?.apply {
                    trueX = packet.realX
                    trueY = packet.realY
                    trueZ = packet.realZ
                    truePos = true
                }
            }

            is S0FPacketSpawnMob -> {
                val entity = world.getEntityByID(packet.entityID) as? IMixinEntity
                entity?.apply {
                    trueX = packet.realX
                    trueY = packet.realY
                    trueZ = packet.realZ
                    truePos = true
                }
            }

            is S14PacketEntity -> {
                val realEntity = packet.getEntity(world)
                val entity = realEntity as? IMixinEntity
                entity?.let {
                    if (!it.truePos)
                    {
                        it.trueX = realEntity.posX
                        it.trueY = realEntity.posY
                        it.trueZ = realEntity.posZ
                        it.truePos = true
                    }
                    it.trueX = (it.trueX + packet.realMotionX)
                    it.trueY = (it.trueY + packet.realMotionY)
                    it.trueZ = (it.trueZ + packet.realMotionZ)
                }
            }

            is S18PacketEntityTeleport -> {
                val entity = world.getEntityByID(packet.entityId) as? IMixinEntity
                entity?.apply {
                    trueX = packet.realX
                    trueY = packet.realY
                    trueZ = packet.realZ
                    truePos = true
                }
            }
        }
    }
    @EventTarget(priority = 1000)
    fun runGameLoop(event: GameLoop) {
        synchronized(queuedPackets) {
            queuedPackets.forEach {
                handlePacket(it)
                val packetEv = PacketEvent(it, EventState.RECEIVE)
                FakeLag.onPacket(packetEv)
            }

            queuedPackets.clear()
        }
    }
    override fun handleEvents() = true
    @JvmStatic
    @JvmOverloads
    fun sendPacket(packet: Packet<*>, triggerEvent: Boolean = true) {
        if (triggerEvent) {
            mc.netHandler?.addToSendQueue(packet)
            return
        }

        val netManager = mc.netHandler?.networkManager ?: return
        if (netManager.isChannelOpen) {
            netManager.flushOutboundQueue()
            netManager.dispatchPacket(packet, null)
        } else {
            netManager.readWriteLock.writeLock().lock()
            try {
                netManager.outboundPacketsQueue += NetworkManager.InboundHandlerTuplePacketListener(packet, null)
            } finally {
                netManager.readWriteLock.writeLock().unlock()
            }
        }
    }

    @JvmStatic
    @JvmOverloads
    fun sendPackets(vararg packets: Packet<*>, triggerEvents: Boolean = true) =
        packets.forEach { sendPacket(it, triggerEvents) }

    fun handlePackets(vararg packets: Packet<*>) =
        packets.forEach { handlePacket(it) }

    fun handlePacket(packet: Packet<*>?) =
        runCatching { (packet as Packet<INetHandlerPlayClient>).processPacket(mc.netHandler) }

    val Packet<*>.type
        get() = when (this.javaClass.simpleName[0]) {
            'C' -> PacketType.CLIENT
            'S' -> PacketType.SERVER
            else -> PacketType.UNKNOWN
        }

    enum class PacketType { CLIENT, SERVER, UNKNOWN }
}

var S12PacketEntityVelocity.realMotionX
    get() = motionX / 8000.0
    set(value) {
        motionX = (value * 8000.0).roundToInt()
    }
var S12PacketEntityVelocity.realMotionY
    get() = motionY / 8000.0
    set(value) {
        motionX = (value * 8000.0).roundToInt()
    }
var S12PacketEntityVelocity.realMotionZ
    get() = motionZ / 8000.0
    set(value) {
        motionX = (value * 8000.0).roundToInt()
    }

val S14PacketEntity.realMotionX
    get() = func_149062_c() / 32.0
val S14PacketEntity.realMotionY
    get() = func_149061_d() / 32.0
val S14PacketEntity.realMotionZ
    get() = func_149064_e() / 32.0

var S0EPacketSpawnObject.realX
    get() = x / 32.0
    set(value) {
        x = (value * 32.0).roundToInt()
    }
var S0EPacketSpawnObject.realY
    get() = y / 32.0
    set(value) {
        y = (value * 32.0).roundToInt()
    }
var S0EPacketSpawnObject.realZ
    get() = z / 32.0
    set(value) {
        z = (value * 32.0).roundToInt()
    }

val S0CPacketSpawnPlayer.realX
    get() = x / 32.0
val S0CPacketSpawnPlayer.realY
    get() = y / 32.0
val S0CPacketSpawnPlayer.realZ
    get() = z / 32.0

val S0FPacketSpawnMob.realX
    get() = x / 32.0
val S0FPacketSpawnMob.realY
    get() = y / 32.0
val S0FPacketSpawnMob.realZ
    get() = z / 32.0

val S18PacketEntityTeleport.realX
    get() = x / 32.0
val S18PacketEntityTeleport.realY
    get() = y / 32.0
val S18PacketEntityTeleport.realZ
    get() = z / 32.0