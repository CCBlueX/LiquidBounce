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
package net.ccbluex.liquidbounce.features.module.modules.combat.criticals.modes

import net.ccbluex.liquidbounce.config.types.group.Mode
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.event.events.BlinkPacketEvent
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.TransferOrigin
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.blink.BlinkManager
import net.ccbluex.liquidbounce.features.module.modules.combat.criticals.ModuleCriticals
import net.ccbluex.liquidbounce.features.module.modules.combat.criticals.ModuleCriticals.wouldDoCriticalHit
import net.ccbluex.liquidbounce.utils.combat.findEnemy
import net.minecraft.network.protocol.common.ServerboundResourcePackPacket
import net.minecraft.network.protocol.game.ServerboundInteractPacket
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket
import net.minecraft.network.protocol.game.ServerboundSignUpdatePacket
import net.minecraft.network.protocol.game.ServerboundSwingPacket
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket

object CriticalsBlink : Mode("Blink") {

    override val parent: ModeValueGroup<*>
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
    private val fakeLagHandler = handler<BlinkPacketEvent> { event ->
        if (event.origin == TransferOrigin.OUTGOING && !wouldDoCriticalHit(ignoreSprint = true) && enemyInRange) {
            if (BlinkManager.isAboveTime(nextDelay.toLong())) {
                nextDelay = delay.random()
                return@handler
            }

            event.action = when (event.packet) {
                is ServerboundUseItemOnPacket,
                is ServerboundPlayerActionPacket,
                is ServerboundSignUpdatePacket,
                is ServerboundInteractPacket,
                is ServerboundSwingPacket,
                is ServerboundResourcePackPacket -> BlinkManager.Action.PASS
                else -> BlinkManager.Action.QUEUE
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


