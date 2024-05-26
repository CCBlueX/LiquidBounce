/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2024 CCBlueX
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
 *
 *
 */

package net.ccbluex.liquidbounce.features.module.modules.movement.fly.modes.sentinel

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.NotificationEvent
import net.ccbluex.liquidbounce.event.events.PlayerMoveEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.modules.exploit.ModulePingSpoof
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.ModuleFly
import net.ccbluex.liquidbounce.lang.translation
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.notification
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.entity.strafe
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket

/**
 * @anticheat Sentinel
 * @anticheatVersion 20.04.2024
 * @testedOn cubecraft.net
 *
 * @note Tested in SkyWars - fly as long as you want. REQUIRES PING SPOOF TO BE ENABLED.
 *
 * Thanks to the_bi11iona1re for making me aware that Sentinal folds to Verus Damage exploit.
 */
internal object FlySentinel20thApr : Choice("Sentinel20thApr") {

    private val horizontalSpeed by float("HorizontalSpeed", 3.5f, 0.1f..10f)
    private val constantSpeed by boolean("ConstantSpeed", false)
    private val verticalSpeed by float("VerticalSpeed", 0.7f, 0.1f..1f)
    private val reboostTicks by int("ReboostTicks", 30, 10..50)

    override val parent: ChoiceConfigurable<*>
        get() = ModuleFly.modes

    private var hasBeenHurt = false

    override fun enable() {
        if (!ModulePingSpoof.enabled) {
            ModulePingSpoof.enabled = true
        }
        hasBeenHurt = false

        chat(regular(translation("liquidbounce.module.fly.messages.cubecraft20thAprBoostUsage")))
        super.enable()
    }

    val repeatable = repeatable {
        boost()
        waitTicks(reboostTicks)
    }

    val moveHandler = handler<PlayerMoveEvent> { event ->
        if (player.hurtTime > 0  && !hasBeenHurt) {
            hasBeenHurt = true
            player.strafe(speed = horizontalSpeed.toDouble())
            notification(
                "Fly",
                translation("liquidbounce.module.fly.messages.cubecraft20thAprBoostMessage"),
                NotificationEvent.Severity.INFO
            )
        }

        if (!hasBeenHurt) {
            return@handler
        }

        event.movement.y = when {
            player.input.jumping -> verticalSpeed.toDouble()
            player.input.sneaking -> (-verticalSpeed).toDouble()
            else -> 0.0
        }

        if (constantSpeed) {
            event.movement.strafe(speed = horizontalSpeed.toDouble(), keyboardCheck = true)
        }
    }

    private fun boost() {
        hasBeenHurt = false
        network.sendPacket(PlayerMoveC2SPacket.PositionAndOnGround(player.x, player.y, player.z, false))
        network.sendPacket(PlayerMoveC2SPacket.PositionAndOnGround(player.x, player.y + 3.25, player.z,
            false))
        network.sendPacket(PlayerMoveC2SPacket.PositionAndOnGround(player.x, player.y, player.z, false))
        network.sendPacket(PlayerMoveC2SPacket.PositionAndOnGround(player.x, player.y, player.z, true))
    }



}
