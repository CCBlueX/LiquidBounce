@file:Suppress("ReplaceSizeCheckWithIsNotEmpty")

package net.ccbluex.liquidbounce.features.module.modules.misc

import com.mojang.authlib.GameProfile
import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.NotificationEvent
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.notification
import net.minecraft.entity.Entity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket

object ModuleAntiBot : Module("AntiBot", Category.MISC) {

    private val modes = choices("Mode", Custom, arrayOf(Custom, Matrix))

    private var pName: String? = null

    private object Custom : Choice("Custom") {
        override val parent: ChoiceConfigurable
            get() = modes

        // This part is up to 1zuna or superblauberee, so I don't mess things up.
        // Basically a multiple antibot option dependent mode.
    }

    private object Matrix : Choice("Matrix") {
        override val parent: ChoiceConfigurable
            get() = modes

        val packetHandler = handler<PacketEvent> { event ->
            if (event.packet is PlayerListS2CPacket && event.packet.action == PlayerListS2CPacket.Action.ADD_PLAYER) {
                for (entry in event.packet.entries) {
                    if (entry.latency < 2 || entry.profile.name.length < 3 || !entry.profile.properties.isEmpty || isTheSamePlayer(entry.profile)) {
                        continue
                    }

                    if (isADuplicate(entry.profile)) {
                        event.cancelEvent()
                        notification("AntiBot", "Removed ${entry.profile.name}", NotificationEvent.Severity.INFO)
                        continue
                    }

                    pName = entry.profile.name
                }
            }
        }

        val repeatable = repeatable {
            if (pName == null) {
                return@repeatable
            }

            for (entity in world.entities) {
                if (entity is PlayerEntity && entity.entityName == pName) {
                    if (!isArmored(entity)) {
                        pName = null
                        continue
                    }

                    world.removeEntity(entity.id, Entity.RemovalReason.DISCARDED)
                    notification("AntiBot", "Removed $pName", NotificationEvent.Severity.INFO)
                    pName = null
                }
            }
        }

        private fun isADuplicate(profile: GameProfile): Boolean {
            return network.playerList.count { it.profile.name == profile.name && it.profile.id != profile.id } == 1
        }

        private fun isArmored(entity: PlayerEntity): Boolean {
            for (i in 0..3) {
                return !entity.inventory.getArmorStack(i).isEmpty
            }
            return false
        }

        private fun isTheSamePlayer(profile: GameProfile): Boolean {
            // Prevents false positives when a player joins a minigame such as Practice
            return network.playerList.count { it.profile.name == profile.name && it.profile.id == profile.id } == 1
        }
    }
}
