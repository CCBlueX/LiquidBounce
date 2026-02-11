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

package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.additions.drawBorder
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.injection.mixins.minecraft.gui.MixinGuiAccessor
import net.ccbluex.liquidbounce.render.ItemStackListRenderer.Companion.drawItemStackList
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.inventory.InventoryManager
import net.ccbluex.liquidbounce.utils.item.getCooldown
import net.ccbluex.liquidbounce.utils.math.toFixed
import net.minecraft.client.gui.Gui
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.render.GuiRenderer
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.core.component.DataComponents
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack

object ModuleBetterInventory : ClientModule("BetterInventory", ModuleCategories.RENDER) {

    private object HighlightClicked : ToggleableValueGroup(this, "HighlightClicked", enabled = true) {
        val mode = modes(this, "Mode", 0) {
            arrayOf(Mode.Border, Mode.Texture)
        }

        sealed class Mode(choiceName: String) : net.ccbluex.liquidbounce.config.types.group.Mode(choiceName) {
            final override val parent: ModeValueGroup<*>
                get() = mode

            abstract fun drawHighlightSlot(context: GuiGraphics, slot: Slot)

            object Border : Mode("Border") {
                val color by color("Color", Color4b.GREEN)

                override fun drawHighlightSlot(context: GuiGraphics, slot: Slot) {
                    context.drawBorder(
                        slot.x,
                        slot.y,
                        GuiRenderer.DEFAULT_ITEM_SIZE,
                        GuiRenderer.DEFAULT_ITEM_SIZE,
                        color.argb,
                    )
                }
            }

            object Texture : Mode("Texture") {
                /**
                 * @see Gui.renderItemHotbar
                 */
                override fun drawHighlightSlot(context: GuiGraphics, slot: Slot) {
                    context.blitSprite(
                        RenderPipelines.GUI_TEXTURED,
                        MixinGuiAccessor.getHotbarSelectionTexture(),
                        slot.x - 3,
                        slot.y - 3,
                        22,
                        21,
                    )
                }
            }
        }
    }

    init {
        tree(HighlightClicked)
    }

    private object TextCooldownProgress : ToggleableValueGroup(this, "TextCooldownProgress", enabled = true) {
        val mode by enumChoice("Mode", CooldownProgressMode.PERCENTAGE)

        val scale by float("Scale", 1F, 0.25F..4F)
        val color by color("Color", Color4b.WHITE)
    }

    init {
        tree(TextCooldownProgress)
    }

    private enum class CooldownProgressMode(override val tag: String): Tagged {
        PERCENTAGE("Percentage"),
        DURATION_TICKS("DurationTicks"),
        DURATION_SECONDS("DurationSeconds"),
    }

    private object ContainerItemView : ToggleableValueGroup(this, "ContainerItemView", enabled = true) {
        val skipEmptyStack by boolean("SkipEmptyStack", false)

        val scale by float("Scale", 1F, 0.25F..4F)
        val relativeToMouse by boolean("RelativeToMouse", true)
        val renderOffsetX by float("RenderOffsetX", 150.0F, -4096F..4096F)
        val renderOffsetY by float("RenderOffsetY", 0.0F, -4096F..4096F)
    }

    init {
        tree(ContainerItemView)
    }

    fun GuiGraphics.drawTextCooldownProgress(stack: ItemStack, x: Int, y: Int) {
        if (!running || stack.isEmpty || !TextCooldownProgress.enabled) return

        val player = mc.player ?: return

        val progress = player.cooldowns.getCooldownPercent(stack, mc.deltaTracker.getGameTimeDeltaPartialTick(true))

        if (progress > 0.0F) {
            this.pose().pushMatrix()
            this.pose().scale(TextCooldownProgress.scale)
            val text = when (TextCooldownProgress.mode) {
                CooldownProgressMode.PERCENTAGE -> "${(progress * 100f).toInt()}%"
                CooldownProgressMode.DURATION_TICKS -> {
                    val entry = player.cooldowns.getCooldown(stack)!!
                    val ticks = entry.endTick - entry.currentTick
                    ticks.toString()
                }
                CooldownProgressMode.DURATION_SECONDS -> {
                    val entry = player.cooldowns.getCooldown(stack)!!
                    val seconds = (entry.endTick - entry.currentTick) * 0.05f
                    if (seconds > 1) "${seconds.toInt()}s" else "${seconds.toFixed(1)}s"
                }
            }
            this.drawCenteredString(
                mc.font,
                text,
                x + GuiRenderer.DEFAULT_ITEM_SIZE / 2,
                y,
                TextCooldownProgress.color.argb,
            )
            this.pose().popMatrix()
        }
    }

    fun GuiGraphics.drawHighlightSlot(slot: Slot) {
        if (!running || !HighlightClicked.enabled || slot.index != InventoryManager.lastClickedSlot) return

        HighlightClicked.mode.activeMode.drawHighlightSlot(this, slot)
    }

    fun GuiGraphics.drawContainerItemView(
        stack: ItemStack,
        x: Int,
        y: Int,
        mouseX: Int,
        mouseY: Int,
    ): Boolean {
        if (!running || stack.isEmpty || !ContainerItemView.enabled) return false

        val containerComponent = stack[DataComponents.CONTAINER] ?: return false

        val stacks = if (ContainerItemView.skipEmptyStack) {
            containerComponent.nonEmptyStream()
        } else {
            containerComponent.stream()
        }.toList()

        if (stacks.isEmpty()) return false

        var renderX = ContainerItemView.renderOffsetX - x.toFloat()
        var renderY = ContainerItemView.renderOffsetY - y.toFloat()

        if (ContainerItemView.relativeToMouse) {
            renderX += mouseX
            renderY += mouseY
        }

        drawItemStackList(stacks)
            .centerX(renderX)
            .centerY(renderY)
            .scale(ContainerItemView.scale)
            .textureBackground()
            .draw()

        return true
    }

}
