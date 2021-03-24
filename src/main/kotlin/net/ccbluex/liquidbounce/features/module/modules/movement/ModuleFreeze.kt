package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.repeatableSequence

object ModuleFreeze : Module("Module", Category.MOVEMENT) {

    val repeatable = repeatableSequence {

        // player.isDead = true
        player.yaw = player.renderYaw
        player.pitch = player.renderPitch
    }

    override fun disable() {
        // player.isDead = false
    }
}
