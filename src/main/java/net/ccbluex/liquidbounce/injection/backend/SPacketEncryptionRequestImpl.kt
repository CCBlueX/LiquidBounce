package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.network.login.server.ISPacketEncryptionRequest
import net.minecraft.network.login.server.S01PacketEncryptionRequest

class SPacketEncryptionRequestImpl<T : S01PacketEncryptionRequest>(wrapped: T) : PacketImpl<T>(wrapped), ISPacketEncryptionRequest {
    override val verifyToken: ByteArray
        get() = wrapped.verifyToken
}

inline fun ISPacketEncryptionRequest.unwrap(): S01PacketEncryptionRequest = (this as SPacketEncryptionRequestImpl<*>).wrapped
inline fun S01PacketEncryptionRequest.wrap(): ISPacketEncryptionRequest = SPacketEncryptionRequestImpl(this)