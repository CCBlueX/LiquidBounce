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
package net.ccbluex.liquidbounce.gametest

import fi.dy.masa.litematica.data.DataManager
import fi.dy.masa.litematica.schematic.LitematicaSchematic
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement
import net.minecraft.core.BlockPos
import java.nio.file.Path
import kotlin.io.path.nameWithoutExtension

// In a file of its own, so Litematica's classes load only in runs that have it

/**
 * What a farm needs besides its blocks to run as its designer left it: the dispensers and droppers that held bone
 * meal, and the crafters with slots switched off.
 */
class SchematicSetup(val feeders: List<BlockPos>, val disabledSlots: Map<BlockPos, IntArray>)

/** Loads the schematic in [file] and places it with its origin at [origin], enabled, as a player does. */
fun placeSchematic(file: Path, origin: BlockPos): SchematicSetup {
    val schematic = checkNotNull(LitematicaSchematic.createFromFile(file.parent, file.fileName.toString())) {
        "Litematica could not read $file"
    }
    val placement = SchematicPlacement.createFor(schematic, origin, file.nameWithoutExtension, true, true)
    DataManager.getSchematicPlacementManager().addSchematicPlacement(placement, false)
    val feeders = ArrayList<BlockPos>()
    val disabledSlots = HashMap<BlockPos, IntArray>()
    for ((region, box) in schematic.areas) {
        val min = BlockPos.min(checkNotNull(box.pos1), checkNotNull(box.pos2))
        for ((pos, data) in schematic.getBlockEntityMapForRegion(region) ?: continue) {
            val at = origin.offset(min).offset(pos)
            when (data.getString("id")) {
                "minecraft:dispenser", "minecraft:dropper" ->
                    if ("minecraft:bone_meal" in data.toString()) feeders += at
                "minecraft:crafter" ->
                    data.getIntArray("disabled_slots").takeIf { it.isNotEmpty() }?.let { disabledSlots[at] = it }
            }
        }
    }
    return SchematicSetup(feeders, disabledSlots)
}

fun removeSchematics() {
    val manager = DataManager.getSchematicPlacementManager()
    manager.allSchematicsPlacements.toList().forEach { manager.removeSchematicPlacement(it, false) }
}
