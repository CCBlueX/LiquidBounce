/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2021 CCBlueX
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
package net.ccbluex.liquidbounce.config

import com.google.gson.annotations.SerializedName
import net.ccbluex.liquidbounce.utils.logger
import kotlin.reflect.jvm.isAccessible

open class Configurable(
    @SerializedName("name")
    val keyName: String,
    @SerializedName("configurables")
    val sub: MutableList<out Configurable> = mutableListOf(),
    @SerializedName("values")
    val values: MutableList<Value<*>> = mutableListOf()
) {

    /**
     * Overwrite current configurable and their existing values from [configurable].
     * [skipNew] allows to skip unknown new values and configurables.
     *
     * TODO: Find another way to overwrite configurable
     */
    fun overwrite(configurable: Configurable, skipNew: Boolean = true) {
        if (!skipNew || values.isNotEmpty()) {
            for (nev in configurable.values) {
                val oev = values.find { it.name == nev.name } ?: continue

                runCatching {
                    val ref = oev::value
                    if (!ref.isAccessible)
                        ref.isAccessible = true
                    ref.set(nev.value)
                }.onFailure {
                    logger.error("Unable to overwrite value ${oev.name}:value:${oev.value} to ${nev.value}", it)
                }
            }
        }

        if (!skipNew || sub.isNotEmpty()) {
            for (sun in configurable.sub) {
                val suo = sub.find { it.keyName == sun.keyName } ?: continue
                suo.overwrite(sun)
            }
        }
    }

}
