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
import net.ccbluex.liquidbounce.deeplearn.model.ModelRegistry
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.AttackEntityEvent
import net.ccbluex.liquidbounce.event.events.ClientShutdownEvent
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.addon.UnstableAddonApi
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.KillAuraRotationsValueGroup
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug.DebuggedLineSegment
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug.debugGeometry
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug.debugParameter
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.client.protocolVersion
import net.ccbluex.liquidbounce.utils.client.world
import net.ccbluex.liquidbounce.utils.entity.hasCooldown
import net.ccbluex.liquidbounce.utils.entity.rotation
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention
import net.minecraft.world.entity.LivingEntity
import kotlin.random.Random

/**
 * Keeps the last ticks of our own fight in the same raw form as recordings, so live inference
 * sees exactly the features the model was trained on.
 */
@UnstableAddonApi
object CombatController : EventListener {
    private const val WINDOW = CombatFeatures.LIVE_WINDOW
    private const val MAX_GAP = 20
    private const val WARM_DISTANCE = 8f
    private val KEY_NAMES = listOf(CombatTrack.FORWARD to "forward", CombatTrack.BACK to "back",
        CombatTrack.LEFT to "left", CombatTrack.RIGHT to "right", CombatTrack.JUMP to "jump",
        CombatTrack.SNEAK to "sneak", CombatTrack.SPRINT to "sprint")

    private class History {
        val self = ArrayDeque<CombatFrame>()
        val opponent = ArrayDeque<CombatFrame>()
        val contexts = ArrayDeque<CombatSelfContext>()
        var lastTick = Int.MIN_VALUE

        fun add(own: CombatFrame, other: CombatFrame, context: CombatSelfContext, tick: Int) {
            self.addLast(own)
            opponent.addLast(other)
            contexts.addLast(context)
            lastTick = tick
            while (self.size > WINDOW) {
                self.removeFirst()
                opponent.removeFirst()
                contexts.removeFirst()
            }
        }
    }

    private var history = History()
    private val self get() = history.self
    private val contexts get() = history.contexts

    /** Players who may become the target next, so a new target has a decision right away. */
    private val warm = Int2ObjectOpenHashMap<History>()
    private val input = FloatArray(CombatFeatures.SIZE)
    private var targetId = Int.MIN_VALUE
    private var lastTick = Int.MIN_VALUE
    private var attacked = false
    private var cached: CombatDecision? = null
    private var lastTarget: LivingEntity? = null
    private var gapTicks = 0

    var randomness = 0.5f
    var lead = 0
    var styleOverride: CombatStyle? = null

    /** Whether KillAura aims with the model. */
    val active get() = ModuleKillAura.running && KillAuraRotationsValueGroup.usesAiRotations

    val style
        get() = styleOverride ?: if (player.hasCooldown) CombatStyle.COOLDOWN else CombatStyle.LEGACY

    val lastDecision get() = cached
    val lastDecisionTick get() = lastTick
    val historySize get() = self.size

    /** The features of the last decision; overwritten by the next one. */
    val lastInput: FloatArray get() = input

    init {
        CombatPackets
    }

    /** [decide] from the rotation we are sending, for callers that do not aim themselves. */
    fun decide(target: LivingEntity) = decide(target, RotationManager.currentRotation ?: player.rotation)

    /**
     * The decision for this tick, computed once from the state before any rotation or attack of
     * this tick. [view] is the rotation the server last received from us.
     */
    fun decide(target: LivingEntity, view: Rotation): CombatDecision? {
        if (mc.gui.screen() != null || player.isDeadOrDying || player.isSpectator || !target.isAlive) {
            reset()
            return null
        }
        val tick = player.tickCount
        if (tick == lastTick && target.id == targetId) {
            return cached
        }
        if (target.id != targetId || tick != lastTick + 1) {
            val previous = lastTarget
            if (previous != null && lastTick == tick - 1) {
                warm.put(previous.id, history)
            }
            reset()
            warm.remove(target.id)?.takeIf { it.lastTick == tick - 1 }?.let { history = it }
        }
        lastTick = tick
        targetId = target.id
        lastTarget = target
        gapTicks = 0
        capture(target, view)
        cached = predict()
        debug(target, view, cached)
        return cached
    }

    /**
     * KillAura skips a tick now and then, e.g. while the opponent's shield makes it drop the target. The history
     * continues through such gaps instead of starting over, which would leave the aim without decisions for a while.
     */
    @Suppress("unused")
    private val gapHandler = handler<GameTickEvent>(priority = EventPriorityConvention.READ_FINAL_STATE) {
        val target = lastTarget ?: return@handler
        val tick = player.tickCount
        if (lastTick != tick - 1 || mc.gui.screen() != null || !target.isAlive || target.isRemoved) {
            return@handler
        }
        if (++gapTicks > MAX_GAP) {
            reset()
            return@handler
        }
        lastTick = tick
        capture(target, RotationManager.serverRotation)
        cached = null
    }

    @Suppress("unused")
    private val warmHandler = handler<GameTickEvent>(priority = EventPriorityConvention.READ_FINAL_STATE) {
        if (!active || mc.gui.screen() != null || player.isDeadOrDying || player.isSpectator) {
            warm.clear()
            return@handler
        }
        val tick = player.tickCount
        val candidates = world.players().filter {
            it !== player && it.id != targetId && it.isAlive && it.distanceTo(player) <= WARM_DISTANCE
        }
        warm.keys.retainAll(candidates.map { it.id }.toSet())
        if (candidates.isEmpty()) {
            return@handler
        }
        val own = own(RotationManager.serverRotation)
        val nearby = CombatSampler.nearbyPlayers()
        for (candidate in candidates) {
            val kept = warm.get(candidate.id)?.takeIf { it.lastTick == tick - 1 }
                ?: History().also { warm.put(candidate.id, it) }
            val other = CombatSampler.frame(candidate)
            kept.add(own, other, CombatSelfContext.capture(world, own, other, nearby, keys(), swung(own)), tick)
        }
    }

    fun reset() {
        history = History()
        targetId = Int.MIN_VALUE
        lastTick = Int.MIN_VALUE
        lastTarget = null
        gapTicks = 0
        attacked = false
        cached = null
    }

    private fun own(view: Rotation) = CombatSampler.frame(player).copy(yaw = view.yaw, pitch = view.pitch)

    private fun swung(own: CombatFrame) = if (own.events and CombatTrack.SWING != 0) 1 else 0

    /** The keys of the movement that brought us here; this tick's are read only after the rotation is decided. */
    private fun keys() = CombatSampler.keys(player.input.keyPresses)

    private fun capture(target: LivingEntity, view: Rotation) {
        val own = own(view)
        val other = CombatSampler.frame(target)
        val context = CombatSelfContext.capture(world, own, other, CombatSampler.nearbyPlayers(), keys(),
            if (attacked) 1 else swung(own))
        history.add(own, other, context, player.tickCount)
        attacked = false
    }

    /** What the model saw and decided this tick, for ModuleDebug. */
    private fun debug(target: LivingEntity, view: Rotation, decision: CombatDecision?) {
        debugParameter("Target") {
            "%s, %.1f blocks, hurt %d".format(target.scoreboardName, target.distanceTo(player), target.hurtTime)
        }
        debugParameter("History") { "${self.size} ticks" }
        debugParameter("Turn") {
            decision?.let { "%.1f yaw, %.1f pitch%s".format(it.yaw, it.pitch, if (it.clamped) ", clamped" else "") }
        }
        debugParameter("Keys") {
            val keys = contexts.lastOrNull()?.input ?: 0
            KEY_NAMES.filter { (bit, _) -> keys and bit != 0 }.joinToString(" ") { it.second }.ifEmpty { "none" }
        }
        val eyes = player.eyePosition
        debugGeometry("View") { DebuggedLineSegment(eyes, eyes.add(view.directionVector.scale(4.0)), Color4b.RED) }
        debugGeometry("Decision") {
            decision?.let {
                val next = Rotation(view.yaw + it.yaw, view.pitch + it.pitch)
                DebuggedLineSegment(eyes, eyes.add(next.directionVector.scale(4.0)), Color4b.GREEN)
            }
        }
    }

    private fun predict(): CombatDecision? {
        if (self.size <= CombatFeatures.HISTORY) {
            return null
        }
        val builder = CombatTimelineBuilder(CombatSource.FIRST_PERSON, style, protocolVersion.version, 0, 0, -1, -1)
        for (index in self.indices) {
            builder.add(self[index], history.opponent[index], contexts[index])
        }
        val timeline = builder.build(0)
        val last = timeline.ticks - 1
        CombatFeatures.write(timeline, CombatPerception(timeline, 0).lead(lead), CombatView.recorded(timeline),
            CombatFeatures.recordedAttacks(timeline), last, input)
        return CombatModels.withActive { model, info ->
            if (info.aim) {
                val output = model.predict(input)
                CombatOutputs.decide(output.values, output.clamped, randomness, info.turnCap, Random.Default)
            } else {
                null
            }
        }
    }

    @Suppress("unused")
    private val attackHandler = handler<AttackEntityEvent> { event ->
        if (event.isCancelled || event.entity.id != targetId) {
            return@handler
        }
        // Recordings put an attack on the frame of the tick it was made in, after this tick's frame was captured
        if (lastTick == player.tickCount && contexts.isNotEmpty()) {
            contexts.addLast(contexts.removeLast().withClick())
        } else {
            attacked = true
        }
    }

    @Suppress("unused")
    private val worldHandler = handler<WorldChangeEvent> {
        reset()
        warm.clear()
    }

    @Suppress("unused")
    private val shutdownHandler = handler<ClientShutdownEvent> {
        ModelRegistry.close()
    }
}
