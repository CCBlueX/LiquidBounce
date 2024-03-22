/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.NotificationEvent
import net.ccbluex.liquidbounce.event.events.PlayerTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.notification
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket

/**
 * NoWeb module
 *
 * Disables web slowdown.
 */
object ModuleNoWeb : Module("NoWeb", Category.MOVEMENT) {

    init {
        enableLock()
    }

    val modes = choices("Mode", Air, arrayOf(Air, GrimBreak))

    val repeatable = repeatable {
        if (ModuleAvoidHazards.enabled) {
            if (ModuleAvoidHazards.cobWebs) {
                ModuleAvoidHazards.enabled = false
                notification(
                    "Compatibility error", "NoWeb is incompatible with AvoidHazards",
                    NotificationEvent.Severity.ERROR
                )
            }
        }
    }

    object Air : Choice("Air") {
        override val parent: ChoiceConfigurable
            get() = modes

        // Mixins take care of anti web slowdown.
    }

    object GrimBreak : Choice("GrimBreak") {

        override val parent: ChoiceConfigurable
            get() = modes


        // Mixins do everything
    }
}
