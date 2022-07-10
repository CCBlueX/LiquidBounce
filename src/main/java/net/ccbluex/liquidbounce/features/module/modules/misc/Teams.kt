/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.minecraft.entity.EntityLivingBase
import net.minecraft.item.ItemArmor
import net.minecraft.item.ItemStack

@ModuleInfo(name = "Teams", description = "Prevents KillAura from attacking team mates.", category = ModuleCategory.MISC)
class Teams : Module()
{
    private val scoreboardValue = BoolValue("ScoreboardTeam", true)
    private val colorValue = BoolValue("Color", true)
    private val gommeSWValue = BoolValue("GommeSW", false)
    private val armorColorValue = BoolValue("ArmorColor", false)
    private val armorColorSensitivityValue = FloatValue("ArmorColorSensitivity", 2.0F, 0.0F, 15.0F)

    /**
     * Check if [entity] is in your own team using scoreboard, name color or team prefix
     */
    fun isInYourTeam(entity: EntityLivingBase): Boolean
    {
        val thePlayer = mc.thePlayer ?: return false

        val playerTeam = thePlayer.team
        val targetTeam = entity.team

        // Scoreboard Team
        if (scoreboardValue.get() && playerTeam != null && targetTeam != null && playerTeam.isSameTeam(targetTeam)) return true

        val displayName = thePlayer.displayName.formattedText
        val entityDisplayName = entity.displayName.formattedText

        // GommeSkywars
        if (gommeSWValue.get())
        {
            val targetName = entityDisplayName.replace("\u00A7r", "")
            val clientName = displayName.replace("\u00A7r", "")

            if (targetName.startsWith("T") && clientName.startsWith("T") && targetName[1].isDigit() && clientName[1].isDigit()) return targetName[1] == clientName[1]
        }

        // Color
        if (colorValue.get())
        {
            val targetName = entityDisplayName.replace("\u00A7r", "")
            val clientName = displayName.replace("\u00A7r", "")

            return targetName.startsWith("\u00A7${clientName[1]}")
        }

        if (armorColorValue.get())
        {
            val getColor = { it: ItemStack -> (it.item!! as ItemArmor).getColor(it) }
            val colorSensitivity = armorColorSensitivityValue.get()

            if ((1..4).asSequence().map { thePlayer.getEquipmentInSlot(it) to entity.getEquipmentInSlot(it) }.filter { it.first?.item is ItemArmor && it.second?.item is ItemArmor }.map { getColor(it.first!!) to getColor(it.second!!) }.filter { (playerColor, targetColor) -> playerColor != -1 && targetColor != -1 && playerColor != 0xA06540 && targetColor != 0xA06540 /* Default leather armor color */ }.any { ColorUtils.compareColor(it.first, it.second) <= colorSensitivity }) return true
        }

        return false
    }
}
