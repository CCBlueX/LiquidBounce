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

package net.ccbluex.liquidbounce.utils.render

import net.ccbluex.liquidbounce.annotations.ValueClassCandidate
import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.WorldFeatureSubmitEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.injection.mixins.minecraft.network.MixinMultiPlayerGameModeAccessor
import net.ccbluex.liquidbounce.render.usePoseStack
import net.ccbluex.liquidbounce.render.withPush
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.submitTextAlwaysOnTop
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.gui.Font
import net.minecraft.client.multiplayer.MultiPlayerGameMode
import net.minecraft.client.renderer.entity.EntityRenderer
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Style
import net.minecraft.util.FormattedCharSequence
import net.minecraft.util.LightCoordsUtil

@ValueClassCandidate
@JvmRecord
data class BreakingProgress(val pos: BlockPos, val value: Float) {
    fun interface Provider {
        fun breakingProgress(): BreakingProgress?

        companion object Default : Provider {
            override fun breakingProgress(): BreakingProgress? {
                val interaction = mc.gameMode ?: return null
                if (!interaction.isDestroying) {
                    return null
                }

                return interaction.breakingProgress()
            }

            /**
             * Reads progress for a module-owned target. Vanilla can keep updating `destroyProgress`
             * without restoring `isDestroying` when continuing a previously stopped target.
             */
            fun breakingProgress(pos: BlockPos): BreakingProgress? {
                val interaction = mc.gameMode ?: return null
                return interaction.breakingProgress().takeIf {
                    it.pos == pos && (interaction.isDestroying || it.value > 0f)
                }
            }

            private fun MultiPlayerGameMode.breakingProgress(): BreakingProgress {
                val pos = (this as MixinMultiPlayerGameModeAccessor).`liquid_bounce$getDestroyBlockPos`()
                return BreakingProgress(pos, destroyProgress)
            }
        }
    }
}

class BreakingProgressRenderer @JvmOverloads constructor(
    parent: EventListener,
    private val progressProvider: BreakingProgress.Provider = BreakingProgress.Provider.Default,
) : ToggleableValueGroup(parent, "ProgressText", true) {

    private val height by float("Height", 0.5f, -4f..4f)
    private val scale by float("Scale", 1f, 0.25f..4f)
    private val color by color("Color", Color4b.WHITE)
    private val shadow by boolean("Shadow", true)

    @Suppress("unused")
    private val renderHandler = handler<WorldFeatureSubmitEvent> { event ->
        val progress = progressProvider.breakingProgress() ?: return@handler
        val percentage = (progress.value.coerceIn(0f, 1f) * 100f).toInt()
        val text = "$percentage%"
        val formattedText = FormattedCharSequence.forward(text, Style.EMPTY)
        val font = mc.font

        val camera = event.camera
        val cameraPos = camera.position()
        usePoseStack {
            withPush {
                translate(
                    progress.pos.x + 0.5 - cameraPos.x,
                    progress.pos.y + height.toDouble() - cameraPos.y,
                    progress.pos.z + 0.5 - cameraPos.z,
                )
                mulPose(camera.rotation())
                scale(
                    EntityRenderer.NAMETAG_SCALE * scale,
                    -EntityRenderer.NAMETAG_SCALE * scale,
                    EntityRenderer.NAMETAG_SCALE * scale,
                )
                event.submitNodeStorage.submitTextAlwaysOnTop(
                    this,
                    -font.width(text) * 0.5f,
                    -font.lineHeight * 0.5f,
                    formattedText,
                    shadow,
                    Font.DisplayMode.SEE_THROUGH,
                    LightCoordsUtil.FULL_BRIGHT,
                    color.argb,
                    0,
                    0,
                )
            }
        }
    }
}
