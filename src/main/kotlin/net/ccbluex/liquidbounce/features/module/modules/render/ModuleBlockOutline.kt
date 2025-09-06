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
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.render.trajectories.ModuleTrajectories
import net.ccbluex.liquidbounce.utils.render.BlockHitRenderer
import net.ccbluex.liquidbounce.utils.render.trajectory.TrajectoryData

/**
 * Block Outline module
 *
 * Changes the way Minecraft highlights blocks.
 *
 * TODO: Implement GUI Information Panel
 *
 * [MixinWorldRenderer.cancelBlockOutline]
 */
object ModuleBlockOutline : ClientModule("BlockOutline", Category.RENDER, aliases = arrayOf("BlockOverlay")) {

    private val blockHitRenderer = tree(BlockHitRenderer(this))

    @Suppress("unused")
    private val renderHandler = handler<WorldRenderEvent> { event ->
        // Disable rendering if ModuleTrajectories is active, rendering block hit ESP,
        // and the player is holding a projectile item
        if (ModuleTrajectories.running && ModuleTrajectories.enableBlockHitESP &&
            player.handItems.any {
                TrajectoryData.getRenderedTrajectoryInfo(player, it.item,
                    ModuleTrajectories.alwaysShowBow) != null }
        ) {
            return@handler
        }
        blockHitRenderer.render(
            enable = true,
            event = event,
            hitResult = mc.crosshairTarget,
        )
    }

    @Suppress("unused")
    private val worldChangeHandler = handler<WorldChangeEvent> {
        blockHitRenderer.resetPositions()
    }
}
