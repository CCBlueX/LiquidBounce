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
package net.ccbluex.liquidbounce.features.module.modules.combat.killaura

import net.ccbluex.liquidbounce.config.NamedChoice
import net.ccbluex.liquidbounce.event.Sequence
import net.ccbluex.liquidbounce.event.events.SimulatedTickEvent
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleCriticals
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura.RaycastMode.*
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.features.*
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.features.FailSwing.dealWithFakeSwing
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.features.NotifyWhenFail.failedHits
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.features.NotifyWhenFail.notifyForFailedHit
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.features.NotifyWhenFail.renderFailedHits
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.utils.aiming.*
import net.ccbluex.liquidbounce.utils.combat.*
import net.ccbluex.liquidbounce.utils.entity.boxedDistanceTo
import net.ccbluex.liquidbounce.utils.entity.squaredBoxedDistanceTo
import net.ccbluex.liquidbounce.utils.entity.wouldBlockHit
import net.ccbluex.liquidbounce.utils.item.InventoryTracker
import net.ccbluex.liquidbounce.utils.item.openInventorySilently
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.render.WorldTargetRenderer
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.AxeItem
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full
import net.minecraft.util.Hand
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * KillAura module
 *
 * Automatically attacks enemies.
 */
object ModuleKillAura : Module("KillAura", Category.COMBAT) {

    // Attack speed
    val clickScheduler = tree(ClickScheduler(this, true))

    // Range
    internal val range by float("Range", 4.2f, 1f..8f)
    private val scanExtraRange by float("ScanExtraRange", 3.0f, 0.0f..7.0f)

    internal val wallRange by float("WallRange", 3f, 0f..8f).listen {
        if (it > range) {
            range
        } else {
            it
        }
    }

    // Target
    val targetTracker = tree(TargetTracker())

    // Rotation
    private val rotations = tree(RotationsConfigurable(40f..60f))
    private val aimMode by enumChoice("AimMode", AimMode.NORMAL, AimMode.values())

    // Target rendering
    private val targetRenderer = tree(WorldTargetRenderer(this))

    // Predict
    private val pointTracker = tree(PointTracker())

    init {
        tree(FightBot)
    }

    // Bypass techniques
    internal val swing by boolean("Swing", true)
    private val keepSprint by boolean("KeepSprint", true)
    private val attackShielding by boolean("AttackShielding", false)
    private val whileUsingItem by boolean("WhileUsingItem", true)

    init {
        tree(AutoBlock)
        tree(TickBase)
    }

    internal val raycast by enumChoice("Raycast", TRACE_ALL, values())

    private val failRate by int("FailRate", 0, 0..100)

    init {
        tree(FailSwing)
        tree(NotifyWhenFail)
    }

    internal val ignoreOpenInventory by boolean("IgnoreOpenInventory", true)
    internal val simulateInventoryClosing by boolean("SimulateInventoryClosing", true)

    override fun disable() {
        targetTracker.cleanup()
        failedHits.clear()
        AutoBlock.stopBlocking()
    }


    private var renderTarget: Entity? = null

    val renderHandler = handler<WorldRenderEvent> { event ->
        val matrixStack = event.matrixStack

        renderTarget(matrixStack, event.partialTicks)
        renderFailedHits(matrixStack)
    }

    private fun renderTarget(matrixStack: MatrixStack, partialTicks: Float) {
        val target = renderTarget ?: return
        renderEnvironmentForWorld(matrixStack) {
            targetRenderer.render(this, target, partialTicks)
        }
    }

    val rotationUpdateHandler = handler<SimulatedTickEvent> {
        // Make sure killaura-logic is not running while inventory is open
        val isInInventoryScreen =
            InventoryTracker.isInventoryOpenServerSide || mc.currentScreen is GenericContainerScreen

        if ((isInInventoryScreen && !ignoreOpenInventory) || player.isSpectator || player.isDead) {
            // Cleanup current target tracker
            targetTracker.cleanup()
            return@handler
        }

        // Update current target tracker to make sure you attack the best enemy
        updateEnemySelection()
    }

    val repeatable = repeatable {
        if (player.isDead || player.isSpectator) {
            return@repeatable
        }

        // Check if there is target to attack
        val target = targetTracker.lockedOnTarget

        if (CombatManager.shouldPauseCombat()) {
            AutoBlock.stopBlocking()
            return@repeatable
        }

        if (target == null || target !is LivingEntity) {
            AutoBlock.stopBlocking()

            // Deal with fake swing when there is no target
            if (FailSwing.enabled) {
                waitTicks(AutoBlock.tickOff)
                dealWithFakeSwing(null)
            }
            return@repeatable
        }

        // Check if our target is in range, otherwise deal with auto block
        if (target.boxedDistanceTo(player) > range) {
            if (AutoBlock.onScanRange) {
                AutoBlock.startBlocking()
            } else {
                AutoBlock.stopBlocking()

                // Deal with fake swing
                if (FailSwing.enabled) {
                    waitTicks(AutoBlock.tickOff)
                    dealWithFakeSwing(target)
                }
            }

            return@repeatable
        }

        // Determine if we should attack the target or someone else
        val rotation = if (aimMode == AimMode.ON_TICK) {
            val rangeSquared = range * range
            val scanRange = if (targetTracker.maxDistanceSquared > rangeSquared) {
                ((range + scanExtraRange) * (range + scanExtraRange)).toDouble()
            } else {
                rangeSquared.toDouble()
            }

            getSpot(target, scanRange)?.rotation ?: RotationManager.serverRotation
        } else {
            RotationManager.serverRotation
        }
        val chosenEntity: Entity

        if (raycast != TRACE_NONE) {
            // Check if between enemy and player is another entity
            chosenEntity = raytraceEntity(range.toDouble(), rotation, filter = {
                when (raycast) {
                    TRACE_ONLYENEMY -> it.shouldBeAttacked()
                    TRACE_ALL -> true
                    else -> false
                }
            }) ?: target

            // Swap enemy if there is a better enemy (closer to the player crosshair)
            if (chosenEntity.shouldBeAttacked() && chosenEntity != target) {
                targetTracker.lock(chosenEntity)
            }
        } else {
            chosenEntity = target
        }

        mightAttack(chosenEntity, rotation)
    }

    private suspend fun Sequence<*>.mightAttack(chosenEntity: Entity, rotation: Rotation) {
        // Are we actually facing the [chosenEntity]
        if (aimMode != AimMode.ON_TICK && !facingEnemy(toEntity = chosenEntity, rotation = rotation,
                range = range.toDouble(),
                wallsRange = wallRange.toDouble())) {
            dealWithFakeSwing(chosenEntity)
            return
        }

        // Attack enemy according to the attack scheduler
        if (clickScheduler.goingToClick && checkIfReadyToAttack(chosenEntity)) {
            AutoBlock.makeSeemBlock()

            prepareAttackEnvironment(rotation) {
                clickScheduler.clicks {
                    // On each click, we check if we are still ready to attack
                    if (!checkIfReadyToAttack(chosenEntity)) {
                        return@clicks false
                    }

                    // Fail rate
                    if (failRate > 0 && failRate > Random.nextInt(100)) {
                        // Fail rate should always make sure to swing the hand, so the server-side knows you missed the enemy.
                        if (swing) {
                            player.swingHand(Hand.MAIN_HAND)
                        } else {
                            network.sendPacket(HandSwingC2SPacket(Hand.MAIN_HAND))
                        }

                        // Notify the user about the failed hit
                        notifyForFailedHit(chosenEntity, RotationManager.serverRotation)
                    } else {
                        // Attack enemy
                        chosenEntity.attack(swing, keepSprint)
                    }

                    true
                }
            }
        } else {
            if (clickScheduler.isClickOnNextTick(AutoBlock.tickOff)) {
                AutoBlock.stopBlocking(pauses = true)
            } else {
                AutoBlock.startBlocking()
            }
        }
    }

    /**
     * Update enemy on target tracker
     */
    private fun updateEnemySelection() {
        val rangeSquared = range * range

        targetTracker.validateLock { it.shouldBeAttacked() && it.squaredBoxedDistanceTo(player) <= rangeSquared }

        val scanRange = if (targetTracker.maxDistanceSquared > rangeSquared) {
            ((range + scanExtraRange) * (range + scanExtraRange)).toDouble()
        } else {
            rangeSquared.toDouble()
        }

        renderTarget = null

        for (target in targetTracker.enemies()) {
            if (target.squaredBoxedDistanceTo(player) > scanRange) {
                continue
            }

            val spot = getSpot(target, scanRange) ?: continue

            renderTarget = target

            // lock on target tracker
            targetTracker.lock(target)

            // aim at target
            val ticks = rotations.howLongItTakes(spot.rotation)
            if (aimMode == AimMode.FLICK && !clickScheduler.isClickOnNextTick(ticks.coerceAtLeast(1))) {
                break
            }

            // On Tick can only be used if the distance is not too far compared to the turn speed
            if (aimMode == AimMode.ON_TICK && ticks <= 1) {
                break
            }

            RotationManager.aimAt(
                rotations.toAimPlan(spot.rotation, !ignoreOpenInventory),
                priority = Priority.IMPORTANT_FOR_USAGE_2,
                provider = this@ModuleKillAura
            )
            break
        }

        // Choose enemy for fight bot
        if (FightBot.enabled && targetTracker.lockedOnTarget == null) {
            // Because target tracker enemies are sorted by priority, we can just take the first one
            val targetByPriority = targetTracker.enemies().firstOrNull() ?: return

            val rotationToEnemy = FightBot.makeClientSideRotationNeeded(targetByPriority) ?: return
            // lock on target tracker
            RotationManager.aimAt(
                rotations.toAimPlan(rotationToEnemy, !ignoreOpenInventory, silent = false),
                priority = Priority.IMPORTANT_FOR_USAGE_2,
                provider = this@ModuleKillAura
            )
            targetTracker.lock(targetByPriority)
        }
    }

    private fun getSpot(entity: LivingEntity, scanRange: Double): VecRotation? {
        val (eyes, nextPoint, box, cutOffBox) = pointTracker.gatherPoint(
            entity,
            clickScheduler.goingToClick || clickScheduler.isClickOnNextTick(1)
        )
        val rotationPreference = LeastDifferencePreference(RotationManager.serverRotation, nextPoint)

        // find best spot
        val spot = raytraceBox(
            eyes, cutOffBox, range = sqrt(scanRange),
            wallsRange = wallRange.toDouble(), rotationPreference = rotationPreference
        ) ?: raytraceBox(
            eyes, box, range = sqrt(scanRange),
            wallsRange = wallRange.toDouble(), rotationPreference = rotationPreference
        ) ?: return null

        return spot
    }

    private fun checkIfReadyToAttack(choosenEntity: Entity): Boolean {
        val critical = !ModuleCriticals.shouldWaitForCrit() || choosenEntity.velocity.lengthSquared() > 0.25 * 0.25
        val shielding = attackShielding || choosenEntity !is PlayerEntity || player.mainHandStack.item is AxeItem ||
            !choosenEntity.wouldBlockHit(player)
        val isInInventoryScreen =
            InventoryTracker.isInventoryOpenServerSide || mc.currentScreen is GenericContainerScreen

        return critical && shielding &&
            !(isInInventoryScreen && !ignoreOpenInventory && !simulateInventoryClosing)
    }

    /**
     * Prepare the environment for attacking an entity
     *
     * This means, we make sure we are not blocking, we are not using another item,
     * and we are not in an inventory screen depending on the configuration.
     */
    internal suspend fun Sequence<*>.prepareAttackEnvironment(rotation: Rotation? = null, attack: () -> Unit) {
        val isInInventoryScreen =
            InventoryTracker.isInventoryOpenServerSide || mc.currentScreen is GenericContainerScreen

        if (simulateInventoryClosing && isInInventoryScreen) {
            network.sendPacket(CloseHandledScreenC2SPacket(0))
        }

        val wasBlocking = player.isBlocking

        if (wasBlocking) {
            if (!AutoBlock.enabled) {
                return
            }

            AutoBlock.stopBlocking(pauses = true)

            // Wait for the tick off time to be over, if it's not 0
            if (AutoBlock.tickOff > 0) {
                waitTicks(AutoBlock.tickOff)
            }
        } else if (player.isUsingItem && !whileUsingItem) {
            return // return if it's not allowed to attack while the player is using another item that's not a shield
        }

        if (aimMode == AimMode.ON_TICK && rotation != null) {
            network.sendPacket(Full(player.x, player.y, player.z, rotation.yaw, rotation.pitch, player.isOnGround))
        }

        attack()

        if (aimMode == AimMode.ON_TICK && rotation != null) {
            network.sendPacket(Full(player.x, player.y, player.z, player.yaw, player.pitch, player.isOnGround))
        }

        if (simulateInventoryClosing && isInInventoryScreen) {
            openInventorySilently()
        }

        // If the player was blocking before, we start blocking again after the attack if the tick on is 0
        if (wasBlocking && AutoBlock.tickOn == 0) {
            AutoBlock.startBlocking()
        }
    }

    enum class AimMode(override val choiceName: String) : NamedChoice {
        NORMAL("Normal"), FLICK("Flick"), ON_TICK("OnTick")
    }

    enum class RaycastMode(override val choiceName: String) : NamedChoice {
        TRACE_NONE("None"), TRACE_ONLYENEMY("Enemy"), TRACE_ALL("All")
    }

}
