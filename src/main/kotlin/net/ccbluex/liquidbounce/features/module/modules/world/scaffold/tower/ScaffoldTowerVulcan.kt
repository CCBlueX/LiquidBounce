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
package net.ccbluex.liquidbounce.features.module.modules.world.scaffold.tower

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.PlayerJumpEvent
import net.ccbluex.liquidbounce.event.events.PlayerNetworkMovementTickEvent
import net.ccbluex.liquidbounce.event.events.PlayerTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold.isBlockBelow
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold.towerMode
import net.ccbluex.liquidbounce.utils.entity.moving
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.stat.Stats
import java.util.*
import kotlin.jvm.optionals.getOrNull
import kotlin.math.truncate

object ScaffoldTowerVulcan : Choice("Vulcan") {

    override val parent: ChoiceConfigurable<Choice>
        get() = towerMode

    val repeatable = repeatable {
        if (!mc.options.jumpKey.isPressed || ModuleScaffold.blockCount <= 0 || !isBlockBelow) {
            return@repeatable
        }

        if(player.age % 2 == 0) {
            player.velocity.y = 0.7
        } else {
            player.velocity.y = if(player.moving) 0.42f.toDouble() else 0.6
        }
    }

    val packetHandler = handler<PacketEvent> { event ->
        val packet = event.packet
        if(packet is PlayerMoveC2SPacket) {
            if(!player.moving) {
                if(player.age % 2 == 0) {
                    packet.x += 0.1
                    packet.z += 0.1
                }
            }
        }
    }
}
