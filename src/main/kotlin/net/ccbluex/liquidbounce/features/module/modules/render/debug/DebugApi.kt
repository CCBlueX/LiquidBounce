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
package net.ccbluex.liquidbounce.features.module.modules.render.debug

import net.ccbluex.liquidbounce.features.misc.DebuggedOwner

/**
 * Shows [value] as [name] under this owner in the Debug overview. [value] is only evaluated while Debug
 * captures this owner.
 */
inline fun DebuggedOwner.debugParameter(
    name: String,
    priority: DebugPriority = DebugPriority.NORMAL,
    value: () -> Any?,
) {
    val section = ModuleDebug.parameterSection(this) ?: return
    ModuleDebug.putParameter(section, name, priority, value())
}

/**
 * Draws [geometry] as [name] for this owner; null removes it.
 */
inline fun DebuggedOwner.debugGeometry(name: String, geometry: () -> DebuggedGeometry?) {
    val section = ModuleDebug.geometrySection(this) ?: return
    ModuleDebug.putGeometry(section, name, geometry())
}
