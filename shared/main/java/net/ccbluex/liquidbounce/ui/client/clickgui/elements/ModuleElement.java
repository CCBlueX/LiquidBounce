/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.clickgui.elements;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.features.module.Module;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Mouse;

@SideOnly(Side.CLIENT)
public class ModuleElement extends ButtonElement {

	private final Module module;

	private boolean showSettings;
	private float settingsWidth;
	private boolean wasPressed;

	public int slowlySettingsYPos;
	public int slowlyFade;

	public ModuleElement(final Module module) {
		super(null);

		displayName = module.getName();
		this.module = module;
	}

	@Override
	public void drawScreen(final int mouseX, final int mouseY, final float button) {
		LiquidBounce.clickGui.style.drawModuleElement(mouseX, mouseY, this);
	}

	@Override
	public void mouseClicked(final int mouseX, final int mouseY, final int mouseButton) {
		if (mouseButton == 0 && isHovering(mouseX, mouseY) && isVisible()) {
			module.toggle();
			mc.getSoundHandler().playSound("gui.button.press", 1.0F);
		}

		if (mouseButton == 1 && isHovering(mouseX, mouseY) && isVisible()) {
			showSettings = !showSettings;
			mc.getSoundHandler().playSound("gui.button.press", 1.0F);
		}
	}

	public Module getModule() {
		return module;
	}

	public boolean isShowSettings() {
		return showSettings;
	}

	public void setShowSettings(final boolean showSettings) {
		this.showSettings = showSettings;
	}

	public boolean isntPressed() {
		return !wasPressed;
	}

	public void updatePressed() {
		wasPressed = Mouse.isButtonDown(0);
	}

	public float getSettingsWidth() {
		return settingsWidth;
	}

	public void setSettingsWidth(final float settingsWidth) {
		this.settingsWidth = settingsWidth;
	}
}
