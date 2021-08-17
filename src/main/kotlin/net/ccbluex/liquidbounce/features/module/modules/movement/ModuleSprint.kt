package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.PlayerMoveEvent
import net.ccbluex.liquidbounce.event.PlayerTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.entity.moving

object ModuleSprint : Module("Sprint", Category.MOVEMENT){
    //It can only be used in small games
    val tickHandler = handler<PlayerTickEvent> {
        if (player.moving){
            mc.player?.setSprinting(true);
        }
    }

        }

