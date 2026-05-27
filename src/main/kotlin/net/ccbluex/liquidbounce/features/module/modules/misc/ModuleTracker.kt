package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.event.events.NotificationEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.PlayerTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.misc.FriendManager
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.script.bindings.api.ScriptClient
import net.ccbluex.liquidbounce.utils.client.notification
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.ItemUseAnimation
import net.minecraft.core.component.DataComponents
import java.util.*

object ModuleTracker : ClientModule("Tracker", ModuleCategories.MISC) {

    private val messagePlace by enumChoice("MessagePlace", Modes.CHAT)
    private val range by int("Range", 64, 8..512)
    private val totemUse by boolean("TotemUse", true)
    private val potionUse by boolean("PotionUse", true)
    private val eatUse by boolean("EatUse", true)

    private val playerEffects = mutableMapOf<UUID, MutableMap<String, MobEffectInstance>>()
    private val activeUseItem = mutableMapOf<UUID, ItemStack>()
    private val useStartTick = mutableMapOf<UUID, Int>()
    private val popCounter = mutableMapOf<UUID, Int>()
    private var lastUpdateTime = System.currentTimeMillis()
    private val recentPops = mutableSetOf<UUID>()
    private const val UPDATE_INTERVAL_MS = 500L

    @Suppress("unused")
    private val onTick = handler<PlayerTickEvent> {
        val world = mc.level ?: return@handler
        val localPlayer = mc.player ?: return@handler

        if (eatUse) {
            for (player in world.players()) {
                if (player == localPlayer || FriendManager.isFriend(player.name.string)) continue
                val distance = localPlayer.distanceToSqr(player)
                if (distance > range * range) continue

                val id = player.uuid
                if (player.isUsingItem) {
                    if (!activeUseItem.containsKey(id)) {
                        activeUseItem[id] = player.useItem.copy()
                        useStartTick[id] = player.tickCount
                    }
                } else {
                    val used = activeUseItem.remove(id)
                    val startTick = useStartTick.remove(id)

                    if (used != null && !used.isEmpty && startTick != null) {
                        val action = used.useAnimation
                        val verb = when (action) {
                            ItemUseAnimation.DRINK -> "drank"
                            ItemUseAnimation.EAT -> "ate"
                            else -> null
                        }

                        if (verb != null && (player.tickCount - startTick) >= 31) {
                            val itemName = used.hoverName.string
                            val effectsStr = getEffectsString(used)
                            val effectsPart = if (effectsStr.isEmpty()) "" else " ($effectsStr)"

                            when(messagePlace) {

                                Modes.CHAT ->
                                    ScriptClient.displayChatMessage("${player.name.string} $verb $itemName$effectsPart")

                                Modes.NOTIFICATIONS -> notification(
                                    "Tracker",
                                    "${player.name.string} $verb $itemName$effectsPart",
                                    NotificationEvent.Severity.SUCCESS
                                )

                            }
                        }
                    }
                }
            }
        }

        if (System.currentTimeMillis() - lastUpdateTime >= UPDATE_INTERVAL_MS) {
            lastUpdateTime = System.currentTimeMillis()
            recentPops.clear()

            val currentPlayers = world.players().map { it.uuid }.toSet()
            activeUseItem.keys.retainAll(currentPlayers)
            useStartTick.keys.retainAll(currentPlayers)
            playerEffects.keys.retainAll(currentPlayers)
            popCounter.keys.retainAll(currentPlayers)

            for (player in world.players()) {
                if (player == localPlayer) continue

                val effectsMap = mutableMapOf<String, MobEffectInstance>()
                player.activeEffects.forEach {
                    val key = "${it.effect.value().descriptionId}:${it.amplifier}"
                    effectsMap[key] = it
                }
                playerEffects[player.uuid] = effectsMap

                if (player.isDeadOrDying) popCounter.remove(player.uuid)
            }
        }
    }

    @Suppress("unused")
    private val onPacket = handler<PacketEvent> { e ->
        val world = mc.level ?: return@handler
        val packet = e.packet

        if (packet is ClientboundUpdateMobEffectPacket && potionUse) {
            val entity = world.getEntity(packet.entityId) as? Player ?: return@handler
            if (entity == mc.player || FriendManager.isFriend(entity.name.string)) return@handler
            val distance = player.distanceToSqr(entity)
            if (distance > range * range) return@handler

            val effectHolder = packet.effect
            val name = Component.translatable(effectHolder.value().descriptionId).string
            val lvl = packet.effectAmplifier + 1
            val dur = getDurationString(packet.effectDurationTicks)

            when(messagePlace) {

                Modes.CHAT -> ScriptClient.displayChatMessage("${entity.name.string} get $name $lvl for $dur")

                Modes.NOTIFICATIONS -> notification(
                    "Tracker",
                    "${entity.name.string} get $name $lvl for $dur",
                    NotificationEvent.Severity.SUCCESS
                )
            }
        }

        if (packet is ClientboundEntityEventPacket && totemUse) {
            if (packet.eventId.toInt() == 35) {
                val entity = packet.getEntity(world) as? Player ?: return@handler
                if (entity == mc.player || FriendManager.isFriend(entity.name.string)) return@handler
                if (recentPops.contains(entity.uuid)) return@handler
                val distance = player.distanceToSqr(entity)
                if (distance > range * range) return@handler

                recentPops.add(entity.uuid)
                val pops = popCounter.getOrDefault(entity.uuid, 0) + 1
                popCounter[entity.uuid] = pops

                when(messagePlace) {

                    Modes.CHAT -> ScriptClient.displayChatMessage("${entity.name.string} pop totem $pops times")

                    Modes.NOTIFICATIONS -> notification(
                        "Tracker",
                        "${entity.name.string} pop totem $pops times",
                        NotificationEvent.Severity.SUCCESS
                    )

                }
            }
        }
    }

    private fun getEffectsString(stack: ItemStack): String {
        val contents = stack.get(DataComponents.POTION_CONTENTS) ?: return ""
        val sb = StringBuilder()

        contents.allEffects.forEach { effectInstance ->

            val name = Component.translatable(effectInstance.effect.value().descriptionId).string
            val lvl = effectInstance.amplifier + 1
            val dur = getDurationString(effectInstance.duration)

            sb.append("$name $lvl for $dur")
        }

        return sb.toString()
    }

    private fun getDurationString(ticks: Int): String {
        if (ticks !in 0..32767 && ticks != -1) return "∞"

        val seconds = ticks / 20
        val m = seconds / 60
        val s = seconds % 60
        return if (m > 0) "${m}m ${s}s" else "${s}s"
    }

    private enum class Modes(override val tag: String) : Tagged {
        NOTIFICATIONS("Notifications"),
        CHAT("Chat"),
    }
}
