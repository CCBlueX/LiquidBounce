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
package net.ccbluex.liquidbounce.addon

import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.RefreshArrayListEvent
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleClickGui

/**
 * The client's own screens and HUD.
 */
object Ui {

    /**
     * Re-reads the module list in the ClickGUI and the HUD, e.g. after a module's tag or visible settings
     * changed.
     */
    @JvmStatic
    fun refresh() {
        EventManager.callEvent(RefreshArrayListEvent)
        ModuleClickGui.sync()
    }

    /**
     * Whether the key opening the ClickGUI is [keyCode]/[scanCode], so another GUI can leave it alone.
     */
    @JvmStatic
    fun isBoundToKey(keyCode: Int, scanCode: Int): Boolean = ModuleClickGui.bind.matchesKey(keyCode, scanCode)

    @JvmStatic
    fun isBoundToMouse(button: Int): Boolean = ModuleClickGui.bind.matchesMouse(button)

}
