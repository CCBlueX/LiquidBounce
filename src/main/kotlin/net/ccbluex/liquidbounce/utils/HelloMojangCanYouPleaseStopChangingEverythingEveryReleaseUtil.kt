package net.ccbluex.liquidbounce.utils

import net.ccbluex.liquidbounce.utils.entity.copy
import net.minecraft.client.network.ClientPlayNetworkHandler
import net.minecraft.network.packet.c2s.play.PlayerInputC2SPacket
import net.minecraft.util.PlayerInput

fun ClientPlayNetworkHandler.sendPlayerInput(input: PlayerInput) {
    this.sendPacket(PlayerInputC2SPacket(input))
}

fun ClientPlayNetworkHandler.sendSneaking(sneak: Boolean) {
    this.sendPacket(PlayerInputC2SPacket(PlayerInput.DEFAULT.copy(sneak = sneak)))
}
