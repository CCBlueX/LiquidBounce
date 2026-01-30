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
package net.ccbluex.liquidbounce.features.module.modules.player.autoqueue.presets

import kotlinx.coroutines.Dispatchers
import net.ccbluex.liquidbounce.config.types.group.Mode
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.event.tickUntil
import net.ccbluex.liquidbounce.event.waitTicks
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.ModuleSpeed
import net.ccbluex.liquidbounce.features.module.modules.player.autoqueue.ModuleAutoQueue
import net.ccbluex.liquidbounce.features.module.modules.player.autoqueue.actions.AutoQueueActionChat
import net.ccbluex.liquidbounce.features.module.modules.player.autoqueue.actions.AutoQueueActionUseItem
import net.ccbluex.liquidbounce.features.module.modules.player.autoqueue.trigger.AutoQueueTriggerItem
import net.ccbluex.liquidbounce.features.module.modules.player.autoqueue.trigger.AutoQueueTriggerMessage
import net.ccbluex.liquidbounce.features.module.modules.player.autoqueue.trigger.AutoQueueTriggerSubtitle
import net.ccbluex.liquidbounce.features.module.modules.player.autoqueue.trigger.AutoQueueTriggerTabFooter
import net.ccbluex.liquidbounce.features.module.modules.player.autoqueue.trigger.AutoQueueTriggerTabHeader
import net.ccbluex.liquidbounce.features.module.modules.player.autoqueue.trigger.AutoQueueTriggerTitle
import net.ccbluex.liquidbounce.utils.kotlin.Minecraft

object AutoQueueCustom : Mode("Custom") {

    override val parent: ModeValueGroup<*>
        get() = ModuleAutoQueue.presets

    internal val triggers = modes("Trigger", 0) {
        arrayOf(
            AutoQueueTriggerTitle,
            AutoQueueTriggerSubtitle,
            AutoQueueTriggerMessage,
            AutoQueueTriggerItem,
            AutoQueueTriggerTabHeader,
            AutoQueueTriggerTabFooter
        )
    }

    internal val actions = modes("Action", 0) {
        arrayOf(
            AutoQueueActionChat,
            AutoQueueActionUseItem
        )
    }

    private object AutoQueueControl : ToggleableValueGroup(this, "Control", true) {

        val killAura by boolean("KillAura", true)
        val speed by boolean("Speed", false)

        var wasInQueue = false
            set(value) {
                field = value

                if (!enabled) {
                    return
                }

                if (killAura) ModuleKillAura.enabled = !value
                if (speed) ModuleSpeed.enabled = !value
            }

    }

    init {
        tree(AutoQueueControl)
    }

    private val waitUntilWorldChange by boolean("WaitUntilWorldChange", true)
    private var worldChangeOccurred = false

    @Suppress("unused")
    private val tickHandler = tickHandler(Dispatchers.Minecraft) {
        val trigger = triggers.activeMode

        if (trigger.isTriggered) {
            AutoQueueControl.wasInQueue = true

            actions.activeMode.execute()

            if (waitUntilWorldChange) {
                tickUntil { worldChangeOccurred }
                worldChangeOccurred = false
            }
            waitTicks(20)
        } else if (AutoQueueControl.enabled && AutoQueueControl.wasInQueue) {
            AutoQueueControl.wasInQueue = false
        }
    }

    @Suppress("unused")
    private val worldChange = handler<WorldChangeEvent> { event ->
        worldChangeOccurred = true
    }

}
