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
package net.ccbluex.liquidbounce.features.command.commands.module

import net.ccbluex.liquidbounce.config.types.Value
import net.ccbluex.liquidbounce.config.types.list.RegistryListValue
import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder
import net.ccbluex.liquidbounce.features.command.builder.entityType
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleClickGui
import net.ccbluex.liquidbounce.utils.client.MessageMetadata
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.variable
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.entity.EntityType
import java.util.SequencedSet

object CommandModule : Command.Factory {

    override fun createCommand() = CommandBuilder
        .begin("module")
        .hub()
        .subcommand(targetsSubcommand())
        .build()

    private fun targetsSubcommand() = CommandBuilder
        .begin("targets")
        .parameter(
            ParameterBuilder.entityType("entity")
                .required()
                .build()
        )
        .handler {
            val entityType = args[0] as EntityType<*>
            val filters = ModuleManager.mapNotNull(::findEntityTypeFilter).distinct()

            if (filters.isEmpty()) {
                chat(
                    regular(command.result("noEntityFilter")),
                    metadata = MessageMetadata(id = "CModule#targets:noFilter")
                )
                return@handler
            }

            val shouldEnable = filters.any { entityType !in getEntityTypes(it) }
            filters.forEach { setEntityTypeEnabled(it, entityType, shouldEnable) }

            ModuleClickGui.sync()

            val localizedState = if (shouldEnable) {
                command.result("enabled")
            } else {
                command.result("disabled")
            }

            chat(
                regular(
                    command.result(
                        "targetToggled",
                        variable(entityTypeName(entityType)),
                        variable(filters.size.toString()),
                        variable(localizedState)
                    )
                ),
                metadata = MessageMetadata(id = "CModule#targets:${entityTypeName(entityType)}")
            )
        }
        .build()

    private fun findEntityTypeFilter(module: ClientModule): RegistryListValue<*, *>? =
        module.collectValuesRecursively().firstOrNull {
            it is RegistryListValue<*, *>
                && it.innerValueType.name == "ENTITY_TYPE"
                && (it.name.equals("Entities", true) || it.name.equals("EntityTypes", true))
        } as RegistryListValue<*, *>?

    @Suppress("UNCHECKED_CAST")
    private fun getEntityTypes(filter: RegistryListValue<*, *>): SequencedSet<EntityType<*>> {
        val typed = filter as Value<SequencedSet<EntityType<*>>>
        return typed.get()
    }

    private fun setEntityTypeEnabled(filter: RegistryListValue<*, *>, entityType: EntityType<*>, enabled: Boolean) {
        val entities = getEntityTypes(filter)
        if (enabled) {
            entities.add(entityType)
        } else {
            entities.remove(entityType)
        }

        @Suppress("UNCHECKED_CAST")
        val typed = filter as Value<SequencedSet<EntityType<*>>>
        typed.set(entities) { }
    }

    private fun entityTypeName(entityType: EntityType<*>): String =
        BuiltInRegistries.ENTITY_TYPE.getKey(entityType).toString()

}
