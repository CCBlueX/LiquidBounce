/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.network.IPacket
import net.ccbluex.liquidbounce.api.minecraft.network.handshake.client.ICPacketHandshake
import net.ccbluex.liquidbounce.api.minecraft.network.play.client.*
import net.ccbluex.liquidbounce.api.minecraft.network.play.server.*
import net.minecraft.network.Packet
import net.minecraft.network.handshake.client.C00Handshake
import net.minecraft.network.play.client.*
import net.minecraft.network.play.server.*

open class PacketImpl<out T : Packet<*>>(val wrapped: T) : IPacket
{
	// <editor-fold desc="Type casting to server-side packet">
	override fun asSPacketAnimation(): ISPacketAnimation = SPacketAnimationImpl(wrapped as S0BPacketAnimation)

	override fun asSPacketEntity(): ISPacketEntity = SPacketEntityImpl(wrapped as S14PacketEntity)

	override fun asSPacketEntityVelocity(): ISPacketEntityVelocity = SPacketEntityVelocityImpl(wrapped as S12PacketEntityVelocity)

	override fun asSPacketCloseWindow(): ISPacketCloseWindow = SPacketCloseWindowImpl(wrapped as S2EPacketCloseWindow)

	override fun asSPacketTabComplete(): ISPacketTabComplete = SPacketTabCompleteImpl(wrapped as S3APacketTabComplete)

	override fun asSPacketPlayerPosLook(): ISPacketPlayerPosLook = SPacketPlayerPosLookImpl(wrapped as S08PacketPlayerPosLook)

	override fun asSPacketResourcePackSend(): ISPacketResourcePackSend = SPacketResourcePackSendImpl(wrapped as S48PacketResourcePackSend)

	override fun asSPacketWindowItems(): ISPacketWindowItems = SPacketWindowItemsImpl(wrapped as S30PacketWindowItems)

	override fun asSPacketChat(): ISPacketChat = SPacketChatImpl(wrapped as S02PacketChat)

	override fun asSPacketCustomPayload(): ISPacketCustomPayload = SPacketCustomPayloadImpl(wrapped as S3FPacketCustomPayload)

	override fun asSPacketSpawnPlayer(): ISPacketPlayerSpawn = SPacketPlayerSpawnImpl(wrapped as S0CPacketSpawnPlayer)

	override fun asSPacketEntityTeleport(): ISPacketEntityTeleport = SPacketEntityTeleportImpl(wrapped as S18PacketEntityTeleport)

	override fun asSPacketTitle(): ISPacketTitle = SPacketTitleImpl(wrapped as S45PacketTitle)

	override fun asSPacketPlayerListItem(): ISPacketPlayerListItem = SPacketPlayerListItemImpl(wrapped as S38PacketPlayerListItem)

	override fun asSPacketChangeGameState(): ISPacketChangeGameState = SPacketChangeGameStateImpl(wrapped as S2BPacketChangeGameState)

	override fun asSPacketEntityEffect(): ISPacketEntityEffect = SPacketEntityEffectImpl(wrapped as S1DPacketEntityEffect)

	override fun asSPacketSpawnGlobalEntity(): ISPacketSpawnGlobalEntity = SPacketSpawnGlobalEntityImpl(wrapped as S2CPacketSpawnGlobalEntity)

	override fun asSPacketEntityEquipment(): ISPacketEntityEquipment = SPacketEntityEquipmentImpl(wrapped as S04PacketEntityEquipment)
	// </editor-fold>

	// <editor-fold desc="Type casting to client-side packet">
	override fun asCPacketPlayerAbilities(): ICPacketPlayerAbilities = CPacketPlayerAbilitiesImpl(wrapped as C13PacketPlayerAbilities)

	override fun asCPacketChatMessage(): ICPacketChatMessage = CPacketChatMessageImpl(wrapped as C01PacketChatMessage)

	override fun asCPacketHeldItemChange(): ICPacketHeldItemChange = CPacketHeldItemChangeImpl(wrapped as C09PacketHeldItemChange)

	override fun asCPacketPlayer(): ICPacketPlayer = CPacketPlayerImpl(wrapped as C03PacketPlayer)

	override fun asCPacketUseEntity(): ICPacketUseEntity = CPacketUseEntityImpl(wrapped as C02PacketUseEntity)

	override fun asCPacketCustomPayload(): ICPacketCustomPayload = CPacketCustomPayloadImpl(wrapped as C17PacketCustomPayload)

	override fun asCPacketHandshake(): ICPacketHandshake = CPacketHandshakeImpl(wrapped as C00Handshake)

	override fun asCPacketPlayerDigging(): ICPacketPlayerDigging = CPacketPlayerDiggingImpl(wrapped as C07PacketPlayerDigging)

	override fun asCPacketPlayerBlockPlacement(): ICPacketPlayerBlockPlacement = CPacketPlayerBlockPlacementImpl(wrapped as C08PacketPlayerBlockPlacement)

	override fun asCPacketKeepAlive(): ICPacketKeepAlive = CPacketKeepAliveImpl(wrapped as C00PacketKeepAlive)

	override fun asCPacketConfirmTransaction(): ICPacketConfirmTransaction = CPacketConfirmTransactionImpl(wrapped as C0FPacketConfirmTransaction)

	override fun asCPacketClientStatus(): ICPacketClientStatus = CPacketClientStatusImpl(wrapped as C16PacketClientStatus)

	override fun asCPacketCloseWindow(): ICPacketCloseWindow = CPacketCloseWindowImpl(wrapped as C0DPacketCloseWindow)
	// </editor-fold>

	override fun equals(other: Any?): Boolean = other is PacketImpl<*> && other.wrapped == wrapped
}

fun IPacket.unwrap(): Packet<*> = (this as PacketImpl<*>).wrapped
fun Packet<*>.wrap(): IPacket = PacketImpl(this)
