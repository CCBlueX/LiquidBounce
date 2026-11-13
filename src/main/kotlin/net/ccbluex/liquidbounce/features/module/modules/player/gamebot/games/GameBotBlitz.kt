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

import it.unimi.dsi.fastutil.longs.LongOpenHashSet
import net.ccbluex.liquidbounce.event.events.ChatReceiveEvent
import net.ccbluex.liquidbounce.event.events.KeybindIsPressedEvent
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.PlayerTickEvent
import net.ccbluex.liquidbounce.event.events.TitleEvent
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.features.KillAuraFightBot
import net.ccbluex.liquidbounce.features.module.modules.player.cheststealer.ModuleChestStealer
import net.ccbluex.liquidbounce.features.module.modules.player.cheststealer.features.FeatureChestAura
import net.ccbluex.liquidbounce.features.module.modules.player.gamebot.Chests
import net.ccbluex.liquidbounce.features.module.modules.player.gamebot.Game
import net.ccbluex.liquidbounce.features.module.modules.player.gamebot.Gear
import net.ccbluex.liquidbounce.features.module.modules.player.gamebot.MapRecorder
import net.ccbluex.liquidbounce.features.module.modules.player.gamebot.ModuleGameBot
import net.ccbluex.liquidbounce.features.module.modules.player.gamebot.ModuleGameBot.report
import net.ccbluex.liquidbounce.features.module.modules.player.gamebot.Opponents
import net.ccbluex.liquidbounce.features.module.modules.player.gamebot.Outnumbered
import net.ccbluex.liquidbounce.features.module.modules.player.gamebot.Plan
import net.ccbluex.liquidbounce.utils.aiming.RotationsValueGroup
import net.ccbluex.liquidbounce.utils.client.SilentHotbar
import net.ccbluex.liquidbounce.utils.inventory.HotbarItemSlot
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.ccbluex.liquidbounce.utils.item.attackDamage
import net.ccbluex.liquidbounce.utils.item.equipmentSlot
import net.ccbluex.liquidbounce.utils.item.foodComponent
import net.ccbluex.liquidbounce.utils.item.isAxe
import net.ccbluex.liquidbounce.utils.item.isPlayerArmor
import net.ccbluex.liquidbounce.utils.item.isSword
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.CRITICAL_MODIFICATION
import net.ccbluex.liquidbounce.utils.math.horizontalDistanceTo
import net.ccbluex.liquidbounce.utils.math.horizontalDistanceToSqr
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.ccbluex.liquidbounce.utils.navigation.Navigator
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.tags.BlockTags
import net.minecraft.util.Mth
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.levelgen.Heightmap
import net.minecraft.world.phys.Vec3
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

/**
 * Blitz Survival Games on one large map: runs to the center from the spawn pedestals and loots its chests first,
 * stops to equip and eat, and keeps looting while under-equipped. Once equipped it hunts down the closest player,
 * opening chests close to the way. After the deathmatch teleport it loots, eats and heals through the countdown,
 * then fights everyone.
 */
@Suppress("TooManyFunctions", "LargeClass", "ReturnCount")
internal object GameBotBlitz : Game("Blitz", "/play blitz_solo_normal") {

    private val fightRange by float("FightRange", 6f, 3f..12f)
    private val travelRange by int("TravelRange", 64, 16..256, "blocks")

    /** How long the bot may spend getting to and looting the center chests. */
    private val centerTime by int("CenterTime", 45, 10..120, "s")

    /** Chests this close are opened on the way to the next player. */
    private val chestDetour by float("ChestDetour", 6f, 0f..16f, "blocks")
    private val chestTime by int("ChestTime", 15, 3..60, "s")
    private val gearTime by int("GearTime", 3, 1..15, "s")
    private val eatHunger by int("EatHunger", 14, 1..19)
    private val useFightBot by boolean("FightBot", true)

    /** How long the deathmatch countdown is taken to last when neither chat nor a title tells. */
    private val prepareTime by int("PrepareTime", 10, 3..60, "s")

    /** Plays a round that is already on when turned on, instead of waiting for the next countdown. */
    private val joinRunning by boolean("JoinRunning", false)

    /** Server lines saying the deathmatch started; player chat and the "begins in" countdown are never read. */
    private val deathmatchKeywords by textList(
        "DeathmatchStart", mutableListOf("deathmatch has begun", "deathmatch has started")
    )

    private val rotations = tree(RotationsValueGroup(this))
    override val navigator = Navigator(this, this, rotations)

    private enum class Phase { IDLE, START, CENTER, GEAR, HUNT, PREPARE, DEATHMATCH, END }

    private var phase = Phase.IDLE
    private var ticks = 0

    private var startAt: Int? = null
    private var roundStartedAt = 0
    private var kitDelay: Int? = null
    private var kitAt: Int? = null
    private var deathmatchHeardAt: Int? = null

    /** When the announced deathmatch countdown runs out. */
    private var deathmatchAt: Int? = null
    private var prepareStartedAt = 0

    /** The deathmatch countdown was waited out this round, so the next teleport goes straight to fighting. */
    private var prepared = false

    private val chests = Chests(this, rotations, SCAN_CHUNKS)
    private val plan = Plan(chests)
    private val visitedChunks = LongOpenHashSet()
    private val unreachableUntil = HashMap<String, Int>()
    private var roam: Vec3? = null
    private var roamAngle = Double.NaN

    private class Sighting(val name: String, val position: Vec3, val at: Int) {
        val label: String
            get() = "seen $name"
    }

    /** Where each opponent was last seen, to look for players once geared. */
    private val lastSeen = HashMap<String, Sighting>()

    /** The spawn the compass points at when the round starts; the server moves it onto a player. */
    private var roundSpawn: BlockPos? = null
    private var centerSoughtAt = NEVER

    /** The middle of the map, where the best chests are; null when it could not be found. */
    private var center: Vec3? = null
    private var centerSince = 0
    private var centerDone = false

    private var target: String? = null
    private var targetSince = 0
    private var bestDistance = Double.MAX_VALUE
    private var progressAt = 0
    private var strandedTicks = 0

    private var opponent: Player? = null
    private var calmTicks = 0
    private var ownsFightBot = false
    private var evading = false

    private var gearStartedAt = 0
    private var gearUntil = 0
    private var hungerCheckAt = 0
    private var eatingSlot: HotbarItemSlot? = null
    private val eating: Boolean
        get() = eatingSlot != null

    private var lastPosition: Vec3? = null

    private var walkGoal: Vec3? = null
    private var walkRadius = 1.0

    override val isPlaying: Boolean
        get() = phase != Phase.IDLE && phase != Phase.END

    /** Off during the deathmatch countdown, which turns KillAura off while attacks do nothing. */
    override val hasStarted: Boolean
        get() = isPlaying && phase != Phase.START && phase != Phase.PREPARE

    override val isFighting: Boolean
        get() = hasStarted && opponent != null

    override val keepsAway: Boolean
        get() = hasStarted && opponent == null && phase != Phase.DEATHMATCH && (phase == Phase.CENTER || !isEquipped)

    override val isOpeningChest: Boolean
        get() = chests.isOpening

    override fun startAnnounced() {
        if (phase == Phase.IDLE || phase == Phase.START) {
            startAt = ticks + START_DELAY_TICKS
            enter(Phase.START, "start announced")
        }
    }

    override fun end() {
        stopPlaying()
        enter(Phase.END, "round over")
    }

    override val phaseName: String
        get() = phase.name

    override val action: String
        get() = when {
            opponent != null -> if (KillAuraFightBot.running) "fightbot" else "fight"
            navigator.isBuilding -> "build"
            walkGoal != null -> "walk"
            else -> "stand"
        }

    override fun situation(): String? = if (phase in PLAYING) "state=$phase action=$action" else null

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
        val deathmatchWorld = isPlaying && phase != Phase.START &&
            (deathmatchHeardAt?.let { ticks - it <= DEATHMATCH_WORLD_TICKS } == true ||
                deathmatchAt?.let { abs(ticks - it) <= DEATHMATCH_WORLD_TICKS } == true)
        stopPlaying()
        forgetWorld()
        if (deathmatchWorld) {
            startPrepare("world change")
        } else {
            enter(Phase.IDLE, "world change")
        }
    }

    @Suppress("unused")
    private val chatHandler = handler<ChatReceiveEvent> { event ->
        if (event.type != ChatReceiveEvent.ChatType.GAME_MESSAGE) {
            return@handler
        }

        val message = event.message.trim()
        // Server lines carry no "name: text" part.
        if (message.contains(": ")) {
            return@handler
        }

        COUNTDOWN.find(message)?.let { match ->
            if (phase == Phase.IDLE) {
                enter(Phase.START, "countdown ${match.groupValues[1]}s")
            }
            return@handler
        }

        KIT.find(message)?.let { match ->
            kitDelay = match.groupValues[2].toInt()
            report("kit ${match.groupValues[1]} items_in=${kitDelay}s")
            if (isPlaying && phase != Phase.START) {
                kitAt = roundStartedAt + match.groupValues[2].toInt() * TICKS_PER_SECOND
            }
            return@handler
        }

        when {
            message.contains(BLITZ_STAR) -> report("star $message")
            noteDeathmatchTimer(message) -> Unit
            deathmatchKeywords.any { message.contains(it, ignoreCase = true) } -> noteDeathmatch(message)
        }
    }

    @Suppress("unused")
    private val titleHandler = handler<TitleEvent.Title> { event -> readCountdown(event.text?.string) }

    @Suppress("unused")
    private val subtitleHandler = handler<TitleEvent.Subtitle> { event -> readCountdown(event.text?.string) }

    /** A title such as `5` or `Deathmatch starts in 5 seconds` during [Phase.PREPARE] tells when the fight starts. */
    private fun readCountdown(text: String?) {
        if (phase != Phase.PREPARE || text == null || noteDeathmatchTimer(text)) {
            return
        }
        val seconds = text.trim().toIntOrNull()?.takeIf { it in 1..MAX_TITLE_COUNTDOWN } ?: return
        deathmatchAt = ticks + seconds * TICKS_PER_SECOND
    }

    @Suppress("unused")
    private val useKeyHandler = handler<KeybindIsPressedEvent> { event ->
        if (eating && event.keyBinding == mc.options.keyUse) {
            event.isPressed = true
        }
    }

    @Suppress("unused")
    private val tickHandler = handler<PlayerTickEvent> {
        ticks++
        if (ticks % SCAN_TICKS == 0) {
            chests.scan()
        }

        walkGoal = null
        if (isPlaying && phase != Phase.START && (player.isSpectator || player.abilities.mayfly)) {
            ModuleGameBot.finish(won = false, reason = "spectator")
            return@handler
        }

        checkTeleport()
        deathmatchAt?.let {
            if (ticks >= it && phase in PLAYING && phase != Phase.PREPARE) {
                deathmatchAt = null
                startPrepare("deathmatch countdown over")
            }
        }
        if (phase in PLAYING) {
            chests.noteLooted()
            visitedChunks.add(ChunkPos.pack(player.blockX shr CHUNK_SHIFT, player.blockZ shr CHUNK_SHIFT))
            for (other in Opponents.around()) {
                lastSeen[other.scoreboardName] = Sighting(other.scoreboardName, other.position(), ticks)
            }
        }

        when (phase) {
            Phase.IDLE, Phase.START -> waitForStart()
            Phase.CENTER -> center()
            Phase.GEAR -> gear()
            Phase.HUNT -> hunt()
            Phase.PREPARE -> prepare()
            Phase.DEATHMATCH -> deathmatch()
            Phase.END -> Unit
        }
    }

    @Suppress("unused")
    private val movementHandler = handler<MovementInputEvent>(priority = CRITICAL_MODIFICATION) { event ->
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

    private fun waitForStart() {
        val at = startAt
        if (at != null && ticks >= at) {
            start("games began", fromPedestal = true)
        } else if (phase == Phase.IDLE && joinRunning && player.onGround()) {
            start("joined running round", fromPedestal = false)
        }
    }

    private fun start(reason: String, fromPedestal: Boolean) {
        startAt = null
        roundStartedAt = ticks
        deathmatchHeardAt = null
        deathmatchAt = null
        prepared = false
        kitAt = kitDelay?.let { ticks + it * TICKS_PER_SECOND }
        ModuleGameBot.roundStarted()
        locateCenter(fromPedestal)
        roundSpawn = world.levelData.respawnData.pos()
        centerSince = ticks
        centerDone = false
        enter(Phase.CENTER, reason)
        if (!ModuleChestStealer.running || !FeatureChestAura.running) {
            report("warning ChestStealer or its Aura is off, chests are not opened")
        }
    }

    /**
     * The players stand on pedestals in a ring around the middle at the start, so the middle of their positions
     * leads to it. The middle is then moved onto the nearest dense group of chests, or found as the group closest
     * to the world border center when the spawns are unknown.
     */
    private fun locateCenter(fromPedestal: Boolean) {
        chests.scan()
        val position = player.position()
        val spawns = if (fromPedestal) {
            (Opponents.around().map { it.position() } + position)
                .filter { it.horizontalDistanceTo(position) <= SPAWN_RANGE }
                .toList()
        } else {
            emptyList()
        }

        val found = if (spawns.size >= MIN_SPAWNS) {
            val middle = Vec3(spawns.sumOf { it.x } / spawns.size, position.y, spawns.sumOf { it.z } / spawns.size)
            val cluster = chestCluster(middle, CENTER_SEARCH)
            report("center spawns=${spawns.size} chests=${cluster?.second ?: 0}")
            cluster?.first ?: groundAt(middle.x, middle.z) ?: middle
        } else {
            val border = world.worldBorder
            val cluster = chestCluster(Vec3(border.centerX, position.y, border.centerZ), Double.MAX_VALUE)
            report("center border chests=${cluster?.second ?: 0}")
            cluster?.first
        }
        center = found
        found?.let { report("center at ${floor(it.x).toInt()},${floor(it.y).toInt()},${floor(it.z).toInt()}") }
    }

    /** The middle of the group of at least [MIN_CLUSTER] chests closest to [reference], with its size. */
    private fun chestCluster(reference: Vec3, maxDistance: Double): Pair<Vec3, Int>? {
        val all = chests.all.toList()
        return all.asSequence()
            .filter { reference.horizontalDistanceTo(it) <= maxDistance }
            .map { pos -> all.filter { it.closerThan(pos, CLUSTER_RADIUS) } }
            .filter { it.size >= MIN_CLUSTER }
            .map { group ->
                Vec3(group.sumOf { it.x + 0.5 }, group.sumOf { it.y.toDouble() }, group.sumOf { it.z + 0.5 })
                    .scale(1.0 / group.size) to group.size
            }
            .minByOrNull { reference.horizontalDistanceToSqr(it.first) }
    }

    /**
     * Walks to the center and opens its chests first. Everyone starts around them, so only a player who hit the bot
     * within [DEFEND_TICKS] is fought or evaded; KillAura still hits whoever comes close meanwhile.
     */
    private fun center() {
        val attacked = Opponents.hitsTaken(DEFEND_TICKS) > 0
        if (attacked && engage() || isContainerOpen(true) || pickUpGear()) {
            return
        }
        if (opponent != null) {
            stopFight("center chests first")
        }
        evading = false

        gearReason()?.let { return startGear(it) }
        val middle = center ?: return finishCenter("no center")
        if (ticks - centerSince >= centerTime * TICKS_PER_SECOND) {
            return finishCenter("center time over")
        }

        val chest = chests.pick(
            chests.unopened()
                .filter { isReachable(chests.label(it)) && middle.horizontalDistanceTo(it) <= CENTER_RADIUS }
        )
        when {
            chest != null -> openChest(chest)
            !isReachable(CENTER_LABEL) -> finishCenter("center unreachable")
            player.position().horizontalDistanceTo(middle) > CENTER_ARRIVAL ->
                walkTo(CENTER_LABEL, middle, CENTER_ARRIVAL)
            else -> finishCenter("center looted")
        }
    }

    private fun finishCenter(reason: String) {
        centerDone = true
        startGear(reason)
    }

    /**
     * Opens chests within [chestDetour] on the way, then goes for what [plan] picks among the unopened chests and
     * the players within [travelRange]; under-equipped only players clearly weaker count. Geared with nobody in sight
     * it looks for players, and explores when the plan has nothing.
     */
    private fun hunt() {
        if (engage() || isContainerOpen(true) || pickUpGear()) {
            return
        }

        gearReason()?.let { return startGear(it) }
        val position = player.position()
        val detour = chests.pick(
            chests.unopened().filter {
                isReachable(chests.label(it)) && Vec3.atCenterOf(it).closerThan(position, chestDetour.toDouble())
            }
        )
        if (detour != null) {
            return openChest(detour)
        }

        val armed = hasWeapon
        val equipped = isEquipped
        val range = travelRange.toDouble()
        val choice = plan.next(
            Opponents.around().filter {
                armed && it.distanceTo(player) <= range && isReachable(playerLabel(it)) && (equipped || isWeaker(it))
            },
            chests.unopened().filter { isReachable(chests.label(it)) },
            armed = armed,
            canExplore = true,
        )
        when (choice) {
            is Plan.Loot -> openChest(choice.chest)
            is Plan.Hunt -> chase(choice.opponent)
            is Plan.Hold -> resetTarget()
            Plan.Seek -> seek()
            null -> explore()
        }
    }

    /** Fights whom [plan] picks among all players in the arena; only defends while unarmed. */
    private fun deathmatch() {
        if (engage() || isContainerOpen(true) || pickUpGear()) {
            return
        }

        val armed = hasWeapon
        val choice = plan.next(
            Opponents.around().filter { armed && isReachable(playerLabel(it)) },
            emptySequence(),
            armed = armed,
            canExplore = false,
        )
        (choice as? Plan.Hunt)?.let { return chase(it.opponent) }
        gather()
    }

    /**
     * The deathmatch countdown after the teleport, while attacks do nothing: loots the chests around the arena
     * nearest first, then eats a golden apple while hurt and food to full hunger, then stands so AutoArmor and
     * InventoryCleaner sort the loot. Ends with the countdown, the deathmatch start in chat, a hit taken, or after
     * [prepareTime] when no countdown was seen.
     */
    private fun prepare() {
        val elapsed = ticks - prepareStartedAt
        val end = deathmatchAt ?: (prepareStartedAt + prepareTime * TICKS_PER_SECOND)
        when {
            // Landing from the teleport can hurt; only player hits after that mean the fight is on.
            elapsed > LANDING_TICKS && Opponents.hitsTaken(elapsed - LANDING_TICKS, byPlayer = true) > 0 ->
                return endPrepare("hit taken")
            ticks >= end -> return endPrepare("countdown over")
            elapsed >= MAX_PREPARE_TICKS -> return endPrepare("prepare too long")
        }
        if (isContainerOpen(true)) {
            return
        }

        val position = player.position()
        val chest = chests.pick(
            chests.unopened().filter {
                isReachable(chests.label(it)) && Vec3.atCenterOf(it).closerThan(position, PREPARE_CHEST_RANGE)
            }
        )
        if (chest != null) {
            stopEating()
            openChest(chest)
            return
        }

        val slot = gappleSlot()?.takeIf {
            player.health < player.maxHealth && !player.hasEffect(MobEffects.REGENERATION) &&
                player.absorptionAmount <= 0f
        } ?: foodSlot()?.takeIf { player.foodData.foodLevel < FULL_HUNGER }
        if (slot == null) {
            stopEating()
            resetTarget()
        } else {
            eat(slot)
        }
    }

    /** The deathmatch countdown starts, unless it was waited out already this round. */
    private fun startPrepare(reason: String) {
        if (prepared) {
            return startDeathmatch(reason)
        }
        if (phase !in PLAYING || phase == Phase.PREPARE) {
            return
        }

        if (opponent != null) {
            stopFight("deathmatch prepare")
        }
        stopEating()
        // A countdown running out now was the one to the teleport.
        deathmatchAt = deathmatchAt?.takeIf { it > ticks + TICKS_PER_SECOND }
        prepareStartedAt = ticks
        chests.scan()
        val position = player.position()
        val nearby = chests.unopened().count { Vec3.atCenterOf(it).closerThan(position, PREPARE_CHEST_RANGE) }
        report("deathmatch prepare chests=$nearby ${condition()}")
        enter(Phase.PREPARE, reason)
    }

    private fun endPrepare(reason: String) {
        prepared = true
        deathmatchAt = null
        stopEating()
        report("deathmatch fight reason=$reason ${condition()}")
        enter(Phase.DEATHMATCH, reason)
    }

    private fun condition() =
        "hp=${formatScore(player.health.toDouble())} hunger=${player.foodData.foodLevel}"

    /** Stands still so AutoArmor and InventoryCleaner can sort the new items, and eats. */
    private fun gear() {
        if (engage()) {
            stopEating()
            return
        }
        if (isContainerOpen(true)) {
            return
        }

        val fed = player.foodData.foodLevel >= EAT_UNTIL || !eat()
        if (fed) {
            stopEating()
        }
        if (fed && ticks >= gearUntil || ticks - gearStartedAt >= MAX_GEAR_TICKS) {
            stopEating()
            hungerCheckAt = ticks + HUNGER_RETRY_TICKS
            enter(if (centerDone) Phase.HUNT else Phase.CENTER, "geared")
        }
    }

    private fun gearReason(): String? {
        kitAt?.let {
            if (ticks >= it) {
                kitAt = null
                return "kit items"
            }
        }

        if (ticks >= hungerCheckAt && player.foodData.foodLevel <= eatHunger && foodSlot() != null) {
            return "hungry food=${player.foodData.foodLevel}"
        }
        return null
    }

    private fun startGear(reason: String) {
        gearStartedAt = ticks
        gearUntil = ticks + gearTime * TICKS_PER_SECOND
        enter(Phase.GEAR, reason)
    }

    private fun eat(): Boolean {
        eat(foodSlot() ?: return false)
        return true
    }

    private fun eat(slot: HotbarItemSlot) {
        if (slot != eatingSlot) {
            report("eat ${slot.itemStack.hoverName.string} food=${player.foodData.foodLevel}")
        }
        SilentHotbar.selectSlotSilently(this, slot, EAT_SLOT_TICKS)
        eatingSlot = slot
    }

    private fun stopEating() {
        if (eating) {
            eatingSlot = null
            SilentHotbar.resetSlot(this)
        }
    }

    private fun foodSlot(): HotbarItemSlot? = Slots.Hotbar
        .filter { it.itemStack.foodComponent != null && it.itemStack.item !in SPARED_FOOD }
        .maxByOrNull { it.itemStack.foodComponent!!.nutrition }

    /** A golden apple to heal with, while more than [SPARE_APPLES] are left for AutoBuff in the fight. */
    private fun gappleSlot(): HotbarItemSlot? {
        val apples = Slots.All.sumOf { if (it.itemStack.`is`(Items.GOLDEN_APPLE)) it.itemStack.count else 0 }
        return if (apples > SPARE_APPLES) Slots.Hotbar.findSlot { it.`is`(Items.GOLDEN_APPLE) } else null
    }

    /**
     * Fights the closest opponent in [fightRange], retreats when outnumbered, or keeps away while under-equipped
     * unless the opponent is clearly weaker; true while a fight goes on.
     */
    private fun engage(): Boolean {
        (Outnumbered.plan as? Outnumbered.Retreat)?.let {
            // KillAura still hits whoever comes close.
            releaseFightBot()
            calmTicks = 0
            walkGoal = it.point
            walkRadius = RETREAT_RADIUS
            return true
        }
        if (!hasWeapon) {
            return evade()
        }
        val equipped = phase == Phase.DEATHMATCH || isEquipped
        val current = Opponents.around()
            .filter {
                val range = if (it === opponent) fightRange + FIGHT_MARGIN else fightRange
                it.distanceTo(player) <= range && (equipped || isWeaker(it))
            }
            .minByOrNull { it.distanceToSqr(player) }

        if (current == null && !equipped) {
            return evade()
        }
        evading = false

        if (current == null) {
            if (opponent != null && ++calmTicks >= CALM_TICKS) {
                stopFight("calm")
            }
            return opponent != null
        }

        calmTicks = 0
        if (current !== opponent) {
            startFight(current)
        }
        holdFightBot()

        // FightBot walks on its own.
        if (!useFightBot) {
            walkGoal = current.position()
            walkRadius = MELEE_RADIUS
        }
        return true
    }

    /**
     * While unarmed or under-equipped nobody is fought: players coming close are walked away from, and only one too
     * close to get away from is hit back. KillAura stays on, so whoever is in reach still gets hit. False while
     * nobody comes.
     */
    private fun evade(): Boolean {
        val threats = Opponents.around().filter { isComing(it) }.toList()
        val closest = threats.minByOrNull { it.distanceToSqr(player) }
        val away = if (threats.isEmpty()) null else Outnumbered.retreatPoint(threats)
        if (closest != null && away == null && closest.distanceTo(player) <= CORNERED_RANGE) {
            if (closest !== opponent) {
                startFight(closest)
            }
            calmTicks = 0
            holdFightBot()
            return true
        }

        if (opponent != null) {
            stopFight("under-equipped")
        }
        if (closest == null || away == null) {
            evading = false
            return false
        }
        if (!evading) {
            evading = true
            report("evade ${playerLabel(closest)} ${if (hasWeapon) "under-equipped" else "unarmed"}")
        }
        walkGoal = away
        walkRadius = RETREAT_RADIUS
        return true
    }

    /** Within [fightRange] and walking towards the bot, or already close. */
    private fun isComing(other: Player): Boolean {
        val distance = other.distanceTo(player)
        if (distance > fightRange) {
            return false
        }
        val step = other.position().subtract(other.xo, other.yo, other.zo)
        val toBot = player.position().subtract(other.position()).normalize()
        return distance <= CLOSE_RANGE || step.dot(toBot) >= APPROACH_SPEED
    }

    /** Walks over a sword, axe or armor lying within [PICKUP_RANGE] that beats the bot's own. */
    private fun pickUpGear(): Boolean {
        val position = player.position()
        val bestDamage = Slots.All.maxOf { it.itemStack.attackDamage }
        val item = world.entitiesForRendering().asSequence()
            .filterIsInstance<ItemEntity>()
            .filter { it.position().closerThan(position, PICKUP_RANGE) && isReachable(itemLabel(it)) }
            .filter { isUpgrade(it.item, bestDamage) }
            .minByOrNull { it.distanceToSqr(player) }
            ?: return false
        walkTo(itemLabel(item), item.position(), PICKUP_RADIUS)
        return true
    }

    private fun isUpgrade(stack: ItemStack, bestDamage: Double): Boolean {
        if (stack.isSword || stack.isAxe) {
            return stack.attackDamage > bestDamage
        }
        val slot = stack.equipmentSlot ?: return false
        return stack.isPlayerArmor && Opponents.armorPoints(stack) > Opponents.armorPoints(player.getItemBySlot(slot))
    }

    private fun itemLabel(item: ItemEntity) = "item ${BuiltInRegistries.ITEM.getKey(item.item.item).path}#${item.id}"

    private fun startFight(target: Player) {
        opponent = target
        stopEating()
        report("fight ${playerLabel(target)} ${gearLine(target)}")
        MapRecorder.event("fight", "phase" to "start", "target" to target.scoreboardName, "gear" to gearLine(target))
    }

    private fun stopFight(reason: String) {
        releaseFightBot()
        opponent = null
        calmTicks = 0
        report("fight over $reason")
        MapRecorder.event("fight", "phase" to "stop", "reason" to reason)
        resetTarget()
    }

    private fun releaseFightBot() {
        if (ownsFightBot) {
            KillAuraFightBot.enabled = false
            ownsFightBot = false
        }
    }

    private fun holdFightBot() {
        if (useFightBot && !KillAuraFightBot.enabled) {
            KillAuraFightBot.enabled = true
            ownsFightBot = true
        }
    }

    private fun chase(prey: Player) {
        walkTo(playerLabel(prey), prey.position(), OPPONENT_RADIUS)
    }

    /**
     * Follows the compass, which the server points at a player, else goes where a recent killer or anyone else was
     * last seen, else to the center now and then, else explores.
     */
    private fun seek() {
        compassTarget()?.takeIf { isReachable(COMPASS_LABEL) }?.let { return walkTo(COMPASS_LABEL, it, ROAM_RADIUS) }

        val position = player.position()
        lastSeen.values.removeIf {
            ticks - it.at > SEEN_TICKS || position.horizontalDistanceTo(it.position) <= SEEK_ARRIVAL
        }
        val sighting = Opponents.recentKillers(SEEN_TICKS)
            .firstNotNullOfOrNull { name -> lastSeen[name]?.takeIf { isReachable(it.label) } }
            ?: lastSeen.values.filter { isReachable(it.label) }.maxByOrNull { it.at }
        if (sighting != null) {
            return walkTo(sighting.label, sighting.position, ROAM_RADIUS)
        }

        val middle = center
        if (middle != null && ticks - centerSoughtAt >= CENTER_REVISIT_TICKS && isReachable(CENTER_LABEL)) {
            if (position.horizontalDistanceTo(middle) > CENTER_ARRIVAL) {
                return walkTo(CENTER_LABEL, middle, CENTER_ARRIVAL)
            }
            centerSoughtAt = ticks
        }
        explore()
    }

    /** Where a carried compass points once the server moved it off the round's spawn, on the ground there. */
    private fun compassTarget(): Vec3? {
        val spawn = roundSpawn ?: return null
        val target = world.levelData.respawnData.pos()
        if (target == spawn || Slots.All.none { it.itemStack.`is`(Items.COMPASS) }) {
            return null
        }
        val x = target.x + 0.5
        val z = target.z + 0.5
        val point = groundAt(x, z) ?: Vec3(x, player.y, z)
        return point.takeIf { player.position().horizontalDistanceTo(it) > SEEK_ARRIVAL }
    }

    /** Walks to the closest chest not opened yet, or on into the parts of the map not walked yet. */
    private fun gather() {
        chests.pick(chests.unopened().filter { isReachable(chests.label(it)) })
            ?.let(::openChest)
            ?: explore()
    }

    /** Walks up to [chest] and opens it once in reach, giving it up after [chestTime]. */
    private fun openChest(chest: BlockPos) {
        val label = chests.label(chest)
        if (label == target && ticks - targetSince > chestTime * TICKS_PER_SECOND) {
            chests.skip(chest)
            resetTarget()
            return
        }
        walkTo(label, Vec3.atCenterOf(chest), CHEST_REACH)
        if (chests.isInReach(chest)) {
            walkGoal = null
            chests.open(chest)
        }
    }

    private fun explore() {
        val position = player.position()
        val current = roam?.takeIf {
            isReachable(roamLabel(it)) && position.horizontalDistanceTo(it) > ROAM_ARRIVAL
        } ?: pickRoam()
        roam = current
        if (current == null) {
            target = null
            return
        }
        walkTo(roamLabel(current), current, ROAM_RADIUS)
    }

    /** A point [ROAM_DISTANCE] away in the direction least walked so far, preferring to keep the heading. */
    private fun pickRoam(): Vec3? {
        val position = player.position()
        var best: Vec3? = null
        var bestAngle = Double.NaN
        var bestScore = Double.MAX_VALUE
        for (i in 0 until ROAM_HEADINGS) {
            val angle = i * 2 * PI / ROAM_HEADINGS
            val x = position.x + cos(angle) * ROAM_DISTANCE
            val z = position.z + sin(angle) * ROAM_DISTANCE
            if (!world.worldBorder.isWithinBounds(x, z)) {
                continue
            }

            val point = groundAt(x, z) ?: continue
            if (!isReachable(roamLabel(point))) {
                continue
            }

            val turn = if (roamAngle.isNaN()) 0.0 else abs(Mth.wrapDegrees(Math.toDegrees(angle - roamAngle)))
            val score = visitedAround(point) * VISITED_WEIGHT + turn
            if (score < bestScore) {
                best = point
                bestAngle = angle
                bestScore = score
            }
        }

        if (best != null) {
            roamAngle = bestAngle
        }
        return best
    }

    private fun visitedAround(point: Vec3): Int {
        val chunkX = floor(point.x).toInt() shr CHUNK_SHIFT
        val chunkZ = floor(point.z).toInt() shr CHUNK_SHIFT
        var count = 0
        for (x in chunkX - VISITED_SPREAD..chunkX + VISITED_SPREAD) {
            for (z in chunkZ - VISITED_SPREAD..chunkZ + VISITED_SPREAD) {
                if (visitedChunks.contains(ChunkPos.pack(x, z))) {
                    count++
                }
            }
        }
        return count
    }

    /** The ground at [x] [z] below any tree, or null where the chunk is not loaded. */
    private fun groundAt(x: Double, z: Double): Vec3? {
        val blockX = floor(x).toInt()
        val blockZ = floor(z).toInt()
        if (!world.hasChunk(blockX shr CHUNK_SHIFT, blockZ shr CHUNK_SHIFT)) {
            return null
        }

        var y = world.getHeight(Heightmap.Types.MOTION_BLOCKING, blockX, blockZ)
        val pos = BlockPos.MutableBlockPos(blockX, y - 1, blockZ)
        while (y > world.minY) {
            val state = world.getBlockState(pos)
            if (!state.`is`(BlockTags.LEAVES) && !state.`is`(BlockTags.LOGS)) {
                break
            }
            y--
            pos.move(0, -1, 0)
        }
        return Vec3(x, y.toDouble(), z)
    }

    /** Walks to [goal] and gives it up for a while once the walk gets no closer. */
    private fun walkTo(label: String, goal: Vec3, radius: Double) {
        if (label != target) {
            target = label
            targetSince = ticks
            resetProgress()
            report("target $label distance=${formatDistance(player.position().distanceTo(goal))}")
            MapRecorder.event(
                "target", "label" to label, "goal" to goal, "distance" to player.position().distanceTo(goal),
            )
        }

        walkGoal = goal
        walkRadius = radius

        val distance = player.position().distanceTo(goal)
        if (distance < bestDistance - MIN_PROGRESS) {
            bestDistance = distance
            progressAt = ticks
        }

        strandedTicks = if (navigator.isStranded) strandedTicks + 1 else 0
        if (strandedTicks >= STRANDED_TICKS || ticks - progressAt >= NO_PROGRESS_TICKS) {
            unreachableUntil[label] = ticks + RETRY_TICKS
            report("unreachable $label")
            MapRecorder.event("unreachable", "label" to label, "goal" to goal)
            resetTarget()
        }
    }

    /** A "Deathmatch begins in N ..." line only sets when it starts; true when [message] is one. */
    private fun noteDeathmatchTimer(message: String): Boolean {
        val match = DEATHMATCH_TIMER.find(message) ?: return false
        val unit = if (match.groupValues[2].startsWith("minute", ignoreCase = true)) SECONDS_PER_MINUTE else 1
        val seconds = match.groupValues[1].toInt() * unit
        deathmatchAt = ticks + seconds * TICKS_PER_SECOND
        report("deathmatch in ${seconds}s")
        return true
    }

    private fun noteDeathmatch(message: String) {
        deathmatchHeardAt = ticks
        if (phase == Phase.PREPARE) {
            endPrepare(message)
        } else {
            startDeathmatch(message)
        }
    }

    private fun startDeathmatch(reason: String) {
        if (phase in PLAYING && phase != Phase.DEATHMATCH) {
            stopEating()
            enter(Phase.DEATHMATCH, reason)
        }
    }

    /** A jump the walk cannot make; a teleport late in the round is taken as the move to the deathmatch. */
    private fun checkTeleport() {
        val position = player.position()
        val last = lastPosition
        lastPosition = position
        if (last == null || phase !in PLAYING) {
            return
        }

        val distance = last.distanceTo(position)
        if (distance < TELEPORT_DISTANCE) {
            return
        }

        report("teleport distance=${formatDistance(distance)}")
        navigator.reset()
        roam = null
        resetTarget()
        if (ticks - roundStartedAt >= DEATHMATCH_MIN_TICKS) {
            startPrepare("teleport")
        }
    }

    /** Enough armor and a weapon of at least a stone sword on 1.8 to go after players. */
    private val isEquipped: Boolean
        get() = hasWeapon && Gear.own().let { it.armor >= MIN_ARMOR && it.damage >= MIN_WEAPON_DAMAGE }

    /** A [Gear.score] lower than the bot's by [WEAKER_MARGIN]. */
    private fun isWeaker(other: Player) = Gear.of(other).score + WEAKER_MARGIN <= Gear.own().score

    private fun gearLine(other: Player) = "gear=${Gear.of(other)} own=${Gear.own()}"

    private fun stopPlaying() {
        releaseFightBot()
        stopEating()
        walkGoal = null
        opponent = null
        resetTarget()
    }

    private fun forgetWorld() {
        chests.clear()
        plan.clear()
        visitedChunks.clear()
        unreachableUntil.clear()
        startAt = null
        kitDelay = null
        kitAt = null
        roam = null
        lastSeen.clear()
        roundSpawn = null
        centerSoughtAt = NEVER
        center = null
        roamAngle = Double.NaN
        lastPosition = null
        calmTicks = 0
    }

    private fun enter(next: Phase, reason: String) {
        if (phase != next) {
            report("state $phase -> $next ($reason)")
            MapRecorder.event("phase", "from" to phase.name, "to" to next.name, "reason" to reason)
        }
        phase = next
        resetTarget()
    }

    private fun resetTarget() {
        target = null
        resetProgress()
    }

    private fun resetProgress() {
        bestDistance = Double.MAX_VALUE
        progressAt = ticks
        strandedTicks = 0
    }

    private fun isReachable(label: String) = (unreachableUntil[label] ?: 0) <= ticks

    /** Whether a chest is open; with [hold], time spent looting does not count against the walk. */
    private fun isContainerOpen(hold: Boolean = false): Boolean {
        val open = mc.gui.screen() is AbstractContainerScreen<*>
        if (open && hold) {
            targetSince = ticks
            progressAt = ticks
        }
        return open
    }

    private fun playerLabel(player: Player) = "player ${player.scoreboardName}"

    private fun roamLabel(point: Vec3) =
        "roam ${floor(point.x).toInt() shr CHUNK_SHIFT},${floor(point.z).toInt() shr CHUNK_SHIFT}"

    private fun formatDistance(distance: Double) = "%.1f".format(Locale.ROOT, distance)

    private fun formatScore(score: Double) = "%.1f".format(Locale.ROOT, score)

    private val PLAYING = setOf(Phase.CENTER, Phase.GEAR, Phase.HUNT, Phase.PREPARE, Phase.DEATHMATCH)

    /** Kept for AutoBuff, or not worth eating. */
    private val SPARED_FOOD = setOf(
        Items.GOLDEN_APPLE, Items.ENCHANTED_GOLDEN_APPLE, Items.ROTTEN_FLESH, Items.SPIDER_EYE,
        Items.POISONOUS_POTATO, Items.PUFFERFISH, Items.CHORUS_FRUIT,
    )

    private val COUNTDOWN = Regex("The games begin in (\\d+) seconds?!")
    private val KIT = Regex("You have chosen the (.+?) kit\\. You will get your items (\\d+) seconds after")
    private val DEATHMATCH_TIMER =
        Regex("deathmatch (?:begins|starts) in (\\d+) (seconds?|minutes?)", RegexOption.IGNORE_CASE)
    private const val BLITZ_STAR = "Blitz Star"

    private const val TICKS_PER_SECOND = 20
    private const val SECONDS_PER_MINUTE = 60
    private const val START_DELAY_TICKS = 20

    private const val SCAN_TICKS = 20
    private const val SCAN_CHUNKS = 8
    private const val CHUNK_SHIFT = 4

    /** From the chest center: close enough for ChestAura's default range of 3 from the eyes. */
    private const val CHEST_REACH = 2.0

    private const val CENTER_LABEL = "center"
    private const val COMPASS_LABEL = "compass"
    private const val NEVER = Int.MIN_VALUE / 2

    /** Geared with nobody in sight: places this close count as looked at. */
    private const val SEEK_ARRIVAL = 8.0
    private const val SEEN_TICKS = 60 * TICKS_PER_SECOND
    private const val CENTER_REVISIT_TICKS = 90 * TICKS_PER_SECOND

    /** Players further away at the start are not taken for pedestals of the bot's ring. */
    private const val SPAWN_RANGE = 64.0
    private const val MIN_SPAWNS = 4
    private const val CENTER_SEARCH = 32.0
    private const val CLUSTER_RADIUS = 8.0
    private const val MIN_CLUSTER = 3

    /** Chests this far from the center belong to it. */
    private const val CENTER_RADIUS = 14.0
    private const val CENTER_ARRIVAL = 3.0

    private const val ROAM_HEADINGS = 12
    private const val ROAM_DISTANCE = 48.0
    private const val ROAM_RADIUS = 3.0
    private const val ROAM_ARRIVAL = 6.0
    private const val VISITED_SPREAD = 2
    private const val VISITED_WEIGHT = 30.0

    private const val MIN_PROGRESS = 1.0
    private const val STRANDED_TICKS = 40
    private const val NO_PROGRESS_TICKS = 200
    private const val RETRY_TICKS = 600

    private const val OPPONENT_RADIUS = 3.0

    private const val FIGHT_MARGIN = 2f
    private const val CALM_TICKS = 20

    /** At the center, a player who hit the bot this recently is fought instead of looted past. */
    private const val DEFEND_TICKS = 40

    private const val MIN_ARMOR = 8.0
    private const val MIN_WEAPON_DAMAGE = 6.0
    private const val WEAKER_MARGIN = 4.0
    private const val MELEE_RADIUS = 2.5
    private const val RETREAT_RADIUS = 1.0

    /** Without a weapon: how close a player may get before it is hit back, and one that counts as coming. */
    private const val CORNERED_RANGE = 2f
    private const val CLOSE_RANGE = 3f

    /** Blocks per tick towards the bot. */
    private const val APPROACH_SPEED = 0.05
    private const val PICKUP_RANGE = 8.0
    private const val PICKUP_RADIUS = 0.5

    private const val EAT_UNTIL = 18
    private const val EAT_SLOT_TICKS = 5
    private const val MAX_GEAR_TICKS = 15 * TICKS_PER_SECOND
    private const val HUNGER_RETRY_TICKS = 30 * TICKS_PER_SECOND

    private const val TELEPORT_DISTANCE = 16.0
    private const val DEATHMATCH_MIN_TICKS = 60 * TICKS_PER_SECOND
    private const val DEATHMATCH_WORLD_TICKS = 90 * TICKS_PER_SECOND

    /** The longest countdown a title shows as a bare number. */
    private const val MAX_TITLE_COUNTDOWN = 30
    private const val MAX_PREPARE_TICKS = 60 * TICKS_PER_SECOND
    private const val LANDING_TICKS = TICKS_PER_SECOND

    /** Chests around the deathmatch arena this close are looted during the countdown. */
    private const val PREPARE_CHEST_RANGE = 16.0
    private const val FULL_HUNGER = 20

    /** Golden apples kept for AutoBuff in the fight. */
    private const val SPARE_APPLES = 1

}
