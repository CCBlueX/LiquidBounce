package net.ccbluex.liquidbounce.api.minecraft.network.login.server

interface ISPacketEncryptionRequest {
    val verifyToken: ByteArray
}