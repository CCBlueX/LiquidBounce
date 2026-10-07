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
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.RotationUpdateEvent
import net.ccbluex.liquidbounce.features.module.modules.world.autobuild.ModuleAutoBuild
import net.ccbluex.liquidbounce.features.module.modules.world.autobuild.PortalMode
import net.ccbluex.liquidbounce.utils.client.mc
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.minecraft.core.BlockPos
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState

/** Rejects obstructed portal sites, then exercises a successful enable and teardown. */
class PortalPositionGameTest : FabricClientGameTest {

    override fun runTest(context: ClientGameTestContext) {
        context.waitForClient()
        val saved = context.fromClient { ConfigSystem.serializeValueGroup(ModuleAutoBuild) }

        try {
            context.worldBuilder().create().use { world ->
                world.server.runCommand("gamemode creative @a")
                context.waitFor({ client -> client.player?.hasInfiniteMaterials() == true }, 200)
                context.onClient {
                    ModuleAutoBuild.enabled = false
                    ModuleAutoBuild.settings.getValue("Mode").setByString("Portal")
                    checkNotNull(mc.player).setItemInHand(
                        InteractionHand.MAIN_HAND, ItemStack(Items.OBSIDIAN, 64)
                    )
                    fillSurroundings(Blocks.STONE.defaultBlockState())

                    // Call the mode directly too: Value.set logs listener exceptions instead of propagating them.
                    PortalMode.enabled()
                    check(ModuleAutoBuild.placer.isDone()) { "An invalid site queued frame blocks" }
                    check(ModuleAutoBuild.placer.support.blockedPositions.isEmpty())
                }
                context.waitTicks(1)

                repeat(2) {
                    context.onClient {
                        ModuleAutoBuild.enabled = true
                        EventManager.callEvent(RotationUpdateEvent)
                    }
                    context.waitFor({ !ModuleAutoBuild.enabled }, 20)
                    context.onClient {
                        check(ModuleAutoBuild.placer.isDone())
                        check(ModuleAutoBuild.placer.support.blockedPositions.isEmpty())
                    }
                }

                assertImmediateRetry(context)
            }
        } finally {
            context.onClient {
                ModuleAutoBuild.enabled = false
                ConfigSystem.deserializeValueGroup(ModuleAutoBuild, saved)
            }
        }
    }

    private fun assertImmediateRetry(context: ClientGameTestContext) {
        context.onClient {
            // Leave an invalid attempt's disable queued, then immediately start a valid attempt.
            ModuleAutoBuild.enabled = true
            ModuleAutoBuild.enabled = false
            fillSurroundings(Blocks.AIR.defaultBlockState())
            ModuleAutoBuild.enabled = true
            check(ModuleAutoBuild.enabled) { "A valid portal site could not be enabled" }
            check(!ModuleAutoBuild.placer.isDone()) { "A valid site did not queue frame blocks" }
            check(ModuleAutoBuild.placer.support.blockedPositions.size == 6)
        }
        context.waitTicks(1)
        context.onClient {
            check(ModuleAutoBuild.enabled) { "An old invalid attempt disabled the valid retry" }
            ModuleAutoBuild.enabled = false
            check(ModuleAutoBuild.placer.isDone())
            check(ModuleAutoBuild.placer.support.blockedPositions.isEmpty())
        }
    }

    private fun fillSurroundings(state: BlockState) {
        val level = checkNotNull(mc.level)
        val center = checkNotNull(mc.player).blockPosition()
        for (pos in BlockPos.betweenClosed(center.offset(-3, -1, -3), center.offset(3, 4, 3))) {
            level.setBlock(pos, state, 3)
        }
    }

}
