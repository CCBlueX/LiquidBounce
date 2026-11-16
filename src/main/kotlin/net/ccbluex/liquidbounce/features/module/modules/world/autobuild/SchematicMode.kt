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
package net.ccbluex.liquidbounce.features.module.modules.world.autobuild

import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import net.ccbluex.agent.v26_3.fabric.litematica.Placements
import net.ccbluex.agent.skills.build.BuildResult
import net.ccbluex.agent.skills.build.build
import net.ccbluex.agent.tick.TickScope
import net.ccbluex.agent.v26_3.toMinecraft
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.markAsError
import net.ccbluex.liquidbounce.utils.inventory.HotbarItemSlot
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.CRITICAL_MODIFICATION
import net.ccbluex.liquidbounce.utils.navigation.Navigator

/**
 * Builds the enabled Litematica placements with Minecraft Agent's build skill: walks to each block, places it facing
 * the way the schematic has it, and reports what it lacked.
 */
object SchematicMode : ModuleAutoBuild.AutoBuildMode("Schematic") {

    private val breakWrong by boolean("BreakWrongBlocks", false)

    private val navigator = Navigator(this, this)
    private val scope = TickScope()
    private var job: Job? = null

    override fun enabled() {
        val blocks = Placements.blocks()
        if (blocks.isEmpty()) {
            chat(markAsError(ModuleAutoBuild.message("noPlacement")), ModuleAutoBuild)
            // The enabled value is committed after onEnabled returns, so defer the disable.
            mc.schedule { ModuleAutoBuild.enabled = false }
            return
        }
        val launched = scope.launch { report(navigator.agent.build(blocks, breakWrong)) }
        job = launched
        launched.invokeOnCompletion { mc.schedule { if (job === launched) ModuleAutoBuild.enabled = false } }
    }

    override fun disabled() {
        job = null
        scope.cancel()
        navigator.reset()
    }

    @Suppress("unused")
    private val inputHandler = handler<MovementInputEvent>(priority = CRITICAL_MODIFICATION) { event ->
        if (job == null) {
            return@handler
        }
        scope.tick()
        navigator.tick(event)
    }

    private fun report(result: BuildResult) {
        if (result.isDone) {
            chat(ModuleAutoBuild.message("built"), ModuleAutoBuild)
            return
        }
        chat(markAsError(ModuleAutoBuild.message("unbuilt", result.unbuilt.size)), ModuleAutoBuild)
        if (result.missing.isNotEmpty()) {
            val items = result.missing.entries.joinToString { (item, count) ->
                "$count ${item.toMinecraft().defaultInstance.hoverName.string}"
            }
            chat(markAsError(ModuleAutoBuild.message("missing", items)), ModuleAutoBuild)
        }
    }

    override fun getSlot(): HotbarItemSlot? = null

}
