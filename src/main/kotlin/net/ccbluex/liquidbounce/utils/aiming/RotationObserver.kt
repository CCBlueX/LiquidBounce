package net.ccbluex.liquidbounce.utils.aiming

import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.fakelag.FakeLag
import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleBacktrack
import net.ccbluex.liquidbounce.utils.aiming.data.Orientation
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.entity.lastOrientation
import net.ccbluex.liquidbounce.utils.entity.orientation
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket

object RotationObserver : Listenable {

    /**
     * The rotation we want to aim at. This DOES NOT mean that the server already received this rotation.
     */
    var currentOrientation: Orientation? = null
        set(value) {
            previousOrientation = field ?: mc.player?.orientation ?: Orientation.ZERO

            field = value
        }

    // Used for rotation interpolation
    var previousOrientation: Orientation? = null

    private val fakeLagging
        get() = FakeLag.isLagging || ModuleBacktrack.isLagging()

    val serverOrientation: Orientation
        get() = if (fakeLagging) theoreticalServerOrientation else actualServerOrientation

    /**
     * The rotation that was already sent to the server and is currently active.
     * The value is not being written by the packets, but we gather the Rotation from the last yaw and pitch variables
     * from our player instance handled by the sendMovementPackets() function.
     */
    var actualServerOrientation = Orientation.ZERO
        private set
    private var theoreticalServerOrientation = Orientation.ZERO

    private var triggerNoDifference = false

    fun rotationMatchesPreviousRotation(): Boolean {
        currentOrientation?.let {
            return it == previousOrientation
        }

        return player.orientation == player.lastOrientation
    }

    @Suppress("unused")
    private val gameTickHandler = handler<GameTickEvent>(priority = EventPriorityConvention.FIRST_PRIORITY) {
        // Reset the trigger
        if (triggerNoDifference) {
            triggerNoDifference = false
        }
    }

    /**
     * Track rotation changes
     *
     * We cannot only rely on player.lastYaw and player.lastPitch because
     * sometimes we update the rotation off chain (e.g. on interactItem)
     * and the player.lastYaw and player.lastPitch are not updated.
     */
    @Suppress("unused")
    private val packetHandler = handler<PacketEvent>(priority = -1000) {
        val packet = it.packet

        val rotation = when (packet) {
            is PlayerMoveC2SPacket -> {
                // If we are not changing the look, we don't need to update the rotation
                // but, we want to handle slow start triggers
                if (!packet.changeLook) {
                    triggerNoDifference = true
                    return@handler
                }

                Orientation(packet.yaw, packet.pitch)
            }
            is PlayerPositionLookS2CPacket -> Orientation(packet.yaw, packet.pitch)
            is PlayerInteractItemC2SPacket -> Orientation(packet.yaw, packet.pitch)
            else -> return@handler
        }

        // This normally applies to Modules like Blink, BadWifi, etc.
        if (!it.isCancelled) {
            actualServerOrientation = rotation
        }

        theoreticalServerOrientation = rotation
    }

}
