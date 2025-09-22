/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
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

package net.ccbluex.liquidbounce.features.command.commands.client

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ccbluex.liquidbounce.api.core.HttpClient
import net.ccbluex.liquidbounce.api.core.HttpMethod
import net.ccbluex.liquidbounce.api.core.parse
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.command.CommandException
import net.ccbluex.liquidbounce.features.command.CommandExecutor.suspendHandler
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder
import net.ccbluex.liquidbounce.features.command.dsl.addParam
import net.ccbluex.liquidbounce.features.command.dsl.buildCommand
import net.ccbluex.liquidbounce.features.command.dsl.cast
import net.ccbluex.liquidbounce.features.command.dsl.castVararg
import net.ccbluex.liquidbounce.features.command.preset.pagedQuery
import net.ccbluex.liquidbounce.render.FontManager
import net.ccbluex.liquidbounce.utils.client.asText
import net.ccbluex.liquidbounce.utils.client.bold
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.copyable
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.variable
import net.ccbluex.liquidbounce.utils.client.withColor
import net.minecraft.util.Formatting
import okio.Buffer
import java.awt.GraphicsEnvironment
import java.io.File

/**
 * Fonts Command
 *
 * Browse and add fonts.
 *
 * TODO: 1. save added fonts
 * TODO: 2. reload glyph manager
 */
object CommandFonts : Command.Factory {

    override fun createCommand(): Command =
        CommandBuilder.begin("fonts")
            .hub()
            .subcommand(addSubcommand())
            .subcommand(listSubcommand())
            .build()

    private fun addSubcommand() = CommandBuilder.begin("add")
        .hub()
        .subcommand(addSystemSubcommand())
        .subcommand(addFileSubcommand())
        .subcommand(addUrlSubcommand())
        .build()

    private fun Command.fontAdded(fontFace: FontManager.FontFace) {
        chat("Added font: ${fontFace.name}", this)
    }

    private fun addSystemSubcommand() = buildCommand("system") {
        val name = addParam("name") {
            verifiedBy(ParameterBuilder.STRING_VALIDATOR)
                .autocompletedFrom {
                    GraphicsEnvironment.getLocalGraphicsEnvironment().availableFontFamilyNames.asList()
                }
                .required()
                .vararg()
        }

        suspendHandler {
            val fontName = name.castVararg().joinToString(" ")
            try {
                val font = FontManager.queueSystemFont(fontName)
                command.fontAdded(font)
            } catch (e: Exception) {
                logger.error("Failed to load font '${fontName}'", e)
                throw CommandException("Failed to load font '${fontName}', check log for details".asText(), e)
            }
        }
    }

    private fun addFileSubcommand() = buildCommand("file") {
        val file = addParam("file") {
            verifiedBy(ParameterBuilder.STRING_VALIDATOR)
                .required()
        }

        suspendHandler {
            var fontFile = File(file.cast())
            if (!fontFile.isAbsolute) fontFile = fontFile.relativeTo(ConfigSystem.rootFolder)
            val font = FontManager.queueFontFromFile(fontFile) ?:
                throw CommandException("Failed to load font from file '${fontFile}', check log for details".asText())
            command.fontAdded(font)
        }
    }

    private fun addUrlSubcommand() = buildCommand("url") {
        val url = addParam("url") {
            verifiedBy(ParameterBuilder.STRING_VALIDATOR)
                .required()
                .autocompletedFrom { listOf("http://", "https://") }
        }

        suspendHandler {
            val fontUrl = url.cast()
            try {
                withContext(Dispatchers.IO) {
                    HttpClient.request(
                        url = fontUrl,
                        method = HttpMethod.GET,
                    ).parse<Buffer>()
                }.use { buffer ->
                    val font = FontManager.queueFontFromStream(buffer.inputStream())
                    command.fontAdded(font)
                }
            } catch (e: Exception) {
                logger.error("Failed to load font from URL '${fontUrl}'", e)
                throw CommandException("Failed to load font from URL '${fontUrl}', check log for details".asText(), e)
            }
        }
    }

    private fun listSubcommand() = CommandBuilder
        .begin("list")
        .pagedQuery(
            pageSize = 8,
            header = {
                result("fonts").withColor(Formatting.RED).bold(true)
            },
            items = {
                FontManager.fontFaces.values
            },
            eachRow = { _, font ->
                "\u2B25 ".asText()
                    .formatted(Formatting.BLUE)
                    .append(variable(font.name).copyable())
                    .append(regular(" ("))
                    .append(variable(font.size.toString()))
                    .append(regular(")"))

                // TODO: link to file
            }
        )

}
