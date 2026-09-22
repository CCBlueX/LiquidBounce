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
package net.ccbluex.liquidbounce.features.module.modules.combat.killaura.features

import net.ccbluex.liquidbounce.deeplearn.combat.CombatController
import net.ccbluex.liquidbounce.deeplearn.combat.CombatLiveDecision
import net.ccbluex.liquidbounce.event.events.SprintEvent
import net.ccbluex.liquidbounce.features.addon.UnstableAddonApi
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.KillAuraClicker
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.KillAuraRotationsValueGroup
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.client.world
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.ccbluex.liquidbounce.utils.movement.getDegreesRelativeToView
import net.ccbluex.liquidbounce.utils.movement.getDirectionalInputForDegrees
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.phys.Vec3
import kotlin.math.cos
import kotlin.math.sin

/**
 * What the model decides for KillAura's movement, shared by FightBot and TargetStrafe.
 */
@UnstableAddonApi
object KillAuraAi {
    private const val STEER_DISTANCE = 4f
    private const val FIGHT_DISTANCE = 6f
    private const val STEP_DISTANCE = 0.8

    /** Whether KillAura fights with the model in any way. */
    val active
        get() = ModuleKillAura.running && (KillAuraClicker.usesAi || KillAuraRotationsValueGroup.usesAiRotations ||
            KillAuraFightBot.usesAi)

    /**
     * This tick's decision against [target] if it is close enough to fight and the model's movement is validated.
     * Jumps and sprint were learned up to [FIGHT_DISTANCE]; steering stays closer so it cannot lead us away.
     */
    fun live(target: LivingEntity): CombatLiveDecision? {
        if (target.distanceTo(player) > FIGHT_DISTANCE || !player.hasLineOfSight(target)) {
            return null
        }
        return CombatController.current(target)?.takeIf { it.heads.movement }
    }

    /**
     * The keys the model would press. Only keys, like a player: the velocity stays the one vanilla derives from
     * them, which is what anticheats predict. Null beyond [STEER_DISTANCE] or where the step would leave the ground.
     */
    fun keys(live: CombatLiveDecision, target: LivingEntity): DirectionalInput? {
        if (target.distanceTo(player) > STEER_DISTANCE) {
            return null
        }
        val forward = live.forward - 1.0
        val left = live.strafe - 1.0
        val angle = Math.toRadians(live.referenceYaw.toDouble())
        val desired = Vec3(left * cos(angle) - forward * sin(angle), 0.0, forward * cos(angle) + left * sin(angle))
        return when {
            desired.horizontalDistanceSqr() < 1e-8 -> DirectionalInput.NONE
            safe(desired.normalize().scale(STEP_DISTANCE)) ->
                getDirectionalInputForDegrees(DirectionalInput.NONE, getDegreesRelativeToView(desired))
            else -> null
        }
    }

    /**
     * The model's sprint, as the sprint key only: vanilla still refuses to sprint in shallow water or while
     * blocking. Where the key is read it presses or releases it; during the movement tick it only ever stops.
     */
    fun sprint(event: SprintEvent, live: CombatLiveDecision) {
        val sprint = live.sprint && event.directionalInput.forwards
        when (event.source) {
            SprintEvent.Source.INPUT -> event.sprint = sprint
            SprintEvent.Source.MOVEMENT_TICK -> if (!sprint) event.sprint = false
            SprintEvent.Source.NETWORK -> Unit
        }
    }

    /** The side the model strafes to, -1 for left and 1 for right, or 0 while it walks straight. */
    fun side(live: CombatLiveDecision) = when (live.strafe) {
        2 -> -1
        0 -> 1
        else -> 0
    }

    /**
     * Blocking or eating is part of the fight the model learned, so it keeps moving the way players do; water,
     * gliding and sneaking are not.
     */
    fun canAdjustMovement(airborne: Boolean = false) =
        (airborne || player.onGround()) && !player.isInWater && !player.isFallFlying && !player.isShiftKeyDown

    private fun safe(displacement: Vec3): Boolean {
        val box = player.boundingBox.move(displacement)
        return world.noCollision(player, box) && !world.noCollision(player, box.move(0.0, -0.6, 0.0))
    }
}
