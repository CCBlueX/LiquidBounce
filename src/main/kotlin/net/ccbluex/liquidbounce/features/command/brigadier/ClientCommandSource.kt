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
package net.ccbluex.liquidbounce.features.command.brigadier

import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import net.ccbluex.liquidbounce.features.module.MinecraftShortcuts
import net.ccbluex.liquidbounce.utils.client.NullableBypass.mc
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.player.LocalPlayer
import net.minecraft.commands.SharedSuggestionProvider
import net.minecraft.core.HolderLookup
import net.minecraft.core.Registry
import net.minecraft.core.RegistryAccess
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.data.registries.VanillaRegistries
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.server.permissions.PermissionSet
import net.minecraft.world.flag.FeatureFlagSet
import net.minecraft.world.flag.FeatureFlags
import net.minecraft.world.level.Level
import java.util.concurrent.CompletableFuture
import java.util.stream.Stream

/**
 * Lightweight command source for LiquidBounce client commands, mirroring the role of
 * Minecraft's `CommandSourceStack` on the server side.
 *
 * The client only executes commands in the local game context, so the source is a
 * singleton that exposes the current game state. `requires` predicates and handlers can
 * use [playerOrNull]/[levelOrNull]/[isIngame] to gate execution, while the inherited
 * [MinecraftShortcuts] properties stay non-null for use after the ingame check passed.
 *
 * Implements vanilla [SharedSuggestionProvider] (like the client's own
 * `ClientSuggestionProvider`) so argument types such as `Vec3ArgumentType`,
 * `ResourceArgument` and `ItemArgument` resolve their suggestions through the source,
 * instead of each provider hardcoding its candidates. Every delegate falls back to an
 * empty/static value when no server connection exists (main menu, unit tests).
 */
object ClientCommandSource : SharedSuggestionProvider {

    val playerOrNull: LocalPlayer?
        get() = mc()?.player

    val levelOrNull: ClientLevel?
        get() = mc()?.level

    val isIngame: Boolean
        get() = playerOrNull != null && levelOrNull != null

    /**
     * Registry access shared by command argument factories ([net.ccbluex.liquidbounce.features.command.arguments.itemArgument],
     * [net.ccbluex.liquidbounce.features.command.arguments.resourceArgument]):
     * the joined world's registries when available, otherwise the
     * static vanilla lookup so parse/suggestions keep working outside a world.
     */
    internal fun commandBuildContext(): HolderLookup.Provider {
        return levelOrNull?.registryAccess() ?: VanillaRegistries.createLookup()
    }

    /**
     * Feature flags announced by the current server connection; the vanilla defaults
     * when no server is joined (main menu, unit tests).
     *
     * The `mc.connection` access is wrapped for the same reason as [commandBuildContext].
     */
    override fun enabledFeatures(): FeatureFlagSet =
        mc()?.connection?.enabledFeatures() ?: FeatureFlags.DEFAULT_FLAGS

    override fun permissions(): PermissionSet {
        // Mirrors ClientPacketListener: the local player carries the permission set the
        // server announced for us; without a player there are no permissions.
        return playerOrNull?.permissions() ?: PermissionSet.NO_PERMISSIONS
    }

    // Replicated from vanilla ClientSuggestionProvider.getOnlinePlayerNames.
    override fun getOnlinePlayerNames(): Collection<String> =
        mc()?.connection?.onlinePlayers?.map { it.profile.name } ?: emptyList()

    // Replicated from vanilla ClientSuggestionProvider.getAllTeams.
    override fun getAllTeams(): Collection<String> =
        mc()?.connection?.scoreboard()?.teamNames ?: emptyList()

    // Replicated from vanilla ClientSuggestionProvider.getAvailableSounds.
    override fun getAvailableSounds(): Stream<Identifier> =
        mc()?.soundManager?.availableSounds?.stream() ?: Stream.empty()

    /**
     * Server-driven custom tab completions require a request/response round-trip with a
     * pending-suggestions id owned by the vanilla `ClientSuggestionProvider`; our client
     * commands never reach the server, so there is nothing to query - empty by design.
     */
    override fun customSuggestion(context: CommandContext<*>): CompletableFuture<Suggestions> =
        Suggestions.empty()

    override fun levels(): Set<ResourceKey<Level>> =
        mc()?.connection?.levels() ?: emptySet()

    /**
     * Synced world registries when a level is loaded; otherwise the static builtin
     * [RegistryAccess] (not [VanillaRegistries.createLookup], which is only a
     * [HolderLookup.Provider] and is not a [RegistryAccess]).
     */
    override fun registryAccess(): RegistryAccess =
        levelOrNull?.registryAccess()
            ?: RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY)

    /**
     * Registry element suggestions for argument types such as `ResourceArgument`.
     *
     * Replicated from vanilla `ClientSuggestionProvider.suggestRegistryElements`, which
     * serves the request from the registries synced with the current server connection;
     * we resolve the same key against [registryAccess] instead.
     */
    override fun suggestRegistryElements(
        key: ResourceKey<out Registry<*>>,
        elements: SharedSuggestionProvider.ElementSuggestionType,
        builder: SuggestionsBuilder,
        context: CommandContext<*>,
    ): CompletableFuture<Suggestions> {
        return SharedSuggestionProvider.listSuggestions(context, builder, key, elements)
            .thenApply { built ->
                // Vanilla falls back to static entries when the synced registry is empty;
                // our lookup already reflects either world or vanilla state.
                if (built.isEmpty) {
                    val holder = commandBuildContext().lookup(key).orElse(null)
                    if (holder != null) {
                        suggestRegistryElements(holder as HolderLookup<*>, elements, builder)
                        return@thenApply builder.build()
                    }
                }
                built
            }
    }
}
