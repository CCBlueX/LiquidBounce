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

package net.ccbluex.liquidbounce.test

import net.minecraft.SharedConstants
import net.minecraft.server.Bootstrap

/**
 * Initializes the minimal vanilla bootstrap required by tests that touch registry-backed or statically bootstrapped
 * Minecraft classes.
 *
 * @see net.minecraft.SharedConstants.tryDetectVersion
 * @see net.minecraft.server.Bootstrap.bootStrap
 */
object MinecraftBootstrap {

    private val lock = Any()
    @Volatile
    private var initialized = false

    fun ensureInitialized() {
        if (initialized) {
            return
        }

        synchronized(lock) {
            if (initialized) {
                return
            }

            SharedConstants.tryDetectVersion()
            Bootstrap.bootStrap()

            initialized = true
        }
    }

}
