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

package net.ccbluex.liquidbounce.config.types

import net.ccbluex.liquidbounce.config.ConfigSystem.configs
import net.ccbluex.liquidbounce.config.ConfigSystem.rootFolder
import net.ccbluex.liquidbounce.config.types.group.ValueGroup
import java.io.File

open class Config(name: String, value: MutableCollection<Value<*>> = mutableListOf()) : ValueGroup(name, value) {

    val jsonFile: File
        get() {
            require(this in configs) { "${this.name} is not registered" }
            return File(rootFolder, "${this.loweredName}.json")
        }

}
