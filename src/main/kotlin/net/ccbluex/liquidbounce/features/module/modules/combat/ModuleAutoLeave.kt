package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.repeatableSequence

object ModuleAutoLeave : Module("AutoLeave", Category.COMBAT) {

    private val health by float("Health", 8f, 0f..20f)

    val tickRepeatable = repeatableSequence {
        if(player.health <= health && !player.abilities.creativeMode && !mc.isIntegratedServerRunning) {
            mc.world!!.disconnect()
        }
        ModuleAutoLeave.disable()
    }
}
