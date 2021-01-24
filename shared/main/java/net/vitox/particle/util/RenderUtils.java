package net.vitox.particle.util;

import static org.lwjgl.opengl.GL11.*;

import net.ccbluex.liquidbounce.utils.MinecraftInstance;

public final class RenderUtils
{

	public static void connectPoints(final float xOne, final float yOne, final float xTwo, final float yTwo)
	{
		glPushMatrix();
		glEnable(GL_LINE_SMOOTH);
		glColor4f(1.0F, 1.0F, 1.0F, 0.8F);
		glDisable(GL_TEXTURE_2D);
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		glEnable(GL_BLEND);
		glLineWidth(0.5F);
		glBegin(GL_LINES);
		glVertex2f(xOne, yOne);
		glVertex2f(xTwo, yTwo);
		glEnd();
		glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		glDisable(GL_LINE_SMOOTH);
		glEnable(GL_TEXTURE_2D);
		glPopMatrix();
	}

	public static void drawCircle(final float x, final float y, final float radius, final int color)
	{
		final float alpha = (color >> 24 & 0xFF) / 255.0F;
		final float red = (color >> 16 & 0xFF) / 255.0F;
		final float green = (color >> 8 & 0xFF) / 255.0F;
		final float blue = (color & 0xFF) / 255.0F;

		final float floatPI = (float)Math.PI;

		glColor4f(red, green, blue, alpha);
		glEnable(GL_BLEND);
		glDisable(GL_TEXTURE_2D);
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		glEnable(GL_LINE_SMOOTH);
		glPushMatrix();
		glLineWidth(1.0F);
		glBegin(GL_POLYGON);
		for (int i = 0; i <= 360; i++)
			glVertex2d(x + MinecraftInstance.functions.sin(i * floatPI / 180.0F) * radius, y + MinecraftInstance.functions.cos(i * floatPI / 180.0F) * radius);
		glEnd();
		glPopMatrix();
		glEnable(GL_TEXTURE_2D);
		glDisable(GL_LINE_SMOOTH);
		glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
	}

	private RenderUtils() {
	}
}
