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
package net.ccbluex.liquidbounce.config.gson.adapter

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.Temporal
import java.time.temporal.TemporalQuery

private fun <T : Temporal> temporalAsStringAdapter(formatter: DateTimeFormatter, temporalQuery: TemporalQuery<T>) =
    SimpleStringTypeAdapter({ formatter.parse(it, temporalQuery) }, formatter::format)

@JvmField
val LocalDateAdapter = temporalAsStringAdapter(DateTimeFormatter.ISO_LOCAL_DATE, LocalDate::from)

@JvmField
val LocalDateTimeAdapter = temporalAsStringAdapter(DateTimeFormatter.ISO_LOCAL_DATE_TIME, LocalDateTime::from)

@JvmField
val OffsetDateTimeAdapter = temporalAsStringAdapter(DateTimeFormatter.ISO_OFFSET_DATE_TIME, OffsetDateTime::from)

private val UNDERLINED_LOCAL_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")

fun LocalDateTime.toUnderlinedString(): String = UNDERLINED_LOCAL_DATE_TIME_FORMATTER.format(this)
