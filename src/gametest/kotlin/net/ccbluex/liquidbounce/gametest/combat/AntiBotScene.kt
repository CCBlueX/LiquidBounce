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
package net.ccbluex.liquidbounce.gametest.combat

import com.google.gson.JsonArray
import com.mojang.authlib.GameProfile
import net.ccbluex.liquidbounce.config.gson.util.GSON_DEFAULT
import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.ModuleAntiBot
import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.modes.CustomAntiBotMode
import net.ccbluex.liquidbounce.gametest.fromClient
import net.ccbluex.liquidbounce.gametest.onClient
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientPacketListener
import net.minecraft.client.multiplayer.PlayerInfo
import net.minecraft.client.player.RemotePlayer
import net.minecraft.world.entity.Entity
import java.util.UUID

const val FAKE_PLAYER_ID = 900_000

/**
 * Enables AntiBot in the custom mode with no checks but the ones [configure] turns on.
 */
fun CombatScene.startCustomAntiBot(configure: () -> Unit) {
    reset()
    context.onClient {
        ModuleAntiBot.modes.setByString("Custom")
        CustomAntiBotMode.value("Conditions").deserializeFrom(GSON_DEFAULT, JsonArray())
        CustomAntiBotMode.set("InvalidGround/Enabled", "false")
        CustomAntiBotMode.set("Name/Enabled", "false")
        configure()
        ModuleAntiBot.enabled = true
    }
}

/**
 * Disables AntiBot and removes the fake players [ids].
 */
fun CombatScene.stopAntiBot(vararg ids: Int) = context.onClient { client ->
    ModuleAntiBot.enabled = false
    for (id in ids) {
        checkNotNull(client.level).removeEntity(id, Entity.RemovalReason.DISCARDED)
    }
}

fun CombatScene.isBot(id: Int) = context.fromClient { client -> ModuleAntiBot.isBot(clientEntity(client, id)) }

fun CombatScene.spawnFakePlayer(uuid: UUID, id: Int, name: String): Int {
    context.onClient { client ->
        val level = checkNotNull(client.level)
        val fake = RemotePlayer(level, GameProfile(uuid, name))
        fake.id = id
        fake.setPos(origin.add(3.0, 0.0, 0.0))
        level.addEntity(fake)
    }
    return id
}

@Suppress("UNCHECKED_CAST")
fun playerInfoMap(client: Minecraft): MutableMap<UUID, PlayerInfo> =
    ClientPacketListener::class.java.getDeclaredField("playerInfoMap").apply { isAccessible = true }
        .get(client.connection) as MutableMap<UUID, PlayerInfo>
