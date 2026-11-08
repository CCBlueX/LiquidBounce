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

import com.mojang.authlib.GameProfile
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.types.group.ValueGroup
import net.ccbluex.liquidbounce.features.module.modules.misc.nameprotect.ModuleNameProtect
import net.ccbluex.liquidbounce.utils.world.nextLocalEntityId
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.player.RemotePlayer
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.Entity
import java.util.UUID

/**
 * [ModuleNameProtect] rolls a new random name for the player every interval with RandomName, and with
 * NumberedAliases and IncludeWorldPlayers names players that are in the world but not in the tab list after the
 * prefix and a number. The screenshot `nameprotect-aliases` shows a chat line with all three names replaced.
 */
class NameProtectGameTest : FabricClientGameTest {

    override fun runTest(context: ClientGameTestContext) {
        context.waitForClient()
        val saved = context.fromClient { ConfigSystem.serializeValueGroup(ModuleNameProtect) }

        try {
            context.worldBuilder().create().use { world ->
                world.server.runCommand("gamemode survival @a")
                context.waitFor({ client -> client.player?.onGround() == true }, SETTLE_TICKS)
                val ownName = context.fromClient { client -> checkNotNull(client.player).gameProfile.name }
                val others = context.fromClient { client ->
                    OTHER_NAMES.map { name -> addPlayer(checkNotNull(client.level), name) }
                }

                try {
                    context.onClient {
                        value("RandomName", "Interval").setByString("1")
                        value("RandomName", "Enabled").setByString("true")
                        ModuleNameProtect.enabled = true
                    }
                    context.waitTicks(UPDATE_TICKS)

                    val first = context.fromClient { ModuleNameProtect.replace(ownName) }
                    check(first != ownName && first != "You") { "RandomName did not replace the name: $first" }
                    context.waitFor({ ModuleNameProtect.replace(ownName) != first }, REROLL_TICKS)

                    context.onClient {
                        value("ObfuscateOthers", "Enabled").setByString("true")
                        value("ObfuscateOthers", "NumberedAliases", "Prefix").setByString("Foe")
                        value("ObfuscateOthers", "NumberedAliases", "Enabled").setByString("true")
                    }
                    context.waitTicks(UPDATE_TICKS)
                    check(context.fromClient { OTHER_NAMES.map(ModuleNameProtect::replace) } == OTHER_NAMES) {
                        "Players outside the tab list were replaced without IncludeWorldPlayers"
                    }

                    context.onClient { value("ObfuscateOthers", "IncludeWorldPlayers").setByString("true") }
                    context.waitTicks(UPDATE_TICKS)
                    val aliases = context.fromClient { OTHER_NAMES.map(ModuleNameProtect::replace) }
                    check(aliases.toSet() == setOf("Foe1", "Foe2")) { "The world players were named $aliases" }

                    context.onClient { client ->
                        client.gui.hud.chat.addClientSystemMessage(
                            Component.literal("${OTHER_NAMES.joinToString()} and $ownName")
                        )
                    }
                    context.waitTicks(UPDATE_TICKS)
                    context.takeScreenshot("nameprotect-aliases")
                } finally {
                    context.onClient { client ->
                        val level = checkNotNull(client.level)
                        others.forEach { level.removeEntity(it.id, Entity.RemovalReason.DISCARDED) }
                    }
                }
            }
        } finally {
            context.onClient {
                ModuleNameProtect.enabled = false
                ConfigSystem.deserializeValueGroup(ModuleNameProtect, saved)
            }
        }
    }

    private fun value(vararg path: String) = path.dropLast(1)
        .fold<String, ValueGroup>(ModuleNameProtect) { group, name ->
            group.containedValues.first { it.name == name } as ValueGroup
        }
        .containedValues.first { it.name == path.last() }

    private fun addPlayer(level: ClientLevel, name: String) =
        RemotePlayer(level, GameProfile(UUID.randomUUID(), name)).apply {
            id = level.nextLocalEntityId()
            setPos(checkNotNull(level.players().first()).position())
            level.addEntity(this)
        }

    private companion object {
        val OTHER_NAMES = listOf("Alphanaut", "Bravonaut")

        const val SETTLE_TICKS = 200
        const val UPDATE_TICKS = 3
        const val REROLL_TICKS = 100
    }

}
