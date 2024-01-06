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
package net.ccbluex.liquidbounce.features.module.modules.misc

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.events.AttackEvent
import net.ccbluex.liquidbounce.event.events.KeyBindingEvent
import net.ccbluex.liquidbounce.event.events.PlayerNetworkMovementTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.aiming.Rotation
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.entity.box
import net.ccbluex.liquidbounce.utils.entity.eyes
import net.ccbluex.liquidbounce.utils.entity.rotation
import net.minecraft.entity.Entity
import java.io.File
import java.nio.file.Files

/**
 * ClickRecorder module
 *
 * Records your clicks which then can be used by cps-utilizing modules.
 */

object ModuleDebugRecorder : Module("DebugRecorder", Category.MISC) {
    private val dataPoints: ArrayList<DataPoint> = ArrayList()

    private var t0: Long? = null
    private var currentEnemy: Entity? = null
    private var attacked: Boolean = false

    data class DataPoint(
        @SerializedName("delta_t")
        val deltaT: Long,
        @SerializedName("target_rotation")
        val targetRotation: Rotation,
        @SerializedName("actual_rotation")
        val actualRotation: Rotation,
        val attacked: Boolean,
        @SerializedName("target_distance")
        val targetDistance: Double,
    )

    override fun enable() {
        currentEnemy = null
        attacked = false
        dataPoints.clear()
    }

    override fun disable() {
        // Write data points to JSON file
        Files.write(
            File(ConfigSystem.rootFolder, "debug-recorder.json").toPath(),
            Gson().toJson(dataPoints).toByteArray(),
        )

        chat("[DebugRecorder] Recorded and saved ${dataPoints.size} data points.")
    }

    val tickHandler =
        handler<PlayerNetworkMovementTickEvent> { event ->
            if (event.state != EventState.PRE) {
                return@handler
            }

            val enemy = this.currentEnemy ?: return@handler

            if (this.t0 == null) {
                this.t0 = System.currentTimeMillis()
            }

            val deltaT = System.currentTimeMillis() - this.t0!!

            val actualRotation = player.rotation
            val targetRotation = RotationManager.makeRotation(enemy.box.center, player.eyes)

            val dataPoint =
                DataPoint(deltaT, targetRotation, actualRotation, this.attacked, player.distanceTo(enemy).toDouble())

            this.attacked = false

            this.dataPoints += dataPoint
        }

    val attackHandler =
        handler<AttackEvent> { event ->
            val lastEnemy = this.currentEnemy

            this.currentEnemy = event.enemy
            this.t0 = System.currentTimeMillis()

            if (lastEnemy == null) {
                chat("[DebugRecorder] Enemy found. Started recording")
            } else if (lastEnemy != event.enemy) {
                chat("[DebugRecorder] Enemy changed. Stopping recording")
                disable()
            }
        }

    val attackClickHandler =
        handler<KeyBindingEvent> {
            if (it.key != mc.options.attackKey) {
                return@handler
            }

            this.attacked = true
        }
}
