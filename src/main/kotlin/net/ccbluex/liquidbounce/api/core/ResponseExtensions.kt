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

package net.ccbluex.liquidbounce.api.core

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import net.ccbluex.liquidbounce.config.gson.util.GSON_DEFAULT
import okhttp3.Response
import okio.sink
import java.io.File

inline fun <reified T> Response.bodyAsJson(gson: Gson = GSON_DEFAULT): T =
    bodyAsJson(gson, object : TypeToken<T>() {})

fun <T> Response.bodyAsJson(gson: Gson, type: TypeToken<T>): T = use {
    gson.fromJson(it.body.charStream(), type)
}

fun Response.bodyToFile(file: File) = use {
    file.sink().use(it.body.source()::readAll)
}
