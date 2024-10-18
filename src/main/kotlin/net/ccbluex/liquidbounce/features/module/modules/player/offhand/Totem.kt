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
package net.ccbluex.liquidbounce.features.module.modules.player.offhand

import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.features.module.modules.player.nofall.ModuleNoFall
import net.ccbluex.liquidbounce.utils.block.isFallDamageBlocking
import net.ccbluex.liquidbounce.utils.entity.*
import net.ccbluex.liquidbounce.utils.inventory.ARMOR_SLOTS
import net.ccbluex.liquidbounce.utils.inventory.ClickInventoryAction
import net.ccbluex.liquidbounce.utils.inventory.InventoryManager
import net.minecraft.entity.EntityPose

class Totem : ToggleableConfigurable(ModuleOffhand, "Totem", true) {

    /**
     * The offhand might have a lower switch delay than other items.
     */
    val switchDelay by int("SwitchDelay", 0, 0..500, "ms")

    /**
     * Switch to a totem on low health and back to the original item when the health goes up again.
     */
    object Health : ToggleableConfigurable(ModuleOffhand, "Health", true) {

        /**
         * At which health we switch to a totem.
         */
        private val healthThreshold by int("HealthThreshold", 14, 0..20)

        /**
         * For crystal pvp, allows to have longer a useful item in your offhand if you're not in danger of
         * the main damage source.
         */
        private object Safety : ToggleableConfigurable(ModuleOffhand, "Safety", true) {
            // TODO option for 2x2 and 2x1

            /**
             * At which health we switch to a totem when we're from explosions and stuff meaning in bedrock / obsidian
             * holes.
             */
            val safeHealth by int("SafeHealthThreshold", 10, 0..20)

        }

        init {
            tree(Safety)
        }

        /**
         * Predicts explosions. See [getExplosionDamageFromEntity].
         */
        private val explosionDamage by boolean("PredictExplosionsDamage", true)

        private object FallDamage : ToggleableConfigurable(this, "PredictFallDamage", true) {
            val ignoreElytra by boolean("IgnoreElytra", false)

            fun getFallDamage(): Float {
                if (!ModuleNoFall.enabled || FallDamage.enabled || player.fallDistance <= 0) {
                    return 0f
                }

                if (ignoreElytra && player.isFallFlying && player.isInPose(EntityPose.FALL_FLYING)) {
                    return 0f
                }

                val collision = FallingPlayer.fromPlayer(player).findCollision(20)?.pos
                if (collision != null && !collision.isFallDamageBlocking()) {
                   return player.getEffectiveDamage(
                        player.damageSources.fall(),
                        player.computeFallDamage(player.fallDistance, 1f).toFloat()
                    )
                }

                return 0f
            }
        }

        private val missingArmor by boolean("MissingArmor", true)

        init {
            tree(FallDamage)
        }

        val switchBack by boolean("SwitchBack", true)

        //val mainHand by boolean("MainHand", false)

        fun healthBellowThreshold(): Boolean {
            if (!enabled) {
                return true
            }

            if (missingArmor && ARMOR_SLOTS.any { it.itemStack.isEmpty }) {
                return true
            }

            var currentHealth = player.health

            if (explosionDamage) {
                var maxDamage = 0f

                world.entities.forEach {
                    val damageFromEntity = player.getExplosionDamageFromEntity(it)

                    // find the maximum damage that could be applied to player
                    maxDamage = maxDamage.coerceAtLeast(damageFromEntity)
                }

                currentHealth -= maxDamage
            }

            currentHealth -= FallDamage.getFallDamage()

            val safetyOperating = Safety.enabled && currentHealth > Safety.safeHealth
            if (safetyOperating && (player.isBurrowed() || player.isInHole())) {
                return false
            }

            return currentHealth <= healthThreshold
        }

    }

    init {
        tree(Health)
    }

    /**
     * Ignores all active inventory requests, switch settings and sends the switch packets directly.
     */
    private val sendDirectly by boolean("SendDirectly", false)

    fun shouldEquip(): Boolean {
        if (!enabled) {
            return false
        }

        if (player.isCreative || player.isSpectator || player.isDead) {
            return false
        }

        return Health.healthBellowThreshold()
    }

    /**
     * @return `true` if the [actions] got performed.
     */
    fun send(actions: List<ClickInventoryAction>): Boolean {
        if (!sendDirectly) {
            return false
        }

        InventoryManager.clickOccurred()
        actions.forEach { it.performAction() }
        return true
    }

}
