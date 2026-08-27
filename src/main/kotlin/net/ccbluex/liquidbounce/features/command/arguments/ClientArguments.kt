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
package net.ccbluex.liquidbounce.features.command.arguments

import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.CommandSyntaxException
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import net.ccbluex.liquidbounce.features.command.CommandException
import net.ccbluex.liquidbounce.features.command.brigadier.ClientCommandSource
import net.ccbluex.liquidbounce.lang.translation
import net.minecraft.commands.CommandBuildContext
import net.minecraft.commands.Commands
import net.minecraft.commands.SharedSuggestionProvider
import net.minecraft.commands.arguments.ResourceArgument
import net.minecraft.commands.arguments.coordinates.Coordinates
import net.minecraft.commands.arguments.coordinates.LocalCoordinates
import net.minecraft.commands.arguments.coordinates.WorldCoordinates
import net.minecraft.commands.arguments.item.ItemArgument
import net.minecraft.core.Holder
import net.minecraft.core.Registry
import net.minecraft.resources.ResourceKey
import net.minecraft.world.phys.Vec3
import java.util.concurrent.CompletableFuture

/**
 * The build context shared by the registry-backed argument factories ([itemArgument],
 * [resourceArgument]): the joined world's registry access plus the connection's feature
 * flags, falling back to static vanilla values when no server connection exists (main
 * menu, unit tests). Rebuilt on every command-tree rebuild, which [CommandManager]
 * triggers on world changes.
 */
private fun clientCommandBuildContext(): CommandBuildContext =
    CommandBuildContext.simple(ClientCommandSource.commandBuildContext(), ClientCommandSource.enabledFeatures())

/**
 * Creates the vanilla [ItemArgument] bound to the current world's registry access,
 * accepting exactly what `/give` does (`id`, `minecraft:id`, `[id|components]`).
 *
 * Falls back to the static vanilla registry lookup and default feature flags when no
 * world is loaded.
 */
fun itemArgument(): ItemArgument = ItemArgument.item(clientCommandBuildContext())

/**
 * Creates the vanilla [ResourceArgument] for the registry identified by [key],
 * parsing to a type-safe [Holder.Reference] with vanilla errors and suggestions.
 */
fun <T : Any> resourceArgument(
    key: ResourceKey<out Registry<T>>,
): ResourceArgument<T> = ResourceArgument.resource(clientCommandBuildContext(), key)

/**
 * Client shell over the vanilla coordinate parsing ([WorldCoordinates.parseDouble] /
 * [LocalCoordinates.parse]). Only position resolution differs from vanilla: the vanilla
 * getters require a server-side [net.minecraft.commands.CommandSourceStack], so
 * [getPosition] resolves against the local player instead.
 */
class Vec3ArgumentType(
    private val centerCorrect: Boolean = true,
) : ArgumentType<Coordinates> {

    @Throws(CommandSyntaxException::class)
    override fun parse(reader: StringReader): Coordinates =
        if (reader.canRead() && reader.peek() == '^') {
            LocalCoordinates.parse(reader)
        } else {
            WorldCoordinates.parseDouble(reader, centerCorrect)
        }

    override fun <S> listSuggestions(
        context: CommandContext<S>,
        builder: SuggestionsBuilder,
    ): CompletableFuture<Suggestions> {
        val source = context.source as? SharedSuggestionProvider
        // Vanilla Vec3Argument delegates to SharedSuggestionProvider.getRelevantCoordinates /
        // getAbsoluteCoordinates when the source implements it (ours does); the fallback list
        // mirrors the vanilla default for foreign sources.
        val coordinates = source?.relevantCoordinates
            ?: listOf(
                SharedSuggestionProvider.TextCoordinates.DEFAULT_LOCAL,
                SharedSuggestionProvider.TextCoordinates.DEFAULT_GLOBAL,
            )
        return SharedSuggestionProvider.suggestCoordinates(
            builder.remaining,
            coordinates,
            builder,
            Commands.createValidator { value -> parse(StringReader(value)) },
        )
    }

    override fun getExamples(): Collection<String> = EXAMPLES

    companion object {
        private val EXAMPLES = listOf("0 0 0", "~ ~ ~", "^ ^ ^", "^1 ^ ^-5", "0.1 -0.5 .9", "~0.5 ~1 ~-5")

        /**
         * Reads the resolved position for the parsed [name] argument. Relative (`~`)
         * coordinates resolve against the player, local (`^`) against the look direction.
         *
         * Defaults to the vanilla `Vec3Argument.vec3()` behavior (`centerCorrect = true`),
         * which centers integer block positions on x/z.
         */
        fun getPosition(context: CommandContext<ClientCommandSource>, name: String): Vec3 {
            val coordinates = context.getArgument(name, Coordinates::class.java)
            val player = ClientCommandSource.playerOrNull
                ?: throw CommandException(translation("liquidbounce.commandManager.notIngame"))
            return when (coordinates) {
                is WorldCoordinates -> {
                    val origin = player.position()
                    Vec3(coordinates.x().get(origin.x), coordinates.y().get(origin.y), coordinates.z().get(origin.z))
                }
                is LocalCoordinates -> Vec3.applyLocalCoordinatesToRotation(
                    player.rotationVector,
                    Vec3(coordinates.left(), coordinates.up(), coordinates.forwards()),
                ).add(player.position())
                else -> error("Unexpected Coordinates implementation: ${coordinates::class}")
            }
        }
    }
}
