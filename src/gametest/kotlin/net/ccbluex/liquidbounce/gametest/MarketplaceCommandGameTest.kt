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

import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceItem
import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceItemStatus
import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceItemType
import net.ccbluex.liquidbounce.features.marketplace.MarketplaceManager
import net.ccbluex.liquidbounce.features.marketplace.SubscribedItem
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext

/**
 * `.marketplace unsubscribe` and `update` name subscribed items and suggest them as `author/name`.
 *
 * Scripts stand in for real items: nothing handles them without the ScriptAPI add-on and none is installed,
 * so unsubscribing touches neither the network nor more than the marketplace config on disk.
 */
class MarketplaceCommandGameTest : FabricClientGameTest {

    private val glass = subscription("Game Test Glass", 4200, "CCBlueX")
    private val ours = subscription("GameTestTwin", 4201, "CCBlueX")
    private val theirs = subscription("GameTestTwin", 4202, "Someone")
    private val items = listOf(glass, ours, theirs)

    override fun runTest(context: ClientGameTestContext) {
        context.waitForClient()
        context.onClient { MarketplaceManager.subscribedItems += items }

        try {
            suggestsAddresses(context)
            unsubscribesByName(context)
        } finally {
            context.onClient { MarketplaceManager.subscribedItems -= items.toSet() }
        }
    }

    private fun suggestsAddresses(context: ClientGameTestContext) {
        for (command in listOf("unsubscribe", "update")) {
            val all = context.suggest(".marketplace $command ")
            val expected = listOf("\"CCBlueX/Game Test Glass\"", "CCBlueX/GameTestTwin", "Someone/GameTestTwin")
            check(all.sorted() == expected.sorted()) { "$command suggests author/name: $all" }

            val typed = context.suggest(".marketplace $command \"ccbluex/game test g")
            check(typed == listOf("\"CCBlueX/Game Test Glass\"")) { "$command completes what is typed: $typed" }
        }
    }

    private fun unsubscribesByName(context: ClientGameTestContext) {
        context.command("marketplace unsubscribe \"game test glass\"")
        context.waitFor { !MarketplaceManager.isSubscribed(glass.id) }

        // Refused with the ids to pick from, so both stay
        context.command("marketplace unsubscribe GameTestTwin")
        context.waitTicks(SETTLE_TICKS)
        check(context.fromClient { items.count { MarketplaceManager.isSubscribed(it.id) } } == 2) {
            "an ambiguous name unsubscribes nothing"
        }

        context.command("marketplace unsubscribe someone/GameTestTwin")
        context.waitFor { !MarketplaceManager.isSubscribed(theirs.id) }
        check(context.fromClient { MarketplaceManager.isSubscribed(ours.id) }) { "author/name keeps the other one" }

        context.command("marketplace unsubscribe 4201")
        context.waitFor { !MarketplaceManager.isSubscribed(ours.id) }
    }

    private fun subscription(name: String, id: Int, author: String) = SubscribedItem(
        MarketplaceItem(
            id = id,
            uid = "",
            type = MarketplaceItemType.SCRIPT,
            name = name,
            branch = "",
            description = "",
            thumbnailPid = null,
            featured = false,
            createdAt = "",
            status = MarketplaceItemStatus.ACTIVE,
            author = author,
        )
    )

    private companion object {
        const val SETTLE_TICKS = 20
    }

}
