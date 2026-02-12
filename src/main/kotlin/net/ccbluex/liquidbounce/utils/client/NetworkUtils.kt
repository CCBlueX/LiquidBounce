/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
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
@file:Suppress("FunctionName", "TooManyFunctions")

package net.ccbluex.liquidbounce.utils.client

import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.TransferOrigin
import net.ccbluex.liquidbounce.event.nextTick
import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.SwitchMode
import net.ccbluex.liquidbounce.features.module.modules.misc.ModulePacketLogger
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.block.SwingMode
import net.ccbluex.liquidbounce.utils.input.shouldSwingHand
import net.ccbluex.liquidbounce.utils.inventory.InventoryManager
import net.ccbluex.liquidbounce.utils.inventory.OffHandSlot
import net.ccbluex.liquidbounce.utils.network.OpenInventorySilentlyPacket
import net.ccbluex.liquidbounce.utils.network.PlayerSneakPacket
import net.ccbluex.liquidbounce.utils.network.sendPacket
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl
import net.minecraft.client.multiplayer.ClientPacketListener
import net.minecraft.client.multiplayer.MultiPlayerGameMode
import net.minecraft.client.player.LocalPlayer
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket
import net.minecraft.network.protocol.game.ServerboundUseItemPacket
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.GameType
import net.minecraft.world.phys.BlockHitResult

fun ClientCommonPacketListenerImpl.send1_21_5StartSneaking() {
    if (!usesViaFabricPlus) return

    sendPacket(PlayerSneakPacket.START)
}

fun ClientCommonPacketListenerImpl.send1_21_5StopSneaking() {
    if (!usesViaFabricPlus) return

    sendPacket(PlayerSneakPacket.STOP)
}

/**
 * Sends an open inventory packet with the help of ViaFabricPlus. This is only for older versions. (<= 1.11.2)
 */
fun ClientCommonPacketListenerImpl.send1_11_1OpenInventory() {
    if (InventoryManager.isInventoryOpenServerSide || !usesViaFabricPlus) {
        return
    }

    sendPacket(
        OpenInventorySilentlyPacket,
        onSuccess = { InventoryManager.isInventoryOpenServerSide = true },
        onFailure = { chat(markAsError("Failed to open inventory using ViaFabricPlus, report to developers!")) }
    )
}

fun ClientCommonPacketListenerImpl.sendStartSprinting() {
    send(ServerboundPlayerCommandPacket(player, ServerboundPlayerCommandPacket.Action.START_SPRINTING))
}

fun ClientCommonPacketListenerImpl.sendStopSprinting() {
    send(ServerboundPlayerCommandPacket(player, ServerboundPlayerCommandPacket.Action.STOP_SPRINTING))
}

fun ClientCommonPacketListenerImpl.sendSwapItemWithOffhand() {
    send(
        ServerboundPlayerActionPacket(
            ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND,
            BlockPos.ZERO,
            Direction.DOWN,
        )
    )
}

fun ClientCommonPacketListenerImpl.sendHeldItemChange(slot: Int) {
    send(ServerboundSetCarriedItemPacket(slot))
}

fun ClientCommonPacketListenerImpl.sendCloseInventory() {
    send(ServerboundContainerClosePacket(0))
}

fun ClientPacketListener.sendChatOrCommand(message: String) =
    if (message.startsWith('/')) {
        sendCommand(message.substring(1))
    } else {
        sendChat(message)
    }

fun LocalPlayer.clickBlockWithSlot(
    rayTraceResult: BlockHitResult,
    slot: Int,
    swingMode: SwingMode,
    switchMode: SwitchMode = SwitchMode.SILENT,
    sequenced: Boolean = true,
) {
    val hand = if (slot == OffHandSlot.hotbarSlotForServer) {
        InteractionHand.OFF_HAND
    } else {
        InteractionHand.MAIN_HAND
    }

    val prevHotbarSlot = this.inventory.selectedSlot
    if (hand == InteractionHand.MAIN_HAND) {
        if (switchMode == SwitchMode.NONE && slot != prevHotbarSlot) {
            // the slot is not selected and we can't switch
            return
        }

        this.inventory.selectedSlot = slot

        if (slot != prevHotbarSlot) {
            connection.sendHeldItemChange(slot)
        }
    }

    if (sequenced) {
        interaction.startPrediction(world) { sequence ->
            ServerboundUseItemOnPacket(hand, rayTraceResult, sequence)
        }
    } else {
        connection.send(ServerboundUseItemOnPacket(hand, rayTraceResult, 0))
    }

    val itemUsageContext = UseOnContext(this, hand, rayTraceResult)

    val itemStack = this.inventory.getItem(slot)

    val actionResult: InteractionResult

    if (this.isCreative) {
        val i = itemStack.count
        actionResult = itemStack.useOn(itemUsageContext)
        itemStack.count = i
    } else {
        actionResult = itemStack.useOn(itemUsageContext)
    }

    if (actionResult.shouldSwingHand()) {
        swingMode.swing(hand)
    }

    if (slot != prevHotbarSlot && hand == InteractionHand.MAIN_HAND && switchMode == SwitchMode.SILENT) {
        connection.sendHeldItemChange(prevHotbarSlot)
    }

    this.inventory.selectedSlot = prevHotbarSlot
}

fun MultiPlayerGameMode.releaseUsingItemNextTick() = nextTick {
    this.releaseUsingItem(player)
}

/**
 * [MultiPlayerGameMode.useItem] but with custom rotations.
 */
fun MultiPlayerGameMode.useItem(
    player: Player,
    hand: InteractionHand,
    yRot: Float,
    xRot: Float,
): InteractionResult {
    if (localPlayerMode == GameType.SPECTATOR) {
        return InteractionResult.PASS
    }

    this.ensureHasSentCarriedItem()
    var interactionResult: InteractionResult = InteractionResult.PASS
    this.startPrediction(world) { sequence ->
        val playerInteractItemC2SPacket = ServerboundUseItemPacket(hand, sequence, yRot, xRot)
        val itemStack = player.getItemInHand(hand)
        if (player.cooldowns.isOnCooldown(itemStack)) {
            interactionResult = InteractionResult.PASS
            return@startPrediction playerInteractItemC2SPacket
        }

        val useResult = itemStack.use(world, player, hand)
        val result = if (useResult is InteractionResult.Success) {
            useResult.heldItemTransformedTo() ?: player.getItemInHand(hand)
        } else {
            player.getItemInHand(hand)
        }

        if (result !== itemStack) {
            player.setItemInHand(hand, result)
        }

        interactionResult = useResult
        return@startPrediction playerInteractItemC2SPacket
    }

    return interactionResult
}

fun handlePacket(packet: Packet<*>) =
    runCatching { (packet as Packet<ClientGamePacketListener>).handle(mc.connection!!) }

fun sendPacketSilently(packet: Packet<*>) {
    // hack fix for the packet handler not being called on Rotation Manager for tracking
    val packetEvent = PacketEvent(TransferOrigin.OUTGOING, packet, false)
    RotationManager.packetHandler.handler.accept(packetEvent)
    ModulePacketLogger.onPacket(TransferOrigin.OUTGOING, packet)
    mc.connection?.connection?.send(packetEvent.packet, null)
}

enum class MovePacketType(override val tag: String, val generatePacket: () -> ServerboundMovePlayerPacket)
    : Tagged {
    ON_GROUND_ONLY("OnGroundOnly", {
        ServerboundMovePlayerPacket.StatusOnly(player.onGround(), player.horizontalCollision)
    }),
    POSITION_AND_ON_GROUND("PositionAndOnGround", {
        ServerboundMovePlayerPacket.Pos(player.x, player.y, player.z, player.onGround(),
            player.horizontalCollision)
    }),
    LOOK_AND_ON_GROUND("LookAndOnGround", {
        ServerboundMovePlayerPacket.Rot(player.yRot, player.xRot, player.onGround(), player.horizontalCollision)
    }),
    FULL("Full", {
        ServerboundMovePlayerPacket.PosRot(player.x, player.y, player.z, player.yRot, player.xRot, player.onGround(),
            player.horizontalCollision)
    });
}
