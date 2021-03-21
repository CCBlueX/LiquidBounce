
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.repeatableSequence

object ModuleFreeze : Module("Freeze", Category.MOVEMENT) {

    val tickRepeatable = repeatableSequence {

        player.isDead
        player.yaw = player.renderYaw
        player.pitch = player.renderPitch
    }

    override fun disable() {
        !player.isDead
    }
}
