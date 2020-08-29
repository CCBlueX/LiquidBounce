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
import net.ccbluex.liquidbounce.injection.backend.utils.SafeVertexBuffer
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
import net.minecraft.enchantment.Enchantment
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.boss.EntityDragon
import net.minecraft.entity.item.*
import net.minecraft.entity.monster.EntityGhast
import net.minecraft.entity.monster.EntityGolem
import net.minecraft.entity.monster.EntityMob
import net.minecraft.entity.monster.EntitySlime
import net.minecraft.entity.passive.EntityAnimal
import net.minecraft.entity.passive.EntityBat
import net.minecraft.entity.passive.EntitySquid
import net.minecraft.entity.passive.EntityVillager
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.projectile.EntityArrow
import net.minecraft.event.ClickEvent
import net.minecraft.init.Blocks
import net.minecraft.init.Items
import net.minecraft.item.*
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.nbt.NBTTagDouble
import net.minecraft.nbt.NBTTagList
import net.minecraft.nbt.NBTTagString
import net.minecraft.network.PacketBuffer
import net.minecraft.network.handshake.client.C00Handshake
import net.minecraft.network.login.client.C01PacketEncryptionResponse
import net.minecraft.network.play.client.*
import net.minecraft.network.play.server.*
import net.minecraft.potion.Potion
import net.minecraft.potion.PotionEffect
import net.minecraft.stats.StatList
import net.minecraft.tileentity.*
import net.minecraft.util.*
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

    override fun createChatComponentText(text: String): IIChatComponent = IChatComponentImpl(ChatComponentText(text))

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

    override fun createPotionEffect(id: Int, time: Int, strength: Int): IPotionEffect = PotionEffectImpl(PotionEffect(id, time, strength))

    override fun createGuiOptions(parentScreen: IGuiScreen, gameSettings: IGameSettings): IGuiScreen = GuiScreenImpl(GuiOptions(parentScreen.unwrap(), gameSettings.unwrap()))

    override fun createGuiSelectWorld(parentScreen: IGuiScreen): IGuiScreen = GuiScreenImpl(GuiSelectWorld(parentScreen.unwrap()))

    override fun createGuiMultiplayer(parentScreen: IGuiScreen): IGuiScreen = GuiScreenImpl(GuiMultiplayer(parentScreen.unwrap()))

    override fun createGuiModList(parentScreen: IGuiScreen): IGuiScreen = GuiScreenImpl(GuiModList(parentScreen.unwrap()))
    override fun createGuiConnecting(parent: IGuiScreen, mc: IMinecraft, serverData: IServerData): IGuiScreen = GuiScreenImpl(GuiConnecting(parent.unwrap(), mc.unwrap(), serverData.unwrap()))

    override fun createCPacketHeldItemChange(slot: Int): ICPacketHeldItemChange = CPacketHeldItemChangeImpl(C09PacketHeldItemChange(slot))

    override fun createCPacketPlayerBlockPlacement(stack: IItemStack?): ICPacketPlayerBlockPlacement = CPacketPlayerBlockPlacementImpl(C08PacketPlayerBlockPlacement(stack?.unwrap()))

    override fun createCPacketPlayerBlockPlacement(positionIn: WBlockPos, placedBlockDirectionIn: Int, stackIn: IItemStack?, facingXIn: Float, facingYIn: Float, facingZIn: Float): ICPacketPlayerBlockPlacement = CPacketPlayerBlockPlacementImpl(C08PacketPlayerBlockPlacement(positionIn.unwrap(), placedBlockDirectionIn, stackIn?.unwrap(), facingXIn, facingYIn, facingZIn))

    override fun createCPacketPlayerPosLook(x: Double, y: Double, z: Double, yaw: Float, pitch: Float, onGround: Boolean): ICPacketPlayerPosLook = CPacketPlayerPosLookImpl(C03PacketPlayer.C06PacketPlayerPosLook(x, y, z, yaw, pitch, onGround))

    override fun createCPacketClientStatus(state: ICPacketClientStatus.WEnumState): ICPacketClientStatus = CPacketClientStatusImpl(C16PacketClientStatus(state.unwrap()))

    override fun createCPacketPlayerDigging(wAction: ICPacketPlayerDigging.WAction, pos: WBlockPos, facing: IEnumFacing): IPacket = PacketImpl(C07PacketPlayerDigging(wAction.unwrap(), pos.unwrap(), facing.unwrap()))

    override fun createCPacketPlayerPosition(x: Double, y: Double, z: Double, onGround: Boolean): ICPacketPlayer = CPacketPlayerImpl(C03PacketPlayer.C04PacketPlayerPosition(x, y, z, onGround))

    override fun createICPacketResourcePackStatus(hash: String, status: ICPacketResourcePackStatus.WAction): IPacket = PacketImpl(C19PacketResourcePackStatus(hash, status.unwrap()))

    override fun createCPacketPlayerLook(yaw: Float, pitch: Float, onGround: Boolean): ICPacketPlayer = CPacketPlayerImpl(C03PacketPlayer.C05PacketPlayerLook(yaw, pitch, onGround))

    override fun createCPacketUseEntity(player: IEntity, wAction: ICPacketUseEntity.WAction): ICPacketUseEntity = CPacketUseEntityImpl(C02PacketUseEntity(player.unwrap(), wAction.unwrap()))

    override fun createCPacketUseEntity(entity: IEntity, positionVector: WVec3): ICPacketUseEntity = CPacketUseEntityImpl(C02PacketUseEntity(entity.unwrap(), positionVector.unwrap()))

    override fun createCPacketCreativeInventoryAction(slot: Int, itemStack: IItemStack): IPacket = PacketImpl(C10PacketCreativeInventoryAction(slot, itemStack.unwrap()))

    override fun createCPacketEntityAction(player: IEntity, wAction: ICPacketEntityAction.WAction): ICPacketEntityAction = CPacketEntityActionImpl(C0BPacketEntityAction(player.unwrap(), wAction.unwrap()))

    override fun createCPacketCustomPayload(channel: String, payload: IPacketBuffer): ICPacketCustomPayload = CPacketCustomPayloadImpl(C17PacketCustomPayload(channel, payload.unwrap()))

    override fun createCPacketCloseWindow(windowId: Int): ICPacketCloseWindow = CPacketCloseWindowImpl(C0DPacketCloseWindow(windowId))

    override fun createCPacketCloseWindow(): ICPacketCloseWindow = CPacketCloseWindowImpl(C0DPacketCloseWindow())

    override fun createCPacketPlayer(onGround: Boolean): ICPacketPlayer = CPacketPlayerImpl(C03PacketPlayer(onGround))

    override fun createCPacketTabComplete(text: String): IPacket = PacketImpl(C14PacketTabComplete(text))

    override fun createCPacketAnimation(): ICPacketAnimation = CPacketAnimationImpl(C0APacketAnimation())

    override fun createCPacketKeepAlive(): ICPacketKeepAlive = CPacketKeepAliveImpl(C00PacketKeepAlive())

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
    override fun isEntityShulker(obj: Any?): Boolean = false

    override fun isTileEntityChest(obj: Any?): Boolean = obj is TileEntityImpl && obj.wrapped is TileEntityChest

    override fun isTileEntityEnderChest(obj: Any?): Boolean = obj is TileEntityImpl && obj.wrapped is TileEntityEnderChest

    override fun isTileEntityFurnace(obj: Any?): Boolean = obj is TileEntityImpl && obj.wrapped is TileEntityFurnace

    override fun isTileEntityDispenser(obj: Any?): Boolean = obj is TileEntityImpl && obj.wrapped is TileEntityDispenser

    override fun isTileEntityHopper(obj: Any?): Boolean = obj is TileEntityImpl && obj.wrapped is TileEntityHopper

    override fun isSPacketEntity(obj: Any?): Boolean = obj is PacketImpl<*> && obj.wrapped is S14PacketEntity

    override fun isSPacketResourcePackSend(obj: Any?): Boolean = obj is PacketImpl<*> && obj.wrapped is S48PacketResourcePackSend

    override fun isSPacketPlayerPosLook(obj: Any?): Boolean = obj is PacketImpl<*> && obj.wrapped is S08PacketPlayerPosLook

    override fun isSPacketAnimation(obj: Any?): Boolean = obj is PacketImpl<*> && obj.wrapped is S0BPacketAnimation

    override fun isSPacketEntityVelocity(obj: Any?): Boolean = obj is PacketImpl<*> && obj.wrapped is S12PacketEntityVelocity

    override fun isSPacketExplosion(obj: Any?): Boolean = obj is PacketImpl<*> && obj.wrapped is S12PacketEntityVelocity

    override fun isSPacketCloseWindow(obj: Any?): Boolean = obj is PacketImpl<*> && obj.wrapped is S2EPacketCloseWindow

    override fun isSPacketTabComplete(obj: Any?): Boolean = obj is PacketImpl<*> && obj.wrapped is S3APacketTabComplete

    override fun isCPacketPlayer(obj: Any?): Boolean = obj is PacketImpl<*> && obj.wrapped is C03PacketPlayer

    override fun isCPacketPlayerBlockPlacement(obj: Any?): Boolean = obj is PacketImpl<*> && obj.wrapped is C08PacketPlayerBlockPlacement

    override fun isCPacketUseEntity(obj: Any?): Boolean = obj is PacketImpl<*> && obj.wrapped is C02PacketUseEntity

    override fun isCPacketCloseWindow(obj: Any?): Boolean = obj is PacketImpl<*> && obj.wrapped is C0DPacketCloseWindow

    override fun isCPacketChatMessage(obj: Any?): Boolean = obj is PacketImpl<*> && obj.wrapped is C01PacketChatMessage

    override fun isCPacketKeepAlive(obj: Any?): Boolean = obj is PacketImpl<*> && obj.wrapped is C00PacketKeepAlive

    override fun isCPacketPlayerPosition(obj: Any?): Boolean = obj is PacketImpl<*> && obj.wrapped is C03PacketPlayer.C04PacketPlayerPosition

    override fun isCPacketPlayerPosLook(obj: Any?): Boolean = obj is PacketImpl<*> && obj.wrapped is C03PacketPlayer.C06PacketPlayerPosLook

    override fun isCPacketClientStatus(obj: Any?): Boolean = obj is PacketImpl<*> && obj.wrapped is C16PacketClientStatus

    override fun isCPacketAnimation(obj: Any?): Boolean = obj is PacketImpl<*> && obj.wrapped is C0APacketAnimation

    override fun isCPacketEntityAction(obj: Any?): Boolean = obj is PacketImpl<*> && obj.wrapped is C0BPacketEntityAction

    override fun isSPacketWindowItems(obj: Any?): Boolean = obj is PacketImpl<*> && obj.wrapped is S30PacketWindowItems

    override fun isCPacketHeldItemChange(obj: Any?): Boolean = obj is PacketImpl<*> && obj.wrapped is C09PacketHeldItemChange

    override fun isCPacketPlayerLook(obj: Any?): Boolean = obj is PacketImpl<*> && obj.wrapped is C03PacketPlayer.C05PacketPlayerLook

    override fun isCPacketCustomPayload(obj: Any?): Boolean = obj is PacketImpl<*> && obj.wrapped is C17PacketCustomPayload

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

    // ItemAir is not a thing in 1.8.9
    override fun isItemAir(obj: Any?): Boolean = false

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

    override fun isBlockBedrock(obj: Any?): Boolean = obj is BlockImpl && obj.wrapped == Blocks.bedrock

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
            PotionType.HEAL -> Potion.heal
            PotionType.REGENERATION -> Potion.regeneration
            PotionType.BLINDNESS -> Potion.blindness
            PotionType.MOVE_SPEED -> Potion.moveSpeed
            PotionType.HUNGER -> Potion.hunger
            PotionType.DIG_SLOWDOWN -> Potion.digSlowdown
            PotionType.CONFUSION -> Potion.confusion
            PotionType.WEAKNESS -> Potion.weakness
            PotionType.MOVE_SLOWDOWN -> Potion.moveSlowdown
            PotionType.HARM -> Potion.harm
            PotionType.WITHER -> Potion.wither
            PotionType.POISON -> Potion.poison
            PotionType.NIGHT_VISION -> Potion.nightVision
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
            BlockType.ENCHANTING_TABLE -> Blocks.enchanting_table.wrap()
            BlockType.CHEST -> Blocks.chest.wrap()
            BlockType.ENDER_CHEST -> Blocks.ender_chest.wrap()
            BlockType.TRAPPED_CHEST -> Blocks.trapped_chest.wrap()
            BlockType.ANVIL -> Blocks.anvil.wrap()
            BlockType.SAND -> Blocks.sand.wrap()
            BlockType.WEB -> Blocks.web.wrap()
            BlockType.TORCH -> Blocks.torch.wrap()
            BlockType.CRAFTING_TABLE -> Blocks.crafting_table.wrap()
            BlockType.FURNACE -> Blocks.furnace.wrap()
            BlockType.WATERLILY -> Blocks.waterlily.wrap()
            BlockType.DISPENSER -> Blocks.dispenser.wrap()
            BlockType.STONE_PRESSURE_PLATE -> Blocks.stone_pressure_plate.wrap()
            BlockType.WODDEN_PRESSURE_PLATE -> Blocks.wooden_pressure_plate.wrap()
            BlockType.TNT -> Blocks.tnt.wrap()
            BlockType.STANDING_BANNER -> Blocks.standing_banner.wrap()
            BlockType.WALL_BANNER -> Blocks.wall_banner.wrap()
            BlockType.REDSTONE_TORCH -> Blocks.redstone_torch.wrap()
            BlockType.NOTEBLOCK -> Blocks.noteblock.wrap()
            BlockType.DROPPER -> Blocks.dropper.wrap()
            BlockType.SNOW_LAYER -> Blocks.snow_layer.wrap()
            BlockType.AIR -> Blocks.air.wrap()
            BlockType.ICE_PACKED -> Blocks.packed_ice.wrap()
            BlockType.ICE -> Blocks.ice.wrap()
            BlockType.WATER -> Blocks.water.wrap()
            BlockType.BARRIER -> Blocks.barrier.wrap()
            BlockType.FLOWING_WATER -> Blocks.flowing_water.wrap()
            BlockType.COAL_ORE -> Blocks.coal_ore.wrap()
            BlockType.IRON_ORE -> Blocks.iron_ore.wrap()
            BlockType.GOLD_ORE -> Blocks.gold_ore.wrap()
            BlockType.REDSTONE_ORE -> Blocks.redstone_ore.wrap()
            BlockType.LAPIS_ORE -> Blocks.lapis_ore.wrap()
            BlockType.DIAMOND_ORE -> Blocks.diamond_ore.wrap()
            BlockType.EMERALD_ORE -> Blocks.emerald_ore.wrap()
            BlockType.QUARTZ_ORE -> Blocks.quartz_ore.wrap()
            BlockType.CLAY -> Blocks.clay.wrap()
            BlockType.GLOWSTONE -> Blocks.glowstone.wrap()
            BlockType.LADDER -> Blocks.ladder.wrap()
            BlockType.COAL_BLOCK -> Blocks.coal_block.wrap()
            BlockType.IRON_BLOCK -> Blocks.iron_block.wrap()
            BlockType.GOLD_BLOCK -> Blocks.gold_block.wrap()
            BlockType.DIAMOND_BLOCK -> Blocks.diamond_block.wrap()
            BlockType.EMERALD_BLOCK -> Blocks.emerald_block.wrap()
            BlockType.REDSTONE_BLOCK -> Blocks.redstone_block.wrap()
            BlockType.LAPIS_BLOCK -> Blocks.lapis_block.wrap()
            BlockType.FIRE -> Blocks.fire.wrap()
            BlockType.MOSSY_COBBLESTONE -> Blocks.mossy_cobblestone.wrap()
            BlockType.MOB_SPAWNER -> Blocks.mob_spawner.wrap()
            BlockType.END_PORTAL_FRAME -> Blocks.end_portal_frame.wrap()
            BlockType.BOOKSHELF -> Blocks.bookshelf.wrap()
            BlockType.COMMAND_BLOCK -> Blocks.command_block.wrap()
            BlockType.LAVA -> Blocks.lava.wrap()
            BlockType.FLOWING_LAVA -> Blocks.flowing_lava.wrap()
            BlockType.LIT_FURNACE -> Blocks.lit_furnace.wrap()
            BlockType.DRAGON_EGG -> Blocks.dragon_egg.wrap()
            BlockType.BROWN_MUSHROOM_BLOCK -> Blocks.brown_mushroom_block.wrap()
            BlockType.RED_MUSHROOM_BLOCK -> Blocks.red_mushroom_block.wrap()
            BlockType.FARMLAND -> Blocks.farmland.wrap()
        }
    }

    override fun getMaterialEnum(type: MaterialType): IMaterial {
        return MaterialImpl(
                when (type) {
                    MaterialType.AIR -> Material.air
                    MaterialType.WATER -> Material.water
                    MaterialType.LAVA -> Material.lava
                }
        )
    }

    override fun getStatEnum(type: StatType): IStatBase {
        return StatBaseImpl(
                when (type) {
                    StatType.JUMP_STAT -> StatList.jumpStat
                }
        )
    }

    override fun getItemEnum(type: ItemType): IItem {
        return ItemImpl(
                when (type) {
                    ItemType.MUSHROOM_STEW -> Items.mushroom_stew
                    ItemType.BOWL -> Items.bowl
                    ItemType.FLINT_AND_STEEL -> Items.flint_and_steel
                    ItemType.LAVA_BUCKET -> Items.lava_bucket
                    ItemType.WRITABLE_BOOK -> Items.writable_book
                    ItemType.WATER_BUCKET -> Items.water_bucket
                    ItemType.COMMAND_BLOCK_MINECART -> Items.command_block_minecart
                    ItemType.POTION_ITEM -> Items.potionitem
                    ItemType.SKULL -> Items.skull
                    ItemType.ARMOR_STAND -> Items.armor_stand
                }
        )
    }

    override fun getEnchantmentEnum(type: EnchantmentType): IEnchantment {
        return EnchantmentImpl(
                when (type) {
                    EnchantmentType.SHARPNESS -> Enchantment.sharpness
                    EnchantmentType.POWER -> Enchantment.power
                    EnchantmentType.PROTECTION -> Enchantment.protection
                    EnchantmentType.FEATHER_FALLING -> Enchantment.featherFalling
                    EnchantmentType.PROJECTILE_PROTECTION -> Enchantment.projectileProtection
                    EnchantmentType.THORNS -> Enchantment.thorns
                    EnchantmentType.FIRE_PROTECTION -> Enchantment.fireProtection
                    EnchantmentType.RESPIRATION -> Enchantment.respiration
                    EnchantmentType.AQUA_AFFINITY -> Enchantment.aquaAffinity
                    EnchantmentType.BLAST_PROTECTION -> Enchantment.blastProtection
                    EnchantmentType.UNBREAKING -> Enchantment.unbreaking
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
    override fun createCPacketEncryptionResponse(secretKey: SecretKey, publicKey: PublicKey, VerifyToken: ByteArray): IPacket = PacketImpl(C01PacketEncryptionResponse(secretKey, publicKey, VerifyToken))

    override fun createCPacketTryUseItem(stack: WEnumHand): PacketImpl<*> = Backend.BACKEND_UNSUPPORTED()

    override fun isTileEntityShulkerBox(obj: Any?): Boolean = false

}