/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.client.entity.player.WEnumPlayerModelParts
import net.ccbluex.liquidbounce.api.minecraft.client.settings.IGameSettings
import net.ccbluex.liquidbounce.api.minecraft.client.settings.IKeyBinding
import net.ccbluex.liquidbounce.api.util.WrappedSet
import net.ccbluex.liquidbounce.injection.backend.utils.unwrap
import net.ccbluex.liquidbounce.injection.backend.utils.wrap
import net.minecraft.client.settings.GameSettings
import net.minecraft.entity.player.EnumPlayerModelParts

class GameSettingsImpl(val wrapped: GameSettings) : IGameSettings {
    override var entityShadows: Boolean
        get() = wrapped.entityShadows
        set(value) {
            wrapped.entityShadows = value
        }
    override var gammaSetting: Float
        get() = wrapped.gammaSetting
        set(value) {
            wrapped.gammaSetting = value
        }
    override val modelParts: Set<WEnumPlayerModelParts>
        get() = WrappedSet(wrapped.modelParts, WEnumPlayerModelParts::unwrap, EnumPlayerModelParts::wrap)
    override val mouseSensitivity: Float
        get() = wrapped.mouseSensitivity
    override val keyBindAttack: IKeyBinding
        get() = wrapped.keyBindAttack.wrap()
    override val keyBindUseItem: IKeyBinding
        get() = wrapped.keyBindUseItem.wrap()
    override val keyBindJump: IKeyBinding
        get() = wrapped.keyBindJump.wrap()
    override val keyBindSneak: IKeyBinding
        get() = wrapped.keyBindSneak.wrap()
    override val keyBindForward: IKeyBinding
        get() = wrapped.keyBindForward.wrap()
    override val keyBindBack: IKeyBinding
        get() = wrapped.keyBindBack.wrap()
    override val keyBindRight: IKeyBinding
        get() = wrapped.keyBindRight.wrap()
    override val keyBindLeft: IKeyBinding
        get() = wrapped.keyBindLeft.wrap()
    override val keyBindSprint: IKeyBinding
        get() = wrapped.keyBindSprint.wrap()

    override fun isKeyDown(key: IKeyBinding): Boolean = GameSettings.isKeyDown(key.unwrap())

    override fun setModelPartEnabled(modelParts: WEnumPlayerModelParts, enabled: Boolean) = wrapped.setModelPartEnabled(modelParts.unwrap(), enabled)

    override fun equals(other: Any?): Boolean {
        return other is GameSettingsImpl && other.wrapped == this.wrapped
    }
}

inline fun IGameSettings.unwrap(): GameSettings = (this as GameSettingsImpl).wrapped
inline fun GameSettings.wrap(): IGameSettings = GameSettingsImpl(this)