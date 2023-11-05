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
 *
 */

package net.ccbluex.liquidbounce.features.misc

import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.SharedConstants
import net.minecraft.client.util.Icons

/**
 * Hides client feature
 *
 * This object is separate from the module because we do not want to initialize the module too early at start-up
 */
object HideClient {

    /**
     * We need this variable because the module state will be updated after
     */
    var isHidingNow = false
        set(value) {
            field = value
            updateClient()
        }

    private fun updateClient() {
        mc.updateWindowTitle()
        mc.window.setIcon(
            mc.defaultResourcePack,
            if (SharedConstants.getGameVersion().isStable) Icons.RELEASE else Icons.SNAPSHOT)
    }

}
