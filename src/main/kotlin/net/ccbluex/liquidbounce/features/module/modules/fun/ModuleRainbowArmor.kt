package net.ccbluex.liquidbounce.features.module.modules.`fun`

import net.ccbluex.liquidbounce.config.types.Choice
import net.ccbluex.liquidbounce.config.types.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.minecraft.item.ArmorItem
import net.minecraft.item.ItemStack

object ModuleRainbowArmor : ClientModule("RainbowArmor", Category.FUN) {
    
    private val colorMode = choices("ColorMode", Rainbow, arrayOf(Rainbow, Static))
    private val speed by float("Speed", 1f, 0.1f..5f)
    private val saturation by float("Saturation", 0.7f, 0f..1f)
    private val brightness by float("Brightness", 1f, 0f..1f)
    
    // Armor Piece Selection
    private val helmet by boolean("Helmet", true)
    private val chestplate by boolean("Chestplate", true)
    private val leggings by boolean("Leggings", true)
    private val boots by boolean("Boots", true)
    
    private var currentTick = 0f
    
    val repeatable = tickHandler {
        currentTick += speed
        updateArmorColors()
    }
    
    private fun updateArmorColors() {
        val player = mc.player ?: return
        
        player.armorItems.forEachIndexed { index, itemStack ->
            if (itemStack.item is ArmorItem) {
                val shouldColor = when(index) {
                    3 -> helmet
                    2 -> chestplate
                    1 -> leggings
                    0 -> boots
                    else -> false
                }
                
                if (shouldColor) {
                    val color = when(colorMode.activeChoice) {
                        is Rainbow -> ColorUtils.rainbow(index * 100, saturation, brightness)
                        else -> ColorUtils.getRed() // Default red color
                    }
                    
                    applyArmorColor(itemStack, color)
                }
            }
        }
    }
    
    private fun applyArmorColor(itemStack: ItemStack, color: Int) {
        // Use NBT data to store color information
        val nbt = itemStack.orCreateNbt
        nbt.putInt("display", color)
    }
    
    private object Rainbow : Choice("Rainbow") {
        override val parent: ChoiceConfigurable<Choice>
            get() = colorMode
    }
    
    private object Static : Choice("Static") {
        override val parent: ChoiceConfigurable<Choice>
            get() = colorMode
    }
}