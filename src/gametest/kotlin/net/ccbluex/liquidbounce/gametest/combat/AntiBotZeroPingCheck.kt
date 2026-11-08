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

import com.mojang.authlib.GameProfile
import io.netty.buffer.Unpooled
import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.modes.CustomAntiBotMode
import net.ccbluex.liquidbounce.gametest.onClient
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.PlayerInfo
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.Action
import java.util.EnumSet
import java.util.UUID

private const val ZERO_PING_GRACE = 5

/**
 * AntiBot: a tab list latency of 0 for longer than the grace period.
 */
fun CombatScene.checkAntiBotZeroPing() {
    val uuid = UUID.randomUUID()
    startCustomAntiBot {
        CustomAntiBotMode.set("ZeroPing/Grace", ZERO_PING_GRACE.toString())
        CustomAntiBotMode.set("ZeroPing/Enabled", "true")
    }
    val player = spawnFakePlayer(uuid, FAKE_PLAYER_ID, "Premium")
    context.onClient { client -> playerInfoMap(client)[uuid] = PlayerInfo(GameProfile(uuid, "Premium"), false) }
    context.waitTicks(ZERO_PING_GRACE - 2)
    check(!isBot(player)) { "AntiBot: zero ping flagged within the grace period" }
    context.waitTicks(ZERO_PING_GRACE)
    check(isBot(player)) { "AntiBot: sustained zero ping not flagged" }
    context.onClient { client -> receiveLatency(client, uuid, 50) }
    context.waitTick()
    check(!isBot(player)) { "AntiBot: flagged with a real ping" }

    context.onClient { client ->
        checkNotNull(client.connection).handlePlayerInfoRemove(ClientboundPlayerInfoRemovePacket(listOf(uuid)))
    }
    stopAntiBot(player)
}

private fun receiveLatency(client: Minecraft, uuid: UUID, latency: Int) {
    val connection = checkNotNull(client.connection)
    val buf = RegistryFriendlyByteBuf(Unpooled.buffer(), connection.registryAccess())
    buf.writeEnumSet(EnumSet.of(Action.UPDATE_LATENCY), Action::class.java)
    buf.writeVarInt(1)
    buf.writeUUID(uuid)
    buf.writeVarInt(latency)
    connection.handlePlayerInfoUpdate(ClientboundPlayerInfoUpdatePacket.STREAM_CODEC.decode(buf))
}
