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
package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.config.types.nesting.Configurable
import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.minecraft.client.network.PlayerListEntry
import net.minecraft.text.Text

/**
 * ModuleBetterTab
 *
 * @author sqlerrorthing
 * @since 12/28/2024
 **/
@Suppress("MagicNumber")
object ModuleBetterTab : ClientModule("BetterTab", Category.RENDER) {

    val sorting by enumChoice("Sorting", Sorting.VANILLA)

    private val visibility by multiEnumChoice("Visibility",
        Visibility.HEADER,
        Visibility.FOOTER
    )

    @JvmStatic
    fun isVisible(visibility: Visibility) = visibility in this.visibility

    object Limits : Configurable("Limits") {
        val tabSize by int("TabSize", 80, 1..1000)
        val height by int("ColumnHeight", 20, 1..100)
    }

    object Highlight : ToggleableConfigurable(ModuleBetterTab, "Highlight", true) {
        open class HighlightColored(
            name: String,
            color: Color4b
        ) : ToggleableConfigurable(this, name, true) {
            val color by color("Color", color)
        }

        class Others(color: Color4b) : HighlightColored("Others", color) {
            val filter = tree(PlayerFilter())
        }

        val self = tree(HighlightColored("Self", Color4b(50, 193, 50, 80)))
        val friends = tree(HighlightColored("Friends", Color4b(16, 89, 203, 80)))
        val others = tree(Others(Color4b(35, 35, 35, 80)))
    }

    object AccurateLatency : ToggleableConfigurable(ModuleBetterTab, "AccurateLatency", true) {
        val suffix by boolean("AppendMSSuffix", true)
    }

    object PlayerHider : ToggleableConfigurable(ModuleBetterTab, "PlayerHider", false) {
        val filter = tree(PlayerFilter())
    }

    init {
        treeAll(
            Limits,
            Highlight,
            AccurateLatency,
            PlayerHider
        )
    }

}

class PlayerFilter: Configurable("Filter") {
    private var filters = setOf<Regex>()

    private val filterBy by multiEnumChoice("FilterBy", Filter.entries)

    @Suppress("unused")
    private val names by textList("Names", mutableListOf()).onChanged { newValue ->
        filters = newValue.mapTo(HashSet(newValue.size, 1.0F)) {
            val regexPattern = it
                .replace("*", ".*")
                .replace("?", ".")

            Regex("^$regexPattern\$")
        }
    }

    fun isInFilter(entry: PlayerListEntry) = filters.any { regex ->
        filterBy.any { filter -> filter.matches(entry, regex) }
    }

    @Suppress("unused")
    private enum class Filter(
        override val choiceName: String,
        val matches: PlayerListEntry.(Regex) -> Boolean
    ) : NamedChoice {
        DISPLAY_NAME("DisplayName", { regex ->
            this.displayName?.string?.let { regex.matches(it) } ?: false
        }),

        PLAYER_NAME("PlayerName", { regex ->
            regex.matches(this.profile.name)
        })
    }
}

@Suppress("unused")
enum class Sorting(
    override val choiceName: String,
    val comparator: Comparator<PlayerListEntry>?
) : NamedChoice {
    VANILLA("Vanilla", null),
    PING("Ping", Comparator.comparingInt { it.latency }),
    LENGTH("NameLength", Comparator.comparingInt { it.profile.name.length }),
    SCORE_LENGTH("DisplayNameLength", Comparator.comparingInt { (it.displayName ?: Text.empty()).string.length }),
    ALPHABETICAL("Alphabetical", Comparator.comparing { it.profile.name }),
    REVERSE_ALPHABETICAL("ReverseAlphabetical", Comparator.comparing({ it.profile.name }, Comparator.reverseOrder())),
    NONE("None", { _, _ -> 0 })
}

@Suppress("unused")
enum class Visibility(
    override val choiceName: String
) : NamedChoice {
    HEADER("Header"),
    FOOTER("Footer"),
    NAME_ONLY("NameOnly")
}

