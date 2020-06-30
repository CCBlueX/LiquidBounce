/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.api

import com.mojang.authlib.GameProfile
import io.netty.buffer.ByteBuf
import net.ccbluex.liquidbounce.api.enums.*
import net.ccbluex.liquidbounce.api.minecraft.block.material.IMaterial
import net.ccbluex.liquidbounce.api.minecraft.client.IMinecraft
import net.ccbluex.liquidbounce.api.minecraft.client.block.IBlock
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntity
import net.ccbluex.liquidbounce.api.minecraft.client.entity.player.IEntityOtherPlayerMP
import net.ccbluex.liquidbounce.api.minecraft.client.gui.IFontRenderer
import net.ccbluex.liquidbounce.api.minecraft.client.gui.IGuiScreen
import net.ccbluex.liquidbounce.api.minecraft.client.multiplayer.IWorldClient
import net.ccbluex.liquidbounce.api.minecraft.client.render.ITessellator
import net.ccbluex.liquidbounce.api.minecraft.client.render.IThreadDownloadImageData
import net.ccbluex.liquidbounce.api.minecraft.client.render.WIImageBuffer
import net.ccbluex.liquidbounce.api.minecraft.client.render.vertex.IVertexFormat
import net.ccbluex.liquidbounce.api.minecraft.enchantments.IEnchantment
import net.ccbluex.liquidbounce.api.minecraft.event.IClickEvent
import net.ccbluex.liquidbounce.api.minecraft.item.IItem
import net.ccbluex.liquidbounce.api.minecraft.item.IItemStack
import net.ccbluex.liquidbounce.api.minecraft.nbt.IJsonToNBT
import net.ccbluex.liquidbounce.api.minecraft.nbt.INBTTagCompound
import net.ccbluex.liquidbounce.api.minecraft.nbt.INBTTagList
import net.ccbluex.liquidbounce.api.minecraft.nbt.INBTTagString
import net.ccbluex.liquidbounce.api.minecraft.network.IPacket
import net.ccbluex.liquidbounce.api.minecraft.network.play.client.*
import net.ccbluex.liquidbounce.api.minecraft.potion.IPotion
import net.ccbluex.liquidbounce.api.minecraft.potion.PotionType
import net.ccbluex.liquidbounce.api.minecraft.stats.IStatList
import net.ccbluex.liquidbounce.api.minecraft.util.*
import net.ccbluex.liquidbounce.api.network.IPacketBuffer
import net.ccbluex.liquidbounce.api.util.IWrappedFontRenderer
import net.ccbluex.liquidbounce.api.util.WrappedGuiScreen
import java.io.File

interface IClassProvider {
    val tessellatorInstance: ITessellator
    val jsonToNBTInstance: IJsonToNBT

    fun createResourceLocation(resourceName: String): IResourceLocation
    fun createThreadDownloadImageData(cacheFileIn: File?, imageUrlIn: String, textureResourceLocation: IResourceLocation?, imageBufferIn: WIImageBuffer): IThreadDownloadImageData
    fun createPacketBuffer(buffer: ByteBuf): IPacketBuffer
    fun createChatComponentText(text: String): IIChatComponent
    fun createClickEvent(action: IClickEvent.WAction, value: String): IClickEvent

    fun createItem(): IItem
    fun createItemStack(item: IItem, amount: Int, meta: Int): IItemStack
    fun createItemStack(item: IItem): IItemStack
    fun createAxisAlignedBB(minX: Double, minY: Double, minZ: Double, maxX: Double, maxY: Double, maxZ: Double): IAxisAlignedBB
    fun createScaledResolution(mc: IMinecraft): IScaledResolution
    fun createNBTTagCompound(): INBTTagCompound
    fun createNBTTagList(): INBTTagList
    fun createNBTTagString(string: String): INBTTagString
    fun createEntityOtherPlayerMP(world: IWorldClient, GameProfile: GameProfile): IEntityOtherPlayerMP

    fun createCPacketHeldItemChange(slot: Int): ICPacketHeldItemChange
    fun createCPacketPlayerBlockPlacement(stack: IItemStack?): ICPacketPlayerBlockPlacement
    fun createCPacketPlayerBlockPlacement(positionIn: WBlockPos, placedBlockDirectionIn: Int, stackIn: IItemStack?, facingXIn: Float, facingYIn: Float, facingZIn: Float): ICPacketPlayerBlockPlacement
    fun createCPacketPlayerPosLook(x: Double, y: Double, z: Double, yaw: Float, pitch: Float, onGround: Boolean): ICPacketPlayerPosLook
    fun createCPacketClientStatus(state: ICPacketClientStatus.WEnumState): ICPacketClientStatus
    fun createCPacketPlayerDigging(wAction: ICPacketPlayerDigging.WAction, pos: WBlockPos, facing: WEnumFacing): ICPacketPlayerDigging
    fun createCPacketPlayerPosition(x: Double, negativeInfinity: Double, z: Double, onGround: Boolean): ICPacketPlayerPosition
    fun createICPacketResourcePackStatus(hash: String, status: ICPacketResourcePackStatus.WAction): ICPacketResourcePackStatus
    fun createCPacketPlayerLook(yaw: Float, pitch: Float, onGround: Boolean): ICPacketPlayerLook
    fun createCPacketUseEntity(player: IEntity, wAction: ICPacketUseEntity.WAction): ICPacketUseEntity
    fun createCPacketUseEntity(entity: IEntity, positionVector: WVec3): ICPacketUseEntity
    fun createCPacketEntityAction(player: IEntity, wAction: ICPacketEntityAction.WAction): ICPacketEntityAction
    fun createCPacketCustomPayload(channel: String, payload: IPacketBuffer): IPacket
    fun createCPacketCloseWindow(windowId: Int): ICPacketCloseWindow
    fun createCPacketCloseWindow(): ICPacketCloseWindow
    fun createCPacketPlayer(onGround: Boolean): ICPacketPlayer
    fun createCPacketTabComplete(text: String): IPacket
    fun createCPacketAnimation(): ICPacketAnimation
    fun createCPacketKeepAlive(): ICPacketKeepAlive

    fun isEntityAnimal(obj: Any?): Boolean
    fun isEntitySquid(obj: Any?): Boolean
    fun isEntityBat(obj: Any?): Boolean
    fun isEntityGolem(obj: Any?): Boolean
    fun isEntityMob(obj: Any?): Boolean
    fun isEntityVillager(obj: Any?): Boolean
    fun isEntitySlime(obj: Any?): Boolean
    fun isEntityGhast(obj: Any?): Boolean
    fun isEntityDragon(obj: Any?): Boolean
    fun isEntityLivingBase(obj: Any?): Boolean
    fun isEntityPlayer(obj: Any?): Boolean
    fun isEntityArmorStand(it: Any?): Boolean
    fun isEntityTNTPrimed(it: Any?): Boolean
    fun isEntityBoat(it: Any?): Boolean
    fun isEntityMinecart(it: Any?): Boolean

    fun isSPacketEntity(obj: Any?): Boolean
    fun isSPacketResourcePackSend(obj: Any?): Boolean
    fun isSPacketPlayerPosLook(obj: Any?): Boolean
    fun isSPacketAnimation(obj: Any?): Boolean
    fun isSPacketEntityVelocity(obj: Any?): Boolean
    fun isSPacketExplosion(packet: Any?): Boolean
    fun isSPacketCloseWindow(packet: Any?): Boolean
    fun isSPacketTabComplete(packet: Any?): Boolean
    fun isCPacketPlayer(obj: Any?): Boolean
    fun isCPacketPlayerBlockPlacement(obj: Any?): Boolean
    fun isCPacketUseEntity(obj: Any?): Boolean
    fun isCPacketCloseWindow(obj: Any?): Boolean
    fun isCPacketChatMessage(obj: Any?): Boolean
    fun isCPacketKeepAlive(obj: Any?): Boolean
    fun isCPacketPlayerPosition(obj: Any?): Boolean
    fun isCPacketPlayerPosLook(obj: Any?): Boolean
    fun isCPacketClientStatus(obj: Any?): Boolean
    fun isCPacketAnimation(obj: Any?): Boolean
    fun isCPacketEntityAction(obj: Any?): Boolean
    fun isSPacketWindowItems(obj: Any?): Boolean
    fun isCPacketHeldItemChange(obj: Any?): Boolean

    fun isItemSword(item: Any?): Boolean
    fun isItemTool(item: Any?): Boolean
    fun isItemArmor(obj: Any?): Boolean
    fun isItemPotion(item: Any?): Boolean
    fun isItemBlock(item: Any?): Boolean
    fun isItemBow(item: Any?): Boolean
    fun isItemBucket(item: Any?): Boolean
    fun isItemFood(item: Any?): Boolean
    fun isItemBucketMilk(item: Any?): Boolean
    fun isItemPickaxe(obj: Any?): Boolean
    fun isItemAxe(obj: Any?): Boolean
    fun isItemBed(obj: Any?): Boolean
    fun isItemEnderPearl(obj: Any?): Boolean
    fun isItemEnchantedBook(obj: Any?): Boolean
    fun isItemBoat(obj: Any?): Boolean
    fun isItemMinecart(obj: Any?): Boolean
    fun isItemAppleGold(obj: Any?): Boolean

    fun isBlockAir(item: Any?): Boolean
    fun isBlockFence(item: Any?): Boolean
    fun isBlockSnow(item: Any?): Boolean
    fun isBlockLadder(item: Any?): Boolean
    fun isBlockVine(item: Any?): Boolean
    fun isBlockSlime(item: Any?): Boolean
    fun isBlockSlab(item: Any?): Boolean
    fun isBlockStairs(item: Any?): Boolean
    fun isBlockCarpet(obj: Any?): Boolean
    fun isBlockPane(obj: Any?): Boolean
    fun isBlockLiquid(obj: Any?): Boolean
    fun isBlockCactus(obj: Any?): Boolean
    fun isBlockBedrock(obj: Any?): Boolean
    fun isItemFishingRod(obj: Any?): Boolean

    fun isGuiInventory(obj: Any?): Boolean
    fun isGuiContainer(obj: Any?): Boolean
    fun isGuiGameOver(obj: Any?): Boolean
    fun isGuiChat(obj: Any?): Boolean
    fun isGuiIngameMenu(obj: Any?): Boolean
    fun isGuiChest(obj: Any?): Boolean
    fun isClickGui(obj: Any?): Boolean

    fun isFontRenderer(item: Any?): Boolean

    fun getPotionEnum(type: PotionType): IPotion
    fun getBlockEnum(type: BlockType): IBlock
    fun getMaterialEnum(type: MaterialType): IMaterial
    fun getStatEnum(type: StatType): IStatList
    fun getItemEnum(type: ItemType): IItem
    fun getEnchantmentEnum(type: EnchantmentType): IEnchantment
    fun getVertexFormatEnum(type: WDefaultVertexFormats): IVertexFormat

    fun wrapFontRenderer(fontRenderer: IWrappedFontRenderer): IFontRenderer
    fun wrapGuiScreen(clickGui: WrappedGuiScreen): IGuiScreen
}