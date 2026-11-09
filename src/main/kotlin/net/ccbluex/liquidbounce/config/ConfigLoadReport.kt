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
package net.ccbluex.liquidbounce.config

/**
 * Counts what applying a config did: the values it set, the settings no value knows, and the values that failed.
 *
 * [collect] gathers one while configs are applied on the current thread; [ConfigSystem] and
 * [net.ccbluex.liquidbounce.config.types.Value.set] count into it.
 */
class ConfigLoadReport private constructor() {

    var applied = 0
        private set
    var skipped = 0
        private set
    var errors = 0
        private set

    internal fun countApplied() {
        applied++
    }

    internal fun countSkipped() {
        skipped++
    }

    internal fun countError() {
        errors++
    }

    companion object {

        private val active = ThreadLocal<ConfigLoadReport>()

        /**
         * The report of the config being applied on this thread, if any.
         */
        val current: ConfigLoadReport?
            get() = active.get()

        /**
         * Runs [block] and returns what the configs it applies did. A nested call counts into its own report.
         */
        fun collect(block: () -> Unit): ConfigLoadReport {
            val report = ConfigLoadReport()
            val outer = active.get()
            active.set(report)
            try {
                block()
            } finally {
                if (outer == null) active.remove() else active.set(outer)
            }
            return report
        }

    }

}
