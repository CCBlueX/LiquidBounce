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
package net.ccbluex.liquidbounce.features.module.metadata

import net.ccbluex.liquidbounce.config.types.NamedChoice

enum class ModuleTag(override val choiceName: String) : NamedChoice {
    // Playstyle
    LEGIT("Legit"),
    BLATENT("Blatent"),

    // Minigame
    ANARCHY("Anarchy"),
    SMP("SMP"),

    // Survival
    PVP("PvP"),
    BEDWARS("BedWars"),
    SKYWARS("SkyWars")
}

enum class ModuleTagGroups(val tags: Set<ModuleTag>, val required: Boolean) {
    PLAYSTYLE(setOf(ModuleTag.LEGIT, ModuleTag.BLATENT), true),
    MINIGAME(setOf(ModuleTag.BEDWARS, ModuleTag.SKYWARS, ModuleTag.PVP), false),
    SURVIVAL(setOf(ModuleTag.ANARCHY, ModuleTag.SMP), false)
}
