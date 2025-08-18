package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.ScheduleInventoryActionEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.ccbluex.liquidbounce.utils.inventory.ClickInventoryAction
import net.ccbluex.liquidbounce.utils.inventory.HotbarItemSlot
import net.ccbluex.liquidbounce.utils.inventory.PlayerInventoryConstraints
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.WrittenBookContentComponent
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.network.packet.c2s.play.BookUpdateC2SPacket
import net.minecraft.text.RawFilteredPair
import net.minecraft.text.Style
import net.minecraft.text.Text
import java.util.*

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
object ModuleBookBot : ClientModule("BookBot", Category.EXPLOIT, disableOnQuit = true) {
    private val inventoryConstraints = tree(PlayerInventoryConstraints())

    private val generationMode = choices("Mode", GenerationMode.Random, arrayOf(GenerationMode.Random)).apply {
        tagBy(this)
    }

    private object Sign : ToggleableConfigurable(ModuleBookBot, "Sign", true) {
        val bookName by text("Name", "Generated book #%count%")
    }

    init {
        treeAll(Sign)
    }

    private val delay by float("Delay", .5f, 0f..20f, suffix = "s")

    private val chronometer = Chronometer()

    private var bookCount = 0

    internal var random: Random = Random()
        private set

    override fun onEnabled() {
        bookCount = 0
        random = Random()
        chronometer.reset()
    }

    private fun isCandidate(itemStack: ItemStack): Boolean {
        return itemStack.item == Items.WRITABLE_BOOK &&
            itemStack.get(DataComponentTypes.WRITABLE_BOOK_CONTENT)?.pages?.isEmpty() == true
    }

    private val randomBook get() = Slots.All.findSlot(::isCandidate)

    @Suppress("unused")
    private val scheduleInventoryAction = handler<ScheduleInventoryActionEvent> { event ->
        val book = randomBook ?: run {
            enabled = false
            return@handler
        }

        if (!isCandidate(player.mainHandStack)) {
            event.schedule(inventoryConstraints, ClickInventoryAction.performSwap(
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
        if (!isCandidate(player.mainHandStack)) {
            return
        }

        val bookBuilder = BookBuilder()
        bookBuilder.buildBookContent(generationMode.activeChoice.generate()) {
            mc.textRenderer.textHandler.widthRetriever.getWidth(it, Style.EMPTY)
        }
        bookBuilder.writeBook()

        bookCount++
    }

    private sealed class GenerationMode(
        name: String,
    ) : Choice(name) {
        override val parent: ChoiceConfigurable<*> = ModuleBookBot.generationMode

        val pages by int("Pages", 50, 0..100)

        abstract fun generate(): PrimitiveIterator.OfInt


        object Random : GenerationMode("Random") {
            private val asciiOnly by boolean("AsciiOnly", false)

            private val allowSpace by boolean("AllowSpace", true)

            /**
             * @source <a href="https://github.com/MeteorDevelopment/meteor-client/blob/2025789457e5b4c0671f04f0d3c7e0d91a31765c/src/main/java/meteordevelopment/meteorclient/systems/modules/misc/BookBot.java#L201-L209">code section</a>
             * @contributor sqlerrorthing (<a href="https://github.com/CCBlueX/LiquidBounce/pull/5076">pull request</a>)
             * @author arlomcwalter (on Meteor Client)
             */
            override fun generate(): PrimitiveIterator.OfInt {
                val origin = if (asciiOnly) 0x21 else 0x0800
                val bound = if (asciiOnly) 0x7E else 0x10FFFF

                return random
                    .ints(origin, bound)
                    .filter { allowSpace || !Character.isWhitespace(it) }
                    .iterator()
            }
        }
    }

    private fun StringBuilder.appendLineBreak(lineIndex: Int) {
        append('\n')
        if (lineIndex == MAX_LINES_PER_PAGE) {
            append(' ')
        }
    }

    private class BookBuilder {
        private val title: String = Sign.bookName.replace("%count%", bookCount.toString())
        private val pageAmount: Int = generationMode.activeChoice.pages

        private val pages = ArrayList<String>(pageAmount)
        private val filteredPages = ArrayList<RawFilteredPair<Text>>(pageAmount)

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
            filteredPages.add(RawFilteredPair.of(Text.literal(page)))
            pages.add(page)
        }

        fun writeBook() {
            player.mainHandStack.set(
                DataComponentTypes.WRITTEN_BOOK_CONTENT,
                WrittenBookContentComponent(
                    RawFilteredPair.of(title),
                    player.gameProfile.name,
                    0,
                    filteredPages,
                    true
                )
            )

            player.networkHandler.sendPacket(
                BookUpdateC2SPacket(
                    player.inventory.selectedSlot,
                    pages,
                    if (Sign.enabled) Optional.of(title) else Optional.empty()
                )
            )
        }
    }
}
