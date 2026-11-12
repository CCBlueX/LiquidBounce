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
package net.ccbluex.liquidbounce.features.global

import net.ccbluex.liquidbounce.config.types.group.ValueGroup
import net.ccbluex.liquidbounce.lang.translation
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.variable
import net.minecraft.world.phys.Vec3
import java.text.DecimalFormat

/** Options of the `.teleport`, `.playerteleport` and `.vclip` commands. */
object GlobalSettingsTeleport : ValueGroup(name = "Teleport") {

    val highTp by boolean("HighTP", false)
    val highTpAmount by float("HighTPAmount", 200.0F, 0.0F..500.0F)
    private val resetMotion by boolean("ResetMotion", true)

    private val decimalFormat = DecimalFormat("##0.000")

    fun teleport(x: Double, y: Double, z: Double) {
        teleportPlayer(x, y, z, resetMotion)

        chat(regular(
            translation(
                "liquidbounce.teleport.teleported",
                variable(decimalFormat.format(x)),
                variable(decimalFormat.format(y)),
                variable(decimalFormat.format(z)),
            )
        ))
    }

    fun teleportPlayer(x: Double, y: Double, z: Double, resetMotion: Boolean) {
        val entity = player.vehicle ?: player
        entity.absSnapTo(x, y, z)

        if (resetMotion) {
            entity.deltaMovement = Vec3.ZERO
        }
    }

}
