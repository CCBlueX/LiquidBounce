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
package net.ccbluex.liquidbounce.utils.client

import java.util.function.BooleanSupplier

/**
 * Represents an operation that does not return a result and can only be executed once when [canExecute] returns true.
 * This is protected, so all future calls won't execute the actual [action].
 */
class RestrictedSingleUseAction(private val canExecute: BooleanSupplier, private val action: Runnable) {

    private var isExecuted = false

    operator fun invoke() {
        if (!isExecuted && canExecute.asBoolean) {
            action.run()
            isExecuted = true
        }
    }

}
