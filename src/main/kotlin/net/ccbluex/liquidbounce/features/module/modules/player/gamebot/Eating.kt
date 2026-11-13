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

import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.KeybindIsPressedEvent
import net.ccbluex.liquidbounce.event.events.PlayerTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.MinecraftShortcuts
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura
import net.ccbluex.liquidbounce.features.module.modules.player.autobuff.ModuleAutoBuff
import net.ccbluex.liquidbounce.features.module.modules.player.gamebot.ModuleGameBot.report
import net.ccbluex.liquidbounce.utils.client.SilentHotbar
import net.ccbluex.liquidbounce.utils.combat.CombatManager
import net.ccbluex.liquidbounce.utils.inventory.HotbarItemSlot
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.ccbluex.liquidbounce.utils.item.isConsumable
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import java.util.Locale

/**
 * Keeps eating from being cut off: while the bot eats or drinks, by AutoBuff, SmartEat or the game, KillAura pauses,
 * unless an opponent in reach is about to kill the bot anyway. Retreating or low on health, the bot eats a golden
 * apple on its own.
 *
 * Logs an `eat` line for every item started.
 */
internal object Eating : EventListener, MinecraftShortcuts {

    override fun parent() = ModuleGameBot

    private var ticks = 0
    private var wasEating = false

    /** The golden apple being eaten on the bot's own, and since when. */
    private var gapple: HotbarItemSlot? = null
    private var gappleSince = 0
    private var gappleEndedAt = Int.MIN_VALUE / 2

    /** Whether the bot is eating or drinking, or about to, so nothing should switch the slot or use another item. */
    val isEating: Boolean
        get() = player.isUsingItem && player.useItem.isConsumable || gapple != null ||
            SilentHotbar.isSlotModifiedBy(ModuleAutoBuff)

    @Suppress("unused")
    private val tickHandler = handler<PlayerTickEvent> {
        ticks++
        if (!ModuleGameBot.games.activeMode.hasStarted) {
            stop()
            return@handler
        }

        updateGapple()
        noteEating()
        if (isEating && !isAboutToDie()) {
            CombatManager.pauseCombatForAtLeast(COMBAT_PAUSE_TICKS)
        }
    }

    @Suppress("unused")
    private val keyHandler = handler<KeybindIsPressedEvent> { event ->
        if (gapple != null && event.keyBinding == mc.options.keyUse) {
            event.isPressed = true
        }
    }

    private fun noteEating() {
        val eating = player.isUsingItem && player.useItem.isConsumable
        if (eating && !wasEating) {
            report(
                "eat ${itemName(player.useItem)} hp=${"%.1f".format(Locale.ROOT, player.health)} " +
                    "hunger=${player.foodData.foodLevel}"
            )
        }
        wasEating = eating
    }

    /** Eats a golden apple while retreating or low on health, unless its effects are still on. */
    private fun updateGapple() {
        gapple?.let { slot ->
            val eaten = ticks - gappleSince > START_TICKS && !player.isUsingItem
            if (eaten || ticks - gappleSince > GAPPLE_TICKS || !isGapple(slot.itemStack)) {
                stopGapple()
            }
            return
        }

        val wanted = Outnumbered.plan is Outnumbered.Retreat || player.health < LOW_HP
        val effectsOn = player.hasEffect(MobEffects.REGENERATION) || player.absorptionAmount > 0f
        if (!wanted || effectsOn || player.isUsingItem || ticks - gappleEndedAt < RETRY_TICKS ||
            mc.gui.screen() is AbstractContainerScreen<*>
        ) {
            return
        }

        val slot = Slots.Hotbar.findSlot { isGapple(it) } ?: return
        if (SilentHotbar.selectSlotSilently(this, slot, GAPPLE_TICKS)) {
            gapple = slot
            gappleSince = ticks
        }
    }

    private fun stopGapple() {
        if (gapple != null) {
            gapple = null
            gappleEndedAt = ticks
            SilentHotbar.resetSlot(this)
        }
    }

    /** An opponent in reach and the bot with so little health that the meal would not finish in time. */
    private fun isAboutToDie(): Boolean {
        val target = ModuleKillAura.targetTracker.target ?: return false
        return player.health <= DANGER_HP && target.distanceTo(player) <= DANGER_RANGE
    }

    private fun isGapple(stack: ItemStack) = stack.`is`(Items.GOLDEN_APPLE) || stack.`is`(Items.ENCHANTED_GOLDEN_APPLE)

    private fun itemName(stack: ItemStack) = BuiltInRegistries.ITEM.getKey(stack.item).path

    /** For when the round ends or GameBot stops. */
    fun stop() {
        stopGapple()
        wasEating = false
    }

    private const val COMBAT_PAUSE_TICKS = 2
    private const val LOW_HP = 10f
    private const val DANGER_HP = 4f
    private const val DANGER_RANGE = 3.5f

    /** A golden apple takes 32 ticks to eat; the use has to show within [START_TICKS] after the slot switch. */
    private const val GAPPLE_TICKS = 50
    private const val START_TICKS = 5
    private const val RETRY_TICKS = 40

}
