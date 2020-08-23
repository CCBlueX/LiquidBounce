/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.render;

import net.minecraft.client.renderer.tileentity.TileEntityItemStackRenderer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(TileEntityItemStackRenderer.class)
@SideOnly(Side.CLIENT)
public class MixinTileEntityItemStackRenderer {

    // TODO Fix this (p0)
//    @Shadow
//    private TileEntityBanner banner;
//
//    @Shadow
//    private TileEntityEnderChest enderChest;
//
//    @Shadow
//    private TileEntityChest field_147718_c;
//
//    @Shadow
//    private TileEntityChest field_147717_b;
//
//    /**
//     * @author CCBlueX
//     */
//    @Overwrite
//    public void renderByItem(ItemStack itemStackIn) {
//        if(itemStackIn.getItem() == Items.banner) {
//            this.banner.setItemValues(itemStackIn);
//            TileEntityRendererDispatcher.instance.renderTileEntityAt(this.banner, 0.0D, 0.0D, 0.0D, 0.0F);
//        }else if(itemStackIn.getItem() == Items.skull) {
//            GameProfile gameprofile = null;
//
//            if(itemStackIn.hasTagCompound()) {
//                NBTTagCompound nbttagcompound = itemStackIn.getTagCompound();
//
//                try {
//                    if(nbttagcompound.hasKey("SkullOwner", 10)) {
//                        gameprofile = NBTUtil.readGameProfileFromNBT(nbttagcompound.getCompoundTag("SkullOwner"));
//                    }else if(nbttagcompound.hasKey("SkullOwner", 8) && nbttagcompound.getString("SkullOwner").length() > 0) {
//                        GameProfile lvt_2_2_ = new GameProfile(null, nbttagcompound.getString("SkullOwner"));
//                        gameprofile = TileEntitySkull.updateGameprofile(lvt_2_2_);
//                        nbttagcompound.removeTag("SkullOwner");
//                        nbttagcompound.setTag("SkullOwner", NBTUtil.writeGameProfile(new NBTTagCompound(), gameprofile));
//                    }
//                }catch(Exception ignored) {
//                }
//            }
//
//            if(TileEntitySkullRenderer.instance != null) {
//                GlStateManager.pushMatrix();
//                GlStateManager.translate(-0.5F, 0.0F, -0.5F);
//                GlStateManager.scale(2.0F, 2.0F, 2.0F);
//                GlStateManager.disableCull();
//                TileEntitySkullRenderer.instance.renderSkull(0.0F, 0.0F, 0.0F, EnumFacing.UP, 0.0F, itemStackIn.getMetadata(), gameprofile, -1);
//                GlStateManager.enableCull();
//                GlStateManager.popMatrix();
//            }
//        }else{
//            Block block = Block.getBlockFromItem(itemStackIn.getItem());
//
//            if(block == Blocks.ender_chest) {
//                TileEntityRendererDispatcher.instance.renderTileEntityAt(this.enderChest, 0.0D, 0.0D, 0.0D, 0.0F);
//            }else if(block == Blocks.trapped_chest) {
//                TileEntityRendererDispatcher.instance.renderTileEntityAt(this.field_147718_c, 0.0D, 0.0D, 0.0D, 0.0F);
//            }else if(block != Blocks.chest)
//                net.minecraftforge.client.ForgeHooksClient.renderTileItem(itemStackIn.getItem(), itemStackIn.getMetadata());
//            else{
//                TileEntityRendererDispatcher.instance.renderTileEntityAt(this.field_147717_b, 0.0D, 0.0D, 0.0D, 0.0F);
//            }
//        }
//    }
}