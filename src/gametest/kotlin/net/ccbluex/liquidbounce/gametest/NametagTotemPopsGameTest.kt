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

import com.google.gson.JsonArray
import com.mojang.authlib.GameProfile
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.gson.util.GSON_DEFAULT
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.TransferOrigin
import net.ccbluex.liquidbounce.features.module.modules.render.nametags.ModuleNametags
import net.ccbluex.liquidbounce.utils.world.nextLocalEntityId
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.minecraft.client.Minecraft
import net.minecraft.client.player.RemotePlayer
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket
import net.minecraft.util.ARGB
import net.minecraft.world.entity.EntityEvent
import net.minecraft.world.entity.player.Player
import java.util.UUID

/**
 * The colour of the pop count, down to the glyph edges that blend into the background of the tag.
 */
private fun isGold(pixel: Int): Boolean {
    val red = ARGB.red(pixel)
    return red > 150 && ARGB.green(pixel) in red / 2..red * 9 / 10 && ARGB.blue(pixel) < red / 2
}

/**
 * With its TotemPops part selected, [ModuleNametags] writes the totems a player popped on its tag in gold, and takes
 * the count off again once the player died. A second player in front of the camera takes its pops from injected
 * entity events. Leaves a screenshot of the tag before the pops, after two pops and after the death.
 */
class NametagTotemPopsGameTest : FabricClientGameTest {

    override fun runTest(context: ClientGameTestContext) {
        context.waitForClient()
        val saved = context.fromClient { ConfigSystem.serializeValueGroup(ModuleNametags) }

        try {
            context.worldBuilder().create().use { world ->
                world.server.runCommand("execute as @a at @s run tp @s ~ ~ ~ 0 0")
                context.waitFor({ client -> client.player!!.yRot == 0f && client.player!!.xRot == 0f }, TIMEOUT)

                val other = context.fromClient { client -> addOtherPlayer(client) }
                context.onClient {
                    ModuleNametags.option("Text/Parts").deserializeFrom(
                        GSON_DEFAULT,
                        JsonArray().apply { listOf("Name", "TotemPops").forEach(::add) },
                    )
                    ModuleNametags.enabled = true
                }
                context.waitTicks(SETTLE_TICKS)

                val unpopped = context.goldPixels("nametag-totem-pops-none")

                repeat(2) { context.entityEvent(other, EntityEvent.PROTECTED_FROM_DEATH) }
                val popped = context.goldPixels("nametag-totem-pops-two")
                check(popped - unpopped >= COUNT_PIXELS) {
                    "No pop count on the tag after two pops: $unpopped gold pixels before, $popped after"
                }

                context.entityEvent(other, EntityEvent.DEATH)
                val dead = context.goldPixels("nametag-totem-pops-dead")
                check(dead - unpopped < COUNT_PIXELS) {
                    "The pop count stayed on the tag after the death: $unpopped gold pixels before, $dead after"
                }
            }
        } finally {
            context.onClient {
                ModuleNametags.enabled = false
                ConfigSystem.deserializeValueGroup(ModuleNametags, saved)
            }
        }
    }

    /**
     * The gold pixels in the middle of the upper half of the screen, where the tag is and the HUD is not.
     */
    private fun ClientGameTestContext.goldPixels(name: String) =
        countPixels(name, ::isGold, area = { x, y -> x in 0.25f..0.75f && y < 0.5f }).single()

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
        const val SETTLE_TICKS = 40

        /** Gold pixels the tag has to gain to count as showing the pops. */
        const val COUNT_PIXELS = 30

        const val OTHER_NAME = "PopTarget"
        const val OTHER_DISTANCE = 3.0
        val OTHER_UUID: UUID = UUID.nameUUIDFromBytes(OTHER_NAME.toByteArray())
    }

}
