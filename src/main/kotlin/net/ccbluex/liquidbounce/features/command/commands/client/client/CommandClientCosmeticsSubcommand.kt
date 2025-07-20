/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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

import kotlinx.coroutines.suspendCancellableCoroutine
import net.ccbluex.liquidbounce.features.command.CommandExecutor.suspendHandler
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.cosmetic.ClientAccountManager
import net.ccbluex.liquidbounce.features.cosmetic.CosmeticService
import net.ccbluex.liquidbounce.utils.client.browseUrl
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.regular
import kotlin.coroutines.resume

object CommandClientCosmeticsSubcommand {
    fun cosmeticsCommand() = CommandBuilder
        .begin("cosmetics")
        .hub()
        .subcommand(refreshSubcommand())
        .subcommand(manageSubcommand())
        .build()

    private fun manageSubcommand() = CommandBuilder.begin("manage")
        .handler { _, _ ->
            browseUrl("https://user.liquidbounce.net/cosmetics")
        }
        .build()

    private fun refreshSubcommand() = CommandBuilder.begin("refresh")
        .suspendHandler { _, _ ->
            chat(
                regular(
                    "Refreshing cosmetics..."
                )
            )
            CosmeticService.carriersCosmetics.clear()
            ClientAccountManager.clientAccount.cosmetics = null

            suspendCancellableCoroutine { continuation ->
                CosmeticService.refreshCarriers(true) {
                    chat(
                        regular(
                            "Cosmetic System has been refreshed."
                        )
                    )
                    continuation.resume(Unit)
                }
            }
        }
        .build()
}
