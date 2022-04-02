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

open class PacketImpl<T : Packet<*>>(val wrapped: T) : IPacket {
    override fun asSPacketAnimation(): ISPacketAnimation = SPacketAnimationImpl(wrapped as S0BPacketAnimation)

    override fun asSPacketEntity(): ISPacketEntity = SPacketEntityImpl(wrapped as S14PacketEntity)

    override fun asCPacketPlayer(): ICPacketPlayer = CPacketPlayerImpl(wrapped as C03PacketPlayer)

    override fun asCPacketUseEntity(): ICPacketUseEntity = CPacketUseEntityImpl(wrapped as C02PacketUseEntity)

    override fun asSPacketEntityVelocity(): ISPacketEntityVelocity = SPacketEntityVelocityImpl(wrapped as S12PacketEntityVelocity)

    override fun asCPacketChatMessage(): ICPacketChatMessage = CPacketChatMessageImpl(wrapped as C01PacketChatMessage)

    override fun asSPacketCloseWindow(): ISPacketCloseWindow = SPacketCloseWindowImpl(wrapped as S2EPacketCloseWindow)

    override fun asSPacketTabComplete(): ISPacketTabComplete = SPacketTabCompleteImpl(wrapped as S3APacketTabComplete)

    override fun asSPacketPosLook(): ISPacketPosLook = SPacketPosLookImpl(wrapped as S08PacketPlayerPosLook)

    override fun asSPacketResourcePackSend(): ISPacketResourcePackSend = SPacketResourcePackSendImpl(wrapped as S48PacketResourcePackSend)

    override fun asCPacketHeldItemChange(): ICPacketHeldItemChange = CPacketHeldItemChangeImpl(wrapped as C09PacketHeldItemChange)

    override fun asSPacketWindowItems(): ISPacketWindowItems = SPacketWindowItemsImpl(wrapped as S30PacketWindowItems)

    override fun asCPacketCustomPayload(): ICPacketCustomPayload = CPacketCustomPayloadImpl(wrapped as C17PacketCustomPayload)

    override fun asCPacketHandshake(): ICPacketHandshake = CPacketHandshakeImpl(wrapped as C00Handshake)

    override fun equals(other: Any?): Boolean {
        return other is PacketImpl<*> && other.wrapped == this.wrapped
    }
}

inline fun IPacket.unwrap(): Packet<*> = (this as PacketImpl<*>).wrapped
inline fun Packet<*>.wrap(): IPacket = PacketImpl(this)