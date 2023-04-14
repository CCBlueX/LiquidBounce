package net.ccbluex.liquidbounce.utils

fun String.toLowerCamelCase() = this.replaceFirst(this.toCharArray()[0], this.toCharArray()[0].lowercaseChar())