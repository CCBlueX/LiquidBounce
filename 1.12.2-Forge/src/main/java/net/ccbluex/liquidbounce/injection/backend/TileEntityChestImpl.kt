package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.enums.WEnumChestType
import net.ccbluex.liquidbounce.api.minecraft.tileentity.ITileEntityChest
import net.minecraft.tileentity.TileEntityChest

class TileEntityChestImpl(override val wrapped: TileEntityChest) : TileEntityImpl(wrapped), ITileEntityChest
{
	override val chestType: WEnumChestType
		get() = WEnumChestType.values()[wrapped.chestType.ordinal]
}
