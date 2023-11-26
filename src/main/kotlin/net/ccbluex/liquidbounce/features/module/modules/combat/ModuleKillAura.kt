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
import net.ccbluex.liquidbounce.event.DummyEvent
import net.ccbluex.liquidbounce.event.Sequence
import net.ccbluex.liquidbounce.event.events.SimulatedTickEvent
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleKillAura.FailSwing.dealWithFakeSwing
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
import net.minecraft.item.ItemStack
import net.minecraft.network.packet.c2s.play.*
import net.minecraft.sound.SoundEvents
import net.minecraft.util.Hand
import net.minecraft.util.UseAction
import net.minecraft.util.hit.HitResult
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

    object Cooldown : ToggleableConfigurable(this, "Cooldown", true) {

        private val rangeCooldown by floatRange("CooldownRange", 0.9f..1f, 0f..1f)

        var nextCooldown = rangeCooldown.random()
            private set

        val readyToAttack: Boolean
            get() = !this.enabled || player.getAttackCooldownProgress(0.0f) >= cooldown.nextCooldown

        fun newCooldown() {
            nextCooldown = rangeCooldown.random()
        }

    }

    val cooldown = tree(Cooldown)

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
    private val rotations = tree(RotationsConfigurable(40f..60f))

    // Predict
    private val pointTracker = tree(PointTracker())

    // Bypass techniques
    private val swing by boolean("Swing", true)
    private val keepSprint by boolean("KeepSprint", true)
    private val unsprintOnCrit by boolean("UnsprintOnCrit", true)
    private val attackShielding by boolean("AttackShielding", false)

    private val whileUsingItem by boolean("WhileUsingItem", true)
    private val whileBlocking by boolean("WhileBlocking", true)

    object AutoBlock : ToggleableConfigurable(this, "AutoBlocking", false) {

        val tickOff by int("TickOff", 0, 0..5)
        val tickOn by int("TickOn", 0, 0..5)
        val onScanRange by boolean("OnScanRange", true)
        val interactWith by boolean("InteractWith", true)
        val onlyWhenInDanger by boolean("OnlyWhenInDanger", true)

        var blockingStateEnforced = false

        fun startBlocking() {
            if (!enabled || player.isBlocking) {
                return
            }

            if (onlyWhenInDanger && !isInDanger()) {
                return
            }

            if (canBlock(player.mainHandStack)) {
                if (interactWith) {
                    interactWithFront()
                }

                interaction.interactItem(player, Hand.MAIN_HAND)
                blockingStateEnforced = true
            } else if (canBlock(player.offHandStack)) {
                if (interactWith) {
                    interactWithFront()
                }

                interaction.interactItem(player, Hand.OFF_HAND)
                blockingStateEnforced = true
            }
        }

        fun stopBlocking() {
            // We do not want the player to stop eating or else. Only when he blocks.
            if (player.isBlocking && !mc.options.useKey.isPressed) {
                interaction.stopUsingItem(player)
            }
            blockingStateEnforced = false
        }

        private fun interactWithFront() {
            // Raycast using the current rotation and find a block or entity that should be interacted with

            val rotationToTheServer = RotationManager.currentRotation ?: return

            val entity = raytraceEntity(range.toDouble(), rotationToTheServer, filter = {
                when (raycast) {
                    TRACE_NONE -> false
                    TRACE_ONLYENEMY -> it.shouldBeAttacked()
                    TRACE_ALL -> true
                }
            })

            if (entity != null) {
                // Interact with entity
                // Check if it makes use to interactAt the entity
                // interaction.interactEntityAtLocation()
                interaction.interactEntity(player, entity, Hand.MAIN_HAND)
                return
            }

            val hitResult = raycast(
                range.toDouble(), rotationToTheServer, includeFluids = false
            ) ?: return

            if (hitResult.type != HitResult.Type.BLOCK) {
                return
            }

            // Interact with block
            interaction.interactBlock(player, Hand.MAIN_HAND, hitResult)
        }

        private fun canBlock(itemStack: ItemStack)
            = itemStack.item?.getUseAction(itemStack) == UseAction.BLOCK

        private fun isInDanger(): Boolean {
            val possibleTargets = targetTracker.enemies()

            for (target in possibleTargets) {
                if (target.isRemoved) {
                    continue
                }

                // Check if the target is facing the player
                if (facingEnemy(fromEntity = target, toEntity = player, rotation = target.rotation,
                        range = range.toDouble(),
                        wallsRange = wallRange.toDouble())) {
                    return true
                }
            }

            return false
        }

    }

    init {
        tree(AutoBlock)
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

        suspend fun Sequence<DummyEvent>.dealWithFakeSwing(target: Entity?) {
            if (!enabled) {
                return
            }

            val entity = target ?: world.findEnemy(0f..LimitRange.range)
            val reach = LimitRange.range + if (LimitRange.asExtraRange) range else 0f

            val shouldSwing = entity != null && !entity.isRemoved
                && (!enabled || entity.boxedDistanceTo(player) <= reach)

            val chosenCPS = if (enabled) UseOwnCPS.cps else cps
            val clicks = cpsTimer.clicks({ shouldSwing }, chosenCPS)

            prepareAttackEnvironment {
                repeat(clicks) {
                    if (swing) {
                        player.swingHand(Hand.MAIN_HAND)
                    } else {
                        network.sendPacket(HandSwingC2SPacket(Hand.MAIN_HAND))
                    }
                }
            }
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
        AutoBlock.stopBlocking()
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

    val rotationUpdateHandler = handler<SimulatedTickEvent> {
        // Make sure killaura-logic is not running while inventory is open
        val isInInventoryScreen =
            InventoryTracker.isInventoryOpenServerSide || mc.currentScreen is GenericContainerScreen

        if ((isInInventoryScreen && !ignoreOpenInventory) || player.isSpectator) {
            // Cleanup current target tracker
            targetTracker.cleanup()
            return@handler
        }

        // Update current target tracker to make sure you attack the best enemy
        updateEnemySelection()
    }

    val repeatable = repeatable {
        // Check if there is target to attack
        val target = targetTracker.lockedOnTarget

        if (target == null || (!AutoBlock.onScanRange && target.boxedDistanceTo(player) > range)) {
            AutoBlock.stopBlocking()

            // Deal with fake swing
            if (FailSwing.enabled) {
                wait(AutoBlock.tickOff)
                dealWithFakeSwing(target)
            }
        }

        if (CombatManager.shouldPauseCombat()) {
            AutoBlock.stopBlocking()
            return@repeatable
        }

        if (target != null) {
            // Check if our target is in range, otherwise deal with auto block
            if (target.boxedDistanceTo(player) > range) {
                AutoBlock.startBlocking()
                return@repeatable
            }

            // Determine if we should attack the target or someone else
            val rotation = RotationManager.serverRotation

            val choosenEntity: Entity
            if (raycast != TRACE_NONE) {
                // Check if between enemy and player is another entity
                choosenEntity = raytraceEntity(range.toDouble(), rotation, filter = {
                    when (raycast) {
                        TRACE_ONLYENEMY -> it.shouldBeAttacked()
                        TRACE_ALL -> true
                        else -> false
                    }
                }) ?: target

                // Swap enemy if there is a better enemy (closer to the player crosshair)
                if (choosenEntity.shouldBeAttacked() && choosenEntity != target) {
                    targetTracker.lock(choosenEntity)
                }
            } else {
                choosenEntity = target
            }

            // Are we actually facing the choosen entity?
            if (!facingEnemy(toEntity = choosenEntity, rotation = rotation, range = range.toDouble(),
                    wallsRange = wallRange.toDouble())) {
                dealWithFakeSwing(choosenEntity)
                return@repeatable
            }

            // Attack enemy according to cps and cooldown
            val clicks = cpsTimer.clicks({ checkIfReadyToAttack(choosenEntity) }, cps)

            if (clicks == 0) {
                if (cpsTimer.isClickOnNextTick(AutoBlock.tickOff)) {
                    AutoBlock.stopBlocking()
                    return@repeatable
                }

                AutoBlock.startBlocking()
                return@repeatable
            }

            prepareAttackEnvironment {
                repeat(clicks) {
                    // Fail rate
                    if (failRate > 0 && failRate > Random.nextInt(100)) {
                        // Fail rate should always make sure to swing the hand, so the server-side knows you missed the enemy.
                        if (swing) {
                            player.swingHand(Hand.MAIN_HAND)
                        } else {
                            network.sendPacket(HandSwingC2SPacket(Hand.MAIN_HAND))
                        }

                        // Notify the user about the failed hit
                        notifyForFailedHit(choosenEntity, rotation)
                    } else {
                        // Attack enemy
                        attackEntity(choosenEntity)
                    }
                }
            }

            return@repeatable
        }
    }

    /**
     * Update enemy on target tracker
     */
    private fun updateEnemySelection() {
        val rangeSquared = range * range

        targetTracker.validateLock {
            it.shouldBeAttacked() && it.squaredBoxedDistanceTo(player) <= rangeSquared
        }

        val scanRange = if (targetTracker.maxDistanceSquared > rangeSquared) {
            ((range + scanExtraRange) * (range + scanExtraRange)).toDouble()
        } else {
            rangeSquared.toDouble()
        }

        for (target in targetTracker.enemies()) {
            if (target.squaredBoxedDistanceTo(player) > scanRange) {
                continue
            }

            val (eyes, nextPoint, box, cutOffBox) = pointTracker.gatherPoint(target, cpsTimer.isClickOnNextTick(1))
            val rotationPreference = LeastDifferencePreference(RotationManager.currentRotation
                ?: player.rotation, nextPoint)

            // find best spot
            val spot = raytraceBox(
                eyes,
                cutOffBox,
                range = sqrt(scanRange),
                wallsRange = wallRange.toDouble(),
                rotationPreference = rotationPreference
            ) ?: raytraceBox(
                eyes,
                box,
                range = sqrt(scanRange),
                wallsRange = wallRange.toDouble(),
                rotationPreference = rotationPreference
            ) ?: continue

            // lock on target tracker
            targetTracker.lock(target)

            // aim at target
            RotationManager.aimAt(rotations.toAimPlan(spot.rotation, !ignoreOpenInventory))
            break
        }
    }

    fun checkIfReadyToAttack(choosenEntity: Entity): Boolean {
        val critical = !ModuleCriticals.shouldWaitForCrit() || choosenEntity.velocity.lengthSquared() > 0.25 * 0.25
        val shielding = attackShielding || choosenEntity !is PlayerEntity || player.mainHandStack.item is AxeItem ||
            !choosenEntity.wouldBlockHit(player)
        val isInInventoryScreen = InventoryTracker.isInventoryOpenServerSide
            || mc.currentScreen is GenericContainerScreen

        return cooldown.readyToAttack && critical && shielding && !(isInInventoryScreen && !ignoreOpenInventory
            && !simulateInventoryClosing)
    }

    /**
     * Prepare the environment for attacking an entity
     *
     * This means, we make sure we are not blocking, we are not using another item
     * and we are not in an inventory screen depending on the configuration.
     */
    private suspend fun Sequence<DummyEvent>.prepareAttackEnvironment(attack: () -> Unit) {
        val isInInventoryScreen =
            InventoryTracker.isInventoryOpenServerSide || mc.currentScreen is GenericContainerScreen

        if (simulateInventoryClosing && isInInventoryScreen) {
            network.sendPacket(CloseHandledScreenC2SPacket(0))
        }

        if (player.isBlocking) {
            if (!whileBlocking) {
                return // return if it's not allowed to attack while using blocking with a shield
            }

            network.sendPacket(PlayerActionC2SPacket(PlayerActionC2SPacket.Action.RELEASE_USE_ITEM,
                BlockPos.ORIGIN, Direction.DOWN))
            if (AutoBlock.tickOff > 0) {
                wait(AutoBlock.tickOff)
            }
        } else if (player.isUsingItem && !whileUsingItem) {
            return // return if it's not allowed to attack while the player is using another item that's not a shield
        }

        attack()
        cooldown.newCooldown()

        if (simulateInventoryClosing && isInInventoryScreen) {
            openInventorySilently()
        }

        if (player.isBlocking) {
            if (AutoBlock.tickOn > 0) {
                wait(AutoBlock.tickOn)
            }
            interaction.sendSequencedPacket(world) { sequence ->
                PlayerInteractItemC2SPacket(player.activeHand, sequence)
            }
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

            val isCrit = ModuleCriticals.wouldCrit(true)

            if (isCrit) {
                world.playSound(
                    null,
                    player.x,
                    player.y,
                    player.z,
                    SoundEvents.ENTITY_PLAYER_ATTACK_CRIT,
                    player.soundCategory,
                    1.0f,
                    1.0f
                )
                player.addCritParticles(entity)
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
