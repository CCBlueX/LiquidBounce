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
package net.ccbluex.liquidbounce.gametest.visual

import net.ccbluex.liquidbounce.gametest.fromClient
import net.ccbluex.liquidbounce.gametest.onClient
import net.ccbluex.liquidbounce.gametest.waitForClient
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.phys.EntityHitResult

/**
 * The theme's target HUD with the held item, the distance, the health trail and the hurt flash.
 */
class HudTargetDetailsGameTest : FabricClientGameTest {

    override fun runTest(context: ClientGameTestContext) {
        context.waitForClient()
        context.inVisualTestWorld {
            val target = context.spawnFakePlayer("Target", 0.0, 3.0)
            context.onClient {
                target.setItemSlot(EquipmentSlot.MAINHAND, ItemStack(Items.DIAMOND_SWORD))
                target.setItemSlot(EquipmentSlot.HEAD, ItemStack(Items.IRON_HELMET))
            }
            context.waitTicks(2)
            check(context.fromClient { (it.hitResult as? EntityHitResult)?.entity === target }) {
                "The target is not in the crosshair"
            }

            context.input.holdKeyFor({ it.keyAttack }, 1)
            context.waitTicks(SETTLE_TICKS)

            // The fake player only exists on the client, so the damage of the first hit is applied here
            context.onClient { target.health = TARGET_HEALTH_AFTER_HIT }
            context.input.holdKeyFor({ it.keyAttack }, 1)
            context.waitTicks(2)
            context.takeScreenshot("visual_hud_targethud")

            context.onClient { target.discard() }
        }
    }

    private companion object {
        const val SETTLE_TICKS = 20
        const val TARGET_HEALTH_AFTER_HIT = 8f
    }

}
