/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package net.ccbluex.liquidbounce.utils.client

import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.TransferOrigin
import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.SwitchMode
import net.ccbluex.liquidbounce.features.module.modules.misc.ModulePacketLogger
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.block.SwingMode
import net.ccbluex.liquidbounce.utils.input.shouldSwingHand
import net.ccbluex.liquidbounce.utils.inventory.OffHandSlot
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.client.network.ClientPlayerInteractionManager
import net.minecraft.client.network.SequencedPacketCreator
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.item.ItemUsageContext
import net.minecraft.network.listener.ClientPlayPacketListener
import net.minecraft.network.packet.Packet
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.world.GameMode
import org.apache.commons.lang3.mutable.MutableObject
import java.util.*

@Suppress("LongParameterList")
fun clickBlockWithSlot(
    player: ClientPlayerEntity,
    rayTraceResult: BlockHitResult,
    slot: Int,
    swingMode: SwingMode,
    switchMode: SwitchMode = SwitchMode.SILENT,
    sequenced: Boolean = true
) {
    val hand = if (slot == OffHandSlot.hotbarSlotForServer) {
        Hand.OFF_HAND
    } else {
        Hand.MAIN_HAND
    }

    val prevHotbarSlot = player.inventory.selectedSlot
    if (hand == Hand.MAIN_HAND) {
        if (switchMode == SwitchMode.NONE && slot != prevHotbarSlot) {
            // the slot is not selected and we can't switch
            return
        }

        player.inventory.selectedSlot = slot

        if (slot != prevHotbarSlot) {
            player.networkHandler.sendPacket(UpdateSelectedSlotC2SPacket(slot))
        }
    }

    if (sequenced) {
        interaction.sendSequencedPacket(world) { sequence ->
            PlayerInteractBlockC2SPacket(hand, rayTraceResult, sequence)
        }
    } else {
        network.sendPacket(PlayerInteractBlockC2SPacket(hand, rayTraceResult, 0))
    }

    val itemUsageContext = ItemUsageContext(player, hand, rayTraceResult)

    val itemStack = player.inventory.getStack(slot)

    val actionResult: ActionResult

    if (player.isCreative) {
        val i = itemStack.count
        actionResult = itemStack.useOnBlock(itemUsageContext)
        itemStack.count = i
    } else {
        actionResult = itemStack.useOnBlock(itemUsageContext)
    }

    if (actionResult.shouldSwingHand()) {
        swingMode.swing(hand)
    }

    if (slot != prevHotbarSlot && hand == Hand.MAIN_HAND && switchMode == SwitchMode.SILENT) {
        player.networkHandler.sendPacket(UpdateSelectedSlotC2SPacket(prevHotbarSlot))
    }

    player.inventory.selectedSlot = prevHotbarSlot
}

/**
 * [ClientPlayerInteractionManager.interactItem] but with custom rotations.
 */
fun ClientPlayerInteractionManager.interactItem(
    player: PlayerEntity,
    hand: Hand,
    yaw: Float,
    pitch: Float
): ActionResult {
    if (gameMode == GameMode.SPECTATOR) {
        return ActionResult.PASS
    }

    this.syncSelectedSlot()
    val mutableObject = MutableObject<ActionResult>()
    this.sendSequencedPacket(world, SequencedPacketCreator { sequence: Int ->
        val playerInteractItemC2SPacket = PlayerInteractItemC2SPacket(hand, sequence, yaw, pitch)
        val itemStack = player.getStackInHand(hand)
        if (player.itemCooldownManager.isCoolingDown(itemStack)) {
            mutableObject.setValue(ActionResult.PASS)
            return@SequencedPacketCreator playerInteractItemC2SPacket
        }

        val typedActionResult = itemStack.use(world, player, hand)
        val itemStack2 = if (typedActionResult is ActionResult.Success) {
            Objects.requireNonNullElseGet<ItemStack>(
                typedActionResult.newHandStack
            ) { player.getStackInHand(hand) } as ItemStack
        } else {
            player.getStackInHand(hand)
        }

        if (itemStack2 != itemStack) {
            player.setStackInHand(hand, itemStack2)
        }

        mutableObject.setValue(typedActionResult)
        return@SequencedPacketCreator playerInteractItemC2SPacket
    })

    return mutableObject.value
}

fun handlePacket(packet: Packet<*>) =
    runCatching { (packet as Packet<ClientPlayPacketListener>).apply(mc.networkHandler) }

fun sendPacketSilently(packet: Packet<*>) {
    // hack fix for the packet handler not being called on Rotation Manager for tracking
    val packetEvent = PacketEvent(TransferOrigin.OUTGOING, packet, false)
    RotationManager.packetHandler.handler(packetEvent)
    ModulePacketLogger.onPacket(TransferOrigin.OUTGOING, packet)
    mc.networkHandler?.connection?.send(packetEvent.packet, null)
}

enum class MovePacketType(override val choiceName: String, val generatePacket: () -> PlayerMoveC2SPacket)
    : NamedChoice {
    ON_GROUND_ONLY("OnGroundOnly", {
        PlayerMoveC2SPacket.OnGroundOnly(player.isOnGround, player.horizontalCollision)
    }),
    POSITION_AND_ON_GROUND("PositionAndOnGround", {
        PlayerMoveC2SPacket.PositionAndOnGround(player.x, player.y, player.z, player.isOnGround,
            player.horizontalCollision)
    }),
    LOOK_AND_ON_GROUND("LookAndOnGround", {
        PlayerMoveC2SPacket.LookAndOnGround(player.yaw, player.pitch, player.isOnGround, player.horizontalCollision)
    }),
    FULL("Full", {
        PlayerMoveC2SPacket.Full(player.x, player.y, player.z, player.yaw, player.pitch, player.isOnGround,
            player.horizontalCollision)
    });
}
