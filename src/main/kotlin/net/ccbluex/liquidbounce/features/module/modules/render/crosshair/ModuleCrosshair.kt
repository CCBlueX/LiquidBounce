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

package net.ccbluex.liquidbounce.features.module.modules.render.crosshair

import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.event.events.OverlayRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.module.modules.render.crosshair.modes.CrosshairCircle
import net.ccbluex.liquidbounce.features.module.modules.render.crosshair.modes.CrosshairCS2
import net.ccbluex.liquidbounce.render.drawQuad
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.inventory.isInContainerScreen
import net.ccbluex.liquidbounce.utils.inventory.isInInventoryScreen
import net.minecraft.util.Util
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.BowItem
import net.minecraft.world.item.CrossbowItem
import net.minecraft.world.item.TridentItem

object ModuleCrosshair : ClientModule("Crosshair", ModuleCategories.RENDER) {
    val modes =
        choices("Mode", 0) {
            arrayOf(
                CrosshairCircle,
                CrosshairCS2,
            )
        }.apply { tagBy(this) }

    /**
     * A bar under the crosshair that fills up with the charge of the used item, item cooldowns or the attack
     * cooldown, and flashes once it is full.
     */
    object CooldownBar : ToggleableValueGroup(this, "CooldownBar", false) {
        private val sources by multiEnumChoice("Sources", CooldownSource.entries)
        private val width by float("Width", 20f, 4f..80f)
        private val height by float("Height", 2f, 0.5f..8f)
        private val offset by float("Offset", 8f, 0f..40f)
        private val color by color("Color", Color4b.WHITE)
        private val background by color("Background", Color4b(0, 0, 0, 120))

        private object ReadyFlash : ToggleableValueGroup(CooldownBar, "ReadyFlash", true) {
            val color by color("Color", Color4b.LIQUID_BOUNCE)
            val duration by int("Duration", 250, 50..1000, "ms")
        }

        init {
            tree(ReadyFlash)
        }

        private var lastSource: CooldownSource? = null
        private var readyAt = 0L

        private fun shownSource(player: Player, tickDelta: Float) = CooldownSource.entries.firstOrNull { source ->
            source in sources && source.progress(player, tickDelta).let { it != null && it < 1f }
        }

        /**
         * How far the shown charge or cooldown is, `null` when nothing charges.
         */
        fun progress(tickDelta: Float): Float? {
            val player = mc.player ?: return null
            return shownSource(player, tickDelta)?.progress(player, tickDelta)
        }

        /**
         * Strength of the ready flash right now, from `1` down to `0`.
         */
        val flash: Float
            get() = if (ReadyFlash.running) {
                (1f - (Util.getMillis() - readyAt) / ReadyFlash.duration.toFloat()).coerceIn(0f, 1f)
            } else {
                0f
            }

        @Suppress("unused")
        private val renderHandler = handler<OverlayRenderEvent> { event ->
            if (isInInventoryScreen || isInContainerScreen) {
                return@handler
            }

            // A draw let go early stops charging instead of filling up, so it never counts as ready
            if (lastSource?.progress(player, event.tickDelta) == 1f) {
                readyAt = Util.getMillis()
            }
            lastSource = shownSource(player, event.tickDelta)

            val progress = lastSource?.progress(player, event.tickDelta)
            val flash = flash
            if (progress == null && flash <= 0f) {
                return@handler
            }

            val x = event.context.guiWidth() / 2f - width / 2f
            val y = event.context.guiHeight() / 2f + offset
            val fillColor = color.interpolateTo(ReadyFlash.color, flash)

            event.context.drawQuad(x, y, x + width, y + height, fillColor = background)
            event.context.drawQuad(x, y, x + width * (progress ?: 1f), y + height, fillColor = fillColor)
        }

        override fun onDisabled() {
            lastSource = null
            readyAt = 0L
        }
    }

    init {
        tree(CooldownBar)
    }

    /**
     * Sources of the cooldown bar, the first one that is not ready yet is shown.
     */
    private enum class CooldownSource(override val tag: String) : Tagged {
        CHARGE("Charge") {
            override fun progress(player: Player, tickDelta: Float): Float? {
                if (!player.isUsingItem) {
                    return null
                }

                val stack = player.useItem
                val ticks = player.getTicksUsingItem(tickDelta)
                val charge = when (stack.item) {
                    is BowItem -> BowItem.getPowerForTime(ticks.toInt())
                    is CrossbowItem -> ticks / CrossbowItem.getChargeDuration(stack, player)
                    is TridentItem -> ticks / TridentItem.THROW_THRESHOLD_TIME
                    else -> return null
                }
                return charge.coerceIn(0f, 1f)
            }
        },
        ITEM("Item") {
            override fun progress(player: Player, tickDelta: Float) = 1f - InteractionHand.entries.maxOf { hand ->
                player.cooldowns.getCooldownPercent(player.getItemInHand(hand), tickDelta)
            }
        },
        ATTACK("Attack") {
            override fun progress(player: Player, tickDelta: Float) = player.getAttackStrengthScale(tickDelta)
        };

        /**
         * How far this source is, `1` once ready and `null` when it does not apply.
         */
        abstract fun progress(player: Player, tickDelta: Float): Float?
    }
}
