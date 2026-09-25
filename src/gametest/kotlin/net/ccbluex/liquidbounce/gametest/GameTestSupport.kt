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

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.features.command.CommandManager
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.minecraft.client.gui.screens.TitleScreen

// LiquidBounce holds a TaskProgressScreen until its startup tasks (e.g. the DJL natives) finish
private const val STARTUP_TIMEOUT_TICKS = 5 * 60 * 20

/**
 * Waits for the title screen behind LiquidBounce's startup tasks.
 */
fun ClientGameTestContext.waitForClient() {
    waitFor({ client -> client.gui.screen() is TitleScreen }, STARTUP_TIMEOUT_TICKS)
    check(fromClient { LiquidBounce.isInitialized }) { "LiquidBounce did not finish initializing" }
}

/**
 * Runs [block] on the client thread; the game only allows touching it there.
 */
fun ClientGameTestContext.onClient(block: () -> Unit) = runOnClient<RuntimeException> { block() }

fun <T> ClientGameTestContext.fromClient(block: () -> T): T = computeOnClient<T, RuntimeException> { block() }

/**
 * Runs a client command, without the prefix.
 */
fun ClientGameTestContext.command(command: String) = onClient { CommandManager.execute(command) }

/**
 * The suggestions for [input], a command with the prefix, completed at its end.
 */
fun ClientGameTestContext.suggest(input: String): List<String> = fromClient {
    CommandManager.autoComplete(input, input.length).get().list.map { it.text }
}
