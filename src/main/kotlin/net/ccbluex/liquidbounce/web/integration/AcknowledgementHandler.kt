/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2024 CCBlueX
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
 *
 *
 */

package net.ccbluex.liquidbounce.web.integration

import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.mcef.MCEFDownloaderMenu
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.web.integration.IntegrationHandler.acknowledgement
import net.ccbluex.liquidbounce.web.integration.IntegrationHandler.browserIsReady
import net.ccbluex.liquidbounce.web.integration.IntegrationHandler.updateIntegrationBrowser

object AcknowledgementHandler : Listenable {

    val desyncCheck = handler<GameTickEvent> {
        if (browserIsReady && mc.currentScreen !is MCEFDownloaderMenu && acknowledgement.isDesynced) {
            chat("UI desync detected, it should now be fixed!")
            acknowledgement.since.reset()
            updateIntegrationBrowser()
        }
    }

}
