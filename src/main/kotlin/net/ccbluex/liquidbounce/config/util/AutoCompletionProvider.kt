package net.ccbluex.liquidbounce.config.util

import net.ccbluex.fastutil.mapToArray
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.config.types.ChooseListValue
import net.ccbluex.liquidbounce.config.types.Value

object AutoCompletionProvider {

    val defaultCompleter = CompletionHandler { emptyArray() }

    val booleanCompleter = CompletionHandler { arrayOf("true", "false") }

    val choiceCompleter = CompletionHandler { value ->
        (value as ChoiceConfigurable<*>).choices.mapToArray { it.choiceName }
    }

    val chooseCompleter = CompletionHandler { value ->
        (value as ChooseListValue<*>).choices.mapToArray { it.choiceName }
    }

    fun interface CompletionHandler {

        /**
         * Gives an array with all possible completions for the [value].
         */
        fun possible(value: Value<*>): Array<String>

    }

}
