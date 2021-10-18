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
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.config.NamedChoice
import net.ccbluex.liquidbounce.event.AttackEvent
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleKillAura.RaycastMode.*
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsConfigurable
import net.ccbluex.liquidbounce.utils.aiming.facingEnemy
import net.ccbluex.liquidbounce.utils.aiming.raytraceEntity
import net.ccbluex.liquidbounce.utils.client.MC_1_8
import net.ccbluex.liquidbounce.utils.client.protocolVersion
import net.ccbluex.liquidbounce.utils.combat.CpsScheduler
import net.ccbluex.liquidbounce.utils.combat.TargetTracker
import net.ccbluex.liquidbounce.utils.combat.shouldBeAttacked
import net.ccbluex.liquidbounce.utils.entity.boxedDistanceTo
import net.ccbluex.liquidbounce.utils.entity.eyesPos
import net.ccbluex.liquidbounce.utils.entity.squaredBoxedDistanceTo
import net.ccbluex.liquidbounce.utils.entity.wouldBlockHit
import net.minecraft.client.gui.screen.ingame.InventoryScreen
import net.minecraft.enchantment.EnchantmentHelper
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityGroup
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.attribute.EntityAttributes
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.AxeItem
import net.minecraft.network.packet.c2s.play.*
import net.minecraft.util.Hand
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Vec3d
import net.minecraft.world.GameMode
import kotlin.random.Random

/**
 * KillAura module
 *
 * Automatically attacks enemies.
 */
object ModuleKillAura : Module("KillAura", Category.COMBAT) {

    // Attack speed
    private val cps by intRange("CPS", 5..8, 1..20)
    private val cooldown by boolean("Cooldown", true)

    // Range
    private val range by float("Range", 4.2f, 1f..8f)
    private val scanExtraRange by float("ScanExtraRange", 3.0f, 0.0f..7.0f)

    private val wallRange by float("WallRange", 3f, 0f..8f).listen {
        if (it > range) {
            range
        } else {
            it
        }
    }

    // Target
    private val targetTracker = tree(TargetTracker())

    // Rotation
    private val rotations = tree(RotationsConfigurable())

    // Predict
    private val predict by floatRange("Predict", 0f..0f, 0f..5f)

    // Bypass techniques
    private val swing by boolean("Swing", true)
    private val keepSprint by boolean("KeepSprint", true)
    private val unsprintOnCrit by boolean("UnsprintOnCrit", true)
    private val attackShielding by boolean("AttackShielding", false)

    private val blockingTicks by int("BlockingTicks", 0, 0..20)

    private val raycast by enumChoice("Raycast", TRACE_ALL, values())

    private val failRate by int("FailRate", 0, 0..100)

    private val ignoreOpenInventory by boolean("IgnoreOpenInventory", true)
    private val simulateInventoryClosing by boolean("SimulateInventoryClosing", true)

    private val cpsTimer = CpsScheduler()

    override fun disable() {
        targetTracker.cleanup()
    }

//    val renderHandler = handler<EngineRenderEvent> {
//        val currentTarget = targetTracker.lockedOnTarget ?: return@handler
//
//        val bb = currentTarget.boundingBox
//
//        val renderTask = ColoredPrimitiveRenderTask(6 * 10 * 10 * 2, PrimitiveType.Lines)
//
//        for (direction in Direction.values()) {
//            val maxRaysOnAxis = 10 - 1
//            val stepFactor = 1.0 / maxRaysOnAxis;
//
//            val face = bb.getFace(direction)
//
//            val outerPoints = face.getAllPoints(Vec3d.of(direction.vector))
//
//            var idx = 0
//
//            for (outerPoint in outerPoints) {
//                val vex = Vec3(outerPoint) - Vec3(
//                    0.0, 0.0, 1.0
//                )
//                val color = Color4b(Color.getHSBColor(idx / 4.0f, 1.0f, 1.0f))
//
//                renderTask.index(renderTask.vertex(vex, Color4b.WHITE))
//                renderTask.index(renderTask.vertex(vex + Vec3(direction.vector), color))
//
//                idx++
//            }
//
//            //            for (x in (0..maxRaysOnAxis)) {
//            //                for (y in (0..maxRaysOnAxis)) {
//            //                    renderTask.index(renderTask.vertex(Vec3(plane.getPoint(x * stepFactor, y * stepFactor)) - Vec3(0.0, 0.0, 1.0), Color4b.WHITE))
//            //                }
//            //            }
//        }
//
//        RenderEngine.enqueueForRendering(RenderEngine.CAMERA_VIEW_LAYER, renderTask)
//    }

    val repeatable = repeatable {
        // Killaura in spectator-mode is pretty useless, trust me.
        if (player.isSpectator) {
            return@repeatable
        }

        // Make sure killaura-logic is not running while inventory is open
        val isInInventoryScreen = mc.currentScreen is InventoryScreen

        if (isInInventoryScreen && !ignoreOpenInventory) {
            // Cleanup current target tracker
            targetTracker.cleanup()
            return@repeatable
        }

        // Update current target tracker to make sure you attack the best enemy
        updateEnemySelection()

        // Check if there is target to attack
        val target = targetTracker.lockedOnTarget ?: return@repeatable
        // Did you ever send a rotation before?
        val rotation = RotationManager.serverRotation ?: return@repeatable

        if (target.boxedDistanceTo(player) <= range && facingEnemy(target, range.toDouble(), rotation)) {
            // Check if between enemy and player is another entity
            val raycastedEntity = raytraceEntity(
                range.toDouble(),
                rotation,
                filter = {
                    when (raycast) {
                        TRACE_NONE -> false
                        TRACE_ONLYENEMY -> it.shouldBeAttacked()
                        TRACE_ALL -> true
                    }
                }
            ) ?: target

            // Swap enemy if there is a better enemy
            // todo: compare current target to locked target
            if (raycastedEntity.shouldBeAttacked() && raycastedEntity != target) {
                targetTracker.lock(raycastedEntity)
            }

            // Attack enemy according to cps and cooldown
            val clicks = cpsTimer.clicks(condition = {
                !cooldown || (player.getAttackCooldownProgress(0.0f) >= 1.0f && (!ModuleCriticals.shouldWaitForCrit() || raycastedEntity.velocity.lengthSquared() > 0.25 * 0.25))
                    && (attackShielding || raycastedEntity !is PlayerEntity || player.mainHandStack.item is AxeItem || !raycastedEntity.wouldBlockHit(player))
            }, cps)

            repeat(clicks) {
                if (simulateInventoryClosing && isInInventoryScreen) {
                    network.sendPacket(CloseHandledScreenC2SPacket(0))
                }

                val blocking = player.isBlocking

                // Make sure to unblock now
                if (blocking) {
                    network.sendPacket(
                        PlayerActionC2SPacket(
                            PlayerActionC2SPacket.Action.RELEASE_USE_ITEM,
                            BlockPos.ORIGIN,
                            Direction.DOWN
                        )
                    )

                    // Wait until the un-blocking delay time is up
                    if (blockingTicks > 0) {
                        wait(blockingTicks)
                    }
                }

                // Fail rate
                if (failRate > 0 && failRate > Random.nextInt(100)) {
                    // Fail rate should always make sure to swing the hand, so the server-side knows you missed the enemy.
                    if (swing) {
                        player.swingHand(Hand.MAIN_HAND)
                    } else {
                        network.sendPacket(HandSwingC2SPacket(Hand.MAIN_HAND))
                    }

                    // todo: might notify client-user about fail hit
                } else {
                    // Attack enemy
                    attackEntity(raycastedEntity)
                }

                // Make sure to block again
                if (blocking) {
                    // Wait until the blocking delay time is up
                    if (blockingTicks > 0) {
                        wait(blockingTicks)
                    }

                    network.sendPacket(PlayerInteractItemC2SPacket(player.activeHand))
                }

                // Make sure to reopen inventory
                if (simulateInventoryClosing && isInInventoryScreen) {
                    network.sendPacket(ClientCommandC2SPacket(player, ClientCommandC2SPacket.Mode.OPEN_INVENTORY))
                }
            }
        }
    }

    /**
     * Update enemy on target tracker
     */
    private fun updateEnemySelection() {
        val rangeSquared = range * range

        targetTracker.validateLock { it.squaredBoxedDistanceTo(player) <= rangeSquared }

        val eyes = player.eyesPos

        val scanRange = if (targetTracker.maxDistanceSquared > rangeSquared) {
            ((range + scanExtraRange) * (range + scanExtraRange)).toDouble()
        } else {
            rangeSquared.toDouble()
        }

        for (target in targetTracker.enemies()) {
            if (target.squaredBoxedDistanceTo(player) > scanRange) {
                continue
            }

            val predictedTicks = predict.start + (predict.endInclusive - predict.start) * Math.random()

            val targetPrediction = Vec3d(
                target.x - target.prevX,
                target.y - target.prevY,
                target.z - target.prevZ
            ).multiply(predictedTicks)

            val playerPrediction = Vec3d(
                player.x - player.prevX,
                player.y - player.prevY,
                player.z - player.prevZ
            ).multiply(predictedTicks)

            val box = target.boundingBox.offset(targetPrediction)

            // find best spot (and skip if no spot was found)
            val (rotation, _) = RotationManager.raytraceBox(
                eyes.add(playerPrediction),
                box,
                range = scanRange,
                wallsRange = wallRange.toDouble()
            ) ?: continue

            // lock on target tracker
            targetTracker.lock(target)

            // aim on target
            RotationManager.aimAt(rotation, configurable = rotations)
            break
        }
    }

    private fun attackEntity(entity: Entity) {
        if (ModuleCriticals.wouldCrit(true) && unsprintOnCrit) {
            network.sendPacket(ClientCommandC2SPacket(player, ClientCommandC2SPacket.Mode.STOP_SPRINTING))
            player.isSprinting = false
        }

        EventManager.callEvent(AttackEvent(entity))

        // Swing before attacking (on 1.8)
        if (swing && protocolVersion == MC_1_8) {
            player.swingHand(Hand.MAIN_HAND)
        }

        network.sendPacket(PlayerInteractEntityC2SPacket.attack(entity, player.isSneaking))

        // Swing after attacking (on 1.9+)
        if (swing && protocolVersion != MC_1_8) {
            player.swingHand(Hand.MAIN_HAND)
        }

        if (keepSprint) {
            var genericAttackDamage = player.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE).toFloat()
            var magicAttackDamage = if (entity is LivingEntity) {
                EnchantmentHelper.getAttackDamage(player.mainHandStack, entity.group)
            } else {
                EnchantmentHelper.getAttackDamage(player.mainHandStack, EntityGroup.DEFAULT)
            }

            val cooldownProgress = player.getAttackCooldownProgress(0.5f)
            genericAttackDamage *= 0.2f + cooldownProgress * cooldownProgress * 0.8f
            magicAttackDamage *= cooldownProgress

            if (genericAttackDamage > 0.0f && magicAttackDamage > 0.0f) {
                player.addEnchantedHitParticles(entity)
            }
        } else {
            if (interaction.currentGameMode != GameMode.SPECTATOR) {
                player.attack(entity)
            }
        }

        // reset cooldown
        player.resetLastAttackedTicks()
    }

    enum class RaycastMode(override val choiceName: String) : NamedChoice {
        TRACE_NONE("None"),
        TRACE_ONLYENEMY("Enemy"), TRACE_ALL("All")
    }

}
