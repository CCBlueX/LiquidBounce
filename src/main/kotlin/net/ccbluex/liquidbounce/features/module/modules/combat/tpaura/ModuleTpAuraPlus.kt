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
package net.ccbluex.liquidbounce.features.module.modules.combat.tpaura

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap
import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.config.types.group.ValueGroup
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.KeybindIsPressedEvent
import net.ccbluex.liquidbounce.event.events.KeyboardKeyEvent
import net.ccbluex.liquidbounce.event.events.MouseButtonEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.RotationUpdateEvent
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.integration.screen.ScreenManager
import net.minecraft.client.KeyMapping
import net.minecraft.client.gui.screens.Screen
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsValueGroup
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.aiming.utils.raytraceBox
import net.ccbluex.liquidbounce.utils.block.SwingMode
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.client.world
import net.ccbluex.liquidbounce.utils.clicking.Clicker
import net.ccbluex.liquidbounce.utils.clicking.isMissCooldownActive
import net.ccbluex.liquidbounce.utils.combat.TargetPriority
import net.ccbluex.liquidbounce.utils.combat.TargetSelector
import net.ccbluex.liquidbounce.utils.combat.attackEntity
import net.ccbluex.liquidbounce.utils.entity.getEyePositionAt
import net.ccbluex.liquidbounce.utils.entity.ping
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.math.fma
import net.ccbluex.liquidbounce.utils.navigation.route.RouteResult
import net.ccbluex.liquidbounce.utils.navigation.teleport.Route
import net.ccbluex.liquidbounce.utils.navigation.teleport.StepsValueGroup
import net.ccbluex.liquidbounce.utils.navigation.teleport.Teleport
import net.ccbluex.liquidbounce.utils.navigation.teleport.TeleportManager
import net.ccbluex.liquidbounce.utils.navigation.teleport.Trip
import net.ccbluex.liquidbounce.utils.navigation.terrain.Terrain
import net.ccbluex.liquidbounce.utils.navigation.terrain.hasRoom
import net.ccbluex.liquidbounce.utils.navigation.terrain.reachFeetPositions
import net.ccbluex.liquidbounce.utils.network.sendPacketSilently
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket
import net.minecraft.network.protocol.game.ClientboundHurtAnimationPacket
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket
import net.minecraft.resources.Identifier
import net.minecraft.sounds.SoundSource
import net.minecraft.util.Mth
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * TpAuraPlus
 *
 * Teleports next to enemies, attacks them, then returns or stays. Routes are found with a faster A*,
 * the destination follows the predicted position of a moving target, and the whole engagement only
 * starts once the attack cooldown will have passed on arrival — no more useless round trips.
 *
 * The [debug] switch writes every stage to `logs/tpauraplus.log` in the game directory and mirrors
 * stage changes to chat, so an engagement can be replayed and its failures diagnosed.
 */
@Suppress("TooManyFunctions")
object ModuleTpAuraPlus : ClientModule("TpAuraPlus", ModuleCategories.COMBAT, disableOnQuit = true) {

    private val rangeValue = float("Range", 30f, 6f..100f, "blocks")
    private val range by rangeValue
    private val afterAttack by enumChoice("AfterAttack", AfterAttack.RETURN)
    private val hold by int("Hold", 10, 1..40, "swings")

    private val prediction = tree(PredictionValueGroup())
    private val landingSide by enumChoice("LandingSide", LandingSide.ANY)
    private val flight by enumChoice("PacketStyle", Teleport.FlightMode.BURST, aliases = listOf("Flight"))

    /** The protocol takes the attack packet straight — no look, no click simulation, just the cooldown. */
    private val attackWithoutLooking by boolean("AttackWithoutLooking", false)

    /** The arm swings on the client only; the server sees a bare attack. */
    private val attackWithoutSwing by boolean("AttackWithoutSwing", false)

    /** Through-wall attacking, its reach nested under the switch. */
    private val throughWall = tree(object : ToggleableValueGroup(this@ModuleTpAuraPlus, "ThroughWall", false) {
        val range by float("Range", 2f, 0f..8f, "blocks")
    })
    private val allowThroughWall: Boolean get() = throughWall.enabled
    private val throughWallRange: Float get() = throughWall.range

    private val debug by boolean("Debug", false)
    private val sfx = tree(SfxValueGroup())
    private val manual = tree(ManualValueGroup())

    private val targetSelector = tree(TargetSelector(TargetPriority.DISTANCE, rangeValue))
    private val airTargetsValue = targetSelector.boolean("AirTargets", false)
    private val airTargets by airTargetsValue

    /** The spray needs the prediction: without it the probes would aim at where the target stands. */
    private val sprays: Boolean
        get() = prediction.enabled && prediction.aggressive

    // The click plan and the rotations only shape the looked attack; without it they configure nothing.
    private val clicker = tree(Clicker(this, mc.options.keyAttack))
    private val rotations = tree(RotationsValueGroup(this, combatSpecific = true))
    private val steps = tree(StepsValueGroup())

    private val trip = Trip(this, steps)
    private val view = trip.view

    init {
        tree(trip.renderer)
    }

    /** How far ahead of the target the destination aims, and when it is moved again. */
    private class PredictionValueGroup : ValueGroup("Prediction") {
        val enabled by boolean("Enabled", true)

        /** An upper bound on the lead; the actual lead is the measured time until the swing lands. */
        val ticks by int("MaxLead", 5, 1..10, "ticks", aliases = listOf("Ticks"))

        /** Moves the destination while the route is still being searched. */
        val liveRetarget by boolean("LiveRetarget", true)

        /** Swings from several spots around the target in one tick, so a landing does not hinge on one spot. */
        val aggressive by boolean("Spray", false, aliases = listOf("Aggressive"))
    }

    private enum class AfterAttack(override val tag: String) : Tagged {
        RETURN("Return"),
        STAY("Stay"),
    }

    private enum class Trigger(override val tag: String) : Tagged {
        LEFT_CLICK("LeftClick"),
        RIGHT_CLICK("RightClick"),
        JUMP("Jump"),
        SNEAK("Sneak"),
    }

    /** Armed but holding: routing is shown, nothing sets off until the player's action. */
    private class ManualValueGroup : ValueGroup("AttackManually") {
        val enabled by boolean("Enabled", false)
        val trigger by enumChoice("Trigger", Trigger.LEFT_CLICK, aliases = listOf("AttackWhen"))
        val preventExtraAction by boolean("PreventExtraAction", true)
        val showRoute by boolean("ShowRoute", true, aliases = listOf("ShowPath"))
        val refreshInterval by int("RefreshInterval", 8, 1..20, "ticks", aliases = listOf("RerouteInterval"))
    }

    /** Sound hints for the stages of an engagement: a switch per event with its sound identifier. */
    private class SfxValueGroup : ValueGroup("SoundHints", aliases = listOf("SFXTips")) {
        val enabled by boolean("Enabled", true)
        val volume by float("Volume", 60f, 0f..100f, "%")

        val pathFinding = tree(hint("PathFinding", "OnPathFinding", false, "block.enchantment_table.use"))
        val teleport = tree(hint("Teleport", "OnTeleport", false, "entity.enderman.teleport"))
        val hitSuccess = tree(hint("HitSuccess", "OnHitSuccess", false, "entity.arrow.hit_player"))
        val hitFail = tree(hint("HitFail", "OnHitFail", true, "entity.villager.trade"))
        val serverSetback = tree(hint("ServerSetback", "OnServerSetback", true, "entity.villager.no"))
        val routeFail = tree(hint("RouteFail", "OnRouteFail", true, "block.note_block.bass"))

        /** One hint, named after the stage, keeping the old `On…` name for saved configs. */
        private fun hint(name: String, legacy: String, enabled: Boolean, sound: String) =
            SoundValueGroup(name, enabled, sound, aliases = listOf(legacy)) { volume }
    }

    /** One hint: the switch of the event with the sound it plays, previewed while its name is typed. */
    private class SoundValueGroup(
        name: String,
        enabled: Boolean,
        defaultSound: String,
        aliases: List<String> = emptyList(),
        private val volume: () -> Float,
    ) : ToggleableValueGroup(null, name, enabled, aliases) {

        val soundValue = text("Sound", defaultSound)

        init {
            soundValue.onChanged { play(it, volume() / 100f) }
        }

        companion object {
            /** Plays [identifier] over the game's master volume; silent when it does not resolve. */
            fun play(identifier: String, volume: Float) {
                val id = Identifier.tryParse(identifier) ?: return
                val sound = BuiltInRegistries.SOUND_EVENT.get(id).orElse(null) ?: return
                val level = mc.level ?: return
                val master = mc.options.getFinalSoundSourceVolume(SoundSource.MASTER)
                level.playSound(player, player.x, player.y, player.z, sound, SoundSource.MASTER, volume * master, 1f)
            }
        }
    }

    /** Which side of the target the landing spot prefers. */
    private enum class LandingSide(override val tag: String) : Tagged {
        ANY("Any"),
        BEHIND("Behind"),
        FLANKS("Flanks"),
    }

    private enum class Phase { IDLE, ROUTING, READY, APPROACH, ATTACK, RETURN }

    /** Where the feet of an engagement may end, standing or hovering. */
    private class AttackGoals(val positions: List<BlockPos>, val feet: Map<BlockPos, Vec3>)

    private class Engagement(val target: LivingEntity) {
        var route: Route? = null
        var waited = 0
        var holdLeft = 0
        var retries = 0
        var dispatchFails = 0

        /** Re-routes because the route went stale under the player's own speed, not real failures. */
        var staleRetries = 0
        var attacked = false

        /** Ticks in ATTACK without a click opportunity, to give up when the click plan stalls. */
        var idleTicks = 0

        /** Click ticks the gate turned away, to give up when the aim never lands. */
        var blockedTicks = 0

        /** Where the goals were aimed at last; a drift moves the destination. */
        var goalAnchor: Vec3? = null

        /** Ticks spent searching for this engagement. */
        var searchTicks = 0

        /** When the route was last requested, to know how long the swing is still away. */
        var requestTick = 0

        /** Where the engagement began, to measure how far the player walked during it. */
        var origin: Vec3? = null
    }

    private var phase = Phase.IDLE
    private var engagement: Engagement? = null
    private var ticks = 0
    private var restUntil = 0

    /** The tick the last sound went out on, and how important it was — one sound wins a tick. */
    private var sfxTick = -1
    private var sfxPriority = 0

    /** Armed for a manual trigger: the route is shown and nothing sets off until the player acts. */
    private var manualTriggered = false
    private var manualRefreshAt = 0

    /** The target whose damage we are waiting for, and the tick that wait ends on. */
    private var awaitingHit: LivingEntity? = null
    private var hitWaitUntil = 0

    /** The binding whose queued press the next tick swallows, and the binding muted for that tick. */
    private var pendingSuppress: KeyMapping? = null
    private var mutedBinding: KeyMapping? = null
    private var mutedTick = -1

    /** Targets without a route, by entity id, until the tick they may be tried again. */
    private val unreachable = Int2IntOpenHashMap()

    /** Positions of the watched target, to measure its velocity and turn. */
    private val targetSamples = ArrayDeque<Pair<Int, Vec3>>()
    private var samplesId = 0

    /** Rolling average of the ticks from requesting a route to the swing landing. */
    private var latencyAvg = 3.0

    private val reach: Double
        get() = player.entityInteractionRange() - REACH_MARGIN

    /** The engagement while its target is still one. */
    private val liveEngagement: Engagement?
        get() = engagement?.takeIf { targetSelector.validate(it.target) }

    override fun onEnabled() {
        TpAuraPlusLog.open()
        dlog(
            "ENABLE",
            TpAuraPlusLog.details(
                "range" to range,
                "afterAttack" to afterAttack,
                "hold" to hold,
                "prediction" to prediction.enabled,
                "maxLead" to prediction.ticks,
                "liveRetarget" to prediction.liveRetarget,
                "airTargets" to airTargets,
                "landingSide" to landingSide,
                "packetStyle" to flight,
                "attackWithoutLooking" to attackWithoutLooking,
                "attackWithoutSwing" to attackWithoutSwing,
                "allowThroughWall" to allowThroughWall,
                "throughWallRange" to fmt(throughWallRange.toDouble()),
                "spray" to sprays,
                "manual" to manual.enabled,
                "soundHints" to sfx.enabled,
                "reach" to fmt(reach),
                "packetsPerTick" to steps.packetsPerTick,
            ),
        )
    }

    override fun onDisabled() {
        dlog("DISABLE", TpAuraPlusLog.details("phase" to phase))
        TeleportManager.undo(this)
        reset()
        restUntil = 0
        unreachable.clear()
        TpAuraPlusLog.close()
    }

    @Suppress("unused")
    private val worldChangeHandler = handler<WorldChangeEvent> {
        dlog("WORLD-CHANGE")
        reset()
        unreachable.clear()
    }

    @Suppress("unused")
    private val tickHandler = handler<GameTickEvent> {
        ticks++

        // The press that triggered the engagement: drop its queued click and mute the key for this
        // tick, before the game drains the click queue and reads the key states. Attacks and item
        // use come from that queue, the held keys from the state — both must be covered here.
        pendingSuppress?.let { binding ->
            binding.clickCount = 0
            mutedBinding = binding
            mutedTick = ticks
            pendingSuppress = null
        }

        // A swing no hurt event answers within the window did no damage: invulnerability, reach or a
        // dead target. Only the server's own word plays the hit sound.
        awaitingHit?.let { waiting ->
            if (ticks > hitWaitUntil) {
                awaitingHit = null
                dlog("HIT-UNCONFIRMED", TpAuraPlusLog.details("target" to desc(waiting)))
            }
        }
        if (player.isDeadOrDying || player.isSpectator || player.isPassenger) {
            leave("player-gone")
            return@handler
        }

        when (phase) {
            Phase.IDLE -> engage()
            Phase.ROUTING -> route()
            Phase.READY -> launch()
            Phase.APPROACH -> approach()
            Phase.ATTACK -> attack()
            Phase.RETURN -> {
                if (!trip.isAway) {
                    dlog(
                        "RETURN-DONE",
                        TpAuraPlusLog.details(
                            "client" to fmt(player.position()),
                            "server" to fmt(trip.feet),
                            "clientServerGap" to fmt(player.position().distanceTo(trip.feet)),
                        ),
                    )
                    view.done()
                    phase = Phase.IDLE
                } else {
                    dlog(
                        "RETURNING",
                        TpAuraPlusLog.details("server" to fmt(trip.feet), "client" to fmt(player.position())),
                    )
                }
            }
        }
    }

    @Suppress("unused")
    private val rotationUpdateHandler = handler<RotationUpdateEvent> {
        if (attackWithoutLooking) {
            return@handler
        }
        val engagement = engagement ?: return@handler
        val feet = when (phase) {
            Phase.READY, Phase.APPROACH -> engagement.route?.destination
            Phase.ATTACK -> trip.feet
            else -> null
        } ?: return@handler

        val eyes = player.getEyePositionAt(feet)
        val box = predictedBox(engagement.target)
        val rotation = raytraceBox(eyes, box, range = reach, wallsRange = 0.0)?.rotation
            ?: Rotation.lookingAt(box.center, eyes)

        RotationManager.setRotationTarget(
            rotations.toRotationTarget(rotation, engagement.target),
            priority = Priority.IMPORTANT_FOR_USAGE_2,
            provider = this@ModuleTpAuraPlus
        )
    }

    @Suppress("unused")
    private val manualMouseHandler = handler<MouseButtonEvent> { event ->
        if (event.isPressed && inputFree(event.screen) && event.key == actionBinding(manual.trigger).key) {
            onManualTrigger()
        }
    }

    @Suppress("unused")
    private val manualKeyHandler = handler<KeyboardKeyEvent> { event ->
        if (event.isPressed && !event.isRepeat && inputFree(event.screen) &&
            event.key == actionBinding(manual.trigger).key
        ) {
            onManualTrigger()
        }
    }

    /** Swallows the triggering action itself, for the tick after the press — once, not again. */
    @Suppress("unused")
    private val manualActionHandler = handler<KeybindIsPressedEvent> { event ->
        if (event.keyBinding === mutedBinding && ticks == mutedTick) {
            event.isPressed = false
        }
    }

    private fun engage() {
        if (ticks < restUntil || !trip.isAvailable) {
            return
        }

        // A manual trigger decides when to go; the cooldown is waited out at dispatch instead.
        val ready = if (manual.enabled) {
            0
        } else {
            readyClickTick() ?: run {
                dlog(
                    "ENGAGE-WAIT-COOLDOWN",
                    TpAuraPlusLog.details(
                        "progress" to fmt(cooldownProgress()),
                        "missTime" to mc.missTime,
                    ),
                )
                return
            }
        }

        val origin = trip.feet
        val target = targetSelector.targets().firstOrNull { unreachable.getOrDefault(it.id, 0) <= ticks }
        engagement = target?.let(::Engagement)
        if (target == null) {
            view.clear()
            return
        }

        engagement?.origin = origin
        engagement?.requestTick = ticks
        view.target = target
        val dist = player.position().distanceTo(target.position())
        if (isHittable(origin, target)) {
            if (manual.enabled && !manualTriggered) {
                // Armed inside reach: the swing waits for the trigger.
                dlog(
                    "MANUAL-ARMED",
                    TpAuraPlusLog.details("target" to desc(target), "dist" to fmt(dist)),
                )
                return
            }

            dlog(
                "ENGAGE-IN-REACH",
                TpAuraPlusLog.details("target" to desc(target), "dist" to fmt(dist), "ready" to ready),
            )
            dchat("engage ${desc(target)} in reach, dist=${fmt(dist)}")
            startAttack()
            attack()
            return
        }

        val goals = attackPositions(target, origin)
        if (goals.positions.isEmpty()) {
            dlog(
                "ENGAGE-NO-GOALS",
                TpAuraPlusLog.details(
                    "target" to desc(target),
                    "dist" to fmt(dist),
                    "targetY" to fmt(target.position().y),
                    "targetOnGround" to target.onGround(),
                ),
            )
            dchat("no attack positions for ${desc(target)} (y=${fmt(target.position().y)} onGround=${target.onGround()})")
            fail(target, "no-attack-positions")
        } else {
            dlog(
                "ENGAGE",
                TpAuraPlusLog.details(
                    "target" to desc(target),
                    "dist" to fmt(dist),
                    "goals" to goals.positions.size,
                    "goal0" to goals.positions.first(),
                    "origin" to fmt(origin),
                    "ready" to ready,
                    "serverPin" to (TeleportManager.serverPosition?.let { fmt(it) } ?: "none"),
                    "pinGap" to (TeleportManager.serverPosition?.let { fmt(player.position().distanceTo(it)) } ?: "-"),
                ),
            )
            dchat("engage ${desc(target)} dist=${fmt(dist)} goals=${goals.positions.size}")
            sfx(sfx.pathFinding, SFX_PATH)
            requestRoute(origin, goals)
        }
    }

    private fun requestRoute(origin: Vec3, goals: AttackGoals, reuseScores: Boolean = false) {
        val anchor = engagement?.target?.let { predictedBox(it).center }
        engagement?.goalAnchor = anchor
        engagement?.searchTicks = 0
        engagement?.requestTick = ticks
        dlog(
            "REQUEST-ROUTE",
            TpAuraPlusLog.details(
                "origin" to fmt(origin),
                "goals" to goals.positions.size,
                "goal0" to goals.positions.first(),
                "airGoals" to goals.feet.count { (pos, feet) -> feet.y == pos.y.toDouble() },
                "reuseScores" to reuseScores,
                "anchor" to anchor?.let(::fmt),
                "lead" to engagement?.target?.let { fmt(leadTicks(it)) },
                "latency" to fmt(latencyAvg),
            ),
        )
        trip.request(
            origin,
            goals.positions,
            maxLength = range * ROUTE_DETOUR,
            reuseScores = reuseScores,
            goalFeet = goals.feet,
        )
        phase = Phase.ROUTING
    }

    private fun route() {
        val engagement = liveEngagement ?: return leave("target-lost-while-routing")
        engagement.searchTicks++
        updateDestination(engagement)
        when (val result = trip.poll()) {
            RouteResult.Searching -> {
                dlog(
                    "ROUTING",
                    TpAuraPlusLog.details(
                        "searchTick" to engagement.searchTicks,
                        "playerDrift" to fmt(playerDrift(engagement)),
                        "targetDist" to fmt(targetDist(engagement)),
                    ),
                )
            }
            RouteResult.AlreadyThere -> {
                dlog("ROUTE-ALREADY-THERE")
                startAttack()
            }
            is RouteResult.Found -> {
                val route = result.value
                engagement.route = route
                engagement.waited = 0
                dlog(
                    "ROUTE-FOUND",
                    TpAuraPlusLog.details(
                        "searchTick" to engagement.searchTicks,
                        "waypoints" to route.points.size,
                        "steps" to route.steps.size,
                        "length" to fmt(route.length),
                        "destination" to fmt(route.destination),
                        "playerDrift" to fmt(playerDrift(engagement)),
                    ),
                )
                dchat("route found (${route.points.size} wp, len=${fmt(route.length)})")
                if (!manual.enabled || manual.showRoute) {
                    view.active(route.path)
                    view.attackPosition = route.destination
                }
                phase = Phase.READY
                launch()
            }
            is RouteResult.Failed -> {
                dlog("ROUTE-FAILED", TpAuraPlusLog.details("failure" to result.failure))
                dchat("route failed: ${result.failure}")
                sfx(sfx.routeFail, SFX_ROUTE_FAIL)
                fail(engagement.target, "route-" + result.failure.name.lowercase())
            }
        }
    }

    /** Sets off once the clicker is about to click when the route arrives. */
    private fun launch() {
        val engagement = liveEngagement ?: return leave("target-lost-while-ready")
        val route = engagement.route ?: return leave("no-route-on-ready")
        val arrival = if (flight == Teleport.FlightMode.PACED) {
            route.batches(steps.packetsPerTick).size
        } else {
            1
        }

        // The destination was aimed at the target for the moment of arrival; check it against the
        // target for that moment, not for a full lead from now — that counts the lead twice.
        if (!isDestinationInReach(route.destination, engagement.target, arrival + STATE_TICKS)) {
            dlog(
                "READY-DESTINATION-OUT-OF-REACH",
                TpAuraPlusLog.details("targetDist" to fmt(targetDist(engagement))),
            )
            return replan("destination-out-of-reach")
        }

        if (manual.enabled) {
            if (!manualTriggered) {
                // Armed: keep the route fresh on a schedule, and never set off on its own.
                if (ticks < manualRefreshAt) {
                    return
                }
                manualRefreshAt = ticks + manual.refreshInterval
                return manualRefresh(engagement)
            }
            // The trigger waits at home until the weapon is ready again, then goes.
            if (!attackReady()) {
                dlog("MANUAL-COOLDOWN", TpAuraPlusLog.details("progress" to fmt(cooldownProgress())))
                return
            }
        }

        val aligned = if (manual.enabled) true else attackReadyAt(arrival)
        val drift = player.position().distanceTo(route.start)

        // A tick or two may pass for the click to line up with the arrival, but not more: every tick
        // waited here is a tick the target moves out of reach. The attack phase waits at the target
        // for its click opportunity anyway.
        if (!manual.enabled && !aligned && drift < DRIFT_SOFT && engagement.waited++ < ALIGN_GRACE) {
            dlog(
                "READY-WAIT",
                TpAuraPlusLog.details(
                    "waited" to engagement.waited,
                    "arrival" to arrival,
                    "aligned" to aligned,
                    "playerDrift" to fmt(drift),
                    "targetDist" to fmt(targetDist(engagement)),
                ),
            )
            return
        }

        // The route is a couple of ticks old by now; at speed it is stale in the literal sense. That is
        // a timing problem, not a broken route: re-route from where the player is, without counting it
        // against the failures that end the engagement.
        // Compare where the server has the player, not where the client is: dispatch judges the route
        // against the tracked server position, so judging it against the client position always looks fine.
        val serverDrift = (TeleportManager.serverPosition ?: player.position()).distanceTo(route.start)
        val driftBudget = driftBudget(engagement)
        if (serverDrift > driftBudget) {
            dlog(
                "DRIFT-STALE",
                TpAuraPlusLog.details(
                    "serverDrift" to fmt(serverDrift),
                    "playerDrift" to fmt(drift),
                    "budget" to fmt(driftBudget),
                    "speed" to fmt(playerSpeed()),
                    "searchTicks" to engagement.searchTicks,
                ),
            )
            return if (engagement.staleRetries++ < MAX_STALE_RETRIES) {
                replan("drift-stale")
            } else {
                leave("drift-stale")
            }
        }

        val finish = if (afterAttack == AfterAttack.RETURN) Teleport.Finish.HOLD else Teleport.Finish.LAND
        val dispatched = trip.dispatch(route, finish, flight, driftBudget)
        if (dispatched == null) {
            dlog(
                "DISPATCH-FAILED",
                TpAuraPlusLog.details(
                    "playerDrift" to fmt(drift),
                    "routeStart" to fmt(route.start),
                    "playerPos" to fmt(player.position()),
                    "waited" to engagement.waited,
                ),
            )
            dchat("dispatch failed (drift=${fmt(drift)})")
            return if (engagement.dispatchFails++ < MAX_DISPATCH_RETRIES) {
                replan("dispatch-drift")
            } else {
                leave("dispatch-failed")
            }
        }

        dlog(
            "DISPATCH",
            TpAuraPlusLog.details(
                "finish" to finish,
                "arrival" to arrival,
                "aligned" to aligned,
                "playerDrift" to fmt(drift),
                "waited" to engagement.waited,
                "destination" to fmt(route.destination),
            ),
        )
        dchat("dispatched, arrival in ${arrival}t")
        sfx(sfx.teleport, SFX_TELEPORT)
        phase = Phase.APPROACH
    }

    private fun approach() {
        when (val state = trip.run?.state) {
            null -> leave("no-run-on-approach")
            Teleport.State.ACTIVE -> dlog("APPROACH-FLYING")
            Teleport.State.DONE -> {
                val feet = trip.feet
                val expected = trip.run?.route?.destination
                dlog("ARRIVED", TpAuraPlusLog.details("feet" to fmt(feet), "expected" to expected?.let(::fmt)))
                if (expected != null && feet.distanceToSqr(expected) > ARRIVE_ACCEPT_SQ) {
                    // The server moved the player back during the flight and the setback raced past us.
                    dlog("ARRIVED-WRONG", TpAuraPlusLog.details("feet" to fmt(feet), "expected" to fmt(expected)))
                    dchat("server moved us back mid-flight")
                    rejected()
                    return
                }
                startAttack()
                attack()
            }
            Teleport.State.REJECTED -> {
                dlog("APPROACH-REJECTED")
                dchat("server rejected the teleport")
                rejected()
            }
            Teleport.State.INVALID, Teleport.State.CANCELLED -> {
                dlog("APPROACH-BROKEN", TpAuraPlusLog.details("state" to state))
                view.invalid()
                trip.showReturn()
                phase = Phase.RETURN
            }
        }
    }

    private fun startAttack() {
        val engagement = engagement
        engagement?.run {
            if (requestTick > 0) {
                latencyAvg = latencyAvg * 0.6 + (ticks - requestTick) * 0.4
            }
            holdLeft = hold
            attacked = false
            retries = 0
            idleTicks = 0
            blockedTicks = 0
            goalAnchor = predictedBox(target).center
        }
        val feet = trip.feet
        val target = engagement?.target
        dlog(
            "START-ATTACK",
            TpAuraPlusLog.details(
                "feet" to fmt(feet),
                "target" to target?.let(::desc),
                "dist" to target?.let { fmt(targetDist(engagement)) },
                "side" to target?.let { sideOf(feet, it) },
                "cooldown" to fmt(cooldownProgress()),
            ),
        )
        phase = Phase.ATTACK
    }

    private fun attack() {
        if (trip.run?.state == Teleport.State.REJECTED) {
            dlog("ATTACK-REJECTED")
            return rejected()
        }

        val engagement = liveEngagement ?: return leave("attack-over")
        val target = engagement.target
        val feet = trip.feet
        if (!isInReach(feet, target)) {
            // The aggressive spray flies to the target on its own: give it the swing before the spot
            // is written off and the route planned again.
            val sprayable = sprays && attackReady()
            dlog(
                "ATTACK-OUT-OF-REACH",
                TpAuraPlusLog.details(
                    "retries" to engagement.retries,
                    "dist" to fmt(targetDist(engagement)),
                    "spraying" to sprayable,
                ),
            )
            if (!sprayable) {
                return if (engagement.retries++ < MAX_RETRIES) {
                    replan("target-out-of-reach")
                } else {
                    leave("retries-exhausted")
                }
            }
        }

        view.engaged = true
        val swingMode = if (attackWithoutSwing) SwingMode.HIDE_SERVER else SwingMode.DO_NOT_HIDE

        if (attackWithoutLooking) {
            // The protocol takes the attack packet without a look or a click: it goes out as soon as
            // the weapon is ready again, the cooldown pacing being the only limit that matters.
            if (attackReady()) {
                deliver(target, swingMode)
                engagement.attacked = true
                engagement.idleTicks = 0
                dlog(
                    "HIT",
                    TpAuraPlusLog.details(
                        "target" to desc(target),
                        "dist" to fmt(targetDist(engagement)),
                        "holdLeft" to engagement.holdLeft,
                    ),
                )
                if (afterAttack == AfterAttack.RETURN && --engagement.holdLeft <= 0) {
                    dlog("HOLD-EXPIRED")
                    leave("hold-expired")
                }
            } else {
                dlog("ATTACK-COOLDOWN", TpAuraPlusLog.details("progress" to fmt(cooldownProgress())))
                if (++engagement.idleTicks > ATTACK_PATIENCE) {
                    dlog("ATTACK-PATIENCE")
                    leave("attack-patience")
                }
            }
            return
        }

        val clickTick = clicker.isClickTick
        val executed = clickTick && isFacing(player.getEyePositionAt(feet), target) &&
            !player.cannotAttackWithItem(player.mainHandItem, 0)

        if (executed) {
            clicker.click {
                deliver(target, swingMode)
                engagement.attacked = true
                dlog(
                    "HIT",
                    TpAuraPlusLog.details(
                        "target" to desc(target),
                        "dist" to fmt(targetDist(engagement)),
                        "holdLeft" to engagement.holdLeft,
                    ),
                )
                true
            }

            // The hold counts swings actually taken, so a swing the gate turned away does not cost
            // the engagement its one attempt.
            engagement.idleTicks = 0
            engagement.blockedTicks = 0
            if (afterAttack == AfterAttack.RETURN && --engagement.holdLeft <= 0) {
                dlog("HOLD-EXPIRED")
                leave("hold-expired")
            }
        } else if (clickTick) {
            dlog(
                "CLICK-BLOCKED",
                TpAuraPlusLog.details(
                    "facing" to isFacing(player.getEyePositionAt(feet), target),
                    "cooldown" to player.cannotAttackWithItem(player.mainHandItem, 0),
                ),
            )
            engagement.idleTicks = 0
            if (++engagement.blockedTicks > MAX_BLOCKED) {
                dlog("FACING-STUCK")
                leave("facing-stuck")
            }
        } else {
            dlog(
                "ATTACK-IDLE",
                TpAuraPlusLog.details(
                    "dist" to fmt(targetDist(engagement)),
                    "progress" to fmt(cooldownProgress()),
                ),
            )
            if (++engagement.idleTicks > ATTACK_PATIENCE) {
                dlog("ATTACK-PATIENCE")
                leave("attack-patience")
            }
        }
    }

    /**
     * While the route is still being searched, follows the target: once its predicted position has drifted
     * from [Engagement.goalAnchor], the goals are computed again and the running search is pointed at them
     * without losing its progress.
     */
    private fun updateDestination(engagement: Engagement) {
        if (!prediction.enabled || !prediction.liveRetarget) {
            return
        }

        val anchor = engagement.goalAnchor ?: return
        val predicted = predictedBox(engagement.target).center
        val drift = predicted.distanceTo(anchor)

        // Scale the threshold with the target's speed: a fixed distance retargets a fast target on
        // every tick and the search only chases its own tail.
        val speed = trackedVelocity(engagement.target)?.length() ?: 0.0
        val threshold = max(RETARGET_DRIFT, speed * RETARGET_TICKS)
        if (drift <= threshold) {
            return
        }

        val goals = attackPositions(engagement.target, trip.feet)
        val retargeted = goals.positions.isNotEmpty() && trip.retarget(goals.positions, goals.feet)
        dlog(
            "RETARGET",
            TpAuraPlusLog.details(
                "predDrift" to fmt(drift),
                "goals" to goals.positions.size,
                "retargeted" to retargeted,
            ),
        )
        if (retargeted) {
            engagement.goalAnchor = predicted
        }
    }

    /** Routes again from where the server has the player, because the target moved. */
    private fun replan(reason: String) {
        val target = engagement?.target ?: return leave("target-lost-on-replan")
        val origin = trip.feet
        val goals = attackPositions(target, origin)
        if (goals.positions.isEmpty()) {
            dlog("REPLAN-NO-GOALS", TpAuraPlusLog.details("reason" to reason, "target" to desc(target)))
            return leave("no-goals-on-replan")
        }

        dlog(
            "REPLAN",
            TpAuraPlusLog.details("reason" to reason, "origin" to fmt(origin), "goals" to goals.positions.size),
        )
        dchat("replan: $reason")
        view.engaged = false
        requestRoute(origin, goals, reuseScores = true)
    }

    /** Ends the engagement and brings the server back to the client. */
    private fun leave(reason: String) {
        if (phase == Phase.RETURN) {
            return
        }

        if (phase == Phase.ATTACK && engagement?.attacked != true) {
            sfx(sfx.hitFail, SFX_HIT)
        }

        dlog(
            "LEAVE",
            TpAuraPlusLog.details(
                "reason" to reason,
                "phase" to phase,
                "client" to fmt(player.position()),
                "server" to fmt(trip.feet),
                "clientServerGap" to fmt(player.position().distanceTo(trip.feet)),
                "serverPin" to (TeleportManager.serverPosition?.let { fmt(it) } ?: "none"),
                "pinGap" to (TeleportManager.serverPosition?.let { fmt(player.position().distanceTo(it)) } ?: "-"),
                "originGap" to engagement?.origin?.let { fmt(player.position().distanceTo(it)) },
            ),
        )
        phase = if (trip.comeBack()) Phase.RETURN else Phase.IDLE
        engagement = null
        // The hurt event of a swing still in flight may arrive after the engagement ended; the wait
        // is kept on purpose, so the sound is not lost on the way home.
        manualTriggered = false
        manualRefreshAt = 0
    }

    private fun fail(target: LivingEntity, reason: String) {
        dlog(
            "FAIL",
            TpAuraPlusLog.details(
                "reason" to reason,
                "target" to desc(target),
                "targetY" to fmt(target.position().y),
                "targetOnGround" to target.onGround(),
            ),
        )
        unreachable.put(target.id, ticks + UNREACHABLE_TICKS)
        view.invalid()
        view.target = null
        engagement = null
        restUntil = ticks + FAIL_REST_TICKS
        phase = Phase.IDLE
    }

    /** The server set the player back, so it gets some rest before trying again. */
    private fun rejected() {
        sfx(sfx.serverSetback, SFX_SETBACK)
        dlog("REJECTED", TpAuraPlusLog.details("client" to fmt(player.position()), "server" to fmt(trip.feet)))
        reset()
        view.invalid()
        restUntil = ticks + SETBACK_REST_TICKS
    }

    private fun reset() {
        trip.reset()
        phase = Phase.IDLE
        engagement = null
        awaitingHit = null
        manualTriggered = false
        manualRefreshAt = 0
        pendingSuppress = null
        view.clear()
    }

    /** Keeps the armed route fresh without setting off: a new search from where the player stands. */
    private fun manualRefresh(engagement: Engagement) {
        val origin = trip.feet
        val goals = attackPositions(engagement.target, origin)
        if (goals.positions.isEmpty()) {
            dlog("MANUAL-REFRESH-NO-GOALS", TpAuraPlusLog.details("target" to desc(engagement.target)))
            return
        }

        dlog(
            "MANUAL-REFRESH",
            TpAuraPlusLog.details("goals" to goals.positions.size, "origin" to fmt(origin)),
        )
        requestRoute(origin, goals, reuseScores = true)
    }

    /** The player acted: arm the engagement, swallow the action itself when asked to. */
    private fun onManualTrigger() {
        if (!manual.enabled || manualTriggered) {
            return
        }
        if (phase == Phase.ATTACK || phase == Phase.APPROACH || phase == Phase.RETURN || ticks < restUntil) {
            return
        }

        val target = engagement?.target
            ?: targetSelector.targets().firstOrNull { unreachable.getOrDefault(it.id, 0) <= ticks }
            ?: return

        manualTriggered = true
        manualRefreshAt = 0
        dlog(
            "MANUAL-TRIGGER",
            TpAuraPlusLog.details("target" to desc(target), "phase" to phase),
        )
        if (manual.preventExtraAction) {
            pendingSuppress = actionBinding(manual.trigger)
        }
    }

    private fun actionBinding(trigger: Trigger) = when (trigger) {
        Trigger.LEFT_CLICK -> mc.options.keyAttack
        Trigger.RIGHT_CLICK -> mc.options.keyUse
        Trigger.JUMP -> mc.options.keyJump
        Trigger.SNEAK -> mc.options.keyShift
    }

    /** Whether no vanilla or client screen sits in the way of the player's input. */
    private fun inputFree(screen: Screen?): Boolean =
        screen == null && mc.gui.overlay() == null && !ScreenManager.isClientScreen(mc.gui.screen())

    /** Where the target will be [lead] ticks from now, the box the destination is aimed at. */
    private fun predictedBox(target: LivingEntity, lead: Double): AABB {
        if (!prediction.enabled) {
            return target.boundingBox
        }

        sampleTarget(target)
        val velocity = trackedVelocity(target) ?: return target.boundingBox
        if (velocity.lengthSqr() < MIN_PREDICT_VELOCITY_SQ) {
            return target.boundingBox
        }

        // A circling target curves away from the straight lead: rotate the velocity by half the
        // turn it makes while we travel, so the aim lands on the arc instead of its tangent.
        val omega = trackedTurn(target)
        val capped = if (abs(omega) < 1.0E-4) lead else min(lead, MAX_TURN / abs(omega))
        val turn = omega * capped * TURN_DAMP / 2.0
        val offset = rotateY(velocity, turn).scale(capped)
        return target.boundingBox.move(offset)
    }

    private fun predictedBox(target: LivingEntity): AABB = predictedBox(target, leadTicks(target))

    /**
     * Ticks of lead when aiming: the measured ticks left until the swing lands — never past the
     * setting, never past what [MAX_TURN] of turning admits. The latency is measured, not guessed:
     * every swing reports how long the engagement really took.
     */
    private fun leadTicks(target: LivingEntity): Double {
        val base = min(prediction.ticks.toDouble(), remainingEta())
        val omega = abs(trackedTurn(target))
        return if (omega < 1.0E-4) base else min(base, MAX_TURN / omega)
    }

    /** Ticks from now until the swing lands, from the rolling average of what past engagements took. */
    private fun remainingEta(): Double {
        val engagement = engagement ?: return prediction.ticks.toDouble()
        return max(1.0, latencyAvg - (ticks - engagement.requestTick))
    }

    // --- Target tracking ---
    //
    // The delta movement of a remote player is not synced — its positions come from move packets —
    // so the velocity the prediction needs is measured from the positions ourselves.

    private fun sampleTarget(target: LivingEntity) {
        val id = target.id
        val samples = targetSamples
        if (samplesId != id) {
            samples.clear()
            samplesId = id
        }
        if (samples.isEmpty() || samples.last().first != ticks) {
            samples.addLast(ticks to target.position())
            while (samples.size > MAX_SAMPLES) {
                samples.removeFirst()
            }
        }
    }

    /** Blocks per tick over the last [SPEED_WINDOW] ticks, or null when the target is too new. */
    private fun trackedVelocity(target: LivingEntity): Vec3? {
        val samples = targetSamples.takeIf { it.size >= 2 && samplesId == target.id } ?: return null
        val (endTick, endPos) = samples.last()
        val (startTick, startPos) = samples.firstOrNull { endTick - it.first >= SPEED_WINDOW } ?: samples.first()
        val elapsed = (endTick - startTick).toDouble()
        if (elapsed < 1.0) {
            return null
        }
        return endPos.subtract(startPos).scale(1.0 / elapsed)
    }

    /** Radians per tick the target turns around its up axis, from the same samples as [trackedVelocity]. */
    private fun trackedTurn(target: LivingEntity): Double {
        val samples = targetSamples.takeIf { it.size >= 3 && samplesId == target.id } ?: return 0.0
        val (endTick, endPos) = samples.last()
        val mid = samples.firstOrNull { endTick - it.first >= SPEED_WINDOW } ?: return 0.0
        val start = samples.firstOrNull { mid.first - it.first >= SPEED_WINDOW } ?: return 0.0

        val newer = endPos.subtract(mid.second)
        val older = mid.second.subtract(start.second)
        if (newer.lengthSqr() < MIN_PREDICT_VELOCITY_SQ || older.lengthSqr() < MIN_PREDICT_VELOCITY_SQ) {
            return 0.0
        }

        val angle = atan2(
            older.x * newer.z - older.z * newer.x,
            older.x * newer.x + older.z * newer.z,
        )
        return angle / (endTick - start.first).coerceAtLeast(1)
    }

    private fun rotateY(v: Vec3, angle: Double): Vec3 {
        if (abs(angle) < 1.0E-4) {
            return v
        }
        val cos = cos(angle)
        val sin = sin(angle)
        return Vec3(v.x * cos - v.z * sin, v.y, v.x * sin + v.z * cos)
    }

    private fun isInReach(feet: Vec3, target: LivingEntity) =
        target.boundingBox.distanceToSqr(player.getEyePositionAt(feet)) <= Mth.square(reach)

    /**
     * Whether the target can be hit from here without moving: in reach and not hidden. A wall a few
     * blocks away is not a reason to skip the route — it may be climbed over.
     */
    private fun isHittable(feet: Vec3, target: LivingEntity): Boolean {
        if (!isInReach(feet, target)) {
            return false
        }
        val eyes = player.getEyePositionAt(feet)
        return hasClearSight(eyes, target.boundingBox.center) || throughWall(eyes, target.boundingBox)
    }

    /**
     * Whether the destination still reaches the target at [arrivalLead] ticks from now — where it is
     * now or where it is heading by then. A jinking target may swing back in as often as out.
     */
    private fun isDestinationInReach(feet: Vec3, target: LivingEntity, arrivalLead: Double): Boolean {
        val eyes = player.getEyePositionAt(feet)
        val limit = Mth.square(reach)
        return target.boundingBox.distanceToSqr(eyes) <= limit ||
            predictedBox(target, arrivalLead).distanceToSqr(eyes) <= limit
    }

    /** Whether the rotation the server has is on where the target is heading — loosely, to swing. */
    private fun isFacing(eyes: Vec3, target: LivingEntity): Boolean {
        val box = predictedBox(target)
        if (box.contains(eyes)) {
            return true
        }

        // The exact ray rarely lands on the box of a jinking target and the smoothed rotation trails
        // it on purpose: a swing only needs the look to be roughly onto the target and unhidden.
        val toCenter = box.center.subtract(eyes)
        if (toCenter.lengthSqr() < 1.0E-6) {
            return true
        }
        return toCenter.normalize().dot(RotationManager.serverRotation.directionVector) >= FACING_DOT &&
            (hasClearSight(eyes, box.center) || throughWall(eyes, box))
    }

    /** Whether the target is close enough to be hit through blocks. */
    private fun throughWall(eyes: Vec3, box: AABB): Boolean =
        allowThroughWall && box.distanceToSqr(eyes) <= Mth.square(throughWallRange)

    /** Like [hasLineOfSight], except leaves and glass do not hide the target. */
    private fun hasClearSight(eyes: Vec3, end: Vec3): Boolean {
        val direction = end.subtract(eyes)
        val limit = direction.lengthSqr()
        val step = direction.normalize().scale(0.05)
        var start = eyes

        repeat(MAX_SIGHT_SKIPS) {
            val hit = world.clip(ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player))
            if (hit.type == HitResult.Type.MISS) {
                return true
            }
            if (world.getBlockState(hit.blockPos).canOcclude()) {
                return false
            }

            start = hit.location.add(step)
            if (start.distanceToSqr(eyes) >= limit) {
                return true
            }
        }
        return false
    }

    /** Landing spots around the predicted box of [target], hovering ones preferred when it flies. */
    private fun attackPositions(target: LivingEntity, from: Vec3): AttackGoals {
        val box = predictedBox(target)
        val terrain = Terrain.live()
        val facing = Vec3.directionFromRotation(0f, target.yBodyRot)
        val center = box.center

        // Against a target in the air, hovering next to it beats standing far below; against one on
        // the ground, a foothold stays first and hovering is only the fallback.
        val hoverFirst = airTargets && !target.onGround()
        val candidates = terrain.reachFeetPositions(
            box,
            reach - TARGET_MOVEMENT_MARGIN,
            from,
            allowAir = airTargets,
            wallsRange = if (allowThroughWall) throughWallRange.toDouble() else 0.0,
        ) { standable, feet ->
            val hoverTier = if (hoverFirst && !standable) 0 else 1
            val sideTier = sideTier(feet, center, facing)
            // Close to the target, not to the player: the far edge of the reach sphere leaves no
            // room for the target to move before the swing, and every step of error ends the attack.
            hoverTier * TIER_HOVER + sideTier * TIER_SIDE + feet.distanceToSqr(center)
        }

        val positions = candidates.map { (position, _) -> position }
        return AttackGoals(positions, candidates.associate { (position, feet) -> position to feet })
    }

    /**
     * Delivers the swing. Normally one attack packet; with the aggressive spray, attacks from
     * several spots around the target within the same tick — the server checks reach for each of
     * them in order, drops the ones out of range without spending the cooldown and takes the first
     * that connects, so the hit does not hinge on one predicted spot being exact.
     */
    private fun deliver(target: LivingEntity, swingMode: SwingMode) {
        awaitHit(target)

        if (!sprays) {
            attackEntity(target, swingMode, keepSprint = true)
            clicker.itemCooldown?.newCooldown()
            return
        }

        val spots = probeSpots(target)
        if (spots.isEmpty()) {
            attackEntity(target, swingMode, keepSprint = true)
            clicker.itemCooldown?.newCooldown()
            return
        }

        for ((_, feet) in spots) {
            sendPacketSilently(ServerboundMovePlayerPacket.Pos(feet, false, false))
            attackEntity(target, swingMode, keepSprint = true)
        }

        clicker.itemCooldown?.newCooldown()
        spots.lastOrNull()?.let { (_, feet) ->
            TeleportManager.notePosition(feet, false)

            // Nothing owns the tracked position in the in-reach engagement: no run was started, so no
            // return will ever clean it up and the server would stand on the last probe spot while the
            // player walks away. Put the server back on the client and stop rewriting its movement.
            val owned = TeleportManager.holds(this)
            if (!owned) {
                sendPacketSilently(
                    ServerboundMovePlayerPacket.Pos(player.position(), player.onGround(), false),
                )
                TeleportManager.releasePosition()
            }

            // Only the unowned case is worth a line: it is the one that has to hand the position back.
            if (!owned) {
                dlog(
                    "SPRAY-PIN",
                    TpAuraPlusLog.details(
                        "pin" to fmt(feet),
                        "client" to fmt(player.position()),
                        "gap" to fmt(player.position().distanceTo(feet)),
                        "spots" to spots.size,
                    ),
                )
            }
        }
    }

    /**
     * Spots along the path of the target, for the spray: the packets are processed a tick or two
     * after the sample, so the probes cover where the target will be by then — a chain along its
     * motion, spaced so one reach still bridges the gaps, each with an arm's height above it.
     */
    private fun probeSpots(target: LivingEntity): List<Pair<BlockPos, Vec3>> {
        val terrain = Terrain.live()
        val speed = trackedVelocity(target)?.length() ?: 0.0

        // How long until the packets are processed: the base window plus half the ping.
        val window = PROBE_TICKS + min(player.ping / 2.0 / 50.0, PROBE_PING_CAP)
        val steps = if (speed * speed < MIN_PREDICT_VELOCITY_SQ) {
            1
        } else {
            min(CHAIN_MAX, max(1, ceil(speed * window / CHAIN_STEP).toInt()))
        }

        val spots = ArrayList<Pair<BlockPos, Vec3>>(MAX_PROBES)
        for (step in 0..steps) {
            val point = predictedBox(target, window * step / steps).center
            addProbe(spots, terrain, point)
            addProbe(spots, terrain, point.add(0.0, 1.0, 0.0))
        }
        return spots.distinctBy { it.first }.take(MAX_PROBES)
    }

    private fun addProbe(spots: MutableList<Pair<BlockPos, Vec3>>, terrain: Terrain, point: Vec3) {
        val block = BlockPos.containing(point)
        val feet = terrain.feet(block)
            ?: if (terrain.hasRoom(block.x, block.y, block.z)) {
                Vec3(block.x + 0.5, block.y.toDouble(), block.z + 0.5)
            } else {
                return
            }
        spots += block to feet
    }

    private fun sideTier(feet: Vec3, center: Vec3, facing: Vec3): Int {
        if (landingSide == LandingSide.ANY) {
            return 0
        }

        val toFeet = Vec3(feet.x - center.x, 0.0, feet.z - center.z)
        if (toFeet.lengthSqr() < 1.0E-6) {
            return 1
        }

        val dot = toFeet.normalize().dot(facing)
        val behind = dot < -SIDE_DOT
        val front = dot > SIDE_DOT
        return when (landingSide) {
            LandingSide.BEHIND -> when {
                behind -> 0
                front -> 2
                else -> 1
            }
            LandingSide.FLANKS -> when {
                behind -> 1
                front -> 2
                else -> 0
            }
            LandingSide.ANY -> 0
        }
    }

    // --- Readiness and debug helpers ---

    /** Whether a click with the attack cooldown passed is expected [tick] ticks from now. */
    /** Whether a swing could land [tick] ticks from now: the weapon's cooldown passed, and a click if one is needed. */
    private fun attackReadyAt(tick: Int): Boolean =
        (attackWithoutLooking || clicker.getClickAmount(tick) > 0) &&
            clicker.itemCooldown?.isCooldownPassed(tick) != false

    /**
     * The first tick from now an attack could land, or null when the weapon is not ready by the time the
     * route would arrive — the engagement then waits at its start instead of standing next to the target
     * for the rest of a long cooldown.
     */
    private fun readyClickTick(): Int? = (1..LAUNCH_WINDOW).firstOrNull(::attackReadyAt)

    private fun cooldownProgress(): Double = clicker.itemCooldown?.cooldownProgress()?.toDouble() ?: 1.0

    /** Whether a swing could go out right now: no miss cooldown, the weapon's cooldown passed and usable. */
    private fun attackReady(): Boolean =
        !isMissCooldownActive && clicker.itemCooldown?.isCooldownPassed() != false &&
            !player.cannotAttackWithItem(player.mainHandItem, 0)

    /** Arms the confirmation for a swing that is going out now; the hurt event answers it. */
    private fun awaitHit(target: LivingEntity) {
        awaitingHit = target
        hitWaitUntil = ticks + hitConfirmWindow()
    }

    /** The player's own speed, which is what ages a route in the ticks a search takes. */
    private fun playerSpeed(): Double = player.deltaMovement.length()

    /**
     * How far the player may have moved before a route counts as stale: the engine's own allowance,
     * widened by what the player covers while a search runs. Standing still, nothing changes.
     */
    private fun driftBudget(engagement: Engagement): Double =
        maxOf(
            DRIFT_FLOOR,
            TeleportManager.MAX_DRIFT + playerSpeed() * (engagement.searchTicks + 1),
        ).coerceAtMost(DRIFT_CAP)

    /** Ticks a hurt event may take: one for the round trip plus the ping. Kept short on purpose. */
    private fun hitConfirmWindow(): Int = (1 + player.ping / 50).coerceIn(2, 6)

    /**
     * Confirms the swing with the server's own word: a hurt event for the target just after it says the
     * damage was accepted. That event carries no attacker, so the window stays short — a team fight or a
     * burning target can answer it for someone else.
     */
    @Suppress("unused")
    private val hitConfirmHandler = handler<PacketEvent> { event ->
        val waiting = awaitingHit ?: return@handler

        val hurtId = when (val packet = event.packet) {
            // What onlookers get: the damage itself, with the entity that caused it. The hurt animation
            // packet only ever goes to the player who was hurt, so it cannot confirm our own swing.
            is ClientboundDamageEventPacket -> {
                // Ours, or — on servers reached through a proxy — a packet the proxy synthesised from
                // the old hurt event, which carries no attacker: both ids are -1.
                val ours = packet.sourceCauseId() == player.id || packet.sourceDirectId() == player.id
                val unattributed = packet.sourceCauseId() <= 0 && packet.sourceDirectId() <= 0
                if (packet.entityId() == waiting.id && (ours || unattributed)) packet.entityId() else null
            }
            // Older servers, translated by ViaFabricPlus, report a hurt through the entity event.
            is ClientboundEntityEventPacket -> if (packet.eventId == ENTITY_EVENT_HURT) {
                packet.getEntity(world)?.id
            } else {
                null
            }
            is ClientboundHurtAnimationPacket -> packet.id()
            else -> null
        }

        if (hurtId != waiting.id) {
            return@handler
        }

        awaitingHit = null
        dlog("HIT-CONFIRMED", TpAuraPlusLog.details("target" to desc(waiting)))
        sfx(sfx.hitSuccess, SFX_HIT)
    }

    /** Plays the hint of [event] unless a more important one already played this tick. */
    private fun sfx(event: SoundValueGroup, priority: Int) {
        if (!sfx.enabled || !event.enabled) {
            return
        }
        if (sfxTick != ticks) {
            sfxTick = ticks
            sfxPriority = 0
        }
        if (priority < sfxPriority) {
            return
        }
        sfxPriority = priority
        SoundValueGroup.play(event.soundValue.get(), sfx.volume / 100f)
    }

    private fun dlog(event: String, details: String = "") {
        if (debug) {
            TpAuraPlusLog.log(ticks, event, details)
        }
    }

    private fun dchat(text: String) {
        if (debug) {
            chat("[TpAuraPlus] $text", this)
        }
    }

    private fun playerDrift(engagement: Engagement): Double =
        engagement.route?.start?.let { player.position().distanceTo(it) }
            ?: engagement.origin?.let { player.position().distanceTo(it) }
            ?: 0.0

    /** Distance from where the server has the player to the target's box, same metric as [isInReach]. */
    private fun targetDist(engagement: Engagement): Double =
        sqrt(engagement.target.boundingBox.distanceToSqr(player.getEyePositionAt(trip.feet)))

    /** front/side/behind relative to where the target's body faces. */
    private fun sideOf(feet: Vec3, target: LivingEntity): String {
        val facing = Vec3.directionFromRotation(0f, target.yBodyRot)
        val toFeet = feet.subtract(target.position())
        val flat = Vec3(toFeet.x, 0.0, toFeet.z)
        if (flat.lengthSqr() < 1.0E-6) {
            return "inside"
        }
        val dot = flat.normalize().dot(facing)
        return when {
            dot < -SIDE_DOT -> "behind"
            dot > SIDE_DOT -> "front"
            else -> "side"
        }
    }

    private fun desc(target: LivingEntity): String =
        "#${target.id}:${target.name.string}"

    private fun fmt(value: Double): String = "%.2f".format(value)

    private fun fmt(value: Vec3): String =
        "(%.2f,%.2f,%.2f)".format(value.x, value.y, value.z)

    private const val REACH_MARGIN = 0.1
    private const val TARGET_MOVEMENT_MARGIN = 0.4
    private const val ROUTE_DETOUR = 2.0

    /** Ticks the click may delay the dispatch before the flight leaves without it. */
    private const val ALIGN_GRACE = 2
    private const val MAX_RETRIES = 2
    private const val MAX_DISPATCH_RETRIES = 2

    /** Stale routes that may be re-planned before the engagement is given up. */
    private const val MAX_STALE_RETRIES = 8

    /** How far a route may go stale at walking speed; the engine's own allowance is tighter. */
    private const val DRIFT_FLOOR = 6.0

    /** Ceiling of the adaptive drift allowance, in blocks. */
    private const val DRIFT_CAP = 24.0
    private const val FAIL_REST_TICKS = 10
    private const val SETBACK_REST_TICKS = 20
    private const val UNREACHABLE_TICKS = 40
    private const val MIN_PREDICT_VELOCITY_SQ = 1.0E-4
    private const val SIDE_DOT = 0.5

    /** The destination retargets once the predicted position drifted this far, or [RETARGET_TICKS] of travel. */
    private const val RETARGET_DRIFT = 0.5
    private const val RETARGET_TICKS = 1.0

    /** Ticks the velocity is measured over, and how much history is kept for the turn. */
    private const val SPEED_WINDOW = 3
    private const val MAX_SAMPLES = 8

    /** The aggressive spray: a chain of probe spots along the target's path, its height included. */
    private const val PROBE_TICKS = 2.0
    private const val PROBE_PING_CAP = 2.0
    private const val CHAIN_STEP = 2.5
    private const val CHAIN_MAX = 4
    private const val MAX_PROBES = 8

    /** `LivingEntity` broadcasts this entity event on every accepted damage: the red flash and hurt time. */
    private const val ENTITY_EVENT_HURT: Byte = 2

    /** Sound priorities within one tick: the highest one keeps it, the lower ones stay silent. */
    private const val SFX_PATH = 1
    private const val SFX_TELEPORT = 2
    private const val SFX_ROUTE_FAIL = 2
    private const val SFX_HIT = 3
    private const val SFX_SETBACK = 4

    /** ATTACK ticks without a click opportunity before giving up on the click plan. */
    private const val ATTACK_PATIENCE = 40

    /** How far the server may end from the destination before the flight counts as set back. */
    private const val ARRIVE_ACCEPT_SQ = 1.0

    /** How far the look may trail the target before a swing stops counting as aimed (cos ~49°). */
    private const val FACING_DOT = 0.65

    /** Click ticks the gate may turn away before the engagement gives up on its aim. */
    private const val MAX_BLOCKED = 8

    /** How much of the measured turn is applied to the lead — most targets do not circle cleanly. */
    private const val TURN_DAMP = 0.9

    /** The lead never follows more than this much turning; beyond it the extrapolation lies. */
    private const val MAX_TURN = 0.7

    /** Ticks between the flight ending and the swing going out. */
    private const val STATE_TICKS = 1.0

    /** Foliage blocks a sight check may pass through before a real wall counts. */
    private const val MAX_SIGHT_SKIPS = 3

    /** How far the player may walk off the route before the click alignment gives up and flies. */
    private const val DRIFT_SOFT = 1.0

    /**
     * Ticks from starting an engagement to the swing landing: the search, the flight and the state
     * machine. Starting earlier only means waiting next to the target for the rest of the cooldown.
     */
    private const val LAUNCH_WINDOW = 5

    /** Sort weights: hovering tier over side tier over distance, without the bands overlapping. */
    private const val TIER_HOVER = 1_000_000_000_000.0
    private const val TIER_SIDE = 1_000_000.0

}
