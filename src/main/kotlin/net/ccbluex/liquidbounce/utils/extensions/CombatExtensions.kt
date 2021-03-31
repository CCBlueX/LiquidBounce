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
package net.ccbluex.liquidbounce.utils.extensions

import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.Configurable
import net.ccbluex.liquidbounce.utils.mc
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.client.world.ClientWorld
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.mob.MobEntity
import net.minecraft.entity.passive.PassiveEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

val globalEnemyConfigurable = EnemyConfigurable()

/**
 * Configurable to configure which entities and their state (like being dead) should be considered as enemy
 *
 * TODO: Might make enemy configurable per module (like use global target settings or not)
 *   This is the reason why enemy configurable is not a object
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
            if (!enabled)
                return false


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
                }else if (enemy is PassiveEntity && animals) {
                    return true
                }else if (enemy is MobEntity && mobs) {
                    return true
                }
            }
        }

        return false
    }

}

// Extensions

fun Entity.shouldBeShown(enemyConf: EnemyConfigurable = globalEnemyConfigurable) = enemyConf.isEnemy(this)

fun Entity.shouldBeAttacked(enemyConf: EnemyConfigurable = globalEnemyConfigurable) = enemyConf.isEnemy(this,
    true)

/**
 * Find the best emeny in current world in a specific range.
 */
fun ClientWorld.findEnemy(range: Float, player: Entity = mc.player!!, enemyConf: EnemyConfigurable = globalEnemyConfigurable)
    = entities.filter { enemyConf.isEnemy(it, true) }
        .map { Pair(it, it.boxedDistanceTo(player)) }
        .filter { (_, distance) -> distance <= range }
        .minByOrNull { (_, distance) -> distance }

/**
 * Allows to calculate the distance between the current entity and [entity] from the nearest corner of the bounding box
 */
fun Entity.boxedDistanceTo(entity: Entity): Double {
    val eyes = entity.getCameraPosVec(1F)
    val pos = getNearestPoint(eyes, boundingBox)
    val xDist = abs(pos.x - eyes.x)
    val yDist = abs(pos.y - eyes.y)
    val zDist = abs(pos.z - eyes.z)
    return sqrt(xDist.pow(2) + yDist.pow(2) + zDist.pow(2))
}

/**
 * Get the nearest point of a box. Very useful to calculate the distance of an enemy.
 */
fun getNearestPoint(eye: Vec3d, box: Box): Vec3d {
    val origin = doubleArrayOf(eye.x, eye.y, eye.z)
    val destMins = doubleArrayOf(box.minX, box.minY, box.minZ)
    val destMaxs = doubleArrayOf(box.maxX, box.maxY, box.maxZ)

    // It loops through every coordinate of the double arrays and picks the nearest point
    for (i in 0..2) {
        if (origin[i] > destMaxs[i])
            origin[i] = destMaxs[i]
        else if (origin[i] < destMins[i])
            origin[i] = destMins[i]
    }

    return Vec3d(origin[0], origin[1], origin[2])
}
