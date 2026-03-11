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

package net.ccbluex.liquidbounce.features.module.modules.combat.elytratarget

import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.event.waitTicks
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.ccbluex.liquidbounce.utils.client.sendHeldItemChange
import net.ccbluex.liquidbounce.utils.entity.squaredBoxedDistanceTo
import net.ccbluex.liquidbounce.utils.inventory.HotbarItemSlot
import net.ccbluex.liquidbounce.utils.inventory.OffHandSlot
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.ccbluex.liquidbounce.utils.inventory.useHotbarSlotOrOffhand
import net.ccbluex.liquidbounce.utils.math.sq
import net.minecraft.network.protocol.game.ServerboundUseItemPacket
import net.minecraft.world.item.Items
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura as KillAura

internal object AutoFirework : ToggleableValueGroup(ModuleElytraTarget, "AutoFirework", true) {
    private val useMode by enumChoice("UseMode", FireworkUseMode.NORMAL)
    private val extraDistance by float("ExtraDistance", 50f, 5f..100f, suffix = "m")
    private val slotResetDelay by intRange("SlotResetDelay", 0..0, 0..20, "ticks")
    private val syncCooldownWithKillAura by boolean("SyncCooldownWithKillAura", false)
    private val cooldown by intRange("Cooldown", 8..10, 1..50, "ticks")

    override val running: Boolean
        get() = super.running && ModuleElytraTarget.target != null

    private const val MILLISECONDS_PER_TICK = 50

    /**
     * Initial firework cooldown
     */
    private var fireworkCooldown = 750

    private val fireworkChronometer = Chronometer()

    private val cooldownReached: Boolean
        get() = fireworkChronometer.hasElapsed((fireworkCooldown * MILLISECONDS_PER_TICK).toLong())

    @Suppress("ComplexCondition")
    private suspend fun canUseFirework(): Boolean {
        if (!KillAura.running
            || !syncCooldownWithKillAura
            || (
                KillAura.clicker.isClickTick
                && KillAura.targetTracker.target
                    ?.squaredBoxedDistanceTo(player)
                    ?.takeIf { it >= KillAura.range.interactionRange.sq() } != null
                )
        ) {
            return true
        }

        /*
         * The Killaura is ready to perform the click.
         * We can use the firework on the next tick.
         * After killaura performed the click
         */
        return if (KillAura.clicker.isClickTick) {
            waitTicks(1)
            true
        } else {
            false
        }
    }

    @Suppress("unused")
    private val autoFireworkHandler = tickHandler {
        val target = ModuleElytraTarget.target ?: return@tickHandler

        if (cooldownReached && canUseFirework()) {
            Slots.OffhandWithHotbar.findSlot(Items.FIREWORK_ROCKET)?.let {
                useMode.useFireworkSlot(it, slotResetDelay.random())
                fireworkChronometer.reset()
            }
        }

        fireworkCooldown = if (target.squaredBoxedDistanceTo(player) > extraDistance.sq()) {
            cooldown.last
        } else {
            cooldown.first
        }
    }


    @Suppress("unused")
    private enum class FireworkUseMode(override val tag: String) : Tagged {
        NORMAL("Normal") {
            override fun useFireworkSlot(slot: HotbarItemSlot, resetDelay: Int) {
                useHotbarSlotOrOffhand(slot, resetDelay)
            }
        },
        PACKET("Packet") {
            override fun useFireworkSlot(slot: HotbarItemSlot, resetDelay: Int) {
                val curSlot = player.inventory.selectedSlot
                val slotUpdateFlag = slot !is OffHandSlot && slot.hotbarSlotForServer != curSlot

                if (slotUpdateFlag) {
                    player.inventory.selectedSlot = slot.hotbarSlotForServer
                    network.sendHeldItemChange(slot.hotbarSlotForServer)
                }

                interaction.startPrediction(world) { sequence ->
                    ServerboundUseItemPacket(slot.useHand, sequence, player.yRot, player.xRot)
                }

                if (slotUpdateFlag) {
                    player.inventory.selectedSlot = curSlot
                    network.sendHeldItemChange(curSlot)
                }
            }
        };

        abstract fun useFireworkSlot(slot: HotbarItemSlot, resetDelay: Int)
    }
}
