package net.ccbluex.liquidbounce.api.minecraft.tileentity

import net.ccbluex.liquidbounce.api.minecraft.util.WEnumChestType

interface ITileEntityChest : ITileEntity
{
	val chestType: WEnumChestType
}
