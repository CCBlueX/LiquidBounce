package net.ccbluex.liquidbounce.utils.extensions

import net.ccbluex.liquidbounce.LiquidBounce

infix fun String.equalTo(second: Any?) = this.equalTo(second, "\u00A77")

fun String.equalTo(second: Any?, colorCodes: String) = "\u00A77$this\u00A78=$colorCodes$second\u00A7r"

fun Iterable<String>.serialize() = this.joinToString(separator = "\u00A78, ", postfix = "\u00A7r")

fun Iterable<Pair<String, Any?>>.serializePair(colorCodes: String = "\u00A77") = this.joinToString(separator = "\u00A78, ", postfix = "\u00A7r") { it.first.equalTo(it.second, colorCodes) }

fun String.withSquareBrackets(textColorCodes: String = "\u00A79\u00A7l", bracketColorCodes: String = "\u00A78") = "$bracketColorCodes[$textColorCodes$this$bracketColorCodes]\u00A7r"

fun String.withParentheses(textColorCodes: String = "\u00A79", parentheseColorCodes: String = "\u00A78") = "$parentheseColorCodes($textColorCodes$this$parentheseColorCodes)\u00A7r"

fun String.withQuotes(textColorCodes: String = "\u00A79", quoteColorCodes: String = "\u00A78") = "$quoteColorCodes'$textColorCodes$this$quoteColorCodes'\u00A7r"

fun String.withDoubleQuotes(textColorCodes: String = "\u00A79", quoteColorCodes: String = "\u00A78") = "$quoteColorCodes\"$textColorCodes$this$quoteColorCodes\"\u00A7r"

fun String.withClientPrefix() = withPrefix(LiquidBounce.CLIENT_NAME, "\u00A79\u00A7l", "\u00A78", "\u00A73")

fun String.withPrefix(prefix: String, prefixColorCodes: String = "\u00A79\u00A7l", bracketColorCodes: String = "\u00A78", textColorCodes: String = "\u00A73") = "${prefix.withSquareBrackets(prefixColorCodes, bracketColorCodes)} $textColorCodes$this"

