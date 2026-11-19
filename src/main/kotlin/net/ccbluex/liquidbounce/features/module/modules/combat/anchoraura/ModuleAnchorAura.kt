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
package net.ccbluex.liquidbounce.features.module.modules.combat.anchoraura

import net.ccbluex.liquidbounce.config.types.group.ValueGroup
import net.ccbluex.liquidbounce.event.events.RotationUpdateEvent
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.render.FULL_BOX
import net.ccbluex.liquidbounce.utils.aiming.NoRotationMode
import net.ccbluex.liquidbounce.utils.aiming.NormalRotationMode
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.aiming.utils.canSeeBox
import net.ccbluex.liquidbounce.utils.aiming.utils.raytraceBox
import net.ccbluex.liquidbounce.utils.block.SwingMode
import net.ccbluex.liquidbounce.utils.block.getSortedSphere
import net.ccbluex.liquidbounce.utils.block.isUnobstructed
import net.ccbluex.liquidbounce.utils.block.placer.BlockPlacer
import net.ccbluex.liquidbounce.utils.block.stateOrEmpty
import net.ccbluex.liquidbounce.utils.client.FloatValueProvider
import net.ccbluex.liquidbounce.utils.client.SilentHotbar
import net.ccbluex.liquidbounce.utils.combat.CombatManager
import net.ccbluex.liquidbounce.utils.combat.TargetTracker
import net.ccbluex.liquidbounce.utils.entity.shouldSwingHand
import net.ccbluex.liquidbounce.utils.inventory.HotbarItemSlot
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.ccbluex.liquidbounce.utils.inventory.findClosestSlot
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.math.center
import net.ccbluex.liquidbounce.utils.math.sq
import net.ccbluex.liquidbounce.utils.raytracing.isFacingBlock
import net.ccbluex.liquidbounce.utils.raytracing.raytraceBlock
import net.ccbluex.liquidbounce.utils.render.TargetRenderer
import net.ccbluex.liquidbounce.utils.world.respawnAnchorWorks
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.RespawnAnchorBlock
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import kotlin.math.max

/**
 * Places, charges once, protects the player's feet, and detonates respawn anchors where they explode instead of
 * setting spawn. Only one anchor and one interaction are tracked at a time; world state confirms every transition.
 */
@Suppress("LargeClass", "TooManyFunctions")
object ModuleAnchorAura : ClientModule(
    "AnchorAura",
    ModuleCategories.COMBAT,
    aliases = listOf("AutoAnchor"),
    disableOnQuit = true,
) {

    private const val MAX_PLACEMENT_TARGET_DISTANCE_SQR = 4.0

    val targetTracker = tree(TargetTracker(
        rangeValue = FloatValueProvider("Range", 5f, 1f..12f)
    ))

    object Damage : ValueGroup("Damage") {
        val minEnemy by float("MinEnemyDamage", 6f, 0f..20f)
        val maxSelf by float("MaxSelfDamage", 6f, 0f..20f)
        val maxFriend by float("MaxFriendDamage", 4f, 0f..20f)
        val antiSuicide by boolean("AntiSuicide", true)
        val lethalOverride by boolean("LethalOverride", true)
        val efficient by boolean("Efficient", true)
        val terrain by boolean("Terrain", true)
        val feetProtection by boolean("FeetProtection", true)
    }

    object Prediction : ValueGroup("Prediction") {
        val selfTicks by int("SelfTicks", 2, 0..10, "ticks")
        val targetTicks by int("TargetTicks", 2, 0..10, "ticks")
    }

    private val searchRadius by float("SearchRadius", 4f, 1f..6f)
    private val searchInterval by int("SearchInterval", 100, 20..1000, "ms")
    private val maxTargets by int("MaxTargets", 3, 1..8)
    private val maxCandidates by int("MaxCandidates", 48, 8..128)
    private val interactionDelay by int("InteractionDelay", 100, 0..1000, "ms")
    private val cycleDelay by int("CycleDelay", 150, 0..2000, "ms")
    private val placeTimeout by int("PlaceTimeout", 2500, 250..10000, "ms")
    private val rotationTimeout by int("RotationTimeout", 1000, 100..5000, "ms")
    private val confirmationTimeout by int("ConfirmationTimeout", 750, 100..5000, "ms")
    private val baseRetryDelay by int("BaseRetryDelay", 250, 0..5000, "ms")
    private val maxRetryDelay by int("MaxRetryDelay", 2000, 0..10000, "ms")
    private val slotResetDelay by int("SlotResetDelay", 2, 0..20, "ticks")
    private val swingMode by enumChoice("Swing", SwingMode.DO_NOT_HIDE, SwingMode.INTERACT_CHOICES)

    private val placer = tree(BlockPlacer(
        "Placing",
        this,
        Priority.IMPORTANT_FOR_USAGE_3,
        {
            val current = active
            when (current?.phase) {
                AnchorAuraPhase.PLACING -> findAnchorSlot().takeIf { isSafe(current, useCache = false) }
                AnchorAuraPhase.PROTECTING -> findProtectionSlot().takeIf {
                    isSafe(current, useCache = false, protectionPos = current.protectionPos)
                }
                else -> null
            }
        },
    ))

    private val rotationMode = modes(this, "InteractionRotation") {
        arrayOf(
            NormalRotationMode(it, this, Priority.IMPORTANT_FOR_USAGE_3, true),
            NoRotationMode(it, this),
        )
    }

    private data class Candidate(
        val pos: BlockPos,
        val target: LivingEntity,
        val action: AnchorAuraAction,
        val charges: Int,
        val score: Float,
    )

    @Suppress("LongParameterList")
    private data class ActiveAnchor(
        val pos: BlockPos,
        var target: LivingEntity,
        var phase: AnchorAuraPhase,
        var observedCharges: Int,
        val startedAt: Long,
        var lastActionAt: Long = 0L,
        var pendingSince: Long = 0L,
        var awaitingSince: Long = 0L,
        var pendingGeneration: Long = 0L,
        var pending: Boolean = false,
        var awaitingConfirmation: Boolean = false,
        var protectionPos: BlockPos? = null,
        var protectionStartedAt: Long = 0L,
        var protectionChecked: Boolean = false,
    )

    private data class RetryState(val failures: Int, val notBefore: Long)
    private data class Visibility(val visible: Boolean)
    private data class ProtectionChoice(
        val pos: BlockPos,
        val damage: AnchorDamageInput,
        val lineDistance: Double,
    )

    private val maxCharge = RespawnAnchorBlock.CHARGE.possibleValues.max()
    private val retries = HashMap<Long, RetryState>()
    private var active: ActiveAnchor? = null
    private var actionGeneration = 0L
    private var nextSearchAt = 0L
    private var cachedSearchRadius = -1f
    private var searchOffsets = emptyArray<BlockPos>()
    private val protectionDirections = arrayOf(Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST)

    init {
        treeAll(Damage, Prediction)
        tree(TargetRenderer(this, targetTracker))
    }

    override fun onEnabled() {
        resetRuntime(clearRetries = true)
    }

    override fun onDisabled() {
        resetRuntime(clearRetries = true)
        placer.disable()
    }

    @Suppress("unused")
    private val worldChangeHandler = handler<WorldChangeEvent> {
        resetRuntime(clearRetries = true)
        placer.disable()
    }

    @Suppress("unused")
    private val tickHandler = handler<RotationUpdateEvent>(priority = -30) {
        AnchorAuraDamage.clearCache()

        if (world.respawnAnchorWorks || player.isDeadOrDying || player.isSpectator ||
            CombatManager.shouldPauseCombat || !placer.canAct()
        ) {
            resetActive()
            targetTracker.reset()
            return@handler
        }

        val now = System.currentTimeMillis()
        val targets = targetTracker.targets()
        val current = active
        if (current != null) {
            val target = targets.firstOrNull { it.id == current.target.id }
            if (target == null) {
                resetActive()
                targetTracker.reset()
                return@handler
            }

            current.target = target
            targetTracker.target = target
            tickActive(current, now)
            return@handler
        }

        targetTracker.reset()
        if (now < nextSearchAt) {
            return@handler
        }

        val candidate = findBestCandidate(targets.take(maxTargets), now)
        if (candidate == null) {
            nextSearchAt = now + searchInterval
            return@handler
        }

        targetTracker.target = candidate.target
        val newActive = ActiveAnchor(
            pos = candidate.pos,
            target = candidate.target,
            phase = AnchorAuraLogic.phaseFor(candidate.action),
            observedCharges = candidate.charges,
            startedAt = now,
        )
        active = newActive
        if (candidate.action == AnchorAuraAction.PLACE) {
            placer.update(setOf(candidate.pos))
        } else {
            placer.clear()
        }
    }

    @Suppress("CognitiveComplexMethod", "LongMethod", "ReturnCount")
    private fun tickActive(current: ActiveAnchor, now: Long) {
        val state = current.pos.stateOrEmpty
        if (state.block !is RespawnAnchorBlock) {
            tickWithoutAnchor(current, now)
            return
        }

        val charges = state.getValue(RespawnAnchorBlock.CHARGE)
        val action = AnchorAuraLogic.actionFor(anchorPresent = true, charges, maxCharge)
        if (action == null) {
            recordFailure(current, now)
            resetActive()
            nextSearchAt = now + searchInterval
            return
        }

        if (current.phase == AnchorAuraPhase.PLACING) {
            placer.clear()
            retries.remove(current.pos.asLong())
            current.phase = AnchorAuraLogic.phaseFor(action)
            current.observedCharges = charges
            current.lastActionAt = now
            current.pending = false
            current.awaitingConfirmation = false
            return
        }

        if (charges != current.observedCharges) {
            current.observedCharges = charges
            if (current.awaitingConfirmation) {
                current.awaitingConfirmation = false
                current.awaitingSince = 0L
                current.lastActionAt = now
                retries.remove(current.pos.asLong())
            }
        }

        if (current.phase == AnchorAuraPhase.PROTECTING && action == AnchorAuraAction.DETONATE) {
            tickProtection(current, now)
            return
        }

        val expectedPhase = AnchorAuraLogic.phaseFor(action)
        if (current.phase != expectedPhase) {
            actionGeneration++
            placer.clear()
            current.phase = expectedPhase
            current.pending = false
            current.pendingGeneration = 0L
            current.awaitingConfirmation = false
            current.awaitingSince = 0L
            current.lastActionAt = now
            current.protectionPos = null
            current.protectionStartedAt = 0L
            current.protectionChecked = false
            if (expectedPhase == AnchorAuraPhase.DETONATING) {
                retries.remove(current.pos.asLong())
            }
        }

        if (action == AnchorAuraAction.DETONATE && Damage.feetProtection) {
            val protectionPos = current.protectionPos
            if (protectionPos != null && !protectionPos.stateOrEmpty.isSolid) {
                current.protectionPos = null
                current.protectionChecked = false
            }

            if (!current.protectionChecked) {
                current.protectionChecked = true
                val protection = findBestProtection(current.pos, current.target)
                if (protection != null) {
                    beginProtection(current, protection.pos, now)
                    return
                }
            }
        }

        if (!isSafe(current, useCache = true)) {
            resetActive()
            nextSearchAt = now + searchInterval
            return
        }

        if (current.awaitingConfirmation) {
            if (now - current.awaitingSince < confirmationTimeout) {
                return
            }

            recordFailure(current, now)
        }

        if (current.pending) {
            if (now - current.pendingSince < rotationTimeout) {
                return
            }

            recordFailure(current, now)
        }

        val notBefore = retries[current.pos.asLong()]?.notBefore ?: 0L
        if (!AnchorAuraLogic.canAttempt(now, current.lastActionAt, notBefore, interactionDelay.toLong())) {
            return
        }

        when (action) {
            AnchorAuraAction.CHARGE -> scheduleInteraction(current, action, now)
            AnchorAuraAction.DETONATE -> scheduleInteraction(current, action, now)
            AnchorAuraAction.PLACE -> {
                actionGeneration++
                resetActive()
            }
        }
    }

    private fun tickWithoutAnchor(current: ActiveAnchor, now: Long) {
        when (current.phase) {
            AnchorAuraPhase.PLACING -> {
                if (findAnchorSlot() == null || !isSafe(current, useCache = true)) {
                    resetActive()
                    nextSearchAt = now + searchInterval
                    return
                }

                if (now - current.startedAt >= placeTimeout) {
                    recordFailure(current, now)
                    resetActive()
                    nextSearchAt = now + searchInterval
                    return
                }

                // BlockPlacer performs click planning, support-path placement, range checks, and click verification.
                placer.update(setOf(current.pos))
            }

            AnchorAuraPhase.DETONATING -> {
                if (current.awaitingConfirmation) {
                    retries.remove(current.pos.asLong())
                    resetActive()
                    nextSearchAt = now + cycleDelay
                } else {
                    resetActive()
                }
            }

            else -> resetActive()
        }
    }

    private fun tickProtection(current: ActiveAnchor, now: Long) {
        val protectionPos = current.protectionPos
        if (!Damage.feetProtection) {
            current.protectionPos = null
            completeProtection(current, now)
            return
        }

        if (protectionPos == null) {
            recordFailure(current, now)
            resetActive()
            nextSearchAt = now + searchInterval
            return
        }

        if (protectionPos.stateOrEmpty.isSolid) {
            completeProtection(current, now)
            return
        }

        if (findProtectionSlot() == null ||
            !isSafe(current, useCache = false, protectionPos = protectionPos) ||
            now - current.protectionStartedAt >= placeTimeout
        ) {
            recordFailure(current, now)
            resetActive()
            nextSearchAt = now + searchInterval
            return
        }

        placer.update(setOf(protectionPos))
    }

    private fun beginProtection(current: ActiveAnchor, protectionPos: BlockPos, now: Long) {
        actionGeneration++
        current.phase = AnchorAuraPhase.PROTECTING
        current.pending = false
        current.pendingGeneration = 0L
        current.awaitingConfirmation = false
        current.awaitingSince = 0L
        current.lastActionAt = now
        current.protectionPos = protectionPos.immutable()
        current.protectionStartedAt = now
        placer.update(setOf(protectionPos))
    }

    private fun completeProtection(current: ActiveAnchor, now: Long) {
        actionGeneration++
        placer.clear()
        current.phase = AnchorAuraPhase.DETONATING
        current.pending = false
        current.pendingGeneration = 0L
        current.awaitingConfirmation = false
        current.awaitingSince = 0L
        current.lastActionAt = now
        current.protectionChecked = true
        retries.remove(current.pos.asLong())
    }

    /** Finds a new solid block on the anchor-to-feet rays and verifies the complete blast result before placing it. */
    private fun findBestProtection(
        anchorPos: BlockPos,
        target: LivingEntity,
        reserveGlowstoneForCharge: Boolean = false,
    ): ProtectionChoice? {
        if (findProtectionSlot(reserveGlowstoneForCharge) == null) {
            return null
        }

        val feet = player.blockPosition()
        val origin = player.position().add(0.0, 0.25, 0.0)
        val anchorCenter = anchorPos.center
        if (origin.distanceToSqr(anchorCenter) < 0.01) {
            return null
        }

        val candidates = LinkedHashSet<BlockPos>(protectionDirections.size * 2)
        for (direction in protectionDirections) {
            candidates.add(feet.relative(direction))
            candidates.add(anchorPos.relative(direction))
        }

        return candidates.asSequence()
            .filter { pos ->
                pos != anchorPos && world.worldBorder.isWithinBounds(pos) &&
                    pos.stateOrEmpty.canBeReplaced() && pos.isUnobstructed()
            }
            .mapNotNull { pos ->
                val lineDistance = AnchorProtectionLogic.lineDistance(pos, origin, anchorCenter)
                    ?: return@mapNotNull null
                val damage = AnchorAuraDamage.calculate(
                    target = target,
                    anchorPos = anchorPos,
                    terrain = Damage.terrain,
                    selfPredictionTicks = Prediction.selfTicks,
                    targetPredictionTicks = Prediction.targetTicks,
                    useCache = false,
                    protectionPos = pos,
                )
                if (AnchorAuraLogic.damageRejection(damage, damageLimits()) != null || !placer.canClickPlace(pos)) {
                    return@mapNotNull null
                }

                ProtectionChoice(pos, damage, lineDistance)
            }
            .minWithOrNull(
                compareBy<ProtectionChoice>(
                    { it.damage.selfDamage },
                    { it.damage.friendDamage },
                    { -it.damage.enemyDamage },
                    { it.lineDistance },
                )
            )
    }

    @Suppress("CognitiveComplexMethod", "LongMethod", "NestedBlockDepth", "LoopWithTooManyJumpStatements")
    private fun findBestCandidate(targets: List<LivingEntity>, now: Long): Candidate? {
        if (targets.isEmpty()) {
            return null
        }

        val anchorsAvailable = findAnchorSlot() != null
        val glowstoneAvailable = findGlowstoneSlot() != null
        val detonationHandAvailable = findDetonationSlot() != null
        val perTargetBudget = (maxCandidates / targets.size).coerceAtLeast(1)
        var evaluated = 0
        var best: Candidate? = null

        for (target in targets) {
            val origin = target.blockPosition()
            var targetEvaluated = 0
            for (offset in offsets()) {
                if (evaluated >= maxCandidates) {
                    return best
                }
                if (targetEvaluated >= perTargetBudget) {
                    break
                }

                val pos = origin.offset(offset)
                if (!world.worldBorder.isWithinBounds(pos)) {
                    continue
                }

                val state = pos.stateOrEmpty
                val isAnchor = state.block is RespawnAnchorBlock
                val charges = if (isAnchor) state.getValue(RespawnAnchorBlock.CHARGE) else 0
                val action: AnchorAuraAction? = if (isAnchor) {
                    AnchorAuraLogic.actionFor(anchorPresent = true, charges, maxCharge)
                } else if (state.canBeReplaced() && pos.isUnobstructed()) {
                    AnchorAuraAction.PLACE
                } else {
                    null
                }
                if (action == null) {
                    continue
                }
                if (action == AnchorAuraAction.PLACE &&
                    pos.distSqr(origin) > MAX_PLACEMENT_TARGET_DISTANCE_SQR
                ) {
                    continue
                }

                val available = when (action) {
                    AnchorAuraAction.PLACE -> anchorsAvailable && glowstoneAvailable && detonationHandAvailable
                    AnchorAuraAction.CHARGE -> glowstoneAvailable && detonationHandAvailable
                    AnchorAuraAction.DETONATE -> detonationHandAvailable
                }
                if (!available || (retries[pos.asLong()]?.notBefore ?: 0L) > now) {
                    continue
                }

                val visibility = visibility(pos, isAnchor) ?: continue
                evaluated++
                targetEvaluated++
                val initialDamage = AnchorAuraDamage.calculate(
                    target = target,
                    anchorPos = pos,
                    terrain = Damage.terrain,
                    selfPredictionTicks = Prediction.selfTicks,
                    targetPredictionTicks = Prediction.targetTicks,
                )
                val rejection = AnchorAuraLogic.damageRejection(initialDamage, damageLimits())
                val damage = if (rejection == null) {
                    initialDamage
                } else {
                    if (!Damage.feetProtection || !canPlanProtection(rejection)) {
                        continue
                    }

                    findBestProtection(
                        pos,
                        target,
                        reserveGlowstoneForCharge = action != AnchorAuraAction.DETONATE,
                    )?.damage ?: continue
                }

                val score = AnchorAuraLogic.candidateScore(
                    damage = damage,
                    distance = player.eyePosition.distanceTo(pos.center),
                    existingAnchor = isAnchor,
                    visible = visibility.visible,
                )
                if (best == null || score > best.score) {
                    best = Candidate(pos.immutable(), target, action, charges, score)
                }
            }
        }

        return best
    }

    private fun visibility(pos: BlockPos, existingAnchor: Boolean): Visibility? {
        val box = FULL_BOX.move(pos)
        val expectedTarget = if (existingAnchor) pos else null
        val eyes = player.eyePosition
        val range = placer.range.toDouble()
        val visible = canSeeBox(
            eyes,
            box,
            range = range,
            wallsRange = 0.0,
            expectedTarget = expectedTarget,
            maxPoints = 32,
        )
        if (visible) {
            return Visibility(true)
        }

        if (placer.wallRange <= 0f) {
            return null
        }

        val reachableThroughWalls = canSeeBox(
            eyes,
            box,
            range = range,
            wallsRange = placer.wallRange.toDouble(),
            expectedTarget = expectedTarget,
            maxPoints = 32,
        )
        return if (reachableThroughWalls) Visibility(false) else null
    }

    @Suppress("CognitiveComplexMethod", "LongMethod")
    private fun scheduleInteraction(current: ActiveAnchor, action: AnchorAuraAction, now: Long) {
        if (interactionSlot(action) == null) {
            recordFailure(current, now)
            resetActive()
            nextSearchAt = now + searchInterval
            return
        }

        val rotation = raytraceBox(
            player.eyePosition,
            FULL_BOX.move(current.pos),
            range = placer.range.toDouble(),
            wallsRange = placer.wallRange.toDouble(),
        )?.rotation ?: run {
            recordFailure(current, now)
            return
        }

        val generation = ++actionGeneration
        current.pending = true
        current.pendingSince = now
        current.pendingGeneration = generation
        current.lastActionAt = now

        rotationMode.activeMode.rotate(
            rotation,
            isFinished = {
                isCurrentInteraction(current, generation, action) &&
                    verifiedHit(current.pos, RotationManager.serverRotation, action) != null
            },
            onFinished = {
                if (!isCurrentInteraction(current, generation, action)) {
                    return@rotate
                }

                current.pending = false
                current.pendingGeneration = 0L
                val verificationRotation = if (rotationMode.activeMode is NoRotationMode) {
                    rotation
                } else {
                    RotationManager.serverRotation
                }
                val hit = verifiedHit(current.pos, verificationRotation, action) ?: run {
                    recordFailure(current, System.currentTimeMillis())
                    return@rotate
                }

                // Post-rotation tasks can run after movement. Never reuse the earlier candidate safety result here.
                if (action == AnchorAuraAction.DETONATE && !isSafe(current, useCache = false)) {
                    resetActive()
                    nextSearchAt = System.currentTimeMillis() + searchInterval
                    return@rotate
                }

                val slot = interactionSlot(action) ?: run {
                    val failureTime = System.currentTimeMillis()
                    recordFailure(current, failureTime)
                    resetActive()
                    nextSearchAt = failureTime + searchInterval
                    return@rotate
                }
                if (!SilentHotbar.selectSlotSilently(this, slot, slotResetDelay)) {
                    recordFailure(current, System.currentTimeMillis())
                    return@rotate
                }

                val actionTime = System.currentTimeMillis()
                if (!performAnchorInteraction(slot, hit, action)) {
                    recordFailure(current, actionTime)
                    return@rotate
                }

                current.lastActionAt = actionTime
                current.awaitingSince = actionTime
                current.awaitingConfirmation = true
            },
        )
    }

    /**
     * Mirrors the use-on branch of vanilla Minecraft.startUseItem. It deliberately has no use-item fallback: if an
     * anchor rejects the click, an unrelated held item must not be activated as a side effect.
     */
    private fun performAnchorInteraction(
        slot: HotbarItemSlot,
        hit: BlockHitResult,
        action: AnchorAuraAction,
    ): Boolean {
        val hand = slot.useHand
        val stack = player.getItemInHand(hand)
        if (action == AnchorAuraAction.CHARGE && stack.item !== Items.GLOWSTONE ||
            action == AnchorAuraAction.DETONATE && stack.item === Items.GLOWSTONE
        ) {
            return false
        }

        val oldCount = stack.count
        val result = interaction.useItemOn(player, hand, hit)
        if (!result.consumesAction()) {
            return false
        }

        if (result.shouldSwingHand()) {
            swingMode.interactSwing(hand)
        }

        if (!stack.isEmpty && (stack.count != oldCount || player.hasInfiniteMaterials())) {
            player.itemUsed(hand)
        }

        return true
    }

    private fun verifiedHit(pos: BlockPos, rotation: Rotation, action: AnchorAuraAction): BlockHitResult? {
        val state = pos.stateOrEmpty
        if (state.block !is RespawnAnchorBlock) {
            return null
        }

        val charges = state.getValue(RespawnAnchorBlock.CHARGE)
        if (AnchorAuraLogic.actionFor(anchorPresent = true, charges, maxCharge) != action) {
            return null
        }

        val hit = raytraceBlock(
            max(placer.range, placer.wallRange).toDouble(),
            rotation,
            pos,
            state,
        ) ?: return null
        if (hit.type != HitResult.Type.BLOCK || hit.blockPos != pos) {
            return null
        }

        val eyes = player.eyePosition
        val visible = player.isFacingBlock(eyes, hit.location, pos, hit.direction)
        val allowedRange = if (visible) placer.range else placer.wallRange
        if (eyes.distanceToSqr(hit.location) > allowedRange.toDouble().sq()) {
            return null
        }

        return hit
    }

    private fun isCurrentInteraction(current: ActiveAnchor, generation: Long, action: AnchorAuraAction): Boolean {
        return active === current && current.pending && current.pendingGeneration == generation && running &&
            current.phase == AnchorAuraLogic.phaseFor(action) && !world.respawnAnchorWorks &&
            !CombatManager.shouldPauseCombat && placer.canAct() && targetTracker.validate(current.target)
    }

    private fun isSafe(
        current: ActiveAnchor,
        useCache: Boolean,
        protectionPos: BlockPos? = current.protectionPos,
    ): Boolean {
        val effectiveProtectionPos = protectionPos.takeIf { Damage.feetProtection }
        val damage = AnchorAuraDamage.calculate(
            target = current.target,
            anchorPos = current.pos,
            terrain = Damage.terrain,
            selfPredictionTicks = Prediction.selfTicks,
            targetPredictionTicks = Prediction.targetTicks,
            useCache = useCache,
            protectionPos = effectiveProtectionPos,
        )
        val rejection = AnchorAuraLogic.damageRejection(damage, damageLimits())
        if (rejection == null) {
            return true
        }

        if (effectiveProtectionPos != null || !Damage.feetProtection || !canPlanProtection(rejection)) {
            return false
        }
        if (current.phase != AnchorAuraPhase.PLACING && current.phase != AnchorAuraPhase.CHARGING) {
            return false
        }

        val protection = findBestProtection(
            current.pos,
            current.target,
            reserveGlowstoneForCharge = true,
        ) ?: return false
        current.protectionPos = protection.pos.immutable()
        current.protectionChecked = true
        return true
    }

    private fun canPlanProtection(rejection: AnchorDamageRejection): Boolean = when (rejection) {
        AnchorDamageRejection.SELF_DAMAGE,
        AnchorDamageRejection.ANTI_SUICIDE,
        AnchorDamageRejection.INEFFICIENT -> true
        AnchorDamageRejection.MIN_ENEMY_DAMAGE,
        AnchorDamageRejection.FRIEND_DAMAGE -> false
    }

    private fun damageLimits() = AnchorDamageLimits(
        minEnemyDamage = Damage.minEnemy,
        maxSelfDamage = Damage.maxSelf,
        maxFriendDamage = Damage.maxFriend,
        antiSuicide = Damage.antiSuicide,
        lethalOverride = Damage.lethalOverride,
        efficient = Damage.efficient,
    )

    private fun interactionSlot(action: AnchorAuraAction): HotbarItemSlot? = when (action) {
        AnchorAuraAction.CHARGE -> findGlowstoneSlot()
        AnchorAuraAction.DETONATE -> findDetonationSlot()
        AnchorAuraAction.PLACE -> findAnchorSlot()
    }

    private fun findAnchorSlot() = Slots.OffhandWithHotbar.findClosestSlot(Items.RESPAWN_ANCHOR)

    private fun findGlowstoneSlot() = Slots.OffhandWithHotbar.findClosestSlot(Items.GLOWSTONE)

    /** Reuse the charging hand when possible, without planning the same last glowstone as both charge and shield. */
    private fun findProtectionSlot(reserveGlowstoneForCharge: Boolean = false): HotbarItemSlot? {
        val glowstoneCount = Slots.OffhandWithHotbar.sumOf { slot ->
            if (slot.itemStack.item === Items.GLOWSTONE) slot.itemStack.count else 0
        }
        return findGlowstoneSlot().takeIf {
            AnchorProtectionLogic.canProtectWithGlowstone(glowstoneCount, reserveGlowstoneForCharge)
        } ?: Slots.OffhandWithHotbar.findClosestSlot(Items.OBSIDIAN)
    }

    /** Empty stacks intentionally qualify. Glowstone in the opposite hand does not matter for a direct hand packet. */
    private fun findDetonationSlot() = Slots.OffhandWithHotbar.findClosestSlot { stack ->
        stack.item !== Items.GLOWSTONE
    }

    private fun recordFailure(current: ActiveAnchor, now: Long) {
        actionGeneration++
        current.pending = false
        current.pendingGeneration = 0L
        current.awaitingConfirmation = false
        current.awaitingSince = 0L
        current.lastActionAt = now

        val key = current.pos.asLong()
        val failures = (retries[key]?.failures ?: 0) + 1
        val delay = AnchorAuraLogic.retryDelayMs(
            failures,
            baseRetryDelay.toLong(),
            maxRetryDelay.toLong(),
        )
        if (retries.size >= 64 && key !in retries) {
            retries.entries.minByOrNull { it.value.notBefore }?.let { retries.remove(it.key) }
        }
        retries[key] = RetryState(failures, now + delay)
    }

    private fun resetActive() {
        actionGeneration++
        active = null
        placer.clear()
        SilentHotbar.resetSlot(this)
        SilentHotbar.resetSlot(placer)
    }

    private fun resetRuntime(clearRetries: Boolean) {
        resetActive()
        targetTracker.reset()
        AnchorAuraDamage.clearCache()
        nextSearchAt = 0L
        if (clearRetries) {
            retries.clear()
        }
    }

    private fun offsets(): Array<BlockPos> {
        if (cachedSearchRadius != searchRadius) {
            cachedSearchRadius = searchRadius
            searchOffsets = BlockPos.ZERO.getSortedSphere(searchRadius)
        }
        return searchOffsets
    }

}
