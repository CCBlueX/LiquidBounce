/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2023 CCBlueX
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

import net.ccbluex.liquidbounce.config.util.decode
import net.ccbluex.liquidbounce.utils.io.HttpClient
import java.util.*

object MojangApi {

    /**
     * Get UUID of username
     */
    fun getUUID(username: String): UUID? = runCatching {
        val text = HttpClient.get("https://api.mojang.com/users/profiles/minecraft/$username")
        val response = decode<ApiProfileResponse>(text)

        // Format UUID because otherwise it will be invalid
        val formattedUuid = response.id.replaceFirst(
            "(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})".toRegex(),
            "$1-$2-$3-$4-$5"
        )

        UUID.fromString(formattedUuid)
    }.onFailure {
        logger.error("Failed to get UUID of $username", it)
    }.getOrNull()

    data class ApiProfileResponse(val id: String, val name: String)

}
