/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend

import com.mojang.authlib.GameProfile
import io.netty.buffer.ByteBuf
import net.ccbluex.liquidbounce.api.IClassProvider
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
import net.ccbluex.liquidbounce.injection.backend.utils.*
import net.ccbluex.liquidbounce.ui.client.clickgui.ClickGui
import net.ccbluex.liquidbounce.ui.client.hud.designer.GuiHudDesigner
import net.minecraft.block.*
import net.minecraft.block.material.Material
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.client.gui.*
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.client.gui.inventory.GuiInventory
import net.minecraft.client.multiplayer.GuiConnecting
import net.minecraft.client.renderer.IImageBuffer
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.ThreadDownloadImageData
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.boss.EntityDragon
import net.minecraft.entity.item.*
import net.minecraft.entity.monster.*
import net.minecraft.entity.passive.EntityAnimal
import net.minecraft.entity.passive.EntityBat
import net.minecraft.entity.passive.EntitySquid
import net.minecraft.entity.passive.EntityVillager
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.projectile.EntityArrow
import net.minecraft.init.Blocks
import net.minecraft.init.Enchantments
import net.minecraft.init.Items
import net.minecraft.init.MobEffects
import net.minecraft.item.*
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.nbt.NBTTagDouble
import net.minecraft.nbt.NBTTagList
import net.minecraft.nbt.NBTTagString
import net.minecraft.network.PacketBuffer
import net.minecraft.network.handshake.client.C00Handshake
import net.minecraft.network.login.client.CPacketEncryptionResponse
import net.minecraft.network.play.client.*
import net.minecraft.network.play.server.*
import net.minecraft.potion.Potion
import net.minecraft.potion.PotionEffect
import net.minecraft.stats.StatList
import net.minecraft.tileentity.*
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.ResourceLocation
import net.minecraft.util.Session
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.text.TextComponentString
import net.minecraft.util.text.event.ClickEvent
import net.minecraftforge.fml.client.GuiModList
import java.awt.image.BufferedImage
import java.io.File
import java.security.PublicKey
import javax.crypto.SecretKey

object ClassProviderImpl : IClassProvider {
    override val tessellatorInstance: ITessellator
        get() = TessellatorImpl(Tessellator.getInstance())
    override val jsonToNBTInstance: IJsonToNBT
        get() = JsonToNBTImpl

    override fun createResourceLocation(resourceName: String): IResourceLocation = ResourceLocationImpl(ResourceLocation(resourceName))

    override fun createThreadDownloadImageData(cacheFileIn: File?, imageUrlIn: String, textureResourceLocation: IResourceLocation?, imageBufferIn: WIImageBuffer): IThreadDownloadImageData {
        return ThreadDownloadImageDataImpl(ThreadDownloadImageData(cacheFileIn, imageUrlIn, textureResourceLocation?.unwrap(), object : IImageBuffer {
            override fun parseUserSkin(image: BufferedImage?): BufferedImage? = imageBufferIn.parseUserSkin(image)
            override fun skinAvailable() = imageBufferIn.skinAvailable()
        }))
    }

    override fun createPacketBuffer(buffer: ByteBuf): IPacketBuffer = PacketBufferImpl(PacketBuffer(buffer))

    override fun createChatComponentText(text: String): IIChatComponent = IChatComponentImpl(TextComponentString(text))

    override fun createClickEvent(action: IClickEvent.WAction, value: String): IClickEvent = ClickEventImpl(ClickEvent(action.unwrap(), value))

    override fun createGuiTextField(id: Int, iFontRenderer: IFontRenderer, x: Int, y: Int, width: Int, height: Int): IGuiTextField = GuiTextFieldImpl(GuiTextField(id, iFontRenderer.unwrap(), x, y, width, height))

    override fun createGuiPasswordField(id: Int, iFontRenderer: IFontRenderer, x: Int, y: Int, width: Int, height: Int): IGuiTextField = GuiTextFieldImpl(GuiPasswordField(id, iFontRenderer.unwrap(), x, y, width, height))

    override fun createGuiButton(id: Int, x: Int, y: Int, width: Int, height: Int, text: String): IGuiButton = GuiButtonImpl(GuiButton(id, x, y, width, height, text))

    override fun createGuiButton(id: Int, x: Int, y: Int, text: String): IGuiButton = GuiButtonImpl(GuiButton(id, x, y, text))

    override fun createSession(name: String, uuid: String, accessToken: String, accountType: String): ISession = SessionImpl(Session(name, uuid, accessToken, accountType))

    override fun createDynamicTexture(image: BufferedImage): IDynamicTexture = DynamicTextureImpl(DynamicTexture(image))

    override fun createItem(): IItem = ItemImpl(Item())

    override fun createItemStack(item: IItem, amount: Int, meta: Int): IItemStack = ItemStackImpl(ItemStack(item.unwrap(), amount, meta))

    override fun createItemStack(item: IItem): IItemStack = ItemStackImpl(ItemStack(item.unwrap()))

    override fun createItemStack(blockEnum: IBlock): IItemStack = ItemStackImpl(ItemStack(blockEnum.unwrap()))

    override fun createAxisAlignedBB(minX: Double, minY: Double, minZ: Double, maxX: Double, maxY: Double, maxZ: Double): IAxisAlignedBB = AxisAlignedBBImpl(AxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ))

    override fun createScaledResolution(mc: IMinecraft): IScaledResolution = ScaledResolutionImpl(ScaledResolution(mc.unwrap()))

    override fun createNBTTagCompound(): INBTTagCompound = NBTTagCompoundImpl(NBTTagCompound())

    override fun createNBTTagList(): INBTTagList = NBTTagListImpl(NBTTagList())

    override fun createNBTTagString(string: String): INBTTagString = NBTTagStringImpl(NBTTagString(string))

    override fun createNBTTagDouble(value: Double): INBTTagDouble = NBTTagDoubleImpl(NBTTagDouble(value))

    override fun createEntityOtherPlayerMP(world: IWorldClient, gameProfile: GameProfile): IEntityOtherPlayerMP = EntityOtherPlayerMPImpl(EntityOtherPlayerMP(world.unwrap(), gameProfile))

    override fun createPotionEffect(id: Int, time: Int, strength: Int): IPotionEffect = PotionEffectImpl(PotionEffect(Potion.getPotionById(id), time, strength))

    override fun createGuiOptions(parentScreen: IGuiScreen, gameSettings: IGameSettings): IGuiScreen = GuiScreenImpl(GuiOptions(parentScreen.unwrap(), gameSettings.unwrap()))

    override fun createGuiSelectWorld(parentScreen: IGuiScreen): IGuiScreen = GuiScreenImpl(GuiWorldSelection(parentScreen.unwrap()))

    override fun createGuiMultiplayer(parentScreen: IGuiScreen): IGuiScreen = GuiScreenImpl(GuiMultiplayer(parentScreen.unwrap()))

    override fun createGuiModList(parentScreen: IGuiScreen): IGuiScreen = GuiScreenImpl(GuiModList(parentScreen.unwrap()))
    override fun createGuiConnecting(parent: IGuiScreen, mc: IMinecraft, serverData: IServerData): IGuiScreen = GuiScreenImpl(GuiConnecting(parent.unwrap(), mc.unwrap(), serverData.unwrap()))

    override fun createCPacketHeldItemChange(slot: Int): ICPacketHeldItemChange = CPacketHeldItemChangeImpl(CPacketHeldItemChange(slot))

    override fun createCPacketPlayerBlockPlacement(stack: IItemStack?): ICPacketPlayerBlockPlacement = Backend.BACKEND_UNSUPPORTED()

    override fun createCPacketTryUseItem(hand: WEnumHand): PacketImpl<*> = PacketImpl(CPacketPlayerTryUseItem(hand.unwrap()))

    override fun createCPacketPlayerBlockPlacement(positionIn: WBlockPos, placedBlockDirectionIn: Int, stackIn: IItemStack?, facingXIn: Float, facingYIn: Float, facingZIn: Float): ICPacketPlayerBlockPlacement = CPacketPlayerBlockPlacementImpl(CPacketPlayerTryUseItemOnBlock(positionIn.unwrap(), EnumFacing.values()[placedBlockDirectionIn], EnumHand.MAIN_HAND, facingXIn, facingYIn, facingZIn))

    override fun createCPacketPlayerPosLook(x: Double, y: Double, z: Double, yaw: Float, pitch: Float, onGround: Boolean): ICPacketPlayerPosLook = CPacketPlayerPosLookImpl(CPacketPlayer.PositionRotation(x, y, z, yaw, pitch, onGround))

    override fun createCPacketClientStatus(state: ICPacketClientStatus.WEnumState): ICPacketClientStatus = CPacketClientStatusImpl(CPacketClientStatus(state.unwrap()))

    override fun createCPacketPlayerDigging(wAction: ICPacketPlayerDigging.WAction, pos: WBlockPos, facing: IEnumFacing): IPacket = PacketImpl(CPacketPlayerDigging(wAction.unwrap(), pos.unwrap(), facing.unwrap()))

    override fun createCPacketPlayerPosition(x: Double, y: Double, z: Double, onGround: Boolean): ICPacketPlayer = CPacketPlayerImpl(CPacketPlayer.Position(x, y, z, onGround))

    override fun createICPacketResourcePackStatus(hash: String, status: ICPacketResourcePackStatus.WAction): IPacket = PacketImpl(CPacketResourcePackStatus(status.unwrap()))

    override fun createCPacketPlayerLook(yaw: Float, pitch: Float, onGround: Boolean): ICPacketPlayer = CPacketPlayerImpl(CPacketPlayer.Rotation(yaw, pitch, onGround))

    override fun createCPacketUseEntity(player: IEntity, wAction: ICPacketUseEntity.WAction): ICPacketUseEntity {
        return when (wAction) {
            ICPacketUseEntity.WAction.INTERACT -> CPacketUseEntityImpl(CPacketUseEntity(player.unwrap(), EnumHand.MAIN_HAND))
            ICPacketUseEntity.WAction.ATTACK -> CPacketUseEntityImpl(CPacketUseEntity(player.unwrap()))
            ICPacketUseEntity.WAction.INTERACT_AT -> Backend.BACKEND_UNSUPPORTED()
        }
    }

    override fun createCPacketUseEntity(entity: IEntity, positionVector: WVec3): ICPacketUseEntity = CPacketUseEntityImpl(CPacketUseEntity(entity.unwrap(), EnumHand.MAIN_HAND, positionVector.unwrap()))

    override fun createCPacketCreativeInventoryAction(slot: Int, itemStack: IItemStack): IPacket = PacketImpl(CPacketCreativeInventoryAction(slot, itemStack.unwrap()))

    override fun createCPacketEntityAction(player: IEntity, wAction: ICPacketEntityAction.WAction): ICPacketEntityAction = CPacketEntityActionImpl(CPacketEntityAction(player.unwrap(), wAction.unwrap()))

    override fun createCPacketCustomPayload(channel: String, payload: IPacketBuffer): ICPacketCustomPayload = CPacketCustomPayloadImpl(CPacketCustomPayload(channel, payload.unwrap()))

    override fun createCPacketCloseWindow(windowId: Int): ICPacketCloseWindow = CPacketCloseWindowImpl(CPacketCloseWindow(windowId))

    override fun createCPacketCloseWindow(): ICPacketCloseWindow = CPacketCloseWindowImpl(CPacketCloseWindow())

    override fun createCPacketPlayer(onGround: Boolean): ICPacketPlayer = CPacketPlayerImpl(CPacketPlayer(onGround))

    override fun createCPacketTabComplete(text: String): IPacket = PacketImpl(CPacketTabComplete(text, null, false))

    override fun createCPacketAnimation(): ICPacketAnimation = CPacketAnimationImpl(CPacketAnimation(EnumHand.MAIN_HAND))

    override fun createCPacketKeepAlive(): ICPacketKeepAlive = CPacketKeepAliveImpl(CPacketKeepAlive())

    override fun isEntityAnimal(obj: Any?): Boolean = obj is EntityImpl<*> && obj.wrapped is EntityAnimal

    override fun isEntitySquid(obj: Any?): Boolean = obj is EntityImpl<*> && obj.wrapped is EntitySquid

    override fun isEntityBat(obj: Any?): Boolean = obj is EntityImpl<*> && obj.wrapped is EntityBat

    override fun isEntityGolem(obj: Any?): Boolean = obj is EntityImpl<*> && obj.wrapped is EntityGolem

    override fun isEntityMob(obj: Any?): Boolean = obj is EntityImpl<*> && obj.wrapped is EntityMob

    override fun isEntityVillager(obj: Any?): Boolean = obj is EntityImpl<*> && obj.wrapped is EntityVillager

    override fun isEntitySlime(obj: Any?): Boolean = obj is EntityImpl<*> && obj.wrapped is EntitySlime

    override fun isEntityGhast(obj: Any?): Boolean = obj is EntityImpl<*> && obj.wrapped is EntityGhast

    override fun isEntityDragon(obj: Any?): Boolean = obj is EntityImpl<*> && obj.wrapped is EntityDragon

    override fun isEntityLivingBase(obj: Any?): Boolean = obj is EntityImpl<*> && obj.wrapped is EntityLivingBase

    override fun isEntityPlayer(obj: Any?): Boolean = obj is EntityImpl<*> && obj.wrapped is EntityPlayer

    override fun isEntityArmorStand(obj: Any?): Boolean = obj is EntityImpl<*> && obj.wrapped is EntityArmorStand

    override fun isEntityTNTPrimed(obj: Any?): Boolean = obj is EntityImpl<*> && obj.wrapped is EntityTNTPrimed

    override fun isEntityBoat(obj: Any?): Boolean = obj is EntityImpl<*> && obj.wrapped is EntityBoat

    override fun isEntityMinecart(obj: Any?): Boolean = obj is EntityImpl<*> && obj.wrapped is EntityMinecart

    override fun isEntityItem(obj: Any?): Boolean = obj is EntityImpl<*> && obj.wrapped is EntityItem

    override fun isEntityArrow(obj: Any?): Boolean = obj is EntityImpl<*> && obj.wrapped is EntityArrow

    override fun isEntityFallingBlock(obj: Any?): Boolean = obj is EntityImpl<*> && obj.wrapped is EntityFallingBlock

    override fun isEntityMinecartChest(obj: Any?): Boolean = obj is EntityImpl<*> && obj.wrapped is EntityMinecartChest
    override fun isEntityShulker(obj: Any?): Boolean = obj is EntityImpl<*> && obj.wrapped is EntityShulker

    override fun isTileEntityChest(obj: Any?): Boolean = obj is TileEntityImpl && obj.wrapped is TileEntityChest

    override fun isTileEntityEnderChest(obj: Any?): Boolean = obj is TileEntityImpl && obj.wrapped is TileEntityEnderChest

    override fun isTileEntityFurnace(obj: Any?): Boolean = obj is TileEntityImpl && obj.wrapped is TileEntityFurnace

    override fun isTileEntityDispenser(obj: Any?): Boolean = obj is TileEntityImpl && obj.wrapped is TileEntityDispenser

    override fun isTileEntityHopper(obj: Any?): Boolean = obj is TileEntityImpl && obj.wrapped is TileEntityHopper
    override fun isTileEntityShulkerBox(obj: Any?): Boolean = obj is TileEntityImpl && obj.wrapped is TileEntityShulkerBox

    override fun isSPacketEntity(obj: Any?): Boolean = obj is PacketImpl<*> && obj.wrapped is SPacketEntity

    override fun isSPacketResourcePackSend(obj: Any?): Boolean = obj is PacketImpl<*> && obj.wrapped is SPacketResourcePackSend

    override fun isSPacketPlayerPosLook(obj: Any?): Boolean = obj is PacketImpl<*> && obj.wrapped is SPacketPlayerPosLook

    override fun isSPacketAnimation(obj: Any?): Boolean = obj is PacketImpl<*> && obj.wrapped is SPacketAnimation

    override fun isSPacketEntityVelocity(obj: Any?): Boolean = obj is PacketImpl<*> && obj.wrapped is SPacketEntityVelocity

    override fun isSPacketExplosion(obj: Any?): Boolean = obj is PacketImpl<*> && obj.wrapped is SPacketEntityVelocity

    override fun isSPacketCloseWindow(obj: Any?): Boolean = obj is PacketImpl<*> && obj.wrapped is SPacketCloseWindow

    override fun isSPacketTabComplete(obj: Any?): Boolean = obj is PacketImpl<*> && obj.wrapped is SPacketTabComplete

    override fun isCPacketPlayer(obj: Any?): Boolean = obj is PacketImpl<*> && obj.wrapped is CPacketPlayer

    override fun isCPacketPlayerBlockPlacement(obj: Any?): Boolean = obj is PacketImpl<*> && obj.wrapped is CPacketPlayerTryUseItemOnBlock

    override fun isCPacketUseEntity(obj: Any?): Boolean = obj is PacketImpl<*> && obj.wrapped is CPacketUseEntity

    override fun isCPacketCloseWindow(obj: Any?): Boolean = obj is PacketImpl<*> && obj.wrapped is CPacketCloseWindow

    override fun isCPacketChatMessage(obj: Any?): Boolean = obj is PacketImpl<*> && obj.wrapped is CPacketChatMessage

    override fun isCPacketKeepAlive(obj: Any?): Boolean = obj is PacketImpl<*> && obj.wrapped is CPacketKeepAlive

    override fun isCPacketPlayerPosition(obj: Any?): Boolean = obj is PacketImpl<*> && obj.wrapped is CPacketPlayer.Position

    override fun isCPacketPlayerPosLook(obj: Any?): Boolean = obj is PacketImpl<*> && obj.wrapped is CPacketPlayer.PositionRotation

    override fun isCPacketClientStatus(obj: Any?): Boolean = obj is PacketImpl<*> && obj.wrapped is CPacketClientStatus

    override fun isCPacketAnimation(obj: Any?): Boolean = obj is PacketImpl<*> && obj.wrapped is CPacketAnimation

    override fun isCPacketEntityAction(obj: Any?): Boolean = obj is PacketImpl<*> && obj.wrapped is CPacketEntityAction

    override fun isSPacketWindowItems(obj: Any?): Boolean = obj is PacketImpl<*> && obj.wrapped is SPacketWindowItems

    override fun isCPacketHeldItemChange(obj: Any?): Boolean = obj is PacketImpl<*> && obj.wrapped is CPacketHeldItemChange

    override fun isCPacketPlayerLook(obj: Any?): Boolean = obj is PacketImpl<*> && obj.wrapped is CPacketPlayer.Rotation

    override fun isCPacketCustomPayload(obj: Any?): Boolean = obj is PacketImpl<*> && obj.wrapped is CPacketCustomPayload

    override fun isCPacketHandshake(obj: Any?): Boolean = obj is PacketImpl<*> && obj.wrapped is C00Handshake

    override fun isItemSword(obj: Any?): Boolean = obj is ItemImpl<*> && obj.wrapped is ItemSword

    override fun isItemTool(obj: Any?): Boolean = obj is ItemImpl<*> && obj.wrapped is ItemTool

    override fun isItemArmor(obj: Any?): Boolean = obj is ItemImpl<*> && obj.wrapped is ItemArmor

    override fun isItemPotion(obj: Any?): Boolean = obj is ItemImpl<*> && obj.wrapped is ItemPotion

    override fun isItemBlock(obj: Any?): Boolean = obj is ItemImpl<*> && obj.wrapped is ItemBlock

    override fun isItemBow(obj: Any?): Boolean = obj is ItemImpl<*> && obj.wrapped is ItemBow

    override fun isItemBucket(obj: Any?): Boolean = obj is ItemImpl<*> && obj.wrapped is ItemBucket

    override fun isItemFood(obj: Any?): Boolean = obj is ItemImpl<*> && obj.wrapped is ItemFood

    override fun isItemBucketMilk(obj: Any?): Boolean = obj is ItemImpl<*> && obj.wrapped is ItemBucketMilk

    override fun isItemPickaxe(obj: Any?): Boolean = obj is ItemImpl<*> && obj.wrapped is ItemPickaxe

    override fun isItemAxe(obj: Any?): Boolean = obj is ItemImpl<*> && obj.wrapped is ItemAxe

    override fun isItemBed(obj: Any?): Boolean = obj is ItemImpl<*> && obj.wrapped is ItemBed

    override fun isItemEnderPearl(obj: Any?): Boolean = obj is ItemImpl<*> && obj.wrapped is ItemEnderPearl

    override fun isItemEnchantedBook(obj: Any?): Boolean = obj is ItemImpl<*> && obj.wrapped is ItemEnchantedBook

    override fun isItemBoat(obj: Any?): Boolean = obj is ItemImpl<*> && obj.wrapped is ItemBoat

    override fun isItemMinecart(obj: Any?): Boolean = obj is ItemImpl<*> && obj.wrapped is ItemMinecart

    override fun isItemAppleGold(obj: Any?): Boolean = obj is ItemImpl<*> && obj.wrapped is ItemAppleGold

    override fun isItemSnowball(obj: Any?): Boolean = obj is ItemImpl<*> && obj.wrapped is ItemSnowball

    override fun isItemEgg(obj: Any?): Boolean = obj is ItemImpl<*> && obj.wrapped is ItemEgg

    override fun isItemFishingRod(obj: Any?): Boolean = obj is ItemImpl<*> && obj.wrapped is ItemFishingRod
    override fun isItemAir(obj: Any?): Boolean = obj is ItemImpl<*> && obj.wrapped is ItemAir

    override fun isBlockAir(obj: Any?): Boolean = obj is BlockImpl && obj.wrapped is BlockAir

    override fun isBlockFence(obj: Any?): Boolean = obj is BlockImpl && obj.wrapped is BlockFence

    override fun isBlockSnow(obj: Any?): Boolean = obj is BlockImpl && obj.wrapped is BlockSnow

    override fun isBlockLadder(obj: Any?): Boolean = obj is BlockImpl && obj.wrapped is BlockLadder

    override fun isBlockVine(obj: Any?): Boolean = obj is BlockImpl && obj.wrapped is BlockVine

    override fun isBlockSlime(obj: Any?): Boolean = obj is BlockImpl && obj.wrapped is BlockSlime

    override fun isBlockSlab(obj: Any?): Boolean = obj is BlockImpl && obj.wrapped is BlockSlab

    override fun isBlockStairs(obj: Any?): Boolean = obj is BlockImpl && obj.wrapped is BlockStairs

    override fun isBlockCarpet(obj: Any?): Boolean = obj is BlockImpl && obj.wrapped is BlockCarpet

    override fun isBlockPane(obj: Any?): Boolean = obj is BlockImpl && obj.wrapped is BlockPane

    override fun isBlockLiquid(obj: Any?): Boolean = obj is BlockImpl && obj.wrapped is BlockLiquid

    override fun isBlockCactus(obj: Any?): Boolean = obj is BlockImpl && obj.wrapped is BlockCactus

    override fun isBlockBedrock(obj: Any?): Boolean = obj is BlockImpl && obj.wrapped == Blocks.BEDROCK

    override fun isBlockBush(obj: Any?): Boolean = obj is BlockImpl && obj.wrapped is BlockBush

    override fun isGuiInventory(obj: Any?): Boolean = obj is GuiImpl<*> && obj.wrapped is GuiInventory

    override fun isGuiContainer(obj: Any?): Boolean = obj is GuiImpl<*> && obj.wrapped is GuiContainer

    override fun isGuiGameOver(obj: Any?): Boolean = obj is GuiImpl<*> && obj.wrapped is GuiGameOver

    override fun isGuiChat(obj: Any?): Boolean = obj is GuiImpl<*> && obj.wrapped is GuiChat

    override fun isGuiIngameMenu(obj: Any?): Boolean = obj is GuiImpl<*> && obj.wrapped is GuiIngameMenu

    override fun isGuiChest(obj: Any?): Boolean = obj is GuiImpl<*> && obj.wrapped is GuiChest

    override fun isGuiHudDesigner(obj: Any?): Boolean = obj is GuiScreenImpl<*> && obj.wrapped is GuiScreenWrapper && obj.wrapped.wrapped is GuiHudDesigner

    override fun isClickGui(obj: Any?): Boolean = obj is GuiScreenImpl<*> && obj.wrapped is GuiScreenWrapper && obj.wrapped.wrapped is ClickGui

    override fun getPotionEnum(type: PotionType): IPotion {
        return PotionImpl(when (type) {
            PotionType.HEAL -> MobEffects.INSTANT_HEALTH
            PotionType.REGENERATION -> MobEffects.REGENERATION
            PotionType.BLINDNESS -> MobEffects.BLINDNESS
            PotionType.MOVE_SPEED -> MobEffects.SPEED
            PotionType.HUNGER -> MobEffects.HUNGER
            PotionType.DIG_SLOWDOWN -> MobEffects.MINING_FATIGUE
            PotionType.CONFUSION -> MobEffects.NAUSEA
            PotionType.WEAKNESS -> MobEffects.WEAKNESS
            PotionType.MOVE_SLOWDOWN -> MobEffects.SLOWNESS
            PotionType.HARM -> MobEffects.INSTANT_DAMAGE
            PotionType.WITHER -> MobEffects.WITHER
            PotionType.POISON -> MobEffects.POISON
            PotionType.NIGHT_VISION -> MobEffects.NIGHT_VISION
        })
    }

    override fun getEnumFacing(type: EnumFacingType): IEnumFacing {
        return EnumFacingImpl(
                when (type) {
                    EnumFacingType.DOWN -> EnumFacing.DOWN
                    EnumFacingType.UP -> EnumFacing.UP
                    EnumFacingType.NORTH -> EnumFacing.NORTH
                    EnumFacingType.SOUTH -> EnumFacing.SOUTH
                    EnumFacingType.WEST -> EnumFacing.WEST
                    EnumFacingType.EAST -> EnumFacing.EAST
                }
        )
    }

    override fun getBlockEnum(type: BlockType): IBlock {
        return when (type) {
            BlockType.ENCHANTING_TABLE -> Blocks.ENCHANTING_TABLE.wrap()
            BlockType.CHEST -> Blocks.CHEST.wrap()
            BlockType.ENDER_CHEST -> Blocks.ENDER_CHEST.wrap()
            BlockType.TRAPPED_CHEST -> Blocks.TRAPPED_CHEST.wrap()
            BlockType.ANVIL -> Blocks.ANVIL.wrap()
            BlockType.SAND -> Blocks.SAND.wrap()
            BlockType.WEB -> Blocks.WEB.wrap()
            BlockType.TORCH -> Blocks.TORCH.wrap()
            BlockType.CRAFTING_TABLE -> Blocks.CRAFTING_TABLE.wrap()
            BlockType.FURNACE -> Blocks.FURNACE.wrap()
            BlockType.WATERLILY -> Blocks.WATERLILY.wrap()
            BlockType.DISPENSER -> Blocks.DISPENSER.wrap()
            BlockType.STONE_PRESSURE_PLATE -> Blocks.STONE_PRESSURE_PLATE.wrap()
            BlockType.WODDEN_PRESSURE_PLATE -> Blocks.WOODEN_PRESSURE_PLATE.wrap()
            BlockType.TNT -> Blocks.TNT.wrap()
            BlockType.STANDING_BANNER -> Blocks.STANDING_BANNER.wrap()
            BlockType.WALL_BANNER -> Blocks.WALL_BANNER.wrap()
            BlockType.REDSTONE_TORCH -> Blocks.REDSTONE_TORCH.wrap()
            BlockType.NOTEBLOCK -> Blocks.NOTEBLOCK.wrap()
            BlockType.DROPPER -> Blocks.DROPPER.wrap()
            BlockType.SNOW_LAYER -> Blocks.SNOW_LAYER.wrap()
            BlockType.AIR -> Blocks.AIR.wrap()
            BlockType.ICE_PACKED -> Blocks.PACKED_ICE.wrap()
            BlockType.ICE -> Blocks.ICE.wrap()
            BlockType.WATER -> Blocks.WATER.wrap()
            BlockType.BARRIER -> Blocks.BARRIER.wrap()
            BlockType.FLOWING_WATER -> Blocks.FLOWING_WATER.wrap()
            BlockType.COAL_ORE -> Blocks.COAL_ORE.wrap()
            BlockType.IRON_ORE -> Blocks.IRON_ORE.wrap()
            BlockType.GOLD_ORE -> Blocks.GOLD_ORE.wrap()
            BlockType.REDSTONE_ORE -> Blocks.REDSTONE_ORE.wrap()
            BlockType.LAPIS_ORE -> Blocks.LAPIS_ORE.wrap()
            BlockType.DIAMOND_ORE -> Blocks.DIAMOND_ORE.wrap()
            BlockType.EMERALD_ORE -> Blocks.EMERALD_ORE.wrap()
            BlockType.QUARTZ_ORE -> Blocks.QUARTZ_ORE.wrap()
            BlockType.CLAY -> Blocks.CLAY.wrap()
            BlockType.GLOWSTONE -> Blocks.GLOWSTONE.wrap()
            BlockType.LADDER -> Blocks.LADDER.wrap()
            BlockType.COAL_BLOCK -> Blocks.COAL_BLOCK.wrap()
            BlockType.IRON_BLOCK -> Blocks.IRON_BLOCK.wrap()
            BlockType.GOLD_BLOCK -> Blocks.GOLD_BLOCK.wrap()
            BlockType.DIAMOND_BLOCK -> Blocks.DIAMOND_BLOCK.wrap()
            BlockType.EMERALD_BLOCK -> Blocks.EMERALD_BLOCK.wrap()
            BlockType.REDSTONE_BLOCK -> Blocks.REDSTONE_BLOCK.wrap()
            BlockType.LAPIS_BLOCK -> Blocks.LAPIS_BLOCK.wrap()
            BlockType.FIRE -> Blocks.FIRE.wrap()
            BlockType.MOSSY_COBBLESTONE -> Blocks.MOSSY_COBBLESTONE.wrap()
            BlockType.MOB_SPAWNER -> Blocks.MOB_SPAWNER.wrap()
            BlockType.END_PORTAL_FRAME -> Blocks.END_PORTAL_FRAME.wrap()
            BlockType.BOOKSHELF -> Blocks.BOOKSHELF.wrap()
            BlockType.COMMAND_BLOCK -> Blocks.COMMAND_BLOCK.wrap()
            BlockType.LAVA -> Blocks.LAVA.wrap()
            BlockType.FLOWING_LAVA -> Blocks.FLOWING_LAVA.wrap()
            BlockType.LIT_FURNACE -> Blocks.LIT_FURNACE.wrap()
            BlockType.DRAGON_EGG -> Blocks.DRAGON_EGG.wrap()
            BlockType.BROWN_MUSHROOM_BLOCK -> Blocks.BROWN_MUSHROOM_BLOCK.wrap()
            BlockType.RED_MUSHROOM_BLOCK -> Blocks.RED_MUSHROOM_BLOCK.wrap()
            BlockType.FARMLAND -> Blocks.FARMLAND.wrap()
        }
    }

    override fun getMaterialEnum(type: MaterialType): IMaterial {
        return MaterialImpl(
                when (type) {
                    MaterialType.AIR -> Material.AIR
                    MaterialType.WATER -> Material.WATER
                    MaterialType.LAVA -> Material.LAVA
                }
        )
    }

    override fun getStatEnum(type: StatType): IStatBase {
        return StatBaseImpl(
                when (type) {
                    StatType.JUMP_STAT -> StatList.JUMP
                }
        )
    }

    override fun getItemEnum(type: ItemType): IItem {
        return ItemImpl(
                when (type) {
                    ItemType.MUSHROOM_STEW -> Items.MUSHROOM_STEW
                    ItemType.BOWL -> Items.BOWL
                    ItemType.FLINT_AND_STEEL -> Items.FLINT_AND_STEEL
                    ItemType.LAVA_BUCKET -> Items.LAVA_BUCKET
                    ItemType.WRITABLE_BOOK -> Items.WRITABLE_BOOK
                    ItemType.WATER_BUCKET -> Items.WATER_BUCKET
                    ItemType.COMMAND_BLOCK_MINECART -> Items.COMMAND_BLOCK_MINECART
                    ItemType.POTION_ITEM -> Items.POTIONITEM
                    ItemType.SKULL -> Items.SKULL
                    ItemType.ARMOR_STAND -> Items.ARMOR_STAND
                }
        )
    }

    override fun getEnchantmentEnum(type: EnchantmentType): IEnchantment {
        return EnchantmentImpl(
                when (type) {
                    EnchantmentType.SHARPNESS -> Enchantments.SHARPNESS
                    EnchantmentType.POWER -> Enchantments.POWER
                    EnchantmentType.PROTECTION -> Enchantments.PROTECTION
                    EnchantmentType.FEATHER_FALLING -> Enchantments.FEATHER_FALLING
                    EnchantmentType.PROJECTILE_PROTECTION -> Enchantments.PROJECTILE_PROTECTION
                    EnchantmentType.THORNS -> Enchantments.THORNS
                    EnchantmentType.FIRE_PROTECTION -> Enchantments.FIRE_PROTECTION
                    EnchantmentType.RESPIRATION -> Enchantments.RESPIRATION
                    EnchantmentType.AQUA_AFFINITY -> Enchantments.AQUA_AFFINITY
                    EnchantmentType.BLAST_PROTECTION -> Enchantments.BLAST_PROTECTION
                    EnchantmentType.UNBREAKING -> Enchantments.UNBREAKING
                }
        )
    }

    override fun getVertexFormatEnum(type: WDefaultVertexFormats): IVertexFormat {
        return VertexFormatImpl(
                when (type) {
                    WDefaultVertexFormats.POSITION -> DefaultVertexFormats.POSITION
                    WDefaultVertexFormats.POSITION_TEX -> DefaultVertexFormats.POSITION_TEX
                    WDefaultVertexFormats.POSITION_COLOR -> DefaultVertexFormats.POSITION_COLOR
                }
        )
    }

    override fun wrapFontRenderer(fontRenderer: IWrappedFontRenderer): IFontRenderer = FontRendererImpl(FontRendererWrapper(fontRenderer))

    override fun wrapGuiScreen(clickGui: WrappedGuiScreen): IGuiScreen {
        val instance = GuiScreenImpl(GuiScreenWrapper(clickGui))

        clickGui.representedScreen = instance

        return instance
    }

    override fun createSafeVertexBuffer(vertexFormat: IVertexFormat): IVertexBuffer = SafeVertexBuffer(vertexFormat.unwrap()).wrap()
    override fun wrapCreativeTab(name: String, wrappedCreativeTabs: WrappedCreativeTabs) {
        wrappedCreativeTabs.representedType = CreativeTabsImpl(CreativeTabsWrapper(wrappedCreativeTabs, name))
    }

    override fun wrapGuiSlot(wrappedGuiSlot: WrappedGuiSlot, mc: IMinecraft, width: Int, height: Int, top: Int, bottom: Int, slotHeight: Int) {
        GuiSlotWrapper(wrappedGuiSlot, mc, width, height, top, bottom, slotHeight)
    }

    override fun getGlStateManager(): IGlStateManager = GlStateManagerImpl
    override fun createCPacketEncryptionResponse(secretKey: SecretKey, publicKey: PublicKey, verifyToken: ByteArray): IPacket = PacketImpl(CPacketEncryptionResponse(secretKey, publicKey, verifyToken))

}
