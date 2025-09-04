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
package net.ccbluex.liquidbounce.features.command.commands.client.marketplace.revisions

import net.ccbluex.liquidbounce.api.services.marketplace.MarketplaceApi
import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.command.CommandException
import net.ccbluex.liquidbounce.features.command.CommandExecutor.suspendHandler
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder
import net.ccbluex.liquidbounce.features.command.preset.accountOrException
import net.ccbluex.liquidbounce.features.cosmetic.ClientAccountManager
import net.ccbluex.liquidbounce.lang.translation
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.variable
import java.io.File

/**
 * Upload marketplace item revision
 */
object MarketplaceUploadRevisionCommand : Command.Factory {

    @Suppress("LongMethod")
    override fun createCommand() = CommandBuilder
        .begin("upload")
        .parameter(
            ParameterBuilder
                .begin<Int>("id")
                .verifiedBy(ParameterBuilder.INTEGER_VALIDATOR)
                .required()
                .build()
        )
        .parameter(
            ParameterBuilder
                .begin<String>("file")
                .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
                .required()
                .build()
        )
        .parameter(
            ParameterBuilder
                .begin<String>("version")
                .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
                .required()
                .build()
        )
        .parameter(
            ParameterBuilder
                .begin<String>("changelog")
                .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
                .vararg()
                .optional()
                .build()
        )
        .parameter(
            ParameterBuilder
                .begin<String>("dependencies")
                .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
                .optional()
                .build()
        )
        .suspendHandler {
            val clientAccount = ClientAccountManager.accountOrException()

            val id = args[0] as Int
            val filePath = args[1] as String
            val version = args[2] as String
            val changelog = (args.getOrNull(3) as? Array<*>)?.joinToString(" ")
            val dependencies = args.getOrNull(4) as? String

            val file = File(filePath)
            if (!file.exists()) {
                throw CommandException(translation("liquidbounce.command.marketplace.error.fileNotFound", filePath))
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
                        command.result(
                            "success",
                            variable(version),
                            variable(id.toString())
                        )
                    )
                )
            } catch (@Suppress("SwallowedException") e: Exception) {
                logger.error("Failed to upload marketplace item revision", e)

                throw CommandException(translation(
                    "liquidbounce.command.marketplace.error.updateFailed",
                    id.toString(),
                    e.message ?: "Unknown error"
                ))
            }
        }
        .build()
}
