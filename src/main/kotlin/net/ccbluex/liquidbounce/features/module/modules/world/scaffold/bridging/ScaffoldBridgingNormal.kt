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
package net.ccbluex.liquidbounce.features.module.modules.world.scaffold.bridging

import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features.ScaffoldEagleFeature
import net.ccbluex.liquidbounce.utils.block.targetfinding.AimMode

/**
 * Aims at the face of the next block, at the point [aimMode] picks.
 */
object ScaffoldBridgingNormal : ScaffoldBridging("Normal", "Sneak") {

    val aimMode by enumChoice("RotationMode", AimMode.STABILIZED)

    init {
        tree(ScaffoldEagleFeature)
    }

}
