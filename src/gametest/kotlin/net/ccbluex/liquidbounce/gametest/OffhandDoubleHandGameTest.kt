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

import net.ccbluex.liquidbounce.features.module.modules.player.offhand.ModuleOffhand
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext

/**
 * The Offhand Totem/DoubleHand option: after a pop, a hotbar totem is in the main hand until the switch delay lets
 * the offhand have one again.
 */
class OffhandDoubleHandGameTest : FabricClientGameTest {

    override fun runTest(context: ClientGameTestContext) = context.inOffhandWorld {
        configure("Totem/DoubleHand/Enabled" to "true", "Totem/SwitchDelay" to "$SWITCH_DELAY_MS")
        // Resistance V of the fixture blocks all damage
        cmd("effect clear @a resistance")
        giveTotems(2)
        context.onClient { ModuleOffhand.enabled = true }
        waitServer("a totem in the offhand") { it.offhandItem.isTotem() }

        // a refill right before the pop, so the switch delay holds back the next one
        refill()
        cmd("item replace entity @a hotbar.$HOTBAR_TOTEM with totem_of_undying")
        cmd("damage @a[limit=1] $LETHAL_DAMAGE")
        waitServer("the hotbar totem in the main hand while the offhand is empty") {
            !it.offhandItem.isTotem() && it.inventory.selectedSlot == HOTBAR_TOTEM && it.mainHandItem.isTotem()
        }
        waitServer("the offhand refilled and the first hotbar slot selected again") {
            it.offhandItem.isTotem() && it.inventory.selectedSlot == 0
        }
    }

    private companion object {
        const val SWITCH_DELAY_MS = 500
        const val HOTBAR_TOTEM = 3
        const val LETHAL_DAMAGE = 1000
    }

}
