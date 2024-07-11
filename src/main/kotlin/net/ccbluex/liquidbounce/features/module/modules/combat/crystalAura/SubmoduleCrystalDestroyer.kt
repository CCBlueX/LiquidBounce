/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015-2024 CCBlueX
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
package net.ccbluex.liquidbounce.features.module.modules.combat.crystalAura

import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.canSeeBox
import net.ccbluex.liquidbounce.utils.aiming.facingEnemy
import net.ccbluex.liquidbounce.utils.aiming.raytraceBox
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.client.world
import net.ccbluex.liquidbounce.utils.combat.attack
import net.ccbluex.liquidbounce.utils.combat.getEntitiesBoxInRange
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.client.world.ClientWorld
import net.minecraft.entity.decoration.EndCrystalEntity

object SubmoduleCrystalDestroyer {
    private var currentTarget: EndCrystalEntity? = null

    fun tick() {
        val range = ModuleCrystalAura.DestroyOptions.range.toDouble()

        updateTarget(player, world, range)

        val target = currentTarget ?: return

        // find best spot (and skip if no spot was found)
        val (rotation, _) =
            raytraceBox(
                player.eyePos,
                target.boundingBox,
                range = range,
                wallsRange = 0.0,
            ) ?: return

        // aim on target
        RotationManager.aimAt(
            rotation,
            configurable = ModuleCrystalAura.rotations,
            priority = Priority.IMPORTANT_FOR_USER_SAFETY,
            provider = ModuleCrystalAura
        )

        if (!facingEnemy(target, range, RotationManager.serverRotation)) {
            return
        }

        target.attack(ModuleCrystalAura.swing)
    }

    private fun updateTarget(
        player: ClientPlayerEntity,
        world: ClientWorld,
        range: Double,
    ) {
        currentTarget =
            world.getEntitiesBoxInRange(player.getCameraPosVec(1.0F), range) { it is EndCrystalEntity }
                .mapNotNull {
                    if (!canSeeBox(
                            player.eyePos,
                            it.boundingBox,
                            range = range,
                            wallsRange = 0.0,
                        )
                    ) {
                        return@mapNotNull null
                    }

                    val damage = ModuleCrystalAura.approximateExplosionDamage(world, it.pos)

                    if (damage < ModuleCrystalAura.DestroyOptions.minEfficiency) {
                        return@mapNotNull null
                    }

                    return@mapNotNull Pair(it as EndCrystalEntity, damage)
                }
                .maxByOrNull { it.second }
                ?.first
    }
}
