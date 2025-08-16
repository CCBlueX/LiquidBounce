/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
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

package net.ccbluex.liquidbounce.features.module.modules.player.cheststealer.features

import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.OverlayRenderEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.ScreenEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.player.cheststealer.ModuleChestStealer
import net.ccbluex.liquidbounce.features.module.modules.player.cheststealer.ModuleChestStealer.canBeStolen
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.newDrawContext
import net.ccbluex.liquidbounce.render.renderEnvironmentForGUI
import net.ccbluex.liquidbounce.utils.inventory.getSlotsInContainer
import net.ccbluex.liquidbounce.utils.render.WorldToScreen
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import kotlin.collections.forEachIndexed

private const val COLUMNS = 9
private const val PLAYER_INV_ROWS = 4
private const val ITEM_SIZE = 16
private const val ITEM_SCALE = 1.0F
private const val BACKGROUND_PADDING = 2

object FeatureSilentScreen : ToggleableConfigurable(ModuleChestStealer, "SilentScreen", false) {

    private val drawInventoryTag = object : ToggleableConfigurable(this, "DrawInventoryTag", enabled = true) {

        private val backgroundColor by color("BackgroundColor", Color4b(Int.MIN_VALUE, hasAlpha = true))
        private val scale by float("Scale", 1.5F, 0.25F..4F)
        private val renderOffset by vec3d("RenderOffset", Vec3d.ZERO)

        val overlayRenderHandler = handler<OverlayRenderEvent> { event ->
            if (!shouldHide) return@handler

            val blockEntity = lastInteractedBlock?.let { world.getBlockEntity(it) } ?: return@handler

            val pos = WorldToScreen.calculateScreenPos(
                blockEntity.pos.toCenterPos().add(renderOffset)
            ) ?: return@handler

            val containerScreen = mc.currentScreen as GenericContainerScreen
            val slots = getSlotsInContainer(containerScreen)

            renderEnvironmentForGUI {
                val dc = newDrawContext()
                val width = ITEM_SIZE * COLUMNS
                val height = ITEM_SIZE * slots.size / COLUMNS

                val itemScale = ITEM_SCALE * scale
                dc.matrices.translate(pos.x, pos.y, 0.0F)
                dc.matrices.scale(itemScale, itemScale, 1.0F)
                dc.matrices.translate(-width / 2f, -height / 2f, pos.z)

                // draw background
                dc.fill(
                    -BACKGROUND_PADDING,
                    -BACKGROUND_PADDING,
                    width + BACKGROUND_PADDING,
                    height + BACKGROUND_PADDING,
                    backgroundColor.toARGB()
                )

                // render stacks
                slots.forEachIndexed { i, slot ->
                    val leftX = i % COLUMNS * ITEM_SIZE
                    val topY = i / COLUMNS * ITEM_SIZE
                    val stack = containerScreen.screenHandler.slots[i].stack
                    if (stack.isEmpty) return@forEachIndexed

                    dc.drawItem(stack, leftX, topY)
                    dc.drawStackOverlay(mc.textRenderer, stack, leftX, topY)
                }
            }
        }
    }

    init {
        tree(drawInventoryTag)
    }

    @get:JvmStatic
    var shouldHide = false

    val screenHandler = handler<ScreenEvent> { event ->
        shouldHide = event.screen?.canBeStolen() == true
    }

    @Volatile
    private var lastInteractedBlock: BlockPos? = null

    val packetHandler = handler<PacketEvent> { event ->
        val packet = event.packet
        // TODO: handle other interactions
        if (packet is PlayerInteractBlockC2SPacket && packet.blockHitResult.type === HitResult.Type.BLOCK) {
            lastInteractedBlock = packet.blockHitResult.blockPos
        }
    }

    override fun onDisabled() {
        shouldHide = false
        lastInteractedBlock = null
    }
}
