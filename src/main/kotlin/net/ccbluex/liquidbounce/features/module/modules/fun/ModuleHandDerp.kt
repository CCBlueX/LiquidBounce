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
package net.ccbluex.liquidbounce.features.module.modules.`fun`

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.event.sequenceHandler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.injection.mixins.minecraft.entity.MixinPlayerEntityAccessor
import net.minecraft.network.packet.c2s.common.ClientOptionsC2SPacket
import net.minecraft.network.packet.c2s.common.SyncedClientOptions
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket


/**
 * Hand derp module
 *
 * Switches your main hand.
 */
object ModuleHandDerp : Module("HandDerp", Category.FUN) {


    private val silent by boolean("Silent", false)
    private val mode = choices("Mode", Delay, arrayOf(Delay, Swing))


    private val originalHand = mc.options.mainArm.value
    private var currentHand = mc.options.mainArm.value

    private fun calculatePlayerPartValue(): Int {
        var value = 0
        for (part in mc.options.enabledPlayerModelParts) {
            value = value or (1 shl part.ordinal)
        }
        return value
    }

    private fun switchHand() {

        currentHand = currentHand.opposite
        network.sendPacket(
            ClientOptionsC2SPacket(
                SyncedClientOptions(
                    mc.options.language,
                    mc.options.viewDistance.value,
                    mc.options.chatVisibility.value,
                    mc.options.chatColors.value,
                    calculatePlayerPartValue(),
                    currentHand,
                    mc.shouldFilterText(),
                    mc.options.allowServerListing.value
                )
            )
        )

    }

    val packetHandler = sequenceHandler<PacketEvent>(priority = 1) {
        val packet = it.packet
        if (silent && packet is EntityTrackerUpdateS2CPacket &&
            packet.trackedValues.any { data ->
                data.id == MixinPlayerEntityAccessor.getTrackedMainArm().id }) {
            it.cancelEvent()
        }
    }

    override fun disable() {
        if (mc.options.mainArm.value != originalHand) {
            switchHand()
        }
    }

    private object Delay : Choice("Delay") {
        override val parent: ChoiceConfigurable<Choice>
            get() = mode

        val delayValue by int("Delay", 1, 0..20, "ticks")

        @Suppress("unused")
        val repeatable = repeatable {
            waitTicks(delayValue)
            switchHand()
        }
    }

    private object Swing : Choice("Swing") {
        override val parent: ChoiceConfigurable<Choice>
            get() = mode

        @Suppress("unused")
        val packetHandler = sequenceHandler<PacketEvent>(priority = 1) {
            val packet = it.packet
            if (packet is HandSwingC2SPacket) {
                switchHand()
            }
        }
    }

}
