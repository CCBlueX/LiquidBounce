/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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
package net.ccbluex.liquidbounce.utils.client

import net.ccbluex.liquidbounce.config.NamedChoice


sealed interface Curve: NamedChoice {
    val at: (Float) -> Float
}

enum class Curves(override val choiceName: String, override val at: (Float) -> Float) : Curve {
    LINEAR("Linear", { t ->
        t
    }),
    EASE_IN("EaseIn", { t ->
        t * t
    }),
    EASE_OUT ("EaseOut", { t ->
        1 - (1 - t) * (1 - t)
    }),
    EASE_IN_OUT ("EaseInOut", { t ->
        2 * (1 - t) * t * t + t * t
    })
}
