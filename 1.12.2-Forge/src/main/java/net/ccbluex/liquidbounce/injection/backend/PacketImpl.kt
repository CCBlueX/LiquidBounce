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
	// Server-sided
	override fun asSPacketAnimation(): ISPacketAnimation = SPacketAnimationImpl(wrapped as SPacketAnimation)

	override fun asSPacketEntity(): ISPacketEntity = SPacketEntityImpl(wrapped as SPacketEntity)

	override fun asSPacketEntityVelocity(): ISPacketEntityVelocity = SPacketEntityVelocityImpl(wrapped as SPacketEntityVelocity)

	override fun asSPacketCloseWindow(): ISPacketCloseWindow = SPacketCloseWindowImpl(wrapped as SPacketCloseWindow)

	override fun asSPacketTabComplete(): ISPacketTabComplete = SPacketTabCompleteImpl(wrapped as SPacketTabComplete)

	override fun asSPacketPosLook(): ISPacketPosLook = SPacketPosLookImpl(wrapped as SPacketPlayerPosLook)

	override fun asSPacketResourcePackSend(): ISPacketResourcePackSend = SPacketResourcePackSendImpl(wrapped as SPacketResourcePackSend)

	override fun asSPacketWindowItems(): ISPacketWindowItems = SPacketWindowItemsImpl(wrapped as SPacketWindowItems)

	override fun asSPacketChat(): ISPacketChat = SPacketChatImpl(wrapped as SPacketChat)

	override fun asSPacketCustomPayload(): ISPacketCustomPayload = SPacketCustomPayloadImpl(wrapped as SPacketCustomPayload)

	override fun asSPacketSpawnPlayer(): ISPacketPlayerSpawn = SPacketPlayerSpawnImpl(wrapped as SPacketSpawnPlayer)

	override fun asSPacketEntityTeleport(): ISPacketEntityTeleport = SPacketEntityTeleportImpl(wrapped as SPacketEntityTeleport)

	override fun asSPacketTitle(): ISPacketTitle = SPacketTitleImpl(wrapped as SPacketTitle)

	// Client-sided
	override fun asCPacketPlayer(): ICPacketPlayer = CPacketPlayerImpl(wrapped as CPacketPlayer)

	override fun asCPacketUseEntity(): ICPacketUseEntity = CPacketUseEntityImpl(wrapped as CPacketUseEntity)

	override fun asCPacketChatMessage(): ICPacketChatMessage = CPacketChatMessageImpl(wrapped as CPacketChatMessage)

	override fun asCPacketHeldItemChange(): ICPacketHeldItemChange = CPacketHeldItemChangeImpl(wrapped as CPacketHeldItemChange)

	override fun asCPacketCustomPayload(): ICPacketCustomPayload = CPacketCustomPayloadImpl(wrapped as CPacketCustomPayload)

	override fun asCPacketHandshake(): ICPacketHandshake = CPacketHandshakeImpl(wrapped as C00Handshake)

	override fun asCPacketPlayerDigging(): ICPacketPlayerDigging = CPacketPlayerDiggingImpl(wrapped as CPacketPlayerDigging)

	override fun asCPacketPlayerBlockPlacement(): ICPacketPlayerBlockPlacement = CPacketPlayerBlockPlacementImpl(wrapped as net.minecraft.network.play.client.CPacketPlayerTryUseItemOnBlock)

	override fun equals(other: Any?): Boolean = other is PacketImpl<*> && other.wrapped == wrapped
}

fun IPacket.unwrap(): Packet<*> = (this as PacketImpl<*>).wrapped
fun Packet<*>.wrap(): IPacket = PacketImpl(this)
