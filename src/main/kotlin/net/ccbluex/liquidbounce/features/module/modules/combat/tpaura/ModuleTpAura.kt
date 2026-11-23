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
import net.ccbluex.agent.math.AABB as AgentAABB
import net.ccbluex.agent.math.BlockPos
import net.ccbluex.agent.navigation.Listener
import net.ccbluex.agent.navigation.Progress
import net.ccbluex.agent.navigation.teleport.Options
import net.ccbluex.agent.navigation.teleport.Pacing
import net.ccbluex.agent.navigation.teleport.Teleport
import net.ccbluex.agent.world.Mechanics
import net.ccbluex.agent.pathfinder.route.Outcome
import net.ccbluex.agent.pathfinder.teleport.Route
import net.ccbluex.agent.pathfinder.terrain.DestinationCheck
import net.ccbluex.agent.pathfinder.terrain.Terrain
import net.ccbluex.agent.pathfinder.terrain.reachPositions
import net.ccbluex.agent.v26_3.live
import net.ccbluex.agent.v26_3.toAgent
import net.ccbluex.agent.v26_3.toMinecraft
import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.config.types.group.ValueGroup
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.KeybindIsPressedEvent
import net.ccbluex.liquidbounce.event.events.KeyboardKeyEvent
import net.ccbluex.liquidbounce.event.events.MouseButtonEvent
import net.ccbluex.liquidbounce.event.events.OverlayRenderEvent
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.render.drawBox
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.renderEnvironment
import net.ccbluex.liquidbounce.render.withPositionRelativeToCamera
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.RotationUpdateEvent
import net.ccbluex.liquidbounce.event.events.TransferOrigin
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.global.GlobalSettingsTeleport
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsValueGroup
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.aiming.utils.raytraceBox
import net.ccbluex.liquidbounce.utils.block.SwingMode
import net.ccbluex.liquidbounce.utils.clicking.Clicker
import net.ccbluex.liquidbounce.utils.combat.TargetPriority
import net.ccbluex.liquidbounce.utils.combat.TargetSelector
import net.ccbluex.liquidbounce.utils.combat.attackEntity
import net.ccbluex.liquidbounce.utils.entity.getEyePositionAt
import net.ccbluex.liquidbounce.utils.entity.ping
import net.ccbluex.liquidbounce.utils.network.sendPacketSilently
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.math.fma
import net.ccbluex.liquidbounce.utils.navigation.teleport.StepsValueGroup
import net.ccbluex.liquidbounce.utils.navigation.teleport.TeleportManager
import net.ccbluex.liquidbounce.utils.raytracing.hasLineOfSight
import net.ccbluex.liquidbounce.utils.render.route.RouteEvents
import net.ccbluex.liquidbounce.utils.render.route.RouteRenderer
import net.ccbluex.liquidbounce.utils.render.route.RouteView
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket
import net.minecraft.network.protocol.game.ClientboundHurtAnimationPacket
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.sounds.SoundSource
import net.minecraft.client.KeyMapping
import net.minecraft.util.Mth
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.sqrt
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import kotlin.math.ceil
import kotlin.math.sqrt
import kotlin.math.max
import kotlin.math.min
import net.ccbluex.agent.math.Vec3 as AgentVec3

/**
 * TpAura
 *
 * Teleports next to enemies, attacks them, then returns or stays.
 */
@Suppress("TooManyFunctions")
object ModuleTpAura : ClientModule("TpAura", ModuleCategories.COMBAT, disableOnQuit = true) {

    private val rangeValue = float("Range", 30f, 6f..100f, "blocks")
    private val range by rangeValue
    private val afterAttack by enumChoice("AfterAttack", AfterAttack.RETURN)
    private val hold by int("Hold", 1, 1..10, "swings")

    /**
     * Attacks through a wall: a target the eyes cannot see is hit when the distance is inside [ThroughWall.range].
     * Off, a target without line of sight is not attacked at all — the module routes to a spot that has one.
     */
    private val throughWall = tree(object : ToggleableValueGroup(this@ModuleTpAura, "ThroughWall", false) {
        val range by float("Range", 2f, 0f..3f, "blocks")
    })

    /** Hides the swing from the server only: the client still animates, no punch packet is sent. */
    private val noSwing by boolean("NoSwing", false, aliases = listOf("AttackWithoutSwing"))

    private val swingMode: SwingMode
        get() = if (noSwing) SwingMode.HIDE_SERVER else SwingMode.DO_NOT_HIDE

    /** Which side of the target the landing spot prefers. */
    private val landingSide by enumChoice("LandingSide", LandingSide.ANY)

    private enum class LandingSide(override val tag: String) : Tagged {
        ANY("Any"),
        BEHIND("Behind"),
        FLANKS("Flanks"),
    }

    private val hud = tree(HudValueGroup())

    private val manual = tree(ManualValueGroup())

    /** Armed but holding: the route is planned and drawn, nothing leaves until the player acts. */
    private class ManualValueGroup : ValueGroup("AttackManually", aliases = listOf("ManualAttack")) {
        val enabled by boolean("Enabled", false)
        val trigger by enumChoice("Trigger", ManualTrigger.LEFT_CLICK, aliases = listOf("AttackWhen"))
        val preventExtraAction by boolean("PreventExtraAction", true)
        val showRoute by boolean("ShowRoute", true, aliases = listOf("ShowPath"))
        val refreshInterval by int("RefreshInterval", 8, 1..20, "ticks", aliases = listOf("RerouteInterval"))
    }

    private enum class ManualTrigger(override val tag: String) : Tagged {
        LEFT_CLICK("LeftClick"),
        RIGHT_CLICK("RightClick"),
        JUMP("Jump"),
        SNEAK("Sneak"),
    }

    private val prediction = tree(PredictionValueGroup())

    /** Ticks a swing waits for a hurt before it counts as a miss. */
    private val hitConfirm by int("HitConfirm", 10, 1..40, "ticks")

    private val targetSelector = tree(TargetSelector(TargetPriority.DISTANCE, rangeValue))
    private val clicker = tree(Clicker(this, mc.options.keyAttack))
    private val rotations = tree(RotationsValueGroup(this, combatSpecific = true))
    private val steps = tree(StepsValueGroup())
    private val debug = tree(DebugValueGroup())

    /**
     * Sends the attack packet straight instead of going through the click plan: no click schedule and no
     * facing check stand between the arrival and the swing. Ported from TpAuraPlus, where the clicker
     * gate was the difference between "arrives and hits" and "arrives and leaves".
     */
    private val attackWithoutLooking by boolean("AttackWithoutLooking", true)

    /** Also tries spots in the air around the target, for targets with no standable spot within reach. */
    private val airTargets by targetSelector.boolean("AirTargets", false)

    /**
     * Aiming at where the target will be when the route arrives, as the module this grew out of did: a switch,
     * then a ceiling for the *measured* flight. The network round trip is added on top of that ceiling, so the
     * effective lead may exceed it by one ping.
     */
    private class PredictionValueGroup : ValueGroup("Prediction") {
        val enabled by boolean("Enabled", true)

        /** Ceiling for the measured flight; the ping round trip is added on top and is not capped. */
        val maxLead by int("MaxLead", 5, 1..10, "ticks")


        /**
         * Covers several future positions in one swing: against a fast flyer a single predicted spot is off by
         * more than the server's reach, and probing them one tick at a time would arrive after it left.
         */
        val spray by boolean("Spray", false, aliases = listOf("Aggressive"))

        /** How many probes one round may send; the chain asks for more than it uses. */
        val maxProbes by int("MaxProbes", 8, 2..10, "probes")

        /** Draws the probes of the last round: the target's predicted box and the spot tried for it. */
        val previewProbes by boolean("PreviewProbes", true)

        /** Follows the target while the route is still being searched, at most once every few ticks. */
        val liveRetarget by boolean("LiveRetarget", true)
    }

    private val sfx = tree(SfxValueGroup())

    /** One hint per stage, each with its own switch and sound, played at most once a tick. */
    private class SfxValueGroup : ValueGroup("SoundHints", aliases = listOf("SFXTips")) {
        val enabled by boolean("Enabled", true)
        val volume by float("Volume", 60f, 0f..100f, "%")

        val pathFinding = tree(hint("PathFinding", "OnPathFinding", false, "block.enchantment_table.use"))
        val teleport = tree(hint("Teleport", "OnTeleport", false, "entity.enderman.teleport"))
        val hitSuccess = tree(hint("HitSuccess", "OnHitSuccess", false, "entity.arrow.hit_player"))
        val hitFail = tree(hint("HitFail", "OnHitFail", true, "entity.villager.trade"))
        val serverSetback = tree(hint("ServerSetback", "OnServerSetback", true, "entity.villager.no"))
        val routeFail = tree(hint("RouteFail", "OnRouteFail", true, "block.note_block.bass"))

        /** One hint, named after the stage, keeping the old `On...` name for saved configs. */
        private fun hint(name: String, legacy: String, enabled: Boolean, sound: String) =
            SoundValueGroup(name, enabled, sound, aliases = listOf(legacy)) { volume }
    }

    /** A stage hint: a switch, a sound identifier, and a preview when the identifier changes. */
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

    /** Diagnostics only: every stage is written to `logs/tpaura.log` so a failed engagement can be read back. */    private class DebugValueGroup : ValueGroup("Debug") {
        val log by boolean("Log", true)

        /**
         * A\* requests per route: more than one splits the search into that many legs, each with its own
         * expansion budget, which is the only way to widen the library's fixed 8000-node limit.
         */
        val segments by int("Segments", 1, 1..3, "requests")

        /**
         * Lets the module plan its own way home when the library's `undo` gives up. Off by default: the
         * library path is the behaviour everything else was measured with.
         */
        val goBack by boolean("IReallyWannaGoBack", false)

        /** Skips the library path entirely, so the advanced return can be tested on its own. */
        val forceGoBack by boolean("ForceGoBack", false)

        /** How many short re-teleports a hold may spend on a target that moved out of reach. */
        val holdRepins by int("HoldRepins", 2, 0..5, "repins")

        /**
         * How far the player may walk before a planned route is re-planned instead of dispatched anyway. The
         * library refuses a route that starts more than two blocks from the server, so this stays below it.
         */
        val driftBudget by float("DriftBudget", 1.8f, 0.5f..1.9f, "blocks")

        /**
         * Ticks the probe chain covers, from the start of the chain onwards. Wider covers more of a fast target's
         * path at the cost of a sparser chain for the same probe budget.
         */
        val probeSpan by int("ProbeSpan", 4, 2..8, "ticks")

        /**
         * Shifts the whole probe chain forward by that many ticks. Measured against a fast target, the probes
         * at zero and one tick were the ones that never landed; one skips them.
         */
        val probeLeadStart by int("ProbeLeadStart", 0, 0..3, "ticks")
    }

    /** Which leg of the advanced return is running. */
    private enum class ReturnLeg { NONE, TO_RETREAT, TO_HOME }

    private val view = RouteView().apply {
        ghost = { partialTicks ->
            TeleportManager.teleports.serverPosition(TeleportManager.owner(this@ModuleTpAura), partialTicks)
                ?.toMinecraft()
        }
    }
    private val trip = TeleportManager.trip(this, steps, PreviewListener(RouteEvents(view)))

    /**
     * Forwards the route events to the view and keeps the search preview: a later search leg starts where the
     * terrain actually lets the route through, not where a straight line would put it.
     */
    private class PreviewListener(private val downstream: Listener) : Listener {
        override fun routeSearching(start: AgentVec3, goal: AgentVec3, radius: Double, preview: List<AgentVec3>?) {
            downstream.routeSearching(start, goal, radius, preview)
            if (preview != null) {
                engagement?.preview = preview
            }
        }

        override fun routeFollowing(points: List<AgentVec3>, progress: Progress) =
            downstream.routeFollowing(points, progress)

        override fun blocksAhead(breaks: Collection<BlockPos>, places: Collection<BlockPos>) =
            downstream.blocksAhead(breaks, places)

        override fun routeDone() = downstream.routeDone()

        override fun routeInvalid() = downstream.routeInvalid()

        override fun routeCleared() = downstream.routeCleared()
    }

    init {
        tree(RouteRenderer(this, view))
    }

    private enum class AfterAttack(override val tag: String) : Tagged {
        RETURN("Return"),
        STAY("Stay"),
    }

    private enum class Phase { IDLE, ROUTING, READY, APPROACH, ATTACK, RETURN }

    private class Engagement(val target: LivingEntity) {
        var route: Route? = null
        var waited = 0

        /** Swings this engagement still owes; set once when it is created, not on every arrival. */
        var holdLeft = 0
        var retries = 0

        /** When the search last followed the target, and how often it did. */
        var retargetTick = 0
        var retargets = 0

        /** Where the planned goals were built from, to notice the target has moved on. */
        var planAnchor: AgentVec3? = null

        /** Repins spent: short teleports towards a target that moved, without a new route. */
        var repins = 0
        var attacked = false

        /** Ticks spent in ATTACK without a swing, to give up when the weapon or the target never allows one. */
        var idleTicks = 0

        /** The goals of the engagement, kept so a later search leg can aim at the same ones. */
        var goals: List<BlockPos> = emptyList()

        /** Corners of the legs found so far, for the label alone. */
        val points = ArrayList<AgentVec3>()

        /** Steps of the legs found so far: they are swept and short, so they are kept as they are. */
        val steps = ArrayList<Route.Step>()

        /** Where the whole route begins, so the concatenated points line up with the steps. */
        var start: AgentVec3? = null

        /** Search legs still to run; one unless `Debug → Segments` asks for more. */
        var segmentsLeft = 1

        /** The running search's preview, to pick a relay the terrain actually lets a route reach. */
        var preview: List<AgentVec3>? = null

        /** How far along the line the next relay sits, shrunk after a leg fails. */
        var legScale = 1.0

        /** Legs already retried with a nearer relay, so the backoff cannot loop. */
        var legAttempts = 0

        /** Where the route was requested from, to notice a client that ended up somewhere else. */
        var origin: AgentVec3? = null

        /** How the route was built, for the diagnostics. */
        var shape: String = "agent"
    }


    /** The swing that landed: a hurt for the target inside the window opened when it was sent. */
    @Suppress("unused")
    private val hurtHandler = handler<PacketEvent>(priority = EventPriorityConvention.READ_FINAL_STATE) { event ->
        if (event.origin != TransferOrigin.INCOMING) {
            return@handler
        }

        val packet = event.packet
        val hurtId = when (packet) {
            is ClientboundDamageEventPacket -> packet.entityId
            // Older servers, translated by ViaFabricPlus, report a hurt through the entity event.
            is ClientboundEntityEventPacket -> if (packet.eventId.toInt() == ENTITY_EVENT_HURT) {
                packet.getEntity(world)?.id
            } else {
                null
            }
            is ClientboundHurtAnimationPacket -> packet.id()
            else -> null
        }

        // Only the target matters here: logging every hurt in the world turns the file into megabytes of noise,
        // and the interesting case is a packet that arrives when nothing is awaited.
        val watching = awaitingHit ?: engagement?.target
        val relevant = hurtId != null && hurtId == watching?.id
        if (relevant) {
            dlog(
                "DMG",
                TpAuraLog.details(
                    "packet" to packet.javaClass.simpleName,
                    "entity" to hurtId,
                    "awaiting" to (awaitingHit?.id ?: "none"),
                    "mine" to hitFromUs(packet),
                    "tick" to ticks,
                ),
            )
        }

        val waiting = awaitingHit ?: return@handler
        if (hurtId == null || hurtId != waiting.id) {
            return@handler
        }

        awaitingHit = null
        hits++
        TpAuraHud.onResult(true)
        sfx(sfx.hitSuccess, SFX_HIT_OK)
        dlog(
            "HIT-CONFIRMED",
            TpAuraLog.details(
                "target" to desc(waiting),
                "dist" to fmt(targetDist(waiting)),
                "lead" to fmt(leadTicks()),
                "rate" to hitRate(),
            ),
        )
    }

    /**
     * Whether the hurt came from this player. A proxied server may send no attacker at all, which is taken as
     * ours: the window is short enough that a stray hurt of the target in it is far more likely to be ours.
     */
    private fun hitFromUs(packet: Any): Boolean {
        if (packet !is ClientboundDamageEventPacket) {
            return true
        }
        return packet.sourceCauseId() == player.id || packet.sourceDirectId() == player.id ||
            (packet.sourceCauseId() <= 0 && packet.sourceDirectId() <= 0)
    }

    /** The server correcting the movement: how many packets had gone out, and where it put the player. */
    @Suppress("unused")
    private val setbackHandler = handler<PacketEvent>(priority = EventPriorityConvention.FIRST_PRIORITY) { event ->
        if (event.origin != TransferOrigin.INCOMING) {
            return@handler
        }
        if (event.packet !is ClientboundPlayerPositionPacket || trip.run == null) {
            return@handler
        }
        sfx(sfx.serverSetback, SFX_SETBACK)
        dlog(
            "SETBACK",
            TpAuraLog.details(
                "lastSent" to (TeleportManager.teleports.serverPosition?.let { fmt(it) } ?: "none"),
                "server" to fmt(event.packet.change.position),
                "client" to fmt(player.position()),
                "state" to (trip.run?.state ?: "none"),
            ),
        )
    }

    /** One-shot flag: the HURT-WAIT line is written once per wait, not on every tick of it. */
    private var hurtWaiting = false

    private var phase = Phase.IDLE
    private var engagement: Engagement? = null
    private var ticks = 0
    private var restUntil = 0

    /** Targets without a route, by entity id, until the tick they may be tried again. */
    private val unreachable = Int2IntOpenHashMap()

    /** Deepest fall inside one tick of the last dispatch. */
    private var lastMaxDrop = 0.0

    /**
     * Positions of one target over the last ticks. A remote entity reports no velocity, so the only usable
     * number is how far it moved between two ticks.
     */
    private class TargetTrack {
        private val samples = ArrayDeque<Pair<Int, AgentVec3>>()
        private var id = -1

        fun sample(entity: LivingEntity, tick: Int) {
            if (id != entity.id) {
                id = entity.id
                samples.clear()
            }
            if (samples.isEmpty() || samples.last().first != tick) {
                samples.addLast(tick to entity.position().toAgent())
                while (samples.size > MAX_SAMPLES) {
                    samples.removeFirst()
                }
            }
        }

        /** How many samples are held; the velocity needs at least two. */
        val size: Int
            get() = samples.size

        /** The trailing run of samples taken on consecutive ticks; a gap would turn an arc into a chord. */
        private fun run(): List<Pair<Int, AgentVec3>> {
            val list = samples.toList()
            if (list.isEmpty()) {
                return emptyList()
            }
            var start = list.size - 1
            while (start > 0 && list[start].first - list[start - 1].first == 1) {
                start--
            }
            return list.subList(start, list.size)
        }

        /** Blocks per tick over the trailing run, or null when the target is too new to tell. */
        fun velocity(): AgentVec3? {
            val run = run()
            if (run.size < 2) {
                return null
            }
            // Right after a corner the window still holds the old direction; the newest step is the truth.
            if (run.size >= 3 && isSharpCorner(run)) {
                return run[run.size - 1].second.subtract(run[run.size - 2].second)
            }
            val last = run[run.size - 1]
            val start = run.firstOrNull { last.first - it.first >= SPEED_WINDOW } ?: run.first()
            val elapsed = (last.first - start.first).toDouble()
            if (elapsed < 1.0) {
                return null
            }
            return last.second.subtract(start.second).scale(1.0 / elapsed)
        }

        /** Radians the target turned over the last tick, from its last two steps. */
        fun turn(): Double {
            val run = run()
            if (run.size < 3) {
                return 0.0
            }
            val newer = run[run.size - 1].second.subtract(run[run.size - 2].second)
            val older = run[run.size - 2].second.subtract(run[run.size - 3].second)
            if (newer.lengthSqr() < MIN_PREDICT_VELOCITY_SQ || older.lengthSqr() < MIN_PREDICT_VELOCITY_SQ) {
                return 0.0
            }
            return atan2(
                older.x * newer.z - older.z * newer.x,
                older.x * newer.x + older.z * newer.z,
            )
        }

        private fun isSharpCorner(run: List<Pair<Int, AgentVec3>>): Boolean {
            val last = run[run.size - 1].second.subtract(run[run.size - 2].second)
            val before = run[run.size - 2].second.subtract(run[run.size - 3].second)
            if (last.lengthSqr() < MIN_PREDICT_VELOCITY_SQ || before.lengthSqr() < MIN_PREDICT_VELOCITY_SQ) {
                return false
            }
            val angle = atan2(
                before.x * last.z - before.z * last.x,
                before.x * last.x + before.z * last.z,
            )
            return abs(angle) > SHARP_TURN
        }
    }

    /** The target being aimed at, sampled every tick so the lead has a velocity to work with. */
    private var aimTarget: LivingEntity? = null
    private val aimTrack = TargetTrack()

    /** Manual mode: armed once the player acts, and the action swallowed once when asked to. */
    private var manualTriggered = false
    private var manualRefreshAt = 0
    private var suppressBinding: KeyMapping? = null
    private var suppressUntil = -1

    /** The hint that already played this tick, so only the most important one is heard. */
    private var sfxTick = -1
    private var sfxPriority = 0

    /** The swing waiting for a hurt packet, and the tick it stops waiting. */
    private var awaitingHit: LivingEntity? = null
    private var hitWaitUntil = 0
    private var hits = 0
    private var misses = 0

    /** Last side tier the goal search settled for, so the fallback is logged once per change. */
    private var lastSideTier = -1

    /** The probes of the last spray round, kept a few ticks so they can be seen. */
    private var lastProbeBox: AgentAABB? = null
    private var lastProbeSpots: List<AgentVec3> = emptyList()
    private var probesUntil = 0

    /** Corners of the route last dispatched, to find the way home along it. */
    private val lastOutbound = ArrayList<AgentVec3>()

    /** When the running search was asked for, and how long it has been running. */
    private var searchStartedAt = 0L
    private var searchStartedTick = 0

    /** Rolling average of the ticks from requesting a route to the swing, used as the aim lead. */
    private var flightAvg = MIN_LEAD

    /** The advanced return: which leg, where it ends, and whether its search is still running. */
    private var returnLeg = ReturnLeg.NONE
    private var returnGoal: AgentVec3? = null
    private var returnSearching = false
    private var returnStartedAt = 0L

    private val reach: Double
        get() = player.entityInteractionRange() - REACH_MARGIN

    /**
     * The engagement while its target is still one. The hurt-time filter is skipped on purpose: a target
     * that was just hit is still the target, otherwise the fight ends one tick after the first swing and
     * `Hold` can never count past one. When a swing may go out is decided in [attack].
     */
    private val liveEngagement: Engagement?
        get() = engagement?.takeIf { targetSelector.validate(it.target, checkHurtTime = false) }

    override fun onEnabled() {
        TpAuraLog.open()
        dlog(
            "ENABLE",
            TpAuraLog.details(
                "range" to fmt(range),
                "afterAttack" to afterAttack,
                "hold" to hold,
                "reach" to fmt(reach),
                "throughWall" to throughWall.enabled,
                "prediction" to prediction.enabled,
                "spray" to prediction.spray,
                "maxProbes" to prediction.maxProbes,
                "landingSide" to landingSide,
                "liveRetarget" to prediction.liveRetarget,
                "probeLeadStart" to debug.probeLeadStart,
                "log" to debug.log,
            ),
        )
    }

    override fun onDisabled() {
        dlog("DISABLE", TpAuraLog.details("phase" to phase))
        TpAuraHud.reset()
        TeleportManager.teleports.undo(TeleportManager.owner(this))
        reset()
        restUntil = 0
        unreachable.clear()
        TpAuraLog.close()
    }

    @Suppress("unused")
    private val worldChangeHandler = handler<WorldChangeEvent> {
        reset()
        unreachable.clear()
    }

    @Suppress("unused")
    private val tickHandler = handler<GameTickEvent> {
        ticks++
        if (player.isDeadOrDying || player.isSpectator || player.isPassenger) {
            leave("player-gone")
            return@handler
        }

        TpAuraHud.phase = phase.name
        TpAuraHud.target = engagement?.target?.name?.string
        TpAuraHud.distance = engagement?.let { targetDist(it.target) } ?: 0.0
        engagement?.target?.let {
            TpAuraHud.targetHealth = it.health.toDouble()
            TpAuraHud.targetMaxHealth = it.maxHealth.toDouble()
        }
        TpAuraHud.lead = if (prediction.enabled) leadTicks() + player.ping / 50.0 else 0.0
        TpAuraHud.reqAttack = flightAvg
        TpAuraHud.waypoints = engagement?.route?.points?.size?.minus(1) ?: 0
        TpAuraHud.routeLength = engagement?.route?.length ?: 0.0
        TpAuraHud.searchTicks = ticks - searchStartedTick
        TpAuraHud.drift = engagement?.route?.start?.distanceTo(player.position().toAgent()) ?: 0.0
        // While a trip holds the position the divergence is the point of the trip, not a warning.
        TpAuraHud.pinGap = if (trip.isAway) {
            0.0
        } else {
            TeleportManager.teleports.serverPosition?.let { player.position().distanceTo(it.toMinecraft()) } ?: 0.0
        }
        TpAuraHud.tick(phase != Phase.IDLE)

        // The preview is computed from the live target, not only from a burst: a round only happens when the
        // target already left the spot, so waiting for one meant the boxes never had anything to show.
        if (prediction.enabled && prediction.spray && prediction.previewProbes && ticks % 2 == 0) {
            aimTarget?.takeIf { !it.isRemoved }?.let { target ->
                val box = predictedBox(target).toAgent()
                val spots = probeSpots(target, prediction.maxProbes)
                lastProbeBox = box
                lastProbeSpots = spots
                probesUntil = ticks + PROBE_PREVIEW_TICKS
                if (debug.log && ticks % 40 == 0) {
                    dlog(
                        "PROBE-PREVIEW",
                        TpAuraLog.details(
                            "n" to spots.size,
                            "box" to fmt(predictedBox(target).center),
                            "first" to (spots.firstOrNull()?.let { fmt(it) } ?: "none"),
                        ),
                    )
                }
            }
        }

        // The lead needs a velocity, and the client cannot read one off a remote entity: sample it here.
        aimTarget?.takeIf { !it.isRemoved }?.let { aimTrack.sample(it, ticks) }
        expireHitWait()

        when (phase) {
            Phase.IDLE -> engage()
            Phase.ROUTING -> {
                updateDestination()
                route()
            }
            Phase.READY -> launch()
            Phase.APPROACH -> approach()
            Phase.ATTACK -> attack()
            Phase.RETURN -> when {
                returnSearching -> pollReturn()
                // A HOLD leg leaves the server away, so the run's state is what says the leg is over.
                trip.run?.state != Teleport.State.ACTIVE -> finishReturnLeg()
            }
        }
    }

    @Suppress("unused")
    private val probeRenderHandler = handler<WorldRenderEvent> { event ->
        if (!prediction.enabled || !prediction.previewProbes || ticks > probesUntil) {
            return@handler
        }
        val probeBox = lastProbeBox ?: return@handler
        event.renderEnvironment {
            withPositionRelativeToCamera {
                drawBox(probeBox.toMinecraft(), PROBE_FILL, PROBE_OUTLINE)
                for (spot in lastProbeSpots) {
                    drawBox(
                        AABB(spot.x - 0.3, spot.y, spot.z - 0.3, spot.x + 0.3, spot.y + 1.8, spot.z + 0.3),
                        PROBE_SPOT_FILL,
                        PROBE_SPOT_OUTLINE,
                    )
                }
            }
        }
    }

    @Suppress("unused")
    private val hudHandler = handler<OverlayRenderEvent> { event ->
        TpAuraHud.render(event.context, hud)
    }

    @Suppress("unused")
    private val manualMouseHandler = handler<MouseButtonEvent> { event ->
        if (!manual.enabled || !event.isPressed || mc.gui.screen() != null) {
            return@handler
        }
        if (event.key == actionBinding(manual.trigger).key) {
            onManualTrigger()
        }
    }

    @Suppress("unused")
    private val manualKeyHandler = handler<KeyboardKeyEvent> { event ->
        if (!manual.enabled || !event.isPressed || event.isRepeat || mc.gui.screen() != null) {
            return@handler
        }
        if (event.key == actionBinding(manual.trigger).key) {
            onManualTrigger()
        }
    }

    /**
     * Swallows the triggering action for as long as the swing is pending: the press is consumed on the very
     * next tick, and a one-tick mute left the real click to go through and spend the weapon's cooldown.
     */
    @Suppress("unused")
    private val manualActionHandler = handler<KeybindIsPressedEvent> { event ->
        val binding = suppressBinding ?: return@handler
        if (ticks > suppressUntil) {
            suppressBinding = null
            return@handler
        }
        if (event.keyBinding === binding) {
            event.isPressed = false
            // `isDown` is the only thing the event covers; a queued press is consumed by `consumeClick`, which
            // is what vanilla's startAttack reads — drain it too or the click still goes through.
            while (binding.consumeClick()) {
                // drained on purpose
            }
        }
    }

    private fun onManualTrigger() {
        if (!manual.enabled || manualTriggered) {
            return
        }
        if (phase == Phase.ATTACK || phase == Phase.APPROACH || phase == Phase.RETURN || ticks < restUntil) {
            return
        }
        if (engagement == null) {
            return
        }

        manualTriggered = true
        manualRefreshAt = 0
        dlog("MANUAL-TRIGGER", TpAuraLog.details("phase" to phase))
        if (manual.preventExtraAction) {
            suppressBinding = actionBinding(manual.trigger)
            suppressUntil = ticks + MANUAL_SUPPRESS_TICKS
            dlog(
                "MANUAL-SUPPRESS",
                TpAuraLog.details("binding" to suppressBinding?.name, "until" to suppressUntil),
            )
        }
    }

    private fun actionBinding(trigger: ManualTrigger): KeyMapping = when (trigger) {
        ManualTrigger.LEFT_CLICK -> mc.options.keyAttack
        ManualTrigger.RIGHT_CLICK -> mc.options.keyUse
        ManualTrigger.JUMP -> mc.options.keyJump
        ManualTrigger.SNEAK -> mc.options.keyShift
    }

    @Suppress("unused")
    private val rotationUpdateHandler = handler<RotationUpdateEvent> {
        // A straight packet swing needs no look at all: turning towards the target only telegraphs the attack
        // and spends rotation time the swing does not wait for.
        if (attackWithoutLooking) {
            return@handler
        }

        val engagement = engagement ?: return@handler
        val feet = when (phase) {
            Phase.READY, Phase.APPROACH -> engagement.route?.destination?.toMinecraft()
            Phase.ATTACK -> trip.position.toMinecraft()
            else -> null
        } ?: return@handler

        val eyes = player.getEyePositionAt(feet)
        val box = engagement.target.boundingBox
        val rotation = raytraceBox(
            eyes,
            box,
            range = reach,
            wallsRange = if (throughWall.enabled) throughWall.range.toDouble() else 0.0,
        )?.rotation
            ?: Rotation.lookingAt(box.center, eyes)

        RotationManager.setRotationTarget(
            rotations.toRotationTarget(rotation, engagement.target),
            priority = Priority.IMPORTANT_FOR_USAGE_2,
            provider = this@ModuleTpAura
        )
    }

    private fun engage() {
        if (ticks < restUntil || !trip.isAvailable) {
            return
        }

        // Wait at home until the weapon is ready: departing on a long cooldown only stands next to the target
        // (or, with a route that ages, fails to dispatch at all). This is the TpAuraPlus launch gate.
        if (readyAttackTick() == null) {
            if (ticks % 10 == 0) {
                dlog("ENGAGE-WAIT-COOLDOWN", TpAuraLog.details("progress" to fmt(cooldownProgress())))
            }
            return
        }

        val origin = trip.position
        val target = targetSelector.targets().firstOrNull { unreachable.getOrDefault(it.id, 0) <= ticks }        // The swing budget belongs to the engagement, not to an arrival: a re-route must not top it up.
        engagement = target?.let { Engagement(it).apply { holdLeft = hold } }
        if (target == null) {
            view.clear()
            return
        }

        view.target = target
        aimTarget = target
        manualTriggered = false
        dlog(
            "ENGAGE",
            TpAuraLog.details(
                "target" to desc(target),
                "dist" to fmt(targetDist(target, origin.toMinecraft())),
                "serverPos" to fmt(origin),
                "onGround" to (TeleportManager.teleports.serverOnGround),
                "reach" to fmt(reach),
                "unreachable" to unreachable.size,
            ),
        )

        // Manual mode decides when: being in reach is not the trigger, the player's action is.
        if (isInReach(origin.toMinecraft(), target) && (!manual.enabled || manualTriggered)) {
            dlog("ENGAGE-IN-REACH", TpAuraLog.details("dist" to fmt(targetDist(target, origin.toMinecraft()))))
            startAttack()
            return
        }

        val goals = attackPositions(target, origin).ifEmpty {
            if (airTargets) attackPositions(target, origin, requireSupport = false) else emptyList()
        }
        if (goals.isEmpty()) {
            dlog(
                "ENGAGE-NO-GOALS",
                TpAuraLog.details(
                    "target" to desc(target),
                    "targetY" to fmt(target.position().y),
                    "targetOnGround" to target.onGround(),
                    "reach" to fmt(reach),
                    "margin" to fmt(TARGET_MOVEMENT_MARGIN),
                ),
            )
            fail(target, "no-goals")
        } else {
            dlog(
                "ENGAGE-GOALS",
                TpAuraLog.details(
                    "count" to goals.size,
                    "first" to goals.first(),
                    "side" to landingSide,
                    "tiers" to goals.take(6).map { goal ->
                        sideTier(
                            feetOrHover(Terrain.live(world), goal),
                            predictedBox(target).center.toAgent(),
                            facingOf(target),
                        )
                    },
                    "facing" to fmt(facingOf(target).toMinecraft()),
                ),
            )
            requestRoute(origin, goals)
        }
    }

    /** A route and the shape it was built with, for the log. */
    private data class Planned(val route: Route, val shape: String)

    private fun requestRoute(origin: AgentVec3, goals: List<BlockPos>) {
        trip.options = steps.options()
        // Measured flight starts here, not at the search: a straight route never reaches the search step.
        searchStartedTick = ticks
        searchStartedAt = System.nanoTime()
        val engagement = engagement ?: return
        engagement.goals = goals
        engagement.points.clear()
        engagement.steps.clear()
        engagement.start = origin
        engagement.segmentsLeft = debug.segments
        engagement.preview = null
        engagement.legScale = 1.0
        engagement.legAttempts = 0
        engagement.origin = origin
        engagement.shape = "agent"

        // Geometry first: the A* only exists for terrain that has to be steered around, and it cannot take a
        // goal without a floor or start anywhere it likes.
        val startedAt = System.nanoTime()
        engagement.planAnchor = predictedBox(engagement.target).center.toAgent()
        engagement.retargets = 0
        val planned = planRoute(origin, goals)
        val planMs = (System.nanoTime() - startedAt) / 1_000_000
        if (planned != null) {
            engagement.route = planned.route
            engagement.shape = planned.shape
            engagement.waited = 0
            view.active(planned.route.path.map { it.toMinecraft() })
            view.attackPosition = planned.route.destination.toMinecraft()
            dlog(
                "ROUTE-DIRECT",
                TpAuraLog.details(
                    "shape" to planned.shape,
                    "points" to planned.route.points.size,
                    "steps" to planned.route.steps.size,
                    "length" to fmt(planned.route.length),
                    "destination" to planned.route.destination,
                    "distToTarget" to fmt(targetDist(engagement.target, planned.route.destination.toMinecraft())),
                    "pacing" to steps.paceName,
                    "tier" to if (landingSide == LandingSide.ANY) {
                        0
                    } else {
                        sideTier(
                            planned.route.destination,
                            predictedBox(engagement.target).center.toAgent(),
                            facingOf(engagement.target),
                        )
                    },
                    "planMs" to planMs,
                ),
            )
            phase = Phase.READY
            launch()
            return
        }
        dlog("PLAN-MISS", TpAuraLog.details("goals" to goals.size, "planMs" to planMs))

        requestAgent(origin, goals)
    }

    /** One leg of the search; further legs are asked for from where this one ends. */
    private fun requestAgent(origin: AgentVec3, goals: List<BlockPos>) {
        val engagement = engagement ?: return
        val terrain = Terrain.live(world)
        val maxLength = range * ROUTE_DETOUR

        // The search runs on batches whatever the dispatch pacing is: `Burst` wraps it in `OneTick`, which
        // rejects any route that does not fit a single tick, and `Instant` in `DirectSearch`, which refuses
        // anything past 9.5 blocks. Both are properties of sending, not of planning.
        trip.options = Options(
            steps.options().stepLength,
            Pacing.Batches(Route.MAX_PACKETS_PER_TICK),
            steps.options().shouldClaimGroundOnDescent,
        )

        val relay = if (engagement.segmentsLeft > 1) relayGoal(engagement, origin, goals) else null

        if (relay != null) {
            dlog(
                "SEGMENT",
                TpAuraLog.details(
                    "left" to engagement.segmentsLeft,
                    "relay" to relay,
                    "origin" to fmt(origin),
                    "dist" to fmt(origin.distanceTo(AgentVec3.atBottomCenterOf(relay))),
                    "scale" to fmt(engagement.legScale),
                ),
            )
            trip.request(origin, listOf(relay), maxLength = maxLength)
        } else {
            dlog(
                "AGENT-REQUEST",
                TpAuraLog.details(
                    "origin" to fmt(origin),
                    "goals" to goals.size,
                    "maxLength" to fmt(maxLength),
                    "pacing" to steps.paceName,
                    "searchPacing" to "Batches",
                    "startStandable" to (terrain.check(BlockPos.containing(origin)) == DestinationCheck.VALID),
                    "straightDist" to fmt(goals.minOf { origin.distanceTo(AgentVec3.atBottomCenterOf(it)) }),
                ),
            )
            trip.request(origin, goals, maxLength = maxLength)
        }
        searchStartedAt = System.nanoTime()
        searchStartedTick = ticks
        sfx(sfx.pathFinding, SFX_PATH)
        phase = Phase.ROUTING
    }

    /**
     * Where a further search leg should stop: the node of the running search's preview that is closest to the
     * goal — the point the terrain actually lets a route reach — else the far end of a short line.
     */
    private fun relayGoal(engagement: Engagement, origin: AgentVec3, goals: List<BlockPos>): BlockPos? {
        val terrain = Terrain.live(world)
        val originNode = BlockPos.containing(origin)
        val goalFeet = goals.map { AgentVec3.atBottomCenterOf(it) }

        // The preview of the last search is the part of the map the route actually reached: the node closest
        // to the goal is where a further leg can start from.
        engagement.preview
            ?.map { BlockPos.containing(it) }
            ?.filter { it != originNode && terrain.feet(it) != null }
            ?.minByOrNull { node -> goalFeet.minOf { AgentVec3.atBottomCenterOf(node).distanceToSqr(it) } }
            ?.let { return it }

        // Nothing to go on yet: walk back along the line and take a spot with room above it, so the next leg
        // starts in the open rather than under the canopy that blocked this one.
        val goal = goalFeet.minByOrNull { origin.distanceToSqr(it) } ?: return null
        val delta = goal.subtract(origin)
        if (delta.length() * engagement.legScale < SEGMENT_MIN_LENGTH) {
            return null
        }
        val candidates = (1..SEGMENT_TRIES)
            .map { origin.add(delta.scale(engagement.legScale * (1.0 - it * 0.15))) }
            .map { BlockPos.containing(it) }
            .filter { it != originNode }

        return candidates.firstOrNull { node ->
            terrain.check(node) == DestinationCheck.VALID &&
                terrain.isClear(terrain.box(AgentVec3(node.x + 0.5, node.y + 1.0, node.z + 0.5)))
        } ?: candidates.firstOrNull { terrain.feet(it) != null }
    }

    /**
     * A route without a search, in the order that costs least: straight, step aside and go straight, step aside
     * and climb, climb from where we stand. Stepping aside first keeps a target behind a tree reachable
     * without gaining height at all.
     */
    private fun planRoute(origin: AgentVec3, goals: List<BlockPos>): Planned? {
        val terrain = Terrain.live(world)
        val stepLength = steps.options().stepLength
        val maxLength = range * ROUTE_DETOUR
        // Order as given: `attackPositions` already put the spots nearest the target first, and re-sorting by
        // distance to the player puts a walled-in target's spot back on the wrong side of its wall.
        val ordered = goals.take(MAX_DIRECT_GOALS)
            .mapNotNull { goal -> feetOf(terrain, goal)?.let { goal to it } }
            .filter { (_, feet) -> origin.distanceTo(feet) <= maxLength }

        for ((_, feet) in ordered.filter { (_, feet) -> feet.y - origin.y <= MAX_STRAIGHT_RISE }) {
            planDetour(terrain, listOf(origin, feet), stepLength, "straight")?.let {
                return Planned(it, "straight")
            }
        }

        for ((_, feet) in ordered.take(LATERAL_GOALS)) {
            for ((dx, dz) in SIDE_OFFSETS) {
                val side = AgentVec3(origin.x + dx, origin.y, origin.z + dz)
                planDetour(terrain, listOf(origin, side, feet), stepLength, "lateral")?.let {
                    return Planned(it, "lateral")
                }
            }
        }

        // Step aside first, then climb only as high as the crossing needs: `Route.plan` lifts a segment to the
        // higher of its endpoints, so naming the apex explicitly is the only way to keep the climb minimal.
        for ((_, feet) in ordered.take(LATERAL_GOALS)) {
            for ((dx, dz) in SIDE_OFFSETS) {
                val side = AgentVec3(origin.x + dx, origin.y, origin.z + dz)
                planMinimalLift(terrain, listOf(origin, side), feet, stepLength, "lateral-lift")?.let {
                    return Planned(it, "lateral-lift")
                }
            }
        }

        for ((_, feet) in ordered) {
            planMinimalLift(terrain, listOf(origin), feet, stepLength, "lift")?.let {
                return Planned(it, "lift")
            }
        }

        // Diagnostics for the case that matters: a goal close enough to touch yet no shape plans. The sweep
        // answer tells whether a block is in the way or the stepping itself refuses the move.
        ordered.firstOrNull()?.let { (_, feet) ->
            dlog(
                "PLAN-REJECT",
                TpAuraLog.details(
                    "dist" to fmt(origin.distanceTo(feet)),
                    "feet" to feet,
                    "origin" to fmt(origin),
                    "sweep" to terrain.sweep(origin, feet),
                    "stepLength" to fmt(stepLength),
                    "goals" to ordered.size,
                ),
            )
        }
        return null
    }

    private fun feetOf(terrain: Terrain, goal: BlockPos): AgentVec3? =
        terrain.feet(goal) ?: AgentVec3(goal.x + 0.5, goal.y.toDouble(), goal.z + 0.5)

    /**
     * Plans [polyline] and keeps bending it until no step crosses a block. `Route.plan` validates with the
     * server's axis-by-axis sweep, which slides past a corner that a straight line goes through; the check here
     * samples the line itself. A failed step is answered with a waypoint — above it or beside it — and never
     * with shorter steps: shortening a step does not move the line.
     */
    private fun planDetour(
        terrain: Terrain,
        polyline: List<AgentVec3>,
        stepLength: Double,
        shape: String,
    ): Route? {
        val points = polyline.toMutableList()
        for (attempt in 0..MAX_DETOURS) {
            val route = Route.plan(terrain, points, stepLength) ?: return null
            val bad = firstIllegalStep(terrain, route) ?: return route
            if (attempt == MAX_DETOURS) {
                return null
            }

            val step = route.steps[bad]
            val from = if (bad == 0) route.start else route.steps[bad - 1].position
            val anchor = from.add(step.position.subtract(from).scale(0.5))
            val insertAt = (step.segment + 1).coerceIn(0, points.size)
            val via = detourCandidates(anchor).firstOrNull { candidate ->
                val trial = points.toMutableList().apply { add(insertAt, candidate) }
                val trialRoute = Route.plan(terrain, trial, stepLength) ?: return@firstOrNull false
                firstIllegalStep(terrain, trialRoute) == null
            } ?: run {
                dlog(
                    "ROUTE-DETOUR-FAIL",
                    TpAuraLog.details("shape" to shape, "step" to bad, "at" to fmt(anchor)),
                )
                return null
            }

            points.add(insertAt, via)
            dlog(
                "ROUTE-DETOUR",
                TpAuraLog.details(
                    "shape" to shape,
                    "step" to bad,
                    "at" to fmt(anchor),
                    "via" to fmt(via),
                ),
            )
        }
        return null
    }

    /** Where a detour can go around [anchor]: above the line first, then to either side of it. */    private fun detourCandidates(anchor: AgentVec3): List<AgentVec3> = buildList {
        for (lift in DETOUR_LIFTS) {
            add(anchor.add(0.0, lift, 0.0))
        }
        for (offset in DETOUR_SIDES) {
            add(anchor.add(offset, 0.0, 0.0))
            add(anchor.add(-offset, 0.0, 0.0))
            add(anchor.add(0.0, 0.0, offset))
            add(anchor.add(0.0, 0.0, -offset))
        }
    }

    /** Whether the library's own way home would plan: the same corners, swept with the same step length. */
    private fun libCanComeBack(from: AgentVec3, home: AgentVec3): Boolean {
        val points = ArrayList<AgentVec3>()
        points += from
        for (point in lastOutbound.asReversed()) {
            if (points.last().distanceToSqr(point) > 1.0E-6) {
                points += point
            }
        }
        if (points.last().distanceToSqr(home) > 1.0E-6) {
            points += home
        }
        return Route.plan(Terrain.live(world), points, steps.options().stepLength) != null
    }

    /**
     * The advanced return, in the order it was asked for: straight shapes first (with the lateral and detour
     * retries), then an open spot 8–10 blocks off to break contact, searched to with A\*, and home from there.
     * The last resort is the library's own path, which lands the client when it cannot plan.
     */
    private fun startAdvancedReturn(from: AgentVec3, home: AgentVec3) {
        // Nothing to undo when the server never left: planning here only invents a move.
        val gap = from.distanceTo(home)
        if (gap < RETURN_HOME_GAP) {
            dlog("RETURN-SKIP", TpAuraLog.details("gap" to fmt(gap)))
            return
        }

        val started = System.nanoTime()
        returnGoal = home
        returnLeg = ReturnLeg.TO_HOME

        planReturn(from, home)?.let { route ->
            trip.options = steps.options()
            if (trip.dispatch(route, Teleport.Finish.RESYNC) != null) {
                dlog(
                    "RETURN-DISPATCHED",
                    TpAuraLog.details(
                        "leg" to "straight",
                        "packets" to route.steps.size,
                        "length" to fmt(route.length),
                        "ms" to msSince(started),
                    ),
                )
                returnGoal = route.destination
                returnSearching = false
                phase = Phase.RETURN
                return
            }
        }

        val retreat = retreatPoint(Terrain.live(world), from, home)
        if (retreat == null) {
            // No spot to fall back to: the search is still the best chance of getting out, straight home.
            dlog("RETURN-FAILED", TpAuraLog.details("reason" to "no-retreat", "ms" to msSince(started)))
            requestReturnSearch(from, home, ReturnLeg.TO_HOME)
            return
        }
        requestReturnSearch(from, retreat, ReturnLeg.TO_RETREAT)
    }

    /**
     * Intermediate legs keep the server position (`HOLD`), otherwise the next leg has nothing to measure its
     * drift against and `dispatch` refuses it; only the leg that reaches home hands the server back (`RESYNC`).
     */
    private fun returnFinish(): Teleport.Finish =
        if (returnLeg == ReturnLeg.TO_HOME) Teleport.Finish.RESYNC else Teleport.Finish.HOLD

    /** Asks the search for one leg of the way home; `pollReturn` picks it up every tick. */
    private fun requestReturnSearch(from: AgentVec3, goal: AgentVec3, leg: ReturnLeg) {
        trip.options = Options(
            steps.options().stepLength,
            Pacing.Batches(Route.MAX_PACKETS_PER_TICK),
            steps.options().shouldClaimGroundOnDescent,
        )
        trip.request(from, listOf(BlockPos.containing(goal)), maxLength = range * ROUTE_DETOUR)
        returnLeg = leg
        returnSearching = true
        returnStartedAt = System.nanoTime()
        phase = Phase.RETURN
        dlog(
            "RETURN-SEARCH",
            TpAuraLog.details("leg" to leg.name.lowercase(), "from" to fmt(from), "goal" to BlockPos.containing(goal)),
        )
    }

    private fun pollReturn() {
        when (val result = trip.poll()) {
            Outcome.Searching -> Unit
            Outcome.AlreadyThere -> finishReturnLeg()
            is Outcome.Found -> {
                val terrain = Terrain.live(world)
                val route = repairAgentRoute(terrain, result.value)
                if (debug.log) {
                    checkRoute(route, "return-" + returnLeg.name.lowercase())
                }
                if (trip.dispatch(route, returnFinish()) == null) {
                    dlog(
                        "RETURN-FAILED",
                        TpAuraLog.details(
                            "reason" to "dispatch",
                            "leg" to returnLeg.name.lowercase(),
                            "routeStart" to route.start,
                            "serverPos" to (TeleportManager.teleports.serverPosition?.let { fmt(it) } ?: "none"),
                            "available" to trip.isAvailable,
                            "onGround" to player.onGround(),
                        ),
                    )
                    landedFallback()
                    return
                }
                dlog(
                    "RETURN-DISPATCHED",
                    TpAuraLog.details(
                        "leg" to returnLeg.name.lowercase(),
                        "packets" to route.steps.size,
                        "length" to fmt(route.length),
                        "ms" to msSince(returnStartedAt),
                    ),
                )
                returnGoal = route.destination
                returnSearching = false
            }
            is Outcome.Failed -> {
                dlog(
                    "RETURN-FAILED",
                    TpAuraLog.details(
                        "reason" to result.failure.name.lowercase(),
                        "leg" to returnLeg.name.lowercase(),
                        "ms" to msSince(returnStartedAt),
                    ),
                )
                landedFallback()
            }
        }
    }

    /** A leg ended: either the rest of the way home from the retreat spot, or the end of the return. */
    private fun finishReturnLeg() {
        val from = returnGoal
        if (returnLeg == ReturnLeg.TO_RETREAT && from != null) {
            val home = player.position().toAgent()
            planReturn(from, home)?.let { route ->
                trip.options = steps.options()
                if (trip.dispatch(route, returnFinish()) != null) {
                    dlog(
                        "RETURN-DISPATCHED",
                        TpAuraLog.details(
                            "leg" to "home-straight",
                            "packets" to route.steps.size,
                            "length" to fmt(route.length),
                        ),
                    )
                    returnLeg = ReturnLeg.TO_HOME
                    returnGoal = route.destination
                    returnSearching = false
                    return
                }
            }
            requestReturnSearch(from, home, ReturnLeg.TO_HOME)
            return
        }

        returnLeg = ReturnLeg.NONE
        returnSearching = false
        view.done()
        phase = Phase.IDLE
    }

    /**
     * The last resort: put the client where the server is, which is what the library's `land` does. `comeBack`
     * cannot help here — a finished leg released the holder, so `undo` has nothing to act on and the server
     * would stay where the failed leg left it, with the client still at home.
     */
    private fun landedFallback() {
        val at = returnGoal
        returnLeg = ReturnLeg.NONE
        returnSearching = false
        if (at != null) {
            dlog("RETURN-LAND", TpAuraLog.details("at" to fmt(at)))
            GlobalSettingsTeleport.teleport(at.x, at.y, at.z)
        } else {
            trip.comeBack()
        }
        // `comeBack` may have started a run of its own; going IDLE now let the next engagement dispatch into it.
        phase = if (trip.isAway) Phase.RETURN else Phase.IDLE
    }

    /**
     * The way home when the library's `undo` gives up: back along the corners of the route that was taken,
     * else via a point far enough from the target to leave a cave, else the straight shapes. The run is sent
     * with `RESYNC`, which hands the server back to the client — the same ending `undo` aims for.
     */
    private fun planReturn(from: AgentVec3, home: AgentVec3): Route? {
        val started = System.nanoTime()
        val terrain = Terrain.live(world)
        val step = steps.options().stepLength

        val back = ArrayList<AgentVec3>()
        back += from
        for (point in lastOutbound.asReversed()) {
            if (back.last().distanceToSqr(point) > 1.0E-6) {
                back += point
            }
        }
        if (back.last().distanceToSqr(home) > 1.0E-6) {
            back += home
        }
        planDetour(terrain, back, step, "return")?.let {
            dlog(
                "RETURN-PLANNED",
                TpAuraLog.details("shape" to "trail", "corners" to back.size, "length" to fmt(it.length), "ms" to msSince(started)),
            )
            return it
        }

        retreatPoint(terrain, from, home)?.let { retreat ->
            planDetour(terrain, listOf(from, retreat, home), step, "return-via")?.let {
                dlog(
                    "RETURN-PLANNED",
                    TpAuraLog.details("shape" to "retreat", "retreat" to fmt(retreat), "length" to fmt(it.length), "ms" to msSince(started)),
                )
                return it
            }
        }

        val feet = feetOf(terrain, BlockPos.containing(home)) ?: home
        planDetour(terrain, listOf(from, feet), step, "return-straight")?.let {
            dlog(
                "RETURN-PLANNED",
                TpAuraLog.details("shape" to "straight", "length" to fmt(it.length), "ms" to msSince(started)),
            )
            return it
        }
        dlog(
            "RETURN-FAILED",
            TpAuraLog.details("from" to fmt(from), "home" to fmt(home), "ms" to msSince(started)),
        )
        return null
    }

    /**
     * A standable spot to break contact: a fan of [RETREAT_DIRECTIONS] around the way home at growing
     * distances, then any standable column near where the server has the player. The fan matters underground —
     * the straight line home is usually solid rock, while a few blocks to the side is a cave.
     */
    private fun retreatPoint(terrain: Terrain, from: AgentVec3, home: AgentVec3): AgentVec3? {
        val delta = home.subtract(from)
        val length = delta.length()
        val forward = if (length > 1.0E-4) delta.scale(1.0 / length) else AgentVec3(0.0, 0.0, 1.0)

        for (distance in RETREAT_DISTANCES) {
            for (step in 0 until RETREAT_DIRECTIONS) {
                val angle = step * (Math.PI * 2.0 / RETREAT_DIRECTIONS)
                val cos = kotlin.math.cos(angle)
                val sin = kotlin.math.sin(angle)
                val direction = AgentVec3(
                    forward.x * cos - forward.z * sin,
                    0.0,
                    forward.x * sin + forward.z * cos,
                )
                val block = BlockPos.containing(from.add(direction.scale(distance)))
                terrain.feet(block)?.let { return it }
            }
        }

        // Last resort: any standable column within a few blocks, at any height that is reachable in a step.
        val origin = BlockPos.containing(from)
        var tried = 0
        for (dx in -RETREAT_SCAN..RETREAT_SCAN) {
            for (dz in -RETREAT_SCAN..RETREAT_SCAN) {
                if (dx == 0 && dz == 0) {
                    continue
                }
                for (dy in RETREAT_SCAN_Y downTo -RETREAT_SCAN_Y) {
                    tried++
                    val feet = terrain.feet(origin.offset(dx, dy, dz)) ?: continue
                    // A spot next to where we stand is no retreat: it leaves us inside the same reach.
                    if (feet.distanceTo(from) >= RETREAT_MIN_GAP) {
                        return feet
                    }
                }
            }
        }
        dlog(
            "RETREAT-NONE",
            TpAuraLog.details(
                "from" to fmt(from),
                "home" to fmt(home),
                "line" to fmt(length),
                "scanned" to tried,
            ),
        )
        return null
    }

    /** Index of the first step whose straight line crosses a block, or null when the whole route is clean. */
    private fun firstIllegalStep(terrain: Terrain, route: Route): Int? {
        var previous = route.start
        for ((index, step) in route.steps.withIndex()) {
            if (!lineClear(terrain, previous, step.position)) {
                return index
            }
            previous = step.position
        }
        return null
    }

    /** Whether the player fits at every sample along the straight line from [from] to [to]. */
    private fun lineClear(terrain: Terrain, from: AgentVec3, to: AgentVec3): Boolean {
        val distance = from.distanceTo(to)
        val count = max(1, ceil(distance / ILLEGAL_SAMPLE).toInt())
        for (sample in 1 until count) {
            val point = from.add(to.subtract(from).scale(sample.toDouble() / count))
            if (!terrain.isClear(terrain.box(point))) {
                return false
            }
        }
        return true
    }

    /**
     * While the route is still being searched, follows the target: once its predicted position has drifted far
     * enough from where the goals were built, the search is pointed at fresh goals. The library cannot steer a
     * running search, so this restarts it — hence the cooldown and the per-engagement cap.
     */
    private fun updateDestination() {
        val engagement = engagement ?: return
        if (!prediction.enabled || !prediction.liveRetarget) {
            return
        }
        if (ticks - engagement.retargetTick < RETARGET_COOLDOWN || engagement.retargets >= MAX_RETARGETS) {
            return
        }

        val anchor = engagement.planAnchor ?: return
        val predicted = predictedBox(engagement.target).center.toAgent()
        val drift = predicted.distanceTo(anchor)
        val speed = aimTrack.velocity()?.length() ?: 0.0
        val threshold = max(RETARGET_DRIFT, speed * RETARGET_TICKS)
        if (drift <= threshold) {
            return
        }

        val goals = attackPositions(engagement.target, trip.position)
        if (goals.isEmpty()) {
            return
        }

        engagement.retargetTick = ticks
        engagement.retargets++
        dlog(
            "RETARGET",
            TpAuraLog.details(
                "drift" to fmt(drift),
                "threshold" to fmt(threshold),
                "goals" to goals.size,
                "n" to engagement.retargets,
            ),
        )
        requestRoute(trip.position, goals)
    }

    private fun route() {
        val engagement = liveEngagement ?: return leave("target-lost-routing")
        when (val result = trip.poll()) {
            Outcome.Searching -> {
                if (ticks % 5 == 0) {
                    dlog("ROUTE-SEARCHING", TpAuraLog.details("origin" to fmt(trip.position)))
                }
            }
            Outcome.AlreadyThere -> {
                dlog("ROUTE-ALREADY-THERE")
                startAttack()
            }
            is Outcome.Found -> {
                val route = repairAgentRoute(Terrain.live(world), result.value)
                engagement.points += route.points
                engagement.steps += route.steps
                engagement.segmentsLeft--
                // Another leg only when it would actually shorten the search: a remainder of a few blocks is
                // cheaper to cover by a straight shape than by another request.
                val remaining = engagement.goals.minOfOrNull {
                    route.destination.distanceTo(AgentVec3.atBottomCenterOf(it))
                } ?: 0.0
                if (engagement.segmentsLeft > 0 && remaining > SEGMENT_MIN_LENGTH) {
                    dlog(
                        "SEGMENT-FOUND",
                        TpAuraLog.details(
                            "left" to engagement.segmentsLeft,
                            "destination" to route.destination,
                            "points" to engagement.points.size,
                        ),
                    )
                    requestAgent(route.destination, engagement.goals)
                    return
                }

                // Legs are stitched into one route: `Teleports` starts a run where the route starts, so a route
                // that began at a relay would be refused for starting too far from the server position.
                // The search's own route is trusted here: every step of it was checked against the same sweep
                // the server uses, while the sampler below is stricter than that and used to discard most of them.
                val stitched = stitch(engagement, route)
                engagement.route = stitched
                engagement.waited = 0
                dlog(
                    "ROUTE-FOUND",
                    TpAuraLog.details(
                        "points" to stitched.points.size,
                        "steps" to stitched.steps.size,
                        "length" to fmt(stitched.length),
                        "start" to stitched.start,
                        "destination" to stitched.destination,
                        "distToTarget" to fmt(targetDist(engagement.target, stitched.destination.toMinecraft())),
                        "segments" to debug.segments,
                        "searchMs" to msSince(searchStartedAt),
                        "searchTicks" to (ticks - searchStartedTick),
                    ),
                )
                view.active(stitched.path.map { it.toMinecraft() })
                view.attackPosition = stitched.destination.toMinecraft()
                phase = Phase.READY
                launch()
            }
            is Outcome.Failed -> {
                dlog(
                    "ROUTE-FAILED",
                    TpAuraLog.details(
                        "failure" to result.failure,
                        "origin" to fmt(trip.position),
                        "onGround" to player.onGround(),
                        "target" to desc(engagement.target),
                        "targetDist" to fmt(targetDist(engagement.target)),
                        "segments" to debug.segments,
                        "searchMs" to msSince(searchStartedAt),
                        "searchTicks" to (ticks - searchStartedTick),
                    ),
                )
                // A leg that failed says the budget or the terrain was too much for it: ask again for a nearer
                // relay instead of giving the target up. Bounded, so it cannot retry forever.
                if (debug.segments > 1 && engagement.legAttempts < MAX_LEG_ATTEMPTS) {
                    engagement.legAttempts++
                    engagement.legScale *= LEG_BACKOFF
                    dlog(
                        "SEGMENT-BACKOFF",
                        TpAuraLog.details(
                            "attempt" to engagement.legAttempts,
                            "scale" to fmt(engagement.legScale),
                        ),
                    )
                    requestAgent(trip.position, engagement.goals)
                    return
                }
                fail(engagement.target, "route-" + result.failure.name.lowercase())
            }
        }
    }

    /** Sets off once the clicker is about to click when the route arrives. */
    private fun launch() {
        val engagement = liveEngagement ?: return leave("target-lost-ready")
        var route = engagement.route ?: return leave("no-route")

        // Diagnostics only: does the line the renderer draws actually pass through blocks the planner swept?
        if (debug.log) {
            checkRoute(route, engagement.shape)
        }

        // Armed but holding: keep the plan fresh and the line drawn, and let the player decide when to go.
        if (manual.enabled && !manualTriggered) {
            if (!manual.showRoute) {
                view.clear()
            }
            if (ticks >= manualRefreshAt) {
                manualRefreshAt = ticks + manual.refreshInterval
                requestRoute(player.position().toAgent(), engagement.goals)
            }
            return
        }

        // The plan may be older than the player: armed and waiting, or waiting out the weapon. The library
        // refuses a route that starts more than two blocks from the server, which is what turned "walks while
        // attacking" into a dispatch failure in manual mode only — the automatic path dispatches the same tick.
        val budget = debug.driftBudget.toDouble()
        if (route.start.distanceToSqr(player.position().toAgent()) > budget * budget) {
            requestRoute(player.position().toAgent(), engagement.goals)
            if (phase == Phase.ROUTING) {
                return
            }
            route = engagement.route ?: return
            if (route.start.distanceToSqr(player.position().toAgent()) > budget * budget) {
                return
            }
            dlog(
                "LAUNCH-REPLAN",
                TpAuraLog.details(
                    "start" to fmt(route.start),
                    "client" to fmt(player.position()),
                    "shape" to engagement.shape,
                ),
            )
        }

        // A previous leg may still be in flight: the library refuses a new dispatch then, and reporting that as
        // a failure left manual mode unable to attack right after a return.
        if (!trip.isAvailable) {
            if (++engagement.idleTicks > ATTACK_PATIENCE) {
                dlog("LAUNCH-BUSY", TpAuraLog.details("run" to (trip.run?.state ?: "none")))
                return leave("launch-busy")
            }
            return
        }

        val arrival = steps.ticks(route)

        // Wait at home until the weapon will be ready when the route lands: departing earlier only buys a longer
        // wait on the far side, and the measured flight then reads the cooldown instead of the travel.
        if (!clicker.canAttackAt(arrival)) {
            // The player may walk while the weapon cools down; a route planned where they stood is then refused
            // for drift, which read as "manual mode cannot attack unless standing still".
            if (route.start.distanceToSqr(player.position().toAgent()) >
                debug.driftBudget.toDouble() * debug.driftBudget.toDouble()
            ) {
                requestRoute(player.position().toAgent(), engagement.goals)
                return
            }
            if (++engagement.idleTicks > ATTACK_PATIENCE) {
                dlog(
                    "LAUNCH-PATIENCE",
                    TpAuraLog.details("arrival" to arrival, "progress" to fmt(cooldownProgress())),
                )
                return leave("launch-patience")
            }
            if (debug.log && ticks % 5 == 0) {
                dlog(
                    "LAUNCH-WAIT",
                    TpAuraLog.details("arrival" to arrival, "progress" to fmt(cooldownProgress())),
                )
            }
            return
        }

        val finish = if (afterAttack == AfterAttack.RETURN) Teleport.Finish.HOLD else Teleport.Finish.LAND
        val dispatched = dispatchRoute(route, finish)
        if (dispatched == null) {
            dlog(
                "DISPATCH-FAILED",
                TpAuraLog.details(
                    "finish" to finish,
                    "arrival" to arrival,
                    "routeStart" to route.start,
                    "serverPos" to (TeleportManager.teleports.serverPosition?.let { fmt(it) } ?: "none"),
                    "available" to trip.isAvailable,
                    "onGround" to player.onGround(),
                ),
            )
            return leave("dispatch-failed")
        }
        dlog(
            "DISPATCH",
            TpAuraLog.details(
                "finish" to finish,
                "arrival" to arrival,
                "points" to route.points.size,
                "packets" to dispatched.route.steps.size,
                "batches" to dispatched.batches,
                "pacing" to steps.paceName,
                "start" to route.start,
                "destination" to route.destination,
                "client" to fmt(player.position()),
                "spoofGround" to steps.options().shouldClaimGroundOnDescent,
                "serverGap" to fmt(player.position().distanceTo(trip.position.toMinecraft())),
                "maxDrop" to fmt(lastMaxDrop),
                "playerY" to fmt(player.position().y),
                "feetY" to (Terrain.live(world).feet(BlockPos.containing(player.position().toAgent()))
                    ?.let { fmt(it.y) } ?: "none"),
                "columnCheck" to Terrain.live(world).check(BlockPos.containing(player.position().toAgent())),
                "startSupported" to Terrain.live(world).isSupported(player.position().toAgent()),
                "goalSupported" to Terrain.live(world).isSupported(route.destination),
            ),
        )
        phase = Phase.APPROACH
    }

    /**
     * Hands [route] to the library in the shape the chosen pacing asks for. `Burst` sends every step in one
     * tick, which the library caps at five per tick unless the route is built for a server that judges each
     * packet on its own; `Instant` sends the destination as a single packet, as older clients did.
     *
     * TODO(spray): a probe chain needs an attack between two position packets, which this single run cannot
     * express — the spray path will have to send its own packets and use `Teleports.syncPosition()` to hand
     * the server back.
     */
    private fun dispatchRoute(route: Route, finish: Teleport.Finish): Prepared? {
        val base = steps.options()
        val prepared = when {
            steps.isInstant -> {
                val last = route.steps.last()
                val jump = Route(
                    route.points,
                    listOf(Route.Step(route.destination, last.onGround, last.segment)),
                    route.mechanics,
                )
                dlog("INSTANT-JUMP", TpAuraLog.details("dist" to fmt(route.length)))
                Prepared(jump, 1, Options(base.stepLength, Pacing.Batches(1), base.shouldClaimGroundOnDescent))
            }
            steps.isBurst -> {
                // Built for a server that judges each packet alone, so the library sends them all in one tick.
                val oneTick = Route(route.points, route.steps, Mechanics.V1_8)
                Prepared(oneTick, 1, Options(base.stepLength, Pacing.Burst, base.shouldClaimGroundOnDescent))
            }
            else -> Prepared(route, route.batches(route.steps.size).size, base)
        }

        // Diagnostics: what the server is about to be told, step by step, and how deep this tick drops.
        val shipped = prepared.route
        logSteps(shipped)
        lastMaxDrop = maxDropPerTick(shipped)

        trip.options = prepared.options
        trip.dispatch(shipped, finish) ?: return null
        lastOutbound.clear()
        lastOutbound += shipped.points
        sfx(sfx.teleport, SFX_TELEPORT)
        return prepared.copy(route = shipped, batches = steps.batchCount(shipped))
    }


    /** One line per step: where it goes, how far it rises or falls, and what kind of move it is. */
    private fun logSteps(route: Route) {
        if (!debug.log) {
            return
        }
        var previous = route.start
        for ((index, step) in route.steps.withIndex()) {
            if (index >= MAX_STEP_LOGS) {
                dlog("STEP", TpAuraLog.details("i" to "${index + 1}/${route.steps.size}", "rest" to "omitted"))
                return
            }
            val delta = step.position.y - previous.y
            val kind = when {
                delta < -1.0E-6 -> "drop"
                delta > BOUNCE_RISE -> "up"
                delta > 1.0E-6 -> "bounce"
                else -> "flat"
            }
            dlog(
                "STEP",
                TpAuraLog.details(
                    "i" to "${index + 1}/${route.steps.size}",
                    "pos" to fmt(step.position),
                    "dy" to fmt(delta),
                    "kind" to kind,
                    "onGround" to step.onGround,
                ),
            )
            previous = step.position
        }
    }

    /** Deepest cumulative fall inside a single tick, which is what a strict server judges as one move. */
    private fun maxDropPerTick(route: Route): Double {
        var index = 0
        var deepest = 0.0
        for (size in steps.batchSizes(route)) {
            var previousY = if (index == 0) route.start.y else route.steps[index - 1].position.y
            var drop = 0.0
            for (step in index until min(index + size, route.steps.size)) {
                val y = route.steps[step].position.y
                if (y < previousY) {
                    drop += previousY - y
                }
                previousY = y
            }
            deepest = max(deepest, drop)
            index += size
        }
        return deepest
    }

    /** What was handed to the library, for the log. */
    private data class Prepared(val route: Route, val batches: Int, val options: Options)

    /**
     * Walks the route again the way the renderer draws it — straight between two steps — and tests the player's
     * box at every sample. `Route.plan` validates with the server's axis-by-axis sweep, which can pass a corner
     * that a straight line visibly cuts through; this tells the two apart, and names the step that does it.
     */
    private fun checkRoute(route: Route, shape: String) {
        val terrain = Terrain.live(world)
        var previous = route.start
        var illegal = 0

        for ((index, step) in route.steps.withIndex()) {
            if (!lineClear(terrain, previous, step.position)) {
                illegal++
                if (illegal <= ROUTE_ILLEGAL_LOGS) {
                    dlog(
                        "ROUTE-ILLEGAL",
                        TpAuraLog.details(
                            "shape" to shape,
                            "step" to index,
                            "from" to fmt(previous),
                            "to" to fmt(step.position),
                            "dist" to fmt(previous.distanceTo(step.position)),
                            "dest" to route.destination,
                            "destCheck" to terrain.check(BlockPos.containing(route.destination)),
                            "destSupported" to terrain.isSupported(route.destination),
                        ),
                    )
                }
            }
            previous = step.position
        }

        if (illegal > 0) {
            dlog(
                "ROUTE-ILLEGAL-SUMMARY",
                TpAuraLog.details(
                    "shape" to shape,
                    "badSteps" to illegal,
                    "steps" to route.steps.size,
                    "length" to fmt(route.length),
                    "start" to route.start,
                    "destination" to route.destination,
                ),
            )
        }
    }

    /** Stitches the legs found so far into one route, falling back to the last leg if the corners do not plan. */
    /**
     * The search compacts its nodes and re-plans them with the full step length, which merges one-block moves
     * into long straight lines that cut corners — through a canopy that means leaves. Re-planning the same
     * corners with a short step keeps the path and lets the detour filter bend around what it still hits.
     */
    private fun repairAgentRoute(terrain: Terrain, route: Route): Route {
        // Full step first: the detour filter needs a route it can plan before it can bend one, and a short
        // step cannot always be swept where the search's own move could.
        for (step in doubleArrayOf(steps.options().stepLength, min(steps.options().stepLength, AGENT_STEP_MAX), AGENT_STEP)) {
            val repaired = planDetour(terrain, route.points, step, "agent") ?: continue
            if (firstIllegalStep(terrain, repaired) != null) {
                continue
            }
            dlog(
                "ROUTE-REPAIR",
                TpAuraLog.details(
                    "steps" to repaired.steps.size,
                    "length" to fmt(repaired.length),
                    "was" to route.steps.size,
                    "step" to fmt(step),
                ),
            )
            return repaired
        }
        dlog(
            "ROUTE-REPAIR-FAIL",
            TpAuraLog.details("was" to route.steps.size, "length" to fmt(route.length)),
        )
        return route
    }

    /**
     * Joins the legs into one route by keeping their steps. Re-planning the corners instead would merge the
     * one-block steps of the search into long straight lines, and a line through a canopy takes leaves with it.
     */
    private fun stitch(engagement: Engagement, last: Route): Route {
        engagement.shape = if (engagement.steps.size > 0 && engagement.points.size > 1) "segmented" else engagement.shape
        val steps = engagement.steps
        val start = engagement.start
        if (steps.isEmpty() || start == null) {
            return last
        }

        val points = ArrayList<AgentVec3>(steps.size + 1)
        points += start
        val rebuilt = ArrayList<Route.Step>(steps.size)
        for ((index, step) in steps.withIndex()) {
            points += step.position
            rebuilt += Route.Step(step.position, step.onGround, index)
        }
        return Route(points, rebuilt, last.mechanics)
    }

    /**
     * Climbs only as high as the crossing needs: the lowest apex whose horizontal leg plans, then one block of
     * margin on top of it — dropped again when that extra block is what makes the plan fail.
     */
    private fun planMinimalLift(
        terrain: Terrain,
        prefix: List<AgentVec3>,
        feet: AgentVec3,
        stepLength: Double,
        shape: String,
    ): Route? {
        val from = prefix.last()
        val base = max(from.y, feet.y)
        var lowest: Double? = null

        // Cheap plans first: the lowest apex that lets the crossing through, one block at a time.
        for (extra in 1..MAX_LIFT) {
            val apex = base + extra
            val points = prefix + listOf(
                AgentVec3(from.x, apex, from.z),
                AgentVec3(feet.x, apex, feet.z),
                feet,
            )
            if (Route.plan(terrain, points, stepLength) != null) {
                lowest = apex
                break
            }
        }
        val apex = lowest ?: return null

        for (candidate in doubleArrayOf(apex + 1.0, apex)) {
            val points = prefix + listOf(
                AgentVec3(from.x, candidate, from.z),
                AgentVec3(feet.x, candidate, feet.z),
                feet,
            )
            planDetour(terrain, points, stepLength, shape)?.let { return it }
        }
        return null
    }

    private fun approach() {
        val state = trip.run?.state ?: return leave("no-run-on-approach")
        if (state != Teleport.State.ACTIVE) {
            dlog(
                "APPROACH",
                TpAuraLog.details(
                    "state" to state,
                    "serverPos" to (TeleportManager.teleports.serverPosition?.let { fmt(it) } ?: "none"),
                    "client" to fmt(player.position()),
                ),
            )
        }
        when (state) {
            Teleport.State.ACTIVE -> {}
            Teleport.State.DONE -> {
                // Measured here, not at the swing: what the landing spot was predicted from is the request, and
                // the ticks to standing on it are what the target travels unaccounted. Measuring at the swing
                // folded the weapon's cooldown into the flight and pinned the lead to its ceiling.
                if (searchStartedTick > 0) {
                    val measured = (ticks - searchStartedTick).toDouble()
                    flightAvg = flightAvg * FLIGHT_EMA_KEEP + measured * FLIGHT_EMA_TAKE
                    dlog(
                        "LATENCY",
                        TpAuraLog.details("measured" to fmt(measured), "avg" to fmt(flightAvg)),
                    )
                }
                dlog("ARRIVED", TpAuraLog.details("serverPos" to fmt(trip.position)))
                startAttack()
            }
            Teleport.State.REJECTED -> rejected()
            Teleport.State.INVALID, Teleport.State.CANCELLED -> {
                view.invalid()
                trip.showReturn()
                phase = Phase.RETURN
            }
        }
    }

    private fun startAttack() {
        // Only per-arrival state is reset: the swing budget and the retry budget belong to the engagement,
        // otherwise every re-route tops them up and `Hold` never expires.
        engagement?.run {
            attacked = false
        }
        dlog(
            "START-ATTACK",
            TpAuraLog.details(
                "serverPos" to fmt(trip.position),
                "holdLeft" to engagement?.holdLeft,
                "dist" to engagement?.let { fmt(targetDist(it.target)) },
            ),
        )
        phase = Phase.ATTACK
    }

    private fun attack() {
        if (trip.run?.state == Teleport.State.REJECTED) {
            return rejected()
        }

        val engagement = liveEngagement ?: return leave("attack-over")
        val target = engagement.target
        val feet = trip.position.toMinecraft()

        // `Target → HurtTime` decides when a swing may go out: attacking inside hurt time spends the weapon's
        // cooldown and deals nothing. Waiting is not idling, so it does not eat the patience budget by itself.
        if (target.hurtTime > targetSelector.hurtTimeLimit) {
            if (!hurtWaiting) {
                hurtWaiting = true
                dlog(
                    "HURT-WAIT",
                    TpAuraLog.details(
                        "hurtTime" to target.hurtTime,
                        "limit" to targetSelector.hurtTimeLimit,
                        "holdLeft" to engagement.holdLeft,
                    ),
                )
            }
            if (++engagement.idleTicks > ATTACK_PATIENCE) {
                dlog("ATTACK-PATIENCE", TpAuraLog.details("waiting" to "hurt-time"))
                return leave("attack-patience")
            }
            return
        }
        hurtWaiting = false

        // With the spray on nothing is swung from where the player happens to stand: the chain walks the server
        // along the target's path first, a position packet and a swing per spot, all inside this tick.
        if (prediction.enabled && prediction.spray && attackReady() &&
            engagement.repins <= debug.holdRepins && sprayAt(target)
        ) {
            engagement.repins++
            return
        }

        if (!isInReach(feet, target)) {
            dlog(
                "ATTACK-OUT-OF-REACH",
                TpAuraLog.details(
                    "dist" to fmt(targetDist(target, feet)),
                    "reach" to fmt(reachTo(feet, target)),
                    "retries" to engagement.retries,
                    "holdLeft" to engagement.holdLeft,
                ),
            )
            return if (engagement.retries++ < MAX_RETRIES) replan() else leave("out-of-reach")
        }

        view.engaged = true
        var swing = false
        if (attackWithoutLooking) {
            // The packet goes out as soon as the weapon allows it; nothing waits for a click plan or a rotation.
            if (attackReady()) {
                swing = true
            } else {
                dlog("ATTACK-COOLDOWN", TpAuraLog.details("progress" to fmt(cooldownProgress())))
            }
        } else {
            val clickTick = clicker.isClickTick
            val facing = clickTick && isFacing(player.getEyePositionAt(feet), target)
            val usable = facing && !player.cannotAttackWithItem(player.mainHandItem, 0)
            if (clickTick && !usable) {
                dlog(
                    "CLICK-BLOCKED",
                    TpAuraLog.details(
                        "facing" to facing,
                        "itemCooldown" to player.cannotAttackWithItem(player.mainHandItem, 0),
                        "dist" to fmt(targetDist(target, feet)),
                        "holdLeft" to engagement.holdLeft,
                    ),
                )
            }
            if (usable) {
                clicker.click {
                    attackEntity(target, swingMode, keepSprint = true)
                    true
                }
                swing = true
            }
        }

        if (swing) {
            if (attackWithoutLooking) {
                attackEntity(target, swingMode, keepSprint = true)
                clicker.itemCooldown?.newCooldown()
            }
            engagement.attacked = true
            engagement.idleTicks = 0
            settlePendingHit("overwritten")
            awaitingHit = target
            hitWaitUntil = ticks + hitConfirm
            // Swallowed a little past the swing: the press that triggered it is handled by vanilla on the next
            // tick, and clearing the mute here let that press through to spend the cooldown anyway.
            suppressUntil = ticks + MANUAL_SUPPRESS_TAIL
            dlog(
                "SWING",
                TpAuraLog.details(
                    "target" to desc(target),
                    "dist" to fmt(targetDist(target, feet)),
                    "holdLeft" to engagement.holdLeft,
                ),
            )
            if (afterAttack == AfterAttack.RETURN && --engagement.holdLeft <= 0) {
                dlog("HOLD-EXPIRED", TpAuraLog.details("attacked" to engagement.attacked))
                leave("hold-expired")
            }
        } else if (++engagement.idleTicks > ATTACK_PATIENCE) {
            dlog("ATTACK-PATIENCE", TpAuraLog.details("waiting" to "cooldown"))
            leave("attack-patience")
        }
    }

    /** Routes again from where the server has the player, because the target moved. */
    private fun replan() {
        val target = engagement?.target ?: return leave("target-lost-replan")
        val origin = trip.position
        val goals = attackPositions(target, origin).ifEmpty {
            if (airTargets) attackPositions(target, origin, requireSupport = false) else emptyList()
        }
        if (goals.isEmpty()) {
            dlog(
                "REPLAN-NO-GOALS",
                TpAuraLog.details(
                    "target" to desc(target),
                    "origin" to fmt(origin),
                    "targetY" to fmt(target.position().y),
                ),
            )
            return leave("no-goals-on-replan")
        }

        dlog(
            "REPLAN",
            TpAuraLog.details("origin" to fmt(origin), "goals" to goals.size),
        )
        view.engaged = false
        requestRoute(origin, goals)
    }

    /** Ends the engagement and brings the server back to the client. */
    private fun leave(reason: String = "unspecified") {
        if (phase == Phase.RETURN) {
            return
        }

        dlog(
            "LEAVE",
            TpAuraLog.details(
                "reason" to reason,
                "phase" to phase,
                "client" to fmt(player.position()),
                "server" to fmt(trip.position),
                "gap" to fmt(player.position().distanceTo(trip.position.toMinecraft())),
            ),
        )
        // A hold of one swing leaves on the same tick the swing goes out; the hurt packet still has to travel
        // back. Settling here marked every one of those swings a miss before the answer could arrive.
        if (awaitingHit != null && ticks <= hitWaitUntil) {
            dlog("HIT-DEFERRED", TpAuraLog.details("left" to (hitWaitUntil - ticks)))
        } else {
            settlePendingHit("leave")
        }
        TpAuraHud.reason = reason
        view.engaged = false
        val origin = engagement?.origin
        val serverEnd = trip.position
        val home = player.position().toAgent()

        // `Stay` never comes home, so the advanced return has nothing to do there. The library's `undo` is the
        // default; the advanced path is only entered when it gave up, or when the debug switch forces it.
        val returning = afterAttack == AfterAttack.RETURN
        // `comeBack()` answers true as soon as the server is away, whether it finds a way home or lands the
        // client on the spot. Ask the same question it does, and take over when the answer is no.
        val libViable = returning && !debug.forceGoBack && libCanComeBack(serverEnd, home)
        val cameBack = if (libViable) trip.comeBack() else false
        if (!cameBack && returning && (debug.goBack || debug.forceGoBack)) {
            startAdvancedReturn(serverEnd, home)
        }
        if (phase != Phase.RETURN) {
            phase = if (cameBack) Phase.RETURN else Phase.IDLE
        }
        engagement = null
        hurtWaiting = false
        manualTriggered = false

        // A client that stays where the server put it after a setback has no way back: that is the state the
        // "it just flew to the target" reports describe, so it is worth a line of its own.
        if (phase == Phase.IDLE && origin != null) {
            val gap = player.position().distanceTo(origin.toMinecraft())
            if (gap > STRANDED_GAP) {
                dlog(
                    "STRANDED",
                    TpAuraLog.details(
                        "reason" to reason,
                        "gap" to fmt(gap),
                        "client" to fmt(player.position()),
                        "origin" to fmt(origin),
                    ),
                )
            }
        }
    }

    private fun fail(target: LivingEntity, reason: String = "unspecified") {
        sfx(sfx.routeFail, SFX_ROUTE_FAIL)
        dlog(
            "FAIL",
            TpAuraLog.details(
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
        val engagement = engagement
        val origin = engagement?.origin
        val destination = engagement?.route?.destination
        dlog(
            "REJECTED",
            TpAuraLog.details(
                "serverPos" to (TeleportManager.teleports.serverPosition?.let { fmt(it) } ?: "none"),
                "client" to fmt(player.position()),
                "state" to (trip.run?.state ?: "none"),
                "shape" to (engagement?.shape ?: "none"),
                "routeLen" to (engagement?.route?.let { fmt(it.length) } ?: "none"),
                "destination" to (destination ?: "none"),
                "distToOrigin" to (origin?.let { fmt(player.position().distanceTo(it.toMinecraft())) } ?: "none"),
                "isAway" to trip.isAway,
            ),
        )
        reset()
        view.invalid()
        restUntil = ticks + SETBACK_REST_TICKS
    }

    private fun reset() {
        trip.reset()
        returnLeg = ReturnLeg.NONE
        returnSearching = false
        phase = Phase.IDLE
        engagement = null
        hurtWaiting = false
        view.clear()
    }

    private fun isInReach(feet: Vec3, target: LivingEntity) =
        target.boundingBox.distanceToSqr(player.getEyePositionAt(feet)) <= Mth.square(reachTo(feet, target))

    /**
     * Ticks of target movement the route and the swing have to make up for: the measured flight, capped by
     * [PredictionValueGroup.maxLead], plus the round trip on the wire which is not capped.
     */
    private fun leadTicks(): Double =
        if (!prediction.enabled) 0.0 else flightAvg.coerceIn(MIN_LEAD, prediction.maxLead.toDouble())

    /** Where the target will be when a route planned now arrives, from the velocity sampled above. */
    private fun predictedBox(target: LivingEntity, lead: Double): AABB {
        if (lead <= 0.0) {
            return target.boundingBox
        }
        val velocity = aimTrack.velocity() ?: return target.boundingBox
        if (velocity.lengthSqr() < MIN_PREDICT_VELOCITY_SQ) {
            return target.boundingBox
        }

        // Local flight plus the round trip on the wire.
        val travel = lead + player.ping / 50.0

        // A circling target curves away from the straight lead: rotate the velocity by half the turn it makes
        // while we travel, so the aim lands on the arc instead of its tangent. A sharp corner is not an arc:
        // there the newest direction is all the truth there is and the turn must not shrink the lead.
        val omega = aimTrack.turn()
        val offset = if (abs(omega) > SHARP_TURN) {
            velocity.scale(travel)
        } else {
            val turn = (omega * travel * TURN_DAMP / 2.0).coerceIn(-MAX_TURN, MAX_TURN)
            rotateY(velocity, turn).scale(travel)
        }
        return target.boundingBox.move(offset.toMinecraft())
    }

    private fun predictedBox(target: LivingEntity): AABB = predictedBox(target, leadTicks())

    private fun rotateY(v: AgentVec3, angle: Double): AgentVec3 {
        val cos = cos(angle)
        val sin = sin(angle)
        return AgentVec3(v.x * cos - v.z * sin, v.y, v.x * sin + v.z * cos)
    }


    /** A swing that never got its hurt packet counts as a miss, which is what makes the rate meaningful. */
    private fun expireHitWait() {
        if (ticks <= hitWaitUntil) {
            return
        }
        settlePendingHit("timeout")
    }

    /**
     * Closes the swing still waiting for its hurt packet. Every swing has to end in exactly one result: a new
     * swing used to overwrite the pending one, and the run ended with 126 swings but only 11 results.
     */
    private fun settlePendingHit(reason: String) {
        val waiting = awaitingHit ?: return
        awaitingHit = null
        misses++
        TpAuraHud.onResult(false)
        sfx(sfx.hitFail, SFX_HIT_FAIL)
        dlog(
            "HIT-UNCONFIRMED",
            TpAuraLog.details("target" to desc(waiting), "reason" to reason, "rate" to hitRate()),
        )
    }

    private fun hitRate(): String {
        val total = hits + misses
        return if (total == 0) "n/a" else "${hits * 100 / total}% ($hits/$total)"
    }

    private fun msSince(started: Long): Long = (System.nanoTime() - started) / 1_000_000

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

    /**
     * What the server accepts: six blocks with a clear line of sight and three without it on 1.8–1.20.4; newer
     * servers read `entity_interaction_range`, which [reach] already reflects. The smaller budget wins.
     */
    private fun reachTo(feet: Vec3, target: LivingEntity): Double = when {
        sighted(feet, target) -> min(reach, SIGHTED_REACH)
        throughWall.enabled -> min(reach, throughWall.range.toDouble())
        // Without line of sight and without the switch, the server would drop the swing: not reachable at all.
        else -> 0.0
    }

    /** The server's own test: a ray from our eyes to the target's eyes, blocked by any collider. */
    private fun sighted(feet: Vec3, target: LivingEntity): Boolean {
        val eyes = target.position().add(0.0, target.eyeHeight.toDouble(), 0.0)
        return player.level().clip(
            ClipContext(
                player.getEyePositionAt(feet),
                eyes,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                player,
            ),
        ).type == HitResult.Type.MISS
    }

    /** Whether a swing could go out right now: no miss cooldown, the weapon's cooldown passed and usable. */
    private fun attackReady(): Boolean =
        clicker.canAttackNow && !player.cannotAttackWithItem(player.mainHandItem, 0)

    /**
     * The first tick from now a swing could go out, or null when the weapon is not ready inside the launch
     * window — the engagement then waits where it is instead of standing next to the target.
     */
    private fun readyAttackTick(): Int? = (1..LAUNCH_WINDOW).firstOrNull(clicker::canAttackAt)

    private fun cooldownProgress(): Double = clicker.itemCooldown?.cooldownProgress()?.toDouble() ?: 1.0

    /** One diagnostic line; off with `Debug → Log`. */
    private fun dlog(event: String, details: String = "") {
        if (debug.log) {
            TpAuraLog.log(ticks, event, details)
        }
    }

    private fun fmt(value: Number): String = String.format(java.util.Locale.ROOT, "%.2f", value.toDouble())

    private fun fmt(value: AgentVec3): String = "(${fmt(value.x)}, ${fmt(value.y)}, ${fmt(value.z)})"

    private fun fmt(value: Vec3): String = "(${fmt(value.x)}, ${fmt(value.y)}, ${fmt(value.z)})"

    private fun desc(target: LivingEntity): String = "#${target.id}:${target.name.string}"

    /** Server-truth distance, the same metric [isInReach] uses. */
    private fun targetDist(target: LivingEntity, feet: Vec3 = trip.position.toMinecraft()): Double =
        target.boundingBox.distanceToSqr(player.getEyePositionAt(feet)).let { Math.sqrt(it) }

    /** Whether the rotation the server has hits [target] from [eyes]. */
    /**
     * Spots along the path of the target, as the module this grew out of built them: the points are spread over
     * a span that reaches back as well as forward, because the server still has the target a tick or two behind
     * the picture the client draws. Neighbours stay within two blind reaches of each other, each point is tried
     * at its own height and one block above, and [DebugValueGroup.probeLeadStart] shifts the whole chain.
     */
    private fun probeSpots(target: LivingEntity, count: Int): List<AgentVec3> {
        val terrain = Terrain.live(world)
        val velocity = aimTrack.velocity()
        val speed = velocity?.length() ?: 0.0

        val spacing = 2.0 * BLIND_REACH * SPRAY_COVERAGE
        val span = debug.probeSpan.toDouble()
        val steps = if (speed * speed < MIN_PREDICT_VELOCITY_SQ) {
            1
        } else {
            min(max(1, ceil(speed * span / spacing).toInt()), max(1, count / 2))
        }
        val leadStart = debug.probeLeadStart.toDouble()

        val candidates = ArrayList<AgentVec3>(count * 2)
        for (step in 0..steps) {
            val lead = leadStart + span * step / steps
            val point = predictedBox(target, lead).center
            addProbe(candidates, terrain, point)
            addProbe(candidates, terrain, point.add(0.0, 1.0, 0.0))
        }

        val spots = ArrayList<AgentVec3>(count)
        val seen = HashSet<BlockPos>()
        for (feet in candidates) {
            if (!seen.add(BlockPos.containing(feet))) {
                continue
            }
            spots += feet
            if (spots.size >= count) {
                break
            }
        }

        if (debug.log) {
            val eyes = player.position()
            dlog(
                "SPRAY-PLAN",
                TpAuraLog.details(
                    "speed" to fmt(speed),
                    "samples" to aimTrack.size,
                    "steps" to steps,
                    "leadStart" to fmt(leadStart),
                    "leads" to (0..steps).joinToString(",") { fmt(leadStart + span * it / steps) },
                    "spots" to spots.size,
                    "max" to count,
                    "probes" to spots.joinToString(",") {
                        fmt(sqrt(target.boundingBox.distanceToSqr(it.toMinecraft())))
                    },
                    "eyes" to fmt(eyes),
                ),
            )
        }
        return legalHops(spots)
    }

    /** The block the point falls in, standing if it can, hovering when the body fits and nothing carries it. */
    private fun addProbe(spots: MutableList<AgentVec3>, terrain: Terrain, point: Vec3) {
        val block = BlockPos.containing(point.toAgent())
        val feet = terrain.feet(block) ?: run {
            val box = terrain.box(block.x.toDouble(), block.y.toDouble(), block.z.toDouble())
            if (!terrain.isClear(box)) {
                return
            }
            AgentVec3(block.x + 0.5, block.y.toDouble(), block.z + 0.5)
        }
        spots += feet
    }

    /** Ordered from the server position outwards: every hop after the first starts where the last one ended. */
    private fun legalHops(spots: List<AgentVec3>): List<AgentVec3> {
        var cursor = trip.position
        val remaining = spots.toMutableList()
        val ordered = ArrayList<AgentVec3>(spots.size)
        while (remaining.isNotEmpty()) {
            val next = remaining.minByOrNull { it.distanceToSqr(cursor) } ?: break
            ordered += next
            remaining.remove(next)
            cursor = next
        }
        return ordered
    }


    /**
     * One spray round: every spot is sent as its own position packet with the swing right behind it, all inside
     * this tick. The server judges each attack from the position it just received, so the round covers the
     * target's whole path instead of one point on it. The round counts as a single swing.
     */
    private fun sprayAt(target: LivingEntity): Boolean {
        val engagement = liveEngagement ?: return false
        val spots = probeSpots(target, prediction.maxProbes)
        if (spots.isEmpty()) {
            dlog("SPRAY-NONE", TpAuraLog.details("dist" to fmt(targetDist(target))))
            return false
        }

        val terrain = Terrain.live(world)
        lastProbeBox = predictedBox(target).toAgent()
        lastProbeSpots = spots
        probesUntil = ticks + PROBE_PREVIEW_TICKS
        TpAuraHud.spraySpots = spots.size
        TpAuraHud.sprayMax = prediction.maxProbes
        for ((index, spot) in spots.withIndex()) {
            val onGround = terrain.isSupported(spot)
            dlog(
                "SPRAY",
                TpAuraLog.details(
                    "i" to "${index + 1}/${spots.size}",
                    "spot" to fmt(spot),
                    "onGround" to onGround,
                    "targetPos" to fmt(target.position()),
                    "vel" to fmt(aimTrack.velocity()?.length() ?: 0.0),
                    "dist" to fmt(targetDist(target)),
                ),
            )
            sendPacketSilently(
                ServerboundMovePlayerPacket.Pos(
                    spot.x,
                    spot.y,
                    spot.z,
                    onGround,
                    player.horizontalCollision,
                ),
            )
            attackEntity(target, swingMode, keepSprint = true)
        }

        engagement.attacked = true
        engagement.idleTicks = 0
        settlePendingHit("overwritten")
        awaitingHit = target
        hitWaitUntil = ticks + hitConfirm
        suppressUntil = ticks + MANUAL_SUPPRESS_TAIL
        dlog(
            "SPRAY-DONE",
            TpAuraLog.details(
                "probes" to spots.size,
                "dist" to fmt(targetDist(target)),
                "holdLeft" to engagement.holdLeft,
            ),
        )
        if (afterAttack == AfterAttack.RETURN && --engagement.holdLeft <= 0) {
            dlog("HOLD-EXPIRED", TpAuraLog.details("attacked" to engagement.attacked, "spray" to true))
            leave("hold-expired")
        }
        return true
    }

    private fun isFacing(eyes: Vec3, target: LivingEntity): Boolean {
        val box = target.boundingBox.inflate(target.pickRadius.toDouble())
        if (box.contains(eyes)) {
            return true
        }

        val end = eyes.fma(reach + REACH_MARGIN, RotationManager.serverRotation.directionVector)
        val hit = box.clip(eyes, end).orElse(null) ?: return false
        return hasLineOfSight(eyes, hit)
    }

    /**
     * Spots around [target] whose eyes see it within reach, closest to [from] first. With [requireSupport]
     * false the library also returns feet in the air, which is what a flying target needs — the swing only
     * needs the player to be there, not a floor under them.
     */
    private fun attackPositions(target: LivingEntity, from: AgentVec3, requireSupport: Boolean = true) =
        attackPositions(predictedBox(target).toAgent(), from, requireSupport, target)

    /**
     * Spots around [box] the swing would connect from: the ones with a clear line of sight first, then — with
     * [ThroughWall] on — the ones just behind a wall. The library's own search only ever returns the first kind,
     * which is why a target inside a box could not be attacked at range at all.
     */
    private fun attackPositions(
        box: AgentAABB,
        from: AgentVec3,
        requireSupport: Boolean = true,
        target: LivingEntity? = null,
    ): List<BlockPos> {
        val terrain = Terrain.live(world)
        val visible = terrain.reachPositions(
            box,
            // The spot search stays inside the blind reach: a spot further out is legal to swing from, but a
            // moving target leaves it before the swing lands, which is how every attack ended up out of range.
            min(reach, BLIND_REACH) - TARGET_MOVEMENT_MARGIN,
            from,
            requiresSupport = requireSupport,
        )
        if (!throughWall.enabled) {
            return visible
        }
        val blind = blindSpots(terrain, box, from, throughWall.range.toDouble(), requireSupport)
        // The library returns at most eight positions, sorted by distance to the player — so they are all on
        // the side the player approaches from, and a side preference has nothing to choose from. Scan the
        // preferred side ourselves and put those in front.
        val preferred = if (target == null || landingSide == LandingSide.ANY) {
            emptyList()
        } else {
            sideSpots(terrain, box, min(reach, BLIND_REACH) - TARGET_MOVEMENT_MARGIN, facingOf(target), requireSupport)
        }
        val all = (preferred + visible + blind).distinct().take(MAX_GOALS)
        if (target == null || landingSide == LandingSide.ANY) {
            return all
        }

        // Prefer the side asked for, then the spot closest to where the target actually is: behind and flank
        // spots are harder for it to turn into a swing of its own, and the reach is the same either way.
        val facing = facingOf(target)
        val center = box.center
        val ranked = all.sortedWith(
            compareBy(
                { position -> sideTier(feetOrHover(terrain, position), center, facing) },
                { position -> feetOrHover(terrain, position).distanceToSqr(center) },
            ),
        )

        // The side is a preference, never a filter: when nothing is on the requested side the list simply
        // starts one tier down. Say so once per change, so a run does not look like it ignored the setting.
        val tiers = ranked.map { position -> sideTier(feetOrHover(terrain, position), center, facing) }
        val best = tiers.firstOrNull()
        if (best != null && best != lastSideTier) {
            lastSideTier = best
            dlog(
                "SIDE-TIER",
                TpAuraLog.details(
                    "side" to landingSide,
                    "best" to best,
                    "spots" to ranked.size,
                    "tiers" to tiers.groupingBy { it }.eachCount().toSortedMap(),
                    "facing" to fmt(facing.toMinecraft()),
                ),
            )
        }
        return ranked
    }

    /** Standable or hovering spots inside [radius] of [box] that sit on the preferred side of the target. */
    private fun sideSpots(
        terrain: Terrain,
        box: AgentAABB,
        radius: Double,
        facing: AgentVec3,
        requireSupport: Boolean,
    ): List<BlockPos> {
        val center = box.center
        val reachRadius = BlockPos.containing(center)
        val r = ceil(radius).toInt()
        val eyeHeight = player.eyeHeight.toDouble()
        val spots = ArrayList<BlockPos>()

        for (dx in -r..r) {
            for (dz in -r..r) {
                for (dy in -r..r) {
                    val position = reachRadius.offset(dx, dy, dz)
                    val feet = terrain.feet(position) ?: if (requireSupport) continue else feetOrHover(terrain, position)
                    if (!terrain.isClear(terrain.box(feet))) {
                        continue
                    }
                    if (terrain.box(feet).intersects(box)) {
                        continue
                    }
                    val eyes = feet.add(0.0, eyeHeight, 0.0)
                    if (box.distanceToSqr(eyes) > radius * radius) {
                        continue
                    }
                    if (sideTier(feet, center, facing) != 0) {
                        continue
                    }
                    spots += position
                }
            }
        }
        return spots
    }

    /**
     * Feet of a candidate spot. `terrain.feet` only answers for spots with a floor, and every candidate for a
     * flying target is a hovering one — that is why the side preference silently did nothing there.
     */
    private fun feetOrHover(terrain: Terrain, position: BlockPos): AgentVec3 =
        terrain.feet(position) ?: AgentVec3(position.x + 0.5, position.y.toDouble(), position.z + 0.5)

    /** The direction the target's body faces, which is what "behind" is measured against. */
    private fun facingOf(target: LivingEntity): AgentVec3 =
        AgentVec3.directionFromRotation(0f, target.yBodyRot)

    /** 0 for the side asked for, 2 for the opposite one, 1 in between. */
    private fun sideTier(feet: AgentVec3, center: AgentVec3, facing: AgentVec3): Int {
        val toFeet = AgentVec3(feet.x - center.x, 0.0, feet.z - center.z)
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

    /** Feet positions whose eyes are within [range] of [box] — through walls on purpose, that is the point. */
    private fun blindSpots(
        terrain: Terrain,
        box: AgentAABB,
        from: AgentVec3,
        range: Double,
        requireSupport: Boolean,
    ): List<BlockPos> {
        val center = BlockPos.containing(box.center)
        val radius = ceil(range).toInt()
        val eyeHeight = player.eyeHeight.toDouble()
        val spots = ArrayList<BlockPos>()

        for (dx in -radius..radius) {
            for (dz in -radius..radius) {
                for (dy in -radius - EYE_DEPTH..radius) {
                    val position = center.offset(dx, dy, dz)
                    val feet = terrain.feet(position) ?: continue
                    if (requireSupport && !terrain.isSupported(feet)) {
                        continue
                    }
                    if (terrain.box(feet).intersects(box)) {
                        continue
                    }
                    val eyes = feet.add(0.0, eyeHeight, 0.0)
                    if (box.distanceToSqr(eyes) <= range * range) {
                        spots += position
                    }
                }
            }
        }
        // Closest to the target first: behind a wall the only thing that decides a hit is how near the swing
        // lands, and the spot nearest to where we stand is usually on the wrong side of the wall.
        return spots.sortedWith(
            compareBy(
                { terrain.feet(it)?.let { feet -> box.distanceToSqr(feet.add(0.0, eyeHeight, 0.0)) } ?: Double.MAX_VALUE },
                { terrain.feet(it)?.distanceToSqr(from) ?: Double.MAX_VALUE },
            ),
        )
    }

    private const val REACH_MARGIN = 0.1
    private const val BLIND_REACH = 3.0
    private const val EYE_DEPTH = 2
    private const val SIGHTED_REACH = 6.0

    /** Goals tried for a direct route; the nearest ones first, so a blocked direction does not blind the rest. */
    private const val MAX_DIRECT_GOALS = 6

    /** Cap on the candidate list: the search takes it whole, and a long list only costs planning time. */
    private const val MAX_GOALS = 16

    /** Heights above the higher endpoint tried when the straight line is blocked. */
    /** Highest climb tried above the taller endpoint, one block at a time. */
    private const val MAX_LIFT = 16

    /** A straight line is only used when the goal is not higher than this; above it the climb is explicit. */
    private const val MAX_STRAIGHT_RISE = 1.5

    /** Height gains tried after stepping aside: a side step alone often clears what the line above the head does not. */

    /** Sideways steps tried before climbing: eight directions at three distances. */
    private val SIDE_OFFSETS: List<Pair<Double, Double>> = buildList {
        for (distance in doubleArrayOf(2.0, 4.0, 6.0)) {
            add(distance to 0.0)
            add(-distance to 0.0)
            add(0.0 to distance)
            add(0.0 to -distance)
            add(distance to distance)
            add(distance to -distance)
            add(-distance to distance)
            add(-distance to -distance)
        }
    }

    /** Goals the lateral shapes are tried against; the straight shape covers all of them. */
    private const val LATERAL_GOALS = 3

    /** Shortest leg worth splitting a search into, and how many candidates a relay is picked from. */
    private const val SEGMENT_MIN_LENGTH = 16.0
    private const val SEGMENT_TRIES = 5

    /** A failed leg is retried with the relay brought closer by this factor, at most this often. */
    private const val LEG_BACKOFF = 0.6
    private const val MAX_LEG_ATTEMPTS = 3

    /** How far the target may drift while a search runs, and how often the search may be restarted for it. */
    private const val RETARGET_DRIFT = 1.5
    private const val RETARGET_TICKS = 1.5
    private const val RETARGET_COOLDOWN = 4
    private const val MAX_RETARGETS = 3

    /** Cosine beyond which a spot counts as in front of or behind the target. */
    private const val SIDE_DOT = 0.5

    /** Probe chain of the spray: how far ahead and behind it reaches, and how densely it covers the path. */
    private const val PROBE_TICKS = 2.0
    private const val PROBE_BACK = 2.0
    private const val SPRAY_COVERAGE = 0.9



    /** Probe preview lifetime and colours. */
    private const val PROBE_PREVIEW_TICKS = 10
    private const val MANUAL_SUPPRESS_TICKS = 60
    private const val MANUAL_SUPPRESS_TAIL = 3
    private val PROBE_FILL = Color4b(120, 200, 255, 40)
    private val PROBE_OUTLINE = Color4b(120, 200, 255, 160)
    private val PROBE_SPOT_FILL = Color4b(93, 202, 165, 60)
    private val PROBE_SPOT_OUTLINE = Color4b(93, 202, 165, 200)

    /** HUD colours, opaque ARGB. */
    private const val HUD_TEXT = -1
    private const val HUD_DIM = -5592406
    private const val HUD_WARN = -22016

    /** Step length the search's own route is rebuilt with, so a compacted corner cannot become a long line. */
    private const val AGENT_STEP = 2.0
    private const val AGENT_STEP_MAX = 3.0

    /** Position samples kept per target, and the distances a retreat point is looked for at. */
    private const val MAX_TRACK = 12
    private val RETREAT_DISTANCES = doubleArrayOf(8.0, 10.0, 12.0, 16.0)
    private const val RETREAT_DIRECTIONS = 8
    private const val RETREAT_SCAN = 8
    private const val RETREAT_SCAN_Y = 2

    /** Below this distance the server never left, so there is nothing to bring back. */
    private const val RETURN_HOME_GAP = 1.0

    /** How far a retreat spot must be from where we stand to count as breaking contact. */
    private const val RETREAT_MIN_GAP = 4.0

    /** Hint priorities: the more surprising the stage, the louder its claim on the tick. */
    private const val SFX_PATH = 10
    private const val SFX_TELEPORT = 20
    private const val SFX_HIT_FAIL = 30
    private const val SFX_ROUTE_FAIL = 35
    private const val SFX_HIT_OK = 40
    private const val SFX_SETBACK = 50

    /** The entity event older servers send for a hurt. */
    private const val ENTITY_EVENT_HURT = 2

    /** Prediction tuning, same numbers as the module this grew out of. */
    private const val MIN_PREDICT_VELOCITY_SQ = 1.0E-4
    private const val SPEED_WINDOW = 3
    private const val MAX_SAMPLES = 8
    private const val TURN_DAMP = 0.9
    private const val SHARP_TURN = 0.35
    private const val MAX_TURN = 0.7
    private const val MIN_LEAD = 1.0

    /** The measured flight is a rolling average, so one slow route cannot swing the aim. */
    private const val FLIGHT_EMA_KEEP = 0.6
    private const val FLIGHT_EMA_TAKE = 0.4
    /** Spacing of the legality samples along a route, and how many offending steps are spelled out. */
    private const val CHECK_SAMPLE = 0.5
    private const val ROUTE_ILLEGAL_LOGS = 3

    /** A rise smaller than this, on a descending route, is `appendDrop`'s bounce rather than a climb. */
    private const val BOUNCE_RISE = 0.25

    /** Steps spelled out per dispatch before the log switches to a summary line. */
    private const val MAX_STEP_LOGS = 40

    /** Sample spacing of the planning check, how often a route may be bent, and how far a detour goes. */
    private const val ILLEGAL_SAMPLE = 0.25
    private const val MAX_DETOURS = 3
    private val DETOUR_LIFTS = doubleArrayOf(3.0, 6.0)
    private val DETOUR_SIDES = doubleArrayOf(2.0, 4.0)

    /** Distance from the origin past which a client left behind is reported as stranded. */
    private const val STRANDED_GAP = 8.0

    /** Ticks ahead the launch gate looks for a ready weapon. */
    private const val LAUNCH_WINDOW = 5

    /** Ticks in ATTACK without a swing before the engagement gives up. */
    private const val ATTACK_PATIENCE = 40
    private const val TARGET_MOVEMENT_MARGIN = 0.4
    private const val ROUTE_DETOUR = 2.0
    private const val MAX_WAIT = 10
    private const val MAX_RETRIES = 2
    private const val FAIL_REST_TICKS = 10
    private const val SETBACK_REST_TICKS = 20
    private const val UNREACHABLE_TICKS = 40

}
