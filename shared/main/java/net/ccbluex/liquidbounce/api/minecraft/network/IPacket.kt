/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.api.minecraft.network

import net.ccbluex.liquidbounce.api.minecraft.network.handshake.client.ICPacketHandshake
import net.ccbluex.liquidbounce.api.minecraft.network.play.client.*
import net.ccbluex.liquidbounce.api.minecraft.network.play.server.*

interface IPacket
{
    // <editor-fold desc="Type casting to server-side packet">
    fun asSPacketAnimation(): ISPacketAnimation
    fun asSPacketEntity(): ISPacketEntity
    fun asSPacketEntityVelocity(): ISPacketEntityVelocity
    fun asSPacketCloseWindow(): ISPacketCloseWindow
    fun asSPacketTabComplete(): ISPacketTabComplete
    fun asSPacketPlayerPosLook(): ISPacketPlayerPosLook
    fun asSPacketResourcePackSend(): ISPacketResourcePackSend
    fun asSPacketWindowItems(): ISPacketWindowItems
    fun asSPacketChat(): ISPacketChat
    fun asSPacketCustomPayload(): ISPacketCustomPayload
    fun asSPacketSpawnPlayer(): ISPacketPlayerSpawn
    fun asSPacketEntityTeleport(): ISPacketEntityTeleport
    fun asSPacketTitle(): ISPacketTitle
    fun asSPacketPlayerListItem(): ISPacketPlayerListItem
    fun asSPacketChangeGameState(): ISPacketChangeGameState
    fun asSPacketEntityEffect(): ISPacketEntityEffect
    fun asSPacketSpawnGlobalEntity(): ISPacketSpawnGlobalEntity
    fun asSPacketEntityEquipment(): ISPacketEntityEquipment
    // </editor-fold>

    // <editor-fold desc="Type casting to client-side packet">
    fun asCPacketPlayer(): ICPacketPlayer
    fun asCPacketUseEntity(): ICPacketUseEntity
    fun asCPacketChatMessage(): ICPacketChatMessage
    fun asCPacketHeldItemChange(): ICPacketHeldItemChange
    fun asCPacketCustomPayload(): ICPacketCustomPayload
    fun asCPacketHandshake(): ICPacketHandshake
    fun asCPacketPlayerDigging(): ICPacketPlayerDigging
    fun asCPacketPlayerBlockPlacement(): ICPacketPlayerBlockPlacement
    fun asCPacketKeepAlive(): ICPacketKeepAlive
    fun asCPacketPlayerAbilities(): ICPacketPlayerAbilities
    fun asCPacketConfirmTransaction(): ICPacketConfirmTransaction
    fun asCPacketClientStatus(): ICPacketClientStatus
    fun asCPacketCloseWindow(): ICPacketCloseWindow
    fun asCPacketEntityAction(): ICPacketEntityAction
    // </editor-fold>
}
