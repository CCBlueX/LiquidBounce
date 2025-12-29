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

package net.ccbluex.liquidbounce.features.module.modules.render.hats

import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.render.hats.ModuleHats.modes
import net.ccbluex.liquidbounce.render.WorldRenderEnvironment
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.render.withPositionRelativeToCamera
import net.ccbluex.liquidbounce.utils.entity.interpolateCurrentPosition

/**
 * @author minecrrrr
 */
abstract class HatsMode(name: String) : Choice(name) {

    final override val parent: ChoiceConfigurable<*>
        get() = modes

    protected val height by float("HeightOffset", 0.1f, 0f..2f)
    protected val showInFirstPerson by boolean("FirstPersonView", true)

    protected abstract fun WorldRenderEnvironment.drawHat()

    @Suppress("unused")
    private val renderHandler = handler<WorldRenderEvent> {
        val player = mc.player ?: return@handler
        val pos = player.interpolateCurrentPosition(it.partialTicks)

        if (mc.options.cameraType.isFirstPerson && !showInFirstPerson) return@handler

        renderEnvironmentForWorld(it.matrixStack) {
            withPositionRelativeToCamera(pos.add(0.0, player.bbHeight + height.toDouble(), 0.0)) {
                drawHat()
            }
        }
    }

}
