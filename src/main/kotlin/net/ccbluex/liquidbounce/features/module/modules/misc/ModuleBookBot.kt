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

package net.ccbluex.liquidbounce.features.module.modules.misc

import it.unimi.dsi.fastutil.ints.IntArrayList
import net.ccbluex.liquidbounce.config.types.group.Mode
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.event.events.ScheduleInventoryActionEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.ccbluex.liquidbounce.utils.inventory.HotbarItemSlot
import net.ccbluex.liquidbounce.utils.inventory.InventoryAction
import net.ccbluex.liquidbounce.utils.inventory.PlayerInventoryConstraints
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.network.protocol.game.ServerboundEditBookPacket
import net.minecraft.server.network.Filterable
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.WrittenBookContent
import okio.buffer
import okio.source
import java.util.Optional
import java.util.PrimitiveIterator
import java.util.Random
import java.util.stream.IntStream

/**
 * Maximum number of lines that can fit on a single page in the Minecraft book.
 *
 * This constant is used to eliminate "magic numbers" in the code and improve readability and maintainability.
 * It defines the limit for the number of lines displayed on a single page, ensuring consistent formatting.
 *
 * **Note:** This value is fixed and should not be changed to maintain the intended functionality.
 */
private const val MAX_LINES_PER_PAGE: Int = 14

/**
 * Maximum width of a single line of text in the Minecraft book, measured in float units.
 *
 * This constant is used to eliminate "magic numbers" in the code and improve readability and maintainability.
 * It defines the maximum length of a single line, ensuring that the text does not overflow or break formatting.
 *
 * **Note:** This value is fixed and should not be changed to maintain the intended functionality.
 */
private const val MAX_LINE_WIDTH: Float = 114f

/**
 * ModuleBookBot
 *
 * This module simplifies the process of filling and creating books using various principles,
 * enabling efficient generation and potential automation for mass book creation or "spam."
 *
 * @author sqlerrorthing
 * @since 12/28/2024
 **/
object ModuleBookBot : ClientModule("BookBot", ModuleCategories.EXPLOIT, disableOnQuit = true) {
    private val inventoryConstraints = tree(PlayerInventoryConstraints())

    internal val generationMode = choices(
        "Mode",
        GenerationMode.Random,
        arrayOf(GenerationMode.Random, GenerationMode.File)
    ).apply {
        tagBy(this)
    }

    private object Sign : ToggleableValueGroup(ModuleBookBot, "Sign", true) {
        val bookName by text("Name", "Generated book #%count%")
    }

    init {
        treeAll(Sign)
    }

    private val delay by float("Delay", .5f, 0f..20f, suffix = "s")

    private val chronometer = Chronometer()

    private var bookCount = 0

    override fun onEnabled() {
        bookCount = 0
        chronometer.reset()
    }

    private fun isCandidate(itemStack: ItemStack): Boolean {
        return itemStack.item == Items.WRITABLE_BOOK &&
            itemStack.get(DataComponents.WRITABLE_BOOK_CONTENT)?.pages()?.isEmpty() == true
    }

    private val randomBook get() = Slots.All.findSlot(::isCandidate)

    @Suppress("unused")
    private val scheduleInventoryAction = handler<ScheduleInventoryActionEvent> { event ->
        val book = randomBook ?: run {
            enabled = false
            return@handler
        }

        if (!isCandidate(player.mainHandItem)) {
            event.schedule(
                inventoryConstraints, InventoryAction.Click.performSwap(
                from = book,
                to = HotbarItemSlot(player.inventory.selectedSlot),
            ))
        }

        if (chronometer.hasElapsed((delay * 1000L).toLong())) {
            chronometer.reset()
            writeBook()
        }
    }

    /**
     * Generates a book with content based on the active choice of the generation mode.
     * The book content is generated character by character, and the text is split into pages,
     * ensuring that each page contains lines that fit within the given width constraints.
     *
     * This method processes each character from the generator, managing line breaks and page formatting,
     * and stores the generated text in the `pages` and `filteredPages` lists. Once a page is full, it is
     * added to the collection, and the process continues until the specified number of pages is reached.
     *
     * The method performs the following steps:
     * - Generates characters using the active choice from the generation mode.
     * - Breaks lines based on a width limit and ensures that a line fits within this constraint.
     * - Adds new lines when a line exceeds the width limit or encounters a line break character (`\r` or `\n`).
     * - If a page is full, it is added to the `pages` and `filteredPages` lists, and the process continues.
     * - Stops once the desired number of pages is generated.
     *
     * The generated pages are used to create a book with the specified name, which is then saved.
     *
     *
     * @see PrimitiveIterator.OfInt
     * @see GenerationMode.generate
     */
    private fun writeBook() {
        if (!isCandidate(player.mainHandItem)) {
            return
        }

        val bookBuilder = BookBuilder()
        val generator = generationMode.activeMode.generate()
            .filter { it.toChar() != '\r' }
            .iterator()

        bookBuilder.buildBookContent(generator) {
            mc.font.splitter.widthProvider.getWidth(it, Style.EMPTY)
        }
        bookBuilder.writeBook()

        bookCount++
    }

    private fun StringBuilder.appendLineBreak(lineIndex: Int) {
        append('\n')
        if (lineIndex == MAX_LINES_PER_PAGE) {
            append(' ')
        }
    }

    private class BookBuilder {
        private val title: String = Sign.bookName.replace("%count%", bookCount.toString())
        private val pageAmount: Int = generationMode.activeMode.pages

        private val pages = ArrayList<String>(pageAmount)
        private val filteredPages = ArrayList<Filterable<Component>>(pageAmount)

        /**
         * @source <a href="https://github.com/MeteorDevelopment/meteor-client/blob/2025789457e5b4c0671f04f0d3c7e0d91a31765c/src/main/java/meteordevelopment/meteorclient/systems/modules/misc/BookBot.java#L252-L326">code section</a>
         * @contributor sqlerrorthing (<a href="https://github.com/CCBlueX/LiquidBounce/pull/5076">pull request</a>)
         * @author arlomcwalter (on Meteor Client)
         */
        @Suppress("detekt:CognitiveComplexMethod")
        inline fun buildBookContent(
            charGenerator: PrimitiveIterator.OfInt,
            charWidthProvider: (charCode: Int) -> Float
        ) {
            var pageIndex = 0
            var lineIndex = 0
            var lineWidth = 0.0f
            val page = StringBuilder()

            while (charGenerator.hasNext()) {
                val char = charGenerator.nextInt().toChar()

                if (lineWidth == 0f && char == ' ') {
                    continue
                } else if (char == '\r' || char == '\n') {
                    page.append('\n')
                    lineWidth = 0.0f
                    lineIndex++
                } else {
                    val charWidth = charWidthProvider(char.code)

                    if (lineWidth + charWidth > MAX_LINE_WIDTH) {
                        lineIndex++
                        lineWidth = charWidth
                        page.appendLineBreak(lineIndex)
                    } else {
                        lineWidth += charWidth
                        page.appendCodePoint(char.code)
                    }
                }

                if (lineIndex == MAX_LINES_PER_PAGE) {
                    this.addPage(page.toString())
                    page.setLength(0)
                    pageIndex++
                    lineIndex = 0

                    if (pageIndex >= pageAmount) {
                        break
                    }

                    if (char != '\r' && char != '\n') {
                        page.append(char)
                    }
                }
            }

            if (page.isNotEmpty() && pageIndex < pageAmount) {
                this.addPage(page.toString())
            }
        }

        fun addPage(page: String) {
            filteredPages.add(Filterable.passThrough(Component.literal(page)))
            pages.add(page)
        }

        fun writeBook() {
            player.mainHandItem.set(
                DataComponents.WRITTEN_BOOK_CONTENT,
                WrittenBookContent(
                    Filterable.passThrough(title),
                    player.gameProfile.name,
                    0,
                    filteredPages,
                    true
                )
            )

            player.connection.send(
                ServerboundEditBookPacket(
                    player.inventory.selectedSlot,
                    pages,
                    if (Sign.enabled) Optional.of(title) else Optional.empty()
                )
            )
        }
    }
}

internal sealed class GenerationMode(
    name: String,
) : Mode(name) {
    override val parent: ModeValueGroup<*> get() = ModuleBookBot.generationMode

    internal val random = Random()

    val pages by int("Pages", 50, 0..100)

    abstract fun generate(): IntStream

    object Random : GenerationMode("Random") {
        private val asciiOnly by boolean("AsciiOnly", false)
        private val allowSpace by boolean("AllowSpace", true)

        @Suppress("MaxLineLength")
        /**
         * @source <a href="https://github.com/MeteorDevelopment/meteor-client/blob/2025789457e5b4c0671f04f0d3c7e0d91a31765c/src/main/java/meteordevelopment/meteorclient/systems/modules/misc/BookBot.java#L201-L209">code section</a>
         * @contributor sqlerrorthing (<a href="https://github.com/CCBlueX/LiquidBounce/pull/5076">pull request</a>)
         * @author arlomcwalter (on Meteor Client)
         */
        override fun generate(): IntStream {
            val origin = if (asciiOnly) 0x21 else 0x0800
            val bound = if (asciiOnly) 0x7E else 0x10FFFF

            return random
                .ints(origin, bound)
                .filter { allowSpace || !Character.isWhitespace(it) }
        }
    }

    object File : GenerationMode("File") {
        private const val MAX_CODE_POINTS: Long = 64 * 1024 * 1024
        private val cyclic by boolean("Cyclic", true)
        private val source = file("Source")

        /**
         * @author sqlerrorthing, MukjepScarlet
         */
        override fun generate(): IntStream {
            val file = source.absoluteFile.takeIf {
                it.exists() && it.isFile && it.canRead() && it.length() != 0L
            } ?: return IntStream.empty()

            // UTF-8 averaged 3 bytes -> 1 code point
            val codePoints = IntArrayList(minOf(MAX_CODE_POINTS, file.length()).toInt() / 3)
            file.source().buffer().use {
                while (!it.exhausted() && codePoints.size < MAX_CODE_POINTS) {
                    codePoints.add(it.readUtf8CodePoint())
                }
            }

            return if (cyclic && codePoints.isNotEmpty()) {
                var index = 0
                IntStream.generate {
                    val value = codePoints.getInt(index)
                    index = (index + 1) % codePoints.size
                    value
                }
            } else {
                codePoints.intStream()
            }
        }
    }
}
