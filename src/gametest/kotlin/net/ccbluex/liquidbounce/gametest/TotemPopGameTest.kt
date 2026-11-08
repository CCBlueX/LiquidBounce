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
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.TransferOrigin
import net.ccbluex.liquidbounce.features.module.modules.misc.ModuleNotifier
import net.ccbluex.liquidbounce.injection.mixins.minecraft.gui.MixinChatComponentAccessor
import net.ccbluex.liquidbounce.utils.combat.TotemPopTracker
import net.ccbluex.liquidbounce.utils.world.nextLocalEntityId
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext
import net.minecraft.client.Minecraft
import net.minecraft.client.player.RemotePlayer
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityEvent
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Items
import java.util.UUID

/**
 * [ModuleNotifier] announces the pops of a player counted since that player last died or came into view, and no count
 * outlives the world. A second player takes its pops from injected entity events, the local player pops a real totem.
 */
class TotemPopGameTest : FabricClientGameTest {

    override fun runTest(context: ClientGameTestContext) {
        context.waitForClient()
        val saved = context.fromClient { ConfigSystem.serializeValueGroup(ModuleNotifier) }

        try {
            val other = context.worldBuilder().create().use { world ->
                context.onClient {
                    ModuleNotifier.set("TotemPopMessages", "true")
                    ModuleNotifier.set("UseNotification", "false")
                    ModuleNotifier.enabled = true
                }

                val other = otherPlayerPops(context)
                ownPops(context, world.server)
                other
            }

            check(context.fromClient { TotemPopTracker.pops(other) } == 0) { "Pops survived leaving the world" }
        } finally {
            context.onClient {
                ModuleNotifier.enabled = false
                ConfigSystem.deserializeValueGroup(ModuleNotifier, saved)
            }
        }
    }

    /**
     * Returns the other player, which still has one pop.
     */
    private fun otherPlayerPops(context: ClientGameTestContext): RemotePlayer {
        val other = context.fromClient { client -> addOtherPlayer(client) }
        context.popAnnounced(other, 1)
        context.popAnnounced(other, 2)

        context.entityEvent(other, EntityEvent.DEATH)
        context.popAnnounced(other, 1)

        context.onClient { client -> client.level!!.removeEntity(other.id, Entity.RemovalReason.DISCARDED) }
        return context.fromClient { client -> addOtherPlayer(client) }.also { context.popAnnounced(it, 1) }
    }

    private fun ownPops(context: ClientGameTestContext, server: TestServerContext) {
        server.runCommand("gamemode survival @a")
        server.runCommand("item replace entity @a weapon.offhand with totem_of_undying")
        context.waitFor({ client -> client.player!!.offhandItem.`is`(Items.TOTEM_OF_UNDYING) }, TIMEOUT)
        server.runCommand("damage @p 100 minecraft:mob_attack")
        context.waitFor({ client -> TotemPopTracker.pops(client.player!!) == 1 }, TIMEOUT)

        server.runCommand("kill @p")
        context.waitFor({ client -> TotemPopTracker.pops(client.player!!) == 0 }, TIMEOUT)
    }

    /**
     * Pops a totem of [player] and waits for the chat message that calls it pop number [count].
     */
    private fun ClientGameTestContext.popAnnounced(player: Player, count: Int) {
        onClient { client -> client.gui.hud.chat.clearMessages(true) }
        entityEvent(player, EntityEvent.PROTECTED_FROM_DEATH)
        waitFor({ client ->
            (client.gui.hud.chat as MixinChatComponentAccessor).allMessages.any {
                it.content().string.contains("$OTHER_NAME popped a totem $count times")
            }
        }, TIMEOUT)
    }

    private fun ClientGameTestContext.entityEvent(player: Player, event: Byte) = onClient {
        EventManager.callEvent(PacketEvent(TransferOrigin.INCOMING, ClientboundEntityEventPacket(player, event)))
    }

    private fun addOtherPlayer(client: Minecraft): RemotePlayer {
        val level = client.level!!
        val local = client.player!!
        return RemotePlayer(level, GameProfile(OTHER_UUID, OTHER_NAME)).apply {
            id = level.nextLocalEntityId()
            setPos(local.x, local.y, local.z + OTHER_DISTANCE)
            level.addEntity(this)
        }
    }

    private companion object {
        const val TIMEOUT = 60
        const val OTHER_NAME = "PopTarget"
        const val OTHER_DISTANCE = 3.0
        val OTHER_UUID: UUID = UUID.nameUUIDFromBytes(OTHER_NAME.toByteArray())
    }

}
