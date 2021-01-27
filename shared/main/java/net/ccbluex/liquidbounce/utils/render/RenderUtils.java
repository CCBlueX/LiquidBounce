/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.render;

import static java.lang.Math.*;
import static org.lwjgl.opengl.GL11.*;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

import net.ccbluex.liquidbounce.api.enums.WDefaultVertexFormats;
import net.ccbluex.liquidbounce.api.minecraft.client.block.IBlock;
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntity;
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntityLivingBase;
import net.ccbluex.liquidbounce.api.minecraft.client.entity.player.IEntityPlayer;
import net.ccbluex.liquidbounce.api.minecraft.client.render.ITessellator;
import net.ccbluex.liquidbounce.api.minecraft.client.render.IWorldRenderer;
import net.ccbluex.liquidbounce.api.minecraft.renderer.entity.IRenderManager;
import net.ccbluex.liquidbounce.api.minecraft.util.*;
import net.ccbluex.liquidbounce.injection.backend.Backend;
import net.ccbluex.liquidbounce.ui.font.Fonts;
import net.ccbluex.liquidbounce.utils.MinecraftInstance;
import net.ccbluex.liquidbounce.utils.block.BlockUtils;

import org.lwjgl.opengl.GL14;

public final class RenderUtils extends MinecraftInstance
{
	private static final Map<Integer, Boolean> glCapMap = new HashMap<>();
	private static final int[] DISPLAY_LISTS_2D = new int[4];
	public static int deltaTime;

	static
	{
		for (int i = 0, j = DISPLAY_LISTS_2D.length; i < j; i++)
			DISPLAY_LISTS_2D[i] = glGenLists(1);

		glNewList(DISPLAY_LISTS_2D[0], GL_COMPILE);

		quickDrawRect(-7.00F, 2.00F, -4.00F, 3.00F);
		quickDrawRect(4.00F, 2.00F, 7.00F, 3.00F);
		quickDrawRect(-7.00F, 0.5F, -6.00F, 3.00F);
		quickDrawRect(6.00F, 0.5F, 7.00F, 3.00F);

		glEndList();

		glNewList(DISPLAY_LISTS_2D[1], GL_COMPILE);

		quickDrawRect(-7.00F, 3.00F, -4.00F, 3.3F);
		quickDrawRect(4.00F, 3.00F, 7.00F, 3.3F);
		quickDrawRect(-7.3F, 0.5F, -7.00F, 3.3F);
		quickDrawRect(7.00F, 0.5F, 7.3F, 3.3F);

		glEndList();

		glNewList(DISPLAY_LISTS_2D[2], GL_COMPILE);

		quickDrawRect(4.00F, -20.00F, 7.00F, -19.00F);
		quickDrawRect(-7.00F, -20.00F, -4.00F, -19.00F);
		quickDrawRect(6.00F, -20.00F, 7.00F, -17.5F);
		quickDrawRect(-7.00F, -20.00F, -6.00F, -17.5F);

		glEndList();

		glNewList(DISPLAY_LISTS_2D[3], GL_COMPILE);

		quickDrawRect(7.00F, -20.00F, 7.3F, -17.5F);
		quickDrawRect(-7.3F, -20.00F, -7.00F, -17.5F);
		quickDrawRect(4.00F, -20.3F, 7.3F, -20.00F);
		quickDrawRect(-7.3F, -20.3F, -4.00F, -20.00F);

		glEndList();
	}

	public static void drawBlockBox(final WBlockPos blockPos, final Color color, final boolean outline)
	{
		final IRenderManager renderManager = mc.getRenderManager();
		final ITimer timer = mc.getTimer();

		final double x = blockPos.getX() - renderManager.getRenderPosX();
		final double y = blockPos.getY() - renderManager.getRenderPosY();
		final double z = blockPos.getZ() - renderManager.getRenderPosZ();

		IAxisAlignedBB axisAlignedBB = classProvider.createAxisAlignedBB(x, y, z, x + 1.0, y + 1.0, z + 1.0);
		final IBlock block = BlockUtils.getBlock(blockPos);

		if (block != null)
		{
			final IEntityPlayer player = mc.getThePlayer();

			final double posX = player.getLastTickPosX() + (player.getPosX() - player.getLastTickPosX()) * timer.getRenderPartialTicks();
			final double posY = player.getLastTickPosY() + (player.getPosY() - player.getLastTickPosY()) * timer.getRenderPartialTicks();
			final double posZ = player.getLastTickPosZ() + (player.getPosZ() - player.getLastTickPosZ()) * timer.getRenderPartialTicks();

			if (Backend.MINECRAFT_VERSION_MINOR < 12)
				block.setBlockBoundsBasedOnState(mc.getTheWorld(), blockPos);

			axisAlignedBB = block.getSelectedBoundingBox(mc.getTheWorld(), mc.getTheWorld().getBlockState(blockPos), blockPos).expand(0.0020000000949949026D, 0.0020000000949949026D, 0.0020000000949949026D).offset(-posX, -posY, -posZ);
		}

		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		enableGlCap(GL_BLEND);
		disableGlCap(GL_TEXTURE_2D, GL_DEPTH_TEST);
		glDepthMask(false);

		glColor(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha() == 255 ? outline ? 26 : 35 : color.getAlpha());
		drawFilledBox(axisAlignedBB);

		if (outline)
		{
			glLineWidth(1.00F);
			enableGlCap(GL_LINE_SMOOTH);
			glColor(color);

			drawSelectionBoundingBox(axisAlignedBB);
		}

		glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
		glDepthMask(true);
		resetCaps();
	}

	public static void drawSelectionBoundingBox(final IAxisAlignedBB boundingBox)
	{
		final ITessellator tessellator = classProvider.getTessellatorInstance();
		final IWorldRenderer worldrenderer = tessellator.getWorldRenderer();

		worldrenderer.begin(GL_LINE_STRIP, classProvider.getVertexFormatEnum(WDefaultVertexFormats.POSITION));

		// Lower Rectangle
		worldrenderer.pos(boundingBox.getMinX(), boundingBox.getMinY(), boundingBox.getMinZ()).endVertex();
		worldrenderer.pos(boundingBox.getMinX(), boundingBox.getMinY(), boundingBox.getMaxZ()).endVertex();
		worldrenderer.pos(boundingBox.getMaxX(), boundingBox.getMinY(), boundingBox.getMaxZ()).endVertex();
		worldrenderer.pos(boundingBox.getMaxX(), boundingBox.getMinY(), boundingBox.getMinZ()).endVertex();
		worldrenderer.pos(boundingBox.getMinX(), boundingBox.getMinY(), boundingBox.getMinZ()).endVertex();

		// Upper Rectangle
		worldrenderer.pos(boundingBox.getMinX(), boundingBox.getMaxY(), boundingBox.getMinZ()).endVertex();
		worldrenderer.pos(boundingBox.getMinX(), boundingBox.getMaxY(), boundingBox.getMaxZ()).endVertex();
		worldrenderer.pos(boundingBox.getMaxX(), boundingBox.getMaxY(), boundingBox.getMaxZ()).endVertex();
		worldrenderer.pos(boundingBox.getMaxX(), boundingBox.getMaxY(), boundingBox.getMinZ()).endVertex();
		worldrenderer.pos(boundingBox.getMinX(), boundingBox.getMaxY(), boundingBox.getMinZ()).endVertex();

		// Upper Rectangle
		worldrenderer.pos(boundingBox.getMinX(), boundingBox.getMaxY(), boundingBox.getMaxZ()).endVertex();
		worldrenderer.pos(boundingBox.getMinX(), boundingBox.getMinY(), boundingBox.getMaxZ()).endVertex();

		worldrenderer.pos(boundingBox.getMaxX(), boundingBox.getMinY(), boundingBox.getMaxZ()).endVertex();
		worldrenderer.pos(boundingBox.getMaxX(), boundingBox.getMaxY(), boundingBox.getMaxZ()).endVertex();

		worldrenderer.pos(boundingBox.getMaxX(), boundingBox.getMaxY(), boundingBox.getMinZ()).endVertex();
		worldrenderer.pos(boundingBox.getMaxX(), boundingBox.getMinY(), boundingBox.getMinZ()).endVertex();

		tessellator.draw();
	}

	public static void drawEntityBox(final IEntity entity, final Color color, final boolean outline)
	{
		final IRenderManager renderManager = mc.getRenderManager();
		final ITimer timer = mc.getTimer();

		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		enableGlCap(GL_BLEND);
		disableGlCap(GL_TEXTURE_2D, GL_DEPTH_TEST);
		glDepthMask(false);

		final double x = entity.getLastTickPosX() + (entity.getPosX() - entity.getLastTickPosX()) * timer.getRenderPartialTicks() - renderManager.getRenderPosX();
		final double y = entity.getLastTickPosY() + (entity.getPosY() - entity.getLastTickPosY()) * timer.getRenderPartialTicks() - renderManager.getRenderPosY();
		final double z = entity.getLastTickPosZ() + (entity.getPosZ() - entity.getLastTickPosZ()) * timer.getRenderPartialTicks() - renderManager.getRenderPosZ();

		final IAxisAlignedBB entityBox = entity.getEntityBoundingBox();
		final IAxisAlignedBB axisAlignedBB = classProvider.createAxisAlignedBB(entityBox.getMinX() - entity.getPosX() + x - 0.05D, entityBox.getMinY() - entity.getPosY() + y, entityBox.getMinZ() - entity.getPosZ() + z - 0.05D, entityBox.getMaxX() - entity.getPosX() + x + 0.05D, entityBox.getMaxY() - entity.getPosY() + y + 0.15D, entityBox.getMaxZ() - entity.getPosZ() + z + 0.05D);

		if (outline)
		{
			glLineWidth(1.00F);
			enableGlCap(GL_LINE_SMOOTH);
			glColor(color.getRed(), color.getGreen(), color.getBlue(), 95);
			drawSelectionBoundingBox(axisAlignedBB);
		}

		glColor(color.getRed(), color.getGreen(), color.getBlue(), outline ? 26 : 35);
		drawFilledBox(axisAlignedBB);
		glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
		glDepthMask(true);
		resetCaps();
	}

	public static void drawAxisAlignedBB(final IAxisAlignedBB axisAlignedBB, final Color color)
	{
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		glEnable(GL_BLEND);
		glLineWidth(2.00F);
		glDisable(GL_TEXTURE_2D);
		glDisable(GL_DEPTH_TEST);
		glDepthMask(false);
		glColor(color);
		drawFilledBox(axisAlignedBB);
		glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
		glEnable(GL_TEXTURE_2D);
		glEnable(GL_DEPTH_TEST);
		glDepthMask(true);
		glDisable(GL_BLEND);
	}

	public static void drawPlatform(final double y, final Color color, final double size)
	{
		final IRenderManager renderManager = mc.getRenderManager();
		final double renderY = y - renderManager.getRenderPosY();

		drawAxisAlignedBB(classProvider.createAxisAlignedBB(size, renderY + 0.02D, size, -size, renderY, -size), color);
	}

	public static void drawPlatform(final IEntity entity, final Color color)
	{
		final IRenderManager renderManager = mc.getRenderManager();
		final ITimer timer = mc.getTimer();

		final double x = entity.getLastTickPosX() + (entity.getPosX() - entity.getLastTickPosX()) * timer.getRenderPartialTicks() - renderManager.getRenderPosX();
		final double y = entity.getLastTickPosY() + (entity.getPosY() - entity.getLastTickPosY()) * timer.getRenderPartialTicks() - renderManager.getRenderPosY();
		final double z = entity.getLastTickPosZ() + (entity.getPosZ() - entity.getLastTickPosZ()) * timer.getRenderPartialTicks() - renderManager.getRenderPosZ();

		final IAxisAlignedBB axisAlignedBB = entity.getEntityBoundingBox().offset(-entity.getPosX(), -entity.getPosY(), -entity.getPosZ()).offset(x, y, z);

		drawAxisAlignedBB(classProvider.createAxisAlignedBB(axisAlignedBB.getMinX(), axisAlignedBB.getMaxY() + 0.2, axisAlignedBB.getMinZ(), axisAlignedBB.getMaxX(), axisAlignedBB.getMaxY() + 0.26, axisAlignedBB.getMaxZ()), color);
	}

	public static void drawFilledBox(final IAxisAlignedBB axisAlignedBB)
	{
		final ITessellator tessellator = classProvider.getTessellatorInstance();
		final IWorldRenderer worldRenderer = tessellator.getWorldRenderer();

		worldRenderer.begin(7, classProvider.getVertexFormatEnum(WDefaultVertexFormats.POSITION));

		worldRenderer.pos(axisAlignedBB.getMinX(), axisAlignedBB.getMinY(), axisAlignedBB.getMinZ()).endVertex();
		worldRenderer.pos(axisAlignedBB.getMinX(), axisAlignedBB.getMaxY(), axisAlignedBB.getMinZ()).endVertex();
		worldRenderer.pos(axisAlignedBB.getMaxX(), axisAlignedBB.getMinY(), axisAlignedBB.getMinZ()).endVertex();
		worldRenderer.pos(axisAlignedBB.getMaxX(), axisAlignedBB.getMaxY(), axisAlignedBB.getMinZ()).endVertex();
		worldRenderer.pos(axisAlignedBB.getMaxX(), axisAlignedBB.getMinY(), axisAlignedBB.getMaxZ()).endVertex();
		worldRenderer.pos(axisAlignedBB.getMaxX(), axisAlignedBB.getMaxY(), axisAlignedBB.getMaxZ()).endVertex();
		worldRenderer.pos(axisAlignedBB.getMinX(), axisAlignedBB.getMinY(), axisAlignedBB.getMaxZ()).endVertex();
		worldRenderer.pos(axisAlignedBB.getMinX(), axisAlignedBB.getMaxY(), axisAlignedBB.getMaxZ()).endVertex();

		worldRenderer.pos(axisAlignedBB.getMaxX(), axisAlignedBB.getMaxY(), axisAlignedBB.getMinZ()).endVertex();
		worldRenderer.pos(axisAlignedBB.getMaxX(), axisAlignedBB.getMinY(), axisAlignedBB.getMinZ()).endVertex();
		worldRenderer.pos(axisAlignedBB.getMinX(), axisAlignedBB.getMaxY(), axisAlignedBB.getMinZ()).endVertex();
		worldRenderer.pos(axisAlignedBB.getMinX(), axisAlignedBB.getMinY(), axisAlignedBB.getMinZ()).endVertex();
		worldRenderer.pos(axisAlignedBB.getMinX(), axisAlignedBB.getMaxY(), axisAlignedBB.getMaxZ()).endVertex();
		worldRenderer.pos(axisAlignedBB.getMinX(), axisAlignedBB.getMinY(), axisAlignedBB.getMaxZ()).endVertex();
		worldRenderer.pos(axisAlignedBB.getMaxX(), axisAlignedBB.getMaxY(), axisAlignedBB.getMaxZ()).endVertex();
		worldRenderer.pos(axisAlignedBB.getMaxX(), axisAlignedBB.getMinY(), axisAlignedBB.getMaxZ()).endVertex();

		worldRenderer.pos(axisAlignedBB.getMinX(), axisAlignedBB.getMaxY(), axisAlignedBB.getMinZ()).endVertex();
		worldRenderer.pos(axisAlignedBB.getMaxX(), axisAlignedBB.getMaxY(), axisAlignedBB.getMinZ()).endVertex();
		worldRenderer.pos(axisAlignedBB.getMaxX(), axisAlignedBB.getMaxY(), axisAlignedBB.getMaxZ()).endVertex();
		worldRenderer.pos(axisAlignedBB.getMinX(), axisAlignedBB.getMaxY(), axisAlignedBB.getMaxZ()).endVertex();
		worldRenderer.pos(axisAlignedBB.getMinX(), axisAlignedBB.getMaxY(), axisAlignedBB.getMinZ()).endVertex();
		worldRenderer.pos(axisAlignedBB.getMinX(), axisAlignedBB.getMaxY(), axisAlignedBB.getMaxZ()).endVertex();
		worldRenderer.pos(axisAlignedBB.getMaxX(), axisAlignedBB.getMaxY(), axisAlignedBB.getMaxZ()).endVertex();
		worldRenderer.pos(axisAlignedBB.getMaxX(), axisAlignedBB.getMaxY(), axisAlignedBB.getMinZ()).endVertex();

		worldRenderer.pos(axisAlignedBB.getMinX(), axisAlignedBB.getMinY(), axisAlignedBB.getMinZ()).endVertex();
		worldRenderer.pos(axisAlignedBB.getMaxX(), axisAlignedBB.getMinY(), axisAlignedBB.getMinZ()).endVertex();
		worldRenderer.pos(axisAlignedBB.getMaxX(), axisAlignedBB.getMinY(), axisAlignedBB.getMaxZ()).endVertex();
		worldRenderer.pos(axisAlignedBB.getMinX(), axisAlignedBB.getMinY(), axisAlignedBB.getMaxZ()).endVertex();
		worldRenderer.pos(axisAlignedBB.getMinX(), axisAlignedBB.getMinY(), axisAlignedBB.getMinZ()).endVertex();
		worldRenderer.pos(axisAlignedBB.getMinX(), axisAlignedBB.getMinY(), axisAlignedBB.getMaxZ()).endVertex();
		worldRenderer.pos(axisAlignedBB.getMaxX(), axisAlignedBB.getMinY(), axisAlignedBB.getMaxZ()).endVertex();
		worldRenderer.pos(axisAlignedBB.getMaxX(), axisAlignedBB.getMinY(), axisAlignedBB.getMinZ()).endVertex();

		worldRenderer.pos(axisAlignedBB.getMinX(), axisAlignedBB.getMinY(), axisAlignedBB.getMinZ()).endVertex();
		worldRenderer.pos(axisAlignedBB.getMinX(), axisAlignedBB.getMaxY(), axisAlignedBB.getMinZ()).endVertex();
		worldRenderer.pos(axisAlignedBB.getMinX(), axisAlignedBB.getMinY(), axisAlignedBB.getMaxZ()).endVertex();
		worldRenderer.pos(axisAlignedBB.getMinX(), axisAlignedBB.getMaxY(), axisAlignedBB.getMaxZ()).endVertex();
		worldRenderer.pos(axisAlignedBB.getMaxX(), axisAlignedBB.getMinY(), axisAlignedBB.getMaxZ()).endVertex();
		worldRenderer.pos(axisAlignedBB.getMaxX(), axisAlignedBB.getMaxY(), axisAlignedBB.getMaxZ()).endVertex();
		worldRenderer.pos(axisAlignedBB.getMaxX(), axisAlignedBB.getMinY(), axisAlignedBB.getMinZ()).endVertex();
		worldRenderer.pos(axisAlignedBB.getMaxX(), axisAlignedBB.getMaxY(), axisAlignedBB.getMinZ()).endVertex();

		worldRenderer.pos(axisAlignedBB.getMinX(), axisAlignedBB.getMaxY(), axisAlignedBB.getMaxZ()).endVertex();
		worldRenderer.pos(axisAlignedBB.getMinX(), axisAlignedBB.getMinY(), axisAlignedBB.getMaxZ()).endVertex();
		worldRenderer.pos(axisAlignedBB.getMinX(), axisAlignedBB.getMaxY(), axisAlignedBB.getMinZ()).endVertex();
		worldRenderer.pos(axisAlignedBB.getMinX(), axisAlignedBB.getMinY(), axisAlignedBB.getMinZ()).endVertex();
		worldRenderer.pos(axisAlignedBB.getMaxX(), axisAlignedBB.getMaxY(), axisAlignedBB.getMinZ()).endVertex();
		worldRenderer.pos(axisAlignedBB.getMaxX(), axisAlignedBB.getMinY(), axisAlignedBB.getMinZ()).endVertex();
		worldRenderer.pos(axisAlignedBB.getMaxX(), axisAlignedBB.getMaxY(), axisAlignedBB.getMaxZ()).endVertex();
		worldRenderer.pos(axisAlignedBB.getMaxX(), axisAlignedBB.getMinY(), axisAlignedBB.getMaxZ()).endVertex();
		tessellator.draw();
	}

	public static void quickDrawRect(final float x, final float y, final float x2, final float y2)
	{
		glBegin(GL_QUADS);

		glVertex2d(x2, y);
		glVertex2d(x, y);
		glVertex2d(x, y2);
		glVertex2d(x2, y2);

		glEnd();
	}

	public static void drawRect(final float x, final float y, final float x2, final float y2, final int color)
	{
		glEnable(GL_BLEND);
		glDisable(GL_TEXTURE_2D);
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		glEnable(GL_LINE_SMOOTH);

		glColor(color);
		glBegin(GL_QUADS);

		glVertex2f(x2, y);
		glVertex2f(x, y);
		glVertex2f(x, y2);
		glVertex2f(x2, y2);
		glEnd();

		glEnable(GL_TEXTURE_2D);
		glDisable(GL_BLEND);
		glDisable(GL_LINE_SMOOTH);
	}

	public static void drawRect(final int x, final int y, final int x2, final int y2, final int color)
	{
		glEnable(GL_BLEND);
		glDisable(GL_TEXTURE_2D);
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		glEnable(GL_LINE_SMOOTH);

		glColor(color);
		glBegin(GL_QUADS);

		glVertex2i(x2, y);
		glVertex2i(x, y);
		glVertex2i(x, y2);
		glVertex2i(x2, y2);
		glEnd();

		glEnable(GL_TEXTURE_2D);
		glDisable(GL_BLEND);
		glDisable(GL_LINE_SMOOTH);
	}

	/**
	 * Like {@link #drawRect(float, float, float, float, int)}, but without setup
	 */
	public static void quickDrawRect(final float x, final float y, final float x2, final float y2, final int color)
	{
		glColor(color);
		glBegin(GL_QUADS);

		glVertex2d(x2, y);
		glVertex2d(x, y);
		glVertex2d(x, y2);
		glVertex2d(x2, y2);

		glEnd();
	}

	public static void drawRect(final float x, final float y, final float x2, final float y2, final Color color)
	{
		drawRect(x, y, x2, y2, color.getRGB());
	}

	public static void drawBorderedRect(final float x, final float y, final float x2, final float y2, final float width, final int color1, final int color2)
	{
		drawRect(x, y, x2, y2, color2);
		drawBorder(x, y, x2, y2, width, color1);
	}

	public static void drawBorder(final float x, final float y, final float x2, final float y2, final float width, final int color1)
	{
		glEnable(GL_BLEND);
		glDisable(GL_TEXTURE_2D);
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		glEnable(GL_LINE_SMOOTH);

		glColor(color1);
		glLineWidth(width);

		glBegin(GL_LINE_LOOP);

		glVertex2d(x2, y);
		glVertex2d(x, y);
		glVertex2d(x, y2);
		glVertex2d(x2, y2);

		glEnd();

		glEnable(GL_TEXTURE_2D);
		glDisable(GL_BLEND);
		glDisable(GL_LINE_SMOOTH);
	}

	public static void quickDrawBorderedRect(final float x, final float y, final float x2, final float y2, final float width, final int color1, final int color2)
	{
		quickDrawRect(x, y, x2, y2, color2);

		glColor(color1);
		glLineWidth(width);

		glBegin(GL_LINE_LOOP);

		glVertex2d(x2, y);
		glVertex2d(x, y);
		glVertex2d(x, y2);
		glVertex2d(x2, y2);

		glEnd();
	}

	public static void drawLoadingCircle(final float x, final float y)
	{
		for (int i = 0; i < 4; i++)
		{
			final int rot = (int) (System.nanoTime() / 5000000 * i % 360);
			drawCircle(x, y, i * 10, rot - 180, rot);
		}
	}

	public static void drawCircle(final float x, final float y, final float radius, final int start, final int end)
	{
		classProvider.getGlStateManager().enableBlend();
		classProvider.getGlStateManager().disableTexture2D();
		classProvider.getGlStateManager().tryBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ZERO);
		glColor(Color.WHITE);

		glEnable(GL_LINE_SMOOTH);
		glLineWidth(2.00F);
		glBegin(GL_LINE_STRIP);
		for (float i = end; i >= start; i -= 360 / 90.0f)
			glVertex2f((float) (x + StrictMath.cos(i * PI / 180) * (radius * 1.001F)), (float) (y + StrictMath.sin(i * PI / 180) * (radius * 1.001F)));
		glEnd();
		glDisable(GL_LINE_SMOOTH);

		classProvider.getGlStateManager().enableTexture2D();
		classProvider.getGlStateManager().disableBlend();
	}

	public static void drawFilledCircle(final int xx, final int yy, final float radius, final Color color)
	{
		final int sections = 50;
		final double dAngle = 2 * PI / sections;
		float x, y;

		glPushAttrib(GL_ENABLE_BIT);

		glEnable(GL_BLEND);
		glDisable(GL_TEXTURE_2D);
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		glEnable(GL_LINE_SMOOTH);
		glBegin(GL_TRIANGLE_FAN);

		for (int i = 0; i < sections; i++)
		{
			x = (float) (radius * StrictMath.sin(i * dAngle));
			y = (float) (radius * StrictMath.cos(i * dAngle));

			glColor4f(color.getRed() / 255.00F, color.getGreen() / 255.00F, color.getBlue() / 255.00F, color.getAlpha() / 255.00F);
			glVertex2f(xx + x, yy + y);
		}

		glColor4f(1.0f, 1.0f, 1.0f, 1.0f);

		glEnd();

		glPopAttrib();
	}

	public static void drawImage(final IResourceLocation image, final int x, final int y, final int width, final int height)
	{
		glDisable(GL_DEPTH_TEST);
		glEnable(GL_BLEND);
		glDepthMask(false);
		GL14.glBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ZERO);
		glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		mc.getTextureManager().bindTexture(image);
		drawModalRectWithCustomSizedTexture(x, y, 0, 0, width, height, width, height);
		glDepthMask(true);
		glDisable(GL_BLEND);
		glEnable(GL_DEPTH_TEST);
	}

	/**
	 * Draws a textured rectangle at z = 0. Args: x, y, u, v, width, height, textureWidth, textureHeight
	 */
	public static void drawModalRectWithCustomSizedTexture(final float x, final float y, final float u, final float v, final float width, final float height, final float textureWidth, final float textureHeight)
	{
		final float f = 1.0F / textureWidth;
		final float f1 = 1.0F / textureHeight;
		final ITessellator tessellator = classProvider.getTessellatorInstance();
		final IWorldRenderer worldrenderer = tessellator.getWorldRenderer();
		worldrenderer.begin(7, classProvider.getVertexFormatEnum(WDefaultVertexFormats.POSITION_TEX));
		worldrenderer.pos(x, y + height, 0.0D).tex(u * f, (v + height) * f1).endVertex();
		worldrenderer.pos(x + width, y + height, 0.0D).tex((u + width) * f, (v + height) * f1).endVertex();
		worldrenderer.pos(x + width, y, 0.0D).tex((u + width) * f, v * f1).endVertex();
		worldrenderer.pos(x, y, 0.0D).tex(u * f, v * f1).endVertex();
		tessellator.draw();
	}

	public static void glColor(final int red, final int green, final int blue, final int alpha)
	{
		glColor4f(red / 255.00F, green / 255.00F, blue / 255.00F, alpha / 255.00F);
	}

	public static void glColor(final Color color)
	{
		glColor(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
	}

	public static void glColor(final int hex)
	{
		glColor(hex >> 16 & 0xFF, hex >> 8 & 0xFF, hex & 0xFF, hex >> 24 & 0xFF);
	}

	public static void draw2D(final IEntity entity, final double posX, final double posY, final double posZ, final int color, final int backgroundColor)
	{
		glPushMatrix();
		glTranslated(posX, posY, posZ);
		glRotated(-mc.getRenderManager().getPlayerViewY(), 0.00F, 1.00F, 0.00F);
		glScaled(-0.1D, -0.1D, 0.1D);

		glDisable(GL_DEPTH_TEST);
		glEnable(GL_BLEND);
		glDisable(GL_TEXTURE_2D);
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

		glDepthMask(true);

		glColor(color);

		glCallList(DISPLAY_LISTS_2D[0]);

		glColor(backgroundColor);

		glCallList(DISPLAY_LISTS_2D[1]);

		glTranslated(0, 21 + -(entity.getEntityBoundingBox().getMaxY() - entity.getEntityBoundingBox().getMinY()) * 12, 0);

		glColor(color);
		glCallList(DISPLAY_LISTS_2D[2]);

		glColor(backgroundColor);
		glCallList(DISPLAY_LISTS_2D[3]);

		// Stop render
		glEnable(GL_DEPTH_TEST);
		glEnable(GL_TEXTURE_2D);
		glDisable(GL_BLEND);

		glPopMatrix();
	}

	public static void draw2D(final WBlockPos blockPos, final int color, final int backgroundColor)
	{
		final IRenderManager renderManager = mc.getRenderManager();

		final double posX = blockPos.getX() + 0.5 - renderManager.getRenderPosX();
		final double posY = blockPos.getY() - renderManager.getRenderPosY();
		final double posZ = blockPos.getZ() + 0.5 - renderManager.getRenderPosZ();

		glPushMatrix();
		glTranslated(posX, posY, posZ);
		glRotated(-mc.getRenderManager().getPlayerViewY(), 0.00F, 1.00F, 0.00F);
		glScaled(-0.1D, -0.1D, 0.1D);

		glDisable(GL_DEPTH_TEST);
		glEnable(GL_BLEND);
		glDisable(GL_TEXTURE_2D);
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

		glDepthMask(true);

		glColor(color);

		glCallList(DISPLAY_LISTS_2D[0]);

		glColor(backgroundColor);

		glCallList(DISPLAY_LISTS_2D[1]);

		glTranslated(0, 9, 0);

		glColor(color);

		glCallList(DISPLAY_LISTS_2D[2]);

		glColor(backgroundColor);

		glCallList(DISPLAY_LISTS_2D[3]);

		// Stop render
		glEnable(GL_DEPTH_TEST);
		glEnable(GL_TEXTURE_2D);
		glDisable(GL_BLEND);

		glPopMatrix();
	}

	public static void renderNameTag(final String string, final double x, final double y, final double z)
	{
		final IRenderManager renderManager = mc.getRenderManager();

		glPushMatrix();
		glTranslated(x - renderManager.getRenderPosX(), y - renderManager.getRenderPosY(), z - renderManager.getRenderPosZ());
		glNormal3f(0.00F, 1.00F, 0.00F);
		glRotatef(-mc.getRenderManager().getPlayerViewY(), 0.00F, 1.00F, 0.00F);
		glRotatef(mc.getRenderManager().getPlayerViewX(), 1.00F, 0.00F, 0.00F);
		glScalef(-0.05F, -0.05F, 0.05F);
		setGlCap(GL_LIGHTING, false);
		setGlCap(GL_DEPTH_TEST, false);
		setGlCap(GL_BLEND, true);
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

		final int width = Fonts.font35.getStringWidth(string) / 2;

		drawRect(-width - 1, -1, width + 1, Fonts.font35.getFontHeight(), Integer.MIN_VALUE);
		Fonts.font35.drawString(string, -width, 1.5F, Color.WHITE.getRGB(), true);

		resetCaps();
		glColor4f(1.00F, 1.00F, 1.00F, 1.00F);
		glPopMatrix();
	}

	public static void drawLine(final double x, final double y, final double x1, final double y1, final float width)
	{
		glDisable(GL_TEXTURE_2D);
		glLineWidth(width);
		glBegin(GL_LINES);
		glVertex2d(x, y);
		glVertex2d(x1, y1);
		glEnd();
		glEnable(GL_TEXTURE_2D);
	}

	public static void makeScissorBox(final float x, final float y, final float x2, final float y2)
	{
		final IScaledResolution scaledResolution = classProvider.createScaledResolution(mc);
		final int factor = scaledResolution.getScaleFactor();
		glScissor((int) (x * factor), (int) ((scaledResolution.getScaledHeight() - y2) * factor), (int) ((x2 - x) * factor), (int) ((y2 - y) * factor));
	}

	/**
	 * GL CAP MANAGER
	 * <p>
	 * TODO: Remove gl cap manager and replace by something better
	 */

	public static void resetCaps()
	{
		glCapMap.forEach(RenderUtils::setGlState);
	}

	public static void enableGlCap(final int cap)
	{
		setGlCap(cap, true);
	}

	public static void enableGlCap(final int... caps)
	{
		for (final int cap : caps)
			setGlCap(cap, true);
	}

	public static void disableGlCap(final int cap)
	{
		setGlCap(cap, true);
	}

	public static void disableGlCap(final int... caps)
	{
		for (final int cap : caps)
			setGlCap(cap, false);
	}

	public static void setGlCap(final int cap, final boolean state)
	{
		glCapMap.put(cap, glGetBoolean(cap));
		setGlState(cap, state);
	}

	public static void setGlState(final int cap, final boolean state)
	{
		if (state)
			glEnable(cap);
		else
			glDisable(cap);
	}

	public static void drawScaledCustomSizeModalRect(final int x, final int y, final float u, final float v, final int uWidth, final int vHeight, final int width, final int height, final float tileWidth, final float tileHeight)
	{
		final float f = 1.0F / tileWidth;
		final float f1 = 1.0F / tileHeight;
		final ITessellator tessellator = classProvider.getTessellatorInstance();
		final IWorldRenderer worldrenderer = tessellator.getWorldRenderer();
		worldrenderer.begin(7, classProvider.getVertexFormatEnum(WDefaultVertexFormats.POSITION_TEX));
		worldrenderer.pos(x, y + height, 0.0D).tex(u * f, (v + vHeight) * f1).endVertex();
		worldrenderer.pos(x + width, y + height, 0.0D).tex((u + uWidth) * f, (v + vHeight) * f1).endVertex();
		worldrenderer.pos(x + width, y, 0.0D).tex((u + uWidth) * f, v * f1).endVertex();
		worldrenderer.pos(x, y, 0.0D).tex(u * f, v * f1).endVertex();
		tessellator.draw();
	}

	public static void drawFoVCircle(final float fov)
	{
		if (mc.getGameSettings().getThirdPersonView() > 0)
			return;
		final IScaledResolution scaledResolution = classProvider.createScaledResolution(mc);
		drawCircle(scaledResolution.getScaledWidth() >> 1, scaledResolution.getScaledHeight() >> 1, fov * 6.0F / (mc.getGameSettings().getFovSettings() / 70.0F), 0, 360);
	}
}
