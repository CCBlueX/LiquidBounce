/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.gui;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.event.EventManager;
import net.ccbluex.liquidbounce.event.Render2DEvent;
import net.ccbluex.liquidbounce.features.module.modules.render.AntiBlind;
import net.ccbluex.liquidbounce.features.module.modules.render.HUD;
import net.ccbluex.liquidbounce.features.module.modules.render.NoScoreboard;
import net.ccbluex.liquidbounce.ui.font.AWTFontRenderer;
import net.ccbluex.liquidbounce.utils.ClassUtils;
import net.ccbluex.liquidbounce.utils.render.FakeItemRender;
import net.ccbluex.liquidbounce.utils.render.RenderUtils;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.gui.GuiStreamIndicator;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static net.ccbluex.liquidbounce.utils.MinecraftInstance.mc;
import static net.minecraft.client.renderer.GlStateManager.*;
import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.glEnable;

@Mixin(GuiIngame.class)
@SideOnly(Side.CLIENT)
public abstract class MixinGuiInGame extends Gui {


    @Shadow
    @Final
    protected static ResourceLocation widgetsTexPath = new ResourceLocation("textures/gui/widgets.png");

    @Shadow
    protected int recordPlayingUpFor;
    @Shadow
    protected int titlesTimer;
    @Shadow
    protected String displayedTitle = "";
    @Shadow
    protected String displayedSubTitle = "";
    @Shadow
    protected int updateCounter;
    @Shadow
    @Final
    protected GuiStreamIndicator streamIndicator;
    @Shadow
    protected int remainingHighlightTicks;
    @Shadow
    protected ItemStack highlightingItemStack;

    @Shadow
    protected abstract void renderHotbarItem(int index, int xPos, int yPos, float partialTicks, EntityPlayer player);

    @Inject(method = "renderScoreboard", at = @At("HEAD"), cancellable = true)
    private void renderScoreboard(CallbackInfo callbackInfo) {
        if (HUD.INSTANCE.handleEvents() || NoScoreboard.INSTANCE.handleEvents())
            callbackInfo.cancel();
    }

    /**
     * @author SuperSkidder
     * @reason keep fake item highlight name
     */
    @Overwrite
    public void updateTick() {
        if (this.recordPlayingUpFor > 0) {
            --this.recordPlayingUpFor;
        }

        if (this.titlesTimer > 0) {
            --this.titlesTimer;
            if (this.titlesTimer <= 0) {
                this.displayedTitle = "";
                this.displayedSubTitle = "";
            }
        }

        ++this.updateCounter;
        this.streamIndicator.updateStreamAlpha();
        if (mc.thePlayer != null) {
            int slot = mc.thePlayer.inventory.currentItem;
            if (FakeItemRender.INSTANCE.getFakeItem() != -1) {
                slot = FakeItemRender.INSTANCE.getFakeItem();
            }
            ItemStack lvt_1_1_ = mc.thePlayer.inventory.getStackInSlot(slot);

            if (lvt_1_1_ == null) {
                this.remainingHighlightTicks = 0;
            } else if (this.highlightingItemStack == null || lvt_1_1_.getItem() != this.highlightingItemStack.getItem() || !ItemStack.areItemStackTagsEqual(lvt_1_1_, this.highlightingItemStack) || !lvt_1_1_.isItemStackDamageable() && lvt_1_1_.getMetadata() != this.highlightingItemStack.getMetadata()) {
                this.remainingHighlightTicks = 40;
            } else if (this.remainingHighlightTicks > 0) {
                --this.remainingHighlightTicks;
            }

            this.highlightingItemStack = lvt_1_1_;
        }

    }

    /**
     * @author CCBlueX & SuperSkidder
     * @reason custom hotbar and fake item
     */
    @Overwrite
    protected void renderTooltip(ScaledResolution sr, float partialTicks) {
        final HUD hud = HUD.INSTANCE;

        if (mc.getRenderViewEntity() instanceof EntityPlayer) {
            EntityPlayer entityPlayer = (EntityPlayer) mc.getRenderViewEntity();
            int slot = entityPlayer.inventory.currentItem;
            if (FakeItemRender.INSTANCE.getFakeItem() != -1) {
                slot = FakeItemRender.INSTANCE.getFakeItem();
            }
            if (hud.handleEvents() && hud.getBlackHotbar()) {
                int middleScreen = sr.getScaledWidth() / 2;

                color(1f, 1f, 1f, 1f);
                RenderUtils.INSTANCE.drawRoundedRectInt(middleScreen - 91, sr.getScaledHeight() - 24, middleScreen + 90, sr.getScaledHeight(), Integer.MIN_VALUE, 4f);
                RenderUtils.INSTANCE.drawRoundedRectInt(middleScreen - 91 - 1 + slot * 20 + 1, sr.getScaledHeight() - 24, middleScreen - 91 - 1 + slot * 20 + 22, sr.getScaledHeight() - 22 - 1 + 24, Integer.MAX_VALUE, 4f);

                enableRescaleNormal();
                glEnable(GL_BLEND);
                tryBlendFuncSeparate(770, 771, 1, 0);
                RenderHelper.enableGUIStandardItemLighting();

                for (int j = 0; j < 9; ++j) {
                    int k = sr.getScaledWidth() / 2 - 90 + j * 20 + 2;
                    int l = sr.getScaledHeight() - 16 - 3;
                    renderHotbarItem(j, k, l, partialTicks, entityPlayer);
                }

                RenderHelper.disableStandardItemLighting();
                disableRescaleNormal();
                disableBlend();

                EventManager.INSTANCE.callEvent(new Render2DEvent(partialTicks));
                AWTFontRenderer.Companion.garbageCollectionTick();
            } else {
                GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                mc.getTextureManager().bindTexture(widgetsTexPath);
                int lvt_4_1_ = sr.getScaledWidth() / 2;
                float lvt_5_1_ = this.zLevel;
                this.zLevel = -90.0F;
                this.drawTexturedModalRect(lvt_4_1_ - 91, sr.getScaledHeight() - 22, 0, 0, 182, 22);
                this.drawTexturedModalRect(lvt_4_1_ - 91 - 1 + slot * 20, sr.getScaledHeight() - 22 - 1, 0, 22, 24, 22);
                this.zLevel = lvt_5_1_;
                GlStateManager.enableRescaleNormal();
                GlStateManager.enableBlend();
                GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
                RenderHelper.enableGUIStandardItemLighting();

                for (int lvt_6_1_ = 0; lvt_6_1_ < 9; ++lvt_6_1_) {
                    int lvt_7_1_ = sr.getScaledWidth() / 2 - 90 + lvt_6_1_ * 20 + 2;
                    int lvt_8_1_ = sr.getScaledHeight() - 16 - 3;
                    this.renderHotbarItem(lvt_6_1_, lvt_7_1_, lvt_8_1_, partialTicks, entityPlayer);
                }

                RenderHelper.disableStandardItemLighting();
                GlStateManager.disableRescaleNormal();
                GlStateManager.disableBlend();
            }
        }
    }

    @Inject(method = "renderTooltip", at = @At("RETURN"))
    private void renderTooltipPost(ScaledResolution sr, float partialTicks, CallbackInfo callbackInfo) {
        if (!ClassUtils.INSTANCE.hasClass("net.labymod.api.LabyModAPI")) {
            EventManager.INSTANCE.callEvent(new Render2DEvent(partialTicks));
            AWTFontRenderer.Companion.garbageCollectionTick();
        }
    }

    @Inject(method = "renderPumpkinOverlay", at = @At("HEAD"), cancellable = true)
    private void renderPumpkinOverlay(final CallbackInfo callbackInfo) {
        final AntiBlind antiBlind = AntiBlind.INSTANCE;

        if (antiBlind.handleEvents() && antiBlind.getPumpkinEffect())
            callbackInfo.cancel();
    }

    @Inject(method = "renderBossHealth", at = @At("HEAD"), cancellable = true)
    private void renderBossHealth(CallbackInfo callbackInfo) {
        final AntiBlind antiBlind = AntiBlind.INSTANCE;

        if (antiBlind.handleEvents() && antiBlind.getBossHealth())
            callbackInfo.cancel();
    }
}
