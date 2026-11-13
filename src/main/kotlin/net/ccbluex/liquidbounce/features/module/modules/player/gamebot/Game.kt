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
package net.ccbluex.liquidbounce.features.module.modules.player.gamebot

import net.ccbluex.liquidbounce.config.types.group.Mode
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.ccbluex.liquidbounce.utils.item.isAxe
import net.ccbluex.liquidbounce.utils.item.isSword
import net.ccbluex.liquidbounce.utils.navigation.Navigator

/** A minigame [ModuleGameBot] plays, queued for with [queueCommand]. */
abstract class Game(name: String, queueCommand: String) : Mode(name) {

    final override val parent: ModeValueGroup<*>
        get() = ModuleGameBot.games

    val queueCommand by text("QueueCommand", queueCommand)

    /** Whether a round is on, from waiting for its start until it is won or lost. */
    abstract val isPlaying: Boolean

    /** Whether the round is on past its cages or countdown, so there is anyone to fight. */
    abstract val hasStarted: Boolean

    /** Whether the game fights an opponent right now, which needs KillAura whatever the distance. */
    abstract val isFighting: Boolean

    /** Whether the bot stays out of fights right now, so nothing should close in on an opponent. */
    open val keepsAway: Boolean
        get() = false

    /** Whether a chest is being opened, which strafing must not pull the player away from. */
    open val isOpeningChest: Boolean
        get() = false

    /** Whether the server respawns the player right now, so a death goes to [died] instead of ending the round. */
    open val respawns: Boolean
        get() = false

    /** The player died while [respawns]. */
    open fun died(reason: String) = Unit

    /** Whether the round is won with looted chests, so ChestStealer, AutoArmor and InventoryCleaner go on. */
    open val loots: Boolean
        get() = true

    /** Why Speed has to stay off right now, such as `bridge`, or null. */
    open fun speedHold(): String? = if (navigator?.isBuilding == true) "build" else null

    /** The server said the round starts. */
    abstract fun startAnnounced()

    /** The round is won or lost; nothing is played until the next one. */
    abstract fun end()

    /** What the bot does right now, as `state=... action=...` for the log; null outside a round. */
    open fun situation(): String? = null

    /** The walk of the game, read by [MapRecorder]. */
    open val navigator: Navigator?
        get() = null

    /** The phase of the round, such as `TRAVEL`, for [MapRecorder]. */
    open val phaseName: String?
        get() = null

    /** What the bot does right now, such as `walk` or `bridge`, for [MapRecorder]. */
    open val action: String?
        get() = null

    /** Whether a sword or an axe is anywhere in the inventory, for AutoWeapon to hold in a fight. */
    protected val hasWeapon: Boolean
        get() = Slots.All.any { it.itemStack.isSword || it.itemStack.isAxe }

}
