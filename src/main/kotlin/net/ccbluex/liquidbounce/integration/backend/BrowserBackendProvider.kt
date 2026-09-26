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
package net.ccbluex.liquidbounce.integration.backend

import net.ccbluex.liquidbounce.features.addon.AddonApi

/**
 * A browser engine to render the client's pages with.
 *
 * @param id Matches `LB_BROWSER_BACKEND` and the saved choice.
 * @param selectable False to offer it only through `LB_BROWSER_BACKEND`.
 */
@AddonApi
class BrowserBackendProvider @JvmOverloads constructor(
    val id: String,
    val name: String,
    val description: String,
    val selectable: Boolean = true,
    val create: () -> BrowserBackend,
)
