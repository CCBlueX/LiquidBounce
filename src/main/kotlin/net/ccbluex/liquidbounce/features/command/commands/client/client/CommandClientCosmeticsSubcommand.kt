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
package net.ccbluex.liquidbounce.features.command.commands.client.client

import net.ccbluex.liquidbounce.api.core.httpException
import net.ccbluex.liquidbounce.features.command.brigadier.CmdLiteralScope
import net.ccbluex.liquidbounce.features.cosmetic.ClientAccountManager
import net.ccbluex.liquidbounce.features.cosmetic.CosmeticService
import net.ccbluex.liquidbounce.utils.client.browseUrl
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.markAsError
import net.ccbluex.liquidbounce.utils.client.regular

object CommandClientCosmeticsSubcommand {
    fun CmdLiteralScope.cosmetics() {
        literal("cosmetics") {
            literal("refresh") {
                execSuspend {
                    refresh()
                }
            }
            literal("manage") {
                exec {
                    browseUrl("https://user.liquidbounce.net/cosmetics")
                    1
                }
            }
        }
    }

    private suspend fun refresh() {
        chat(
            regular(
                "Refreshing cosmetics..."
            )
        )
        CosmeticService.carriersCosmetics.clear()
        ClientAccountManager.clientAccount.cosmetics = null

        CosmeticService.refreshCarriers(force = true).onSuccess {
            chat(
                regular(
                    "Cosmetics have been refreshed."
                )
            )
        }.onFailure { error ->
            val reason = error.httpException?.let { "the API answered with code ${it.code}" }
                ?: error.message
                ?: error.javaClass.simpleName
            chat(markAsError("Failed to refresh cosmetics: $reason"))
        }
    }
}
