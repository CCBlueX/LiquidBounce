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

package net.ccbluex.liquidbounce.integration.backend.browser

sealed class BrowserState private constructor(val isCompleted: Boolean) {
    data object Idle : BrowserState(false)
    data object Stateless : BrowserState(true)
    data object Loading : BrowserState(false)
    data class Success(val httpStatusCode: Int) : BrowserState(true)
    data class Failure(
        val errorCode: Int,
        val errorText: String,
        val failedUrl: String
    ) : BrowserState(true)
}
