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

package net.ccbluex.liquidbounce.features.module.modules.player.cheststealer.features

import net.ccbluex.fastutil.mapToArray
import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.event.events.OverlayRenderEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.ScreenEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.player.cheststealer.ModuleChestStealer
import net.ccbluex.liquidbounce.features.module.modules.player.cheststealer.ModuleChestStealer.canBeStolen
import net.ccbluex.liquidbounce.render.ItemStackListRenderer.BackgroundMode.Companion.backgroundChoices
import net.ccbluex.liquidbounce.render.ItemStackListRenderer.Companion.drawItemStackList
import net.ccbluex.liquidbounce.render.engine.type.Vec3f
import net.ccbluex.liquidbounce.utils.block.anotherChestPartDirection
import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.inventory.getSlotsInContainer
import net.ccbluex.liquidbounce.utils.math.toVec3d
import net.ccbluex.liquidbounce.utils.render.WorldToScreen
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.core.BlockPos
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket
import net.minecraft.world.phys.HitResult

/**
 * @see net.ccbluex.liquidbounce.injection.mixins.minecraft.client.MixinMinecraft
 * @see net.ccbluex.liquidbounce.injection.mixins.minecraft.gui.MixinAbstractContainerScreen
 */
object FeatureSilentScreen : ToggleableValueGroup(ModuleChestStealer, "SilentScreen", false) {

    val unlockCursor by boolean("UnlockCursor", false)

    private val drawInventoryTag = object : ToggleableValueGroup(this, "DrawInventoryTag", enabled = true) {

        private val background = modes(this, "Background", 0, ::backgroundChoices)
        private val scale by float("Scale", 1.5F, 0.25F..4F)
        private val renderOffset by vec3d("RenderOffset", useLocateButton = false)
        private val showTitle by boolean("ShowTitle", false)

        init {
            // This is a feature for rendering, skip it in config publication.
            doNotIncludeAlways()
        }

        private fun getRenderPos(): Vec3f? {
            val pos = lastInteractedBlock ?: return null
            val state = pos.getState() ?: return null
            val anotherPartDirection = state.anotherChestPartDirection()

            // Double chest
            val centerPos = anotherPartDirection?.let {
                pos.toVec3d(
                    0.5 + anotherPartDirection.stepX * 0.5,
                    0.5 + anotherPartDirection.stepY * 0.5,
                    0.5 + anotherPartDirection.stepZ * 0.5,
                )
            } ?: pos.center

            return WorldToScreen.calculateScreenPos(centerPos.add(renderOffset))
        }

        @Suppress("unused")
        private val overlayRenderHandler = handler<OverlayRenderEvent> { event ->
            if (!shouldHide) return@handler

            val pos = getRenderPos() ?: return@handler

            val containerScreen = mc.screen as AbstractContainerScreen<*>

            event.context.drawItemStackList(containerScreen.getSlotsInContainer().mapToArray { it.itemStack })
                .title(containerScreen.title.takeIf { showTitle })
                .centerX(pos.x)
                .centerY(pos.y)
                .scale(scale)
                .background(background.activeMode)
                .draw()
        }
    }

    init {
        tree(drawInventoryTag)
    }

    var shouldHide = false
        private set

    @Suppress("unused")
    private val screenHandler = handler<ScreenEvent> { event ->
        shouldHide = event.screen?.canBeStolen() == true
    }

    @Volatile
    private var lastInteractedBlock: BlockPos? = null

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent> { event ->
        val packet = event.packet
        // TODO: handle other interactions
        if (packet is ServerboundUseItemOnPacket && packet.hitResult.type === HitResult.Type.BLOCK) {
            lastInteractedBlock = packet.hitResult.blockPos
        }
    }

    override fun onDisabled() {
        shouldHide = false
        lastInteractedBlock = null
    }
}
