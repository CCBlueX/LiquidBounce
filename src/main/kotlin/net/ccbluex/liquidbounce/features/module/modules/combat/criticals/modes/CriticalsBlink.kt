/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
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
package net.ccbluex.liquidbounce.features.module.modules.combat.criticals.modes

import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.QueuePacketEvent
import net.ccbluex.liquidbounce.event.events.TransferOrigin
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.combat.criticals.ModuleCriticals
import net.ccbluex.liquidbounce.features.module.modules.combat.criticals.ModuleCriticals.wouldDoCriticalHit
import net.ccbluex.liquidbounce.utils.client.PacketQueueManager
import net.ccbluex.liquidbounce.utils.combat.findEnemy
import net.minecraft.network.packet.c2s.common.ResourcePackStatusC2SPacket
import net.minecraft.network.packet.c2s.play.*

object CriticalsBlink : Choice("Blink") {

    override val parent: ChoiceConfigurable<*>
        get() = ModuleCriticals.modes

    private val delay by intRange("Delay", 300..600, 0..1000, "ms")
    private val range by float("Range", 4.0f, 0.0f..10.0f)
    private var nextDelay = delay.random()

    var isInState = false
    private var enemyInRange = false

    @Suppress("unused")
    private val tickHandler = handler<GameTickEvent> {
        enemyInRange = world.findEnemy(0.0f..range) != null
    }

    @Suppress("unused")
    private val fakeLagHandler = handler<QueuePacketEvent> { event ->
        if (event.origin == TransferOrigin.OUTGOING && !wouldDoCriticalHit(ignoreSprint = true) && enemyInRange) {
            if (PacketQueueManager.isAboveTime(nextDelay.toLong())) {
                nextDelay = delay.random()
                return@handler
            }

            event.action = when (event.packet) {
                is PlayerInteractBlockC2SPacket,
                is PlayerActionC2SPacket,
                is UpdateSignC2SPacket,
                is PlayerInteractEntityC2SPacket,
                is HandSwingC2SPacket,
                is ResourcePackStatusC2SPacket -> PacketQueueManager.Action.PASS
                else -> PacketQueueManager.Action.QUEUE
            }
            isInState = true
        } else {
            isInState = false
        }
    }

    override fun disable() {
        isInState = false
        super.disable()
    }

}


