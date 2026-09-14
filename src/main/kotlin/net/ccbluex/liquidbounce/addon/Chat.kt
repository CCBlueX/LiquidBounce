/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package net.ccbluex.liquidbounce.addon

import net.ccbluex.liquidbounce.event.events.NotificationEvent
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.markAsError as errorText
import net.ccbluex.liquidbounce.utils.client.notification
import net.ccbluex.liquidbounce.utils.client.regular as infoText
import net.ccbluex.liquidbounce.utils.client.warning as warningText
import net.minecraft.network.chat.Component

/**
 * Client-side messages, shown with the client's prefix and never sent to the server.
 */
object Chat {

    @JvmStatic
    fun info(message: String): Unit = chat(infoText(message))

    @JvmStatic
    fun warning(message: String): Unit = chat(warningText(message))

    @JvmStatic
    fun error(message: String): Unit = chat(errorText(message))

    @JvmStatic
    fun print(message: Component): Unit = chat(message)

    @JvmStatic
    fun notify(title: String, message: String, severity: Severity) {
        notification(title, message, severity.event)
    }

    enum class Severity(internal val event: NotificationEvent.Severity) {
        INFO(NotificationEvent.Severity.INFO),
        SUCCESS(NotificationEvent.Severity.SUCCESS),
        ERROR(NotificationEvent.Severity.ERROR),
    }

}
