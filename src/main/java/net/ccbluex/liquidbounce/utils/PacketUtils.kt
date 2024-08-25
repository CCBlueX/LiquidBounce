/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.modules.combat.FakeLag
import net.ccbluex.liquidbounce.features.module.modules.combat.Velocity
import net.ccbluex.liquidbounce.injection.implementations.IMixinEntity
import net.minecraft.entity.LivingEntity
import net.minecraft.network.NetworkManager
import net.minecraft.network.Packet
import net.minecraft.network.play.INetHandlerPlayClient
import net.minecraft.network.packet.s2c.play.*
import kotlin.math.roundToInt

object PacketUtils : MinecraftInstance(), Listenable {

    val queuedPackets = mutableListOf<Packet<*>>()

    @EventTarget(priority = 2)
    fun onTick(event: GameTickEvent) {
        for (entity in mc.world.entities) {
            if (entity is LivingEntity) {
                (entity as? IMixinEntity)?.apply {
                    if (!truePos) {
                        trueX = entity.posX
                        trueY = entity.posY
                        trueZ = entity.posZ
                        truePos = true
                    }
                }
            }
        }
    }

    @EventTarget(priority = 2)
    fun onPacket(event: PacketEvent) {
        val packet = event.packet
        val world = mc.world ?: return

        when (packet) {
            is PlayerSpawnS2CPacket
 ->
                (world.getEntityById(packet.entityID) as? IMixinEntity)?.apply {
                    trueX = packet.realX
                    trueY = packet.realY
                    trueZ = packet.realZ
                    truePos = true
                }

            is MobSpawnS2CPacket ->
                (world.getEntityById(packet.entityID) as? IMixinEntity)?.apply {
                    trueX = packet.realX
                    trueY = packet.realY
                    trueZ = packet.realZ
                    truePos = true
                }

            is EntityS2CPacket -> {
                val entity = packet.getEntity(world)
                val mixinEntity = entity as? IMixinEntity

                mixinEntity?.apply {
                    if (!truePos) {
                        trueX = entity.posX
                        trueY = entity.posY
                        trueZ = entity.posZ
                        truePos = true
                    }

                    trueX += packet.realMotionX
                    trueY += packet.realMotionY
                    trueZ += packet.realMotionZ
                }
            }

            is EntityPositionS2CPacket ->
                (world.getEntityById(packet.entityId) as? IMixinEntity)?.apply {
                    trueX = packet.realX
                    trueY = packet.realY
                    trueZ = packet.realZ
                    truePos = true
                }
        }
    }

    @EventTarget(priority = -5)
    fun onGameLoop(event: GameLoopEvent) {
        synchronized(queuedPackets) {
            queuedPackets.forEach {
                handlePacket(it)
                val packetEvent = PacketEvent(it, EventState.RECEIVE)
                FakeLag.onPacket(packetEvent)
                Velocity.onPacket(packetEvent)
            }

            queuedPackets.clear()
        }
    }

    @EventTarget(priority = -1)
    fun onDisconnect(event: WorldEvent) {
        if (event.worldClient == null) {
            synchronized(queuedPackets) {
                queuedPackets.clear()
            }
        }
    }

    override fun handleEvents() = true

    @JvmStatic
    fun sendPacket(packet: Packet<*>, triggerEvent: Boolean = true) {
        if (triggerEvent) {
            mc.netHandler?.addToSendQueue(packet)
            return
        }

        val netManager = mc.netHandler?.networkManager ?: return

        PPSCounter.registerType(PPSCounter.PacketType.SEND)
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
    fun sendPackets(vararg packets: Packet<*>, triggerEvents: Boolean = true) =
        packets.forEach { sendPacket(it, triggerEvents) }

    fun handlePackets(vararg packets: Packet<*>) =
        packets.forEach { handlePacket(it) }

    fun handlePacket(packet: Packet<*>?) {
        runCatching { (packet as Packet<INetHandlerPlayClient>).processPacket(mc.netHandler) }.onSuccess {
            PPSCounter.registerType(PPSCounter.PacketType.RECEIVED)
        }
    }

    val Packet<*>.type
        get() = when (this.javaClass.simpleName[0]) {
            'C' -> PacketType.CLIENT
            'S' -> PacketType.SERVER
            else -> PacketType.UNKNOWN
        }

    enum class PacketType { CLIENT, SERVER, UNKNOWN }
}

var EntityVelocityUpdateS2CPacket.realMotionX
    get() = velocityX / 8000.0
    set(value) {
        velocityX = (value * 8000.0).roundToInt()
    }
var EntityVelocityUpdateS2CPacket.realMotionY
    get() = velocityY / 8000.0
    set(value) {
        velocityX = (value * 8000.0).roundToInt()
    }
var EntityVelocityUpdateS2CPacket.realMotionZ
    get() = velocityZ / 8000.0
    set(value) {
        velocityX = (value * 8000.0).roundToInt()
    }

val EntityS2CPacket.realMotionX
    get() = func_149062_c() / 32.0
val EntityS2CPacket.realMotionY
    get() = func_149061_d() / 32.0
val EntityS2CPacket.realMotionZ
    get() = func_149064_e() / 32.0

var EntitySpawnS2CPacket.realX
    get() = x / 32.0
    set(value) {
        x = (value * 32.0).roundToInt()
    }
var EntitySpawnS2CPacket.realY
    get() = y / 32.0
    set(value) {
        y = (value * 32.0).roundToInt()
    }
var EntitySpawnS2CPacket.realZ
    get() = z / 32.0
    set(value) {
        z = (value * 32.0).roundToInt()
    }

val PlayerSpawnS2CPacket
.realX
    get() = x / 32.0
val PlayerSpawnS2CPacket
.realY
    get() = y / 32.0
val PlayerSpawnS2CPacket
.realZ
    get() = z / 32.0

val MobSpawnS2CPacket.realX
    get() = x / 32.0
val MobSpawnS2CPacket.realY
    get() = y / 32.0
val MobSpawnS2CPacket.realZ
    get() = z / 32.0

val EntityPositionS2CPacket.realX
    get() = x / 32.0
val EntityPositionS2CPacket.realY
    get() = y / 32.0
val EntityPositionS2CPacket.realZ
    get() = z / 32.0