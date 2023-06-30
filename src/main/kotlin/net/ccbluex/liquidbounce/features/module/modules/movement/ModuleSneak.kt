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

package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.PlayerNetworkMovementTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.pressedOnKeyboard
import net.ccbluex.liquidbounce.utils.entity.moving
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket

/**
 * Sneak module
 *
 * Automatically sneaks all the time.
 */

object ModuleSneak : Module("Sneak", Category.MOVEMENT) {

    var modes = choices("Mode", Vanilla, arrayOf(Legit, Vanilla, Switch))
    var stopMove by boolean("StopMove", false)
    var sneaking = false

    private object Legit : Choice("Legit") {

        override val parent: ChoiceConfigurable
            get() = modes

        val networkTick = handler<PlayerNetworkMovementTickEvent> {
            if (stopMove && player.moving) {
                if (sneaking) {
                    disable()
                } else return@handler
            }
            mc.options.sneakKey.isPressed = true
        }

        override fun disable() {
            if (!mc.options.sneakKey.pressedOnKeyboard) {
                mc.options.sneakKey.isPressed = false
                sneaking = false
            }
        }
    }

    private object Vanilla : Choice("Vanilla") {

        override val parent: ChoiceConfigurable
            get() = modes

        val networkTick = handler<PlayerNetworkMovementTickEvent> {
            if (stopMove && player.moving) {
                if (sneaking) {
                    disable()
                } else return@handler
            }
            if (!sneaking) {
                network.sendPacket(ClientCommandC2SPacket(player, ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY))
            }
        }

        override fun disable() {
            network.sendPacket(ClientCommandC2SPacket(player, ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY))
            sneaking = false
        }
    }

    private object Switch : Choice("Switch") {

        override val parent: ChoiceConfigurable
            get() = modes

        val networkTick = handler<PlayerNetworkMovementTickEvent> { event ->
            if (stopMove && player.moving) {
                if (sneaking) {
                    disable()
                } else return@handler
            }
            when (event.state) {
                EventState.PRE -> {
                    network.sendPacket(ClientCommandC2SPacket(player, ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY))
                    network.sendPacket(ClientCommandC2SPacket(player, ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY))
                }
                EventState.POST -> {
                    network.sendPacket(ClientCommandC2SPacket(player, ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY))
                    network.sendPacket(ClientCommandC2SPacket(player, ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY))
                }
            }
        }

        override fun disable() {
            network.sendPacket(ClientCommandC2SPacket(player, ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY))
            sneaking = false
        }
    }
}
