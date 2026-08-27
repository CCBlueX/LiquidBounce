/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, either version 3 of
 * the License, or (at your option) any later version.
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
import net.ccbluex.fastutil.toEnumSet
import net.ccbluex.liquidbounce.features.command.CommandRegistrar
import net.ccbluex.liquidbounce.features.command.arguments.ClientStringArgumentType
import net.ccbluex.liquidbounce.features.command.arguments.ModuleArgumentType
import net.ccbluex.liquidbounce.features.command.arguments.MultiTaggedArgumentType
import net.ccbluex.liquidbounce.features.command.arguments.TaggedArgumentType
import net.ccbluex.liquidbounce.features.command.brigadier.ClientCommandSource
import net.ccbluex.liquidbounce.features.command.brigadier.CmdI18n
import net.ccbluex.liquidbounce.features.command.brigadier.get
import net.ccbluex.liquidbounce.features.command.brigadier.register
import net.ccbluex.liquidbounce.features.command.brigadier.suggestions
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleClickGui
import net.ccbluex.liquidbounce.utils.client.MessageMetadata
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.variable
import net.ccbluex.liquidbounce.utils.input.InputBind
import net.ccbluex.liquidbounce.utils.input.availableInputKeys
import net.ccbluex.liquidbounce.utils.input.bind
import net.ccbluex.liquidbounce.utils.input.inputByName
import net.ccbluex.liquidbounce.utils.input.renderText
import net.ccbluex.liquidbounce.utils.input.unbind

/**
 * Bind Command
 *
 * Allows you to bind a key to a module, which means that the module will be activated when the key is pressed.
 */
object CommandBind : CommandRegistrar {
    override fun register(dispatcher: CommandDispatcher<ClientCommandSource>) {
        dispatcher.register("bind") {
            argument("module", ModuleArgumentType("module")) { module ->
                argument("key", ClientStringArgumentType.word(), suggestions(availableInputKeys)) { key ->
                    optional(
                        "action",
                        TaggedArgumentType<InputBind.BindAction>("action"),
                        default = null,
                    ) { action ->
                        optional(
                            "modifiers",
                            MultiTaggedArgumentType("modifiers", InputBind.Modifier.entries, InputBind.Modifier::tag),
                            default = null,
                        ) { modifiers ->
                            exec { ctx ->
                                bind(
                                    ctx.get(module),
                                    ctx.get(key),
                                    ctx.get(action),
                                    ctx.get(modifiers),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun CmdI18n.bind(
        module: ClientModule,
        keyName: String,
        action: InputBind.BindAction?,
        modifiers: List<InputBind.Modifier>?,
    ): Int {
        val resolvedAction = action ?: module.bindValue.get().action
        val resolvedModifiers = modifiers?.toEnumSet() ?: module.bindValue.get().modifiers

        if (keyName.equals("none", true)) {
            module.bindValue.unbind()
            ModuleClickGui.sync()
            chat(
                regular(t("moduleUnbound", variable(module.name))),
                metadata = MessageMetadata(id = "Bind#${module.name}")
            )
            return 1
        }

        runCatching {
            module.bindValue.bind(inputByName(keyName), resolvedAction, resolvedModifiers)
            ModuleClickGui.sync()
        }.onSuccess {
            chat(
                regular(
                    t("moduleBound",
                        variable(module.name),
                        module.bind.renderText()
                    )
                ),
                metadata = MessageMetadata(id = "Bind#${module.name}")
            )
        }.onFailure {
            chat(
                regular(t("keyNotFound", variable(keyName))),
                metadata = MessageMetadata(id = "Bind#${module.name}")
            )
        }

        return 1
    }

}
