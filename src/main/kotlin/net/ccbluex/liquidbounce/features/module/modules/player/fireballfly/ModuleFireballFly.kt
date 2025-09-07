package net.ccbluex.liquidbounce.features.module.modules.player.fireballfly

import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.NotificationEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.RotationUpdateEvent
import net.ccbluex.liquidbounce.event.events.TransferOrigin
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsConfigurable
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.client.PacketSnapshot
import net.ccbluex.liquidbounce.utils.client.SilentHotbar
import net.ccbluex.liquidbounce.utils.client.notification
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.ccbluex.liquidbounce.utils.inventory.findClosestSlot
import net.ccbluex.liquidbounce.utils.inventory.interactItem
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.minecraft.item.Items
import net.minecraft.network.packet.Packet
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket
import net.minecraft.network.packet.c2s.play.CommandExecutionC2SPacket
import net.minecraft.network.packet.s2c.common.DisconnectS2CPacket
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket
import net.minecraft.network.packet.s2c.play.HealthUpdateS2CPacket
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket
import net.minecraft.sound.SoundEvents
import net.minecraft.util.math.MathHelper

object ModuleFireballFly : ClientModule("FireballFly", Category.MOVEMENT, disableOnQuit = true) {

    private val fireballDelay by int("FireballDelay", 10, 1..200, "ticks")
    private val maxFireballCount by int("MaxFireballCount", 4, 1..64)
    private val slotResetDelay by int("SlotResetDelay", 5, 0..40, "ticks")

    private object Jump : ToggleableConfigurable(this, "Jump", true) {
        val jumpDelay by int("JumpDelay", 3, 0..20, "ticks")
    }

    private object Rotations : RotationsConfigurable(this) {
        val pitch by float("Pitch", 70f, 0f..90f)
        val backwards by boolean("Backwards", true)
    }

    init {
        tree(Jump)
        tree(Rotations)
    }

    private val delayedPacketQueue = mutableListOf<PacketSnapshot>()
    val packetProcessQueue = mutableListOf<Packet<*>>()

    private var canThrow = false
    private var canRotate = false
    private var delay = 0
    private var fireballCount = 0
    private var totalFireballCount = 0

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent> { event ->
        if (event.origin != TransferOrigin.INCOMING || event.isCancelled) {
            return@handler
        }

        val packet = event.packet

        when (packet) {
            is ChatMessageC2SPacket, is GameMessageS2CPacket, is CommandExecutionC2SPacket -> {
                return@handler
            }

            is PlayerPositionLookS2CPacket, is DisconnectS2CPacket -> {
                processPackets()
                return@handler
            }

            is PlaySoundS2CPacket -> {
                if (packet.sound.value() == SoundEvents.ENTITY_PLAYER_HURT) {
                    return@handler
                }
            }

            is HealthUpdateS2CPacket -> {
                if (packet.health <= 0) {
                    processPackets()
                    return@handler
                }
            }
        }

        event.cancelEvent()
        delayedPacketQueue.add(PacketSnapshot(packet, event.origin, System.currentTimeMillis()))
    }

    @Suppress("unused")
    private val rotationUpdateEventHandler = handler<RotationUpdateEvent> {
        if (canRotate) {
            val rotation = Rotation(if (Rotations.backwards) invertYaw(player.yaw) else player.yaw, Rotations.pitch)
            RotationManager.setRotationTarget(
                rotation = rotation,
                configurable = Rotations,
                priority = Priority.IMPORTANT_FOR_PLAYER_LIFE,
                provider = this
            )
        }
    }


    fun processPackets() {
        delayedPacketQueue.removeIf {
            if (it.timestamp <= System.currentTimeMillis() - delay * 50) {
                packetProcessQueue.add(it.packet)
                true
            } else {
                false
            }
        }
    }

    @Suppress("unused")
    private val tickHandler = tickHandler {
        if (!canThrow) return@tickHandler
        canThrow = false

        val fireballItem = Slots.OffhandWithHotbar.findClosestSlot(Items.FIRE_CHARGE)
        if (fireballItem != null) {
            SilentHotbar.selectSlotSilently(this, fireballItem, slotResetDelay)
        } else {
            SilentHotbar.resetSlot(this)
            enabled = false
            return@tickHandler
        }

        if (Jump.enabled) {
            if (player.isOnGround) player.jump()
            waitTicks(Jump.jumpDelay)
        }

        interactItem(fireballItem.useHand)
        fireballCount--
        notification("FireballFly",
            "Thrown a fireball (${totalFireballCount - fireballCount} / $totalFireballCount)",
            NotificationEvent.Severity.INFO
        )

        if (fireballCount != 0) {
            waitTicks(fireballDelay - if (Jump.enabled) Jump.jumpDelay else 0)
            canThrow = true
        } else {
            canRotate = false
            waitTicks(delay + 5)
            enabled = false
            processPackets()
            canThrow = false
        }
    }

    private fun invertYaw(yaw: Float): Float {
        return MathHelper.wrapDegrees(yaw + 180)
    }

    override fun onEnabled() {
        val fireballItem = Slots.OffhandWithHotbar.findClosestSlot(Items.FIRE_CHARGE)
        if (fireballItem != null) {
            val count = fireballItem.itemStack.count
            fireballCount = if (count < maxFireballCount) count else maxFireballCount
            totalFireballCount = fireballCount
            delay = fireballCount * fireballDelay
            canThrow = true
            canRotate = true
        } else {
            enabled = false
        }
    }

    override fun onDisabled() {
        processPackets()
        canThrow = false
        canRotate = false
    }

}
