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
import net.ccbluex.liquidbounce.features.module.modules.misc.ModuleTeams
import net.ccbluex.liquidbounce.gametest.fromClient
import net.ccbluex.liquidbounce.gametest.onClient
import net.ccbluex.liquidbounce.utils.combat.shouldBeAttacked
import net.minecraft.client.player.RemotePlayer
import net.minecraft.world.entity.Entity
import java.util.UUID

private const val MATE_ID = 910_000
private const val SYNC_TICKS = 5

/**
 * Teams NameColor: the color counts wherever in the name it is set, here by a colored team prefix.
 */
fun CombatScene.checkTeamsNameColor() {
    reset()
    val self = context.fromClient { client -> checkNotNull(client.player).scoreboardName }
    for ((team, color) in listOf("ours" to "red", "mates" to "red", "others" to "blue")) {
        world.server.runCommand("team add $team")
        world.server.runCommand("""team modify $team prefix {"text":"[$team] ","color":"$color"}""")
    }
    world.server.runCommand("team join ours $self")
    world.server.runCommand("team join mates Mate")
    world.server.runCommand("team join others Other")

    context.onClient { client ->
        val level = checkNotNull(client.level)
        for ((offset, name) in listOf(0 to "Mate", 1 to "Other")) {
            val fake = RemotePlayer(level, GameProfile(UUID.randomUUID(), name))
            fake.id = MATE_ID + offset
            fake.setPos(origin.add(3.0, 0.0, offset.toDouble()))
            level.addEntity(fake)
        }
        ModuleTeams.value("Matches").deserializeFrom(GSON_DEFAULT, JsonArray().apply { add("NameColor") })
        ModuleTeams.value("ArmorColor").deserializeFrom(GSON_DEFAULT, JsonArray())
        ModuleTeams.enabled = true
    }
    context.waitTicks(SYNC_TICKS)

    try {
        val (mate, other) = context.fromClient { client ->
            !clientEntity(client, MATE_ID).shouldBeAttacked() to !clientEntity(client, MATE_ID + 1).shouldBeAttacked()
        }
        check(mate) { "Teams: a red prefixed player is not a teammate" }
        check(!other) { "Teams: a blue prefixed player is a teammate" }
    } finally {
        context.onClient { client ->
            ModuleTeams.enabled = false
            for (id in listOf(MATE_ID, MATE_ID + 1)) {
                checkNotNull(client.level).removeEntity(id, Entity.RemovalReason.DISCARDED)
            }
        }
        for (team in listOf("ours", "mates", "others")) {
            world.server.runCommand("team remove $team")
        }
    }
}
