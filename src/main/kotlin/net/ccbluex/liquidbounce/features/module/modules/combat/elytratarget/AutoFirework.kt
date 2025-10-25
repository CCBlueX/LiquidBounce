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
 *
 */

package net.ccbluex.liquidbounce.features.module.modules.combat.elytratarget

import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.waitTicks
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.ccbluex.liquidbounce.utils.entity.squaredBoxedDistanceTo
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.minecraft.item.Items
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura as KillAura

private const val MILLISECONDS_PER_TICK = 50

/**
 * Initial firework cooldown
 */
@Suppress("MagicNumber")
private var fireworkCooldown = 750

private val fireworkChronometer = Chronometer()

@Suppress("MagicNumber")
internal object AutoFirework : ToggleableConfigurable(ModuleElytraTarget, "AutoFirework", true) {
    private val useMode by enumChoice("UseMode", FireworkUseMode.NORMAL)
    private val extraDistance by float("ExtraDistance", 50f, 5f..100f, suffix = "m")
    private val slotResetDelay by intRange("SlotResetDelay", 0..0, 0..20, "ticks")
    private val syncCooldownWithKillAura by boolean("SyncCooldownWithKillAura", false)
    private val cooldown by intRange("Cooldown", 8..10, 1..50, "ticks")

    override val running: Boolean
        get() = super.running && ModuleElytraTarget.target != null

    private inline val cooldownReached: Boolean
        get() = fireworkChronometer.hasElapsed((fireworkCooldown * MILLISECONDS_PER_TICK).toLong())

    @Suppress("ComplexCondition")
    private suspend fun canUseFirework(): Boolean {
        if (!KillAura.running
            || !syncCooldownWithKillAura
            || (
                KillAura.clickScheduler.isClickTick
                && KillAura.targetTracker.target
                    ?.squaredBoxedDistanceTo(player)
                    ?.takeIf { it >= KillAura.range * KillAura.range } != null
                )
        ) {
            return true
        }

        /*
         * The Killaura is ready to perform the click.
         * We can use the firework on the next tick.
         * After killaura performed the click
         */
        return if (KillAura.clickScheduler.isClickTick) {
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

        fireworkCooldown = if (target.squaredBoxedDistanceTo(player) > extraDistance * extraDistance) {
            cooldown.max()
        } else {
            cooldown.min()
        }
    }
}
