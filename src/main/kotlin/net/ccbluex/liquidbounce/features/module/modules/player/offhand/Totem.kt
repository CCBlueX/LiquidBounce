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
package net.ccbluex.liquidbounce.features.module.modules.player.offhand

import net.ccbluex.liquidbounce.config.types.ToggleableConfigurable
import net.ccbluex.liquidbounce.features.module.modules.player.nofall.ModuleNoFall
import net.ccbluex.liquidbounce.utils.block.*
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.ccbluex.liquidbounce.utils.entity.*
import net.ccbluex.liquidbounce.utils.inventory.ClickInventoryAction
import net.ccbluex.liquidbounce.utils.inventory.InventoryManager
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.ccbluex.liquidbounce.utils.math.toVec3d
import net.minecraft.block.BedBlock
import net.minecraft.block.RespawnAnchorBlock
import net.minecraft.entity.EntityPose
import net.minecraft.util.math.BlockPos

object Totem : ToggleableConfigurable(ModuleOffhand, "Totem", true) {

    /**
     * The totem mode might have a lower switch delay than other items.
     */
    val switchDelay by int("SwitchDelay", 0, 0..500, "ms")

    /**
     * The totem mode might have a higher and separate switch back delay than other items.
     */
    val switchBackDelay by int("SwitchBackDelay", 40, 0..500, "ms")

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
         * Subtracts the calculated maximum possible damage from the current health.
         *
         * This can lead to a more aggressive auto totem, where a totem is put in the offhand if it
         * might not be necessary.
         */
        private val subtractCalculatedDamage by boolean("SubtractCalculatedDamage", false)

        /**
         * Predicts explosions from creepers, crystals, etc. See [getExplosionDamageFromEntity].
         */
        private val explosionDamage by boolean("PredictExplosionDamageEntities", true)

        /**
         * Predicts explosions from beds and respawn anchors.
         */
        private val explosionDamageBlocks by boolean("PredictExplosionDamageBlocks", false).onChanged {
            sphere = BlockPos.ORIGIN.getSortedSphere(10f)
        }

        private object FallDamage : ToggleableConfigurable(this, "PredictFallDamage", true) {

            val ignoreElytra by boolean("IgnoreElytra", false)

            fun getFallDamage(): Float {
                if (ModuleNoFall.running || !FallDamage.enabled || player.fallDistance <= 3f) {
                    return 0f
                }

                if (ignoreElytra && player.isGliding && player.isInPose(EntityPose.GLIDING)) {
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

        private var sphere: Array<BlockPos>? = null

        fun healthBelowThreshold(): Boolean {
            if (!enabled) {
                return true
            }

            if (missingArmor && Slots.Armor.any { it.itemStack.isEmpty }) {
                return true
            }

            val health = player.health + player.absorptionAmount

            val safetyOperating = Safety.enabled && (player.isBurrowed() || player.isInHole())
            var allowedDamage = health - if (safetyOperating) {
                Safety.safeHealth.toFloat()
            } else {
                healthThreshold.toFloat()
            }

            // the health is below or at the threshold
            if (allowedDamage <= 0f) {
                return true
            }

            // if we don't subtract, we only put a totem in the offhand if the damage would kill the player
            if (!subtractCalculatedDamage) {
                allowedDamage = health
            }

            var calculatedDamage = getDamageFromEntities(allowedDamage)

            // the damage would exceed the threshold
            if (calculatedDamage >= allowedDamage) {
                return true
            }

            calculatedDamage = calculatedDamage.coerceAtLeast(getDamageFromBlocks(allowedDamage))
            if (calculatedDamage >= allowedDamage) {
                return true
            }

            calculatedDamage += FallDamage.getFallDamage()
            return calculatedDamage >= allowedDamage
        }

        private fun getDamageFromEntities(allowedDamage: Float): Float {
            if (!explosionDamage) {
                return 0f
            }

            var maxDamage = 0f

            world.entities.forEach {
                val damageFromEntity = player.getExplosionDamageFromEntity(it)

                // find the maximum damage that could be applied to player
                maxDamage = maxDamage.coerceAtLeast(damageFromEntity)

                // the entity does already enough harm, we can return here
                if (maxDamage >= allowedDamage) {
                    return maxDamage
                }
            }

            return maxDamage
        }

        private fun getDamageFromBlocks(allowedDamage: Float): Float {
            if (!explosionDamageBlocks || sphere == null) {
                return 0f
            }

            val overworld = world.dimension.bedWorks
            val nether = world.dimension.respawnAnchorWorks
            val playerPos = player.blockPos
            var maxDamage = 0f

            sphere!!.forEach {
                val pos = it.add(playerPos)
                val block = pos.getBlock()
                val state = pos.getState()!!

                val noBedExplosion = overworld || block !is BedBlock
                val noAnchorExplosion = nether || block !is RespawnAnchorBlock || !block.isCharged(state)
                if (noBedExplosion && noAnchorExplosion) {
                    return@forEach
                }

                // exclude the block as it gets removed before the explosion happens
                val exclude = if (noBedExplosion) {
                    // the anchor is just the block itself
                    arrayOf(pos)
                } else {
                    // a bed consists of two blocks
                    arrayOf(pos, (block as BedBlock).getPotentialSecondBedBlock(state, pos))
                }

                maxDamage = maxDamage.coerceAtLeast(
                    player.getDamageFromExplosion(pos.toVec3d(), 5f, 10f, 100f, exclude)
                )

                if (maxDamage >= allowedDamage) {
                    return maxDamage
                }
            }

            return maxDamage
        }

    }

    init {
        tree(Health)
    }

    /**
     * Ignores all active inventory requests, switch settings and sends the switch packets directly.
     */
    private val sendDirectly by boolean("SendDirectly", false)

    val switchBack = Chronometer()
    var switchBackStarted = false

    fun shouldEquip(): Boolean {
        if (!enabled) {
            return false
        }

        if (player.isCreative || player.isSpectator || player.isDead) {
            return false
        }

        return Health.healthBelowThreshold()
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
