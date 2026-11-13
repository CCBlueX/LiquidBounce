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

import net.ccbluex.liquidbounce.config.types.group.ValueGroup
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.event.events.ChatReceiveEvent
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.PlayerTickEvent
import net.ccbluex.liquidbounce.event.events.RotationUpdateEvent
import net.ccbluex.liquidbounce.event.events.TitleEvent
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.features.KillAuraFightBot
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.ModuleFly
import net.ccbluex.liquidbounce.features.module.modules.player.gamebot.Bridge
import net.ccbluex.liquidbounce.features.module.modules.player.gamebot.Game
import net.ccbluex.liquidbounce.features.module.modules.player.gamebot.MapRecorder
import net.ccbluex.liquidbounce.features.module.modules.player.gamebot.ModuleGameBot
import net.ccbluex.liquidbounce.features.module.modules.player.gamebot.ModuleGameBot.report
import net.ccbluex.liquidbounce.features.module.modules.player.gamebot.Opponents
import net.ccbluex.liquidbounce.features.module.modules.player.gamebot.Setbacks
import net.ccbluex.liquidbounce.features.module.modules.player.gamebot.VoidSafety
import net.ccbluex.liquidbounce.features.module.modules.world.ModuleAutoTool
import net.ccbluex.liquidbounce.features.module.modules.world.fucker.ModuleFucker
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsValueGroup
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.block.anotherBedPartDirection
import net.ccbluex.liquidbounce.utils.block.bed.BedBlockTracker
import net.ccbluex.liquidbounce.utils.block.isBed
import net.ccbluex.liquidbounce.utils.entity.doesNotCollideBelow
import net.ccbluex.liquidbounce.utils.entity.interactEntity
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.ccbluex.liquidbounce.utils.item.isSword
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.CRITICAL_MODIFICATION
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.math.horizontalDistanceTo
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.ccbluex.liquidbounce.utils.navigation.Navigator
import net.ccbluex.liquidbounce.utils.text.stripMinecraftColorCodes
import net.minecraft.client.gui.screens.DeathScreen
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.gui.screens.inventory.InventoryScreen
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.tags.ItemTags
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.ContainerInput
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import net.minecraft.world.scores.DisplaySlot
import net.minecraft.world.scores.PlayerTeam
import net.minecraft.world.scores.TeamColor
import java.util.Locale
import kotlin.math.abs

/**
 * Bed Wars: finds the own bed and island generator after the spawn. [Strategy.HOLD_BASE] stays on the island at the
 * generator, fights whoever comes close to the bed and walks back there after every respawn until the bed is gone.
 * [Strategy.RUSH] gathers iron, buys wool and a stone sword at the shop, bridges to the closest enemy bed and breaks
 * it with Fucker.
 */
@Suppress("TooManyFunctions", "LargeClass")
internal object GameBotBedWars : Game("BedWars", "/play bedwars_eight_one"), BedBlockTracker.Subscriber {

    private enum class Strategy(override val tag: String) : Tagged {
        HOLD_BASE("Hold base"),
        RUSH("Rush"),
    }

    private val strategy by enumChoice("Strategy", Strategy.HOLD_BASE)

    /** How far from the bed the bot walks at the base, to the generator or after an opponent. */
    private val baseRadius by int("BaseRadius", 14, 6..32, "blocks")

    /** Opponents this close to the bed are fought at the base. */
    private val defendRange by float("DefendRange", 10f, 3f..24f, "blocks")
    private val fightRange by float("FightRange", 6f, 3f..12f, "blocks")

    /** Stands at the island generator rather than at the bed, so iron and gold are picked up. */
    private val collect by boolean("CollectResources", true)

    /** Plays a round that is already on when turned on: the closest bed is taken as the own one. */
    private val joinRunning by boolean("JoinRunning", false)

    /** Server lines saying the round started; player chat is never read. */
    private val startKeywords by textList(
        "StartKeywords", mutableListOf("Protect your bed and destroy the enemy beds."),
    )

    /** Lines and titles saying the own bed is gone; `BED DESTRUCTION > <Team> Bed` with the own team counts too. */
    private val bedLostKeywords by textList(
        "BedLostKeywords", mutableListOf("BED DESTRUCTION > Your Bed", "BED DESTROYED"),
    )

    /** Names of the shop NPC or the hologram above it. */
    private val shopNames by textList("ShopNames", mutableListOf("ITEM SHOP", "Item Shop"))

    private object Rush : ValueGroup("Rush") {
        /** Iron to gather before going to the shop. */
        val iron by int("Iron", 24, 4..64)
        val gatherTime by int("GatherTime", 40, 5..180, "s")
        val wool by int("Wool", 48, 16..128)
        val breakTime by int("BreakTime", 20, 5..60, "s")
        val useFightBot by boolean("FightBot", true)
    }

    init {
        tree(Rush)
    }

    private val rotations = tree(RotationsValueGroup(this))
    override val navigator = Navigator(this, this, rotations, allowPlace = true) { ModuleScaffold.running }
    private val voidSafety = VoidSafety(this, navigator, ::activitySituation) { false }
    private val bridge = Bridge(this, navigator)

    override val maxLayers: Int
        get() = 1

    private enum class Phase { IDLE, SPAWN, HOLD, DEFEND, GATHER, SHOP, RUSH, BREAK, FIGHT, RESPAWN, END }

    private var phase = Phase.IDLE
    private var phaseSince = 0
    private var ticks = 0

    private var startHeardAt: Int? = null
    private var spawn: Vec3? = null
    private var team: String? = null

    private var ownBed: BlockPos? = null
    private var ownBedParts = emptyList<BlockPos>()
    private var bedAlive = true
    private var generator: Vec3? = null

    private var opponent: Player? = null
    private var calmTicks = 0
    private var resumePhase = Phase.RUSH
    private var ownsFightBot = false
    private var ownsFucker = false
    private var ownsAutoTool = false

    private var gatherUntil = 0

    private var shopNpc: Entity? = null
    private var shopClicks = 0
    private var shopClickedAt = NEVER
    private var buyAt = 0
    private var ironBeforeBuy: Int? = null

    private var targetBed: BlockPos? = null
    private val unreachableUntil = HashMap<BlockPos, Int>()
    private var bestDistance = Double.MAX_VALUE
    private var progressAt = 0
    private var strandedTicks = 0

    /** Whether Fly was on last tick, which someone else turned on; the bot stands aside until it is off. */
    private var flyPaused = false

    private var walkGoal: Vec3? = null
    private var walkRadius = 1.0

    override val isPlaying: Boolean
        get() = phase != Phase.IDLE && phase != Phase.END

    override val hasStarted: Boolean
        get() = isPlaying && phase != Phase.SPAWN && phase != Phase.RESPAWN

    override val isFighting: Boolean
        get() = phase == Phase.DEFEND || phase == Phase.FIGHT

    override val isOpeningChest: Boolean
        get() = phase == Phase.SHOP

    override val respawns: Boolean
        get() = isPlaying && bedAlive

    override val loots: Boolean
        get() = false

    override fun speedHold(): String? = when {
        strategy == Strategy.HOLD_BASE -> "hold base"
        bridge.isActive || navigator.isBuilding -> "bridge"
        voidSafety.isFalling -> "fall"
        voidSafety.isNearEdge -> "void edge"
        phase == Phase.SHOP -> "shop"
        else -> null
    }

    override fun startAnnounced() {
        if (phase == Phase.IDLE) {
            startHeardAt = ticks
            enter(Phase.SPAWN, "start announced")
        }
    }

    override fun died(reason: String) {
        if (hasStarted) {
            stopActing()
            enter(Phase.RESPAWN, "died ${reason.trim()}")
        }
    }

    override fun end() {
        stopActing()
        enter(Phase.END, "round over")
    }

    override fun enable() {
        BedBlockTracker.subscribe(this)
        forgetWorld()
        phase = Phase.IDLE
    }

    override fun disable() {
        stopActing()
        navigator.reset()
        BedBlockTracker.unsubscribe(this)
        phase = Phase.IDLE
    }

    @Suppress("unused")
    private val worldChangeHandler = handler<WorldChangeEvent> {
        stopActing()
        // The pregame lobby may sit in another world than the map the start teleports to.
        if (phase == Phase.SPAWN) {
            return@handler
        }
        forgetWorld()
        enter(Phase.IDLE, "world change")
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

        if (startKeywords.any(message::contains)) {
            startAnnounced()
            return@handler
        }
        if (!isPlaying) {
            return@handler
        }

        val team = team
        when {
            isBedLostMessage(message, team) -> loseBed(message)
            team != null && message.contains("TEAM ELIMINATED > $team Team") ->
                ModuleGameBot.finish(won = false, reason = message)

            message.startsWith("Winner") &&
                (message.contains(player.scoreboardName) || team != null && message.contains(team)) ->
                ModuleGameBot.finish(won = true, reason = message)
        }
    }

    private fun isBedLostMessage(message: String, team: String?) =
        bedLostKeywords.any(message::contains) || team != null && message.startsWith("BED DESTRUCTION > $team Bed")

    @Suppress("unused")
    private val titleHandler = handler<TitleEvent.Title> { event -> readTitle(event.text?.string) }

    @Suppress("unused")
    private val subtitleHandler = handler<TitleEvent.Subtitle> { event -> readTitle(event.text?.string) }

    private fun readTitle(text: String?) {
        if (text != null && isPlaying && bedLostKeywords.any(text::contains)) {
            loseBed(text)
        }
    }

    @Suppress("unused")
    private val deathScreenHandler = handler<GameTickEvent> {
        val self = mc.player ?: return@handler
        if (isPlaying && mc.gui.screen() is DeathScreen) {
            self.respawn()
            mc.gui.setScreen(null)
        }
    }

    @Suppress("unused")
    private val rotationHandler = handler<RotationUpdateEvent> {
        val npc = shopNpc?.takeIf { phase == Phase.SHOP && it.isAlive && isInShopReach(it) } ?: return@handler
        RotationManager.setRotationTarget(
            aimAt(npc),
            valueGroup = rotations,
            priority = Priority.IMPORTANT_FOR_USAGE_1,
            provider = this,
        )
    }

    @Suppress("unused")
    private val tickHandler = handler<PlayerTickEvent> {
        ticks++
        walkGoal = null
        if (!isPlaying) {
            if (phase == Phase.IDLE && joinRunning && player.onGround() && closestBed(player.position()) != null) {
                start("joined running round")
            }
            return@handler
        }
        if (phase == Phase.SPAWN) {
            waitForSpawn()
            return@handler
        }

        watchBase()
        if (checkSpectator()) {
            return@handler
        }
        guardOwnBed()
        if (pausedForFly() || Setbacks.isHolding || voidSafety.isFalling) {
            return@handler
        }

        when (phase) {
            Phase.HOLD -> hold()
            Phase.DEFEND -> defend()
            Phase.GATHER -> gather()
            Phase.SHOP -> shop()
            Phase.RUSH -> rush()
            Phase.BREAK -> breakBed()
            Phase.FIGHT -> fight()
            Phase.RESPAWN -> respawn()
            Phase.IDLE, Phase.SPAWN, Phase.END -> Unit
        }
    }

    @Suppress("unused")
    private val movementHandler = handler<MovementInputEvent>(priority = CRITICAL_MODIFICATION) { event ->
        if (Setbacks.isHolding && isPlaying) {
            event.directionalInput = DirectionalInput.NONE
            event.jump = false
            event.sneak = false
            return@handler
        }

        val goal = walkGoal
        if (goal == null) {
            navigator.halt()
            return@handler
        }

        if (mc.gui.screen() is AbstractContainerScreen<*>) {
            navigator.halt()
            event.directionalInput = DirectionalInput.NONE
            event.jump = false
            return@handler
        }

        navigator.navigate(event, goal, walkRadius)
    }

    private fun waitForSpawn() {
        val heardAt = startHeardAt ?: ticks
        if (ticks - heardAt >= SPAWN_TICKS && player.onGround()) {
            start("spawned")
        }
    }

    private fun start(reason: String) {
        startHeardAt = null
        spawn = player.position()
        bedAlive = true
        ownBed = null
        ownBedParts = emptyList()
        generator = null
        team = null
        unreachableUntil.clear()
        ModuleGameBot.roundStarted()
        enter(basePhase(), "$reason strategy=${strategy.tag}")
    }

    private fun basePhase() = if (strategy == Strategy.RUSH) Phase.GATHER else Phase.HOLD

    /** Finds the own team, bed and generator once they are known, and notices the bed is gone. */
    private fun watchBase() {
        if (team == null && ticks % TEAM_TICKS == 0) {
            team = readTeam()?.also { report("team $it") }
        }

        val bed = ownBed
        if (bed == null) {
            if (ticks % BED_TICKS == 0) {
                val center = spawn ?: player.position()
                closestBed(center)?.let(::noteOwnBed)
            }
            return
        }

        if (bedAlive && world.isLoaded(bed) && !world.getBlockState(bed).isBed) {
            loseBed("bed block gone")
        }
        if (generator == null && ticks % GENERATOR_TICKS == 0) {
            generator = findGenerator(Vec3.atCenterOf(bed))?.also {
                report("generator at ${formatPosition(it)} bed_dist=${formatDistance(it.distanceTo(basePoint()))}")
            }
        }
    }

    private fun noteOwnBed(pos: BlockPos) {
        ownBed = pos
        val other = world.getBlockState(pos).anotherBedPartDirection()?.let(pos::relative)
        ownBedParts = listOfNotNull(pos, other)
        report("bed at ${pos.x},${pos.y},${pos.z}")
        MapRecorder.event("bed", "pos" to pos, "state" to "own")
    }

    /** The bed closest to [center] within [BED_SEARCH] blocks. */
    private fun closestBed(center: Vec3): BlockPos? = BedBlockTracker.allPositions()
        .filter { Vec3.atCenterOf(it).distanceToSqr(center) <= BED_SEARCH * BED_SEARCH }
        .minByOrNull { Vec3.atCenterOf(it).distanceToSqr(center) }
        ?.immutable()

    private fun loseBed(reason: String) {
        if (!bedAlive) {
            return
        }
        bedAlive = false
        report("bed lost reason=${reason.trim()}")
        MapRecorder.event("bed", "pos" to ownBed, "state" to "lost")
    }

    /**
     * The own team from the sidebar line marked `YOU`, such as `R Red: ✔ YOU`, else from the color of the
     * scoreboard team.
     */
    private fun readTeam(): String? {
        val scoreboard = world.scoreboard
        scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR)?.let { objective ->
            for (entry in scoreboard.listPlayerScores(objective)) {
                val line = PlayerTeam.formatNameForTeam(scoreboard.getPlayersTeam(entry.owner()), entry.ownerName())
                    .string.stripMinecraftColorCodes()
                OWN_TEAM_LINE.find(line)?.let { return it.groupValues[1] }
            }
        }

        val color = scoreboard.getPlayersTeam(player.scoreboardName)?.color?.orElse(null) ?: return null
        return when (color) {
            TeamColor.LIGHT_PURPLE -> "Pink"
            TeamColor.DARK_GRAY -> "Gray"
            else -> color.serializedName.replaceFirstChar { it.titlecase(Locale.ROOT) }
        }
    }

    /** Where iron and gold lie on the ground within the base, which is where the island generator drops them. */
    private fun findGenerator(bed: Vec3): Vec3? {
        val radius = baseRadius.toDouble()
        val box = AABB.ofSize(bed, radius * 2, GENERATOR_HEIGHT * 2, radius * 2)
        return world.getEntitiesOfClass(ItemEntity::class.java, box) {
            it.item.`is`(Items.IRON_INGOT) || it.item.`is`(Items.GOLD_INGOT)
        }.minByOrNull { it.distanceToSqr(bed) }?.position()
    }

    /** Respawning shows as spectator; once the bed is gone that is the final death. */
    private fun checkSpectator(): Boolean {
        val spectating = player.isSpectator || player.abilities.mayfly && !ModuleFly.enabled
        if (!spectating) {
            return false
        }

        if (!bedAlive) {
            ModuleGameBot.finish(won = false, reason = "final death")
        } else if (phase != Phase.RESPAWN) {
            died("spectator")
        }
        return true
    }

    /** Turns Fucker off while it would break the own bed, which it does without a self-bed mode that knows it. */
    private fun guardOwnBed() {
        if (!ModuleFucker.enabled || !bedAlive) {
            return
        }

        val reach = ModuleFucker.range + 1.0
        val exposed = ownBedParts.firstOrNull { pos ->
            val state = world.getBlockState(pos)
            player.eyePosition.distanceToSqr(Vec3.atCenterOf(pos)) <= reach * reach &&
                state.isBed && ModuleFucker.isTarget(pos, state)
        } ?: return

        ModuleFucker.enabled = false
        ownsFucker = false
        report("fucker off reason=own bed is a target at ${exposed.x},${exposed.y},${exposed.z}")
    }

    /** Fly turned on by someone else, such as the bypass lab, moves the player; the bot only fights meanwhile. */
    private fun pausedForFly(): Boolean {
        val flying = ModuleFly.enabled
        if (flying != flyPaused) {
            flyPaused = flying
            report(if (flying) "pause reason=fly" else "resume after fly")
            if (flying) {
                bridge.stop()
                voidSafety.stop()
            }
        }
        return flying
    }

    private fun hold() {
        defender()?.let { return startDefend(it) }
        standAtBase()
    }

    private fun standAtBase() {
        val generator = generator?.takeIf { collect }
        walkGoal = generator ?: basePoint()
        walkRadius = if (generator != null) GENERATOR_RADIUS else BED_RADIUS
    }

    /** The opponent to fight at the base: close to the bed or to the bot, on ground within the base. */
    private fun defender(): Player? {
        val bed = basePoint()
        return Opponents.around()
            .filter { isDefendable(it) }
            .filter { it.position().distanceTo(bed) <= defendRange || it.distanceTo(player) <= fightRange }
            .minByOrNull { it.distanceToSqr(player) }
    }

    private fun isDefendable(other: Player) = Opponents.isOpponent(other) && isInBase(other.position()) &&
        !other.doesNotCollideBelow()

    private fun isInBase(pos: Vec3): Boolean {
        val base = basePoint()
        return pos.horizontalDistanceTo(base) <= baseRadius && abs(pos.y - base.y) <= BASE_HEIGHT
    }

    private fun startDefend(target: Player) {
        opponent = target
        calmTicks = 0
        enter(Phase.DEFEND, "opponent near bed ${target.scoreboardName}")
        MapRecorder.event("fight", "phase" to "start", "target" to target.scoreboardName, "reason" to "defend")
    }

    /** Follows the opponent within the base only; KillAura hits it, VoidSafety keeps the bot on the island. */
    private fun defend() {
        val target = opponent?.takeIf(::isDefendable) ?: defender()
        if (target == null) {
            if (++calmTicks >= CALM_TICKS) {
                opponent = null
                MapRecorder.event("fight", "phase" to "stop")
                enter(Phase.HOLD, "base clear")
            }
            return
        }

        calmTicks = 0
        if (target !== opponent) {
            opponent = target
            report("target ${target.scoreboardName}")
        }
        walkGoal = target.position()
        walkRadius = MELEE_RADIUS
    }

    private fun respawn() {
        if (!player.onGround() || ticks - phaseSince < MIN_RESPAWN_TICKS) {
            return
        }

        val waited = ticks - phaseSince
        if (isInBase(player.position()) || waited >= RESPAWN_TIMEOUT_TICKS) {
            enter(basePhase(), "respawned after ${waited / TICKS_PER_SECOND}s")
        }
    }

    private fun gather() {
        opponentInReach()?.let { return startFight(it) }

        val iron = count(Items.IRON_INGOT)
        val wool = woolCount()
        when {
            iron >= Rush.iron -> enter(Phase.SHOP, "iron=$iron")
            ticks < gatherUntil -> standAtBase()
            wool >= MIN_RUSH_WOOL -> enter(Phase.RUSH, "wool=$wool")
            iron >= WOOL_IRON -> enter(Phase.SHOP, "gather time over iron=$iron")
            else -> standAtBase()
        }
    }

    private fun shop() {
        val screen = mc.gui.screen() as? AbstractContainerScreen<*>
        if (screen != null && screen !is InventoryScreen) {
            buy(screen)
            return
        }
        if (ticks - phaseSince > SHOP_TICKS || shopClicks >= MAX_SHOP_CLICKS) {
            leaveShop("shop not opened clicks=$shopClicks")
            return
        }

        val npc = shopNpc?.takeIf { it.isAlive } ?: findShop()?.also {
            shopNpc = it
            val kind = BuiltInRegistries.ENTITY_TYPE.getKey(it.type).path
            report("shop at ${formatPosition(it.position())} kind=$kind")
        }
        if (npc == null) {
            leaveShop("no shop npc")
            return
        }

        if (!isInShopReach(npc)) {
            walkGoal = npc.position()
            walkRadius = SHOP_RADIUS
            return
        }

        val aimed = RotationManager.serverRotation.rotationDeltaTo(aimAt(npc)).length() <= AIM_TOLERANCE
        if (aimed && ticks - shopClickedAt >= SCREEN_TICKS) {
            interactEntity(npc)
            shopClicks++
            shopClickedAt = ticks
        }
    }

    /** The NPC named one of [shopNames], or the one under such a hologram, closest to the base. */
    private fun findShop(): Entity? {
        val base = basePoint()
        val labels = world.entitiesForRendering().filter { entity ->
            val name = entity.customName?.string?.stripMinecraftColorCodes() ?: return@filter false
            shopNames.any { name.contains(it, ignoreCase = true) }
        }

        return labels.mapNotNull { label ->
            if (label is LivingEntity && label !is ArmorStand) {
                label
            } else {
                world.getEntities(label, label.boundingBox.inflate(HOLOGRAM_REACH)) { isNpc(it) }
                    .minByOrNull { it.distanceToSqr(label) }
            }
        }.filter { it.position().distanceTo(base) <= baseRadius * 2.0 }.minByOrNull { it.distanceToSqr(base) }
    }

    private fun isNpc(entity: Entity) = entity is LivingEntity && entity !is ArmorStand && entity !== player &&
        !(entity is Player && Opponents.isOpponent(entity))

    private fun isInShopReach(npc: Entity) =
        player.eyePosition.distanceToSqr(npc.boundingBox.center) <= SHOP_REACH * SHOP_REACH

    private fun aimAt(entity: Entity) = Rotation.lookingAt(entity.boundingBox.center, player.eyePosition)

    /** Buys a stone sword, then wool up to [Rush.wool], one click per [BUY_TICKS]; stops once iron does not drop. */
    private fun buy(screen: AbstractContainerScreen<*>) {
        if (ticks < buyAt) {
            return
        }

        val iron = count(Items.IRON_INGOT)
        val before = ironBeforeBuy
        if (before != null && iron >= before) {
            closeShop("purchase refused iron=$iron")
            return
        }

        val shopSlots = screen.menu.slots.withIndex().filter { it.value.container !is Inventory }
        val pick = when {
            iron >= SWORD_IRON && !hasStoneSword() -> shopSlots.firstOrNull { it.value.item.`is`(Items.STONE_SWORD) }
            iron >= WOOL_IRON && woolCount() < Rush.wool -> shopSlots.firstOrNull { it.value.item.`is`(ItemTags.WOOL) }
            else -> null
        }
        if (pick == null) {
            closeShop("done")
            return
        }

        interaction.handleContainerInput(screen.menu.containerId, pick.index, 0, ContainerInput.PICKUP, player)
        report("shop buy ${itemName(pick.value.item)} slot=${pick.index} iron=$iron")
        ironBeforeBuy = iron
        buyAt = ticks + BUY_TICKS
    }

    private fun closeShop(reason: String) {
        player.closeContainer()
        leaveShop(reason)
    }

    private fun leaveShop(reason: String) {
        val wool = woolCount()
        val summary = "$reason wool=$wool sword=${hasStoneSword()} iron=${count(Items.IRON_INGOT)}"
        enter(if (wool >= MIN_RUSH_WOOL) Phase.RUSH else Phase.GATHER, "shop $summary")
    }

    private fun rush() {
        opponentInReach()?.let { return startFight(it) }

        val bed = targetBed?.takeIf(::isEnemyBed) ?: pickBed()
        if (bed == null) {
            enter(Phase.GATHER, "no enemy bed known")
            return
        }

        val center = Vec3.atCenterOf(bed)
        if (player.eyePosition.distanceTo(center) <= ModuleFucker.range - REACH_MARGIN) {
            enter(Phase.BREAK, "at bed ${bed.x},${bed.y},${bed.z}")
            return
        }
        if (ModuleScaffold.blockCount <= 0 && navigator.isStranded) {
            enter(Phase.GATHER, "out of blocks")
            return
        }

        walkGoal = center
        walkRadius = BREAK_RADIUS
        checkProgress(bed, center)
    }

    private fun pickBed(): BlockPos? {
        val position = player.position()
        val next = BedBlockTracker.allPositions()
            .filter { isEnemyBed(it) && (unreachableUntil[it] ?: 0) <= ticks }
            .minByOrNull { Vec3.atCenterOf(it).distanceToSqr(position) }
            ?.immutable()
        if (next != targetBed) {
            val distance = next?.let { formatDistance(Vec3.atCenterOf(it).distanceTo(position)) }
            report(if (next == null) "target none" else "target bed ${next.x},${next.y},${next.z} distance=$distance")
            MapRecorder.event("target", "label" to "bed", "goal" to next)
            resetProgress()
        }
        targetBed = next
        return next
    }

    private fun isEnemyBed(pos: BlockPos): Boolean {
        val own = ownBed
        return world.getBlockState(pos).isBed &&
            (own == null || Vec3.atCenterOf(own).distanceTo(Vec3.atCenterOf(pos)) > OWN_BED_SEPARATION)
    }

    /** Gives a bed up for a while once the walk gets no closer. */
    private fun checkProgress(bed: BlockPos, goal: Vec3) {
        val distance = player.position().distanceTo(goal)
        if (distance < bestDistance - MIN_PROGRESS) {
            bestDistance = distance
            progressAt = ticks
        }

        strandedTicks = if (navigator.isStranded) strandedTicks + 1 else 0
        if (strandedTicks >= STRANDED_TICKS || ticks - progressAt >= NO_PROGRESS_TICKS) {
            giveUpBed(bed, "unreachable")
        }
    }

    private fun giveUpBed(bed: BlockPos, reason: String) {
        unreachableUntil[bed] = ticks + RETRY_TICKS
        report("$reason bed ${bed.x},${bed.y},${bed.z}")
        MapRecorder.event("unreachable", "label" to "bed", "goal" to bed)
        targetBed = null
        resetProgress()
    }

    /** Fucker breaks the bed and whatever covers it, AutoTool picks the tool; both go back off afterwards. */
    private fun breakBed() {
        val bed = targetBed
        if (bed == null || !world.getBlockState(bed).isBed) {
            releaseBreaking()
            bed?.let { report("bed broken ${it.x},${it.y},${it.z}") }
            targetBed = null
            enter(if (woolCount() >= MIN_RUSH_WOOL) Phase.RUSH else Phase.GATHER, "bed broken")
            return
        }
        if (ticks - phaseSince > Rush.breakTime * TICKS_PER_SECOND) {
            releaseBreaking()
            giveUpBed(bed, "break timeout")
            enter(Phase.RUSH, "break timeout")
            return
        }

        holdBreaking()
        walkGoal = Vec3.atCenterOf(bed)
        walkRadius = BREAK_RADIUS
    }

    private fun holdBreaking() {
        if (!ModuleFucker.enabled) {
            ModuleFucker.enabled = true
            ownsFucker = true
            report("fucker on")
        }
        if (!ModuleAutoTool.enabled) {
            ModuleAutoTool.enabled = true
            ownsAutoTool = true
        }
    }

    private fun releaseBreaking() {
        if (ownsFucker) {
            ModuleFucker.enabled = false
            ownsFucker = false
            report("fucker off")
        }
        if (ownsAutoTool) {
            ModuleAutoTool.enabled = false
            ownsAutoTool = false
        }
    }

    private fun startFight(target: Player) {
        resumePhase = phase
        opponent = target
        calmTicks = 0
        holdFightBot()
        enter(Phase.FIGHT, "opponent near ${target.scoreboardName}")
        MapRecorder.event("fight", "phase" to "start", "target" to target.scoreboardName, "reason" to "rush")
    }

    private fun fight() {
        val current = opponentInReach(fightRange + FIGHT_MARGIN)
        if (current == null) {
            if (++calmTicks >= CALM_TICKS) {
                releaseFightBot()
                opponent = null
                MapRecorder.event("fight", "phase" to "stop")
                enter(resumePhase, "fight over")
            }
            return
        }

        calmTicks = 0
        holdFightBot()
        if (current !== opponent) {
            opponent = current
            report("target ${current.scoreboardName}")
        }
        // FightBot walks on its own; without it the walk only follows onto ground.
        if (!Rush.useFightBot && !current.doesNotCollideBelow()) {
            walkGoal = current.position()
            walkRadius = MELEE_RADIUS
        }
    }

    /** The closest opponent within [range], unless it stands over the void out of melee reach. */
    private fun opponentInReach(range: Float = fightRange): Player? = Opponents.around()
        .filter {
            val distance = it.distanceTo(player)
            distance <= range && (distance <= MELEE_REACH || !it.doesNotCollideBelow())
        }
        .minByOrNull { it.distanceToSqr(player) }

    private fun holdFightBot() {
        if (Rush.useFightBot && !KillAuraFightBot.enabled) {
            KillAuraFightBot.enabled = true
            ownsFightBot = true
        }
    }

    private fun releaseFightBot() {
        if (ownsFightBot) {
            KillAuraFightBot.enabled = false
            ownsFightBot = false
        }
    }

    private fun basePoint(): Vec3 = ownBed?.let(Vec3::atCenterOf) ?: spawn ?: player.position()

    private fun count(item: Item) =
        Slots.All.sumOf { if (it.itemStack.`is`(item)) it.itemStack.count else 0 }

    private fun woolCount() = Slots.All.sumOf { if (it.itemStack.`is`(ItemTags.WOOL)) it.itemStack.count else 0 }

    private fun hasStoneSword() = Slots.All.any { it.itemStack.isSword && !it.itemStack.`is`(Items.WOODEN_SWORD) }

    private fun itemName(stack: ItemStack) = BuiltInRegistries.ITEM.getKey(stack.item).path

    private fun activitySituation(): String? =
        if (hasStarted && !ModuleFly.enabled) "state=$phase action=$action" else null

    override fun situation(): String? = activitySituation()?.let {
        "$it guard=${voidSafety.hold.tag} bed=${if (bedAlive) "alive" else "lost"} team=${team ?: "unknown"}"
    }

    override val phaseName: String
        get() = phase.name

    override val action: String
        get() = when {
            flyPaused -> "fly"
            phase == Phase.FIGHT && KillAuraFightBot.running -> "fightbot"
            phase == Phase.BREAK && ModuleFucker.hasTarget -> "break"
            navigator.isBuilding -> if (bridge.isActive) "bridge" else "build"
            walkGoal != null -> "walk"
            else -> "stand"
        }

    /** Lets go of everything the bot steers, for a death, the round end or turning off. */
    private fun stopActing() {
        releaseFightBot()
        releaseBreaking()
        voidSafety.stop()
        bridge.stop()
        walkGoal = null
        opponent = null
        shopNpc = null
    }

    private fun forgetWorld() {
        startHeardAt = null
        spawn = null
        team = null
        ownBed = null
        ownBedParts = emptyList()
        bedAlive = true
        generator = null
        targetBed = null
        unreachableUntil.clear()
        flyPaused = false
        calmTicks = 0
    }

    private fun enter(next: Phase, reason: String) {
        if (phase != next) {
            report("state $phase -> $next ($reason)")
            MapRecorder.event("phase", "from" to phase.name, "to" to next.name, "reason" to reason)
        }
        phase = next
        phaseSince = ticks
        when (next) {
            Phase.GATHER -> gatherUntil = ticks + Rush.gatherTime * TICKS_PER_SECOND
            Phase.SHOP -> {
                shopNpc = null
                shopClicks = 0
                shopClickedAt = NEVER
                buyAt = 0
                ironBeforeBuy = null
            }

            else -> Unit
        }
        resetProgress()
    }

    private fun resetProgress() {
        bestDistance = Double.MAX_VALUE
        progressAt = ticks
        strandedTicks = 0
    }

    private fun formatDistance(distance: Double) = "%.1f".format(Locale.ROOT, distance)

    private fun formatPosition(pos: Vec3) = "%.1f,%.1f,%.1f".format(Locale.ROOT, pos.x, pos.y, pos.z)

    private val OWN_TEAM_LINE = Regex("""([A-Z][a-z]+): .*\bYOU\b""")

    private const val NEVER = Int.MIN_VALUE / 2
    private const val TICKS_PER_SECOND = 20

    private const val SPAWN_TICKS = 10
    private const val TEAM_TICKS = 40
    private const val BED_TICKS = 10
    private const val GENERATOR_TICKS = 10

    /** From the spawn, which lies a few blocks from the bed. */
    private const val BED_SEARCH = 24.0
    private const val GENERATOR_HEIGHT = 6.0
    private const val GENERATOR_RADIUS = 0.6
    private const val BED_RADIUS = 2.0
    private const val BASE_HEIGHT = 8.0
    private const val OWN_BED_SEPARATION = 6.0

    private const val MIN_RESPAWN_TICKS = 20
    private const val RESPAWN_TIMEOUT_TICKS = 300

    /** Rewound's Hypixel shop: 16 wool for 4 iron, a stone sword for 10. */
    private const val WOOL_IRON = 4
    private const val SWORD_IRON = 10
    private const val MIN_RUSH_WOOL = 16
    private const val SHOP_TICKS = 600
    private const val SHOP_REACH = 3.0
    private const val SHOP_RADIUS = 2.0
    private const val HOLOGRAM_REACH = 3.0
    private const val AIM_TOLERANCE = 10f
    private const val SCREEN_TICKS = 10
    private const val MAX_SHOP_CLICKS = 5
    private const val BUY_TICKS = 6

    private const val BREAK_RADIUS = 2.5
    private const val REACH_MARGIN = 0.5
    private const val MIN_PROGRESS = 1.0
    private const val STRANDED_TICKS = 60
    private const val NO_PROGRESS_TICKS = 300
    private const val RETRY_TICKS = 1200

    private const val FIGHT_MARGIN = 2f
    private const val CALM_TICKS = 20
    private const val MELEE_REACH = 3.5f
    private const val MELEE_RADIUS = 2.5

}
