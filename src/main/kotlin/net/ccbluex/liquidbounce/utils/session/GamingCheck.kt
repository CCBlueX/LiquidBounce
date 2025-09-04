package net.ccbluex.liquidbounce.utils.session

import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.PlayerTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.client.world
import net.minecraft.block.Blocks

object GamingCheck : EventListener {
    var OnGlass : Boolean = false

    init {
        handler<PlayerTickEvent> { event ->
            val blockPos = player.blockPos.down()
            val block = world.getBlockState(blockPos).block
            OnGlass  = block == Blocks.GLASS || block == Blocks.TINTED_GLASS
        }
    }
}
