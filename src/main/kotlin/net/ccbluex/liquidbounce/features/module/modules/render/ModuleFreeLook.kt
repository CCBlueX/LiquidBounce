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

package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.event.events.MouseRotationEvent
import net.ccbluex.liquidbounce.event.events.PerspectiveEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.utils.input.InputBind
import net.minecraft.client.option.Perspective
import net.minecraft.client.option.Perspective.THIRD_PERSON_BACK
import net.minecraft.client.option.Perspective.THIRD_PERSON_FRONT

object ModuleFreeLook : ClientModule(
    "FreeLook", Category.RENDER, disableOnQuit = true, bindAction = InputBind.BindAction.HOLD
) {

    private val perspective by enumChoice("Perspective", PerspectiveChoice.BACK)
    private val senseBoost by float("SenseBoost", 1f, 0.1f..2f)
    private val noPitchLimit by boolean("NoPitchLimit", true)

    var cameraYaw = 0f
    var cameraPitch = 0f

    @get:JvmName("isInvertedView")
    val invertedView get() = perspective.perspective == THIRD_PERSON_FRONT

    override fun onEnabled() {
        cameraYaw = player.yaw
        cameraPitch = player.pitch
    }

    @Suppress("unused")
    private val handlePerspective = handler<PerspectiveEvent> { event ->
        event.perspective = perspective.perspective
    }

    @Suppress("unused")
    private val mouseRotationInputHandler = handler<MouseRotationEvent> { event ->
        cameraYaw += event.cursorDeltaX.toFloat() * 0.15f * senseBoost
        cameraPitch += event.cursorDeltaY.toFloat() * 0.15f * senseBoost

        if (!noPitchLimit) {
            cameraPitch = cameraPitch.coerceIn(-90f..90f)
        }

        event.cancelEvent()
    }

    @Suppress("unused")
    private enum class PerspectiveChoice(
        override val choiceName: String,
        val perspective: Perspective
    ) : NamedChoice {
        FRONT("Front", THIRD_PERSON_FRONT),
        BACK("Back", THIRD_PERSON_BACK)
    }
}
