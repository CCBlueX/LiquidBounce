package net.ccbluex.liquidbounce.api.minecraft.tileentity

import net.ccbluex.liquidbounce.api.enums.WEnumChestType

interface ITileEntityChest : ITileEntity
{
    val chestType: WEnumChestType
}
