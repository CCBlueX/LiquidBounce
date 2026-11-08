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
package net.ccbluex.liquidbounce.gametest.combat

import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.ModuleAntiBot
import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.modes.CustomAntiBotMode
import net.ccbluex.liquidbounce.gametest.onClient
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.minecraft.core.UUIDUtil
import java.util.UUID

/**
 * The UUID version check of the custom AntiBot mode flags exactly the players whose UUID version differs from
 * the configured one.
 */
class AntiBotUuidVersionGameTest : FabricClientGameTest {

    override fun runTest(context: ClientGameTestContext) =
        context.runCombatChecks(listOf(ModuleAntiBot), CombatScene::checkUuidVersion)

}

private fun CombatScene.checkUuidVersion() {
    startCustomAntiBot {
        CustomAntiBotMode.set("UUIDVersion/Enabled", "true")
    }
    val online = spawnFakePlayer(UUID.randomUUID(), FAKE_PLAYER_ID, "Online")
    val offline = spawnFakePlayer(UUIDUtil.createOfflinePlayerUUID("Offline"), FAKE_PLAYER_ID + 1, "Offline")

    context.onClient { CustomAntiBotMode.set("UUIDVersion/Version", "4") }
    check(!isBot(online)) { "Version 4 flagged the player with a version 4 UUID" }
    check(isBot(offline)) { "Version 4 did not flag the player with a version 3 UUID" }

    context.onClient { CustomAntiBotMode.set("UUIDVersion/Version", "3") }
    check(isBot(online)) { "Version 3 did not flag the player with a version 4 UUID" }
    check(!isBot(offline)) { "Version 3 flagged the player with a version 3 UUID" }

    stopAntiBot(online, offline)
}
