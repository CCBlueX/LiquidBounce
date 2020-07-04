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
import net.ccbluex.liquidbounce.api.minecraft.client.multiplayer.IWorldClient
import net.ccbluex.liquidbounce.api.minecraft.client.render.ITessellator
import net.ccbluex.liquidbounce.api.minecraft.client.render.IThreadDownloadImageData
import net.ccbluex.liquidbounce.api.minecraft.client.render.WIImageBuffer
import net.ccbluex.liquidbounce.api.minecraft.client.render.texture.IDynamicTexture
import net.ccbluex.liquidbounce.api.minecraft.client.render.vertex.IVertexFormat
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
import net.ccbluex.liquidbounce.api.util.WrappedGuiScreen
import net.ccbluex.liquidbounce.injection.backend.utils.unwrap
import net.ccbluex.liquidbounce.injection.backend.utils.wrap
import net.minecraft.client.gui.FontRenderer
import net.minecraft.client.renderer.IImageBuffer
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.ThreadDownloadImageData
import net.minecraft.init.Blocks
import net.minecraft.util.ResourceLocation
import java.awt.image.BufferedImage
import java.io.File

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

    override fun createPacketBuffer(buffer: ByteBuf): IPacketBuffer {
        TODO("Not yet implemented")
    }

    override fun createChatComponentText(text: String): IIChatComponent {
        TODO("Not yet implemented")
    }

    override fun createClickEvent(action: IClickEvent.WAction, value: String): IClickEvent {
        TODO("Not yet implemented")
    }

    override fun createGuiTextField(id: Int, iFontRenderer: IFontRenderer, x: Int, y: Int, width: Int, height: Int): IGuiTextField {
        TODO("Not yet implemented")
    }

    override fun createGuiPasswordField(id: Int, iFontRenderer: IFontRenderer, x: Int, y: Int, width: Int, height: Int): IGuiTextField {
        TODO("Not yet implemented")
    }

    override fun createGuiButton(id: Int, x: Int, y: Int, width: Int, height: Int, text: String): IGuiButton {
        TODO("Not yet implemented")
    }

    override fun createGuiButton(id: Int, x: Int, y: Int, text: String): IGuiButton {
        TODO("Not yet implemented")
    }

    override fun createSession(name: String, uuid: String, accessToken: String, accountType: String): ISession {
        TODO("Not yet implemented")
    }

    override fun createDynamicTexture(image: BufferedImage): IDynamicTexture {
        TODO("Not yet implemented")
    }

    override fun createItem(): IItem {
        TODO("Not yet implemented")
    }

    override fun createItemStack(item: IItem, amount: Int, meta: Int): IItemStack {
        TODO("Not yet implemented")
    }

    override fun createItemStack(item: IItem): IItemStack {
        TODO("Not yet implemented")
    }

    override fun createItemStack(blockEnum: IBlock): IItemStack {
        TODO("Not yet implemented")
    }

    override fun createAxisAlignedBB(minX: Double, minY: Double, minZ: Double, maxX: Double, maxY: Double, maxZ: Double): IAxisAlignedBB {
        TODO("Not yet implemented")
    }

    override fun createScaledResolution(mc: IMinecraft): IScaledResolution {
        TODO("Not yet implemented")
    }

    override fun createNBTTagCompound(): INBTTagCompound {
        TODO("Not yet implemented")
    }

    override fun createNBTTagList(): INBTTagList {
        TODO("Not yet implemented")
    }

    override fun createNBTTagString(string: String): INBTTagString {
        TODO("Not yet implemented")
    }

    override fun createNBTTagDouble(value: Double): INBTTagDouble {
        TODO("Not yet implemented")
    }

    override fun createEntityOtherPlayerMP(world: IWorldClient, GameProfile: GameProfile): IEntityOtherPlayerMP {
        TODO("Not yet implemented")
    }

    override fun createPotionEffect(id: Int, time: Int, strength: Int): IPotionEffect {
        TODO("Not yet implemented")
    }

    override fun createGuiOptions(parentScreen: IGuiScreen, gameSettings: IGameSettings): IGuiScreen {
        TODO("Not yet implemented")
    }

    override fun createGuiSelectWorld(parentScreen: IGuiScreen): IGuiScreen {
        TODO("Not yet implemented")
    }

    override fun createGuiMultiplayer(parentScreen: IGuiScreen): IGuiScreen {
        TODO("Not yet implemented")
    }

    override fun createGuiModList(parentScreen: IGuiScreen): IGuiScreen {
        TODO("Not yet implemented")
    }

    override fun createCPacketHeldItemChange(slot: Int): ICPacketHeldItemChange {
        TODO("Not yet implemented")
    }

    override fun createCPacketPlayerBlockPlacement(stack: IItemStack?): ICPacketPlayerBlockPlacement {
        TODO("Not yet implemented")
    }

    override fun createCPacketPlayerBlockPlacement(positionIn: WBlockPos, placedBlockDirectionIn: Int, stackIn: IItemStack?, facingXIn: Float, facingYIn: Float, facingZIn: Float): ICPacketPlayerBlockPlacement {
        TODO("Not yet implemented")
    }

    override fun createCPacketPlayerPosLook(x: Double, y: Double, z: Double, yaw: Float, pitch: Float, onGround: Boolean): ICPacketPlayerPosLook {
        TODO("Not yet implemented")
    }

    override fun createCPacketClientStatus(state: ICPacketClientStatus.WEnumState): ICPacketClientStatus {
        TODO("Not yet implemented")
    }

    override fun createCPacketPlayerDigging(wAction: ICPacketPlayerDigging.WAction, pos: WBlockPos, facing: IEnumFacing): ICPacketPlayerDigging {
        TODO("Not yet implemented")
    }

    override fun createCPacketPlayerPosition(x: Double, negativeInfinity: Double, z: Double, onGround: Boolean): ICPacketPlayerPosition {
        TODO("Not yet implemented")
    }

    override fun createICPacketResourcePackStatus(hash: String, status: ICPacketResourcePackStatus.WAction): ICPacketResourcePackStatus {
        TODO("Not yet implemented")
    }

    override fun createCPacketPlayerLook(yaw: Float, pitch: Float, onGround: Boolean): ICPacketPlayerLook {
        TODO("Not yet implemented")
    }

    override fun createCPacketUseEntity(player: IEntity, wAction: ICPacketUseEntity.WAction): ICPacketUseEntity {
        TODO("Not yet implemented")
    }

    override fun createCPacketUseEntity(entity: IEntity, positionVector: WVec3): ICPacketUseEntity {
        TODO("Not yet implemented")
    }

    override fun createCPacketCreativeInventoryAction(slot: Int, itemStack: IItemStack): IPacket {
        TODO("Not yet implemented")
    }

    override fun createCPacketEntityAction(player: IEntity, wAction: ICPacketEntityAction.WAction): ICPacketEntityAction {
        TODO("Not yet implemented")
    }

    override fun createCPacketCustomPayload(channel: String, payload: IPacketBuffer): IPacket {
        TODO("Not yet implemented")
    }

    override fun createCPacketCloseWindow(windowId: Int): ICPacketCloseWindow {
        TODO("Not yet implemented")
    }

    override fun createCPacketCloseWindow(): ICPacketCloseWindow {
        TODO("Not yet implemented")
    }

    override fun createCPacketPlayer(onGround: Boolean): ICPacketPlayer {
        TODO("Not yet implemented")
    }

    override fun createCPacketTabComplete(text: String): IPacket {
        TODO("Not yet implemented")
    }

    override fun createCPacketAnimation(): ICPacketAnimation {
        TODO("Not yet implemented")
    }

    override fun createCPacketKeepAlive(): ICPacketKeepAlive {
        TODO("Not yet implemented")
    }

    override fun isEntityAnimal(obj: Any?): Boolean {
        TODO("Not yet implemented")
    }

    override fun isEntitySquid(obj: Any?): Boolean {
        TODO("Not yet implemented")
    }

    override fun isEntityBat(obj: Any?): Boolean {
        TODO("Not yet implemented")
    }

    override fun isEntityGolem(obj: Any?): Boolean {
        TODO("Not yet implemented")
    }

    override fun isEntityMob(obj: Any?): Boolean {
        TODO("Not yet implemented")
    }

    override fun isEntityVillager(obj: Any?): Boolean {
        TODO("Not yet implemented")
    }

    override fun isEntitySlime(obj: Any?): Boolean {
        TODO("Not yet implemented")
    }

    override fun isEntityGhast(obj: Any?): Boolean {
        TODO("Not yet implemented")
    }

    override fun isEntityDragon(obj: Any?): Boolean {
        TODO("Not yet implemented")
    }

    override fun isEntityLivingBase(obj: Any?): Boolean {
        TODO("Not yet implemented")
    }

    override fun isEntityPlayer(obj: Any?): Boolean {
        TODO("Not yet implemented")
    }

    override fun isEntityArmorStand(it: Any?): Boolean {
        TODO("Not yet implemented")
    }

    override fun isEntityTNTPrimed(it: Any?): Boolean {
        TODO("Not yet implemented")
    }

    override fun isEntityBoat(it: Any?): Boolean {
        TODO("Not yet implemented")
    }

    override fun isEntityMinecart(it: Any?): Boolean {
        TODO("Not yet implemented")
    }

    override fun isEntityItem(it: Any?): Boolean {
        TODO("Not yet implemented")
    }

    override fun isEntityArrow(it: Any?): Boolean {
        TODO("Not yet implemented")
    }

    override fun isEntityFallingBlock(it: Any?): Boolean {
        TODO("Not yet implemented")
    }

    override fun isEntityMinecartChest(it: Any?): Boolean {
        TODO("Not yet implemented")
    }

    override fun isTileEntityChest(it: Any?): Boolean {
        TODO("Not yet implemented")
    }

    override fun isTileEntityEnderChest(it: Any?): Boolean {
        TODO("Not yet implemented")
    }

    override fun isTileEntityFurnace(it: Any?): Boolean {
        TODO("Not yet implemented")
    }

    override fun isTileEntityDispenser(it: Any?): Boolean {
        TODO("Not yet implemented")
    }

    override fun isTileEntityHopper(it: Any?): Boolean {
        TODO("Not yet implemented")
    }

    override fun isSPacketEntity(obj: Any?): Boolean {
        TODO("Not yet implemented")
    }

    override fun isSPacketResourcePackSend(obj: Any?): Boolean {
        TODO("Not yet implemented")
    }

    override fun isSPacketPlayerPosLook(obj: Any?): Boolean {
        TODO("Not yet implemented")
    }

    override fun isSPacketAnimation(obj: Any?): Boolean {
        TODO("Not yet implemented")
    }

    override fun isSPacketEntityVelocity(obj: Any?): Boolean {
        TODO("Not yet implemented")
    }

    override fun isSPacketExplosion(packet: Any?): Boolean {
        TODO("Not yet implemented")
    }

    override fun isSPacketCloseWindow(packet: Any?): Boolean {
        TODO("Not yet implemented")
    }

    override fun isSPacketTabComplete(packet: Any?): Boolean {
        TODO("Not yet implemented")
    }

    override fun isCPacketPlayer(obj: Any?): Boolean {
        TODO("Not yet implemented")
    }

    override fun isCPacketPlayerBlockPlacement(obj: Any?): Boolean {
        TODO("Not yet implemented")
    }

    override fun isCPacketUseEntity(obj: Any?): Boolean {
        TODO("Not yet implemented")
    }

    override fun isCPacketCloseWindow(obj: Any?): Boolean {
        TODO("Not yet implemented")
    }

    override fun isCPacketChatMessage(obj: Any?): Boolean {
        TODO("Not yet implemented")
    }

    override fun isCPacketKeepAlive(obj: Any?): Boolean {
        TODO("Not yet implemented")
    }

    override fun isCPacketPlayerPosition(obj: Any?): Boolean {
        TODO("Not yet implemented")
    }

    override fun isCPacketPlayerPosLook(obj: Any?): Boolean {
        TODO("Not yet implemented")
    }

    override fun isCPacketClientStatus(obj: Any?): Boolean {
        TODO("Not yet implemented")
    }

    override fun isCPacketAnimation(obj: Any?): Boolean {
        TODO("Not yet implemented")
    }

    override fun isCPacketEntityAction(obj: Any?): Boolean {
        TODO("Not yet implemented")
    }

    override fun isSPacketWindowItems(obj: Any?): Boolean {
        TODO("Not yet implemented")
    }

    override fun isCPacketHeldItemChange(obj: Any?): Boolean {
        TODO("Not yet implemented")
    }

    override fun isCPacketPlayerLook(obj: Any?): Boolean {
        TODO("Not yet implemented")
    }

    override fun isCPacketCustomPayload(obj: Any?): Boolean {
        TODO("Not yet implemented")
    }

    override fun isCPacketHandshake(obj: Any?): Boolean {
        TODO("Not yet implemented")
    }

    override fun isItemSword(item: Any?): Boolean {
        TODO("Not yet implemented")
    }

    override fun isItemTool(item: Any?): Boolean {
        TODO("Not yet implemented")
    }

    override fun isItemArmor(obj: Any?): Boolean {
        TODO("Not yet implemented")
    }

    override fun isItemPotion(item: Any?): Boolean {
        TODO("Not yet implemented")
    }

    override fun isItemBlock(item: Any?): Boolean {
        TODO("Not yet implemented")
    }

    override fun isItemBow(item: Any?): Boolean {
        TODO("Not yet implemented")
    }

    override fun isItemBucket(item: Any?): Boolean {
        TODO("Not yet implemented")
    }

    override fun isItemFood(item: Any?): Boolean {
        TODO("Not yet implemented")
    }

    override fun isItemBucketMilk(item: Any?): Boolean {
        TODO("Not yet implemented")
    }

    override fun isItemPickaxe(obj: Any?): Boolean {
        TODO("Not yet implemented")
    }

    override fun isItemAxe(obj: Any?): Boolean {
        TODO("Not yet implemented")
    }

    override fun isItemBed(obj: Any?): Boolean {
        TODO("Not yet implemented")
    }

    override fun isItemEnderPearl(obj: Any?): Boolean {
        TODO("Not yet implemented")
    }

    override fun isItemEnchantedBook(obj: Any?): Boolean {
        TODO("Not yet implemented")
    }

    override fun isItemBoat(obj: Any?): Boolean {
        TODO("Not yet implemented")
    }

    override fun isItemMinecart(obj: Any?): Boolean {
        TODO("Not yet implemented")
    }

    override fun isItemAppleGold(obj: Any?): Boolean {
        TODO("Not yet implemented")
    }

    override fun isItemSnowball(obj: Any?): Boolean {
        TODO("Not yet implemented")
    }

    override fun isItemEgg(obj: Any?): Boolean {
        TODO("Not yet implemented")
    }

    override fun isBlockAir(item: Any?): Boolean {
        TODO("Not yet implemented")
    }

    override fun isBlockFence(item: Any?): Boolean {
        TODO("Not yet implemented")
    }

    override fun isBlockSnow(item: Any?): Boolean {
        TODO("Not yet implemented")
    }

    override fun isBlockLadder(item: Any?): Boolean {
        TODO("Not yet implemented")
    }

    override fun isBlockVine(item: Any?): Boolean {
        TODO("Not yet implemented")
    }

    override fun isBlockSlime(item: Any?): Boolean {
        TODO("Not yet implemented")
    }

    override fun isBlockSlab(item: Any?): Boolean {
        TODO("Not yet implemented")
    }

    override fun isBlockStairs(item: Any?): Boolean {
        TODO("Not yet implemented")
    }

    override fun isBlockCarpet(obj: Any?): Boolean {
        TODO("Not yet implemented")
    }

    override fun isBlockPane(obj: Any?): Boolean {
        TODO("Not yet implemented")
    }

    override fun isBlockLiquid(obj: Any?): Boolean {
        TODO("Not yet implemented")
    }

    override fun isBlockCactus(obj: Any?): Boolean {
        TODO("Not yet implemented")
    }

    override fun isBlockBedrock(obj: Any?): Boolean {
        TODO("Not yet implemented")
    }

    override fun isItemFishingRod(obj: Any?): Boolean {
        TODO("Not yet implemented")
    }

    override fun isGuiInventory(obj: Any?): Boolean {
        TODO("Not yet implemented")
    }

    override fun isGuiContainer(obj: Any?): Boolean {
        TODO("Not yet implemented")
    }

    override fun isGuiGameOver(obj: Any?): Boolean {
        TODO("Not yet implemented")
    }

    override fun isGuiChat(obj: Any?): Boolean {
        TODO("Not yet implemented")
    }

    override fun isGuiIngameMenu(obj: Any?): Boolean {
        TODO("Not yet implemented")
    }

    override fun isGuiChest(obj: Any?): Boolean {
        TODO("Not yet implemented")
    }

    override fun isGuiHudDesigner(obj: Any?): Boolean {
        TODO("Not yet implemented")
    }

    override fun isClickGui(obj: Any?): Boolean {
        TODO("Not yet implemented")
    }

    override fun isFontRenderer(item: Any?): Boolean = item is FontRenderer

    override fun getPotionEnum(type: PotionType): IPotion {
        TODO("Not yet implemented")
    }

    override fun getEnumFacing(type: EnumFacingType): IEnumFacing {
        TODO("Not yet implemented")
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
        TODO("Not yet implemented")
    }

    override fun getStatEnum(type: StatType): IStatBase {
        TODO("Not yet implemented")
    }

    override fun getItemEnum(type: ItemType): IItem {
        TODO("Not yet implemented")
    }

    override fun getEnchantmentEnum(type: EnchantmentType): IEnchantment {
        TODO("Not yet implemented")
    }

    override fun getVertexFormatEnum(type: WDefaultVertexFormats): IVertexFormat {
        TODO("Not yet implemented")
    }

    override fun wrapFontRenderer(fontRenderer: IWrappedFontRenderer): IFontRenderer {
        TODO("Not yet implemented")
    }

    override fun wrapGuiScreen(clickGui: WrappedGuiScreen): IGuiScreen {
        TODO("Not yet implemented")
    }

    override fun createSafeVertexBuffer(vertexFormat: IVertexFormat): IVertexBuffer {
        TODO("Not yet implemented")
    }

}