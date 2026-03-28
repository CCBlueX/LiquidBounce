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

package net.ccbluex.liquidbounce.features.module.modules.misc

import kotlinx.atomicfu.atomic
import kotlinx.coroutines.delay
import net.ccbluex.fastutil.intListOf
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.event.events.AttackEntityEvent
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import kotlin.random.Random

object ModuleAutoL : ClientModule("AutoL", ModuleCategories.MISC){
    private val triggerDelay by intRange("Trigger Delay", 100..100, 0..1000)
    private val pattern by enumChoice("Pattern", AutoLPattern.RANDOM)
    private val messages by textList("Messages", mutableListOf(
        "First Break! The Sleeping Dragon Emerges from His Seclusion! You have already been eliminated by LiquidBounce Client!",
        "Double Kill! Rise to Fame with a Single Battle! You have already been eliminated by LiquidBounce Client!",
        "Triple Kill! The Whole World is Astounded! You have already been eliminated by LiquidBounce Client!",
        "Quadra Kill! Unmatched Across All Under Heaven! You have already been eliminated by LiquidBounce Client!",
        "Penta Kill! Conquer the Heavens and Annihilate the Earth! You have already been eliminated by LiquidBounce Client!",
        "Hexa Kill! Conquer the Heavens and Annihilate the Earth! You have already been eliminated by LiquidBounce Client!",
        "Hepta Kill! Conquer the Heavens and Annihilate the Earth! You have already been eliminated by LiquidBounce Client!"
    ))

    private val enemies = mutableListOf<Entity>()

    private val linear = atomic(0)

    @Volatile
    private var lineIndex = intListOf()

    @Suppress("unused")
    private val attackEntityEvent = handler<AttackEntityEvent> { event ->
        val entity = event.entity
        //only player will be added into enemies list
        if (entity is Player && !enemies.contains(entity)){
            enemies.add(entity)
        }
    }

    @Suppress("unused")
    private val tickHandler = tickHandler {
        enemies.filter { !it.isAlive }.forEach {
            delay(triggerDelay.random().toLong())
            val index = when (pattern) {
                AutoLPattern.RANDOM ->
                    lineIndex.getInt(Random.nextInt(lineIndex.size))
                AutoLPattern.LINEAR ->
                    lineIndex.getInt(linear.getAndIncrement() % lineIndex.size)
            }
            network.sendChat(it.name.contents.toString() + messages[index])
        }
    }

    @Suppress("unused")
    private val worldChangeHandler = handler<WorldChangeEvent> {
        enemies.clear()
    }

    enum class AutoLPattern(override val tag: String) : Tagged {
        RANDOM("Random"),
        LINEAR("Linear"),
    }
}
