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
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.misc.FriendManager
import net.ccbluex.liquidbounce.gametest.fromClient
import net.ccbluex.liquidbounce.gametest.onClient
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext
import net.minecraft.client.player.RemotePlayer
import net.minecraft.network.protocol.game.ServerboundSpectatorActionPacket
import net.minecraft.world.entity.Entity
import net.minecraft.world.phys.EntityHitResult
import java.util.UUID

private const val FRIEND_ID = 1_000_002
private const val FRIEND_NAME = "Buddy"

/**
 * With CancelAttack, a click on a friend sends neither the attack nor the swing, so the server keeps the attack
 * cooldown, while a spectator still gets to spectate the friend. Without it, the click attacks and swings.
 */
class FriendCancelAttackGameTest : FabricClientGameTest {

    override fun runTest(context: ClientGameTestContext) = context.combatTest { server, y ->
        val cancelAttack = context.fromClient { FriendManager.value("CancelAttack").get() }
        val friend = context.fromClient { client ->
            val level = checkNotNull(client.level)
            val friend = RemotePlayer(level, GameProfile(UUID.randomUUID(), FRIEND_NAME))
            // Client-only, so it takes an id the server never hands out
            friend.id = FRIEND_ID
            friend.setPos(0.5, y.toDouble(), 2.5)
            level.addEntity(friend)
            FriendManager.add(FriendManager.Friend(FRIEND_NAME, null))
            FriendManager.set("CancelAttack", "true")
            friend
        }

        try {
            aimAt(context, friend)
            click(context)
            context.onClient {
                check(CombatEvents.attacks.isEmpty()) { "CancelAttack: attacked the friend" }
                check(CombatEvents.punches == 0) { "CancelAttack: swung at the friend" }
            }
            check(server.attackCooldown() == 1f) { "CancelAttack: reset the attack cooldown" }

            context.onClient { FriendManager.set("CancelAttack", "false") }
            click(context)
            context.onClient {
                check(friend in CombatEvents.attacks) { "CancelAttack off: did not attack the friend" }
                check(CombatEvents.punches > 0) { "CancelAttack off: did not swing" }
            }
            check(server.attackCooldown() < 1f) { "CancelAttack off: kept the attack cooldown" }

            context.onClient { FriendManager.set("CancelAttack", "true") }
            server.runCommand("gamemode spectator @a")
            context.waitFor({ client -> client.gameMode?.isSpectator == true }, WAIT_TICKS)
            aimAt(context, friend)
            click(context)
            check(SpectateRequests.target == FRIEND_ID) { "CancelAttack: a spectator could not spectate the friend" }
        } finally {
            context.onClient {
                FriendManager.remove(FRIEND_NAME)
                FriendManager.set("CancelAttack", cancelAttack.toString())
                friend.discard()
            }
        }
    }

    private fun aimAt(context: ClientGameTestContext, entity: Entity) {
        context.waitFor({ client -> (client.hitResult as? EntityHitResult)?.entity === entity }, WAIT_TICKS)
        context.waitTicks(SETTLE_TICKS)
    }

    private fun click(context: ClientGameTestContext) {
        context.onClient {
            CombatEvents.reset()
            SpectateRequests.target = null
        }
        context.input.holdKeyFor({ it.keyAttack }, 1)
        context.waitTick()
    }

    private fun TestServerContext.attackCooldown() = computeOnServer<Float, RuntimeException> { server ->
        server.playerList.players.single().getAttackStrengthScale(0f)
    }

    /**
     * The entity the last click asked the server to spectate.
     */
    private object SpectateRequests : EventListener {
        @Volatile
        var target: Int? = null

        @Suppress("unused")
        private val packetHandler = handler<PacketEvent> { event ->
            val packet = event.packet
            if (packet is ServerboundSpectatorActionPacket && packet.spectateEntityId().isPresent) {
                target = packet.spectateEntityId().asInt
            }
        }
    }

}
