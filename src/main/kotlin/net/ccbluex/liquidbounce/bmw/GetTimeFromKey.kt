package net.ccbluex.liquidbounce.bmw

import java.time.LocalDateTime

data class GetTimeFromKeyReturn(
    val startTime: LocalDateTime,
    val days: Int,
    val successful: Boolean
)

var nowKey = ""

fun getTimeFromKey(key: String) : GetTimeFromKeyReturn {
    var position = 1
    var jump = -1
    var year = 0
    var month = 0
    var day = 0
    var hour = 0
    var minute = 0
    var second = 0
    var days = 0
    key.forEach {
        if (jump > 0) {
            jump--
        } else if (jump == 0) {
            val number = when (it) {
                in 'a'..'z' -> it.code - 'a'.code
                in 'A'..'Z' -> it.code - 'A'.code
                else -> -1
            }
            if (number == -1) {
                return GetTimeFromKeyReturn(LocalDateTime.now(), 0, false)
            }
            when (position) {
                in 1..4 -> year = year * 10 + number
                in 5..6 -> month = month * 10 + number
                in 7..8 -> day = day * 10 + number
                in 9..10 -> hour = hour * 10 + number
                in 11..12 -> minute = minute * 10 + number
                in 13..14 -> second = second * 10 + number
                in 15..17 -> days = days * 10 + number
                else -> {
                    return GetTimeFromKeyReturn(LocalDateTime.now(), 0, false)
                }
            }
            jump = -1
            position++
        } else if (jump == -1) {
            val base = when (it) {
                in 'a'..'z' -> 'a'
                in 'A'..'Z' -> 'A'
                else -> '?'
            }
            if (base == '?') {
                return GetTimeFromKeyReturn(LocalDateTime.now(), 0, false)
            }
            jump = (it - base) % 3 + 1
        }
    }
    return GetTimeFromKeyReturn(LocalDateTime.of(year, month, day, hour, minute, second), days, true)
}
