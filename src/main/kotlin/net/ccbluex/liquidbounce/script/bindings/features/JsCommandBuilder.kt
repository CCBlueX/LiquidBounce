/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2024 CCBlueX
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
 *
 *
 */

package net.ccbluex.liquidbounce.script.bindings.features

import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.command.Parameter
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder.Companion.STRING_VALIDATOR
import java.util.function.Function

/**
 * Command.createCommand({
 *     name: "localconfig",
 *     aliases: ["lc"],
 *     hub: true,
 *     subcommands: [
 *         Command.createCommand({
 *             name: "load",
 *             parameters: [
 *                 Command.createParameter({
 *                     name: "name",
 *                     required: true,
 *                     verifiedBy(v) {
 *
 *                     },
 *                     autoCompletedWith(v) {
 *
 *                     }
 *                 })
 *             ]
 *         })
 *     ],
 *     onExecute(args) {
 *
 *     }
 * })
 */
class JsCommandBuilder(private val commandObject: Map<String, Any>) {

    private fun createCommand(commandObject: Map<String, Any>): Command {
        val commandBuilder = CommandBuilder
            .begin(commandObject["name"] as String)
            .alias(*(commandObject["aliases"] as? Array<String>) ?: emptyArray())

        if (commandObject.containsKey("subcommands")) {
            val subcommands = commandObject["subcommands"] as List<Map<String, Any>>

            for (subcommand in subcommands) {
                commandBuilder.subcommand(createCommand(subcommand))
            }
        }

        if (commandObject.containsKey("parameters")) {
            val parameters = commandObject["parameters"] as List<Map<String, Any>>

            for (parameter in parameters) {
                commandBuilder.parameter(createParameter(parameter))
            }
        }

        if (commandObject.containsKey("onExecute")) {
            val handler = commandObject["onExecute"] as Function<Array<Any>, Unit>

            commandBuilder.handler { _, args ->
                println()
                for (arg in args) {
                    println(arg)
                }
                handler.apply(args)
            }
        }

        if (commandObject.containsKey("hub") && commandObject["hub"] as Boolean) {
            commandBuilder.hub()
        }

        return commandBuilder.build()
    }

    /**
     *  Command.createParameter({
     *      name: "name",
     *      required: true,
     *      verifiedBy(v) { },
     *      autoCompletedWith(v) { }
     *  })
     */
    private fun createParameter(parameterObject: Map<String, Any>): Parameter<*> {
        val parameterBuilder =
            ParameterBuilder.begin<String>(parameterObject["name"] as String)

        if (parameterObject.containsKey("required") && parameterObject["required"] as Boolean) {
            parameterBuilder.required()
        } else {
            parameterBuilder.optional()
        }

        if (parameterObject.containsKey("vararg") && parameterObject["vararg"] as Boolean) {
            parameterBuilder.vararg()
        }

        // todo: add support for custom verifiedBy and autoCompletedWith
        parameterBuilder.verifiedBy(STRING_VALIDATOR)
        parameterBuilder.useMinecraftAutoCompletion()

        return parameterBuilder.build()
    }

    fun build(): Command {
        return createCommand(commandObject)
    }

}
