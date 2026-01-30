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
package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.fastutil.objectRBTreeSetOf
import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.event.waitTicks
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug.debugParameter
import net.ccbluex.liquidbounce.utils.collection.asComparator
import net.ccbluex.liquidbounce.utils.math.sq
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.protocol.game.ClientboundSoundPacket
import net.minecraft.network.protocol.game.ServerboundUseItemPacket
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.FishingRodItem

/**
 * AutoFish module
 *
 * Automatically catches fish when using a rod.
 */

object ModuleAutoFish : ClientModule("AutoFish", ModuleCategories.PLAYER) {

    private val reelDelay by intRange("ReelDelay", 5..8, 0..20, "ticks")

    private object RecastRod : ToggleableValueGroup(this, "RecastRod", true) {
        val delay by intRange("Delay", 15..20, 10..30, "ticks")
    }

    /**
     * Usually we only require [SoundEvents.FISHING_BOBBER_SPLASH]
     * to trigger the pull, but if a server has a custom sound,
     * we might want to add it here.
     */
    private val sounds by sounds(
        "Sounds", objectRBTreeSetOf(
            BuiltInRegistries.SOUND_EVENT.asComparator(),
            SoundEvents.FISHING_BOBBER_SPLASH,
        )
    )

    /**
     * This is useful to prevent false triggers when the sound is played
     * from a different position than our fishing hook.
     */
    private object PullTriggerSoundDistance : ToggleableValueGroup(
        this,
        "SoundDistance",
        true
    ) {
        val distance by float("MaxDistance", 1.0f, 0.0f..10.0f, "blocks")
    }

    init {
        tree(PullTriggerSoundDistance)
        tree(RecastRod)
    }

    private var caughtFish = false

    override fun onDisabled() {
        caughtFish = false
    }

    @Suppress("unused")
    private val tickHandler = tickHandler {
        if (!caughtFish) {
            return@tickHandler
        }

        for (hand in InteractionHand.entries) {
            if (player.getItemBySlot(hand.asEquipmentSlot()).item !is FishingRodItem) {
                continue
            }

            waitTicks(reelDelay.random())
            interaction.startPrediction(world) { sequence ->
                ServerboundUseItemPacket(hand, sequence, player.yRot, player.xRot)
            }

            player.swing(hand)

            if (RecastRod.enabled) {
                waitTicks(RecastRod.delay.random())
                interaction.startPrediction(world) { sequence ->
                    ServerboundUseItemPacket(hand, sequence, player.yRot, player.xRot)
                }
                player.swing(hand)
            }
            break
        }

        caughtFish = false
    }

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent> { event ->
        val packet = event.packet
        val fishHook = player.fishing ?: return@handler
        if (fishHook.isRemoved) {
            return@handler
        }

        if (packet is ClientboundSoundPacket && packet.sound.value() in sounds) {
            if (PullTriggerSoundDistance.running) {
                val hookToSoundSq = fishHook.position().distanceToSqr(packet.x, packet.y, packet.z)
                debugParameter("HookToSoundSq") { hookToSoundSq }

                // From my testing, we should see distances around 0.04 - 0.08 (Paper version 1.21.1-132)
                // so a threshold of 1.0 should be more than enough.
                if (hookToSoundSq > PullTriggerSoundDistance.distance.sq()) {
                    return@handler
                }
            }

            caughtFish = true
        }
    }

}
