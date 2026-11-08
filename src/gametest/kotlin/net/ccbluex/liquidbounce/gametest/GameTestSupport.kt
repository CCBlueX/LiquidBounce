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
@file:Suppress("TooManyFunctions")

package net.ccbluex.liquidbounce.gametest

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.config.types.Value
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.config.types.group.ValueGroup
import net.ccbluex.liquidbounce.features.command.CommandManager
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.TitleScreen
import net.minecraft.core.BlockPos
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.levelgen.Heightmap
import net.minecraft.world.phys.Vec3
import org.apache.commons.lang3.function.FailableConsumer
import org.apache.commons.lang3.function.FailableFunction

// LiquidBounce holds a TaskProgressScreen until its startup tasks (e.g. the DJL natives) finish
private const val STARTUP_TIMEOUT_TICKS = 5 * 60 * 20

/** `-PgameTestAngleSmooth=<mode>` (Sigmoid, Acceleration, ...) runs the tests with every AngleSmooth on that mode. */
private val angleSmooth: String? = System.getProperty("net.ccbluex.liquidbounce.gametest.angleSmooth")

fun ClientGameTestContext.waitForClient() {
    waitFor({ client -> client.gui.screen() is TitleScreen }, STARTUP_TIMEOUT_TICKS)
    check(fromClient { LiquidBounce.isInitialized }) { "LiquidBounce did not finish initializing" }
    if (angleSmooth != null) {
        onClient { ModuleManager.forEach(::useAngleSmooth) }
    }
}

/** Applies the AngleSmooth mode of the run to settings a test makes itself; modules get it in [waitForClient]. */
fun useAngleSmooth(group: ValueGroup) {
    val mode = angleSmooth ?: return
    group.collectValuesRecursively()
        .filterIsInstance<ModeValueGroup<*>>()
        .filter { setting -> setting.name == "AngleSmooth" && setting.modes.any { it.tag == mode } }
        .forEach { it.setByString(mode) }
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

fun ClientGameTestContext.playerPosition(): Vec3 = fromClient { checkNotNull(it.player).position() }

fun MinecraftServer.player(): ServerPlayer = playerList.players.first()

fun <T> TestServerContext.player(block: (ServerPlayer) -> T): T =
    computeOnServer<T, RuntimeException> { block(it.player()) }

fun TestServerContext.groundY(): Int = computeOnServer<Int, RuntimeException> {
    it.overworld().getHeight(Heightmap.Types.MOTION_BLOCKING, 8, 8)
}

fun TestServerContext.countBlocks(from: BlockPos, to: BlockPos): Int = computeOnServer<Int, RuntimeException> {
    val level = it.overworld()
    BlockPos.betweenClosed(from, to).count { pos -> !level.getBlockState(pos).isAir }
}

/**
 * The option at [path] below this group: names separated by `/`, a mode of a mode group is entered by its name.
 */
fun ValueGroup.option(path: String): Value<*> = path.split('/').fold<String, Value<*>>(this) { current, name ->
    val group = current as? ValueGroup ?: error("${current.name} has no option $name")
    group.containedValues.firstOrNull { it.name == name }
        ?: (group as? ModeValueGroup<*>)?.modes?.firstOrNull { it.name == name }
        ?: error("${group.name} has no option $name")
}

/**
 * Sets the option at [path], see [option], from its text form.
 */
fun ValueGroup.set(path: String, value: String) = option(path).setByString(value)

/**
 * Runs [block] on the server thread and returns its result.
 */
fun <T> TestServerContext.query(block: (MinecraftServer) -> T): T =
    computeOnServer<T, RuntimeException> { block(it) }
