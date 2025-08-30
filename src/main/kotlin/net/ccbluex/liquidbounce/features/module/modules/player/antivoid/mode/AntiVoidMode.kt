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
package net.ccbluex.liquidbounce.features.module.modules.player.antivoid.mode

import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.ModuleFly
import net.ccbluex.liquidbounce.features.module.modules.player.antivoid.ModuleAntiVoid
import net.minecraft.util.math.Vec3d

abstract class AntiVoidMode(name: String) : Choice(name) {

    override val parent: ChoiceConfigurable<*>
        get() = ModuleAntiVoid.mode

    // Cases in which the AntiVoid protection should not be active.
    open val isExempt: Boolean
        get() = player.isDead || ModuleFly.running

    open fun discoverRescuePosition(): Vec3d? {
        if (!ModuleAntiVoid.isLikelyFalling) {
            return player.pos
        }
        return null
    }

    /**
     * Attempt to safely move the player to a safe location.
     */
    abstract fun rescue(): Boolean

}
