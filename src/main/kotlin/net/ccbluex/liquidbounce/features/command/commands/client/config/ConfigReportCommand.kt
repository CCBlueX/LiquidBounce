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
package net.ccbluex.liquidbounce.features.command.commands.client.config

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.services.marketplace.MarketplaceApi
import net.ccbluex.liquidbounce.features.command.brigadier.CmdI18n
import net.ccbluex.liquidbounce.features.command.brigadier.CmdLiteralScope
import net.ccbluex.liquidbounce.features.marketplace.autoconfig.ConfigTracker
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.variable
import net.ccbluex.liquidbounce.utils.text.dropPort

/**
 * Tells other players whether the tracked config works
 */
object ConfigReportCommand {

    fun CmdLiteralScope.report() {
        literal("report") {
            literal("works") {
                execSuspend { report(works = true) }
            }
            literal("broken") {
                execSuspend { report(works = false) }
            }
        }
    }

    private suspend fun CmdI18n.report(works: Boolean) {
        requireTracked()

        request {
            MarketplaceApi.putConfigReport(
                session(),
                ConfigTracker.itemId,
                ConfigTracker.revisionId,
                works,
                LiquidBounce.clientVersion,
                mc.currentServer?.ip?.dropPort()
            )
        }
        chat(regular(t("report.reported", variable(ConfigTracker.itemName))))
    }

}
