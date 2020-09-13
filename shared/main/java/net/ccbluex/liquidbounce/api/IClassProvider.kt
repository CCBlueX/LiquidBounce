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
import net.ccbluex.liquidbounce.api.minecraft.client.gui.IGuiButton
import net.ccbluex.liquidbounce.api.minecraft.client.gui.IGuiScreen
import net.ccbluex.liquidbounce.api.minecraft.client.gui.IGuiTextField
import net.ccbluex.liquidbounce.api.minecraft.client.multiplayer.IServerData
import net.ccbluex.liquidbounce.api.minecraft.client.multiplayer.IWorldClient
import net.ccbluex.liquidbounce.api.minecraft.client.render.ITessellator
import net.ccbluex.liquidbounce.api.minecraft.client.render.IThreadDownloadImageData
import net.ccbluex.liquidbounce.api.minecraft.client.render.WIImageBuffer
import net.ccbluex.liquidbounce.api.minecraft.client.render.texture.IDynamicTexture
import net.ccbluex.liquidbounce.api.minecraft.client.render.vertex.IVertexFormat
import net.ccbluex.liquidbounce.api.minecraft.client.renderer.IGlStateManager
import net.ccbluex.liquidbounce.api.minecraft.client.renderer.vertex.IVertexBuffer
import net.ccbluex.liquidbounce.api.minecraft.client.settings.IGameSettings
import net.ccbluex.liquidbounce.api.minecraft.enchantments.IEnchantment
import net.ccbluex.liquidbounce.api.minecraft.event.IClickEvent
import net.ccbluex.liquidbounce.api.minecraft.item.IItem
import net.ccbluex.liquidbounce.api.minecraft.item.IItemStack
import net.ccbluex.liquidbounce.api.minecraft.nbt.*
import net.ccbluex.liquidbounce.api.minecraft.network.IPacket
import net.ccbluex.liquidbounce.api.minecraft.network.play.client.*
import net.ccbluex.liquidbounce.api.minecraft.potion.IPotion
import net.ccbluex.liquidbounce.api.minecraft.potion.IPotionEffect
import net.ccbluex.liquidbounce.api.minecraft.potion.PotionType
import net.ccbluex.liquidbounce.api.minecraft.stats.IStatBase
import net.ccbluex.liquidbounce.api.minecraft.util.*
import net.ccbluex.liquidbounce.api.network.IPacketBuffer
import net.ccbluex.liquidbounce.api.util.IWrappedFontRenderer
import net.ccbluex.liquidbounce.api.util.WrappedCreativeTabs
import net.ccbluex.liquidbounce.api.util.WrappedGuiScreen
import net.ccbluex.liquidbounce.api.util.WrappedGuiSlot
import net.ccbluex.liquidbounce.injection.backend.ClassProviderImpl.createCPacketTryUseItem
import net.ccbluex.liquidbounce.injection.backend.PacketImpl
import java.awt.image.BufferedImage
import java.io.File
import java.security.PublicKey
import javax.crypto.SecretKey

interface IClassProvider {
    val tessellatorInstance: ITessellator
    val jsonToNBTInstance: IJsonToNBT

    fun createResourceLocation(resourceName: String): IResourceLocation
    fun createThreadDownloadImageData(cacheFileIn: File?, imageUrlIn: String, textureResourceLocation: IResourceLocation?, imageBufferIn: WIImageBuffer): IThreadDownloadImageData
    fun createPacketBuffer(buffer: ByteBuf): IPacketBuffer
    fun createChatComponentText(text: String): IIChatComponent
    fun createClickEvent(action: IClickEvent.WAction, value: String): IClickEvent
    fun createGuiTextField(id: Int, iFontRenderer: IFontRenderer, x: Int, y: Int, width: Int, height: Int): IGuiTextField
    fun createGuiPasswordField(id: Int, iFontRenderer: IFontRenderer, x: Int, y: Int, width: Int, height: Int): IGuiTextField
    fun createGuiButton(id: Int, x: Int, y: Int, width: Int, height: Int, text: String): IGuiButton
    fun createGuiButton(id: Int, x: Int, y: Int, text: String): IGuiButton
    fun createSession(name: String, uuid: String, accessToken: String, accountType: String): ISession
    fun createDynamicTexture(image: BufferedImage): IDynamicTexture

    fun createItem(): IItem
    fun createItemStack(item: IItem, amount: Int, meta: Int): IItemStack
    fun createItemStack(item: IItem): IItemStack
    fun createItemStack(blockEnum: IBlock): IItemStack
    fun createAxisAlignedBB(minX: Double, minY: Double, minZ: Double, maxX: Double, maxY: Double, maxZ: Double): IAxisAlignedBB
    fun createScaledResolution(mc: IMinecraft): IScaledResolution
    fun createNBTTagCompound(): INBTTagCompound
    fun createNBTTagList(): INBTTagList
    fun createNBTTagString(string: String): INBTTagString
    fun createNBTTagDouble(value: Double): INBTTagDouble
    fun createEntityOtherPlayerMP(world: IWorldClient, GameProfile: GameProfile): IEntityOtherPlayerMP
    fun createPotionEffect(id: Int, time: Int, strength: Int): IPotionEffect
    fun createGuiOptions(parentScreen: IGuiScreen, gameSettings: IGameSettings): IGuiScreen
    fun createGuiSelectWorld(parentScreen: IGuiScreen): IGuiScreen
    fun createGuiMultiplayer(parentScreen: IGuiScreen): IGuiScreen
    fun createGuiModList(parentScreen: IGuiScreen): IGuiScreen
    fun createGuiConnecting(parent: IGuiScreen, mc: IMinecraft, serverData: IServerData): IGuiScreen

    fun createCPacketHeldItemChange(slot: Int): ICPacketHeldItemChange

    /**
     * Only available for 1.8.9, can be replaced with [createCPacketTryUseItem] in later versions
     */
    @SupportsMinecraftVersions(value = [MinecraftVersion.MC_1_8])
    fun createCPacketPlayerBlockPlacement(stack: IItemStack?): ICPacketPlayerBlockPlacement
    fun createCPacketPlayerBlockPlacement(positionIn: WBlockPos, placedBlockDirectionIn: Int, stackIn: IItemStack?, facingXIn: Float, facingYIn: Float, facingZIn: Float): ICPacketPlayerBlockPlacement
    fun createCPacketPlayerPosLook(x: Double, y: Double, z: Double, yaw: Float, pitch: Float, onGround: Boolean): ICPacketPlayerPosLook
    fun createCPacketClientStatus(state: ICPacketClientStatus.WEnumState): ICPacketClientStatus
    fun createCPacketPlayerDigging(wAction: ICPacketPlayerDigging.WAction, pos: WBlockPos, facing: IEnumFacing): IPacket
    fun createCPacketPlayerPosition(x: Double, negativeInfinity: Double, z: Double, onGround: Boolean): ICPacketPlayer
    fun createICPacketResourcePackStatus(hash: String, status: ICPacketResourcePackStatus.WAction): IPacket
    fun createCPacketPlayerLook(yaw: Float, pitch: Float, onGround: Boolean): ICPacketPlayer
    fun createCPacketUseEntity(player: IEntity, wAction: ICPacketUseEntity.WAction): ICPacketUseEntity
    fun createCPacketUseEntity(entity: IEntity, positionVector: WVec3): ICPacketUseEntity
    fun createCPacketCreativeInventoryAction(slot: Int, itemStack: IItemStack): IPacket
    fun createCPacketEntityAction(player: IEntity, wAction: ICPacketEntityAction.WAction): ICPacketEntityAction
    fun createCPacketCustomPayload(channel: String, payload: IPacketBuffer): ICPacketCustomPayload
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
    fun isEntityArmorStand(obj: Any?): Boolean
    fun isEntityTNTPrimed(obj: Any?): Boolean
    fun isEntityBoat(obj: Any?): Boolean
    fun isEntityMinecart(obj: Any?): Boolean
    fun isEntityItem(obj: Any?): Boolean
    fun isEntityArrow(obj: Any?): Boolean
    fun isEntityFallingBlock(obj: Any?): Boolean
    fun isEntityMinecartChest(obj: Any?): Boolean
    fun isEntityShulker(obj: Any?): Boolean

    fun isTileEntityChest(obj: Any?): Boolean
    fun isTileEntityEnderChest(obj: Any?): Boolean
    fun isTileEntityFurnace(obj: Any?): Boolean
    fun isTileEntityDispenser(obj: Any?): Boolean
    fun isTileEntityHopper(obj: Any?): Boolean

    fun isSPacketEntity(obj: Any?): Boolean
    fun isSPacketResourcePackSend(obj: Any?): Boolean
    fun isSPacketPlayerPosLook(obj: Any?): Boolean
    fun isSPacketAnimation(obj: Any?): Boolean
    fun isSPacketEntityVelocity(obj: Any?): Boolean
    fun isSPacketExplosion(obj: Any?): Boolean
    fun isSPacketCloseWindow(obj: Any?): Boolean
    fun isSPacketTabComplete(obj: Any?): Boolean
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
    fun isCPacketPlayerLook(obj: Any?): Boolean
    fun isCPacketCustomPayload(obj: Any?): Boolean
    fun isCPacketHandshake(obj: Any?): Boolean

    fun isItemSword(obj: Any?): Boolean
    fun isItemTool(obj: Any?): Boolean
    fun isItemArmor(obj: Any?): Boolean
    fun isItemPotion(obj: Any?): Boolean
    fun isItemBlock(obj: Any?): Boolean
    fun isItemBow(obj: Any?): Boolean
    fun isItemBucket(obj: Any?): Boolean
    fun isItemFood(obj: Any?): Boolean
    fun isItemBucketMilk(obj: Any?): Boolean
    fun isItemPickaxe(obj: Any?): Boolean
    fun isItemAxe(obj: Any?): Boolean
    fun isItemBed(obj: Any?): Boolean
    fun isItemEnderPearl(obj: Any?): Boolean
    fun isItemEnchantedBook(obj: Any?): Boolean
    fun isItemBoat(obj: Any?): Boolean
    fun isItemMinecart(obj: Any?): Boolean
    fun isItemAppleGold(obj: Any?): Boolean
    fun isItemSnowball(obj: Any?): Boolean
    fun isItemEgg(obj: Any?): Boolean
    fun isItemFishingRod(obj: Any?): Boolean
    fun isItemAir(obj: Any?): Boolean

    fun isBlockAir(obj: Any?): Boolean
    fun isBlockFence(obj: Any?): Boolean
    fun isBlockSnow(obj: Any?): Boolean
    fun isBlockLadder(obj: Any?): Boolean
    fun isBlockVine(obj: Any?): Boolean
    fun isBlockSlime(obj: Any?): Boolean
    fun isBlockSlab(obj: Any?): Boolean
    fun isBlockStairs(obj: Any?): Boolean
    fun isBlockCarpet(obj: Any?): Boolean
    fun isBlockPane(obj: Any?): Boolean
    fun isBlockLiquid(obj: Any?): Boolean
    fun isBlockCactus(obj: Any?): Boolean
    fun isBlockBedrock(obj: Any?): Boolean
    fun isBlockBush(obj: Any?): Boolean

    fun isGuiInventory(obj: Any?): Boolean
    fun isGuiContainer(obj: Any?): Boolean
    fun isGuiGameOver(obj: Any?): Boolean
    fun isGuiChat(obj: Any?): Boolean
    fun isGuiIngameMenu(obj: Any?): Boolean
    fun isGuiChest(obj: Any?): Boolean
    fun isGuiHudDesigner(obj: Any?): Boolean
    fun isClickGui(obj: Any?): Boolean

    fun getPotionEnum(type: PotionType): IPotion
    fun getEnumFacing(type: EnumFacingType): IEnumFacing
    fun getBlockEnum(type: BlockType): IBlock
    fun getMaterialEnum(type: MaterialType): IMaterial
    fun getStatEnum(type: StatType): IStatBase
    fun getItemEnum(type: ItemType): IItem
    fun getEnchantmentEnum(type: EnchantmentType): IEnchantment
    fun getVertexFormatEnum(type: WDefaultVertexFormats): IVertexFormat

    fun wrapFontRenderer(fontRenderer: IWrappedFontRenderer): IFontRenderer
    fun wrapGuiScreen(clickGui: WrappedGuiScreen): IGuiScreen
    fun createSafeVertexBuffer(vertexFormat: IVertexFormat): IVertexBuffer
    fun wrapCreativeTab(name: String, wrappedCreativeTabs: WrappedCreativeTabs)
    fun wrapGuiSlot(wrappedGuiSlot: WrappedGuiSlot, mc: IMinecraft, width: Int, height: Int, top: Int, bottom: Int, slotHeight: Int)
    fun getGlStateManager(): IGlStateManager
    fun createCPacketEncryptionResponse(secretKey: SecretKey, publicKey: PublicKey, VerifyToken: ByteArray): IPacket

    @SupportsMinecraftVersions(value = [MinecraftVersion.MC_1_12])
    fun createCPacketTryUseItem(stack: WEnumHand): PacketImpl<*>
    fun isTileEntityShulkerBox(obj: Any?): Boolean
}