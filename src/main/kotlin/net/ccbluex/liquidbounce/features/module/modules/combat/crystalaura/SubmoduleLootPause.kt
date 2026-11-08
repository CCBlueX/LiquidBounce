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
package net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura

import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3

/**
 * Stops placing and breaking after the target died while its loot still lies around, so the explosions
 * don't destroy it.
 */
object SubmoduleLootPause : ToggleableValueGroup(ModuleCrystalAura, "LootPause", false) {

    private const val LOOT_SPAWN_GRACE_MS = 1000L

    private val radius by float("Radius", 6f, 1f..12f)
    private val timeout by int("Timeout", 10, 1..60, "s")

    private var lastTarget: LivingEntity? = null
    private var deathPos: Vec3? = null
    private val sinceDeath = Chronometer()

    /**
     * Whether the crystal aura waits for the loot to be picked up. Updated once per tick by [update].
     */
    var pausing = false
        private set

    fun update(target: LivingEntity?) {
        val previous = lastTarget
        if (previous != null && previous.isDeadOrDying) {
            deathPos = previous.position()
            sinceDeath.reset()
        }
        lastTarget = target

        pausing = enabled && lootLies()
    }

    private fun lootLies(): Boolean {
        val pos = deathPos ?: return false
        if (sinceDeath.hasElapsed(timeout * 1000L)) {
            deathPos = null
            return false
        }

        val area = AABB.ofSize(pos, radius * 2.0, radius * 2.0, radius * 2.0)
        val lies = world.getEntitiesOfClass(ItemEntity::class.java, area) { it.isAlive }.isNotEmpty()
        // the drops can arrive a few ticks after the death
        if (!lies && sinceDeath.hasElapsed(LOOT_SPAWN_GRACE_MS)) {
            deathPos = null
        }

        return lies
    }

    override fun onDisabled() {
        lastTarget = null
        deathPos = null
        pausing = false
    }

}
