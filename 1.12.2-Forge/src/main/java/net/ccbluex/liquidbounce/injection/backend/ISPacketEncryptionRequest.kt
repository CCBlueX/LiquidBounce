package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.network.login.server.ISPacketEncryptionRequest
import net.minecraft.network.login.server.SPacketEncryptionRequest

class SPacketEncryptionRequestImpl<T : SPacketEncryptionRequest>(wrapped: T) : PacketImpl<T>(wrapped), ISPacketEncryptionRequest {
    override val verifyToken: ByteArray
        get() = wrapped.verifyToken

}

 fun ISPacketEncryptionRequest.unwrap(): SPacketEncryptionRequest = (this as SPacketEncryptionRequestImpl<*>).wrapped
 fun SPacketEncryptionRequest.wrap(): ISPacketEncryptionRequest = SPacketEncryptionRequestImpl(this)
