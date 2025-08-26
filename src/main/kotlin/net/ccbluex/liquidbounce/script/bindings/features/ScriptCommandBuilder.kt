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
 *
 *
 */

package net.ccbluex.liquidbounce.script.bindings.features

import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.command.Parameter
import net.ccbluex.liquidbounce.features.command.ParameterValidationResult
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder.Companion.STRING_VALIDATOR
import net.ccbluex.liquidbounce.script.asArray
import org.graalvm.polyglot.Value

class ScriptCommandBuilder(private val commandObject: Value) {

    private fun createCommand(commandObject: Value): Command {
        val aliases = if (commandObject.hasMember("aliases")) {
            commandObject.getMember("aliases").asArray<String>()
        } else {
            emptyArray()
        }

        val commandBuilder = CommandBuilder
            .begin(commandObject.getMember("name").asString())
            .alias(aliases = aliases)

        if (commandObject.hasMember("subcommands")) {
            val subcommands = commandObject.getMember("subcommands").asArray<Value>()

            for (subcommand in subcommands) {
                commandBuilder.subcommand(createCommand(subcommand))
            }
        }

        if (commandObject.hasMember("parameters")) {
            val parameters = commandObject.getMember("parameters").asArray<Value>()

            for (parameter in parameters) {
                commandBuilder.parameter(createParameter(parameter))
            }
        }

        if (commandObject.hasMember("onExecute")) {
            val handler = commandObject.getMember("onExecute")

            @Suppress("SpreadOperator")
            commandBuilder.handler { _, args ->
                handler.execute(*args)
            }
        }

        if (commandObject.hasMember("hub") && commandObject.getMember("hub").asBoolean()) {
            commandBuilder.hub()
        }

        return commandBuilder.build()
    }

    private fun createParameter(parameterObject: Value): Parameter<*> {
        val parameterBuilder =
            ParameterBuilder.begin<String>(parameterObject.getMember("name").asString())

        if (parameterObject.hasMember("required") && parameterObject.getMember("required").asBoolean()) {
            parameterBuilder.required()
        } else {
            parameterBuilder.optional()
        }

        if (parameterObject.hasMember("vararg") && parameterObject.getMember("vararg").asBoolean()) {
            parameterBuilder.vararg()
        }

        if (parameterObject.hasMember("getCompletions")) {
            val completions = parameterObject.getMember("getCompletions")

            parameterBuilder.autocompletedWith { begin, args ->
                completions.execute(begin, args).asArray<String>().asList()
            }
        }

        if (parameterObject.hasMember("validate")) {
            val validator = parameterObject.getMember("validate")

            parameterBuilder.verifiedBy { param ->
                val result = validator.execute(param)

                if (result.getMember("accept").asBoolean()) {
                    ParameterValidationResult.ok(toObject(result.getMember("value")))
                } else {
                    ParameterValidationResult.error(result.getMember("error").asString())
                }
            }
        } else {
            parameterBuilder.verifiedBy(STRING_VALIDATOR)
        }


        return parameterBuilder.build()
    }

    private fun <T> toObject(v: Value): T {
        return if (v.isHostObject) {
            v.asHostObject()
        } else {
            v as T
        }
    }

    fun build(): Command {
        return createCommand(commandObject)
    }

}
