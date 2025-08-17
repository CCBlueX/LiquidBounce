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
package net.ccbluex.liquidbounce.features.module.modules.combat.aimbot

import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.combat.aimbot.autobow.AutoBowAimbotFeature
import net.ccbluex.liquidbounce.features.module.modules.combat.aimbot.autobow.AutoBowAutoShootFeature
import net.ccbluex.liquidbounce.features.module.modules.combat.aimbot.autobow.AutoBowFastChargeFeature
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.minecraft.item.BowItem
import java.util.*

/**
 * AutoBow module
 *
 * Automatically shoots with your bow when it's fully charged
 *  + and make it possible to shoot faster
 */
object ModuleAutoBow : ClientModule("AutoBow", Category.COMBAT, aliases = arrayOf("BowAssist", "BowAimbot")) {
    val random = Random()

    /**
     * Keeps track of the last bow shot that has taken place
     */
    val lastShotTimer = Chronometer()

    @JvmStatic
    fun onStopUsingItem() {
        if (player.activeItem.item is BowItem) {
            lastShotTimer.reset()
        }
    }

    override fun onDisabled() {
        AutoBowAimbotFeature.targetTracker.reset()
    }

    init {
        tree(AutoBowAutoShootFeature)
        tree(AutoBowAimbotFeature)
        tree(AutoBowFastChargeFeature)
    }
}
