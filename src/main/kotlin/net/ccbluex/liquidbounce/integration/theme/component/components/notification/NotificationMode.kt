package net.ccbluex.liquidbounce.integration.theme.component.components.notification

import net.ccbluex.liquidbounce.config.types.nesting.Choice

abstract class NotificationMode (
    name: String
) : Choice(name) {
    override val parent
        get() = NotificationComponent.modes
}
