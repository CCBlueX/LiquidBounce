/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2023 CCBlueX
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

import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.minecraft.entity.EquipmentSlot
import net.minecraft.item.FishingRodItem
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket
import net.minecraft.sound.SoundEvents
import net.minecraft.util.Hand

/**
 * AutoFish module
 *
 * Automatically catches fish when using a rod.
 */

object ModuleAutoFish : Module("AutoFish", Category.PLAYER) {

    private val reelDelay by intRange("ReelDelay", 5..8, 0..20)

    private object RecastRod : ToggleableConfigurable(this, "RecastRod", true) {
        val delay by intRange("Delay", 15..20, 10..30)
    }

    init {
        tree(RecastRod)
    }

    private var caughtFish = false

    override fun disable() {
        caughtFish = false
    }

    val repeatable = repeatable {
        if (caughtFish) {
            for (hand in arrayOf(Hand.MAIN_HAND, Hand.OFF_HAND)) {
                if (player.getEquippedStack(hand.equipmentSlot).item !is FishingRodItem) {
                    continue
                }

                wait(reelDelay.random())
                interaction.sendSequencedPacket(world) { sequence ->
                    PlayerInteractItemC2SPacket(hand, sequence)
                }

                player.swingHand(hand)

                if (RecastRod.enabled) {
                    wait(RecastRod.delay.random())
                    interaction.sendSequencedPacket(world) { sequence ->
                        PlayerInteractItemC2SPacket(hand, sequence)
                    }
                    player.swingHand(hand)
                }

                caughtFish = false
            }
        }
    }

    val packetHandler = handler<PacketEvent> { event ->
        if (player.fishHook == null) {
            return@handler
        }

        if (event.packet !is PlaySoundS2CPacket || event.packet.sound.value() != SoundEvents.ENTITY_FISHING_BOBBER_SPLASH) {
            return@handler
        }

        caughtFish = true
    }

    private val Hand.equipmentSlot: EquipmentSlot
        get() = when (this) {
            Hand.MAIN_HAND -> EquipmentSlot.MAINHAND
            Hand.OFF_HAND -> EquipmentSlot.OFFHAND
        }

}
