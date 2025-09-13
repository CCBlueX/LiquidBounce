package net.ccbluex.liquidbounce.integration.theme.component.components.targethud

import net.ccbluex.liquidbounce.config.types.nesting.Choice

abstract class TargetHudMode (
    name: String
) : Choice(name) {
    override val parent
        get() = TargetHudComponent.modes
}
