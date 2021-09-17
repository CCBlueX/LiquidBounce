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
	/* Block */
	fun getBlockById(id: Int): IBlock?
	fun getIdFromBlock(block: IBlock): Int
	fun getBlockFromName(name: String): IBlock?
	fun getBlockRegistryKeys(): Collection<IResourceLocation>
	fun isBlockEqualTo(block1: IBlock?, block2: IBlock?): Boolean

	/* Item */
	fun getModifierForCreature(heldItem: IItemStack?, creatureAttribute: IEnumCreatureAttribute): Float
	fun getItemRegistryKeys(): Collection<IResourceLocation>
	fun getObjectFromItemRegistry(res: IResourceLocation): IItem?
	fun getItemByName(name: String): IItem?
	fun getIdFromItem(item: IItem): Int

	/* Encahntment */
	fun getEnchantmentByLocation(location: String): IEnchantment?
	fun getEnchantmentById(enchantID: Int): IEnchantment?
	fun getEnchantments(): Collection<IResourceLocation>
	fun getEnchantments(item: IItemStack): Map<Int, Int>
	fun getEnchantmentLevel(enchId: Int, stack: IItemStack): Int

	/* Render-related */
	fun enableStandardItemLighting()
	fun enableGUIStandardItemLighting()
	fun disableStandardItemLighting()
	fun setActiveTextureLightMapTexUnit()
	fun setActiveTextureDefaultTexUnit()
	fun getLightMapTexUnit(): Int
	fun setLightmapTextureCoords(target: Int, x: Float, y: Float)
	fun renderTileEntity(tileEntity: ITileEntity, partialTicks: Float, destroyStage: Int)
	fun disableFastRender()

	/* Translation */
	fun formatI18n(key: String, vararg values: String): String
	fun translateToLocal(key: String): String

	/* Potion */
	fun getPotionById(potionID: Int): IPotion
	fun getLiquidColor(potionDamage: Int, bypassCache: Boolean): Int

	/* Session */
	fun sessionServiceJoinServer(profile: GameProfile, token: String, sessionHash: String)

	/* Scoreboard */
	fun scoreboardFormatPlayerName(scorePlayerTeam: ITeam?, playerName: String): String

	/* JSON */
	fun jsonToComponent(toString: String): IIChatComponent

	/* Facing */
	fun getHorizontalFacing(yaw: Float): IEnumFacing

	/* Delegate to MathHelper (it's faster and compatible with BetterFps) */
	fun cos(radians: Float): Float
	fun sin(radians: Float): Float
}
