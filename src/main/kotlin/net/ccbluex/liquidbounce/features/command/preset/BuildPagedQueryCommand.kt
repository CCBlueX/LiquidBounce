/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
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

import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.command.ParameterValidationResult
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder
import net.ccbluex.liquidbounce.utils.client.*
import net.minecraft.text.HoverEvent
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import java.util.function.IntConsumer
import kotlin.math.ceil

private val TEXT_SPACE: Text = " ".asText()

@Suppress("CognitiveComplexMethod")
private fun buildPaginationText(
    currentPage: Int,
    maxPage: Int,
    boundaryLimit: Int = 3,
    ellipsisThreshold: Int = 5,
    sendPage: IntConsumer,
): Text {
    fun MutableText.disabled() = withColor(Formatting.DARK_GRAY)
    fun MutableText.pageAction(page: Int) = this
        .onHover(HoverEvent(HoverEvent.Action.SHOW_TEXT, page.toString().asText()))
        .onClick { sendPage.accept(page) }

    val texts = mutableListOf<Text>()

    // Previous page
    texts += "\u2B9C".asText().apply {
        if (currentPage == 1) disabled() else pageAction(currentPage - 1).withColor(Formatting.GRAY)
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
        if (currentPage == maxPage) disabled() else pageAction(currentPage + 1).withColor(Formatting.GRAY)
    }

    return texts.joinToText(TEXT_SPACE)
}

/**
 * Builds a general paged query command with one optional integer parameter.
 *
 * @param pageSize the size of a single page. should be greater than 0.
 * @param header the generator function for page header before all items.
 * @param items provides all items. This function should be light-weighted.
 * @param eachRow controls how to render the item in chat HUD.
 *
 * @author MukjepScarlet
 */
fun <T> CommandBuilder.pagedQuery(
    pageSize: Int = 8,
    header: Command.() -> Text,
    items: () -> Collection<T>,
    eachRow: Command.(index: Int, T) -> Text,
): Command {
    require(pageSize > 0) { "pageSize must be greater than 0" }

    fun maxPage() = ceil(items().size.toFloat() / pageSize).toInt()

    fun Command.sendPage(currentPage: Int) {
        val msgId = "C${this.name}#PagedQuery"
        val msgMetadata = MessageMetadata(id = msgId, remove = false)
        fun send(text: Text) = chat(text, metadata = msgMetadata)

        val all = items()
        val maxPage = maxPage()
        val currentPageItems = if (all is List<T>) {
            all.subList((currentPage - 1) * pageSize, minOf(currentPage * pageSize, all.size))
        } else {
            all.drop((currentPage - 1) * pageSize).subList(0, pageSize)
        }

        mc.inGameHud.chatHud.removeMessage(msgId) // remove old

        // Header
        send(header(this))
        // Content
        currentPageItems.forEachIndexed { index, item ->
            send(eachRow(this, index, item))
        }
        // Pagination
        if (maxPage > 1) {
            send(buildPaginationText(currentPage, maxPage, sendPage = ::sendPage))
        }
    }

    return parameter(
        ParameterBuilder.begin<Int>("page")
            .verifiedBy {
                val input =
                    it.toIntOrNull() ?: return@verifiedBy ParameterValidationResult.error("'$it' is not an integer")
                val maxPage = maxPage()
                ParameterValidationResult.ofNullable(input.takeIf { i -> i in 1..maxPage }) {
                    "'$it' is not in range 1..$maxPage"
                }
            }.optional().build()
    ).handler { command, args ->
        val currentPage = args.getOrNull(0) as Int? ?: 1
        command.sendPage(currentPage)
    }
        .build()
}
