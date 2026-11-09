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
 * A module whose enable or disable throws while `.localconfig load` applies a config goes back to its previous
 * state, the failure is counted in the summary, and the rest of the config still applies.
 */
class ConfigRollbackGameTest : FabricClientGameTest {

    /**
     * [active] is what [onEnabled] sets up and [onDisabled] tears down, before either throws when told to.
     */
    private class FlakyModule : ClientModule("GameTestFlaky", ModuleCategories.MISC) {
        val number by int("Number", 1, 0..100)

        var failEnable = false
        var failDisable = false
        var active = false

        override fun onEnabled() {
            active = true
            check(!failEnable) { "GameTestFlaky failed to enable" }
        }

        override fun onDisabled() {
            active = false
            check(!failDisable) { "GameTestFlaky failed to disable" }
        }
    }

    private val notifications = CopyOnWriteArrayList<NotificationEvent>()

    override fun runTest(context: ClientGameTestContext) {
        context.waitForClient()
        val folder = context.fromClient { ConfigSystem.userConfigsFolder }
        val module = FlakyModule()
        val hook = object : EventListener {}.on(NotificationEvent::class.java) { notifications += it }

        try {
            context.worldBuilder().create().use {
                context.onClient { ModuleManager.addModule(module) }

                module.failEnable = true
                load(context, folder, enabled = true, number = 5)
                context.onClient {
                    check(!module.enabled && !module.active && !module.running) {
                        "A module that failed to enable is left ${state(module)}"
                    }
                    check(module.number == 5) { "A failed enable stopped the config, got ${module.number}" }
                }
                checkFailureCounted()

                module.failEnable = false
                context.onClient {
                    module.enabled = true
                    check(module.active) { "The module did not enable outside a config" }
                }

                module.failDisable = true
                load(context, folder, enabled = false, number = 6)
                context.onClient {
                    check(module.enabled && module.active && module.running) {
                        "A module that failed to disable is left ${state(module)}"
                    }
                    check(module.number == 6) { "A failed disable stopped the config, got ${module.number}" }
                }
                checkFailureCounted()
            }
        } finally {
            hook.close()
            module.failEnable = false
            module.failDisable = false
            context.onClient {
                if (ModuleManager.any { it === module }) {
                    ModuleManager.removeModule(module)
                }
                // The loads above made the tracker show the local config
                ConfigSystem.deserializeValueGroup(
                    ConfigTracker,
                    """{"name": "MarketplaceConfig", "value": [{"name": "LocalName", "value": ""}]}"""
                )
            }
            folder.resolve("$NAME.json").delete()
        }
    }

    private fun load(context: ClientGameTestContext, folder: File, enabled: Boolean, number: Int) {
        folder.resolve("$NAME.json").writeText(
            """{"name": "modules", "value": [{"name": "GameTestFlaky", "value": [""" +
                """{"name": "Enabled", "value": $enabled}, {"name": "Number", "value": $number}]}]}"""
        )
        notifications.clear()
        context.command("localconfig load $NAME")
    }

    /**
     * The summary names one applied value (Number) and one failed (Enabled).
     */
    private fun checkFailureCounted() {
        val summary = notifications.singleOrNull { it.title == "Loaded $NAME" }
        check(
            summary != null && summary.severity == NotificationEvent.Severity.ERROR &&
                summary.message.startsWith("1 applied") && summary.message.endsWith("1 failed")
        ) { "Expected one error summary with the failed toggle, got ${summary?.message} as ${summary?.severity}" }
    }

    private fun state(module: FlakyModule) =
        "enabled ${module.enabled}, set up ${module.active}, running ${module.running}"

    private companion object {
        const val NAME = "gametest_rollback"
    }

}
