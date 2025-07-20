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
package net.ccbluex.liquidbounce.utils.aiming

import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.PlayerNetworkMovementTickEvent
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.FIRST_PRIORITY

/**
 * Executes code right after the client sent the normal movement packet or at the start of the next tick.
 */
object PostRotationExecutor : EventListener {

    /**
     * This should be used by actions that depend on the rotation sent in the tick movement packet.
     */
    private var priorityAction: Pair<ClientModule, () -> Unit>? = null

    private var priorityActionPostMove = false

    /**
     * All other actions that should be executed on post-move.
     */
    private val postMoveTasks = ArrayDeque<Pair<ClientModule, () -> Unit>>()

    /**
     * All other actions that should be executed on tick.
     */
    private val normalTasks = ArrayDeque<Pair<ClientModule, () -> Unit>>()

    @Suppress("unused")
    val worldChangeHandler = handler<WorldChangeEvent> {
        postMoveTasks.clear()
        normalTasks.clear()
    }

    /**
     * Executes the currently waiting actions.
     *
     * Has [EventPriorityConvention.FIRST_PRIORITY] to run before any other module can send packets.
     */
    @Suppress("unused")
    val networkMoveHandler = handler<PlayerNetworkMovementTickEvent>(priority = FIRST_PRIORITY) { event ->
        if (event.state != EventState.POST) {
            return@handler
        }

        // if the priority action doesn't run on post-move, no other action can
        val preventedByAction = !priorityActionPostMove && priorityAction != null

        // another module might need the rotation,
        // then we can't run the actions on post-move because they might change the rotation with packets,
        // but if the priority action is not null, the rotation got most likely set because of it
        val preventedByCurrentRot = RotationManager.currentRotation != null && priorityAction == null
        if (preventedByAction || preventedByCurrentRot) {
           return@handler
        }

        priorityAction?.let { action ->
            if (action.first.running) {
                action.second.invoke()
            }
        }

        priorityAction = null

        // execute all other actions
        while (postMoveTasks.isNotEmpty()) {
            val next = postMoveTasks.removeFirst()
            if (next.first.running) {
                next.second.invoke()
            }
        }
    }

    /**
     * Executes the currently waiting actions.
     *
     * Has [EventPriorityConvention.FIRST_PRIORITY] to run before any other module can send packets.
     */
    @Suppress("unused")
    val tickHandler = handler<GameTickEvent>(priority = FIRST_PRIORITY) {
        if (!priorityActionPostMove) {
            // execute the priority action
            priorityAction?.let { action ->
                if (action.first.running) {
                    action.second.invoke()
                }
            }

            priorityAction = null
        }

        // if we reach this point, the post-move queue should be empty, if not it gets cleared here
        while (postMoveTasks.isNotEmpty()) {
            val next = postMoveTasks.removeFirst()
            if (next.first.running) {
                next.second.invoke()
            }
        }

        // execute all other actions
        while (normalTasks.isNotEmpty()) {
            val next = normalTasks.removeFirst()
            if (next.first.running) {
                next.second.invoke()
            }
        }
    }

    fun addTask(module: ClientModule, postMove: Boolean, task: () -> Unit, priority: Boolean = false) {
        if (priority) {
            priorityAction = module to task
            priorityActionPostMove = postMove
        } else if (postMove) {
            postMoveTasks.add(module to task)
        } else {
            normalTasks.add(module to task)
        }
    }

}
