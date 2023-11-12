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
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.config.NamedChoice
import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleKillAura.RaycastMode.*
import net.ccbluex.liquidbounce.render.*
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.render.engine.Vec3
import net.ccbluex.liquidbounce.render.utils.rainbow
import net.ccbluex.liquidbounce.utils.aiming.*
import net.ccbluex.liquidbounce.utils.combat.*
import net.ccbluex.liquidbounce.utils.entity.*
import net.ccbluex.liquidbounce.utils.item.InventoryTracker
import net.ccbluex.liquidbounce.utils.item.openInventorySilently
import net.ccbluex.liquidbounce.utils.kotlin.random
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen
import net.minecraft.enchantment.EnchantmentHelper
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityGroup
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.attribute.EntityAttributes
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.AxeItem
import net.minecraft.network.packet.c2s.play.*
import net.minecraft.sound.SoundEvents
import net.minecraft.util.Hand
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Vec3d
import net.minecraft.world.GameMode
import org.apache.commons.lang3.RandomUtils
import org.apache.commons.lang3.tuple.MutablePair
import kotlin.math.sqrt
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

    private val whileUsingItem by boolean("WhileUsingItem", true)

    object WhileBlocking : ToggleableConfigurable(this, "WhileBlocking", true) {
        val blockingTicks by int("BlockingTicks", 0, 0..20)
    }

    private val legitAimingConfigurable = LegitAimpointTracker.LegitAimpointTrackerConfigurable(this)

    init {
        tree(WhileBlocking)
        tree(legitAimingConfigurable)
    }

    private val raycast by enumChoice("Raycast", TRACE_ALL, values())

    private val failRate by int("FailRate", 0, 0..100)

    private object FailSwing : ToggleableConfigurable(this, "FailSwing", false) {
        object UseOwnCPS : ToggleableConfigurable(ModuleKillAura, "UseOwnCPS", true) {
            val cps by intRange("CPS", 5..8, 0..20)
        }

        object LimitRange : ToggleableConfigurable(ModuleKillAura, "LimitRange", false) {
            val asExtraRange by boolean("AsExtraRange", true)
            val range by float("Range", 2f, 0f..10f)
        }

        init {
            tree(UseOwnCPS)
            tree(LimitRange)
        }
    }

    init {
        tree(FailSwing)
    }

    private object NotifyWhenFail : ToggleableConfigurable(this, "NotifyWhenFail", false) {
        val mode = choices("Mode", Box, arrayOf(Box, Sound))

        object Box : Choice("Box") {
            override val parent: ChoiceConfigurable
                get() = mode

            val fadeSeconds by int("FadeSeconds", 4, 1..10)

            val color by color("Color", Color4b(255, 179, 72, 255))
            val colorRainbow by boolean("Rainbow", false)
        }

        object Sound : Choice("Sound") {
            override val parent: ChoiceConfigurable
                get() = mode

            val volume by float("Volume", 50f, 0f..100f)

            object NoPitchRandomization : ToggleableConfigurable(ModuleKillAura, "NoPitchRandomization", false) {
                val pitch by float("Pitch", 0.8f, 0f..2f)
            }

            init {
                tree(NoPitchRandomization)
            }
        }
    }

    init {
        tree(NotifyWhenFail)
    }

    private val ignoreOpenInventory by boolean("IgnoreOpenInventory", true)
    private val simulateInventoryClosing by boolean("SimulateInventoryClosing", true)

    private val cpsTimer = tree(CpsScheduler())

    private val boxFadeSeconds
        get() = 50 * NotifyWhenFail.Box.fadeSeconds

    override fun disable() {
        targetTracker.cleanup()
        failedHits.clear()
    }

    private var failedHits = arrayListOf<MutablePair<Vec3d, Long>>()

    val renderHandler = handler<WorldRenderEvent> { event ->
        val matrixStack = event.matrixStack

        if (failedHits.isEmpty() || (!NotifyWhenFail.enabled || !NotifyWhenFail.Box.isActive)) {
            failedHits.clear()
            return@handler
        }

        failedHits.forEach { it.setRight(it.getRight() + 1) }
        failedHits = failedHits.filter { it.right <= boxFadeSeconds } as ArrayList<MutablePair<Vec3d, Long>>

        val markedBlocks = failedHits

        val base = if (NotifyWhenFail.Box.colorRainbow) rainbow() else NotifyWhenFail.Box.color

        val box = Box(0.0, 0.0, 0.0, 0.05, 0.05, 0.05)

        renderEnvironmentForWorld(matrixStack) {
            for ((pos, opacity) in markedBlocks) {
                val vec3 = Vec3(pos)

                val fade = (255 + (0 - 255) * opacity.toDouble() / boxFadeSeconds.toDouble()).toInt()

                val baseColor = base.alpha(fade)
                val outlineColor = base.alpha(fade)

                withPosition(vec3) {
                    withColor(baseColor) {
                        drawSolidBox(box)
                    }

                    withColor(outlineColor) {
                        drawOutlinedBox(box)
                    }
                }
            }
        }
    }

    val rotationUpdateHandler = handler<PlayerNetworkMovementTickEvent> {
        // Killaura in spectator-mode is pretty useless, trust me.
        if (it.state != EventState.PRE || player.isSpectator) {
            return@handler
        }

        // Make sure killaura-logic is not running while inventory is open
        val isInInventoryScreen =
            InventoryTracker.isInventoryOpenServerSide || mc.currentScreen is GenericContainerScreen

        if (isInInventoryScreen && !ignoreOpenInventory) {
            // Cleanup current target tracker
            targetTracker.cleanup()
            return@handler
        }

        // Update current target tracker to make sure you attack the best enemy
        updateEnemySelection()
    }

    val repeatable = repeatable {
        val isInInventoryScreen =
            InventoryTracker.isInventoryOpenServerSide || mc.currentScreen is GenericContainerScreen

        // Check if there is target to attack
        val target = targetTracker.lockedOnTarget
        // Did you ever send a rotation before?
        val rotation = RotationManager.currentRotation

        if (CombatManager.shouldPauseCombat())
            return@repeatable

        if (rotation != null && target != null && target.boxedDistanceTo(player) <= range && facingEnemy(
                target, rotation, range.toDouble(), wallRange.toDouble()
            )
        ) {
            // Check if between enemy and player is another entity
            val raycastedEntity = raytraceEntity(range.toDouble(), rotation, filter = {
                when (raycast) {
                    TRACE_NONE -> false
                    TRACE_ONLYENEMY -> it.shouldBeAttacked()
                    TRACE_ALL -> true
                }
            }) ?: target

            // Swap enemy if there is a better enemy
            // todo: compare current target to locked target
            if (raycastedEntity.shouldBeAttacked() && raycastedEntity != target) {
                targetTracker.lock(raycastedEntity)
            }

            // Attack enemy according to cps and cooldown
            val clicks = cpsTimer.clicks(condition = {
                (!cooldown || player.getAttackCooldownProgress(0.0f) >= 1.0f) &&
                    (!ModuleCriticals.shouldWaitForCrit() || raycastedEntity.velocity.lengthSquared() > 0.25 * 0.25)
                    && (attackShielding || raycastedEntity !is PlayerEntity || player.mainHandStack.item is AxeItem ||
                    !raycastedEntity.wouldBlockHit(player))
                    && !(isInInventoryScreen && !ignoreOpenInventory && !simulateInventoryClosing)
            }, cps)

            repeat(clicks) {
                if (simulateInventoryClosing && isInInventoryScreen) {
                    network.sendPacket(CloseHandledScreenC2SPacket(0))
                }

                val blocking = player.isBlocking

                if (blocking) {
                    if (!WhileBlocking.enabled) {
                        return@repeat // return if it's not allowed to attack while using blocking with a shield
                    }
                } else if (player.isUsingItem && !whileUsingItem) {
                    return@repeat // return if it's not allowed to attack while the player is using another item that's not a shield
                }

                // Make sure to unblock now
                if (blocking) {
                    network.sendPacket(
                        PlayerActionC2SPacket(
                            PlayerActionC2SPacket.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, Direction.DOWN
                        )
                    )

                    // Wait until the un-blocking delay time is up
                    if (WhileBlocking.blockingTicks > 0) {
                        mc.options.useKey.isPressed = false
                        wait(WhileBlocking.blockingTicks)
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

                    // Notify the user about the failed hit
                    notifyForFailedHit(raycastedEntity, rotation)
                } else {
                    // Attack enemy
                    attackEntity(raycastedEntity)
                }

                // Make sure to block again
                if (blocking) {
                    // Wait until the blocking delay time is up
                    if (WhileBlocking.blockingTicks > 0) {
                        wait(WhileBlocking.blockingTicks)
                    }

                    interaction.sendSequencedPacket(world) { sequence ->
                        PlayerInteractItemC2SPacket(player.activeHand, sequence)
                    }

                    mc.options.useKey.isPressed = true
                }

                if (simulateInventoryClosing && isInInventoryScreen) {
                    openInventorySilently()
                }
            }

            return@repeatable
        }

        if (!FailSwing.enabled) {
            return@repeatable
        }

        val entity = target ?: world.findEnemy(0f..FailSwing.LimitRange.range)

        val reach = FailSwing.LimitRange.range + if (FailSwing.LimitRange.asExtraRange) range else 0f

        val shouldSwing =
            entity != null && !entity.isRemoved && (!FailSwing.LimitRange.enabled || entity.boxedDistanceTo(player) <= reach)

        val chosenCPS = if (FailSwing.UseOwnCPS.enabled) FailSwing.UseOwnCPS.cps else cps
        val supposedRotation = rotation ?: player.rotation

        val clicks = cpsTimer.clicks({
            shouldSwing && raytraceEntity(
                range.toDouble(), supposedRotation
            ) { true } == null
        }, chosenCPS)

        repeat(clicks) {
            if (swing) {
                player.swingHand(Hand.MAIN_HAND)
            } else {
                network.sendPacket(HandSwingC2SPacket(Hand.MAIN_HAND))
            }
        }
    }

    private val legitAimpointTracker = LegitAimpointTracker(legitAimingConfigurable)

    private var lastRotation: VecRotation? = null

    /**
     * Update enemy on target tracker
     */
    private fun updateEnemySelection() {
        val rangeSquared = range * range

        targetTracker.validateLock { it.squaredBoxedDistanceTo(player) <= rangeSquared }

        val eyes = player.eyes

        val scanRange = if (targetTracker.maxDistanceSquared > rangeSquared) {
            ((range + scanExtraRange) * (range + scanExtraRange)).toDouble()
        } else {
            rangeSquared.toDouble()
        }

        for (target in targetTracker.enemies()) {
            if (target.squaredBoxedDistanceTo(player) > scanRange) {
                continue
            }

            val predictedTicks = predict.random()

            val targetPrediction = target.pos.subtract(target.prevPos).multiply(predictedTicks)
            val playerPrediction = player.pos.subtract(player.prevPos).multiply(predictedTicks)

            val box = target.box.offset(targetPrediction)

            val rotationPreference =
                this.lastRotation
                    ?.let { LeastDifferencePreference(it.rotation, basePoint = it.vec) }
                    ?: LeastDifferencePreference.LEAST_DISTANCE_TO_CURRENT_ROTATION

            // find best spot
            val spot = raytraceBox(
                eyes.add(playerPrediction), box, range = sqrt(scanRange), wallsRange = wallRange.toDouble(),
                rotationPreference = rotationPreference
            ) ?: continue

            // lock on target tracker
            targetTracker.lock(target)

            val nextPoint = if (this.legitAimingConfigurable.enabled) {
                val aimpointChange = target.pos.subtract(target.prevPos).subtract(player.pos.subtract(player.prevPos))

                val nextPoint = this.legitAimpointTracker.nextPoint(box, spot.vec, aimpointChange)

                lastRotation = VecRotation(
                    RotationManager.makeRotation(nextPoint.aimSpotWithoutNoise, eyes),
                    nextPoint.aimSpotWithoutNoise
                )

                nextPoint.aimSpot
            } else {
                lastRotation = null

                spot.vec
            }

            // aim at target
            RotationManager.aimAt(
                RotationManager.makeRotation(nextPoint, player.eyes),
                openInventory = ignoreOpenInventory,
                configurable = rotations
            )
            break
        }
    }

    private fun attackEntity(entity: Entity) {
        if (ModuleCriticals.wouldCrit(true) && unsprintOnCrit) {
            network.sendPacket(ClientCommandC2SPacket(player, ClientCommandC2SPacket.Mode.STOP_SPRINTING))
            player.isSprinting = false
        }

        entity.attack(swing)

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

    private fun notifyForFailedHit(entity: Entity, rotation: Rotation) {
        if (!NotifyWhenFail.enabled) {
            return
        }

        when (NotifyWhenFail.mode.activeChoice) {
            NotifyWhenFail.Box -> {
                val centerDistance = entity.box.center.subtract(player.eyes).length()
                val boxSpot = player.eyes.add(rotation.rotationVec.multiply(centerDistance))

                failedHits.add(MutablePair(boxSpot, 0L))
            }

            NotifyWhenFail.Sound -> {
                // Maybe a custom sound would be better
                val pitch =
                    if (NotifyWhenFail.Sound.NoPitchRandomization.enabled) NotifyWhenFail.Sound.NoPitchRandomization.pitch else RandomUtils.nextFloat(
                        0f, 2f
                    )

                world.playSound(
                    player,
                    player.x,
                    player.y,
                    player.z,
                    SoundEvents.UI_BUTTON_CLICK.value(),
                    player.soundCategory,
                    NotifyWhenFail.Sound.volume / 100f,
                    pitch
                )
            }
        }
    }

    enum class RaycastMode(override val choiceName: String) : NamedChoice {
        TRACE_NONE("None"), TRACE_ONLYENEMY("Enemy"), TRACE_ALL("All")
    }

}
