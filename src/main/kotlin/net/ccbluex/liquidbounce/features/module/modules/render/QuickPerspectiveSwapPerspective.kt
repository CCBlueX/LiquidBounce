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
package net.ccbluex.liquidbounce.features.module.modules.render

import net.minecraft.client.CameraType

/**
 * Use the vanilla camera views instead of rotating a second camera on top of F5.
 * The first-person view swaps to the selected rear/front view; both third-person
 * views swap with each other. Releasing the key restores the vanilla view.
 */
internal fun swappedPerspective(current: CameraType, rearView: Boolean): CameraType =
    when (current) {
        CameraType.FIRST_PERSON -> if (rearView) CameraType.THIRD_PERSON_BACK else CameraType.THIRD_PERSON_FRONT
        CameraType.THIRD_PERSON_BACK -> CameraType.THIRD_PERSON_FRONT
        CameraType.THIRD_PERSON_FRONT -> CameraType.THIRD_PERSON_BACK
    }
