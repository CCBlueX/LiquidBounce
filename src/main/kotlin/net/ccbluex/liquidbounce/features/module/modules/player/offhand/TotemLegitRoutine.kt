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
package net.ccbluex.liquidbounce.features.module.modules.player.offhand

import net.ccbluex.liquidbounce.additions.setPosition
import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.event.events.GameRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.minecraft.client.gui.screens.inventory.InventoryScreen
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.core.component.DataComponents
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.Slot
import org.joml.Vector2d
import org.joml.Vector2dc
import kotlin.random.Random

/**
 * With [Totem.legit], refills the offhand the way a player does: opens the inventory, moves the cursor along a curved
 * path to a totem and rests on it until the swap is done, then closes the inventory again. The Offhand constraints
 * time the swap after opening and the closing. Clicks and hotbar keys of the user are ignored while the inventory is
 * open for this.
 */
internal object TotemLegitRoutine : ToggleableValueGroup(Totem, "LegitRoutine", false) {

    private val reaction by intRange("Reaction", 100..200, 0..1000, "ms")
    private val moveTime by intRange("MoveTime", 150..300, 50..1000, "ms")

    /**
     * Gives up when the swap does not happen within this time after the cursor reached the totem, for example while
     * the inventory constraints hold it back.
     */
    private const val SWAP_TIMEOUT_MS = 2000L

    private enum class Phase { IDLE, REACTING, MOVING, HOVERING, CLOSING }

    private var phase = Phase.IDLE
    private val timer = Chronometer()
    private var wait = 0L
    private var screen: LockedInventoryScreen? = null
    private var target: Slot? = null
    private var path: CursorPath? = null

    /**
     * Whether the cursor is still on its way to the totem, so that no totem it passes over is taken.
     */
    val holdsSwap get() = phase == Phase.MOVING

    @Suppress("unused")
    private val frameHandler = handler<GameRenderEvent> {
        when (phase) {
            Phase.IDLE -> if (needsTotem()) {
                enter(Phase.REACTING, reaction.random())
            }

            Phase.REACTING -> when {
                !needsTotem() -> reset()
                timer.hasElapsed(wait) -> open()
            }

            else -> proceed()
        }
    }

    override fun onDisabled() {
        if (phase != Phase.IDLE && phase != Phase.REACTING) {
            close()
        }
        reset()
    }

    private fun needsTotem() = Totem.legit && ModuleOffhand.equipsTotem && mc.gui.screen() == null &&
        !player.offhandItem.has(DataComponents.DEATH_PROTECTION) &&
        player.inventory.contains { it.has(DataComponents.DEATH_PROTECTION) }

    private fun open() {
        val screen = LockedInventoryScreen(player)
        mc.gui.setScreen(screen)
        if (mc.gui.screen() !== screen) {
            reset()
            return
        }

        this.screen = screen
        enter(Phase.MOVING, moveTime.random())
    }

    private fun proceed() {
        val screen = screen
        // the user closed it
        if (screen == null || mc.gui.screen() !== screen) {
            reset()
            return
        }

        when (phase) {
            Phase.MOVING, Phase.HOVERING -> aim(screen)
            Phase.CLOSING -> if (timer.hasElapsed(wait)) {
                close()
                reset()
            }

            else -> Unit
        }
    }

    private fun aim(screen: LockedInventoryScreen) {
        if (player.offhandItem.has(DataComponents.DEATH_PROTECTION)) {
            enter(Phase.CLOSING, ModuleOffhand.inventoryConstraints.closeDelay.random() * 50)
            return
        }

        val timedOut = phase == Phase.HOVERING && timer.hasElapsed(SWAP_TIMEOUT_MS)
        if (timedOut || target?.item?.has(DataComponents.DEATH_PROTECTION) != true && !pickTarget(screen)) {
            close()
            reset()
            return
        }

        val path = path ?: return
        if (phase == Phase.MOVING) {
            val progress = timer.elapsed.toDouble() / wait
            moveCursor(path.at(progress))
            if (progress >= 1.0) {
                enter(Phase.HOVERING, 0)
            }
        } else {
            moveCursor(path.end)
        }
    }

    /**
     * Aims at a random totem of the main inventory, or of the hotbar when the main inventory has none.
     *
     * @return `false` if there is no totem.
     */
    private fun pickTarget(screen: LockedInventoryScreen): Boolean {
        val totems = screen.menu.slots.filter {
            it.container === player.inventory && it.containerSlot < Inventory.INVENTORY_SIZE &&
                it.item.has(DataComponents.DEATH_PROTECTION)
        }
        val main = totems.filter { it.containerSlot >= Inventory.SELECTION_SIZE }
        val slot = main.ifEmpty { totems }.randomOrNull() ?: return false

        target = slot
        path = CursorPath(Vector2d(mc.mouseHandler.xpos(), mc.mouseHandler.ypos()), screen.cursorOn(slot))
        enter(Phase.MOVING, moveTime.random())
        return true
    }

    private fun moveCursor(position: Vector2dc) = mc.mouseHandler.setPosition(position.x(), position.y())

    private fun enter(phase: Phase, wait: Int) {
        this.phase = phase
        this.wait = wait.toLong()
        timer.reset()
    }

    private fun close() {
        if (screen != null && mc.gui.screen() === screen) {
            player.closeContainer()
        }
    }

    private fun reset() {
        phase = Phase.IDLE
        screen = null
        target = null
        path = null
    }

}

/**
 * The inventory the [TotemLegitRoutine] opens. Ignores mouse clicks, the hotbar and swap keys and the drop key, so
 * the user can't move items while the routine works in it.
 */
internal class LockedInventoryScreen(player: Player) : InventoryScreen(player) {

    override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean) = true

    override fun checkHotbarKeyPressed(event: KeyEvent) = false

    override fun keyPressed(event: KeyEvent): Boolean {
        if (minecraft.options.keyDrop.matches(event)) {
            return true
        }

        return super.keyPressed(event)
    }

    /**
     * The window position of the center of [slot].
     */
    fun cursorOn(slot: Slot): Vector2d {
        val window = minecraft.window
        val scale = window.screenWidth.toDouble() / window.guiScaledWidth
        return Vector2d(leftPos + slot.x + SLOT_CENTER, topPos + slot.y + SLOT_CENTER).mul(scale)
    }

    private companion object {
        const val SLOT_CENTER = 8.0
    }

}

/**
 * A curved cursor path from [start] to [end]. The cursor speeds up and slows down like a hand reaching for a target
 * and shakes a little, less so the closer it gets.
 */
private class CursorPath(private val start: Vector2dc, val end: Vector2dc) {

    private val first: Vector2dc
    private val second: Vector2dc

    init {
        val delta = end.sub(start, Vector2d())
        // unit vector to the side of the straight line
        val side = Vector2d(delta).perpendicular().div(delta.length().coerceAtLeast(1.0))
        val bend = (delta.length() * BEND).coerceAtMost(MAX_BEND) * if (Random.nextBoolean()) 1.0 else -1.0

        first = start.fma(Random.nextDouble(FIRST_CONTROL_START, FIRST_CONTROL_END), delta, Vector2d())
            .fma(bend * Random.nextDouble(MIN_FIRST_BEND, 1.0), side)
        second = start.fma(Random.nextDouble(SECOND_CONTROL_START, SECOND_CONTROL_END), delta, Vector2d())
            .fma(bend * Random.nextDouble(0.0, MAX_SECOND_BEND), side)
    }

    fun at(progress: Double): Vector2dc {
        if (progress >= 1.0) {
            return end
        }

        // minimum jerk profile of a reaching movement
        val t = progress * progress * progress * (progress * (progress * 6 - 15) + 10)
        val u = 1 - t
        val shake = u * SHAKE
        return Vector2d(start).mul(u * u * u)
            .fma(3 * u * u * t, first)
            .fma(3 * u * t * t, second)
            .fma(t * t * t, end)
            .add(Random.nextDouble(-shake, shake), Random.nextDouble(-shake, shake))
    }

    private companion object {
        const val BEND = 0.2
        const val MAX_BEND = 40.0
        const val MIN_FIRST_BEND = 0.3
        const val MAX_SECOND_BEND = 0.4
        const val FIRST_CONTROL_START = 0.2
        const val FIRST_CONTROL_END = 0.4
        const val SECOND_CONTROL_START = 0.6
        const val SECOND_CONTROL_END = 0.8
        const val SHAKE = 1.5
    }

}
