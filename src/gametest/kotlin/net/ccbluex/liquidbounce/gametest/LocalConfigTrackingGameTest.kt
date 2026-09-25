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

import com.google.gson.JsonParser
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.features.marketplace.autoconfig.ConfigTracker
import net.ccbluex.liquidbounce.features.module.modules.misc.ModuleAutoConfig
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import java.io.File

/**
 * `.localconfig load` over a tracked marketplace config: a local config saved from it, or only some modules of
 * one, goes on as its edits; any other replaces it.
 *
 * The tracked config is faked through the tracker's stored values, so nothing is downloaded.
 */
class LocalConfigTrackingGameTest : FabricClientGameTest {

    private class Tracking(val state: ConfigTracker.State, val itemId: Int?, val localName: String, val tag: String?)

    override fun runTest(context: ClientGameTestContext) {
        context.waitForClient()
        val folder = context.fromClient { ConfigSystem.userConfigsFolder }

        try {
            roundTripKeepsTracking(context, folder)
            otherConfigStopsTracking(context)
            partialLoadKeepsTracking(context)
        } finally {
            context.onClient { setTracker(State.NONE, 0, "") }
            listOf(ORIGIN, OTHER).forEach { folder.resolve("$it.json").delete() }
        }
    }

    private fun roundTripKeepsTracking(context: ClientGameTestContext, folder: File) {
        context.onClient { track() }
        context.command("localconfig save $ORIGIN true")

        val saved = folder.resolve("$ORIGIN.json").bufferedReader().use { JsonParser.parseReader(it).asJsonObject }
        val origin = saved["marketplaceItemId"]?.asInt
        check(origin == ITEM_ID) { "saving keeps the tracked config, got $origin" }

        context.command("localconfig load $ORIGIN")
        checkTracked(context, "loading a config saved from the tracked one")
    }

    private fun otherConfigStopsTracking(context: ClientGameTestContext) {
        context.onClient { setTracker(State.NONE, 0, "") }
        context.command("localconfig save $OTHER true")
        context.onClient { track() }
        context.command("localconfig load $OTHER")

        val tracking = tracking(context)
        check(tracking.state == State.NONE) { "another local config stops tracking, got ${tracking.state}" }
        check(tracking.localName == OTHER && tracking.tag == OTHER) {
            "AutoConfig shows the local config, got ${tracking.localName} and ${tracking.tag}"
        }
    }

    private fun partialLoadKeepsTracking(context: ClientGameTestContext) {
        context.onClient { track() }
        context.command("localconfig load $OTHER KillAura")
        checkTracked(context, "loading some modules")
    }

    private fun checkTracked(context: ClientGameTestContext, case: String) {
        val tracking = tracking(context)
        check(tracking.state != State.NONE && tracking.itemId == ITEM_ID && tracking.localName.isEmpty()) {
            "$case keeps tracking, got ${tracking.state} of ${tracking.itemId}, local ${tracking.localName}"
        }
    }

    private fun tracking(context: ClientGameTestContext) = context.fromClient {
        Tracking(ConfigTracker.state, ConfigTracker.trackedItemId, ConfigTracker.localName, ModuleAutoConfig.tag)
    }

    private fun track() = setTracker(State.TRACKED, ITEM_ID, "GameTest")

    private fun setTracker(state: State, itemId: Int, name: String) {
        ConfigSystem.deserializeValueGroup(
            ConfigTracker,
            JsonParser.parseString(
                """
                {"name": "MarketplaceConfig", "value": [
                    {"name": "State", "value": "${state.tag}"},
                    {"name": "ItemId", "value": $itemId},
                    {"name": "ItemName", "value": "$name"},
                    {"name": "LocalName", "value": ""}
                ]}
                """
            )
        )
    }

    private companion object {
        const val ITEM_ID = 4242
        const val ORIGIN = "gametest_origin"
        const val OTHER = "gametest_other"
    }

}

private typealias State = ConfigTracker.State
