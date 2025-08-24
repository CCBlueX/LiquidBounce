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
 *
 */

package net.ccbluex.liquidbounce.features.module.modules.player.autobuff

import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.ScheduleInventoryActionEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.player.autobuff.features.*
import net.ccbluex.liquidbounce.utils.aiming.RotationsConfigurable
import net.ccbluex.liquidbounce.utils.client.SilentHotbar
import net.ccbluex.liquidbounce.utils.combat.CombatManager

object ModuleAutoBuff : ClientModule(
    name = "AutoBuff",
    category = Category.PLAYER,
    aliases = arrayOf("AutoPot", "AutoGapple", "AutoSoup")
) {

    /**
     * All buff features
     */
    internal val features = arrayOf(
        Soup,
        Head,
        Pot,
        Drink,
        Gapple
    )

    init {
        // Register features to configurable
        features.forEach(this::tree)
    }

    /**
     * Auto Swap will automatically swap your selected slot to the best item for the situation.
     * For example, if you're low on health, it will swap to the next health pot.
     *
     * It also allows to customize the delay between each swap.
     */
    internal object AutoSwap : ToggleableConfigurable(ModuleAutoBuff, "AutoSwap", true) {

        /**
         * How long should we wait after swapping to the item?
         */
        val delayIn by intRange("DelayIn", 1..1, 0..20, "ticks")

        /**
         * How long should we wait after using the item?
         */
        val delayOut by intRange("DelayOut", 1..1, 0..20, "ticks")

    }

    init {
        tree(AutoSwap)
        tree(Refill)
    }

    internal class AutoBuffRotationsConfigurable : RotationsConfigurable(this) {

        val rotationTiming by enumChoice("RotationTiming", RotationTimingMode.NORMAL)

        enum class RotationTimingMode(override val choiceName: String) : NamedChoice {
            NORMAL("Normal"),
            ON_TICK("OnTick"),
            ON_USE("OnUse")
        }

    }

    /**
     * Rotation Configurable for every feature that depends on rotation change
     */
    internal val rotations = tree(AutoBuffRotationsConfigurable())

    internal val combatPauseTime by int("CombatPauseTime", 0, 0..40, "ticks")
    private val notDuringCombat by boolean("NotDuringCombat", false)

    private val activeFeatures
        get() = features.filter { it.enabled }

    @Suppress("unused")
    private val tickHandler = tickHandler {
        if (notDuringCombat && CombatManager.isInCombat) {
            return@tickHandler
        }

        if (player.isDead || player.isCreative || player.isSpectator || player.age < 20) {
            return@tickHandler
        }

        for (feature in activeFeatures) {
            if (feature.runIfPossible(this)) {
                return@tickHandler
            }
        }
    }

    @Suppress("unused")
    private val refiller = handler<ScheduleInventoryActionEvent> {
        // If no feature was run, we should run refill
        if (Refill.enabled) {
            Refill.execute(it)
        }
    }

    override fun onDisabled() {
        SilentHotbar.resetSlot(ModuleAutoBuff)

        features.forEach { it.onDisabled() }
        super.onDisabled()
    }

}
