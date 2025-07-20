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

import it.unimi.dsi.fastutil.ints.Int2LongLinkedOpenHashMap
import it.unimi.dsi.fastutil.ints.Int2LongMap
import it.unimi.dsi.fastutil.ints.Int2LongMaps
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.minecraft.network.packet.s2c.play.EntitiesDestroyS2CPacket
import net.minecraft.network.packet.s2c.play.PlaySoundFromEntityS2CPacket
import net.minecraft.sound.SoundEvents

/**
 * Can be implemented to handle actions after crystals got attacked.
 */
abstract class CrystalPostAttackTracker : EventListener {

    protected val attackedIds: Int2LongMap = Int2LongMaps.synchronize(Int2LongLinkedOpenHashMap())

    @Suppress("unused")
    private val repeatable = tickHandler {
        val currentTime = System.currentTimeMillis()
        val attackTime = currentTime - timeOutAfter()
        attackedIds.int2LongEntrySet().iterator().apply {
            while (hasNext()) {
                val entry = next()
                if (entry.longValue < attackTime) {
                    timedOut(entry.intKey)
                    remove()
                }
            }
        }
    }

    @Suppress("unused")
    private val worldChangeHandler = handler<WorldChangeEvent> {
        attackedIds.clear()
        cleared()
    }

    @Suppress("unused")
    private val explodeListener = handler<PacketEvent> { event ->
        when (val packet = event.packet) {
            is PlaySoundFromEntityS2CPacket -> {
                if (packet.sound != SoundEvents.ENTITY_GENERIC_EXPLODE) {
                    return@handler
                }

                val id = packet.entityId
                // Default (non-existing) value = 0
                if (attackedIds.remove(id) != 0L) {
                    confirmed(id)
                }
            }

            is EntitiesDestroyS2CPacket -> {
                packet.entityIds.forEach { id ->
                    if (attackedIds.remove(id) != 0L) {
                        confirmed(id)
                    }
                }
            }
        }
    }

    fun onToggle() {
        attackedIds.clear()
        cleared()
    }

    /**
     * Gets called when we are sure the crystal got destroyed.
     *
     * @param id The id of the attacked entity.
     */
    open fun confirmed(id: Int) {}

    /**
     * Gets called when the crystal was not confirmed in the time defined by [timeOutAfter] in ms.
     *
     * @param id The id of the attacked entity.
     */
    open fun timedOut(id: Int) {}

    /**
     * Gets called when the attacked id list gets cleared.
     */
    open fun cleared() {}

    /**
     * Show be called when the crystal aura attacks.
     *
     * @param id The id of the attacked entity.
     */
    open fun attacked(id: Int) {
        if (!running) {
            return
        }

        attackedIds.put(id, System.currentTimeMillis())
    }

    /**
     * After how many ms attacks are not tracked anymore.
     */
    open fun timeOutAfter(): Long = 5000L // 5 seconds

}
