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

import net.ccbluex.gametest.harness.connection.GameTestEnvironment
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.features.command.CommandManager
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.TitleScreen
import net.minecraft.world.level.levelgen.Heightmap
import net.minecraft.world.phys.Vec3
import org.apache.commons.lang3.function.FailableConsumer
import org.apache.commons.lang3.function.FailableFunction

// LiquidBounce holds a TaskProgressScreen until its startup tasks (e.g. the DJL natives) finish
private const val STARTUP_TIMEOUT_TICKS = 5 * 60 * 20

/** CI runs the server game tests once per anticheat, and the tests of the client alone in a job of their own. */
private val localRun = System.getenv("GAMETEST_SERVER_ANTICHEAT").isNullOrBlank()

/**
 * A game test of the client alone (commands, configs, registries, simulations), on the integrated server if it needs
 * a world. It runs only when no anticheat is picked; `-PgameTestServerOnly` selects these too.
 */
abstract class LocalGameTest : FabricClientGameTest {

    private val name = javaClass.simpleName.removeSuffix("GameTest")

    final override fun runTest(context: ClientGameTestContext) {
        if (!localRun || !GameTestEnvironment.isSelected(name)) {
            return
        }
        context.waitForClient()
        test(context)
    }

    protected abstract fun test(context: ClientGameTestContext)

}

fun ClientGameTestContext.waitForClient() {
    waitFor({ client -> client.gui.screen() is TitleScreen }, STARTUP_TIMEOUT_TICKS)
    check(fromClient { LiquidBounce.isInitialized }) { "LiquidBounce did not finish initializing" }
}

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

fun TestServerContext.groundY(): Int = computeOnServer<Int, RuntimeException> {
    it.overworld().getHeight(Heightmap.Types.MOTION_BLOCKING, 8, 8)
}
