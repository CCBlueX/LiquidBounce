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
package net.ccbluex.liquidbounce.features.module.modules.combat.killaura.features

import com.google.gson.JsonObject
import net.ccbluex.liquidbounce.LiquidBounce.logger
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.types.Value
import net.ccbluex.liquidbounce.config.types.ValueType
import net.ccbluex.liquidbounce.features.module.MinecraftShortcuts
import net.ccbluex.liquidbounce.utils.kotlin.random
import net.ccbluex.liquidbounce.utils.range.RangeValueGroup
import kotlin.math.max

/**
 * Allows adjusting your attack range and scan range.
 */
object KillAuraRange : RangeValueGroup("Range", 1f, 3f), MinecraftShortcuts {

    internal val scanRange
        get() = maxOf(interactionRange, interactionThroughWallsRange) + currentScanRangeAddition

    private var scanRangeIncrease by floatRange(
        "ScanRangeIncrease",
        2.0f..3.0f,
        0.0f..7.0f,
        "blocks"
    ).onChanged { range ->
        currentScanRangeAddition = range.random()
    }
    private var currentScanRangeAddition: Float = scanRangeIncrease.random()

    fun update() {
        currentScanRangeAddition = scanRangeIncrease.random()
    }

    /**
     * Migrates the old values from the config.
     *
     * todo: remove this when no one uses the format anymore
     */
    fun migrateFromValues(map: Map<String, JsonObject>) {
        if (!map.containsKey("WallRange") || !map.containsKey("ScanExtraRange")) {
            // This cannot be an old format.
            return
        }

        this.maxRangeIncrease = max(0f, withDummy("Range", map["Range"]!!, 4.2f) - 3f)
        this.throughWallsRange = withDummy("WallRange", map["WallRange"]!!, 3f)
        this.scanRangeIncrease = withDummy("ScanExtraRange", map["ScanExtraRange"]!!, 2f..3f)
        logger.info("KillAura Range Config migrated from old format.")
    }

    private fun <T : Any> withDummy(name: String, jsonObject: JsonObject, value: T): T {
        val dummy = Value(name, defaultValue = value, valueType = ValueType.INVALID)
        ConfigSystem.deserializeValue(dummy, jsonObject)
        return dummy.get()
    }

}
