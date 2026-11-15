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
package net.ccbluex.liquidbounce.gametest

import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.features.command.CommandManager
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.minecraft.client.Minecraft
import net.minecraft.world.phys.Vec3
import org.apache.commons.lang3.function.FailableConsumer
import org.apache.commons.lang3.function.FailableFunction

fun ClientGameTestContext.onClient(
    block: FailableConsumer<Minecraft, RuntimeException>
) = runOnClient<RuntimeException>(block)

fun <T> ClientGameTestContext.fromClient(
    block: FailableFunction<Minecraft, T, RuntimeException>
): T = computeOnClient(block)

fun ClientGameTestContext.command(command: String) = onClient { CommandManager.execute(command) }

/** The suggestions for [input], a command with the prefix, completed at its end. */
fun ClientGameTestContext.suggest(input: String): List<String> = fromClient {
    CommandManager.autoComplete(input, input.length).get().list.map { it.text }
}

/** Runs [block], then turns [modules] off and puts their settings back. */
inline fun ClientGameTestContext.restoring(vararg modules: ClientModule, block: () -> Unit) {
    val saved = fromClient { modules.map { ConfigSystem.serializeValueGroup(it) } }
    try {
        block()
    } finally {
        onClient {
            for ((module, config) in modules.zip(saved)) {
                module.enabled = false
                ConfigSystem.deserializeValueGroup(module, config)
            }
        }
    }
}

fun ClientGameTestContext.playerPosition(): Vec3 = fromClient { checkNotNull(it.player).position() }
