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
import net.ccbluex.liquidbounce.render.ItemStackListRenderer.BackgroundChoice.Companion.backgroundChoices
import net.ccbluex.liquidbounce.render.ItemStackListRenderer.Companion.drawItemStackList
import net.ccbluex.liquidbounce.render.engine.type.Vec3
import net.ccbluex.liquidbounce.render.renderEnvironmentForGUI
import net.ccbluex.liquidbounce.utils.block.anotherChestPartDirection
import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.inventory.getSlotsInContainer
import net.ccbluex.liquidbounce.utils.math.toVec3d
import net.ccbluex.liquidbounce.utils.render.WorldToScreen
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d

/**
 * @see net.ccbluex.liquidbounce.injection.mixins.minecraft.client.MixinMinecraftClient
 * @see net.ccbluex.liquidbounce.injection.mixins.minecraft.gui.MixinHandledScreen
 */
object FeatureSilentScreen : ToggleableConfigurable(ModuleChestStealer, "SilentScreen", false) {

    @get:JvmStatic
    val unlockCursor by boolean("UnlockCursor", false)

    private val drawInventoryTag = object : ToggleableConfigurable(this, "DrawInventoryTag", enabled = true) {

        private val background = choices(this, "Background", 0, ::backgroundChoices)
        private val scale by float("Scale", 1.5F, 0.25F..4F)
        private val renderOffset by vec3d("RenderOffset", Vec3d.ZERO)
        private val showTitle by boolean("ShowTitle", false)

        init {
            // This is a feature for rendering, skip it in config publication.
            doNotIncludeAlways()
        }

        private fun getRenderPos(): Vec3? {
            val pos = lastInteractedBlock ?: return null
            val state = pos.getState() ?: return null
            val anotherPartDirection = state.anotherChestPartDirection()

            // Double chest
            val centerPos = anotherPartDirection?.let {
                pos.toVec3d(
                    0.5 + anotherPartDirection.offsetX * 0.5,
                    0.5 + anotherPartDirection.offsetY * 0.5,
                    0.5 + anotherPartDirection.offsetZ * 0.5,
                )
            } ?: pos.toCenterPos()

            return WorldToScreen.calculateScreenPos(centerPos.add(renderOffset))
        }

        @Suppress("unused")
        private val overlayRenderHandler = handler<OverlayRenderEvent> { event ->
            if (!shouldHide) return@handler

            val pos = getRenderPos() ?: return@handler

            val containerScreen = mc.currentScreen as HandledScreen<*>

            renderEnvironmentForGUI {
                event.context.drawItemStackList(containerScreen.getSlotsInContainer().map { it.itemStack })
                    .title(if (showTitle) containerScreen.title.string else "")
                    .center(pos)
                    .scale(scale)
                    .background(background.activeChoice)
                    .draw()
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
