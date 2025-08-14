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
package net.ccbluex.liquidbounce.features.module.modules.player.antivoid

import net.ccbluex.liquidbounce.common.ShapeFlag
import net.ccbluex.liquidbounce.event.events.NotificationEvent
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.player.antivoid.mode.AntiVoidBlinkMode
import net.ccbluex.liquidbounce.features.module.modules.player.antivoid.mode.AntiVoidFlagMode
import net.ccbluex.liquidbounce.features.module.modules.player.antivoid.mode.AntiVoidGhostBlockMode
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug.debugGeometry
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug.debugParameter
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.client.notification
import net.ccbluex.liquidbounce.utils.entity.PlayerSimulationCache
import net.minecraft.util.math.Vec3d
import net.minecraft.util.shape.VoxelShapes

/**
 * AntiVoid module protects the player from falling into the void by simulating
 * future movements and taking action if necessary.
 */
object ModuleAntiVoid : ClientModule("AntiVoid", Category.PLAYER) {

    val mode = choices(
        "Mode", AntiVoidGhostBlockMode, arrayOf(
            AntiVoidGhostBlockMode,
            AntiVoidFlagMode,
            AntiVoidBlinkMode
        )
    )

    // The height at which the void is deemed to begin.
    private val voidThreshold by int("VoidLevel", 0, -256..0)

    // Flags indicating if an action has been already taken or needs to be taken.
    var isLikelyFalling = false
    var rescuePosition: Vec3d? = null
        private set

    // How many future ticks to simulate to ensure safety.
    private const val SAFE_TICKS_THRESHOLD = 10

    override fun onEnabled() {
        isLikelyFalling = false
        super.onDisabled()
    }

    /**
     * Executes periodically to check if an anti-void action is required, and triggers it if necessary.
     */
    @Suppress("unused")
    private val voidHandler = tickHandler {
        // Analyzes if the player might be falling into the void soon.
        try {
            ShapeFlag.noShapeChange = true
            isLikelyFalling = isPredictingFall()
        } finally {
            ShapeFlag.noShapeChange = false
        }

        val rescuePosition = mode.activeChoice.discoverRescuePosition()
        if (rescuePosition != null) {
            this@ModuleAntiVoid.rescuePosition = rescuePosition
        }

        debugParameter("IsExempt") { mode.activeChoice.isExempt }
        debugParameter("IsLikelyFalling") { isLikelyFalling }
        debugParameter("SafePosition") { ModuleAntiVoid.rescuePosition }

        if (mode.activeChoice.isExempt || !isLikelyFalling) {
            return@tickHandler
        }

        if (ModuleAntiVoid.rescuePosition == null) {
            return@tickHandler
        }

        val boundingBox = player.boundingBox.withMinY(voidThreshold.toDouble())

        // If no collision is detected within a threshold beyond which falling
        // into void is likely, take the necessary action.
        val collisions = world.getBlockCollisions(player, boundingBox)

        if (collisions.none() || collisions.all { shape -> shape == VoxelShapes.empty() }) {
            if (mode.activeChoice.rescue()) {
                notification(
                    "AntiVoid", "Action taken to prevent void fall",
                    NotificationEvent.Severity.INFO
                )
                ModuleAntiVoid.rescuePosition = null
            }
        }
    }

    /**
     * Checks if the player is likely to fall into the void within a certain threshold.
     */
    private fun isPredictingFall(): Boolean {
        for (tick in 0 until SAFE_TICKS_THRESHOLD) {
            val snapshot = PlayerSimulationCache.getSimulationForLocalPlayer().getSnapshotAt(tick)
            if (snapshot.fallDistance > 0.0) {
                return isSafeForRescue(snapshot.pos)
            }
        }
        return false
    }

    fun isSafeForRescue(pos: Vec3d): Boolean {
        val boundingBox = player.boundingBox
            // Change position to the snapshot position
            .offset(pos.subtract(player.pos))
            .withMinY(voidThreshold.toDouble())

        // If no collision is detected within a threshold beyond which falling
        // into void is likely, take the necessary action.
        val collisions = world.getBlockCollisions(player, boundingBox)
        val hasCollision = collisions.none() || collisions.all { shape -> shape == VoxelShapes.empty() }
        debugGeometry("BoundingBox") {
            ModuleDebug.DebuggedBox(
                boundingBox, if (hasCollision) {
                    Color4b.RED
                } else {
                    Color4b.GREEN
                }.alpha(150)
            )
        }

        return hasCollision
    }

}
