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
package net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.post

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import it.unimi.dsi.fastutil.ints.IntCollection
import it.unimi.dsi.fastutil.ints.IntOpenHashSet
import net.ccbluex.fastutil.synchronized
import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.ModuleCrystalAura
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.boss.enderdragon.EndCrystal

/**
 * Ignores or removes hit crystals immediately instead of waiting for the server remove packet,
 * allowing faster consecutive placements without off-thread race conditions.
 */
object SubmoduleSetDead : ToggleableValueGroup(ModuleCrystalAura, "SetDead", true) {

    private val mode by enumChoice("Mode", SetDeadMode.REMOVE)

    /**
     * If no explosion sound or entity remove packet arrives within this window, the crystal is restored.
     */
    val confirmTime by int("ConfirmTime", 150, 10..2000, "ms")

    enum class SetDeadMode(override val tag: String) : Tagged {
        /**
         * Removes the crystal entity client-side and restores it if unconfirmed.
         */
        REMOVE("Remove"),

        /**
         * Keeps the crystal entity in the world (avoiding strict anti-cheat entity desyncs)
         * while excluding its ID from placement obstruction checks until confirmed or timed out.
         */
        EXCLUDE("Exclude")
    }

    val excludedIds: IntCollection?
        get() = if (running) CrystalTracker.snapshotAttackedIds() else null

    object CrystalTracker : CrystalPostAttackTracker() {

        @JvmRecord
        private data class RemovedCrystal(
            val entity: EndCrystal,
            val level: ClientLevel
        )

        private val entities = Int2ObjectOpenHashMap<RemovedCrystal>().synchronized()

        fun snapshotAttackedIds(): IntCollection? {
            synchronized(attackedIds) {
                if (attackedIds.isEmpty()) {
                    return null
                }
                return IntOpenHashSet(attackedIds.keys)
            }
        }

        override fun attacked(id: Int) {
            if (!running) {
                return
            }

            // Record immediately so off-thread placement tasks exclude this ID before mc.execute runs
            super.attacked(id)

            if (mode != SetDeadMode.REMOVE) {
                return
            }

            mc.execute {
                val currentWorld = mc.level ?: return@execute
                val entity = currentWorld.getEntity(id)
                if (entity is EndCrystal) {
                    currentWorld.removeEntity(id, Entity.RemovalReason.DISCARDED)
                    entities.put(id, RemovedCrystal(entity, currentWorld))
                }
            }
        }

        override fun confirmed(id: Int) {
            entities.remove(id)
        }

        override fun timedOut(id: Int) {
            mc.execute {
                val tracked = entities.remove(id) ?: return@execute
                val currentWorld = mc.level ?: return@execute
                if (tracked.level !== currentWorld || currentWorld.getEntity(id) != null) {
                    return@execute
                }

                tracked.entity.unsetRemoved()
                currentWorld.addEntity(tracked.entity)
            }
        }

        override fun cleared() {
            mc.execute {
                val currentWorld = mc.level ?: run {
                    entities.clear()
                    return@execute
                }
                entities.values.toTypedArray().forEach { tracked ->
                    if (tracked.level === currentWorld && currentWorld.getEntity(tracked.entity.id) == null) {
                        tracked.entity.unsetRemoved()
                        currentWorld.addEntity(tracked.entity)
                    }
                }
                entities.clear()
            }
        }

        override fun timeOutAfter() = confirmTime.toLong()

        override fun parent() = SubmoduleSetDead

    }

}
