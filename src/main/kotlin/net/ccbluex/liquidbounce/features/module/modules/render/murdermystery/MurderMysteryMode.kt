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

package net.ccbluex.liquidbounce.features.module.modules.render.murdermystery

import net.ccbluex.liquidbounce.config.types.group.Mode
import net.minecraft.client.player.AbstractClientPlayer
import net.minecraft.resources.Identifier

sealed class MurderMysteryMode(name: String) : Mode(name) {
    final override val parent
        get() = ModuleMurderMystery.modes

    abstract fun handleHasBow(
        entity: AbstractClientPlayer,
        locationSkin: Identifier,
    )

    abstract fun handleHasSword(
        entity: AbstractClientPlayer,
        locationSkin: Identifier,
    )

    open fun disallowsArrowDodge(): Boolean = false

    abstract fun shouldAttack(entity: AbstractClientPlayer): Boolean

    abstract fun getPlayerType(player: AbstractClientPlayer): PlayerType

    abstract fun reset()

    enum class PlayerType {
        NEUTRAL,
        DETECTIVE_LIKE,
        MURDERER,
    }
}
