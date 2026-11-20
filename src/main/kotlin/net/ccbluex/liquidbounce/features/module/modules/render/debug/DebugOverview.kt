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
package net.ccbluex.liquidbounce.features.module.modules.render.debug

import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.features.misc.DebuggedOwner
import net.ccbluex.liquidbounce.features.module.modules.render.debug.DebugOverviewLayout.PADDING
import net.ccbluex.liquidbounce.features.module.modules.render.debug.DebugOverviewLayout.ROW_HEIGHT
import net.ccbluex.liquidbounce.render.FontManager
import net.ccbluex.liquidbounce.render.drawQuad
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.math.geometry.LineSegment
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.ccbluex.liquidbounce.utils.text.withFormat
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.core.Vec3i
import net.minecraft.network.chat.Component
import net.minecraft.util.FormattedCharSequence
import net.minecraft.world.entity.Entity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.Vec3
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

/**
 * Draws the captured parameters as one table per root owner, laid out by [DebugOverviewLayout]. When the tables
 * reach below the visible area, the mouse wheel scrolls them while the chat is open.
 */
@Suppress("TooManyFunctions")
internal object DebugOverview {

    private const val LEFT = 120f
    private const val TOP = 22f
    private const val MARGIN = 4f

    // Vanilla hotbar plus the health, food and armor rows above it
    private const val BOTTOM_HUD = 50f
    private const val TEXT_SCALE = 0.8f
    private const val INDENT = 6f
    private const val NAME_VALUE_GAP = 4f
    private const val NAME_SHARE = 0.55f
    private const val SCROLL_STEP = 3 * ROW_HEIGHT
    private const val FRESH_MILLIS = 1000L
    private const val MIN_ALPHA = 0.4f
    private const val MAX_ENTRIES = 8
    private const val ELLIPSIS = "…"

    private val BACKGROUND = Color4b(0, 0, 0, 150)
    private val HEADER_BACKGROUND = Color4b(255, 255, 255, 24)
    private val HEADER = Color4b(255, 170, 0)
    private val SUBHEADER = Color4b(85, 255, 255)
    private val NAME = Color4b(170, 170, 170)
    private val VALUE = Color4b.WHITE
    private val TRUE = Color4b(85, 255, 85)
    private val FALSE = Color4b(255, 85, 85)
    private val MUTED = Color4b.GRAY

    /**
     * @param nameColumn offset of the value column from the left padding
     */
    private class CardView(
        val card: PlacedCard<DebugParameter>,
        val labels: Array<FormattedCharSequence?>,
        val nameColumn: Float,
    )

    private var views = emptyList<CardView>()
    private var viewsVersion = -1
    private var viewsGuiWidth = -1
    private var viewsGuiHeight = -1
    private var viewsCardWidth = -1

    private var contentBottom = TOP
    private var contentRight = LEFT
    private var viewBottom = TOP
    private var scrollOffset = 0f

    private var statusAbove = -1
    private var statusBelow = -1
    private var status: FormattedCharSequence = FormattedCharSequence.EMPTY

    private val font
        get() = FontManager.FONT_RENDERER

    context(ctx: GuiGraphicsExtractor)
    fun render(sections: Collection<DebugSection>, version: Int, width: Int, expireMillis: Long) {
        val guiWidth = ctx.guiWidth()
        val guiHeight = ctx.guiHeight()
        if (version != viewsVersion || guiWidth != viewsGuiWidth || guiHeight != viewsGuiHeight ||
            width != viewsCardWidth
        ) {
            buildViews(sections, OverviewArea(LEFT, TOP, guiWidth - MARGIN, guiHeight - BOTTOM_HUD), width)
            viewsVersion = version
            viewsGuiWidth = guiWidth
            viewsGuiHeight = guiHeight
            viewsCardWidth = width
        }

        viewBottom = guiHeight - BOTTOM_HUD
        val overflows = contentBottom > viewBottom
        // The last line shows what is out of view
        val cardsBottom = if (overflows) viewBottom - ROW_HEIGHT else viewBottom
        scrollOffset = scrollOffset.coerceIn(0f, max(0f, contentBottom - cardsBottom))

        val now = System.currentTimeMillis()
        for (view in views) {
            drawCard(view, cardsBottom, now, expireMillis)
        }
        if (overflows) {
            drawStatus(cardsBottom)
        }
    }

    /**
     * Scrolls by [amount] wheel steps when the cursor at [mouseX], [mouseY] is over tables that do not fit.
     *
     * @return whether the wheel moved the overview
     */
    fun scroll(amount: Float, mouseX: Float, mouseY: Float): Boolean {
        if (contentBottom <= viewBottom || mouseX !in LEFT..contentRight || mouseY !in TOP..viewBottom) {
            return false
        }

        scrollOffset -= amount * SCROLL_STEP
        return true
    }

    /**
     * Drops the cached cards, which hold on to the captured values.
     */
    fun clear() {
        views = emptyList()
        viewsVersion = -1
        contentBottom = TOP
        scrollOffset = 0f
    }

    private fun buildViews(sections: Collection<DebugSection>, area: OverviewArea, width: Int) {
        val groups = LinkedHashMap<DebuggedOwner, MutableList<OverviewSection<DebugParameter>>>()
        for (section in sections) {
            if (section.accepted && section.parameters.isNotEmpty()) {
                groups.getOrPut(section.root) { ArrayList() } +=
                    OverviewSection(section.label, section.parameters.values.toList())
            }
        }

        val cards = groups.map { (root, group) -> OverviewCard(root.debugName, group) }
        val layout = DebugOverviewLayout.layout(cards, area, width.toFloat())
        views = layout.cards.map { card ->
            val inner = card.width - 2 * PADDING
            CardView(card, Array(card.rows.size) { label(card.rows[it], inner) }, nameColumn(card, inner))
        }
        contentBottom = layout.bottom
        contentRight = layout.cards.maxOfOrNull { it.x + it.width } ?: LEFT
    }

    private fun nameColumn(card: PlacedCard<DebugParameter>, inner: Float): Float {
        var widest = 0f
        for (row in card.rows) {
            if (row is OverviewRow.Item) {
                widest = max(widest, nameWidth(row.item) + if (row.indented) INDENT else 0f)
            }
        }
        return min(widest + NAME_VALUE_GAP, inner * NAME_SHARE)
    }

    private fun nameWidth(parameter: DebugParameter): Float {
        if (parameter.nameWidth < 0f) {
            parameter.nameWidth = width(parameter.name)
        }
        return parameter.nameWidth
    }

    private fun label(row: OverviewRow<*>, inner: Float): FormattedCharSequence? = when (row) {
        is OverviewRow.Header -> fit(row.title, inner).withFormat()
        is OverviewRow.Subheader -> fit(row.label, inner).withFormat()
        is OverviewRow.Item -> null
    }

    context(ctx: GuiGraphicsExtractor)
    private fun drawCard(view: CardView, cardsBottom: Float, now: Long, expireMillis: Long) {
        val card = view.card
        val top = card.y - scrollOffset
        if (top + card.height <= TOP || top >= cardsBottom) {
            return
        }

        val right = card.x + card.width
        val visibleTop = max(top, TOP)
        ctx.drawQuad(
            x1 = card.x,
            y1 = visibleTop,
            x2 = right,
            y2 = min(top + card.height, cardsBottom),
            fillColor = BACKGROUND,
        )

        card.rows.forEachIndexed { index, row ->
            val rowY = top + PADDING + index * ROW_HEIGHT
            if (rowY < TOP || rowY + ROW_HEIGHT > cardsBottom) {
                return@forEachIndexed
            }

            when (row) {
                is OverviewRow.Item -> drawItem(view, row, rowY, alpha(now - row.item.time, expireMillis))
                is OverviewRow.Header -> {
                    ctx.drawQuad(
                        x1 = card.x,
                        y1 = visibleTop,
                        x2 = right,
                        y2 = rowY + ROW_HEIGHT,
                        fillColor = HEADER_BACKGROUND,
                    )
                    drawText(view.labels[index]!!, card.x + PADDING, rowY, HEADER)
                }
                is OverviewRow.Subheader -> drawText(view.labels[index]!!, card.x + PADDING, rowY, SUBHEADER)
            }
        }
    }

    context(ctx: GuiGraphicsExtractor)
    private fun drawItem(view: CardView, row: OverviewRow.Item<DebugParameter>, rowY: Float, alpha: Float) {
        val parameter = row.item
        val indent = if (row.indented) INDENT else 0f
        val left = view.card.x + PADDING
        refresh(
            parameter,
            nameMax = view.nameColumn - indent - NAME_VALUE_GAP,
            valueMax = view.card.width - 2 * PADDING - view.nameColumn,
        )

        drawText(parameter.drawnName, left + indent, rowY, NAME.fade(alpha))
        drawText(parameter.drawnValue, left + view.nameColumn, rowY, parameter.valueColor.fade(alpha))
    }

    context(ctx: GuiGraphicsExtractor)
    private fun drawStatus(top: Float) {
        var above = 0
        var below = 0
        for (view in views) {
            val firstRowY = view.card.y - scrollOffset + PADDING
            for (index in view.card.rows.indices) {
                val rowY = firstRowY + index * ROW_HEIGHT
                if (rowY < TOP) {
                    above++
                } else if (rowY + ROW_HEIGHT > top) {
                    below++
                }
            }
        }

        if (above != statusAbove || below != statusBelow) {
            val text = "rows above: $above, below: $below, scroll with chat open"
            status = fit(text, contentRight - LEFT - 2 * PADDING).withFormat()
            statusAbove = above
            statusBelow = below
        }

        ctx.drawQuad(x1 = LEFT, y1 = top, x2 = contentRight, y2 = top + ROW_HEIGHT, fillColor = BACKGROUND)
        drawText(status, LEFT + PADDING, top, MUTED)
    }

    context(ctx: GuiGraphicsExtractor)
    private fun drawText(text: FormattedCharSequence, textX: Float, rowY: Float, textColor: Color4b) {
        font.draw(text) {
            x = textX
            y = rowY + 0.5f
            scale = font.scaleToVanillaFont * TEXT_SCALE
            color = textColor
        }
    }

    private fun alpha(age: Long, expireMillis: Long): Float {
        if (age <= FRESH_MILLIS) {
            return 1f
        }

        val progress = (age - FRESH_MILLIS).toFloat() / max(1L, expireMillis - FRESH_MILLIS)
        return 1f - (1f - MIN_ALPHA) * progress.coerceIn(0f, 1f)
    }

    /**
     * Fits name and value of [parameter] into their columns, again only when the value or the space changed.
     */
    private fun refresh(parameter: DebugParameter, nameMax: Float, valueMax: Float) {
        if (nameMax != parameter.drawnNameMax) {
            parameter.drawnName = fit(parameter.name, nameMax, nameWidth(parameter)).withFormat()
            parameter.drawnNameMax = nameMax
        }

        val stamp = parameter.stamp
        if (stamp == parameter.formattedStamp && valueMax == parameter.drawnValueMax) {
            return
        }

        val value = parameter.value
        parameter.drawnValue = fit(formatValue(value), valueMax).withFormat()
        parameter.valueColor = valueColor(value)
        parameter.formattedStamp = stamp
        parameter.drawnValueMax = valueMax
    }

    private fun width(text: String) = font.getStringWidth(text.withFormat()) * font.scaleToVanillaFont * TEXT_SCALE

    /**
     * The longest prefix of [text] that fits into [max] with an ellipsis, or [text] itself if it fits.
     */
    private fun fit(text: String, max: Float, textWidth: Float = width(text)): String {
        if (max <= 0f) {
            return ""
        }
        if (textWidth <= max) {
            return text
        }

        var low = 0
        var high = text.length - 1
        while (low < high) {
            val mid = (low + high + 1) ushr 1
            if (width(text.substring(0, mid) + ELLIPSIS) <= max) {
                low = mid
            } else {
                high = mid - 1
            }
        }

        if (low > 0 && text[low - 1].isHighSurrogate()) {
            low--
        }
        return if (low == 0 && width(ELLIPSIS) > max) "" else text.substring(0, low) + ELLIPSIS
    }

    private fun formatValue(value: Any?): String = when (value) {
        null -> "null"
        is CharSequence -> value.toString()
        is Boolean -> value.toString()
        is Double -> number(value)
        is Float -> number(value.toDouble())
        is Number -> value.toString()
        is Tagged -> value.tag
        is Enum<*> -> value.name
        is Vec3 -> "${number(value.x)}, ${number(value.y)}, ${number(value.z)}"
        is Vec3i -> "${value.x}, ${value.y}, ${value.z}"
        is Rotation -> "${number(value.yaw.toDouble())}, ${number(value.pitch.toDouble())}"
        is LineSegment -> "${formatValue(value.start)} to ${formatValue(value.end)}"
        is DirectionalInput -> directions(value)
        is Entity -> value.name.string
        is ItemStack -> if (value.isEmpty) "empty" else "${value.count}x ${value.hoverName.string}"
        is Component -> value.string
        is Pair<*, *> -> "${formatValue(value.first)}, ${formatValue(value.second)}"
        is Map<*, *> -> entries(value.size, value.entries.asSequence().map(::formatEntry))
        is Collection<*> -> entries(value.size, value.asSequence().map(::formatValue))
        is Array<*> -> entries(value.size, value.asSequence().map(::formatValue))
        else -> value.toString()
    }

    private fun directions(input: DirectionalInput) = buildList {
        if (input.forwards) add("forwards")
        if (input.backwards) add("backwards")
        if (input.left) add("left")
        if (input.right) add("right")
    }.joinToString(" ").ifEmpty { "none" }

    private fun formatEntry(entry: Map.Entry<*, *>) = "${formatValue(entry.key)}=${formatValue(entry.value)}"

    private fun entries(size: Int, formatted: Sequence<String>) = buildString {
        append('[').append(size).append(']')
        if (size > 0) {
            formatted.take(MAX_ENTRIES).joinTo(this, separator = ", ", prefix = " ")
        }
        if (size > MAX_ENTRIES) {
            append(", ").append(ELLIPSIS)
        }
    }

    private fun number(value: Double): String {
        if (value.isNaN() || value.isInfinite()) {
            return value.toString()
        }

        val text = "%.3f".format(Locale.ROOT, value).trimEnd('0').trimEnd('.')
        return if (text == "-0") "0" else text
    }

    private fun valueColor(value: Any?) = when (value) {
        true -> TRUE
        false -> FALSE
        null -> MUTED
        else -> VALUE
    }

}
