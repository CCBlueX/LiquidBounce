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
package net.ccbluex.liquidbounce.features.module.modules.player.gamebot.games

import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.PlayerTickEvent
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.features.KillAuraFightBot
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.ModuleFly
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.modes.rewound.FlyRewound20261112
import net.ccbluex.liquidbounce.features.module.modules.player.cheststealer.ModuleChestStealer
import net.ccbluex.liquidbounce.features.module.modules.player.cheststealer.features.FeatureChestAura
import net.ccbluex.liquidbounce.features.module.modules.player.gamebot.Bridge
import net.ccbluex.liquidbounce.features.module.modules.player.gamebot.Chests
import net.ccbluex.liquidbounce.features.module.modules.player.gamebot.Game
import net.ccbluex.liquidbounce.features.module.modules.player.gamebot.MapRecorder
import net.ccbluex.liquidbounce.features.module.modules.player.gamebot.ModuleGameBot
import net.ccbluex.liquidbounce.features.module.modules.player.gamebot.ModuleGameBot.report
import net.ccbluex.liquidbounce.features.module.modules.player.gamebot.Opponents
import net.ccbluex.liquidbounce.features.module.modules.player.gamebot.Outnumbered
import net.ccbluex.liquidbounce.features.module.modules.player.gamebot.Plan
import net.ccbluex.liquidbounce.features.module.modules.player.gamebot.Setbacks
import net.ccbluex.liquidbounce.features.module.modules.player.gamebot.VoidSafety
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.ccbluex.liquidbounce.utils.aiming.RotationsValueGroup
import net.ccbluex.liquidbounce.utils.entity.doesNotCollideBelow
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.CRITICAL_MODIFICATION
import net.ccbluex.liquidbounce.utils.math.allEmpty
import net.ccbluex.liquidbounce.utils.math.horizontalDistanceTo
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.ccbluex.liquidbounce.utils.movement.getDirectionalInputTowards
import net.ccbluex.liquidbounce.utils.movement.inputFrameYaw
import net.ccbluex.liquidbounce.utils.navigation.Navigator
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import java.util.Locale
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Waits in the cage, loots the own island, then walks or bridges to what [Plan] picks: chests while under-equipped,
 * players it outgears, the middle once nothing is left. Walking never leaves the ground into the void; gaps are
 * bridged with Scaffold, or flown over, and [VoidSafety] catches whatever walks off anyway.
 */
@Suppress("TooManyFunctions", "LargeClass", "ReturnCount")
internal object GameBotSkyWars : Game("SkyWars", "/play solo_normal") {

    private val islandRadius by int("IslandRadius", 14, 6..32, "blocks")
    private val lootTime by int("LootTime", 45, 5..180, "s")
    private val chestTime by int("ChestTime", 12, 3..60, "s")
    private val fightRange by float("FightRange", 6f, 3f..12f)
    private val travelRange by int("TravelRange", 96, 16..256, "blocks")
    private val visitMiddle by boolean("VisitMiddle", true)
    private val useFightBot by boolean("FightBot", true)

    /** Plays a round that is already on when turned on, instead of waiting for the next cage. */
    private val joinRunning by boolean("JoinRunning", false)

    /**
     * Turns Fly on, whatever its mode, to get up to a player standing higher; Rewound's only works after the cages and
     * flies to the height of the goal by itself.
     */
    private object FlyAcross : ToggleableValueGroup(this, "FlyAcross", true) {
        val maxDistance by int("MaxDistance", 30, 8..96, "blocks")
        val maxTime by int("MaxTime", 10, 1..30, "s")
    }

    /** Turns Fly on to get back to the last ground after a fall into the void no block could catch. */
    private val flyRescue by boolean("FlyRescue", true)

    init {
        tree(FlyAcross)
    }

    private val rotations = tree(RotationsValueGroup(this))
    override val navigator = Navigator(this, this, rotations, allowPlace = true) { ModuleScaffold.running }
    private val voidSafety = VoidSafety(this, navigator, ::activitySituation, ::flyBack)
    private val bridge = Bridge(this, navigator)

    private enum class Phase { IDLE, CAGE, LOOT, TRAVEL, FIGHT, END }

    /** Where to go; an [opponent] is followed, chests within [lootRadius] of [point] are looted on arrival. */
    private class Destination(
        val label: String,
        val point: Vec3,
        val radius: Double,
        val opponent: Player? = null,
        val lootRadius: Double = 0.0,
    ) {
        val goal: Vec3
            get() = opponent?.position() ?: point
    }

    private class Flight(
        val origin: Vec3,
        var goal: Vec3,
        val startedAt: Int,
        val startY: Double,
        val maxFall: Double = FLY_FALL,
    ) {
        var airborne = false
        var crossedVoid = false
        val setbacksAtStart = Setbacks.count

        /** The edge point the start walks off from, null once past it or without one. */
        var runUp: Vec3? = null
    }

    private var phase = Phase.IDLE
    private var ticks = 0

    private var startHeard = false
    private var enclosedTicks = 0
    private var cageY = 0.0
    private var freeTicks = 0

    private val chests = Chests(this, rotations, SCAN_CHUNKS)
    private val plan = Plan(chests)
    private var middle: Vec3? = null
    private var middleDone = false
    private val unreachableUntil = HashMap<String, Int>()

    private var lootCenter: Vec3? = null
    private var lootRadius = 0.0
    private var lootUntil = 0
    private var chest: BlockPos? = null
    private var chestSince = 0

    private var destination: Destination? = null
    private var bestDistance = Double.MAX_VALUE
    private var progressAt = 0
    private var strandedTicks = 0

    private var opponent: Player? = null
    private var calmTicks = 0
    private var ownsFightBot = false

    private var flight: Flight? = null
    private var flightEndedAt = Int.MIN_VALUE / 2
    private var lastFlightSetbacks = 0

    /** Stage of the Rewound fly the flight was last seen in, and the ticks it was seen in it. */
    private var flightStage: FlyRewound20261112.Stage? = null
    private var flightStageTicks = 0

    /** When the way to the destination is looked at next, and what was decided last, so it is logged once. */
    private var travelCheckAt = 0
    private var travelDecision: String? = null

    private var walkGoal: Vec3? = null
    private var walkRadius = 1.0

    override val isPlaying: Boolean
        get() = phase != Phase.IDLE && phase != Phase.END

    override val hasStarted: Boolean
        get() = phase == Phase.LOOT || phase == Phase.TRAVEL || phase == Phase.FIGHT

    override val isFighting: Boolean
        get() = phase == Phase.FIGHT

    override val isOpeningChest: Boolean
        get() = chests.isOpening

    override fun speedHold(): String? = when {
        flight != null -> "fly"
        bridge.isActive || navigator.isBuilding -> "bridge"
        voidSafety.isFalling -> "fall"
        voidSafety.isNearEdge -> "void edge"
        else -> null
    }

    override fun startAnnounced() {
        if (phase == Phase.IDLE || phase == Phase.CAGE) {
            startHeard = true
        }
    }

    override fun end() {
        stopPlaying()
        enter(Phase.END, "round over")
    }

    override fun enable() {
        forgetWorld()
        phase = Phase.IDLE
    }

    override fun disable() {
        stopPlaying()
        navigator.reset()
        phase = Phase.IDLE
    }

    @Suppress("unused")
    private val worldChangeHandler = handler<WorldChangeEvent> {
        stopPlaying()
        forgetWorld()
        enter(Phase.IDLE, "world change")
    }

    @Suppress("unused")
    private val tickHandler = handler<PlayerTickEvent> {
        ticks++
        if (ticks % SCAN_TICKS == 0) {
            chests.scan()
        }

        walkGoal = null
        if (isPlaying && phase != Phase.CAGE && (player.isSpectator || player.abilities.mayfly)) {
            ModuleGameBot.finish(won = false, reason = "spectator")
            return@handler
        }

        // The server keeps setting every move of a stuck player back; stand still until it takes a clean one.
        if (Setbacks.isHolding) {
            flight?.let { stopFlight("setback loop") }
            navigator.halt()
            return@handler
        }

        flight?.let { return@handler fly(it) }
        if (voidSafety.isFalling) {
            return@handler
        }

        when (phase) {
            Phase.IDLE -> waitForCage()
            Phase.CAGE -> waitForDrop()
            Phase.LOOT -> loot()
            Phase.TRAVEL -> travel()
            Phase.FIGHT -> fight()
            Phase.END -> Unit
        }
    }

    @Suppress("unused")
    private val movementHandler = handler<MovementInputEvent>(priority = CRITICAL_MODIFICATION) { event ->
        if (Setbacks.isHolding) {
            event.directionalInput = DirectionalInput.NONE
            event.jump = false
            event.sneak = false
            return@handler
        }

        flight?.let {
            // Walking on through the charge and the damage stage is what launches the fly off the edge.
            val aim = it.runUp ?: it.goal
            event.directionalInput = getDirectionalInputTowards(aim.subtract(player.position()), inputFrameYaw())
            // The jump key would override the height Rewound's fly heads for.
            event.jump = !fliesToHeight() && it.goal.y > player.y + FLY_RISE
            event.sneak = false
            return@handler
        }

        val goal = walkGoal
        if (goal == null) {
            navigator.halt()
            return@handler
        }

        if (isContainerOpen()) {
            navigator.halt()
            event.directionalInput = DirectionalInput.NONE
            event.jump = false
            return@handler
        }

        navigator.navigate(event, goal, walkRadius)
    }

    private fun waitForCage() {
        if (isEnclosed()) {
            if (++enclosedTicks >= CAGE_TICKS) {
                cageY = player.y
                enter(Phase.CAGE, "caged")
            }
            return
        }

        enclosedTicks = 0
        if ((startHeard || joinRunning) && player.onGround()) {
            start(if (startHeard) "start announced" else "joined running round")
        }
    }

    private fun waitForDrop() {
        if (isEnclosed()) {
            cageY = player.y
            freeTicks = 0
            return
        }

        if (!player.onGround()) {
            return
        }

        freeTicks++
        if (player.y < cageY - DROP_HEIGHT || startHeard || freeTicks >= FREE_TICKS) {
            start("cage dropped")
        }
    }

    private fun start(reason: String) {
        startHeard = false
        ModuleGameBot.roundStarted()
        startLoot(player.position(), islandRadius.toDouble(), reason)
    }

    private fun startLoot(center: Vec3, radius: Double, reason: String) {
        lootCenter = center
        lootRadius = radius
        lootUntil = ticks + lootTime * TICKS_PER_SECOND
        chest = null
        enter(Phase.LOOT, reason)
        if (!ModuleChestStealer.running || !FeatureChestAura.running) {
            report("warning ChestStealer or its Aura is off, chests are not opened")
        }
    }

    private fun loot() {
        opponentInReach()?.let { return startFight(it, "opponent near") }
        if (isContainerOpen()) {
            chestSince = ticks
            return
        }

        chests.noteLooted()
        val current = chest?.takeIf(chests::isUnopened) ?: nextChest()
        if (current == null || ticks >= lootUntil) {
            enter(Phase.TRAVEL, if (current == null) "chests looted" else "loot time over")
            return
        }

        if (current != chest) {
            chest = current
            chestSince = ticks
            strandedTicks = 0
        }

        strandedTicks = if (navigator.isStranded) strandedTicks + 1 else 0
        if (ticks - chestSince > chestTime * TICKS_PER_SECOND || strandedTicks > STRANDED_TICKS) {
            chests.skip(current)
            chest = null
            return
        }

        if (chests.isInReach(current)) {
            chests.open(current)
            return
        }
        walkGoal = Vec3.atCenterOf(current)
        walkRadius = CHEST_REACH
    }

    private fun nextChest(): BlockPos? {
        val center = lootCenter ?: return null
        val position = player.position()
        return chests.unopened()
            .filter { isWithin(it, center, lootRadius, LOOT_HEIGHT) }
            .minByOrNull { Vec3.atCenterOf(it).distanceToSqr(position) }
    }

    private fun travel() {
        opponentInReach()?.let { return startFight(it, "opponent near") }

        val position = player.position()
        val nearby = chests.unopened().firstOrNull { isWithin(it, position, DETOUR_RANGE, DETOUR_HEIGHT) }
        if (nearby != null) {
            startLoot(position, DETOUR_RADIUS, "chest nearby")
            return
        }

        if (ticks % PICK_TICKS == 0 || destination?.let(::isValid) != true) {
            pickDestination()
        }

        val destination = destination ?: return
        val goal = destination.goal
        if (position.closerThan(goal, destination.radius + ARRIVAL_MARGIN)) {
            arrive(destination)
            return
        }

        if (fliesInstead(destination, goal)) {
            startFlight(goal, destination)
            return
        }

        walkGoal = goal
        walkRadius = destination.radius
        checkProgress(destination, goal)
    }

    /**
     * Whether to fly to [goal] rather than walk and bridge: over a void stretch longer than [VOID_CROSSING], or up
     * to a player standing more than [HIGHER_GROUND] higher. Walking stays the way while Fly is off, the flight path
     * is blocked, the last flight was a moment ago or got set back, or health is too low for the damage the fly takes.
     */
    private fun fliesInstead(destination: Destination, goal: Vec3): Boolean {
        if (ticks < travelCheckAt || !player.onGround() || ModuleFly.enabled) {
            return false
        }
        travelCheckAt = ticks + TRAVEL_CHECK_TICKS

        val void = longestVoid(goal)
        val climb = goal.y - player.y
        val wants = when {
            void > VOID_CROSSING -> "void=${formatDistance(void)}"
            destination.opponent != null && climb > HIGHER_GROUND -> "higher=${formatDistance(climb)}"
            else -> return false
        }

        val cooldown = if (lastFlightSetbacks > 0) SETBACK_COOLDOWN_TICKS else FLY_COOLDOWN_TICKS
        val blocker = when {
            !FlyAcross.running -> "fly_off"
            player.health < MIN_FLY_HP -> "hp"
            ticks < flightEndedAt + cooldown -> "cooldown"
            player.position().horizontalDistanceTo(goal) > FlyAcross.maxDistance -> "distance"
            !canFlyTo(goal) -> "path"
            else -> null
        }

        val way = if (blocker == null) "fly" else "bridge"
        val key = "${destination.label} $way ${blocker.orEmpty()}"
        if (key != travelDecision) {
            travelDecision = key
            report("travel via $way reason=${blocker ?: wants} $wants to=${destination.label}")
        }
        return blocker == null
    }

    /** The longest stretch, in blocks, of the straight way to [goal] with nothing below to land on. */
    private fun longestVoid(goal: Vec3): Double {
        val from = player.position()
        val distance = from.horizontalDistanceTo(goal)
        val samples = ceil(distance / VOID_SAMPLE_SPACING).toInt()
        val step = distance / samples.coerceAtLeast(1)
        var run = 0.0
        var longest = 0.0
        for (sample in 1..samples) {
            val t = sample.toDouble() / samples
            val x = from.x + (goal.x - from.x) * t
            val y = from.y + (goal.y - from.y) * t
            val z = from.z + (goal.z - from.z) * t
            if (isVoidBelow(x, y + 1.0, z)) {
                run += step
                longest = max(longest, run)
            } else {
                run = 0.0
            }
        }
        return longest
    }

    /** Nothing to land on within [VOID_DEPTH] below [top] at [x] [z]. */
    private fun isVoidBelow(x: Double, top: Double, z: Double): Boolean {
        val column = AABB(x - HALF_WIDTH, top - VOID_DEPTH, z - HALF_WIDTH, x + HALF_WIDTH, top, z + HALF_WIDTH)
        return world.getBlockCollisions(player, column).allEmpty()
    }

    /**
     * Where a flight start walks off: [RUN_UP_PAST] beyond the void edge toward [goal], along a way the body passes
     * without a wall, turning aside up to [RUN_UP_OFFSETS] when straight on is blocked; null without such an edge
     * within [RUN_UP_RANGE], as when the goal lies on the same ground.
     */
    private fun runUpPoint(goal: Vec3): Vec3? {
        val from = player.position()
        val toward = atan2(goal.z - from.z, goal.x - from.x)
        for (offset in RUN_UP_OFFSETS) {
            val angle = toward + Math.toRadians(offset)
            val dx = cos(angle)
            val dz = sin(angle)
            var distance = RUN_UP_SAMPLE
            while (distance <= RUN_UP_RANGE) {
                val point = Vec3(from.x + dx * distance, from.y, from.z + dz * distance)
                if (!world.noCollision(player, player.dimensions.makeBoundingBox(point))) {
                    break
                }
                if (isVoidBelow(point.x, point.y, point.z)) {
                    val past = point.add(dx * RUN_UP_PAST, 0.0, dz * RUN_UP_PAST)
                    return past.takeIf { world.noCollision(player, player.dimensions.makeBoundingBox(it)) }
                }
                distance += RUN_UP_SAMPLE
            }
        }
        return null
    }

    private fun isValid(destination: Destination): Boolean {
        val opponent = destination.opponent
        return isReachable(destination.label) && (opponent == null || Opponents.isOpponent(opponent))
    }

    /** What [plan] picks among the players on ground and the unopened chests; the middle once it picks nothing. */
    private fun pickDestination() {
        val range = travelRange.toDouble()
        val choice = plan.next(
            Opponents.around().filter {
                !it.doesNotCollideBelow() && it.distanceTo(player) <= range && isReachable(playerLabel(it))
            },
            chests.unopened().filter { isReachable(chests.label(it)) },
            armed = hasWeapon,
            canExplore = false,
        )
        val next = when (choice) {
            is Plan.Hunt -> Destination(
                choice.label, choice.opponent.position(), OPPONENT_RADIUS, opponent = choice.opponent
            )
            is Plan.Loot -> Destination(
                choice.label, Vec3.atCenterOf(choice.chest), CHEST_REACH, lootRadius = DETOUR_RADIUS
            )
            is Plan.Hold -> null
            Plan.Seek, null -> middleDestination()
        }
        if (next?.label != destination?.label) {
            val distance = next?.let { formatDistance(player.position().distanceTo(it.goal)) }
            report(if (next == null) "target none" else "target ${next.label} distance=$distance")
            MapRecorder.event(
                "target", "label" to next?.label, "goal" to next?.goal,
                "distance" to next?.let { player.position().distanceTo(it.goal) },
            )
            resetProgress()
        }
        destination = next
    }

    private fun middleDestination(): Destination? {
        if (!visitMiddle || middleDone || !isReachable(MIDDLE)) {
            return null
        }

        val point = middle ?: findMiddle()?.also { middle = it } ?: return null
        return Destination(MIDDLE, point, MIDDLE_RADIUS, lootRadius = islandRadius.toDouble())
    }

    /** The middle of all chests, which on a map built around its middle island is that island. */
    private fun findMiddle(): Vec3? {
        if (chests.all.size < MIN_MIDDLE_CHESTS) {
            return null
        }

        var x = 0.0
        var y = 0.0
        var z = 0.0
        for (pos in chests.all) {
            x += pos.x + 0.5
            y += pos.y
            z += pos.z + 0.5
        }
        val count = chests.all.size.toDouble()
        return Vec3(x / count, y / count, z / count)
    }

    private fun arrive(destination: Destination) {
        if (destination.label == MIDDLE) {
            middleDone = true
        }
        if (destination.lootRadius > 0.0) {
            this.destination = null
            startLoot(destination.point, destination.lootRadius, "arrived ${destination.label}")
        }
    }

    /** Gives a destination up for a while once the walk gets no closer, unless it can be flown to. */
    private fun checkProgress(destination: Destination, goal: Vec3) {
        val distance = player.position().distanceTo(goal)
        if (distance < bestDistance - MIN_PROGRESS) {
            bestDistance = distance
            progressAt = ticks
        }

        strandedTicks = if (navigator.isStranded) strandedTicks + 1 else 0
        if (strandedTicks < STRANDED_TICKS && ticks - progressAt < NO_PROGRESS_TICKS) {
            return
        }

        unreachableUntil[destination.label] = ticks + RETRY_TICKS
        report("unreachable ${destination.label}")
        MapRecorder.event("unreachable", "label" to destination.label, "goal" to destination.goal)
        this.destination = null
        resetProgress()
    }

    private fun startFight(opponent: Player, reason: String) {
        this.opponent = opponent
        calmTicks = 0
        holdFightBot()
        enter(Phase.FIGHT, "$reason ${opponent.scoreboardName}")
        MapRecorder.event("fight", "phase" to "start", "target" to opponent.scoreboardName, "reason" to reason)
    }

    private fun fight() {
        (Outnumbered.plan as? Outnumbered.Retreat)?.let { return retreat(it.point) }

        val current = opponentInReach(fightRange + FIGHT_MARGIN)
        if (current == null) {
            if (++calmTicks >= CALM_TICKS) {
                leaveFight()
            }
            return
        }

        calmTicks = 0
        holdFightBot()
        if (current !== opponent) {
            opponent = current
            report("target ${playerLabel(current)}")
        }

        // FightBot walks on its own; without it the walk only follows onto ground.
        if (!useFightBot && !current.doesNotCollideBelow()) {
            walkGoal = current.position()
            walkRadius = MELEE_RADIUS
        }
    }

    /** Walks away from the opponents the bot is outnumbered by; KillAura still hits whoever comes close. */
    private fun retreat(point: Vec3) {
        releaseFightBot()
        calmTicks = 0
        walkGoal = point
        walkRadius = RETREAT_RADIUS
    }

    private fun holdFightBot() {
        if (useFightBot && !KillAuraFightBot.enabled) {
            KillAuraFightBot.enabled = true
            ownsFightBot = true
        }
    }

    private fun leaveFight() {
        releaseFightBot()
        opponent = null
        MapRecorder.event("fight", "phase" to "stop")
        if (ticks < lootUntil && nextChest() != null) {
            enter(Phase.LOOT, "fight over")
        } else {
            enter(Phase.TRAVEL, "fight over")
        }
    }

    private fun releaseFightBot() {
        if (ownsFightBot) {
            KillAuraFightBot.enabled = false
            ownsFightBot = false
        }
    }

    /** The closest opponent within [range], unless it stands over the void out of melee reach. */
    private fun opponentInReach(range: Float = fightRange): Player? = Opponents.around()
        .filter {
            val distance = it.distanceTo(player)
            distance <= range && (distance <= MELEE_REACH || !it.doesNotCollideBelow())
        }
        .minByOrNull { it.distanceToSqr(player) }

    /**
     * Whether the flight to [goal] hits nothing, sampled along the way it takes: a bit above the feet, and with
     * Rewound's fly climbing or sinking to the goal height at its rates while it moves on at boost speed.
     */
    private fun canFlyTo(goal: Vec3): Boolean {
        if (!player.onGround() || ModuleFly.enabled) {
            return false
        }

        val from = player.position()
        val distance = from.horizontalDistanceTo(goal)
        if (distance > FlyAcross.maxDistance) {
            return false
        }

        val toHeight = goal.y + LANDING_LIFT - (from.y + FLY_RISE)
        val rate = when {
            !isRewoundFly() -> 0.0
            toHeight > 0.0 -> FlyRewound20261112.climbPerTick
            else -> FlyRewound20261112.sinkPerTick
        }
        val ticksPerBlock = 1.0 / FlyRewound20261112.boostPerTick

        val box = player.boundingBox
        val samples = ceil(distance / FLY_SAMPLE_SPACING).toInt()
        for (sample in 1..samples) {
            val t = sample.toDouble() / samples
            val heightChange = min(abs(toHeight), rate * distance * t * ticksPerBlock)
            val rise = FLY_RISE + if (toHeight > 0.0) heightChange else -heightChange
            val offset = Vec3((goal.x - from.x) * t, rise, (goal.z - from.z) * t)
            if (!world.noCollision(player, box.move(offset))) {
                return false
            }
        }
        return true
    }

    private fun isRewoundFly() = ModuleFly.modes.activeMode === FlyRewound20261112

    private fun startFlight(goal: Vec3, destination: Destination) {
        val flight = Flight(player.position(), goal, ticks, player.y)
        flight.runUp = runUpPoint(goal)
        beginFlight(flight, destination.label)
    }

    /** Flies back up to [ground] after a fall into the void, when Fly may be used. */
    private fun flyBack(ground: Vec3): Boolean {
        if (!flyRescue || flight != null || ModuleFly.enabled) {
            return false
        }

        beginFlight(Flight(ground, ground, ticks, player.y, maxFall = RESCUE_FALL), "rescue")
        return true
    }

    private fun beginFlight(flight: Flight, label: String) {
        this.flight = flight
        navigator.halt()
        ModuleFly.enabled = true
        FlyRewound20261112.targetY = flight.goal.y + LANDING_LIFT
        flightStage = null
        flightStageTicks = 0
        val distance = formatDistance(player.position().horizontalDistanceTo(flight.goal))
        val runUp = flight.runUp?.let(::formatPosition) ?: "none"
        report("fly start to=${formatPosition(flight.goal)} dist=$distance runup=$runUp for=$label")
    }

    private fun fliesToHeight() = ModuleFly.running && isRewoundFly()

    /** Lands on the first ground past the void, or at the goal; heads back once flying takes too long. */
    private fun fly(flight: Flight) {
        logFlightStage()
        if (isRewoundFly()) {
            FlyRewound20261112.abortReason?.let {
                stopFlight(it)
                return
            }
        }

        FlyRewound20261112.targetY = flight.goal.y + LANDING_LIFT
        flight.runUp?.let { runUp ->
            if (player.position().horizontalDistanceTo(runUp) <= RUN_UP_REACH || player.doesNotCollideBelow()) {
                flight.runUp = null
            }
        }
        navigator.lookAt(flight.goal)
        val overVoid = player.doesNotCollideBelow()
        val ground = !player.doesNotCollideBelow(player.y - LANDING_DEPTH)
        // The charge and the damage stage are still on the island; a ground below them is no landing.
        val boosted = !isRewoundFly() || FlyRewound20261112.boosting
        flight.crossedVoid = flight.crossedVoid || boosted && overVoid
        flight.airborne = flight.airborne || boosted && !player.onGround()
        val near = player.position().horizontalDistanceTo(flight.goal) <= LANDING_RADIUS
        val elapsed = ticks - flight.startedAt
        val maxTicks = FlyAcross.maxTime * TICKS_PER_SECOND

        when {
            // A setback can put the player back on the ground for a moment; the fly handles it and goes on.
            flight.airborne && player.onGround() && Setbacks.ticksSinceLast > SETBACK_GRACE_TICKS ->
                stopFlight("grounded")
            !flight.airborne && elapsed > LIFT_TICKS -> stopFlight("no lift")
            boosted && (ground && flight.crossedVoid || near && !overVoid) -> stopFlight("landed")
            player.y < flight.startY - flight.maxFall -> stopFlight("falling")
            elapsed > 2 * maxTicks -> stopFlight("timeout")
            elapsed > maxTicks && flight.goal != flight.origin -> {
                flight.goal = flight.origin
                report("fly return")
            }
        }
    }

    /** Logs the stage the flight just left with the ticks it lasted, see [FlyRewound20261112.stage]. */
    private fun logFlightStage() {
        if (!isRewoundFly()) {
            return
        }

        val stage = FlyRewound20261112.stage
        if (stage == flightStage) {
            flightStageTicks++
            return
        }

        reportFlightStage()
        flightStage = stage
        flightStageTicks = 1
    }

    private fun reportFlightStage() {
        val stage = flightStage ?: return
        report("fly stage ${stage.name.lowercase(Locale.ROOT)} t=$flightStageTicks")
    }

    private fun activitySituation(): String? = when (phase) {
        Phase.LOOT, Phase.TRAVEL, Phase.FIGHT -> "state=$phase action=${activity()}"
        else -> null
    }

    override fun situation(): String? {
        val activity = activitySituation() ?: return null
        val placement = navigator.placement?.let { "${it.verdict.tag}@${it.pos.x},${it.pos.y},${it.pos.z}" } ?: "none"
        return "$activity guard=${voidSafety.hold.tag} placement=$placement"
    }

    override val phaseName: String
        get() = phase.name

    override val action: String
        get() = activity()

    private fun activity() = when {
        flight != null -> "fly"
        phase == Phase.FIGHT && KillAuraFightBot.running -> "fightbot"
        navigator.isBuilding -> if (bridge.isActive) "bridge" else "build"
        walkGoal != null -> "walk"
        else -> "stand"
    }

    private fun stopFlight(reason: String) {
        val setbacks = Setbacks.count - (flight?.setbacksAtStart ?: Setbacks.count)
        reportFlightStage()
        flightStage = null
        flightStageTicks = 0
        flight = null
        flightEndedAt = ticks
        lastFlightSetbacks = setbacks
        ModuleFly.enabled = false
        report("fly end landed=${formatPosition(player.position())} setbacks=$setbacks reason=$reason")
        resetProgress()
    }

    private fun stopPlaying() {
        releaseFightBot()
        if (flight != null) {
            stopFlight("stopped")
        }
        voidSafety.stop()
        bridge.stop()
        walkGoal = null
        destination = null
        opponent = null
    }

    private fun forgetWorld() {
        chests.clear()
        plan.clear()
        unreachableUntil.clear()
        middle = null
        middleDone = false
        startHeard = false
        enclosedTicks = 0
        freeTicks = 0
        lootCenter = null
        chest = null
        calmTicks = 0
    }

    private fun enter(next: Phase, reason: String) {
        if (phase != next) {
            report("state $phase -> $next ($reason)")
            MapRecorder.event("phase", "from" to phase.name, "to" to next.name, "reason" to reason)
        }
        phase = next
        destination = null
        resetProgress()
    }

    private fun resetProgress() {
        bestDistance = Double.MAX_VALUE
        progressAt = ticks
        strandedTicks = 0
    }

    private fun isReachable(label: String) = (unreachableUntil[label] ?: 0) <= ticks

    private fun isContainerOpen() = mc.gui.screen() is AbstractContainerScreen<*>

    /** A cage: blocks all around the feet and the head. */
    private fun isEnclosed(): Boolean {
        if (!player.onGround()) {
            return false
        }

        val feet = player.blockPosition()
        return Direction.Plane.HORIZONTAL.all { isSolid(feet.relative(it)) && isSolid(feet.above().relative(it)) }
    }

    private fun isSolid(pos: BlockPos) = !world.getBlockState(pos).getCollisionShape(world, pos).isEmpty

    private fun isWithin(pos: BlockPos, center: Vec3, radius: Double, height: Double) =
        Vec3.atCenterOf(pos).horizontalDistanceTo(center) <= radius && abs(pos.y + 0.5 - center.y) <= height

    private fun playerLabel(player: Player) = "player ${player.scoreboardName}"

    private fun formatDistance(distance: Double) = "%.1f".format(Locale.ROOT, distance)

    private fun formatPosition(pos: Vec3) = "%.1f,%.1f,%.1f".format(Locale.ROOT, pos.x, pos.y, pos.z)

    private const val MIDDLE = "middle"
    private const val TICKS_PER_SECOND = 20

    private const val CAGE_TICKS = 10
    private const val FREE_TICKS = 20
    private const val DROP_HEIGHT = 0.9

    private const val SCAN_TICKS = 20
    private const val SCAN_CHUNKS = 8

    /** From the chest center: close enough for ChestAura's default range of 3 from the eyes. */
    private const val CHEST_REACH = 2.0
    private const val LOOT_HEIGHT = 12.0
    private const val DETOUR_RANGE = 6.0
    private const val DETOUR_HEIGHT = 4.0
    private const val DETOUR_RADIUS = 8.0

    private const val PICK_TICKS = 20
    private const val ARRIVAL_MARGIN = 1.0
    private const val OPPONENT_RADIUS = 3.0
    private const val MIDDLE_RADIUS = 4.0
    private const val MIN_MIDDLE_CHESTS = 6
    private const val MIN_PROGRESS = 1.0
    private const val STRANDED_TICKS = 40
    private const val NO_PROGRESS_TICKS = 200
    private const val RETRY_TICKS = 600

    private const val FIGHT_MARGIN = 2f
    private const val CALM_TICKS = 20
    private const val MELEE_REACH = 3.5f
    private const val MELEE_RADIUS = 2.5
    private const val RETREAT_RADIUS = 1.0

    private const val FLY_SAMPLE_SPACING = 0.5
    private const val FLY_RISE = 0.5
    private const val FLY_FALL = 4.0
    private const val RESCUE_FALL = 12.0
    private const val LIFT_TICKS = 60
    private const val LANDING_DEPTH = 4.0
    private const val LANDING_RADIUS = 2.0

    /** Above the goal's feet, which Rewound's fly heads for. */
    private const val LANDING_LIFT = 0.5

    /** Void stretches longer than this, in blocks, are flown over, as are climbs to players this much higher. */
    private const val VOID_CROSSING = 8.0
    private const val HIGHER_GROUND = 3.0
    private const val VOID_SAMPLE_SPACING = 0.5
    private const val VOID_DEPTH = 40.0
    private const val HALF_WIDTH = 0.3

    /** The run-up of a flight start: searched this far ahead in steps, ending this far past the edge. */
    private const val RUN_UP_RANGE = 8.0
    private const val RUN_UP_SAMPLE = 0.25
    private const val RUN_UP_PAST = 1.0
    private const val RUN_UP_REACH = 0.5
    private val RUN_UP_OFFSETS = doubleArrayOf(0.0, 15.0, -15.0, 30.0, -30.0, 45.0, -45.0)

    /** Ticks after a setback in which landing on the ground does not end a flight. */
    private const val SETBACK_GRACE_TICKS = 5
    private const val TRAVEL_CHECK_TICKS = 10

    /** The fly costs health; after a flight it waits, longer if the server set it back. */
    private const val MIN_FLY_HP = 6f
    private const val FLY_COOLDOWN_TICKS = 60
    private const val SETBACK_COOLDOWN_TICKS = 400

}
