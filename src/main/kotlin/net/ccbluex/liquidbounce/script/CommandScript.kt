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
package net.ccbluex.liquidbounce.script

import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.command.CommandFactory
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder
import net.ccbluex.liquidbounce.register.IncludeCommand
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.variable
import net.minecraft.util.Util

@IncludeCommand
object CommandScript : CommandFactory {

    override fun createCommand(): Command {
        return CommandBuilder.begin("script")
            .hub()
            .subcommand(CommandBuilder.begin("reload").handler { command, _ ->
                runCatching {
                    ScriptManager.reloadScripts()
                }.onSuccess {
                    chat(regular(command.result("reloaded")))
                }.onFailure {
                    chat(regular(command.result("reloadFailed", variable(it.message ?: "unknown"))))
                }
            }.build())
            .subcommand(CommandBuilder.begin("load").parameter(
                ParameterBuilder.begin<String>("name").verifiedBy(ParameterBuilder.STRING_VALIDATOR).required()
                    .build()
            ).handler { command, args ->
                val name = args[0] as String
                val scriptFile = ScriptManager.scriptsRoot.resolve("$name.js")

                if (!scriptFile.exists()) {
                    chat(regular(command.result("notFound", variable(name))))
                    return@handler
                }

                // Check if script is already loaded
                if (ScriptManager.loadedScripts.any { it.scriptFile == scriptFile }) {
                    chat(regular(command.result("alreadyLoaded", variable(name))))
                    return@handler
                }

                runCatching {
                    ScriptManager.loadScript(scriptFile)
                }.onSuccess {
                    chat(regular(command.result("loaded", variable(name))))
                }.onFailure {
                    chat(regular(command.result("failedToLoad", variable(it.message ?: "unknown"))))
                }

            }.build())
            .subcommand(CommandBuilder.begin("unload").parameter(
                ParameterBuilder.begin<String>("name").verifiedBy(ParameterBuilder.STRING_VALIDATOR).required()
                    .build()
            ).handler { command, args ->
                val name = args[0] as String

                val script = ScriptManager.loadedScripts.find { it.scriptName.equals(name, true) }

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
            }.build())
            .subcommand(CommandBuilder.begin("list").handler { command, _ ->
                val scripts = ScriptManager.loadedScripts
                val scriptNames = scripts.map { it.scriptName }

                if (scriptNames.isEmpty()) {
                    chat(regular(command.result("noScripts")))
                    return@handler
                }

                chat(regular(command.result("scripts", variable(scriptNames.joinToString(", ")))))
            }.build())
            .subcommand(CommandBuilder.begin("browse").handler { command, _ ->
                Util.getOperatingSystem().open(ScriptManager.scriptsRoot)
                chat(regular(command.result("browse", variable(ScriptManager.scriptsRoot.absolutePath))))
            }.build())
            .subcommand(CommandBuilder.begin("edit").parameter(
                ParameterBuilder.begin<String>("name").verifiedBy(ParameterBuilder.STRING_VALIDATOR).required()
                    .build()
            ).handler { command, args ->
                val name = args[0] as String
                val scriptFile = ScriptManager.scriptsRoot.resolve("$name.js")

                if (!scriptFile.exists()) {
                    chat(regular(command.result("notFound", variable(name))))
                    return@handler
                }

                Util.getOperatingSystem().open(scriptFile)
                chat(regular(command.result("opened", variable(name))))
            }.build())
            .build()
    }

}
