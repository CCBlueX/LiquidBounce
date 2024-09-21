/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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
package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.config.NoneChoice
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket

/**
 * FastBreak module
 *
 * Allows you to break blocks faster.
 */
object ModuleFastBreak : Module("FastBreak", Category.WORLD) {

    private val breakDamage by float("BreakDamage", 0.8f, 0.1f..1f)

    private val modeChoice = choices<Choice>("Mode", { it.choices[0] }, { arrayOf(NoneChoice(it), AbortAnother) }).apply { tagBy(this) }


    val repeatable = repeatable {
        interaction.blockBreakingCooldown = 0

        if (interaction.currentBreakingProgress > breakDamage) {
            interaction.currentBreakingProgress = 1f
        }
    }

    /**
     * Bypass Grim 2.3.48 anti-cheat
     * Tested on eu.loyisa.cn
     *
     * https://github.com/GrimAnticheat/Grim/issues/1296
     */
    object AbortAnother : Choice("AbortAnother") {

        override val parent: ChoiceConfigurable<Choice>
            get() = modeChoice

        val packetHandler = handler<PacketEvent> {
            val packet = it.packet

            if (packet is PlayerActionC2SPacket && packet.action == PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK) {
                val blockPos = packet.pos ?: return@handler

                // Abort block break on the block above (which we are not breaking)
                network.sendPacket(PlayerActionC2SPacket(PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK,
                    blockPos.up(), packet.direction))
            }
        }

    }

}
