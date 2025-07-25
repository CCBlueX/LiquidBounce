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
 *
 *
 */
package net.ccbluex.liquidbounce.features.module.modules.player.antivoid.mode

import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.QueuePacketEvent
import net.ccbluex.liquidbounce.event.events.TransferOrigin
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.player.antivoid.ModuleAntiVoid
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.ccbluex.liquidbounce.utils.client.PacketQueueManager
import net.ccbluex.liquidbounce.utils.client.PacketQueueManager.Action
import net.ccbluex.liquidbounce.utils.client.PacketQueueManager.positions

object AntiVoidBlinkMode : AntiVoidMode("Blink") {

    override val parent: ChoiceConfigurable<*>
        get() = ModuleAntiVoid.mode

    // Cases in which the AntiVoid protection should not be active.
    override val isExempt
        get() = super.isExempt || ModuleScaffold.running

    // Whether artificial lag is needed to prevent falling into the void.
    private val requiresLag
        get() = running && ModuleAntiVoid.isLikelyFalling && ModuleAntiVoid.rescuePosition != null && !isExempt

    @Suppress("unused")
    private val fakeLagHandler = handler<QueuePacketEvent> { event ->
        if (event.origin == TransferOrigin.OUTGOING && requiresLag) {
            event.action = Action.QUEUE
        }
    }

    /**
     * This method is called to discover a safe position to teleport to.
     * In this case, we simply return the last known safe position.
     *
     * TODO: This does not seem to be consistent enough,
     *   so we rather rely on the base [discoverRescuePosition] method.
     */
//    override fun discoverRescuePosition(): Vec3d? {
//        return positions.first { pos -> ModuleAntiVoid.isSafeForRescue(pos) }
//    }

    override fun rescue(): Boolean {
        if (!requiresLag) {
            return false
        }

        // Check if we have any previous positions to teleport to.
        if (positions.isEmpty()) {
            return false
        }

        PacketQueueManager.cancel()
        return true
    }

}
