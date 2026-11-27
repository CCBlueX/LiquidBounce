/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2026 CCBlueX
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

import net.ccbluex.liquidbounce.config.types.group.ValueGroup
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.render.FontManager
import net.ccbluex.liquidbounce.render.drawQuad
import net.ccbluex.liquidbounce.render.drawRoundedRect
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.withPush
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.chat.Component

/** Where the strip sits on screen. */
internal enum class HudAnchor(override val tag: String) : Tagged {
    TOP_LEFT("TopLeft"),
    TOP_RIGHT("TopRight"),
    BOTTOM_LEFT("BottomLeft"),
    BOTTOM_RIGHT("BottomRight"),

    /** Placed by [HudValueGroup.x] and [HudValueGroup.y], as a percentage of the screen. */
    CUSTOM("Custom"),
}

internal class HudValueGroup : ValueGroup("Hud") {
    val enabled by boolean("Enabled", true)
    val anchor by enumChoice("Anchor", HudAnchor.TOP_LEFT)
    val scale by float("Scale", 1f, 0.5f..2f, "x")

    /** Centre of the strip as a percentage of the screen, used when [anchor] is [HudAnchor.CUSTOM]. */
    val x by float("X", 50f, 0f..100f, "%")
    val y by float("Y", 8f, 0f..100f, "%")

    /** What the strip shows; the box grows and shrinks with the switches that are on. */
    val phase by boolean("ShowPhase", true)
    val target by boolean("ShowTarget", true)
    val lead by boolean("ShowLead", true)
    val reqAttack by boolean("ShowReqAttack", true, aliases = listOf("ShowFlightEma", "ShowLatency"))
    val hitRate by boolean("ShowHitRate", true)
    val health by boolean("ShowHealth", true)
    val damage by boolean("ShowDamage", true)
    val history by boolean("ShowHistory", true)
    val route by boolean("ShowRoute", false)
    val search by boolean("ShowSearch", false)
    val drift by boolean("ShowDrift", false)
    val spray by boolean("ShowSpray", true)
    val pin by boolean("ShowPinGap", true)
}

/**
 * The strip of the module: what is happening right now, and how the recent engagements went.
 *
 * Drawn from the module's own event handler, like
 * [net.ccbluex.liquidbounce.features.module.modules.render.debug.DebugOverview], so nothing outside the
 * module is involved. Every part is optional, and the box is measured from the parts that are on.
 *
 * Once the module goes idle the last state, with the reason it left, stays readable for [HOLD_MS];
 * otherwise a lost target or a failed route would flash by in a single frame.
 */
internal object TpAuraHud {

    private const val PAD = 6f
    private const val MARGIN = 6f
    private const val LINE = 12f
    private const val ROW_GAP = 2f
    private const val GAP = 8f
    private const val MIN_WIDTH = 96f
    private const val DOT = 5f
    private const val DOT_GAP = 7f

    /** More than this between the server and the client position means the two are drifting apart. */
    private const val PIN_WARN = 1.0

    /** How long the last state stays readable after the module went idle or failed. */
    private const val HOLD_MS = 2_000L

    private val BACKGROUND = Color4b(18, 20, 16, 170)
    private val OUTLINE = Color4b(96, 102, 86, 120)
    private val TITLE = Color4b(237, 235, 227)
    private val MUTED = Color4b(154, 156, 146)
    private val DIM = Color4b(112, 114, 104)
    private val VALUE = Color4b(214, 214, 202)
    private val ACCENT = Color4b(175, 169, 236)
    private val HIT = Color4b(93, 202, 165)
    private val MISS = Color4b(240, 149, 149)
    private val WARN = Color4b(239, 159, 39)

    /** Last engagement outcomes, newest last. */
    private val results = ArrayDeque<Boolean>()
    private var confirmed = 0
    private var missed = 0

    var phase: String = "IDLE"
    var target: String? = null
    var distance: Double = 0.0
    var lead: Double = 0.0
    var reqAttack: Double = 0.0
    var pinGap: Double = 0.0
    var targetHealth: Double = 0.0
    var targetMaxHealth: Double = 0.0
    var lastDamage: Double = 0.0
    var waypoints: Int = 0
    var routeLength: Double = 0.0
    var searchTicks: Int = 0
    var drift: Double = 0.0
    var spraySpots: Int = 0
    var sprayMax: Int = 0

    /** Reason of the last leave or failure, shown while the strip is held. */
    var reason: String? = null

    private var heldTill = 0L
    private var held: Frame? = null

    /** Everything the strip shows, frozen so it outlives the state it came from. */
    private data class Frame(
        val phase: String,
        val target: String?,
        val distance: Double,
        val lead: Double,
        val reqAttack: Double,
        val pinGap: Double,
        val health: Double,
        val maxHealth: Double,
        val damage: Double,
        val waypoints: Int,
        val routeLength: Double,
        val searchTicks: Int,
        val drift: Double,
        val spraySpots: Int,
        val sprayMax: Int,
        val reason: String?,
        val results: List<Boolean>,
        val hitRate: Int,
        val samples: Int,
        val held: Boolean,
    )

    /** Counts one swing whose damage the server confirmed or refused. */
    fun onResult(confirmedHit: Boolean) {
        if (confirmedHit) {
            confirmed++
        } else {
            missed++
        }

        results.addLast(confirmedHit)
        while (results.size > 12) {
            results.removeFirst()
        }
    }

    fun reset() {
        held = null
        heldTill = 0L
        reason = null
        results.clear()
        confirmed = 0
        missed = 0
        phase = "IDLE"
        target = null
        pinGap = 0.0
        targetHealth = 0.0
        targetMaxHealth = 0.0
        lastDamage = 0.0
        waypoints = 0
        routeLength = 0.0
        searchTicks = 0
        drift = 0.0
        spraySpots = 0
        sprayMax = 0
    }

    /** Called once per tick: keeps the last state readable for [HOLD_MS] once the module has none. */
    fun tick(active: Boolean) {
        if (active) {
            held = frame(held = false)
            heldTill = 0L
            return
        }

        if (held == null) {
            return
        }

        if (heldTill == 0L) {
            held = frame(held = true)
            heldTill = System.currentTimeMillis() + HOLD_MS
            return
        }

        if (System.currentTimeMillis() > heldTill) {
            held = null
            heldTill = 0L
            reason = null
        }
    }

    private fun frame(held: Boolean) = Frame(
        phase = phase,
        target = target,
        distance = distance,
        lead = lead,
        reqAttack = reqAttack,
        pinGap = pinGap,
        health = targetHealth,
        maxHealth = targetMaxHealth,
        damage = lastDamage,
        waypoints = waypoints,
        routeLength = routeLength,
        searchTicks = searchTicks,
        drift = drift,
        spraySpots = spraySpots,
        sprayMax = sprayMax,
        reason = reason,
        results = results.toList(),
        hitRate = percent(),
        samples = confirmed + missed,
        held = held,
    )

    fun render(ctx: GuiGraphicsExtractor, config: HudValueGroup) {
        if (!config.enabled) {
            return
        }

        val shown = held ?: idleFrame()
        val rows = rows(config, shown)
        if (rows.isEmpty()) {
            return
        }

        val scale = config.scale
        val contentWidth = rows.maxOf { rowWidth(it) }
        val width = maxOf(MIN_WIDTH, contentWidth + PAD * 2)
        val height = PAD * 2 + rows.size * LINE + (rows.size - 1) * ROW_GAP

        val screenWidth = mc.window.guiScaledWidth.toFloat()
        val screenHeight = mc.window.guiScaledHeight.toFloat()
        val scaledWidth = width * scale
        val scaledHeight = height * scale

        val desiredLeft = when (config.anchor) {
            HudAnchor.TOP_LEFT, HudAnchor.BOTTOM_LEFT -> MARGIN
            HudAnchor.TOP_RIGHT, HudAnchor.BOTTOM_RIGHT -> screenWidth - MARGIN - scaledWidth
            HudAnchor.CUSTOM -> screenWidth * (config.x / 100f) - scaledWidth / 2f
        }
        val desiredTop = when (config.anchor) {
            HudAnchor.TOP_LEFT, HudAnchor.TOP_RIGHT -> MARGIN
            HudAnchor.BOTTOM_LEFT, HudAnchor.BOTTOM_RIGHT -> screenHeight - MARGIN - scaledHeight
            HudAnchor.CUSTOM -> screenHeight * (config.y / 100f) - scaledHeight / 2f
        }

        // Whatever the resolution or GUI scale, the strip stays inside the screen.
        val left = desiredLeft.coerceIn(MARGIN, maxOf(MARGIN, screenWidth - MARGIN - scaledWidth))
        val top = desiredTop.coerceIn(MARGIN, maxOf(MARGIN, screenHeight - MARGIN - scaledHeight))

        ctx.pose().withPush {
            translate(left, top)
            scale(scale, scale)

            ctx.drawRoundedRect(0f, 0f, width, height, 4f, BACKGROUND, null, OUTLINE, 0.5f)

            rows.forEachIndexed { index, row ->
                var x = PAD
                val y = PAD + index * (LINE + ROW_GAP)
                for (part in row) {
                    part.draw(ctx, x, y)
                    x += part.width + GAP
                }
            }
        }
    }

    /** One piece of a line: how wide it is and how to paint it. */
    private class Part(val width: Float, val draw: (GuiGraphicsExtractor, Float, Float) -> Unit)

    private fun rowWidth(row: List<Part>): Float =
        row.sumOf { it.width.toDouble() }.toFloat() + GAP * (row.size - 1)

    private fun text(value: String, color: Color4b): Part {
        val font = FontManager.FONT_RENDERER
        val component = Component.literal(value)
        return Part(font.getStringWidth(component)) { ctx, x, y -> font.draw(ctx, component, x, y, color) }
    }

    private fun idleFrame() = Frame(
        phase = "idle",
        target = null,
        distance = 0.0,
        lead = 0.0,
        reqAttack = 0.0,
        pinGap = 0.0,
        health = 0.0,
        maxHealth = 0.0,
        damage = 0.0,
        waypoints = 0,
        routeLength = 0.0,
        searchTicks = 0,
        drift = 0.0,
        spraySpots = 0,
        sprayMax = 0,
        reason = null,
        results = emptyList(),
        hitRate = 0,
        samples = 0,
        held = true,
    )

    private fun rows(config: HudValueGroup, shown: Frame): List<List<Part>> {
        val rows = ArrayList<List<Part>>()
        val dim = shown.held

        val header = ArrayList<Part>()
        header += text("TpAura+", if (dim) DIM else TITLE)
        if (config.phase) {
            header += text(shown.phase.lowercase(), if (dim) DIM else ACCENT)
        }
        val name = shown.target
        if (config.target && name != null) {
            header += text("$name  ${fmt(shown.distance)} m", MUTED)
        }
        if (config.health && shown.maxHealth > 0.0) {
            val low = shown.health / shown.maxHealth < 0.3
            header += text("hp ${fmt(shown.health)}", if (low) MISS else VALUE)
        }
        addRow(rows, header)

        val stats = ArrayList<Part>()
        // Zero means "no lead in use" (prediction off): keeping the row would show a number that
        // nothing in the flight actually uses.
        if (config.lead && shown.lead > 0.0) {
            stats += text("lead ${fmt(shown.lead)} t", HIT)
        }
        if (config.reqAttack) {
            // "request → attack": the whole approach, from asking for the route to standing on the spot.
            // ASCII arrow on purpose — the default font has no U+2192 and would draw a missing glyph.
            stats += text("req->attack ${fmt(shown.reqAttack)} t", MUTED)
        }
        if (config.hitRate && shown.samples > 0) {
            stats += text("hit ${shown.hitRate}%  (${shown.samples})", if (shown.hitRate >= 60) HIT else MISS)
        }
        if (config.damage && shown.damage > 0.0) {
            stats += text("dmg ${fmt(shown.damage)}", ACCENT)
        }
        if (config.history && shown.results.isNotEmpty()) {
            stats += dots(shown.results)
        }
        addRow(rows, stats)

        val routeRow = ArrayList<Part>()
        if (config.route && shown.waypoints > 0) {
            routeRow += text("${shown.waypoints} wp · ${fmt(shown.routeLength)} m", VALUE)
        }
        if (config.search) {
            routeRow += text("search ${shown.searchTicks} t", MUTED)
        }
        if (config.drift) {
            routeRow += text("drift ${fmt(shown.drift)} m", if (shown.drift > 6.0) WARN else MUTED)
        }
        if (config.spray && shown.spraySpots > 0) {
            routeRow += text("spray ${shown.spraySpots}/${shown.sprayMax}", ACCENT)
        }
        addRow(rows, routeRow)

        val reason = shown.reason
        if (reason != null) {
            addRow(rows, listOf(text(reason, WARN)))
        } else if (config.pin) {
            val warning = shown.pinGap > PIN_WARN
            addRow(rows, listOf(text("pin ${fmt(shown.pinGap)} m", if (warning) WARN else MUTED)))
        }

        return rows
    }

    private fun addRow(rows: MutableList<List<Part>>, row: List<Part>) {
        if (row.isNotEmpty()) {
            rows += row
        }
    }

    /** The last engagements as squares, newest last. */
    private fun dots(states: List<Boolean>): Part {
        val count = states.size
        return Part(count * DOT_GAP - (DOT_GAP - DOT)) { ctx, x, y ->
            var dotX = x
            for (result in states) {
                ctx.drawQuad(dotX, y, dotX + DOT, y + DOT, if (result) HIT else MISS)
                dotX += DOT_GAP
            }
        }
    }

    private fun percent(): Int {
        val total = confirmed + missed
        return if (total == 0) 0 else (confirmed * 100.0 / total).toInt()
    }

    private fun fmt(value: Double): String = "%.1f".format(value)
}
