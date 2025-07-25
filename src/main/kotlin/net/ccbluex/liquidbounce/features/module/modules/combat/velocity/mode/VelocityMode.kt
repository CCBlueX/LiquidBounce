package net.ccbluex.liquidbounce.features.module.modules.combat.velocity.mode

import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.features.module.modules.combat.velocity.ModuleVelocity.modes
import net.ccbluex.liquidbounce.features.module.modules.combat.velocity.ModuleVelocity.pause

abstract class VelocityMode(name: String) : Choice(name) {

    override val parent: ChoiceConfigurable<VelocityMode>
        get() = modes

    override val running: Boolean
        get() = super.running && pause == 0

}
