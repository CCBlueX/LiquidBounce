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

package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.injection.mixins.minecraft.gui.MixinInGameHudAccessor
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.inventory.InventoryManager
import net.ccbluex.liquidbounce.utils.item.getCooldown
import net.ccbluex.liquidbounce.utils.math.toFixed
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.render.RenderLayer
import net.minecraft.item.ItemStack
import net.minecraft.screen.slot.Slot

object ModuleBetterInventory : ClientModule("BetterInventory", Category.RENDER) {

    private object HighlightClicked : ToggleableConfigurable(this, "HighlightClicked", enabled = true) {
        val mode = choices("Mode", Mode.Border, arrayOf(Mode.Border, Mode.Texture))

        sealed class Mode(choiceName: String) : Choice(choiceName) {
            final override val parent: ChoiceConfigurable<*>
                get() = mode

            abstract fun drawHighlightSlot(context: DrawContext, slot: Slot)

            object Border : Mode("Border") {
                private const val STACK_SIZE = 16
                val color by color("Color", Color4b.GREEN)

                override fun drawHighlightSlot(context: DrawContext, slot: Slot) {
                    context.drawBorder(slot.x, slot.y, STACK_SIZE, STACK_SIZE, color.toARGB())
                }
            }

            object Texture : Mode("Texture") {
                /**
                 * @see net.minecraft.client.gui.hud.InGameHud.renderHotbar
                 */
                override fun drawHighlightSlot(context: DrawContext, slot: Slot) {
                    context.matrices.push()
                    context.matrices.translate(0f, 0f, 1000f)
                    context.drawGuiTexture(
                        RenderLayer::getGuiTextured,
                        MixinInGameHudAccessor.getHotbarSelectionTexture(),
                        slot.x - 3,
                        slot.y - 3,
                        22,
                        21,
                    )
                    context.matrices.pop()
                }
            }
        }
    }

    init {
        tree(HighlightClicked)
    }


    private object TextCooldownProgress : ToggleableConfigurable(this, "TextCooldownProgress", enabled = true) {
        val mode by enumChoice("Mode", CooldownProgressMode.PERCENTAGE)

        val scale by float("Scale", 1F, 0.25F..4F)
        val color by color("Color", Color4b.WHITE)
    }

    init {
        tree(TextCooldownProgress)
    }

    private enum class CooldownProgressMode(override val choiceName: String): NamedChoice {
        PERCENTAGE("Percentage"),
        DURATION_TICKS("DurationTicks"),
        DURATION_SECONDS("DurationSeconds"),
    }

    fun DrawContext.drawTextCooldownProgress(stack: ItemStack, x: Int, y: Int) {
        if (!running || stack.isEmpty || !TextCooldownProgress.enabled) return

        val player = mc.player ?: return

        val progress = player.itemCooldownManager.getCooldownProgress(stack, mc.renderTickCounter.getTickDelta(true))

        if (progress > 0.0F) {
            this.matrices.push()
            this.matrices.translate(0.0, 0.0, 200.0)
            this.matrices.scale(TextCooldownProgress.scale, TextCooldownProgress.scale, 1f)
            val text = when (TextCooldownProgress.mode) {
                CooldownProgressMode.PERCENTAGE -> "${(progress * 100f).toInt()}%"
                CooldownProgressMode.DURATION_TICKS -> {
                    val entry = player.itemCooldownManager.getCooldown(stack)!!
                    val ticks = entry.endTick - entry.currentTick
                    ticks.toString()
                }
                CooldownProgressMode.DURATION_SECONDS -> {
                    val entry = player.itemCooldownManager.getCooldown(stack)!!
                    val seconds = (entry.endTick - entry.currentTick) * 0.05f
                    if (seconds > 1) "${seconds.toInt()}s" else "${seconds.toFixed(1)}s"
                }
            }
            this.drawCenteredTextWithShadow(mc.textRenderer, text, x + 16 / 2, y, TextCooldownProgress.color.toARGB())
            this.matrices.pop()
        }
    }

    fun drawHighlightSlot(context: DrawContext, slot: Slot) {
        if (!running || slot.id != InventoryManager.lastClickedSlot) return

        HighlightClicked.mode.activeChoice.drawHighlightSlot(context, slot)
    }

}
