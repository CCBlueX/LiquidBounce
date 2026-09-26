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
package net.ccbluex.liquidbounce.features.command.preset

import com.mojang.brigadier.arguments.IntegerArgumentType
import net.ccbluex.liquidbounce.features.command.brigadier.CmdLiteralScope
import net.ccbluex.liquidbounce.features.command.brigadier.get
import net.ccbluex.liquidbounce.utils.client.MessageMetadata
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.text.asPlainText
import net.ccbluex.liquidbounce.utils.text.asText
import net.ccbluex.liquidbounce.utils.client.bold
import net.ccbluex.liquidbounce.utils.text.joinToText
import net.ccbluex.liquidbounce.utils.client.onClickRun
import net.ccbluex.liquidbounce.utils.client.onHover
import net.ccbluex.liquidbounce.utils.client.removeMessage
import net.ccbluex.liquidbounce.utils.client.withColor
import net.ccbluex.liquidbounce.utils.text.PlainText
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.MutableComponent
import java.util.function.IntConsumer
import kotlin.math.ceil

@Suppress("CognitiveComplexMethod")
private fun buildPaginationText(
    currentPage: Int,
    maxPage: Int,
    boundaryLimit: Int = 3,
    ellipsisThreshold: Int = 5,
    sendPage: IntConsumer,
): Component {
    fun MutableComponent.disabled() = withColor(ChatFormatting.DARK_GRAY)
    fun MutableComponent.pageAction(page: Int) = this
        .onHover(HoverEvent.ShowText(page.toString().asPlainText()))
        .onClickRun { sendPage.accept(page) }

    val texts = mutableListOf<Component>()

    // Previous page
    texts += "\u2B9C".asText().apply {
        if (currentPage == 1) disabled() else pageAction(currentPage - 1).withColor(ChatFormatting.GRAY)
    }

    // Numeral page text (clickable)
    fun numeral(i: Int) = i.toString().asText().apply {
        if (i == currentPage) disabled().bold(true) else pageAction(i)
    }

    // Ellipsis page text (clickable)
    fun ellipsis(left: Int, right: Int) = "…".asText().pageAction((left + right) / 2)

    var i: Int
    when {
        maxPage <= ellipsisThreshold -> {
            i = 1
            while (i <= maxPage) {
                texts += numeral(i++)
            }
        }

        currentPage <= boundaryLimit -> {
            i = 1
            while (i <= boundaryLimit) {
                texts += numeral(i++)
            }
            texts += ellipsis(i, maxPage)
            texts += numeral(maxPage)
        }

        currentPage >= maxPage - boundaryLimit + 1 -> {
            i = maxPage - boundaryLimit + 1
            texts += numeral(1)
            texts += ellipsis(2, i)
            while (i <= maxPage) {
                texts += numeral(i++)
            }
        }

        else -> {
            i = currentPage - 1
            texts += numeral(1)
            texts += ellipsis(2, i)
            while (i <= currentPage + 1) {
                texts += numeral(i++)
            }
            texts += ellipsis(i, maxPage)
            texts += numeral(maxPage)
        }
    }

    // Next page
    texts += "\u2B9E".asText().apply {
        if (currentPage == maxPage) disabled() else pageAction(currentPage + 1).withColor(ChatFormatting.GRAY)
    }

    return texts.joinToText(PlainText.SPACE)
}

/**
 * Adds the optional page argument and executor to this literal (e.g. `.help [page]`).
 * For a listing that lives under `list`, use [pagedList].
 */
fun <T> CmdLiteralScope.pagedQuery(
    pageSize: Int = 8,
    header: () -> Component,
    items: () -> Collection<T>,
    eachRow: (index: Int, T) -> Component,
) {
    require(pageSize > 0) { "pageSize must be greater than 0" }

    fun pageCount(itemCount: Int) = ceil(itemCount.toFloat() / pageSize).toInt().coerceAtLeast(1)

    fun sendPage(requestedPage: Int) {
        val msgId = "C$path#PagedQuery"
        val msgMetadata = MessageMetadata(id = msgId, remove = false)
        fun send(text: Component) = chat(text, metadata = msgMetadata)

        val all = items()
        val maxPage = pageCount(all.size)
        val currentPage = requestedPage.coerceAtMost(maxPage)
        val currentPageItems = if (all is List<T>) {
            all.subList((currentPage - 1) * pageSize, minOf(currentPage * pageSize, all.size))
        } else {
            val drop = all.drop((currentPage - 1) * pageSize)
            drop.subList(0, minOf(pageSize, drop.size))
        }

        mc.gui.hud.chat.removeMessage(msgId) // remove old

        // Header
        send(header())
        // Content
        currentPageItems.forEachIndexed { index, item ->
            send(eachRow(index, item))
        }
        // Pagination
        if (maxPage > 1) {
            send(buildPaginationText(currentPage, maxPage, sendPage = ::sendPage))
        }
    }

    optional("page", IntegerArgumentType.integer(1), default = 1) { page ->
        exec { ctx ->
            sendPage(ctx.get(page))
            1
        }
    }
}

/** Adds a `list [page]` subcommand with the same paging UI as [pagedQuery]. */
fun <T> CmdLiteralScope.pagedList(
    pageSize: Int = 8,
    header: () -> Component,
    items: () -> Collection<T>,
    eachRow: (index: Int, T) -> Component,
) {
    literal("list") {
        pagedQuery(pageSize, header, items, eachRow)
    }
}
