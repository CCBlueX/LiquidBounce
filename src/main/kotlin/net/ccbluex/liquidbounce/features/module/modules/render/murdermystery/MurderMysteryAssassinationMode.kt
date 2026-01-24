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

package net.ccbluex.liquidbounce.features.module.modules.render.murdermystery

import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.math.levenshtein
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.multiplayer.PlayerInfo
import net.minecraft.client.player.AbstractClientPlayer
import net.minecraft.client.player.LocalPlayer
import net.minecraft.core.component.DataComponents
import net.minecraft.network.protocol.game.ClientboundSoundPacket
import net.minecraft.resources.Identifier
import net.minecraft.world.item.MapItem
import net.minecraft.world.level.saveddata.maps.MapId
import java.util.Locale
import java.util.UUID
import kotlin.math.absoluteValue

object MurderMysteryAssassinationMode : MurderMysteryMode("Assassination") {

    private var lastMap: MapId? = null
    private var currentAssassinationTarget: UUID? = null
    private var currentAssassin: UUID? = null

    val packetHandler =
        handler<PacketEvent> { packetEvent ->
            val world = mc.level ?: return@handler

            if (packetEvent.packet is ClientboundSoundPacket) {
                val packet = packetEvent.packet

                if (packet.sound.value().location.toString() != "minecraft:block.note_block.basedrum") {
                    return@handler
                }

                val expectedDistance = calculateDistanceFromWarningVolume(packet.volume)

                val probablyAssassin =
                    world.players().minByOrNull {
                        (it.distanceTo(player) - expectedDistance).absoluteValue
                    } ?: return@handler

                val newAssassin = probablyAssassin.gameProfile.id

                if (currentAssassin != newAssassin) {
                    chat("Your Assassin: " + probablyAssassin.gameProfile.name)
                }

                currentAssassin = newAssassin
            }
        }

    private fun calculateDistanceFromWarningVolume(volume: Float): Double {
        // Fitted by observed values
        return ((1 / volume) - 0.98272992) / 0.04342088
    }

    val repeatable =
        tickHandler {
            assassinModeBs(player, world)
        }

    private fun assassinModeBs(
        player: LocalPlayer,
        world: ClientLevel,
    ) {
        val equippedItem = player.inventory.getItem(3)

        val item = equippedItem?.item

        if (item !is MapItem) {
            // reset lastMap when map was removed (no longer in game)
            lastMap = null
            return
        }

        val mapId = equippedItem.get(DataComponents.MAP_ID)
        val mapState = mapId?.let { world.getMapData(it) } ?: return

        if (mapId == lastMap) {
            return
        }

        lastMap = mapId

        val outs = MurderMysteryFontDetection.readContractLine(mapState)

        val s = outs.split(' ').toTypedArray()

        if (s.isNotEmpty() && s[0].startsWith("NAME:")) {
            val target = s[0].substring("NAME:".length).lowercase(Locale.getDefault()).trim()
            val targetPlayer = findPlayerWithClosestName(target, player)

            if (targetPlayer != null) {
                currentAssassinationTarget = targetPlayer.profile.id

                chat("Target: " + targetPlayer.profile.name)
            } else {
                chat("Failed to find target, but the name is: $target")
            }
        }
    }

    private fun findPlayerWithClosestName(
        name: String,
        player: LocalPlayer,
    ): PlayerInfo? {
        return player.connection.onlinePlayers.minByOrNull { netInfo ->
            levenshtein(name, netInfo.profile.name.lowercase().trim())
        }
    }

    override fun handleHasBow(
        entity: AbstractClientPlayer,
        locationSkin: Identifier,
    ) {
        // Nobody has a bow in this game mode
    }

    override fun handleHasSword(
        entity: AbstractClientPlayer,
        locationSkin: Identifier,
    ) {
        // Everyone has a sword in this game mode
    }

    override fun shouldAttack(entity: AbstractClientPlayer): Boolean {
        // This person is either our assasin or our target. Attack them.
        return this.getPlayerType(entity) == PlayerType.MURDERER
    }

    override fun getPlayerType(player: AbstractClientPlayer): PlayerType {
        if (player.gameProfile.id == currentAssassinationTarget || player.gameProfile.id == currentAssassin) {
            return PlayerType.MURDERER
        }

        return PlayerType.NEUTRAL
    }

    override fun reset() {
        this.currentAssassinationTarget = null
        this.currentAssassin = null
    }
}
