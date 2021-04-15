/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2021 CCBlueX
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
package net.ccbluex.liquidbounce.utils.combat

import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.Configurable
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.entity.boxedDistanceTo
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.client.world.ClientWorld
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.mob.MobEntity
import net.minecraft.entity.passive.PassiveEntity
import net.minecraft.entity.player.PlayerEntity

val globalEnemyConfigurable = EnemyConfigurable()

/**
 * Configurable to configure which entities and their state (like being dead) should be considered as enemy
 */
class EnemyConfigurable : Configurable("enemies") {

    // Players should be considered as a enemy
    val players by boolean("Players", true)

    // Hostile mobs (like skeletons and zombies) should be considered as a enemy
    val mobs by boolean("Mobs", true)

    // Animals (like cows, pigs and so on) should be considered as a enemy
    val animals by boolean("Animals", false)

    // Invisible entities should be also considered as a enemy
    var invisible by boolean("Invisible", true)

    // Dead entities should be also considered as a enemy to bypass modern anti cheat techniques
    var dead by boolean("Dead", false)

    // Friends (client friends - other players) should be also considered as enemy
    val friends by boolean("Friends", false)

    // Should bots be blocked to bypass anti cheat techniques
    val antibot = tree(AntiBotConfigurable())

    class AntiBotConfigurable : Configurable("AntiBot") {

        /**
         * Should always be enabled. A good antibot should never detect a real player as a bot (on default settings).
         */
        val enabled by boolean("Enabled", true)

        /**
         * Check if player might be a bot
         */
        fun isBot(player: ClientPlayerEntity): Boolean {
            if (!enabled) {
                return false
            }

            return false
        }

    }

    init {
        ConfigSystem.root(this)
    }

    /**
     * Check if entity is considered a enemy
     */
    fun isEnemy(enemy: Entity, attackable: Boolean = false): Boolean {
        // Check if enemy is living and not dead (or ignore being dead)
        if (enemy is LivingEntity && (dead || enemy.isAlive)) {
            // Check if enemy is invisible (or ignore being invisible)
            if (invisible || !enemy.isInvisible) {
                // Check if enemy is a player and should be considered as enemy
                if (enemy is PlayerEntity && players && enemy != mc.player) {
                    // TODO: Check friends because there is no friend system right now

                    // Check if player might be a bot
                    if (enemy is ClientPlayerEntity && antibot.isBot(enemy)) {
                        return false
                    }

                    return true
                } else if (enemy is PassiveEntity && animals) {
                    return true
                } else if (enemy is MobEntity && mobs) {
                    return true
                }
            }
        }

        return false
    }

}

// Extensions

fun Entity.shouldBeShown(enemyConf: EnemyConfigurable = globalEnemyConfigurable) = enemyConf.isEnemy(this)

fun Entity.shouldBeAttacked(enemyConf: EnemyConfigurable = globalEnemyConfigurable) = enemyConf.isEnemy(
    this,
    true
)

/**
 * Find the best emeny in current world in a specific range.
 */
fun ClientWorld.findEnemy(
    range: Float,
    player: Entity = mc.player!!,
    enemyConf: EnemyConfigurable = globalEnemyConfigurable
) = entities.filter { enemyConf.isEnemy(it, true) }
    .map { Pair(it, it.boxedDistanceTo(player)) }
    .filter { (_, distance) -> distance <= range }
    .minByOrNull { (_, distance) -> distance }
