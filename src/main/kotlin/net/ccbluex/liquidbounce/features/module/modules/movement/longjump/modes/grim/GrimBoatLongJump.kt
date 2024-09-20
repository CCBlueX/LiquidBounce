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

package net.ccbluex.liquidbounce.features.module.modules.movement.longjump.modes.grim

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.PlayerTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.movement.longjump.ModuleLongJump
import net.ccbluex.liquidbounce.utils.entity.strafe
import net.ccbluex.liquidbounce.utils.entity.upwards
import net.minecraft.entity.Entity
import net.minecraft.entity.vehicle.BoatEntity

internal object GrimBoatLongJump : Choice("GrimBoat") {
    override val parent: ChoiceConfigurable<*>
        get() = ModuleLongJump.mode

    private val verticalLaunch by float("VerticalLaunch", 0.6f, 0.0f..1f)
    private val horizontalSpeed by float("HorizontalSpeed", 0.6f, 0.0f..1f)

    val box = player.boundingBox.expand(1.0)

    private fun inBoat(entity: Entity) =
        entity != player && entity is BoatEntity

    val tickHandler = handler<PlayerTickEvent> {
        for (entity in world.entities) {
            val entityBox = entity.boundingBox
            if (inBoat(entity) && box.intersects(entityBox)) {
                player.stopRiding()
                player.upwards(verticalLaunch)
                player.strafe(speed = horizontalSpeed.toDouble())
            }
        }
    }
}
