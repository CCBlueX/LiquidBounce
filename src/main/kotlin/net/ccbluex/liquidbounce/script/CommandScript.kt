package net.ccbluex.liquidbounce.script

import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.variable
import net.minecraft.util.Util

object CommandScript {

    fun createCommand(): Command {
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
            .subcommand(CommandBuilder.begin("directory").handler { command, _ ->
                Util.getOperatingSystem().open(ScriptManager.scriptsRoot)
                chat(regular(command.result("scriptsDirectory", variable(ScriptManager.scriptsRoot.absolutePath))))
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
