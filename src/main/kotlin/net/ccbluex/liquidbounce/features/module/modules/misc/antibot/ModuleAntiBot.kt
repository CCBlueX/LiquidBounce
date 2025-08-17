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
package net.ccbluex.liquidbounce.features.module.modules.misc.antibot

import com.mojang.authlib.GameProfile
import net.ccbluex.liquidbounce.event.events.TagEntityEvent
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.modes.CustomAntiBotMode
import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.modes.HorizonAntiBotMode
import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.modes.IntaveHeavyAntiBotMode
import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.modes.MatrixAntiBotMode
import net.minecraft.entity.Entity
import net.minecraft.entity.player.PlayerEntity

object ModuleAntiBot : ClientModule("AntiBot", Category.MISC) {

    val modes = choices("Mode", CustomAntiBotMode, arrayOf(
        CustomAntiBotMode,
        MatrixAntiBotMode,
        IntaveHeavyAntiBotMode,
        HorizonAntiBotMode
    ))

    private val literalNPC by boolean("LiteralNPC", false)

    @Suppress("unused")
    private val tagHandler = handler<TagEntityEvent> {
        if (isBot(it.entity)) {
           it.ignore()
        }
    }

    private fun reset() = this.modes.choices.forEach {
        (it as IAntiBotMode).reset()
    }

    override fun onDisabled() {
        reset()
    }

    @Suppress("unused")
    private val handleWorldChange = handler<WorldChangeEvent> {
        reset()
    }

    fun isADuplicate(profile: GameProfile): Boolean {
        return network.playerList.count { it.profile.name == profile.name && it.profile.id != profile.id } == 1
    }

    /**
     * Checks if the game profile is known at most once in the player list.
     *
     * Used to prevent false positives when a player is on a minigame such as Practice and joins a duel
     */
    fun isGameProfileUnique(profile: GameProfile): Boolean {
        return network.playerList.count { it.profile.name == profile.name && it.profile.id == profile.id } == 1
    }

    /**
     * Check if player might be a bot
     */
    fun isBot(player: Entity): Boolean {
        if (!running) {
            return false
        }

        if (player !is PlayerEntity) {
            return false
        }

        if (literalNPC && !network.playerUuids.contains(player.uuid)) {
            return true
        }

        return (this.modes.activeChoice as IAntiBotMode).isBot(player)
    }

    interface IAntiBotMode {
        fun reset() { }
        fun isBot(entity: PlayerEntity): Boolean
    }
}
