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

package net.ccbluex.liquidbounce.features.module.modules.render.wings.modes

import com.mojang.math.Axis
import net.ccbluex.liquidbounce.features.module.modules.render.wings.WingsColorSettings
import net.ccbluex.liquidbounce.features.module.modules.render.wings.WingsMode
import net.ccbluex.liquidbounce.render.ClientRenderPipelines
import net.ccbluex.liquidbounce.render.WorldRenderEnvironment
import net.ccbluex.liquidbounce.render.drawCustomMesh
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.setColor
import net.ccbluex.liquidbounce.render.withPush
import org.joml.Matrix4f

internal object WingsLines : WingsMode("Lines") {

    private val colors = WingsColorSettings()

    private object WingsOptions {
    }

    override fun WorldRenderEnvironment.drawWings(isHurt: Boolean, bodyRot: Float) {

    }
}
