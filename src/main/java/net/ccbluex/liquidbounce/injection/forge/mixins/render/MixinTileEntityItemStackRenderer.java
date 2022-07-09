/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.render;

import com.mojang.authlib.GameProfile;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntityItemStackRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.tileentity.TileEntitySkullRenderer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.tileentity.TileEntityBanner;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityEnderChest;
import net.minecraft.tileentity.TileEntitySkull;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(TileEntityItemStackRenderer.class)
@SideOnly(Side.CLIENT)
public class MixinTileEntityItemStackRenderer
{
    @Shadow
    private TileEntityBanner banner;

    @Shadow
    private TileEntityEnderChest enderChest;

    @Shadow
    private TileEntityChest field_147718_c;

    @Shadow
    private TileEntityChest field_147717_b;

    /**
     * @author CCBlueX
     * @reason
     */
    @Overwrite
    public void renderByItem(final ItemStack itemStackIn)
    {
        if (itemStackIn.getItem() == Items.banner)
        {
            banner.setItemValues(itemStackIn);
            TileEntityRendererDispatcher.instance.renderTileEntityAt(banner, 0.0D, 0.0D, 0.0D, 0.0F);
        }
        else if (itemStackIn.getItem() == Items.skull)
        {
            GameProfile gameprofile = null;

            if (itemStackIn.hasTagCompound())
            {
                final NBTTagCompound nbttagcompound = itemStackIn.getTagCompound();

                try
                {
                    if (nbttagcompound.hasKey("SkullOwner", 10))
                        gameprofile = NBTUtil.readGameProfileFromNBT(nbttagcompound.getCompoundTag("SkullOwner"));
                    else if (nbttagcompound.hasKey("SkullOwner", 8) && !nbttagcompound.getString("SkullOwner").isEmpty())
                    {
                        final GameProfile skullOwnerProfile = new GameProfile(null, nbttagcompound.getString("SkullOwner"));
                        gameprofile = TileEntitySkull.updateGameprofile(skullOwnerProfile);
                        nbttagcompound.removeTag("SkullOwner");
                        nbttagcompound.setTag("SkullOwner", NBTUtil.writeGameProfile(new NBTTagCompound(), gameprofile));
                    }
                }
                catch (final Exception ignored)
                {
                }
            }

            if (TileEntitySkullRenderer.instance != null)
            {
                GlStateManager.pushMatrix();
                GlStateManager.translate(-0.5F, 0.0F, -0.5F);
                GlStateManager.scale(2.0F, 2.0F, 2.0F);
                GlStateManager.disableCull();
                TileEntitySkullRenderer.instance.renderSkull(0.0F, 0.0F, 0.0F, EnumFacing.UP, 0.0F, itemStackIn.getMetadata(), gameprofile, -1);
                GlStateManager.enableCull();
                GlStateManager.popMatrix();
            }
        }
        else
        {
            final Block block = Block.getBlockFromItem(itemStackIn.getItem());

            if (block == Blocks.ender_chest)
                TileEntityRendererDispatcher.instance.renderTileEntityAt(enderChest, 0.0D, 0.0D, 0.0D, 0.0F);
            else if (block == Blocks.trapped_chest)
                TileEntityRendererDispatcher.instance.renderTileEntityAt(field_147718_c, 0.0D, 0.0D, 0.0D, 0.0F);
            else if (block == Blocks.chest)
                TileEntityRendererDispatcher.instance.renderTileEntityAt(field_147717_b, 0.0D, 0.0D, 0.0D, 0.0F);
            else
                ForgeHooksClient.renderTileItem(itemStackIn.getItem(), itemStackIn.getMetadata());
        }
    }
}
