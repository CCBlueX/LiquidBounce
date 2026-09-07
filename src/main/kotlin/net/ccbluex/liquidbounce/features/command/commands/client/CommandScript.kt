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
package net.ccbluex.liquidbounce.features.command.commands.client

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.IntegerArgumentType
import net.ccbluex.liquidbounce.features.command.CommandRegistrar
import net.ccbluex.liquidbounce.features.command.arguments.BooleanArgumentType
import net.ccbluex.liquidbounce.features.command.arguments.ClientStringArgumentType
import net.ccbluex.liquidbounce.features.command.arguments.TaggedArgumentType
import net.ccbluex.liquidbounce.features.command.brigadier.ClientCommandSource
import net.ccbluex.liquidbounce.features.command.brigadier.CmdI18n
import net.ccbluex.liquidbounce.features.command.brigadier.CmdChainScope
import net.ccbluex.liquidbounce.features.command.brigadier.CmdLiteralScope
import net.ccbluex.liquidbounce.features.command.brigadier.get
import net.ccbluex.liquidbounce.features.command.brigadier.register
import net.ccbluex.liquidbounce.features.command.brigadier.suggestions
import net.ccbluex.liquidbounce.script.DebugProtocol
import net.ccbluex.liquidbounce.script.ScriptDebugOptions
import net.ccbluex.liquidbounce.script.ScriptManager
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.clickablePath
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.variable
import net.minecraft.util.Util
import java.io.File

@Suppress("detekt:TooManyFunctions")
object CommandScript : CommandRegistrar {
    @Suppress("detekt:LongMethod")
    override fun register(dispatcher: CommandDispatcher<ClientCommandSource>) {
        dispatcher.register("script") {
            literal("reload") {
                exec {
                    reloadScripts()
                }
            }
            literal("load") {
                scriptNameArgument { name ->
                    exec { ctx ->
                        loadScript(ctx.get(name))
                    }
                }
            }
            literal("unload") {
                loadedScriptNameArgument { name ->
                    exec { ctx ->
                        unloadScript(ctx.get(name))
                    }
                }
            }
            literal("debug") {
                scriptNameArgument { name ->
                    optional("protocol", TaggedArgumentType<DebugProtocol>("protocol")) { protocol ->
                        optional("suspendOnStart", BooleanArgumentType("suspendOnStart")) { suspendOnStart ->
                            optional("inspectInternals", BooleanArgumentType("inspectInternals")) { inspectInternals ->
                                optional("port", IntegerArgumentType.integer(1, 65535)) { port ->
                                    exec { ctx ->
                                        debugScript(
                                            ctx.get(name),
                                            ctx.get(protocol),
                                            ctx.get(suspendOnStart),
                                            ctx.get(inspectInternals),
                                            ctx.get(port),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            literal("list") {
                exec {
                    listScripts()
                }
            }
            literal("browse") {
                exec {
                    browseScripts()
                }
            }
            literal("edit") {
                scriptNameArgument { name ->
                    exec { ctx ->
                        editScript(ctx.get(name))
                    }
                }
            }
        }
    }

    private fun CmdLiteralScope.scriptNameArgument(block: CmdChainScope.ArgContinuation<String>) =
        argument(
            "name",
            ClientStringArgumentType.word(),
            suggestions(Util.nonCriticalIoPool()) {
                ScriptManager.root.listFiles()?.map { it.name }
            },
            block,
        )

    private fun CmdLiteralScope.loadedScriptNameArgument(block: CmdChainScope.ArgContinuation<String>) =
        argument(
            "name",
            ClientStringArgumentType.word(),
            suggestions(strings = { ScriptManager.scripts.map { it.scriptName } }),
            block,
        )

    private fun CmdI18n.editScript(name: String): Int {
        val scriptFile = ScriptManager.root.resolve(name)

        if (!scriptFile.exists()) {
            chat(regular(t("edit.notFound", variable(name))))
            return 1
        }

        Util.getPlatform().openFile(scriptFile)
        chat(regular(t("edit.opened", variable(name))))
        return 1
    }

    private fun CmdI18n.browseScripts(): Int {
        Util.getPlatform().openFile(ScriptManager.root)
        chat(regular(t("browse.browse", clickablePath(ScriptManager.root))))
        return 1
    }

    private fun CmdI18n.listScripts(): Int {
        val scripts = ScriptManager.scripts
        val scriptNames = scripts.map { script -> "${script.scriptName} (${script.language})" }

        if (scriptNames.isEmpty()) {
            chat(regular(t("list.noScripts")))
            return 1
        }

        chat(regular(t("list.scripts", variable(scriptNames.joinToString(", ")))))
        return 1
    }

    private fun CmdI18n.debugScript(
        name: String,
        protocol: DebugProtocol?,
        suspendOnStart: Boolean?,
        inspectInternals: Boolean?,
        port: Int?,
    ): Int {
        val scriptFile = ScriptManager.root.resolve(name)

        if (!scriptFile.exists()) {
            chat(regular(t("debug.notFound", variable(name))))
            return 1
        }

        unloadIfLoaded(scriptFile, name)
        loadScriptWithDebug(scriptFile, name, protocol, suspendOnStart, inspectInternals, port)
        return 1
    }

    private fun CmdI18n.loadScriptWithDebug(
        scriptFile: File,
        name: String,
        protocol: DebugProtocol?,
        suspendOnStart: Boolean?,
        inspectInternals: Boolean?,
        port: Int?,
    ) {
        val effectiveProtocol = protocol ?: DebugProtocol.INSPECT

        runCatching {
            ScriptManager.loadScript(
                scriptFile, debugOptions = ScriptDebugOptions(
                    enabled = true,
                    protocol = effectiveProtocol,
                    suspendOnStart = suspendOnStart == true,
                    inspectInternals = inspectInternals == true,
                    port = port
                        ?: if (effectiveProtocol == DebugProtocol.INSPECT) 4242 else 4711,
                )
            ).enable()
        }.onSuccess {
            chat(regular(t("debug.loaded", variable(name))))
        }.onFailure {
            chat(regular(t("debug.failedToLoad", variable(it.message ?: "unknown"))))
        }
    }

    private fun CmdI18n.unloadIfLoaded(
        scriptFile: File,
        name: String,
    ) {
        ScriptManager.scripts.find { it.file == scriptFile }?.also { script ->
            chat(regular(t("debug.alreadyLoaded", variable(name))))

            runCatching {
                ScriptManager.unloadScript(script)
            }.onSuccess {
                chat(regular(t("debug.unloaded", variable(name))))
            }.onFailure {
                chat(regular(t("debug.failedToUnload", variable(it.message ?: "unknown"))))
            }
        }
    }

    private fun CmdI18n.unloadScript(name: String): Int {
        val script = ScriptManager.scripts.find { it.scriptName.equals(name, true) }

        if (script == null) {
            chat(regular(t("unload.notFound", variable(name))))
            return 1
        }

        runCatching {
            ScriptManager.unloadScript(script)
        }.onSuccess {
            chat(regular(t("unload.unloaded", variable(name))))
        }.onFailure {
            chat(regular(t("unload.failedToUnload", variable(it.message ?: "unknown"))))
        }
        return 1
    }

    private fun CmdI18n.loadScript(name: String): Int {
        val scriptFile = ScriptManager.root.resolve(name)

        if (!scriptFile.exists()) {
            chat(regular(t("load.notFound", variable(name))))
            return 1
        }

        // Check if script is already loaded
        if (ScriptManager.scripts.any { it.file == scriptFile }) {
            chat(regular(t("load.alreadyLoaded", variable(name))))
            return 1
        }

        runCatching {
            ScriptManager.loadScript(scriptFile).enable()
        }.onSuccess {
            chat(regular(t("load.loaded", variable(name))))
        }.onFailure {
            chat(regular(t("load.failedToLoad", variable(it.message ?: "unknown"))))
        }
        return 1
    }

    private fun CmdI18n.reloadScripts(): Int {
        runCatching {
            ScriptManager.reload()
        }.onSuccess {
            chat(regular(t("reload.reloaded")))
        }.onFailure {
            chat(regular(t("reload.reloadFailed", variable(it.message ?: "unknown"))))
        }
        return 1
    }

}
