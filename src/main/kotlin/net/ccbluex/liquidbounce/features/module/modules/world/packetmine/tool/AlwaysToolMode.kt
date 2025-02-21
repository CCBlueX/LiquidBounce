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
package net.ccbluex.liquidbounce.features.module.modules.world.packetmine.tool

import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.SelectHotbarSlotSilentlyEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.world.ModuleAutoTool
import net.ccbluex.liquidbounce.features.module.modules.world.packetmine.MineTarget
import net.ccbluex.liquidbounce.features.module.modules.world.packetmine.ModulePacketMine
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket
import net.minecraft.network.packet.s2c.play.UpdateSelectedSlotS2CPacket

object AlwaysToolMode : MineToolMode("Always", syncOnStart = true) {

    private val abortOnSwitch by boolean("AbortOnSwitch", true)
    private val cancelAutomaticSwitching by boolean("CancelAutomaticSwitching", true)

    @Suppress("unused")
    private val packetHandler  = handler<PacketEvent> { event ->
        mc.execute {
            val target = ModulePacketMine._target ?: return@execute
            if (!abortOnSwitch || !target.started) {
                return@execute
            }

            val packet = event.packet
            val serverInitiatedSwitch = packet is UpdateSelectedSlotS2CPacket &&
                packet.slot == getSlot(target.blockState)?.firstInt()
            val clientInitiatedSwitch = packet is UpdateSelectedSlotC2SPacket &&
                packet.selectedSlot == getSlot(target.blockState)?.firstInt()
            if (serverInitiatedSwitch || clientInitiatedSwitch) {
                ModulePacketMine._resetTarget()
            }
        }
    }

    @Suppress("unused")
    private val silentSwitchHandler = handler<SelectHotbarSlotSilentlyEvent> { event ->
        val target = ModulePacketMine._target ?: return@handler

        val requester = event.requester
        val fromPacketMine = requester == ModulePacketMine
        val fromAutoTool = requester == ModuleAutoTool && event.slot == getSlot(target.blockState)?.firstInt()
        if (cancelAutomaticSwitching && target.started && !fromPacketMine && !fromAutoTool) {
            event.cancelEvent()
        }
    }

    override fun shouldSwitch(mineTarget: MineTarget) = true

}
