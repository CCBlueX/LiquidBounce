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

import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.event.events.ChatReceiveEvent
import net.ccbluex.liquidbounce.event.events.DeathEvent
import net.ccbluex.liquidbounce.event.events.PlayerDeathEvent
import net.ccbluex.liquidbounce.event.events.PlayerKillEvent
import net.ccbluex.liquidbounce.event.events.PlayerTickEvent
import net.ccbluex.liquidbounce.event.events.TitleEvent
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleAutoWeapon
import net.ccbluex.liquidbounce.features.module.modules.combat.autoarmor.ModuleAutoArmor
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleSmartEat
import net.ccbluex.liquidbounce.features.module.modules.player.autobuff.ModuleAutoBuff
import net.ccbluex.liquidbounce.features.module.modules.player.cheststealer.ModuleChestStealer
import net.ccbluex.liquidbounce.features.module.modules.player.cheststealer.features.FeatureChestAura
import net.ccbluex.liquidbounce.features.module.modules.player.gamebot.games.GameBotBedWars
import net.ccbluex.liquidbounce.features.module.modules.player.gamebot.games.GameBotBlitz
import net.ccbluex.liquidbounce.features.module.modules.player.gamebot.games.GameBotSkyWars
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.ModuleInventoryCleaner
import net.ccbluex.liquidbounce.utils.client.SilentHotbar
import net.ccbluex.liquidbounce.utils.inventory.HotbarItemSlot
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.FIRST_PRIORITY
import net.ccbluex.liquidbounce.utils.network.sendChatOrCommand
import net.minecraft.network.chat.ClickEvent
import java.util.Locale

/**
 * Plays minigame rounds: the [Game] mode plays, this module tells it when a round is over and queues again.
 *
 * Everything worth following is logged with the prefix `[GameBot]`.
 */
@Suppress("TooManyFunctions")
object ModuleGameBot : ClientModule("GameBot", ModuleCategories.PLAYER) {

    internal val games = choices(
        "Game", GameBotSkyWars, arrayOf(GameBotSkyWars, GameBotBlitz, GameBotBedWars)
    ).apply(::tagBy)

    /**
     * Turns AutoWeapon, AutoBuff and SmartEat on when a round starts, with ChestStealer and its aura, AutoArmor and
     * InventoryCleaner in games that [Game.loots], KillAura only while an opponent is within [engageRange], and
     * Speed as [Pace] allows.
     */
    private val manageModules by boolean("ManageModules", true)

    val managesModules: Boolean
        get() = manageModules

    /** Records each round for review with `scripts/bot/map/`, see [MapRecorder]. */
    private val recordMaps by boolean("RecordMaps", true)

    /** KillAura goes off again once no opponent was within this and [AURA_OFF_MARGIN] for [AURA_OFF_TICKS]. */
    private val engageRange by float("EngageRange", 10f, 3f..32f, "blocks")

    private val startKeywords by textList(
        "StartKeywords",
        mutableListOf("Cages opened! FIGHT!", "The games begin in 1 second!"),
    )
    private val winKeywords by textList("WinKeywords", mutableListOf("VICTORY", "You won"))
    private val lossKeywords by textList(
        "LossKeywords",
        mutableListOf(
            "YOU DIED", "GAME OVER", "DEFEAT",
            "You died, use your compass to spectate players!", "You died!",
        ),
    )

    /** Lines `<prefix> - [RANK] <name> ...`, a win when the name is the player's own. */
    private val winnerPrefixes by textList("WinnerPrefixes", mutableListOf("Winner", "1st"))

    /** Text between victim and killer in a kill message. */
    private val killPhrases by textList(
        "KillPhrases",
        mutableListOf(
            " was killed by ", " died in close combat to ", " was thrown into the void by ",
            " fought to the edge with ", " was knocked into the void by ",
        ),
    )

    /** Chat of spectators is never read. */
    private val spectatorPrefix by text("SpectatorPrefix", "[SPECTATOR]")

    private object Requeue : ToggleableValueGroup(this, "Requeue", true) {
        val mode by enumChoice("Mode", RequeueMode.COMMAND)

        /** Runs the command of a clickable loss message such as "Click here!" instead of the game's queue command. */
        val useClickCommand by boolean("UseClickCommand", true)
        val itemNames by textList("ItemNames", mutableListOf("Play Again"))
        val delay by int("Delay", 3, 0..30, "s")
        val retry by int("Retry", 20, 0..120, "s")

        /** Chat showing we wait in a pregame lobby, where queueing again would leave it. */
        val waitingKeywords by textList(
            "WaitingKeywords",
            mutableListOf("You have chosen", "The games begin in", "Cages open in", "The game starts in"),
        )
    }

    private enum class RequeueMode(override val tag: String) : Tagged {
        COMMAND("Command"),
        ITEM("Item"),
    }

    init {
        tree(Requeue)
        tree(Clips)
    }

    private var ticks = 0
    private var kills = 0

    /** When to queue next, while no round is played. */
    private var requeueAt: Int? = null
    private var requeueSlot: HotbarItemSlot? = null
    private var clickCommand: String? = null

    /** Set while in a pregame lobby; cleared by the next world change. */
    private var waiting = false

    /** Whether KillAura is on for an opponent near. */
    private var auraOn = false
    private var auraCalmTicks = 0

    private val game: Game
        get() = games.activeMode

    fun report(message: String) {
        logger.info("[GameBot] $message")
        MapRecorder.note(message)
    }

    /** Called by the game once the round really started. */
    fun roundStarted() {
        kills = 0
        requeueAt = null
        waiting = false
        clickCommand = null
        Opponents.start()
        Setbacks.startRound()
        Strafing.stop()
        Outnumbered.stop()
        Eating.stop()
        if (recordMaps) {
            MapRecorder.begin(game)
        }
        report("round start game=${game.name}")
        if (!manageModules) {
            return
        }

        ModuleAutoWeapon.enabled = true
        ModuleAutoBuff.enabled = true
        ModuleSmartEat.enabled = true
        if (game.loots) {
            ModuleChestStealer.enabled = true
            FeatureChestAura.enabled = true
            ModuleAutoArmor.enabled = true
            ModuleInventoryCleaner.enabled = true
        }
    }

    override fun onEnabled() {
        ticks = 0
        requeueAt = null
        requeueSlot = null
        waiting = false
        report("enabled game=${game.name}")
        Pace.stop()
    }

    override fun onDisabled() {
        Opponents.stop("disabled")
        Strafing.stop()
        Outnumbered.stop()
        Eating.stop()
        Pace.stop()
        if (auraOn) {
            auraOff("disabled")
            ModuleKillAura.enabled = false
        }
        report("disabled")
        MapRecorder.abort("disabled")
    }

    @Suppress("unused")
    private val worldChangeHandler = handler<WorldChangeEvent> {
        requeueSlot = null
        waiting = false
        // The new world may be a lobby that needs another try.
        requeueAt = requeueAt?.let { ticks + Requeue.retry * TICKS_PER_SECOND }
    }

    @Suppress("unused")
    private val tickHandler = handler<PlayerTickEvent> {
        ticks++
        updateAura()

        requeueSlot?.let { slot ->
            requeueSlot = null
            interaction.useItem(player, slot.useHand)
        }

        val at = requeueAt ?: return@handler
        if (game.isPlaying) {
            requeueAt = null
        } else if (ticks >= at && !waiting) {
            requeue()
        }
    }

    // Before KillDeathTracker, so the killer named in chat is known when it reports the death
    @Suppress("unused")
    private val chatHandler = handler<ChatReceiveEvent>(priority = FIRST_PRIORITY) { event ->
        if (event.type != ChatReceiveEvent.ChatType.GAME_MESSAGE) {
            return@handler
        }
        val message = event.message.trim()
        if (spectatorPrefix.isNotEmpty() && message.startsWith(spectatorPrefix)) {
            return@handler
        }

        if (!waiting && isWaitingMessage(message)) {
            waiting = true
            report("waiting in lobby: $message")
        }

        when {
            startKeywords.any(message::contains) -> game.startAnnounced()
            lossKeywords.any(message::contains) -> {
                clickCommand = event.textData.toFlatList().firstNotNullOfOrNull {
                    (it.style.clickEvent as? ClickEvent.RunCommand)?.command
                }
                lose(message)
            }

            isOwnWin(message) -> finish(won = true, reason = message)
            else -> readKill(message)
        }
    }

    private fun isOwnWin(message: String): Boolean {
        val prefix = winnerPrefixes.firstOrNull { message.startsWith("$it - ") } ?: return false
        val name = message.removePrefix("$prefix - ").substringAfter("] ").substringBefore(' ')
        return name == player.scoreboardName
    }

    /** Own join message `* <name> has joined (N/M)!` or one of [Requeue.waitingKeywords]. */
    private fun isWaitingMessage(message: String) =
        message.contains("${player.scoreboardName} has joined (") && message.endsWith(")!") ||
            Requeue.waitingKeywords.any(message::contains)

    private fun readKill(message: String) {
        val phrase = killPhrases.firstOrNull(message::contains) ?: return
        val victim = message.substringBefore(phrase).trim().substringAfterLast(' ')
        val killer = message.substringAfter(phrase).trim().substringBefore(' ').trimEnd('!', '.')
        Opponents.noteKill(victim, killer)
        if (killer == player.scoreboardName) {
            kills++
            report("kill $victim kills=$kills")
            MapRecorder.event("kill", "victim" to victim, "kills" to kills)
        }
    }

    @Suppress("unused")
    private val titleHandler = handler<TitleEvent.Title> { event -> readTitle(event.text?.string) }

    @Suppress("unused")
    private val subtitleHandler = handler<TitleEvent.Subtitle> { event -> readTitle(event.text?.string) }

    private fun readTitle(text: String?) {
        text ?: return
        when {
            winKeywords.any(text::contains) -> finish(won = true, reason = text)
            lossKeywords.any(text::contains) -> lose(text)
            startKeywords.any(text::contains) -> game.startAnnounced()
        }
    }

    @Suppress("unused")
    private val killHandler = handler<PlayerKillEvent> { event ->
        if (Opponents.isOurKill(event.victim)) {
            report("kill event ${event.victim}")
        }
    }

    @Suppress("unused")
    private val playerDeathHandler = handler<PlayerDeathEvent> { event ->
        Opponents.noteDeath(event.killer)
        lose("death")
    }

    @Suppress("unused")
    private val deathHandler = handler<DeathEvent> {
        Opponents.noteDeath(null)
        lose("death screen")
    }

    /** A death, which ends the round unless the game respawns the player. */
    private fun lose(reason: String) {
        if (game.respawns) {
            game.died(reason)
        } else {
            finish(won = false, reason = reason)
        }
    }

    /** Ends the round being played, as told by the server or seen by the game. */
    fun finish(won: Boolean, reason: String) {
        if (!game.isPlaying) {
            return
        }

        report("${if (won) "win" else "loss"} kills=$kills reason=${reason.trim()}")
        game.end()
        Opponents.stop("round")
        MapRecorder.end(won, reason, kills)
        if (Requeue.enabled) {
            requeueAt = ticks + Requeue.delay * TICKS_PER_SECOND
        }
    }

    private fun requeue() {
        val retry = Requeue.retry
        requeueAt = if (retry > 0) ticks + retry * TICKS_PER_SECOND else null

        when (Requeue.mode) {
            RequeueMode.COMMAND -> {
                val command = clickCommand?.takeIf { Requeue.useClickCommand }
                    ?.let { if (it.startsWith("/")) it else "/$it" } ?: game.queueCommand
                report("requeue command=$command")
                network.sendChatOrCommand(command)
            }

            RequeueMode.ITEM -> {
                val slot = Slots.Hotbar.findSlot { stack ->
                    val name = stack.hoverName.string
                    Requeue.itemNames.any { name.contains(it, ignoreCase = true) }
                }
                if (slot == null) {
                    report("requeue item missing")
                    return
                }

                report("requeue item=${slot.itemStack.hoverName.string}")
                SilentHotbar.selectSlotSilently(this, slot, REQUEUE_SLOT_TICKS)
                // The slot change reaches the server first.
                requeueSlot = slot
            }
        }
    }

    /**
     * KillAura on while the game fights or an opponent is within [engageRange] of a started round, off once none
     * stays near.
     */
    private fun updateAura() {
        if (!manageModules) {
            return
        }

        val wasOn = auraOn
        val nearest = if (game.hasStarted) Opponents.around().minByOrNull { it.distanceToSqr(player) } else null
        val distance = nearest?.distanceTo(player) ?: Float.MAX_VALUE
        when {
            !game.hasStarted -> auraOff("round")
            game.isFighting || distance <= engageRange -> {
                auraCalmTicks = 0
                if (!auraOn) {
                    auraOn = true
                    val dist = if (nearest == null) "none" else "%.1f".format(Locale.ROOT, distance)
                    report("aura on target=${nearest?.scoreboardName ?: "none"} dist=$dist fight=${game.isFighting}")
                }
            }

            distance <= engageRange + AURA_OFF_MARGIN -> auraCalmTicks = 0
            ++auraCalmTicks >= AURA_OFF_TICKS -> auraOff("no opponent near")
        }
        if (ModuleKillAura.enabled != auraOn) {
            // Something else turned KillAura off while the bot wanted it on.
            if (wasOn && auraOn) {
                report("aura reenable state=${game.situation() ?: "none"}")
            }
            ModuleKillAura.enabled = auraOn
        }
    }

    private fun auraOff(reason: String) {
        if (auraOn) {
            auraOn = false
            report("aura off reason=$reason")
        }
        auraCalmTicks = 0
    }

    private const val TICKS_PER_SECOND = 20
    private const val REQUEUE_SLOT_TICKS = 20
    private const val AURA_OFF_MARGIN = 3f
    private const val AURA_OFF_TICKS = 20

}
