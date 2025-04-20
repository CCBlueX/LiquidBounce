package net.ccbluex.liquidbounce.features.module.modules.`fun`

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.LiquidBounce.Companion.color
import net.ccbluex.liquidbounce.LiquidBounce.Companion.colorMode
import net.ccbluex.liquidbounce.LiquidBounce.Companion.float
import net.ccbluex.liquidbounce.LiquidBounce.Companion.boolean
import net.ccbluex.liquidbounce.render.engine.RenderSystem
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.render.RenderLayer 
import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.EntityRenderEvent
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.event.sequenceHandler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.VertexConsumer
import net.minecraft.entity.EquipmentSlot
import net.minecraft.item.ArmorItem
import net.minecraft.item.ItemStack
import java.awt.Color

object ModuleRainbowArmor : Module("RainbowArmor", Category.FUN) {
    private val colorMode = choices("ColorMode", Rainbow, arrayOf(Rainbow, Static, PerPiece))
    private val speed by float("Speed", 1f, 0.1f..5f)
    private val saturation by float("Saturation", 0.7f, 0f..1f)
    private val brightness by float("Brightness", 1f, 0f..1f)
    private val opacity by float("Opacity", 1f, 0f..1f)
    
    // Glow effect
    private val glowEffect by boolean("Glow", false)
    private val glowStrength by float("GlowStrength", 0.5f, 0.1f..2f)
    private val glowPulse by boolean("GlowPulse", false)
    
    // Pulse effect
    private val pulseEffect by boolean("Pulse", false)
    private val pulseSpeed by float("PulseSpeed", 1f, 0.1f..5f)
    
    // Per-piece colors
    private val helmetColor by color("HelmetColor", Color4b(255, 0, 0, 255))
    private val chestplateColor by color("ChestplateColor", Color4b(0, 255, 0, 255))
    private val leggingsColor by color("LeggingsColor", Color4b(0, 0, 255, 255))
    private val bootsColor by color("BootsColor", Color4b(255, 255, 0, 255))
    
    // Static color
    private val staticColor by color("StaticColor", Color4b(255, 0, 0, 255))
    
    // Armor Piece Selection
    private val helmet by boolean("Helmet", true)
    private val chestplate by boolean("Chestplate", true)
    private val leggings by boolean("Leggings", true)
    private val boots by boolean("Boots", true)
    
    private var currentTick = 0f
    private var pulseTick = 0f
    
    val repeatable = repeatable {
        currentTick += speed
        if (pulseEffect) {
            pulseTick += pulseSpeed
        }
    }

    // Handle armor rendering
    val renderHandler = sequenceHandler<EntityRenderEvent> { event ->
        val player = mc.player ?: return@sequenceHandler
        if (event.entity != player) return@sequenceHandler
        
        player.armorItems.forEachIndexed { index, itemStack ->
            if (itemStack.item !is ArmorItem) return@forEachIndexed
            
            val shouldColor = when(index) {
                3 -> helmet
                2 -> chestplate
                1 -> leggings
                0 -> boots
                else -> false
            }
            
            if (!shouldColor) return@forEachIndexed
            
            val baseColor = when(colorMode.activeChoice) {
                is Rainbow -> ColorUtils.rainbow(
                    index * 100 + currentTick.toInt(),
                    saturation,
                    brightness
                )
                is Static -> staticColor.rgb
                is PerPiece -> when(index) {
                    3 -> helmetColor.rgb
                    2 -> chestplateColor.rgb
                    1 -> leggingsColor.rgb
                    0 -> bootsColor.rgb
                    else -> Color.WHITE.rgb
                }
                else -> Color.WHITE.rgb
            }
            
            // Apply pulse effect
            val pulseModifier = if (pulseEffect) {
                val pulse = (Math.sin(pulseTick.toDouble()) + 1) / 2
                pulse.toFloat()
            } else 1f
            
            // Apply glow effect
            val glowModifier = if (glowEffect) {
                val glow = if (glowPulse) {
                    (Math.sin(currentTick.toDouble()) + 1) / 2 * glowStrength
                } else glowStrength
                glow.toFloat()
            } else 0f
            
            val finalColor = applyEffects(baseColor, pulseModifier, glowModifier)
            
            renderArmorPiece(event.matrixStack, mc.bufferBuilders.entityVertexConsumers, index, finalColor, itemStack)
        }
    }
    
    private fun applyEffects(baseColor: Int, pulseModifier: Float, glowModifier: Float): Int {
        val r = (baseColor shr 16 and 255) * pulseModifier
        val g = (baseColor shr 8 and 255) * pulseModifier
        val b = (baseColor and 255) * pulseModifier
        
        return colorToRGB(
            (r + (255 - r) * glowModifier).toInt().coerceIn(0, 255),
            (g + (255 - g) * glowModifier).toInt().coerceIn(0, 255),
            (b + (255 - b) * glowModifier).toInt().coerceIn(0, 255),
            (opacity * 255).toInt()
        )
    }
    
    private fun renderArmorPiece(
        matrices: MatrixStack,
        vertexConsumers: VertexConsumerProvider,
        armorSlot: Int,
        color: Int,
        itemStack: ItemStack
    ) {
        val slot = when(armorSlot) {
            3 -> EquipmentSlot.HEAD
            2 -> EquipmentSlot.CHEST
            1 -> EquipmentSlot.LEGS
            0 -> EquipmentSlot.FEET
            else -> return
        }
        
        matrices.push()
        
        val armorItem = itemStack.item as? ArmorItem ?: return
        val renderLayer = RenderLayer.getArmorCutoutNoCull(armorItem.getArmorTexture(itemStack, slot))
        val vertexConsumer = vertexConsumers.getBuffer(renderLayer)
        
        val r = (color shr 16 and 255) / 255f
        val g = (color shr 8 and 255) / 255f
        val b = (color and 255) / 255f
        
        vertexConsumer.color(r, g, b, opacity)
        
        // Apply armor texture and render
        RenderSystem.setShaderTexture(0, armorItem.getArmorTexture(itemStack, slot))
        armorItem.render(matrices, vertexConsumers, itemStack, slot, player.getEquippedModel())
        
        matrices.pop()
    }

    companion object {
        private val armorTexture = Identifier("minecraft", "textures/models/armor/diamond_layer_1.png")
        
        fun apply(matrices: MatrixStack, vertexConsumers: VertexConsumerProvider, itemStack: ItemStack, slot: EquipmentSlot, color: Color4b) {
            val armorItem = itemStack.item as? ArmorItem ?: return
            val renderLayer = RenderLayer.getArmorCutoutNoCull(armorTexture)
            val vertexConsumer = vertexConsumers.getBuffer(renderLayer)
            
            matrices.push()
            RenderSystem.setShaderTexture(0, armorTexture)
            vertexConsumer.color(color.r / 255f, color.g / 255f, color.b / 255f, color.a / 255f)
            matrices.pop()
        }
    }
}

private object Rainbow : Choice("Rainbow") {
    override val parent: ChoiceConfigurable<Choice>
        get() = ModuleRainbowArmor.colorMode
}

private object Static : Choice("Static") {
    override val parent: ChoiceConfigurable<Choice>
        get() = ModuleRainbowArmor.colorMode
}

private object PerPiece : Choice("PerPiece") {
    override val parent: ChoiceConfigurable<Choice>
        get() = ModuleRainbowArmor.colorMode
}