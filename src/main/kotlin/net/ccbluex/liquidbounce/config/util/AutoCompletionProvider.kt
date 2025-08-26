package net.ccbluex.liquidbounce.config.util

import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.config.types.ChooseListValue
import net.ccbluex.liquidbounce.config.types.Value
import net.ccbluex.liquidbounce.utils.kotlin.mapArray

object AutoCompletionProvider {

    val defaultCompleter = CompletionHandler { emptyArray() }

    val booleanCompleter = CompletionHandler { arrayOf("true", "false") }

    val choiceCompleter = CompletionHandler { value ->
        (value as ChoiceConfigurable<*>).choices.mapArray { it.choiceName }
    }

    val chooseCompleter = CompletionHandler { value ->
        (value as ChooseListValue<*>).choices.mapArray { it.choiceName }
    }

    fun interface CompletionHandler {

        /**
         * Gives an array with all possible completions for the [value].
         */
        fun possible(value: Value<*>): Array<String>

    }

}
