package net.ccbluex.liquidbounce.features.module.modules.misc.antibot.modes

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.ModuleAntiBot
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket
import net.minecraft.network.packet.s2c.play.PlayerRemoveS2CPacket
import java.util.*

object HorizonAntiBotMode : Choice("Horizon"), ModuleAntiBot.IAntiBotMode {
    override val parent: ChoiceConfigurable
        get() = ModuleAntiBot.modes

    private val botList = HashSet<UUID>()

    val packetHandler = handler<PacketEvent> {
        when (val packet = it.packet) {
            is PlayerListS2CPacket -> {
                if (packet.actions.first() == PlayerListS2CPacket.Action.ADD_PLAYER) {
                    for (entry in packet.entries) {
                        if (entry.gameMode != null) {
                            continue
                        }

                        botList.add(entry.profileId)
                    }
                }
            }

            is PlayerRemoveS2CPacket -> {
                for (id in packet.profileIds) {
                    if (botList.contains(id)) {
                        botList.remove(id)
                    }
                }
            }
        }
    }

    override fun isBot(entity: PlayerEntity): Boolean {
        return botList.contains(player.uuid)
    }

    override fun reset() {
        botList.clear()
    }
}
