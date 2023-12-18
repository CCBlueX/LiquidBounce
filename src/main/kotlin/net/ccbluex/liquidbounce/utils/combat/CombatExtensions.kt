/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2023 CCBlueX
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
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.AttackEvent
import net.ccbluex.liquidbounce.features.misc.FriendManager
import net.ccbluex.liquidbounce.features.module.modules.misc.ModuleFocus
import net.ccbluex.liquidbounce.features.module.modules.misc.ModuleTeams
import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.ModuleAntiBot
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleMurderMystery
import net.ccbluex.liquidbounce.utils.client.isOldCombat
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.entity.squaredBoxedDistanceTo
import net.ccbluex.liquidbounce.utils.kotlin.toDouble
import net.minecraft.client.network.AbstractClientPlayerEntity
import net.minecraft.client.world.ClientWorld
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.mob.HostileEntity
import net.minecraft.entity.mob.Monster
import net.minecraft.entity.passive.PassiveEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket
import net.minecraft.util.Hand
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d

/**
 * Global enemy configurable
 *
 * Modules can have their own enemy configurable if required. If not, they should use this as default.
 * Global enemy configurable can be used to configure which entities should be considered as an enemy.
 *
 * This can be adjusted by the .enemy command and the panel inside the ClickGUI.
 */
val globalEnemyConfigurable = EnemyConfigurable()

/**
 * Configurable to configure which entities and their state (like being dead) should be considered as an enemy
 */
class EnemyConfigurable : Configurable("Enemies") {

    // Players should be considered as an enemy
    var players by boolean("Players", true)

    // Hostile mobs (like skeletons and zombies) should be considered as an enemy
    var hostile by boolean("Hostile", true)

    // Passive mobs (like cows, pigs and so on) should be considered as an enemy
    var passive by boolean("Passive", false)

    // Invisible entities should be also considered as an enemy
    var invisible by boolean("Invisible", true)

    // Dead entities should NOT be considered as an enemy - but this is useful to bypass anti-cheats
    var dead by boolean("Dead", false)

    // Sleeping entities should NOT be considered as an enemy
    var sleeping by boolean("Sleeping", false)

    // Friends (client friends - other players) should be also considered as enemy - similar to module NoFriends
    var friends by boolean("Friends", false)

    init {
        ConfigSystem.root(this)
    }

    /**
     * Check if an entity is considered an enemy
     */
    fun isTargeted(suspect: Entity, attackable: Boolean = false): Boolean {
        // Check if the enemy is living and not dead (or ignore being dead)
        if (suspect is LivingEntity && (dead || suspect.isAlive)) {
            // Check if enemy is sleeping (or ignore being sleeping)
            if (suspect.isSleeping && !sleeping) {
                return false
            }

            // Check if enemy is invisible (or ignore being invisible)
            if (invisible || !suspect.isInvisible) {
                if (attackable && ModuleTeams.isInClientPlayersTeam(suspect)) {
                    return false
                }

                // Check if enemy is a player and should be considered as an enemy
                if (suspect is PlayerEntity && suspect != mc.player) {
                    if (attackable && !friends && FriendManager.isFriend(suspect)) {
                        return false
                    }

                    if (suspect is AbstractClientPlayerEntity) {
                        if (ModuleFocus.enabled && (attackable || !ModuleFocus.combatFocus)
                            && !ModuleFocus.isInFocus(suspect)) {
                            return false
                        }

                        if (attackable && ModuleMurderMystery.enabled && !ModuleMurderMystery.shouldAttack(suspect)) {
                            return false
                        }
                    }

                    // Check if player might be a bot
                    if (ModuleAntiBot.isBot(suspect)) {
                        return false
                    }

                    return players
                } else if (suspect is PassiveEntity) {
                    return passive
                } else if (suspect is HostileEntity || suspect is Monster) {
                    return hostile
                }
            }
        }

        return false
    }

}

// Extensions

@JvmOverloads
fun Entity.shouldBeShown(enemyConf: EnemyConfigurable = globalEnemyConfigurable) = enemyConf.isTargeted(this)

fun Entity.shouldBeAttacked(enemyConf: EnemyConfigurable = globalEnemyConfigurable) = enemyConf.isTargeted(
    this,
    true
)

/**
 * Find the best enemy in the current world in a specific range.
 */
fun ClientWorld.findEnemy(
    range: ClosedFloatingPointRange<Float>,
    player: Entity = mc.player!!,
    enemyConf: EnemyConfigurable = globalEnemyConfigurable
): Entity? {
    val squaredRange = (range.start * range.start..range.endInclusive * range.endInclusive).toDouble()

    val (bestTarget, _) = getEntitiesInCuboid(player.eyePos, squaredRange.endInclusive)
        .filter { it.shouldBeAttacked(enemyConf) }
        .map { Pair(it, it.squaredBoxedDistanceTo(player)) }
        .filter { (_, distance) -> distance in squaredRange }
        .minByOrNull { (_, distance) -> distance } ?: return null

    return bestTarget
}

/**
 * Find the best enemy in the current world in a specific range.
 */
fun ClientWorld.countEnemies(
    range: ClosedFloatingPointRange<Float>,
    player: Entity = mc.player!!,
    enemyConf: EnemyConfigurable = globalEnemyConfigurable
): Int {
    val squaredRange = (range.start * range.start..range.endInclusive * range.endInclusive).toDouble()

    val enemiesCount =
        getEntitiesInCuboid(player.eyePos, squaredRange.endInclusive)
            .filter { it.shouldBeAttacked(enemyConf) }
            .map { Pair(it, it.squaredBoxedDistanceTo(player)) }
            .filter { (_, distance) -> distance in squaredRange }
            .count()

    return enemiesCount
}

fun ClientWorld.getEntitiesInCuboid(
    midPos: Vec3d,
    range: Double,
    predicate: (Entity) -> Boolean = { true }
): MutableList<Entity> {
    return getOtherEntities(null, Box(midPos.subtract(range, range, range), midPos.add(range, range, range)), predicate)
}

inline fun ClientWorld.getEntitiesBoxInRange(
    midPos: Vec3d,
    range: Double,
    crossinline predicate: (Entity) -> Boolean = { true }
): MutableList<Entity> {
    val rangeSquared = range * range

    return getEntitiesInCuboid(midPos, range) { predicate(it) && it.squaredBoxedDistanceTo(midPos) <= rangeSquared }
}

fun Entity.attack(swing: Boolean) {
    val player = mc.player ?: return
    val network = mc.networkHandler ?: return

    EventManager.callEvent(AttackEvent(this))

    // Swing before attacking (on 1.8)
    if (swing && isOldCombat) {
        player.swingHand(Hand.MAIN_HAND)
    }

    network.sendPacket(PlayerInteractEntityC2SPacket.attack(this, player.isSneaking))

    // Swing after attacking (on 1.9+)
    if (swing && !isOldCombat) {
        player.swingHand(Hand.MAIN_HAND)
    }
}
