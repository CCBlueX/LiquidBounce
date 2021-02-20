/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.api

import com.mojang.authlib.GameProfile
import net.ccbluex.liquidbounce.api.minecraft.client.block.IBlock
import net.ccbluex.liquidbounce.api.minecraft.enchantments.IEnchantment
import net.ccbluex.liquidbounce.api.minecraft.entity.IEnumCreatureAttribute
import net.ccbluex.liquidbounce.api.minecraft.item.IItem
import net.ccbluex.liquidbounce.api.minecraft.item.IItemStack
import net.ccbluex.liquidbounce.api.minecraft.potion.IPotion
import net.ccbluex.liquidbounce.api.minecraft.scoreboard.ITeam
import net.ccbluex.liquidbounce.api.minecraft.tileentity.ITileEntity
import net.ccbluex.liquidbounce.api.minecraft.util.IEnumFacing
import net.ccbluex.liquidbounce.api.minecraft.util.IIChatComponent
import net.ccbluex.liquidbounce.api.minecraft.util.IResourceLocation

interface IExtractedFunctions
{
	fun getModifierForCreature(heldItem: IItemStack?, creatureAttribute: IEnumCreatureAttribute): Float
	fun getObjectFromItemRegistry(res: IResourceLocation): IItem?
	fun renderTileEntity(tileEntity: ITileEntity, partialTicks: Float, destroyStage: Int)

	fun getBlockById(id: Int): IBlock?
	fun getIdFromBlock(block: IBlock): Int
	fun getBlockFromName(name: String): IBlock?
	fun getBlockRegistryKeys(): Collection<IResourceLocation>

	fun getItemRegistryKeys(): Collection<IResourceLocation>
	fun getItemByName(name: String): IItem?
	fun getIdFromItem(item: IItem): Int

	fun getEnchantmentByLocation(location: String): IEnchantment?
	fun getEnchantmentById(enchantID: Int): IEnchantment?
	fun getEnchantments(): Collection<IResourceLocation>
	fun getEnchantments(item: IItemStack): Map<Int, Int>
	fun getEnchantmentLevel(enchId: Int, stack: IItemStack): Int

	fun enableStandardItemLighting()

	fun disableStandardItemLighting()
	fun disableFastRender()
	fun setActiveTextureLightMapTexUnit()
	fun setActiveTextureDefaultTexUnit()
	fun getLightMapTexUnit(): Int
	fun setLightmapTextureCoords(target: Int, j: Float, k: Float)

	fun formatI18n(key: String, vararg values: String): String
	fun sessionServiceJoinServer(profile: GameProfile, token: String, sessionHash: String)
	fun getPotionById(potionID: Int): IPotion
	fun scoreboardFormatPlayerName(scorePlayerTeam: ITeam?, playerName: String): String
	fun jsonToComponent(toString: String): IIChatComponent
	fun getHorizontalFacing(yaw: Float): IEnumFacing
	fun translateToLocal(key: String): String
	fun isBlockEqualTo(block1: IBlock?, block2: IBlock?): Boolean

	// MathHelper's sin and cos algorithm is faster than StrictMath's (Because MathHelper uses better algorithm and it is compatible with BetterFps mod)
	fun cos(radians: Float): Float
	fun sin(radians: Float): Float
}
