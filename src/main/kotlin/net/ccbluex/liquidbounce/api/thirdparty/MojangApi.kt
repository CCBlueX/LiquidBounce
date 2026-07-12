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

package net.ccbluex.liquidbounce.api.thirdparty

import com.mojang.util.UndashedUuid
import net.ccbluex.liquidbounce.api.core.HttpClient.mojangApiClient
import java.util.UUID

internal suspend fun lookupUuidByName(username: String): UUID? = try {
    UndashedUuid.fromString(mojangApiClient.mcServicesApi.lookupUuidByName(username).id)
} catch (e: retrofit2.HttpException) {
    if (e.code() == 404) null else throw IllegalStateException("Failed to execute profile lookup", e)
}
