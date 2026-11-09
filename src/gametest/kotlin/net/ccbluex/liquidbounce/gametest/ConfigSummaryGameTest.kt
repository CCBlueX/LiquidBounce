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
package net.ccbluex.liquidbounce.gametest

import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.NotificationEvent
import net.ccbluex.liquidbounce.features.marketplace.autoconfig.ConfigTracker
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList

/**
 * `.localconfig load` sums up what the config did in one notification, coloured by the outcome. Settings no value
 * knows count as skipped; a value that refuses its new setting counts as failed. The rest of the config still
 * applies either way.
 */
class ConfigSummaryGameTest : FabricClientGameTest {

    private class SummaryModule : ClientModule("GameTestSummary", ModuleCategories.MISC) {
        val number by int("Number", 1, 0..100)
        val picky by int("Picky", 1, 0..100).onChange {
            check(it != REFUSED) { "Picky refuses $it" }
            it
        }
    }

    private val notifications = CopyOnWriteArrayList<NotificationEvent>()

    override fun runTest(context: ClientGameTestContext) {
        context.waitForClient()
        val folder = context.fromClient { ConfigSystem.userConfigsFolder }
        val module = SummaryModule()
        context.onClient { ModuleManager.addModule(module) }
        val hook = object : EventListener {}.on(NotificationEvent::class.java) { notifications += it }

        try {
            load(context, folder, """{"name": "Number", "value": 5}, {"name": "Picky", "value": 6}""")
            checkSummary("2 applied, 0 skipped, 0 failed", NotificationEvent.Severity.SUCCESS)
            check(module.number == 5 && module.picky == 6) {
                "A clean config was not applied, got ${module.number} and ${module.picky}"
            }

            load(context, folder, """{"name": "Number", "value": 7}, {"name": "Picky", "value": $REFUSED}""")
            checkSummary("1 applied, 0 skipped, 1 failed", NotificationEvent.Severity.ERROR)
            check(module.number == 7 && module.picky == 6) {
                "A refused value stopped the config or was applied, got ${module.number} and ${module.picky}"
            }

            load(
                context,
                folder,
                """{"name": "Number", "value": 8}, {"name": "Gone", "value": 1}""",
                """, {"name": "GameTestMissing", "value": []}"""
            )
            checkSummary("1 applied, 2 skipped, 0 failed", NotificationEvent.Severity.INFO)
            check(module.number == 8) { "Unknown settings stopped the config, got ${module.number}" }
        } finally {
            hook.close()
            context.onClient {
                ModuleManager.removeModule(module)
                // The loads above made the tracker show the local config
                ConfigSystem.deserializeValueGroup(
                    ConfigTracker,
                    """{"name": "MarketplaceConfig", "value": [{"name": "LocalName", "value": ""}]}"""
                )
            }
            folder.resolve("$NAME.json").delete()
        }
    }

    /**
     * Loads a config setting [values] of the test module, with [extraModules] after it in the module list.
     */
    private fun load(context: ClientGameTestContext, folder: File, values: String, extraModules: String = "") {
        folder.resolve("$NAME.json").writeText(
            """{"name": "modules", "value": [{"name": "GameTestSummary", "value": [$values]}$extraModules]}"""
        )
        notifications.clear()
        context.command("localconfig load $NAME")
    }

    private fun checkSummary(message: String, severity: NotificationEvent.Severity) {
        val summaries = notifications.filter { it.title == "Loaded $NAME" }
        check(summaries.size == 1) { "Expected one summary, got ${summaries.map { it.message }}" }
        val summary = summaries.single()
        check(summary.message == message && summary.severity == severity) {
            "Expected '$message' as $severity, got '${summary.message}' as ${summary.severity}"
        }
    }

    private companion object {
        const val NAME = "gametest_summary"
        const val REFUSED = 13
    }

}
