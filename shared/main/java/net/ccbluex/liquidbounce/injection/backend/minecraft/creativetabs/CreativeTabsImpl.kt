/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend.minecraft.creativetabs

import net.ccbluex.liquidbounce.api.minecraft.creativetabs.ICreativeTabs
import net.minecraft.creativetab.CreativeTabs

class CreativeTabsImpl(val wrapped: CreativeTabs) : ICreativeTabs
{
    override var backgroundImageName: String
        get() = wrapped.backgroundImageName
        set(value)
        {
            wrapped.backgroundImageName = value
        }

    override fun equals(other: Any?): Boolean = other is CreativeTabsImpl && other.wrapped == wrapped
}

fun ICreativeTabs.unwrap(): CreativeTabs = (this as CreativeTabsImpl).wrapped
fun CreativeTabs.wrap(): ICreativeTabs = CreativeTabsImpl(this)
