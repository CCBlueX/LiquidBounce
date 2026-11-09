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
import net.ccbluex.liquidbounce.config.autoconfig.AutoConfig
import net.ccbluex.liquidbounce.features.marketplace.autoconfig.ConfigTracker
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import java.io.File

/**
 * `.localconfig load` refuses configs above the size and nesting limits and still loads one right at the nesting
 * limit.
 */
class ConfigLimitsGameTest : FabricClientGameTest {

    private class LimitsModule : ClientModule("GameTestLimits", ModuleCategories.MISC) {
        val number by int("Number", 1, 0..100)
    }

    override fun runTest(context: ClientGameTestContext) {
        context.waitForClient()
        val folder = context.fromClient { ConfigSystem.userConfigsFolder }
        val module = LimitsModule()
        context.onClient { ModuleManager.addModule(module) }

        try {
            load(context, folder, AT_LIMIT, config(AT_LIMIT_NUMBER, nesting(AutoConfig.MAX_CONFIG_DEPTH)))
            check(module.number == AT_LIMIT_NUMBER) {
                "A config at the nesting limit did not load, got ${module.number}"
            }

            load(context, folder, TOO_DEEP, config(TOO_DEEP_NUMBER, nesting(AutoConfig.MAX_CONFIG_DEPTH + 1)))
            check(module.number == AT_LIMIT_NUMBER) { "A config nested too deep was applied, got ${module.number}" }

            val padding = " ".repeat(AutoConfig.MAX_CONFIG_CHARS) + "0"
            load(context, folder, TOO_LARGE, config(TOO_LARGE_NUMBER, padding))
            check(module.number == AT_LIMIT_NUMBER) {
                "A config above the size limit was applied, got ${module.number}"
            }
        } finally {
            context.onClient {
                ModuleManager.removeModule(module)
                // The loads above made the tracker show the local config
                ConfigSystem.deserializeValueGroup(
                    ConfigTracker,
                    """{"name": "MarketplaceConfig", "value": [{"name": "LocalName", "value": ""}]}"""
                )
            }
            listOf(AT_LIMIT, TOO_DEEP, TOO_LARGE).forEach { folder.resolve("$it.json").delete() }
        }
    }

    private fun load(context: ClientGameTestContext, folder: File, name: String, config: String) {
        folder.resolve("$name.json").writeText(config)
        context.command("localconfig load $name")
    }

    /**
     * A config setting [number], with [extra] after its last value. The root object is the first level.
     */
    private fun config(number: Int, extra: String) =
        """{"name": "modules", "value": [{"name": "GameTestLimits", "value": [""" +
            """{"name": "Number", "value": $number}]}], "extra": $extra}"""

    /**
     * Arrays nested so the whole config reaches [depth] levels.
     */
    private fun nesting(depth: Int) = "[".repeat(depth - 1) + "]".repeat(depth - 1)

    private companion object {
        const val AT_LIMIT = "gametest_at_limit"
        const val TOO_DEEP = "gametest_too_deep"
        const val TOO_LARGE = "gametest_too_large"

        const val AT_LIMIT_NUMBER = 5
        const val TOO_DEEP_NUMBER = 6
        const val TOO_LARGE_NUMBER = 7
    }

}
