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
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.NotificationEvent
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.notification
import net.ccbluex.liquidbounce.utils.entity.moving
import net.ccbluex.liquidbounce.utils.entity.strafe
import net.minecraft.block.Blocks
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction

/**
 * NoWeb module
 *
 * Disables web slowdown.
 */
object ModuleNoWeb : Module("NoWeb", Category.MOVEMENT) {

    init {
        enableLock()
    }

    private val modes = choices("Mode", Air, arrayOf(Air, GrimBreak, Intave14))

    val repeatable = repeatable {
        if (ModuleAvoidHazards.enabled && ModuleAvoidHazards.cobWebs) {
            ModuleAvoidHazards.enabled = false

            notification(
                "Compatibility error", "NoWeb is incompatible with AvoidHazards",
                NotificationEvent.Severity.ERROR
            )
            waitTicks(20)
        }
    }

    /**
     * Handle cobweb collision
     *
     * @see net.minecraft.block.CobwebBlock.onEntityCollision
     * @return if we should cancel the slowdown effect
     */
    fun handleEntityCollision(pos: BlockPos): Boolean {
        if (!enabled) {
            return false
        }

        return modes.activeChoice.handleEntityCollision(pos)
    }

    abstract class NoWebMode(name: String) : Choice(name) {

        override val parent: ChoiceConfigurable<NoWebMode>
            get() = modes

        abstract fun handleEntityCollision(pos: BlockPos): Boolean
    }

    /**
     * No collision with cobwebs
     */
    object Air : NoWebMode("Air") {
        override fun handleEntityCollision(pos: BlockPos) = true
    }

    /**
     * No collision with cobwebs and breaks them to bypass check
     *
     * @anticheat Grim
     * @version 2.3.65
     */
    object GrimBreak : NoWebMode("Grim2365") {

        // Needed to bypass BadPacketsX
        private val breakOnWorld by boolean("BreakOnWorld", true)

        override fun handleEntityCollision(pos: BlockPos): Boolean {
            if (breakOnWorld) mc.world?.setBlockState(pos, Blocks.AIR.defaultState)

            val start = PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, pos, Direction.DOWN)
            val abort = PlayerActionC2SPacket(PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, pos, Direction.DOWN)
            val finish = PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, pos, Direction.DOWN)

            network.sendPacket(start)
            network.sendPacket(abort)
            network.sendPacket(finish)

            return true
        }
    }

    /**
     * Intave needs to improve their movement checks
     * works on intave 14.8.4
     */

    object Intave14 : NoWebMode("Intave14") {
        override fun handleEntityCollision(pos: BlockPos): Boolean {
            if (player.moving) {
                if (player.isOnGround) {
                    if (player.age % 3 == 0) {
                        player.strafe(strength = 0.734)
                    } else {
                        player.jump()
                        player.strafe(strength = 0.346)
                    }
                }
            }
            return false
        }
    }
}
