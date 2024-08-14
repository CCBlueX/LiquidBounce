package net.ccbluex.liquidbounce.features.module.modules.bmw

import net.ccbluex.liquidbounce.event.events.PlayerUseMultiplier
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.minecraft.util.UseAction

object ModuleDropEatNoSlow : Module("DropEatNoSlow", Category.BMW) {

    private var dropped = false

    val multiplierEventHandler = handler<PlayerUseMultiplier> { event ->
        if (player.activeItem.useAction != UseAction.EAT || player.itemUseTimeLeft <= 0) {
            dropped = false
            return@handler
        }

        if (!dropped) {
            player.dropSelectedItem(false)
            dropped = true
        } else {
            event.forward = 1f
            event.sideways = 1f
        }
    }

    override fun enable() {
        dropped = false
    }

}
