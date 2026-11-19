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
package net.ccbluex.liquidbounce.api.models.user

import net.ccbluex.liquidbounce.api.models.cosmetics.Cosmetic

/**
 * @param permissions of all roles together, such as `cosmetics` or `moderate`
 * @param cosmetics disabled ones included
 */
data class UserInformation(
    val uid: String,
    val email: String,
    val name: String?,
    val nickname: String,
    val roles: List<Role>,
    val permissions: Set<String>,
    val premium: Boolean,
    val cosmetics: Set<Cosmetic>
) {
    data class Role(val id: String, val displayName: String)
}
