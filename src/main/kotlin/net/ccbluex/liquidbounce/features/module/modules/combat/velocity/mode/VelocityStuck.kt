package net.ccbluex.liquidbounce.features.module.modules.combat.velocity.mode

import com.google.common.collect.Queues
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.PlayerTickEvent
import net.ccbluex.liquidbounce.event.events.TransferOrigin
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.sequenceHandler
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.aiming.utils.raycast
import net.ccbluex.liquidbounce.utils.client.PacketSnapshot
import net.ccbluex.liquidbounce.utils.client.SilentHotbar
import net.ccbluex.liquidbounce.utils.client.handlePacket
import net.ccbluex.liquidbounce.utils.inventory.InventoryManager
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen
import net.minecraft.item.Items
import net.minecraft.item.SwordItem
import net.minecraft.item.consume.UseAction
import net.minecraft.network.packet.c2s.common.CommonPongC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket
import net.minecraft.network.packet.s2c.play.EntityDamageS2CPacket
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult

@Suppress("all")
internal object VelocityStuck : VelocityMode("Freeze") {
    private var canCancelVelocity = false
    private var isDelayingPackets = false
    private var requiresClickAction = false
    private var awaitingBlockUpdate = false
    private var awaitingPingResponse = false
    private var skipNextInteraction = false
    private var targetBlockHit: BlockHitResult? = null
    private val queuedPackets = Queues.newConcurrentLinkedQueue<PacketSnapshot>()
    private var freezeTickCounter = 0

    override fun enable() {
        canCancelVelocity = false
        isDelayingPackets = false
        requiresClickAction = false
        awaitingBlockUpdate = false
        awaitingPingResponse = false
        skipNextInteraction = false
        targetBlockHit = null
        queuedPackets.clear()
    }

    override fun disable() {
        queuedPackets.forEach { handlePacket(it.packet) }
        queuedPackets.clear()
    }

    @Suppress("unused")
    private val packetInterceptor = sequenceHandler<PacketEvent> { event ->
        val packet = event.packet

        when (packet) {
            is PlayerInteractEntityC2SPacket, is PlayerInteractBlockC2SPacket -> {
                skipNextInteraction = true
            }

            is PlayerMoveC2SPacket -> {
                if (packet.changesPosition() && awaitingBlockUpdate) {
                    event.cancelEvent()
                }
            }

            is CommonPongC2SPacket -> {
                if (awaitingPingResponse) {
                    waitTicks(1)
                    awaitingBlockUpdate = false
                    awaitingPingResponse = false
                }
                return@sequenceHandler
            }

            is BlockUpdateS2CPacket -> {
                if (packet.pos == player.blockPos && awaitingBlockUpdate) {
                    waitTicks(1)
                    awaitingPingResponse = true
                    requiresClickAction = false
                    return@sequenceHandler
                }
            }

            is EntityDamageS2CPacket -> {
                if (packet.entityId == player.id) {
                    canCancelVelocity = true
                }
            }

            is EntityVelocityUpdateS2CPacket -> {
                if (packet.entityId == player.id && canCancelVelocity) {
                    event.cancelEvent()
                    isDelayingPackets = true
                    canCancelVelocity = false
                    requiresClickAction = true
                }
            }

            is ExplosionS2CPacket -> {
                if (canCancelVelocity
                    && player.activeItem.useAction != UseAction.EAT
                    && player.activeItem.useAction != UseAction.DRINK
                    && !InventoryManager.isInventoryOpen
                    && mc.currentScreen !is GenericContainerScreen
                ) {
                    event.cancelEvent()
                    isDelayingPackets = true
                    canCancelVelocity = false
                    requiresClickAction = true
                }
            }
        }

        if (event.isCancelled || event.origin == TransferOrigin.OUTGOING) return@sequenceHandler

        if (awaitingBlockUpdate) return@sequenceHandler

        if (isDelayingPackets) {
            queuedPackets.add(PacketSnapshot(packet, event.origin, System.currentTimeMillis()))
            event.cancelEvent()
        }
    }

    private fun switchToEmptyOrWeapon() {
        val currentItem = player.inventory.mainHandStack.item
        if (currentItem == Items.FIRE_CHARGE || currentItem == Items.FLINT_AND_STEEL) {
            val targetSlot = Slots.Hotbar.find { slot ->
                val stack = slot.itemStack
                stack.isEmpty || stack.item is SwordItem
            }
            targetSlot?.let { SilentHotbar.selectSlotSilently(this, it, 20) }
        }
    }

    @Suppress("unused")
    private val tickListener = handler<PlayerTickEvent> { event ->
        if (requiresClickAction) {
            // Switch to empty hand or weapon before block interaction
            switchToEmptyOrWeapon()

            targetBlockHit = raycast(rotation = Rotation(player.yaw, 90f))
            val placePos = targetBlockHit?.blockPos?.offset(targetBlockHit!!.side)
            if (placePos != player.blockPos || skipNextInteraction || player.isUsingItem) {
                targetBlockHit = null
            }
        }

        targetBlockHit?.let { hitResult ->
            isDelayingPackets = false
            queuedPackets.forEach { handlePacket(it.packet) }
            queuedPackets.clear()



            if (RotationManager.serverRotation.pitch != 90f) {
                network.sendPacket(
                    PlayerMoveC2SPacket.LookAndOnGround(
                        player.yaw - (Math.random() * (0.004 - 0.002)).toFloat(),
                        89.1f + (Math.random() * (90.0 - 89.1)).toFloat() - (Math.random() * (1.0 - 0.002)).toFloat(),
                        player.isOnGround,
                        player.horizontalCollision
                    )
                )
            } else {
                network.sendPacket(
                    PlayerMoveC2SPacket.OnGroundOnly(
                        player.isOnGround,
                        player.horizontalCollision
                    )
                )
            }

            interaction.interactBlock(player, Hand.MAIN_HAND, hitResult)?.let {
                if (it == ActionResult.SUCCESS) {
                    player.swingHand(Hand.MAIN_HAND)
                }
            }
            freezeTickCounter = 0
            awaitingBlockUpdate = true
            targetBlockHit = null
            requiresClickAction = false
        }

        if (awaitingBlockUpdate) {
            event.cancelEvent()
            freezeTickCounter++
            if (freezeTickCounter > 20) {
                awaitingBlockUpdate = false
                awaitingPingResponse = false
                requiresClickAction = false
            }
        }

        skipNextInteraction = false
    }
}
