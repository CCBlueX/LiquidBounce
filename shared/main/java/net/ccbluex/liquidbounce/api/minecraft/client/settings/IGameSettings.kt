/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.api.minecraft.client.settings

import net.ccbluex.liquidbounce.api.minecraft.client.entity.player.WEnumPlayerModelParts

interface IGameSettings {
    var entityShadows: Boolean
    var gammaSetting: Float
    val modelParts: Set<WEnumPlayerModelParts>
    val mouseSensitivity: Float

    val keyBindAttack: IKeyBinding
    val keyBindUseItem: IKeyBinding
    val keyBindJump: IKeyBinding
    val keyBindSneak: IKeyBinding
    val keyBindForward: IKeyBinding
    val keyBindBack: IKeyBinding
    val keyBindRight: IKeyBinding
    val keyBindLeft: IKeyBinding
    val keyBindSprint: IKeyBinding

    fun isKeyDown(key: IKeyBinding): Boolean
    fun setModelPartEnabled(modelParts: WEnumPlayerModelParts, enabled: Boolean)
}