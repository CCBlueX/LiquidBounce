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
package net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.post

import it.unimi.dsi.fastutil.ints.Int2ObjectMap
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import net.ccbluex.liquidbounce.config.types.ToggleableConfigurable
import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.ModuleCrystalAura
import net.minecraft.entity.Entity
import net.minecraft.entity.decoration.EndCrystalEntity

/**
 * Removes hit crystals instantly from the world instead of waiting for the actual remove packet
 * what might allow faster placement.
 */
object SubmoduleSetDead : ToggleableConfigurable(ModuleCrystalAura, "SetDead", true) {

    /**
     * If the crystal was removed but no entity remove packet was sent after the confirmation time, the
     * crystal is added back to the world.
     */
    val confirmTime by int("ConfirmTime", 150, 10..2000, "ms")

    object CrystalTracker : CrystalPostAttackTracker() {

        val entities: Int2ObjectMap<EndCrystalEntity> = Int2ObjectMaps.synchronize(Int2ObjectOpenHashMap())

        override fun attacked(id: Int) {
            if (!running) {
                return
            }

            mc.execute {
                val entity = world.getEntityById(id)
                if (entity is EndCrystalEntity) {
                    super.attacked(id)
                    world.removeEntity(id, Entity.RemovalReason.DISCARDED)
                    entities.put(id, entity)
                }
            }
        }

        override fun confirmed(id: Int) {
            entities.remove(id)
        }

        override fun timedOut(id: Int) {
            mc.execute {
                val entity = entities.remove(id) ?: return@execute
                entity.unsetRemoved()
                world.addEntity(entity)
            }
        }

        override fun cleared() {
            entities.clear()
        }

        override fun timeOutAfter() = confirmTime.toLong()

        override fun parent() = SubmoduleSetDead

    }

}
