/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.gui;

import java.util.List;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.api.minecraft.client.gui.IFontRenderer;
import net.ccbluex.liquidbounce.features.module.modules.render.HUD;
import net.ccbluex.liquidbounce.ui.font.Fonts;
import net.ccbluex.liquidbounce.utils.render.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer.EnumChatVisibility;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.MathHelper;

import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GuiNewChat.class)
public abstract class MixinGuiNewChat
{

    @Shadow
    @Final
    private Minecraft mc;
    @Shadow
    @Final
    private List<ChatLine> drawnChatLines;
    @Shadow
    private int scrollPos;
    @Shadow
    private boolean isScrolled;

    @Shadow
    public abstract int getLineCount();

    @Shadow
    public abstract boolean getChatOpen();

    @Shadow
    public abstract float getChatScale();

    @Shadow
    public abstract int getChatWidth();

    @Inject(method = "drawChat", at = @At("HEAD"), cancellable = true)
    private void drawChat(final int updateCounter, final CallbackInfo callbackInfo)
    {
        final HUD hud = (HUD) LiquidBounce.moduleManager.get(HUD.class);

        if (hud.getState() && hud.getFontChatValue().get())
        {
            final IFontRenderer font = hud.getChatFontValue().get();
            callbackInfo.cancel();
            if (mc.gameSettings.chatVisibility != EnumChatVisibility.HIDDEN)
            {
                final int lineCount = getLineCount();
                boolean chatOpen = false;
                int lvt_4_1_ = 0;
                final int drawnChatLinesSize = drawnChatLines.size();
                final float chatOpacity = mc.gameSettings.chatOpacity * 0.9F + 0.1F;

                if (drawnChatLinesSize > 0)
                {
                    if (getChatOpen())
                        chatOpen = true;

                    final float chatScale = getChatScale();
                    final int chatWidthRatio = MathHelper.ceiling_float_int(getChatWidth() / chatScale);

                    GlStateManager.pushMatrix();
                    GlStateManager.translate(2.0F, 20.0F, 0.0F);
                    GlStateManager.scale(chatScale, chatScale, 1.0F);

                    int currentLine;
                    int updateCounterDelta;
                    int currentLineOpacity;
                    final int chatlineSize = drawnChatLines.size();

                    for (currentLine = 0; currentLine + scrollPos < chatlineSize && currentLine < lineCount; ++currentLine)
                    {
                        final ChatLine lvt_10_1_ = drawnChatLines.get(currentLine + scrollPos);
                        if (lvt_10_1_ != null)
                        {
                            updateCounterDelta = updateCounter - lvt_10_1_.getUpdatedCounter();
                            if (updateCounterDelta < 200 || chatOpen)
                            {
                                double updatedPercentage = updateCounterDelta * 0.005;
                                updatedPercentage = 1.0D - updatedPercentage;
                                updatedPercentage *= 10.0D;
                                updatedPercentage = MathHelper.clamp_double(updatedPercentage, 0.0D, 1.0D);
                                updatedPercentage *= updatedPercentage;
                                currentLineOpacity = (int) (255.0D * updatedPercentage);
                                if (chatOpen)
                                    currentLineOpacity = 255;

                                currentLineOpacity *= chatOpacity;
                                ++lvt_4_1_;
                                if (currentLineOpacity > 3)
                                {
                                    final int xPos = 0;
                                    final int yPos = -currentLine * 9;

                                    Gui.drawRect(xPos, yPos - 9, xPos + chatWidthRatio + 4, yPos, currentLineOpacity >> 1 << 24);

                                    final String formattedText = lvt_10_1_.getChatComponent().getFormattedText();

                                    font.drawStringWithShadow(formattedText, xPos + 2, yPos - 8, 0xffffff + (currentLineOpacity << 24));
                                    RenderUtils.resetColor();
                                    GlStateManager.resetColor();
                                }
                            }
                        }
                    }

                    if (chatOpen)
                    {
                        currentLine = Fonts.font40.getFontHeight();

                        GlStateManager.translate(-3.0F, 0.0F, 0.0F);

                        final int lvt_10_2_ = drawnChatLinesSize * currentLine + drawnChatLinesSize;
                        updateCounterDelta = lvt_4_1_ * currentLine + lvt_4_1_;
                        final int lvt_12_2_ = scrollPos * updateCounterDelta / drawnChatLinesSize;
                        final int lvt_13_1_ = updateCounterDelta * updateCounterDelta / lvt_10_2_;

                        if (lvt_10_2_ != updateCounterDelta)
                        {
                            currentLineOpacity = lvt_12_2_ > 0 ? 170 : 96;
                            final int lvt_15_2_ = isScrolled ? 13382451 : 3355562;

                            Gui.drawRect(0, -lvt_12_2_, 2, -lvt_12_2_ - lvt_13_1_, lvt_15_2_ + (currentLineOpacity << 24));
                            Gui.drawRect(2, -lvt_12_2_, 1, -lvt_12_2_ - lvt_13_1_, 13421772 + (currentLineOpacity << 24));
                        }
                    }

                    GlStateManager.popMatrix();
                }
            }
        }
    }

    // TODO: Make real fix
    /*
     * @Inject(method = "setChatLine", at = @At("HEAD"), cancellable = true) private void setChatLine(IChatComponent p_setChatLine_1_, int p_setChatLine_2_, int p_setChatLine_3_, boolean p_setChatLine_4_, final CallbackInfo callbackInfo) { final HUD hud =
     * (HUD) LiquidBounce.moduleManager.get(HUD.class);
     * 
     * if(hud.getState() && hud.fontChatValue.asBoolean()) { callbackInfo.cancel();
     * 
     * if (p_setChatLine_2_ != 0) { this.deleteChatLine(p_setChatLine_2_); }
     * 
     * int lvt_5_1_ = MathHelper.floor_float((float)this.getChatWidth() / this.getChatScale()); List<IChatComponent> lvt_6_1_ = GuiUtilRenderComponents.splitText(p_setChatLine_1_, lvt_5_1_, Fonts.font40, false, false); boolean lvt_7_1_ =
     * this.getChatOpen();
     * 
     * IChatComponent lvt_9_1_; for(Iterator lvt_8_1_ = lvt_6_1_.iterator(); lvt_8_1_.hasNext(); this.drawnChatLines.add(0, new ChatLine(p_setChatLine_3_, lvt_9_1_, p_setChatLine_2_))) { lvt_9_1_ = (IChatComponent)lvt_8_1_.next(); if (lvt_7_1_ &&
     * this.scrollPos > 0) { this.isScrolled = true; this.scroll(1); } }
     * 
     * while(this.drawnChatLines.size() > 100) { this.drawnChatLines.remove(this.drawnChatLines.size() - 1); }
     * 
     * if (!p_setChatLine_4_) { this.chatLines.add(0, new ChatLine(p_setChatLine_3_, p_setChatLine_1_, p_setChatLine_2_));
     * 
     * while(this.chatLines.size() > 100) { this.chatLines.remove(this.chatLines.size() - 1); } } } }
     */

    @Inject(method = "getChatComponent", at = @At("HEAD"), cancellable = true)
    private void getChatComponent(final int p_getChatComponent_1_, final int p_getChatComponent_2_, final CallbackInfoReturnable<? super IChatComponent> callbackInfo)
    {
        final HUD hud = (HUD) LiquidBounce.moduleManager.get(HUD.class);

        if (hud.getState() && hud.getFontChatValue().get())
        {
            final IFontRenderer font = hud.getChatFontValue().get();
            if (getChatOpen())
            {
                final ScaledResolution lvt_3_1_ = new ScaledResolution(mc);
                final int lvt_4_1_ = lvt_3_1_.getScaleFactor();
                final float lvt_5_1_ = getChatScale();
                int lvt_6_1_ = p_getChatComponent_1_ / lvt_4_1_ - 3;
                lvt_6_1_ = MathHelper.floor_float(lvt_6_1_ / lvt_5_1_);

                int lvt_7_1_ = p_getChatComponent_2_ / lvt_4_1_ - 27;
                lvt_7_1_ = MathHelper.floor_float(lvt_7_1_ / lvt_5_1_);

                if (lvt_6_1_ >= 0 && lvt_7_1_ >= 0)
                {
                    final int lvt_8_1_ = Math.min(getLineCount(), drawnChatLines.size());

                    if (lvt_6_1_ <= MathHelper.floor_float(getChatWidth() / getChatScale()) && lvt_7_1_ < Fonts.font40.getFontHeight() * lvt_8_1_ + lvt_8_1_)
                    {
                        final int lvt_9_1_ = lvt_7_1_ / font.getFontHeight() + scrollPos;

                        if (lvt_9_1_ >= 0 && lvt_9_1_ < drawnChatLines.size())
                        {
                            final ChatLine lvt_10_1_ = drawnChatLines.get(lvt_9_1_);
                            int lvt_11_1_ = 0;

                            for (final IChatComponent lvt_13_1_ : lvt_10_1_.getChatComponent())
                                if (lvt_13_1_ instanceof ChatComponentText)
                                {
                                    lvt_11_1_ += font.getStringWidth(GuiUtilRenderComponents.func_178909_a(((ChatComponentText) lvt_13_1_).getChatComponentText_TextValue(), false));

                                    if (lvt_11_1_ > lvt_6_1_)
                                    {
                                        callbackInfo.setReturnValue(lvt_13_1_);
                                        return;
                                    }
                                }
                        }

                    }
                }

            }

            callbackInfo.setReturnValue(null);
        }
    }
}
