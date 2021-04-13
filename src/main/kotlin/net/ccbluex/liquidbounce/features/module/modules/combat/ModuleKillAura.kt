/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2021 CCBlueX
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
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.AttackEvent
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.extensions.*
import net.minecraft.entity.Entity
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket
import net.minecraft.util.Hand

/**
 * KillAura module
 *
 * Automatically attacks enemies
 */
object ModuleKillAura : Module("KillAura", Category.COMBAT) {

    // Attack speed
    val cps by intRange("CPS", 5..8, 1..20) // todo:
    val cooldown by boolean("Cooldown", true) // todo:

    // Range
    val range by float("Range", 4.7f, 1f..8f)
    val wallRange by float("WallRange", 3f, 0f..8f) // todo:

    // Target
    val targetTracker = TargetTracker()

    // Rotation
    val rotations = RotationsConfigurable()

    // Predict
    val predict by floatRange("Predict", 0f..0f, 0f..5f) // todo:
    val backtrack by floatRange("Backtrack", 0f..0f, 0f..20f) // todo:

    // Bypass techniques
    val swing by boolean("Swing", true) // todo:
    val keepSprint by boolean("KeepSprint", true)

    val failRate by int("FailRate", 0, 0..100) // todo:
    val missSwing by boolean("MissSwing", true) // todo:

    val checkableInventory by boolean("CheckableInventory", false) // todo:

    override fun enable() {
        targetTracker.update()
    }

    override fun disable() {
        targetTracker.cleanup()
    }

    val repeatable = repeatable {
        update()
    }

    private fun update() {
        targetTracker.update()

        val eyes = player.eyesPos

        // todo: add predict to eyes

        val rangeSquared = range * range

        targetTracker.lockedOnTarget = null

        for (target in targetTracker) {
            if (target.squaredDistanceTo(player) > rangeSquared)
                continue

            val box = target.boundingBox

            // todo: add predict to box

            // find best spot (and skip if no spot was found)
            val (rotation, _) = RotationManager.raytraceBox(eyes, box, throughWalls = false, range = range.toDouble())
                ?: continue

            // lock on target tracker
            targetTracker.lock(target)

            // aim on target
            RotationManager.aimAt(rotation, configurable = rotations)
        }

        val target = targetTracker.lockedOnTarget ?: return

        if (target.squaredDistanceTo(player) <= rangeSquared && RotationManager.facingEnemy(target, range.toDouble())
            && player.getAttackCooldownProgress(0.0f) >= 1.0f
        ) {
            attackEntity(target)
        }
    }

    private fun attackEntity(entity: Entity) {
        // todo: stop blocking (1.8 support sword / 1.9+ shield)

        EventManager.callEvent(AttackEvent(entity))

        // todo: swing now (1.8)

        network.sendPacket(PlayerInteractEntityC2SPacket(entity, player.isSneaking))

        // swing post (1.9+)
        player.swingHand(Hand.MAIN_HAND)

        if (keepSprint) {
            // todo: show crits
        }else{
            player.attack(entity)
        }

        // reset cooldown
        player.resetLastAttackedTicks()
    }

}
