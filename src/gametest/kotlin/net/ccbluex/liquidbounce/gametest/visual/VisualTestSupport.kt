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
package net.ccbluex.liquidbounce.gametest.visual

import com.mojang.authlib.GameProfile
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.features.command.commands.ingame.fakeplayer.FakePlayer
import net.ccbluex.liquidbounce.gametest.fromClient
import net.ccbluex.liquidbounce.gametest.onClient
import net.ccbluex.liquidbounce.utils.world.nextLocalEntityId
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext
import net.minecraft.client.Minecraft
import java.util.UUID

internal const val SETTLE_TIMEOUT_TICKS = 200

/**
 * Creates a flat world and runs [block] in it, the player standing on the ground in creative mode.
 */
internal fun ClientGameTestContext.inVisualTestWorld(block: (TestSingleplayerContext) -> Unit) {
    worldBuilder().create().use { world ->
        world.server.runCommand("gamemode creative @a")
        waitFor({ client -> client.player?.onGround() == true }, SETTLE_TIMEOUT_TICKS)
        input.lookAt(0f, 0f)
        block(world)
    }
}

/**
 * The pitch to look at the ground a few blocks ahead with.
 */
internal const val LOOK_DOWN_PITCH = 50f

/**
 * Sets a value by its key below `liquidbounce.modules.`.
 */
internal fun ClientGameTestContext.setValue(key: String, value: String) = onClient {
    val fullKey = "liquidbounce.modules.$key"
    val configValue = checkNotNull(ConfigSystem.findValueByKey(fullKey)) { "no value $fullKey" }
    configValue.setByString(value)
}

/**
 * A player only the client knows, standing [offsetX] and [offsetZ] blocks away from the own one.
 */
internal fun ClientGameTestContext.spawnFakePlayer(name: String, offsetX: Double, offsetZ: Double): FakePlayer =
    fromClient { client ->
        val level = client.level()
        val player = client.player()
        FakePlayer(level, GameProfile(UUID.randomUUID(), name)).apply {
            id = level.nextLocalEntityId()
            setPos(player.x + offsetX, player.y, player.z + offsetZ)
            level.addEntity(this)
        }
    }

internal fun Minecraft.player() = checkNotNull(player)

internal fun Minecraft.level() = checkNotNull(level)
