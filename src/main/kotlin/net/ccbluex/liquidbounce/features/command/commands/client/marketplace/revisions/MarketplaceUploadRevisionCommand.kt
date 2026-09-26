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
package net.ccbluex.liquidbounce.features.command.commands.client.marketplace.revisions

import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import net.ccbluex.liquidbounce.features.command.arguments.ClientStringArgumentType
import net.ccbluex.liquidbounce.api.services.marketplace.MarketplaceApi
import net.ccbluex.liquidbounce.features.command.CommandException
import net.ccbluex.liquidbounce.features.command.brigadier.CmdLiteralScope
import net.ccbluex.liquidbounce.features.command.brigadier.get
import net.ccbluex.liquidbounce.features.command.preset.accountOrException
import net.ccbluex.liquidbounce.features.cosmetic.ClientAccountManager
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.variable
import java.io.File

/**
 * Upload marketplace item revision
 */
object MarketplaceUploadRevisionCommand {

    @Suppress("LongMethod")
    fun CmdLiteralScope.upload() {
        literal("upload") {
            argument("id", IntegerArgumentType.integer(1)) { id ->
                argument("file", ClientStringArgumentType.word()) { file ->
                    argument("version", ClientStringArgumentType.word()) { version ->
                        optional("changelog", StringArgumentType.greedyString(), default = null) { changelog ->
                            execSuspend { ctx ->
                                // Omitting the changelog uploads without one.
                                this@literal.doUpload(
                                    ctx.get(id),
                                    ctx.get(file),
                                    ctx.get(version),
                                    ctx.get(changelog) ?: "",
                                    null
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private suspend fun CmdLiteralScope.doUpload(
        id: Int,
        filePath: String,
        version: String,
        changelog: String,
        dependencies: String?,
    ) {
        val clientAccount = ClientAccountManager.accountOrException()

        val file = File(filePath)
        if (!file.exists()) {
            throw CommandException(t("error.fileNotFound", filePath))
        }

        try {
            MarketplaceApi.createMarketplaceItemRevision(
                clientAccount.takeSession(),
                id,
                file,
                version,
                changelog,
                dependencies
            )

            chat(
                regular(
                    t("revisions.upload.success",
                        variable(version),
                        variable(id.toString())
                    )
                )
            )
        } catch (@Suppress("SwallowedException") e: Exception) {
            logger.error("Failed to upload marketplace item revision", e)

            throw CommandException(t("error.updateFailed",
                id.toString(),
                e.message ?: "Unknown error"
            ))
        }
    }

}
