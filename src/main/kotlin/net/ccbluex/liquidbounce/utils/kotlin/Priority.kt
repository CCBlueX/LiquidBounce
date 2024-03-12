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
package net.ccbluex.liquidbounce.utils.kotlin

enum class Priority(val priority: Int) {
    IMPORTANT_FOR_USER_SAFETY(60),

    /**
     * Scaffold, etc.
     */
    IMPORTANT_FOR_PLAYER_LIFE(40),
    IMPORTANT_FOR_USAGE_3(35),

    /**
     * KillAura, etc.
     */
    IMPORTANT_FOR_USAGE_2(30),
    IMPORTANT_FOR_USAGE_1(20),
    NORMAL(0),
    NOT_IMPORTANT(-20),
}

object EventPriorityConvention {

    /**
     * The event should be called first.
     */
    const val FIRST_PRIORITY: Int = 1000

    /**
     * At the stage of modeling what the player is actually going to do after other events added their suggestions
     */
    const val MODEL_STATE: Int = -10

    /**
     * Should be the one of the last functionalities that run, because the player safety depends on it.
     * Can be objected though by handlers with [OBJECTION_AGAINST_EVERYTHING] priority
     */
    const val SAFETY_FEATURE: Int = -50

    /**
     * Used when the event handler should be able to object anything that happened previously
     */
    const val OBJECTION_AGAINST_EVERYTHING: Int = -100

    /**
     * The event should be called last. It should not only be used for events that want to read the final state of the
     * event
     */
    const val READ_FINAL_STATE: Int = -1000
}
