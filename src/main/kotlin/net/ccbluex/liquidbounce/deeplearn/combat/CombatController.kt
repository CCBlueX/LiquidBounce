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
package net.ccbluex.liquidbounce.deeplearn.combat

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.AttackEntityEvent
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.addon.UnstableAddonApi
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.client.protocolVersion
import net.ccbluex.liquidbounce.utils.client.world
import net.ccbluex.liquidbounce.utils.entity.rotation
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention
import net.minecraft.world.entity.LivingEntity

/** What the controller saw and decided at [tick]. [input] holds the features. */
@UnstableAddonApi
class CombatStep(
    val tick: Int,
    val targetId: Int,
    val history: Int,
    val input: FloatArray,
    val decision: CombatDecision?,
)

/**
 * Keeps the last ticks of our fights in the same raw form as recordings, so live inference sees exactly the
 * features the model was trained on. Runs while [parent] does.
 */
@UnstableAddonApi
class CombatController(private val parent: EventListener) : EventListener {
    private class Step(val self: CombatFrame, val target: CombatFrame, val context: CombatSelfContext)

    private class History {
        val steps = ArrayDeque<Step>()
        var lastTick = Int.MIN_VALUE
    }

    /** Every player who may be the target next and the last target, so a new target has a decision at once. */
    private val histories = Int2ObjectOpenHashMap<History>()
    private var target: LivingEntity? = null
    private var targetTick = Int.MIN_VALUE
    private var attacked = false

    var lastStep: CombatStep? = null
        private set

    init {
        CombatPackets.capture(this)
    }

    override fun parent() = parent

    /** The decision for this tick, or null until [target]'s history is long enough. */
    fun decide(target: LivingEntity, model: BundledCombatModel, randomness: Float, lead: Int): CombatDecision? {
        val tick = player.tickCount
        lastStep?.takeIf { it.tick == tick && it.targetId == target.id }?.let { return it.decision }
        this.target = target
        targetTick = tick
        val history = histories.get(target.id)
            ?.takeIf { it.lastTick == tick && it.steps.size > CombatFeatures.HISTORY && target.isAlive }
            ?: return null
        val builder = CombatTimelineBuilder(CombatSource.FIRST_PERSON, CombatStyle.of(player),
            protocolVersion.version, 0, 0, -1, -1)
        history.steps.forEach { builder.add(it.self, it.target, it.context) }
        val timeline = builder.build(0)
        val input = FloatArray(CombatFeatures.SIZE)
        CombatFeatures.write(timeline, CombatPerception(timeline, 0).lead(lead), CombatView.recorded(timeline),
            CombatFeatures.recordedAttacks(timeline), timeline.ticks - 1, input)
        val decision = CombatModels.decide(model, input, randomness)
        lastStep = CombatStep(tick, target.id, history.steps.size, input, decision)
        return decision
    }

    fun reset() {
        histories.clear()
        target = null
        lastStep = null
    }

    /**
     * After this tick's packets and before rotations turn away from the view recorded here; recordings sample at
     * the same point.
     */
    @Suppress("unused")
    private val captureHandler = handler<GameTickEvent>(
        priority = (EventPriorityConvention.FIRST_PRIORITY + 1).toShort()
    ) {
        val clicked = attacked
        attacked = false
        if (mc.gui.screen() != null || player.isDeadOrDying || player.isSpectator) {
            reset()
            return@handler
        }
        val tick = player.tickCount
        val candidates = world.players().filterTo(ArrayList<LivingEntity>()) {
            it !== player && it.isAlive && it.distanceTo(player) <= CANDIDATE_RANGE
        }
        target?.takeIf { it.isAlive && !it.isRemoved && it !in candidates && tick - targetTick <= TARGET_TICKS }
            ?.let(candidates::add)
        histories.keys.retainAll(candidates.mapTo(HashSet()) { it.id })
        if (candidates.isEmpty()) {
            return@handler
        }
        val view = RotationManager.currentRotation ?: player.rotation
        val own = CombatSampler.frame(player).copy(yaw = view.yaw, pitch = view.pitch)
        // The keys of the movement that brought us here; this tick's are read only after the rotation is decided
        val keys = CombatSampler.keys(player.input.keyPresses)
        val clicks = if (clicked || own.events and CombatTrack.Event.SWING != 0) 1 else 0
        val nearby = CombatSampler.nearbyPlayers()
        val probes = CombatSampler.probes(world, own)
        for (candidate in candidates) {
            val history = histories.get(candidate.id)?.takeIf { it.lastTick == tick - 1 }
                ?: History().also { histories.put(candidate.id, it) }
            val other = CombatSampler.frame(candidate)
            val context = CombatSelfContext(CombatSampler.lineOfSight(world, own, other), probes,
                CombatSampler.nearestOther(own, other, nearby), CombatSampler.othersSwingNear(own, other, nearby),
                keys, clicks)
            history.steps.addLast(Step(own, other, context))
            history.lastTick = tick
            if (history.steps.size > CombatFeatures.LIVE_WINDOW) {
                history.steps.removeFirst()
            }
        }
    }

    /** Recordings show a click on the next frame, like the swing it sends, which NoSwing may keep from the server. */
    @Suppress("unused")
    private val attackHandler = handler<AttackEntityEvent> { event ->
        if (!event.isCancelled) {
            attacked = true
        }
    }

    private companion object {
        const val CANDIDATE_RANGE = 8f
        const val TARGET_TICKS = 20
    }
}
