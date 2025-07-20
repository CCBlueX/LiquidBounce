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

import net.ccbluex.liquidbounce.features.command.AutoCompletionProvider
import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.command.CommandFactory
import net.ccbluex.liquidbounce.features.command.ParameterValidationResult
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder
import net.ccbluex.liquidbounce.script.DebugProtocol
import net.ccbluex.liquidbounce.script.ScriptDebugOptions
import net.ccbluex.liquidbounce.script.ScriptManager
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.clickablePath
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.variable
import net.minecraft.util.Util
import java.io.File

object CommandScript : CommandFactory {

    private val listScriptFiles = AutoCompletionProvider { prefix: String, _ ->
        ScriptManager.scripts.filter { it.file.name.startsWith(prefix) }
            .map { it.file.name.removeSuffix(".js") }
    }

    override fun createCommand(): Command {
        return CommandBuilder.begin("script")
            .hub()
            .subcommand(reloadSubcommand())
            .subcommand(loadSubcommand())
            .subcommand(unloadSubcommand())
            .subcommand(debugSubcommand())
            .subcommand(listSubcommand())
            .subcommand(browseSubcommand())
            .subcommand(editSubcommand())
            .build()
    }

    private fun editSubcommand() = CommandBuilder.begin("edit").parameter(
        ParameterBuilder.begin<String>("name").verifiedBy(ParameterBuilder.STRING_VALIDATOR).required()
            .autocompletedWith(listScriptFiles)
            .build()
    ).handler { command, args ->
        val name = args[0] as String
        val scriptFile = ScriptManager.root.resolve("$name.js")

        if (!scriptFile.exists()) {
            chat(regular(command.result("notFound", variable(name))))
            return@handler
        }

        Util.getOperatingSystem().open(scriptFile)
        chat(regular(command.result("opened", variable(name))))
    }.build()

    private fun browseSubcommand() = CommandBuilder.begin("browse").handler { command, _ ->
        Util.getOperatingSystem().open(ScriptManager.root)
        chat(regular(command.result("browse", clickablePath(ScriptManager.root))))
    }.build()

    private fun listSubcommand() = CommandBuilder.begin("list").handler { command, _ ->
        val scripts = ScriptManager.scripts
        val scriptNames = scripts.map { script -> "${script.scriptName} (${script.language})" }

        if (scriptNames.isEmpty()) {
            chat(regular(command.result("noScripts")))
            return@handler
        }

        chat(regular(command.result("scripts", variable(scriptNames.joinToString(", ")))))
    }.build()

    private fun debugSubcommand() = CommandBuilder.begin("debug")
        .parameter(
            ParameterBuilder.begin<String>("name")
                .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
                .required()
                .autocompletedWith(listScriptFiles)
                .build()

        )
        .parameter(
            ParameterBuilder.begin<String>("protocol")
                .verifiedBy {
                    try {
                        ParameterValidationResult.ok(DebugProtocol.valueOf(it).name)
                    } catch (_: IllegalArgumentException) {
                        ParameterValidationResult.error("")
                    }
                }
                .optional()
                .autocompletedWith { prefix, _ ->
                    DebugProtocol.entries.map { it.toString() }.filter { it.startsWith(prefix) }
                }
                .build()
        )
        .parameter(
            ParameterBuilder.begin<Boolean>("suspendOnStart")
                .verifiedBy(ParameterBuilder.BOOLEAN_VALIDATOR)
                .optional()
                .build()
        )
        .parameter(
            ParameterBuilder.begin<Boolean>("inspectInternals")
                .verifiedBy(ParameterBuilder.BOOLEAN_VALIDATOR)
                .optional()
                .build()
        )
        .parameter(
            ParameterBuilder.begin<Int>("port")
                .verifiedBy(ParameterBuilder.INTEGER_VALIDATOR)
                .optional()
                .build()
        )
        .handler { command, args ->
            val name = args[0] as String
            val scriptFile = ScriptManager.root.resolve("$name.js")

            if (!scriptFile.exists()) {
                chat(regular(command.result("notFound", variable(name))))
                return@handler
            }

            unloadIfLoaded(scriptFile, command, name)
            loadScriptWithDebug(args, scriptFile, command, name)
        }
        .build()

    private fun loadScriptWithDebug(
        args: Array<Any>,
        scriptFile: File,
        command: Command,
        name: String
    ) {
        val protocol =
            args.getOrNull(1)?.let { DebugProtocol.valueOf((it as String).uppercase()) } ?: DebugProtocol.INSPECT

        runCatching {
            ScriptManager.loadScript(
                scriptFile, debugOptions = ScriptDebugOptions(
                    enabled = true,
                    protocol = protocol,
                    suspendOnStart = args.getOrNull(2) as Boolean? == true,
                    inspectInternals = args.getOrNull(3) as Boolean? == true,
                    port = args.getOrNull(4) as Int?
                        ?: if (protocol == DebugProtocol.INSPECT) 4242 else 4711,
                )
            ).enable()
        }.onSuccess {
            chat(regular(command.result("loaded", variable(name))))
        }.onFailure {
            chat(regular(command.result("failedToLoad", variable(it.message ?: "unknown"))))
        }
    }

    private fun unloadIfLoaded(
        scriptFile: File,
        command: Command,
        name: String
    ) {
        ScriptManager.scripts.find { it.file == scriptFile }?.also { script ->
            chat(regular(command.result("alreadyLoaded", variable(name))))

            runCatching {
                ScriptManager.unloadScript(script)
            }.onSuccess {
                chat(regular(command.result("unloaded", variable(name))))
            }.onFailure {
                chat(regular(command.result("failedToUnload", variable(it.message ?: "unknown"))))
            }
        }
    }

    private fun unloadSubcommand() = CommandBuilder.begin("unload").parameter(
        ParameterBuilder.begin<String>("name").verifiedBy(ParameterBuilder.STRING_VALIDATOR).required()
            .autocompletedWith { prefix, _ ->
                ScriptManager.scripts.filter { it.scriptName.startsWith(prefix) }.map { it.scriptName }
            }
            .build()
    ).handler { command, args ->
        val name = args[0] as String

        val script = ScriptManager.scripts.find { it.scriptName.equals(name, true) }

        if (script == null) {
            chat(regular(command.result("notFound", variable(name))))
            return@handler
        }

        runCatching {
            ScriptManager.unloadScript(script)
        }.onSuccess {
            chat(regular(command.result("unloaded", variable(name))))
        }.onFailure {
            chat(regular(command.result("failedToUnload", variable(it.message ?: "unknown"))))
        }
    }.build()

    private fun loadSubcommand() = CommandBuilder.begin("load").parameter(
        ParameterBuilder.begin<String>("name").verifiedBy(ParameterBuilder.STRING_VALIDATOR).required()
            .autocompletedWith(listScriptFiles)
            .build()
    ).handler { command, args ->
        val name = args[0] as String
        val scriptFile = ScriptManager.root.resolve("$name.js")

        if (!scriptFile.exists()) {
            chat(regular(command.result("notFound", variable(name))))
            return@handler
        }

        // Check if script is already loaded
        if (ScriptManager.scripts.any { it.file == scriptFile }) {
            chat(regular(command.result("alreadyLoaded", variable(name))))
            return@handler
        }

        runCatching {
            ScriptManager.loadScript(scriptFile).enable()
        }.onSuccess {
            chat(regular(command.result("loaded", variable(name))))
        }.onFailure {
            chat(regular(command.result("failedToLoad", variable(it.message ?: "unknown"))))
        }

    }.build()

    private fun reloadSubcommand() = CommandBuilder.begin("reload").handler { command, _ ->
        runCatching {
            ScriptManager.reload()
        }.onSuccess {
            chat(regular(command.result("reloaded")))
        }.onFailure {
            chat(regular(command.result("reloadFailed", variable(it.message ?: "unknown"))))
        }
    }.build()

}
