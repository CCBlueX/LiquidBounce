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
package net.ccbluex.liquidbounce.features.spoofer

import net.ccbluex.liquidbounce.config.types.nesting.Configurable
import net.ccbluex.liquidbounce.utils.client.exploitpreventer.ExpCompatibility

/**
 * Spoofer Manager
 *
 * Includes all spoofer features shown in the Multiplayer GUI.
 * Spoofers will usually allow fixes or spoof data sent to the server
 * to e.g., trick the server into thinking you are connecting from
 * another client brand.
 */
object SpooferManager : Configurable("Spoofer") {

    val usesExploitPreventer = runCatching {
        Class.forName("com.nikoverflow.exploitpreventer.ExploitPreventer")
        true
    }.getOrDefault(false)

    init {
        tree(SpooferClient)
        tree(SpooferResourcePack)
        tree(SpooferBungeeCord)

        if (usesExploitPreventer) {
            registerExpModules()
        } else {
            // Exploit Preventer comes with a fingerprint spoofer
            tree(SpooferFingerprint)
        }
    }

    private fun registerExpModules() {
        val modules = ExpCompatibility.INSTANCE.modules ?: return

        for ((expEnumName, expDisplayName) in modules) {
            tree(SpooferExploitPreventerModule(expEnumName, expDisplayName))
        }
    }

}
