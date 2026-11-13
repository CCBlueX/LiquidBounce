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
package net.ccbluex.liquidbounce.features.module.modules.player.gamebot

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.EntityHealthUpdateEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.PlayerTickEvent
import net.ccbluex.liquidbounce.event.events.TransferOrigin
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.misc.FriendManager
import net.ccbluex.liquidbounce.features.module.MinecraftShortcuts
import net.ccbluex.liquidbounce.features.module.modules.combat.criticals.ModuleCriticals
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura
import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.ModuleAntiBot
import net.ccbluex.liquidbounce.features.module.modules.player.gamebot.ModuleGameBot.report
import net.ccbluex.liquidbounce.utils.combat.shouldBeAttacked
import net.ccbluex.liquidbounce.utils.entity.ping
import net.ccbluex.liquidbounce.utils.entity.squaredBoxedDistanceTo
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.ccbluex.liquidbounce.utils.item.armorValue
import net.ccbluex.liquidbounce.utils.item.attackDamage
import net.ccbluex.liquidbounce.utils.item.isAxe
import net.ccbluex.liquidbounce.utils.item.isSword
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.READ_FINAL_STATE
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.protocol.game.ClientboundAnimatePacket
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket
import net.minecraft.network.protocol.game.ClientboundSwingAnimationPacket
import net.minecraft.network.protocol.game.ServerboundAttackPacket
import net.minecraft.network.protocol.game.ServerboundPunchPacket
import net.minecraft.util.Mth
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.phys.Vec3
import java.util.Locale
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Watches the players near the bot during a round: how they move and fight, whether they are server bots,
 * and who hit the bot before it died. Logs one `opponent` line per player when it leaves, dies or the round ends.
 */
@Suppress("TooManyFunctions", "CognitiveComplexMethod")
internal object Opponents : EventListener, MinecraftShortcuts {

    override fun parent() = ModuleGameBot

    private class Opponent(val name: String) {
        var entity: Player? = null
        var end = ""

        /** Died or turned spectator; not watched again this round. */
        var gone = false

        var prevPos: Vec3? = null
        var prevYaw = Float.NaN
        var prevHurtTime = 0
        var seenTicks = 0
        var moved = false

        var speedSum = 0.0
        var speedSamples = 0
        var maxSpeed = 0.0
        var maxRise = 0.0
        var maxFall = 0.0
        var airTicks = 0
        var maxAirTicks = 0
        var airStartY = 0.0
        var flew = false
        var maxYawDelta = 0f

        var swings = 0
        var swungAt = NEVER
        var blockTicks = 0
        var sprintTicks = 0
        var item = "none"
        var damage = 0.0
        var armor = 0.0
        var armorItems = "none"

        var hits = 0
        var reachSum = 0.0
        var reachMin = Double.MAX_VALUE
        var reachMax = 0.0

        var inTab = false
        var hasGameMode = false
        var maxPing = 0
        var antiBot = false
        var uuidVersion = 0
        var metadata = -1

        var attackedAt = NEVER
        var targetedAt = NEVER
        var hurtAt = NEVER
        var hurtSource = -1

        /** The bot's side over the round, and of the fight going on. */
        var total: Exchange? = null
        var fight: Exchange? = null

        /** Ticks of the bot's attacks not yet followed by a hurt animation. */
        val pendingAttacks = ArrayDeque<Int>()
    }

    private class Hit(val tick: Int, val attacker: String?)

    private class HealthDrop(val tick: Int, val amount: Float)

    private var active = false
    private var ticks = 0

    private val opponents = HashMap<String, Opponent>()
    private val kills = HashMap<String, Int>()
    private val killedAt = HashMap<String, Int>()

    /** Entry count of the first metadata packet of each entity; server NPCs send few. */
    private val metadataSizes = Int2IntOpenHashMap()

    private val hits = ArrayDeque<Hit>()

    /** Ticks of the hits the bot landed. */
    private val landedHits = ArrayDeque<Int>()
    private val healthDrops = ArrayDeque<HealthDrop>()
    private var prevHurtTime = 0
    private var hitSource = -1

    /** Own health lost since the last tick, before the hurt animation names an attacker. */
    private var freshHp = 0f
    private var lastHitBy: Opponent? = null
    private var lastHitAt = NEVER

    private var deathAt: Int? = null
    private var deathKiller: String? = null
    private var deathReported = false

    /** The bot's gear on its last tick alive; at the death line the inventory is already gone. */
    private var aliveGear = "own=none"

    fun start() {
        flush("round")
        opponents.clear()
        kills.clear()
        killedAt.clear()
        hits.clear()
        landedHits.clear()
        healthDrops.clear()
        deathAt = null
        deathKiller = null
        deathReported = false
        aliveGear = "own=none"
        active = true
    }

    fun stop(reason: String) {
        flush(reason)
        active = false
    }

    /** A kill message in chat; the bot's own death when it is the [victim]. */
    fun noteKill(victim: String, killer: String) {
        kills.merge(killer, 1, Int::plus)
        killedAt[killer] = ticks
        if (victim == mc.player?.scoreboardName) {
            noteDeath(killer)
        } else {
            opponents[victim]?.takeIf { it.entity != null }?.let { log(it, "killed") }
        }
    }

    /** Players the kill feed named as killer within the last [window] ticks, the latest first. */
    fun recentKillers(window: Int): List<String> = killedAt.entries
        .filter { ticks - it.value <= window }
        .sortedByDescending { it.value }
        .map { it.key }

    /** The bot died; the line waits a moment for a kill message naming the killer. */
    fun noteDeath(killer: String?) {
        if (deathReported) {
            return
        }
        if (deathAt == null) {
            deathAt = ticks
        }
        killer?.let { deathKiller = it }
        if (deathKiller != null) {
            reportDeath()
        }
    }

    /** Whether the bot's KillAura target was [victim] and the last hit on it was the bot's. */
    fun isOurKill(victim: String): Boolean {
        val opponent = opponents[victim] ?: return false
        if (ticks - opponent.targetedAt > TARGET_TICKS) {
            return false
        }
        if (opponent.hurtSource >= 0) {
            return opponent.hurtSource == mc.player?.id
        }
        return opponent.hurtAt - opponent.attackedAt in 0..LAST_HIT_TICKS
    }

    /** The living opponents in the loaded world. */
    fun around(): Sequence<Player> = world.players().asSequence().filter(::isOpponent)

    // Players missing from the tab list are the server's NPCs; friends stay out even where combat targets them.
    fun isOpponent(other: Player) = other !== player && other.isAlive && !other.isSpectator &&
        !other.isInvisible && other.uuid in network.onlinePlayerIds && !FriendManager.isFriend(other) &&
        other.shouldBeAttacked()

    /** Hits the bot took within the last [window] ticks; with [byPlayer] only those a player is taken to have dealt. */
    fun hitsTaken(window: Int, byPlayer: Boolean = false) =
        hits.count { ticks - it.tick <= window && (!byPlayer || it.attacker != null) }

    /** Hits the bot landed within the last [window] ticks. */
    fun hitsLanded(window: Int) = landedHits.count { ticks - it <= window }

    @Suppress("unused")
    private val worldChangeHandler = handler<WorldChangeEvent> {
        if (deathAt != null && !deathReported) {
            reportDeath()
        }
        stop("world")
        metadataSizes.clear()
        prevHurtTime = 0
    }

    @Suppress("unused")
    private val tickHandler = handler<PlayerTickEvent> {
        ticks++
        noteHit()
        mc.player?.takeIf { it.isAlive && it.health > 0f }?.let { self ->
            // Every tick while hurt, which a death always follows.
            if (self.hurtTime > 0 || ticks % GEAR_TICKS == 0) {
                aliveGear = ownGear()
            }
        }
        while (hits.firstOrNull()?.let { ticks - it.tick > HISTORY_TICKS } == true) {
            hits.removeFirst()
        }
        while (landedHits.firstOrNull()?.let { ticks - it > HISTORY_TICKS } == true) {
            landedHits.removeFirst()
        }
        while (healthDrops.firstOrNull()?.let { ticks - it.tick > HISTORY_TICKS } == true) {
            healthDrops.removeFirst()
        }

        deathAt?.let { if (!deathReported && ticks - it >= KILLER_WAIT_TICKS) reportDeath() }

        if (!active) {
            return@handler
        }

        (ModuleKillAura.targetTracker.target as? Player)?.let { opponents[it.scoreboardName]?.targetedAt = ticks }

        for (other in world.players()) {
            if (other !== player && !other.isSpectator && player.distanceToSqr(other) <= WATCH_RANGE_SQ) {
                watch(other)
            }
        }

        for (opponent in opponents.values) {
            opponent.entity?.let { entity ->
                if (!entity.isRemoved) {
                    noteHurt(opponent, entity)
                }
                when {
                    entity.isRemoved || world.getEntity(entity.id) !== entity -> log(opponent, "despawn")
                    entity.isDeadOrDying -> log(opponent, "death")
                    entity.isSpectator -> log(opponent, "spectator")
                    else -> sample(opponent, entity)
                }
            }
            opponent.fight?.let { if (ticks - it.lastTick > FIGHT_IDLE_TICKS) endFight(opponent, "idle") }
        }
    }

    @Suppress("unused")
    private val sendHandler = handler<PacketEvent>(priority = READ_FINAL_STATE) { event ->
        if (event.origin != TransferOrigin.OUTGOING || event.isCancelled) {
            return@handler
        }
        when (val packet = event.packet) {
            is ServerboundAttackPacket -> noteAttack(packet.entityId)
            is ServerboundPunchPacket -> swingTarget()?.let { opponent ->
                opponent.total?.let { it.swings++ }
                opponent.fight?.let { it.swings++ }
            }
        }
    }

    @Suppress("unused")
    private val healthHandler = handler<EntityHealthUpdateEvent> { event ->
        val entity = event.entity
        if (event.new >= event.old || entity !is Player) {
            return@handler
        }
        val amount = event.old - event.new
        if (entity === mc.player) {
            healthDrops.addLast(HealthDrop(ticks, amount))
            val attacker = lastHitBy
            if (attacker != null && ticks - lastHitAt <= HIT_HP_TICKS) {
                confirm(attacker) { it.takenHp += amount }
            } else {
                freshHp += amount
            }
        } else {
            watched(entity.id)?.takeIf { ticks - it.attackedAt <= LAST_HIT_TICKS }?.let { opponent ->
                confirm(opponent) { it.damage += amount }
            }
        }
    }

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent> { event ->
        when (val packet = event.packet) {
            is ClientboundSwingAnimationPacket -> mc.execute {
                watched(packet.entityId)?.let {
                    it.swings++
                    it.swungAt = ticks
                }
            }

            is ClientboundAnimatePacket if packet.action == ClientboundAnimatePacket.CRITICAL_HIT -> mc.execute {
                watched(packet.id)?.takeIf { ticks - it.attackedAt <= LAST_HIT_TICKS }?.let { opponent ->
                    confirm(opponent) { it.critsShown++ }
                }
            }

            is ClientboundDamageEventPacket -> mc.execute {
                if (packet.entityId == mc.player?.id) {
                    hitSource = packet.sourceCauseId
                } else {
                    watched(packet.entityId)?.hurtSource = packet.sourceCauseId
                }
            }

            is ClientboundMoveEntityPacket if packet.hasRotation() -> mc.execute {
                val level = mc.level ?: return@execute
                val entity = packet.getEntity(level) ?: return@execute
                val opponent = watched(entity.id) ?: return@execute
                val yaw = packet.yRot
                if (!opponent.prevYaw.isNaN()) {
                    opponent.maxYawDelta = max(opponent.maxYawDelta, abs(Mth.wrapDegrees(yaw - opponent.prevYaw)))
                }
                opponent.prevYaw = yaw
            }

            is ClientboundSetEntityDataPacket -> mc.execute {
                if (!metadataSizes.containsKey(packet.id)) {
                    metadataSizes.put(packet.id, packet.packedItems.size)
                }
            }
        }
    }

    private fun watched(id: Int) = opponents.values.firstOrNull { it.entity?.id == id }

    private fun watch(entity: Player) {
        val opponent = opponents.getOrPut(entity.scoreboardName) { Opponent(entity.scoreboardName) }
        if (opponent.entity === entity || opponent.gone) {
            return
        }

        opponent.entity = entity
        opponent.prevPos = null
        opponent.prevYaw = Float.NaN
        opponent.prevHurtTime = entity.hurtTime
        opponent.airTicks = 0
        opponent.uuidVersion = entity.uuid.version()
        opponent.metadata = metadataSizes.getOrDefault(entity.id, -1)
    }

    private fun sample(opponent: Opponent, entity: Player) {
        if (player.distanceToSqr(entity) > WATCH_RANGE_SQ) {
            opponent.prevPos = null
            return
        }

        opponent.seenTicks++
        val position = entity.position()
        opponent.prevPos?.let { prev ->
            val dx = position.x - prev.x
            val dz = position.z - prev.z
            val horizontal = sqrt(dx * dx + dz * dz) * TICKS_PER_SECOND
            val vertical = (position.y - prev.y) * TICKS_PER_SECOND
            // Larger jumps are teleports
            if (horizontal < TELEPORT_SPEED) {
                opponent.speedSum += horizontal
                opponent.speedSamples++
                opponent.maxSpeed = max(opponent.maxSpeed, horizontal)
                opponent.maxRise = max(opponent.maxRise, vertical)
                opponent.maxFall = max(opponent.maxFall, -vertical)
            }
            if (dx != 0.0 || dz != 0.0 || position.y != prev.y) {
                opponent.moved = true
            }
        }
        opponent.prevPos = position

        if (entity.onGround()) {
            opponent.airTicks = 0
        } else {
            if (opponent.airTicks++ == 0) {
                opponent.airStartY = position.y
            }
            opponent.maxAirTicks = max(opponent.maxAirTicks, opponent.airTicks)
            if (opponent.airTicks > HOVER_TICKS && position.y >= opponent.airStartY - HOVER_DROP) {
                opponent.flew = true
            }
        }

        if (entity.isBlocking || entity.isUsingItem) {
            opponent.blockTicks++
        }
        if (entity.isSprinting) {
            opponent.sprintTicks++
        }
        entity.mainHandItem.takeUnless { it.isEmpty }?.let {
            opponent.item = itemName(it)
            opponent.damage = max(opponent.damage, it.attackDamage)
        }
        val armor = armorPoints(entity)
        if (armor >= opponent.armor) {
            opponent.armor = armor
            opponent.armorItems = ARMOR_SLOTS.joinToString(",") { itemName(entity.getItemBySlot(it)) }
        }

        val info = network.getPlayerInfo(entity.uuid)
        if (info != null) {
            opponent.inTab = true
            opponent.hasGameMode = opponent.hasGameMode || info.gameMode != null
            opponent.maxPing = max(opponent.maxPing, entity.ping)
        }
        opponent.antiBot = opponent.antiBot || ModuleAntiBot.isBot(entity)
    }

    /** A hurt animation within a moment after the bot's attack, not caused by someone else, is a landed hit. */
    private fun noteHurt(opponent: Opponent, entity: Player) {
        if (entity.hurtTime > opponent.prevHurtTime) {
            opponent.hurtAt = ticks
            val pending = opponent.pendingAttacks
            val byOther = opponent.hurtSource >= 0 && opponent.hurtSource != player.id
            if (!byOther && pending.any { ticks - it <= LAST_HIT_TICKS }) {
                confirm(opponent) { it.landed++ }
                landedHits.addLast(ticks)
            }
            // Attacks before this hurt either landed now or met the damage immunity
            pending.clear()
        }
        opponent.prevHurtTime = entity.hurtTime
    }

    private fun noteAttack(id: Int) {
        val self = mc.player ?: return
        val target = mc.level?.getEntity(id) as? Player ?: return
        if (active && target !== self) {
            watch(target)
        }
        val opponent = opponents[target.scoreboardName]?.takeIf { it.entity === target } ?: return

        opponent.attackedAt = ticks
        opponent.pendingAttacks.addLast(ticks)
        val distance = sqrt(target.squaredBoxedDistanceTo(self))
        val blocking = self.isBlocking || self.isUsingItem
        val crit = ModuleCriticals.running || !self.onGround() && self.deltaMovement.y < 0
        val held = self.mainHandItem
        record(opponent) { it.attack(ticks, distance, blocking, crit, itemName(held), held.attackDamage) }
    }

    /** Arm swings belong to the KillAura target, otherwise to the opponent attacked last in a fight. */
    private fun swingTarget(): Opponent? {
        val target = ModuleKillAura.targetTracker.target as? Player
        return target?.let { opponents[it.scoreboardName] }?.takeIf { it.fight != null }
            ?: opponents.values.maxByOrNull { if (it.fight != null) it.attackedAt else NEVER }
                ?.takeIf { it.fight != null }
    }

    /** Starts the fight with [opponent] when none is going on. */
    private inline fun record(opponent: Opponent, action: (Exchange) -> Unit) {
        action(opponent.total ?: Exchange(ticks, player.health).also { opponent.total = it })
        action(opponent.fight ?: Exchange(ticks, player.health).also { opponent.fight = it })
    }

    private inline fun confirm(opponent: Opponent, action: (Exchange) -> Unit) {
        opponent.total?.let(action)
        opponent.fight?.let(action)
    }

    private fun endFight(opponent: Opponent, end: String) {
        val fight = opponent.fight ?: return
        opponent.fight = null
        opponent.pendingAttacks.clear()
        report("fight summary vs ${opponent.name} ${fight.format(ownHp())} ${ownGear()} end=$end")
    }

    // The hurt animation restarts with every hit taken
    private fun noteHit() {
        val hurtTime = mc.player?.hurtTime ?: 0
        if (hurtTime > prevHurtTime) {
            val attacker = attacker()
            attacker?.entity?.let { entity ->
                val reach = sqrt(player.squaredBoxedDistanceTo(entity))
                attacker.hits++
                attacker.reachSum += reach
                attacker.reachMin = min(attacker.reachMin, reach)
                attacker.reachMax = max(attacker.reachMax, reach)
                val hp = freshHp
                record(attacker) {
                    it.taken(ticks)
                    it.takenHp += hp
                }
            }
            lastHitBy = attacker
            lastHitAt = ticks
            hits.addLast(Hit(ticks, attacker?.name))
        }
        prevHurtTime = hurtTime
        hitSource = -1
        freshHp = 0f
    }

    /** The player the server names as the source, otherwise the closest one swinging and looking at the bot. */
    private fun attacker(): Opponent? {
        if (hitSource >= 0) {
            (world.getEntity(hitSource) as? Player)?.let { source ->
                if (active && source !== player) {
                    watch(source)
                }
                return opponents[source.scoreboardName]?.takeIf { it.entity != null }
            }
        }

        var best: Opponent? = null
        var bestScore = Double.MAX_VALUE
        for (opponent in opponents.values) {
            val entity = opponent.entity ?: continue
            val reachSq = player.squaredBoxedDistanceTo(entity)
            if (reachSq > ATTACK_RANGE_SQ) {
                continue
            }
            val toUs = player.boundingBox.center.subtract(entity.eyePosition).normalize()
            val facing = Math.toDegrees(acos(entity.getViewVector(1f).dot(toUs).coerceIn(-1.0, 1.0)))
            val swingBonus = if (ticks - opponent.swungAt <= SWING_TICKS) SWING_BONUS else 0.0
            val score = sqrt(reachSq) + facing / FACING_DEGREES_PER_BLOCK - swingBonus
            if (score < bestScore) {
                best = opponent
                bestScore = score
            }
        }
        return best
    }

    private fun reportDeath() {
        deathReported = true
        val end = deathAt ?: ticks
        val start = end - WINDOW_TICKS
        val recentHits = hits.filter { it.tick in start..end }
        val hpLost = healthDrops.filter { it.tick in start..end }.sumOf { it.amount.toDouble() }
        val hitters = recentHits.groupingBy { it.attacker ?: "?" }.eachCount()
            .entries.sortedByDescending { it.value }

        val killer = deathKiller
        val statsName = killer ?: hitters.firstOrNull { it.key != "?" }?.key
        val stats = statsName?.let { name -> opponents[name]?.let { "$name: ${summary(it)}" } } ?: "untracked"
        val ours = statsName?.let { opponents[it]?.total?.format(ownHp()) } ?: "none"
        report(
            "death killer=${killer ?: "unknown"} hp_lost_last5s=${hpLost.format()} " +
                "hits_taken_last5s=${recentHits.size} " +
                "hit_by=${hitters.joinToString(",") { "${it.key}x${it.value}" }.ifEmpty { "none" }} " +
                "killer_stats=[$stats] ours=[$ours] $aliveGear"
        )
        MapRecorder.event(
            "death", "killer" to killer, "hpLost" to hpLost, "hits" to recentHits.size,
            "ago" to ticks - end, "stats" to stats,
        )
        Clips.trigger(
            if (killer != null || recentHits.isNotEmpty()) Clips.Trigger.LOST_FIGHT else Clips.Trigger.DEATH
        )
    }

    private fun flush(reason: String) {
        for (opponent in opponents.values) {
            if (opponent.entity != null) {
                log(opponent, reason)
            }
        }
    }

    private fun log(opponent: Opponent, end: String) {
        endFight(opponent, end)
        opponent.entity = null
        opponent.end = end
        opponent.gone = end in GONE
        if (opponent.seenTicks > 0) {
            val ours = opponent.total?.format(ownHp()) ?: "none"
            report("opponent ${opponent.name} ${summary(opponent)} ours=[$ours]")
        }
    }

    private fun ownHp() = mc.player?.health ?: 0f

    /** The bot's gear: what it holds, its best hotbar weapon, armor points, health and golden apples. */
    private fun ownGear(): String {
        val self = mc.player ?: return "own=none"
        val best = Slots.Hotbar.filter { it.itemStack.isSword || it.itemStack.isAxe }
            .maxByOrNull { it.itemStack.attackDamage }?.itemStack ?: ItemStack.EMPTY
        val apples = Slots.All.sumOf { slot ->
            val stack = slot.itemStack
            if (stack.`is`(Items.GOLDEN_APPLE) || stack.`is`(Items.ENCHANTED_GOLDEN_APPLE)) stack.count else 0
        }
        return "own_held=${itemName(self.mainHandItem)} " +
            "own_best=${itemName(best)}/${best.attackDamage.format()} own_armor=${armorPoints(self).format()} " +
            "own_hp=${self.health.toDouble().format()} gapples=$apples"
    }

    private fun itemName(stack: ItemStack) =
        if (stack.isEmpty) "none" else BuiltInRegistries.ITEM.getKey(stack.item).path

    /** Armor points of what [entity] wears. */
    fun armorPoints(entity: LivingEntity): Double = ARMOR_SLOTS.sumOf { armorPoints(entity.getItemBySlot(it)) }

    /** Items of a 1.8 server can come without their armor attribute, so the item type's own value counts as well. */
    fun armorPoints(stack: ItemStack): Double = max(stack.armorValue, stack.item.components().armorValue)

    /** The most damage of an item [other] was seen holding this round, at least what it holds now. */
    fun weaponDamage(other: Player): Double {
        val seen = opponents[other.scoreboardName]?.takeIf { it.entity === other }?.damage ?: 0.0
        return max(seen, other.mainHandItem.attackDamage)
    }

    private fun summary(opponent: Opponent): String {
        val (kind, reason) = kind(opponent)
        val seconds = opponent.seenTicks.toDouble() / TICKS_PER_SECOND
        val avgSpeed = if (opponent.speedSamples > 0) opponent.speedSum / opponent.speedSamples else 0.0
        val reach = if (opponent.hits > 0) {
            "${opponent.reachMin.format()}/${(opponent.reachSum / opponent.hits).format()}/" +
                opponent.reachMax.format()
        } else {
            "none"
        }
        return "kind=$kind reason=$reason end=${opponent.end} seen=${seconds.format()}s " +
            "speed_max=${opponent.maxSpeed.format()} speed_avg=${avgSpeed.format()} " +
            "vy_up=${opponent.maxRise.format()} vy_down=${opponent.maxFall.format()} " +
            "air_max=${opponent.maxAirTicks} flew=${opponent.flew} " +
            "swings=${(opponent.swings / seconds.coerceAtLeast(1.0)).format()}/s " +
            "yaw_snap=${opponent.maxYawDelta.toDouble().format()} " +
            "hits_on_us=${opponent.hits} reach=$reach " +
            "block=${percent(opponent.blockTicks, opponent.seenTicks)}% " +
            "sprint=${percent(opponent.sprintTicks, opponent.seenTicks)}% " +
            "item=${opponent.item} armor=${opponent.armor.format()} armor_items=${opponent.armorItems} " +
            "kills=${kills[opponent.name] ?: 0}"
    }

    /** Like AntiBot: missing from the tab list, NPC UUIDs, never moving or flagged by AntiBot are bots. */
    private fun kind(opponent: Opponent): Pair<String, String> {
        val strong = buildList {
            if (!opponent.inTab) add("no_tab")
            if (opponent.uuidVersion == NPC_UUID_VERSION) add("uuid_v2")
            if (!opponent.moved && opponent.seenTicks >= STATIC_TICKS) add("static")
            if (!VALID_NAME.matches(opponent.name)) add("name")
            if (opponent.antiBot) add("antibot")
        }
        val weak = buildList {
            if (opponent.inTab && opponent.maxPing <= 0) add("ping0")
            if (opponent.inTab && !opponent.hasGameMode) add("no_gamemode")
            if (opponent.metadata in 0..MINIMAL_METADATA) add("meta${opponent.metadata}")
        }
        val isBot = strong.isNotEmpty() || weak.size >= 2
        val evidence = "uuid_v${opponent.uuidVersion},ping${opponent.maxPing},meta${opponent.metadata}"
        val flags = (strong + weak).joinToString(",")
        val reason = when {
            isBot -> "$flags,$evidence"
            flags.isNotEmpty() -> "tab,moved,$flags,$evidence"
            else -> "tab,moved,$evidence"
        }
        return (if (isBot) "bot" else "player") to reason
    }

    private fun percent(part: Int, total: Int) = if (total > 0) part * PERCENT / total else 0

    private fun Double.format() = "%.1f".format(Locale.ROOT, this)

    val ARMOR_SLOTS = arrayOf(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET)
    private val VALID_NAME = Regex("[A-Za-z0-9_]{3,16}")
    private val GONE = setOf("killed", "death", "spectator")

    private const val NEVER = Int.MIN_VALUE / 2
    private const val TICKS_PER_SECOND = 20
    private const val PERCENT = 100

    private const val WATCH_RANGE_SQ = 12.0 * 12.0
    private const val ATTACK_RANGE_SQ = 8.0 * 8.0
    private const val TELEPORT_SPEED = 100.0
    private const val HOVER_TICKS = 40
    private const val HOVER_DROP = 1.0

    private const val SWING_TICKS = 5
    private const val SWING_BONUS = 2.0
    private const val FACING_DEGREES_PER_BLOCK = 15.0

    private const val TARGET_TICKS = 40
    private const val LAST_HIT_TICKS = 10
    private const val HIT_HP_TICKS = 5
    private const val FIGHT_IDLE_TICKS = 60

    private const val WINDOW_TICKS = 100
    private const val HISTORY_TICKS = 200
    private const val KILLER_WAIT_TICKS = 20
    private const val GEAR_TICKS = 20

    private const val NPC_UUID_VERSION = 2
    private const val STATIC_TICKS = 160
    private const val MINIMAL_METADATA = 3

}
