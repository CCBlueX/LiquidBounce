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
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.combat.criticals.ModuleCriticals.allowsCriticalHit
import net.ccbluex.liquidbounce.features.module.modules.combat.criticals.ModuleCriticals.modes
import net.minecraft.client.KeyMapping
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.phys.EntityHitResult

/**
 * Ground ticks after a held click before giving up on the jump, for example under a ceiling.
 */
private const val MAX_GROUND_TICKS = 2

/**
 * Holds a click on an entity from the ground back and jumps instead. Once falling, the click goes through on
 * whatever living entity the crosshair is on; landing first drops it.
 */
object CriticalsLegit : Mode("Legit") {

    override val parent: ModeValueGroup<Mode>
        get() = modes

    private var holding = false
    private var leftGround = false
    private var groundTicks = 0

    private val crosshairEntity
        get() = (mc.hitResult as? EntityHitResult)?.entity

    /**
     * Whether the attack click on the crosshair target is held back, asked before the game handles it.
     */
    fun holdsClick(): Boolean {
        if (!running) {
            return false
        }

        val entity = crosshairEntity ?: return false

        // Further clicks wait for the held one
        if (holding) {
            return true
        }

        if (entity !is LivingEntity || !player.onGround() || !allowsCriticalHit(ignoreOnGround = true)) {
            return false
        }

        holding = true
        leftGround = false
        groundTicks = 0
        return true
    }

    @Suppress("unused")
    private val movementInputHandler = handler<MovementInputEvent> { event ->
        if (holding && !leftGround && player.onGround()) {
            event.jump = true
        }
    }

    @Suppress("unused")
    private val tickHandler = handler<GameTickEvent> {
        if (!holding) {
            return@handler
        }

        if (player.onGround()) {
            // Landed without the crosshair on an entity, or the jump did not happen
            if (leftGround || ++groundTicks > MAX_GROUND_TICKS) {
                holding = false
            }
            return@handler
        }

        leftGround = true
        if (player.fallDistance > 0.0 && crosshairEntity is LivingEntity) {
            holding = false
            // The game handles the click later this tick
            KeyMapping.click(mc.options.keyAttack.key)
        }
    }

    override fun disable() {
        holding = false
        super.disable()
    }

}
