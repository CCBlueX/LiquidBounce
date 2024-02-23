/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2024 CCBlueX
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
 *
 */

package net.ccbluex.liquidbounce.features.module.modules.movement.fly.modes.fireball

import net.ccbluex.liquidbounce.config.*
import net.ccbluex.liquidbounce.event.events.NotificationEvent
import net.ccbluex.liquidbounce.event.events.SimulatedTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.ModuleFly
import net.ccbluex.liquidbounce.utils.aiming.Rotation
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsConfigurable
import net.ccbluex.liquidbounce.utils.client.notification
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.minecraft.item.Items
import net.minecraft.util.Hand

/**
 * Legit fireball jump fly
 */
internal object FlyFireball : Choice("Fireball") {

    override val parent: ChoiceConfigurable
        get() = ModuleFly.modes

    private val rotations = tree(RotationsConfigurable(50f..80f))
    private val pitch by float("Rotation Pitch", 75f, 0f..90f)

    val technique = choices(ModuleFly, "Technique",
        FlyFireballHighTechnique,
        arrayOf(FlyFireballHighTechnique, FlyFireballLongTechnique, FlyFireballCustomTechnique, FlyFireballStompTechnique))

    private val rotationUpdateHandler = handler<SimulatedTickEvent> {
        RotationManager.aimAt(
            rotations.toAimPlan(Rotation(player.yaw, pitch)),
            priority = Priority.IMPORTANT_FOR_USAGE_2,
            provider = ModuleFly
        )
    }

    val moduleRepeatable = repeatable {
        if (mc.player?.mainHandStack?.item == Items.FIRE_CHARGE) {
            if (mc.player?.isOnGround == true)
                mc.player?.jump()

            var ticks = 0

            when (technique.activeChoice) {
                FlyFireballHighTechnique -> ticks = 1
                FlyFireballStompTechnique -> ticks = 7
                FlyFireballLongTechnique -> ticks = 4
                FlyFireballCustomTechnique -> ticks = (technique.activeChoice as FlyFireballCustomTechnique).customDelay
            }

            waitTicks(ticks)
            mc.interactionManager?.interactItem(mc.player, Hand.MAIN_HAND)
            ModuleFly.enabled = false
        } else {
            notification("Fly", "You need to hold a fireball!", NotificationEvent.Severity.ERROR)
            ModuleFly.enabled = false
            return@repeatable
        }
    }

}
