package net.ccbluex.liquidbounce.features.module.modules.render.murdermystery

import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.math.levenshtein
import net.minecraft.client.network.AbstractClientPlayerEntity
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.client.network.PlayerListEntry
import net.minecraft.client.world.ClientWorld
import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.MapIdComponent
import net.minecraft.item.FilledMapItem
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket
import net.minecraft.util.Identifier
import java.util.*
import kotlin.math.absoluteValue

object MurderMysteryAssassinationMode : Choice("Assassination"), MurderMysteryMode {
    override val parent
        get() = ModuleMurderMystery.modes

    private var lastMap: MapIdComponent? = null
    private var currentAssassinationTarget: UUID? = null
    private var currentAssassin: UUID? = null

    val packetHandler =
        handler<PacketEvent> { packetEvent ->
            val world = mc.world ?: return@handler

            if (packetEvent.packet is PlaySoundS2CPacket) {
                val packet = packetEvent.packet

                if (packet.sound.value().id.toString() != "minecraft:block.note_block.basedrum") {
                    return@handler
                }

                val expectedDistance = calculateDistanceFromWarningVolume(packet.volume)

                val probablyAssassin =
                    world.players.minByOrNull {
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
        player: ClientPlayerEntity,
        world: ClientWorld,
    ) {
        val equippedItem = player.inventory.getStack(3)

        val item = equippedItem?.item

        if (item !is FilledMapItem) {
            // reset lastMap when map was removed (no longer in game)
            lastMap = null
            return
        }

        val mapId = equippedItem.get(DataComponentTypes.MAP_ID)
        val mapState = mapId?.let { world.getMapState(it) } ?: return

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
        player: ClientPlayerEntity,
    ): PlayerListEntry? {
        return player.networkHandler.playerList.minByOrNull { netInfo ->
            levenshtein(name, netInfo.profile.name.lowercase().trim())
        }
    }

    override fun handleHasBow(
        entity: AbstractClientPlayerEntity,
        locationSkin: Identifier,
    ) {
        // Nobody has a bow in this game mode
    }

    override fun handleHasSword(
        entity: AbstractClientPlayerEntity,
        locationSkin: Identifier,
    ) {
        // Everyone has a sword in this game mode
    }

    override fun shouldAttack(entity: AbstractClientPlayerEntity): Boolean {
        // This person is either our assasin or our target. Attack them.
        return this.getPlayerType(entity) == MurderMysteryMode.PlayerType.MURDERER
    }

    override fun getPlayerType(player: AbstractClientPlayerEntity): MurderMysteryMode.PlayerType {
        if (player.gameProfile.id == currentAssassinationTarget || player.gameProfile.id == currentAssassin) {
            return MurderMysteryMode.PlayerType.MURDERER
        }

        return MurderMysteryMode.PlayerType.NEUTRAL
    }

    override fun reset() {
        this.currentAssassinationTarget = null
        this.currentAssassin = null
    }
}
