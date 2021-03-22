package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.repeatableSequence
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket

object ModuleNoFall : Module("NoFall", Category.PLAYER) {
    val tickRepeatable = repeatableSequence {
        if(player.fallDistance > 2f) {
            mc.networkHandler!!.sendPacket(PlayerMoveC2SPacket(true))
        }
    }
}
