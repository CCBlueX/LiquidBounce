package net.ccbluex.liquidbounce.injection.backend.minecraft.tileentity

import net.ccbluex.liquidbounce.api.minecraft.tileentity.ITileEntityChest
import net.ccbluex.liquidbounce.api.enums.WEnumChestType
import net.minecraft.tileentity.TileEntityChest

class TileEntityChestImpl(override val wrapped: TileEntityChest) : TileEntityImpl(wrapped), ITileEntityChest
{
	override val chestType: WEnumChestType
		get() = WEnumChestType.values()[wrapped.chestType]
}
