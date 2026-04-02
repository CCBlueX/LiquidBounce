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
package net.ccbluex.liquidbounce.features.module.modules.world.nuker.mode

import net.ccbluex.liquidbounce.config.types.group.Mode
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.event.events.RotationUpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.world.nuker.ModuleNuker
import net.ccbluex.liquidbounce.features.module.modules.world.nuker.ModuleNuker.areaMode
import net.ccbluex.liquidbounce.features.module.modules.world.nuker.ModuleNuker.mode
import net.ccbluex.liquidbounce.features.module.modules.world.nuker.ModuleNuker.wasTarget
import net.ccbluex.liquidbounce.features.module.modules.world.packetmine.ModulePacketMine
import net.ccbluex.liquidbounce.utils.block.breaker.BlockBreaker
import net.ccbluex.liquidbounce.utils.block.isNotBreakable
import net.ccbluex.liquidbounce.utils.block.state
import net.minecraft.core.BlockPos
import java.util.function.BooleanSupplier

object LegitNukerMode : Mode("Legit") {

    private var currentTarget: BlockPos? = null
    private val blockBreaker = tree(BlockBreaker(
        "Breaker",
        this,
        ignoreOpenInventorySupplier = BooleanSupplier { ModuleNuker.ignoreOpenInventory }
    ))

    override val parent: ModeValueGroup<Mode>
        get() = mode

    override fun disable() {
        currentTarget = null
        wasTarget = null
        blockBreaker.disable()
    }

    @Suppress("unused")
    private val simulatedTickHandler = handler<RotationUpdateEvent> {
        if (blockBreaker.isBlocked()) {
            return@handler
        }

        val target = lookupTarget()
        currentTarget = target?.pos
        blockBreaker.setTarget(target)

        if (!ModulePacketMine.running) {
            wasTarget = currentTarget
        } else if (target == null) {
            wasTarget = null
        }
    }

    /**
     * Chooses the best block to break next and aims at it.
     */
    private fun lookupTarget(): BlockBreaker.PreparedTarget? {
        // Check if the current target is still valid
        currentTarget?.let { pos ->
            val blockState = pos.state ?: return@let

            if (blockState.isNotBreakable(pos) || !ModuleNuker.isValid(blockState)) {
                return@let
            }

            blockBreaker.prepareTarget(pos)?.let { return it }
        }

        for ((pos, _) in areaMode.activeMode.lookupTargets(blockBreaker.range)) {
            blockBreaker.prepareTarget(pos)?.let { return it }
        }

        return null
    }

}
