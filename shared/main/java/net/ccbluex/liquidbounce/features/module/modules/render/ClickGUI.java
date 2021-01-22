/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.render;

import java.awt.*;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.api.minecraft.network.IPacket;
import net.ccbluex.liquidbounce.event.EventTarget;
import net.ccbluex.liquidbounce.event.PacketEvent;
import net.ccbluex.liquidbounce.features.module.Module;
import net.ccbluex.liquidbounce.features.module.ModuleCategory;
import net.ccbluex.liquidbounce.features.module.ModuleInfo;
import net.ccbluex.liquidbounce.ui.client.clickgui.style.styles.LiquidBounceStyle;
import net.ccbluex.liquidbounce.ui.client.clickgui.style.styles.NullStyle;
import net.ccbluex.liquidbounce.ui.client.clickgui.style.styles.SlowlyStyle;
import net.ccbluex.liquidbounce.utils.render.ColorUtils;
import net.ccbluex.liquidbounce.value.BoolValue;
import net.ccbluex.liquidbounce.value.FloatValue;
import net.ccbluex.liquidbounce.value.IntegerValue;
import net.ccbluex.liquidbounce.value.ListValue;

import org.lwjgl.input.Keyboard;

@ModuleInfo(name = "ClickGUI", description = "Opens the ClickGUI.", category = ModuleCategory.RENDER, keyBind = Keyboard.KEY_RSHIFT, canEnable = false)
public class ClickGUI extends Module
{
	private final ListValue styleValue = new ListValue("Style", new String[]
	{
			"LiquidBounce", "Null", "Slowly"
	}, "Slowly")
	{
		@Override
		protected void onChanged(final String oldValue, final String newValue)
		{
			updateStyle();
		}
	};

	public final FloatValue scaleValue = new FloatValue("Scale", 1F, 0.7F, 2F);
	public final IntegerValue maxElementsValue = new IntegerValue("MaxElements", 15, 1, 20);

	private static final IntegerValue colorRedValue = new IntegerValue("R", 0, 0, 255);
	private static final IntegerValue colorGreenValue = new IntegerValue("G", 160, 0, 255);
	private static final IntegerValue colorBlueValue = new IntegerValue("B", 255, 0, 255);

	private static final BoolValue colorRainbowValue = new BoolValue("Rainbow", false);
	private static final IntegerValue rainbowSpeedValue = new IntegerValue("Rainbow-Speed", 10, 1, 10);
	private static final FloatValue saturationValue = new FloatValue("HSB-Saturation", 1.0F, 0.0F, 1.0F);
	private static final FloatValue brightnessValue = new FloatValue("HSB-Brightness", 1.0F, 0.0F, 1.0F);

	public static Color generateColor()
	{
		return colorRainbowValue.get() ? ColorUtils.rainbow(11 - Math.min(Math.max(rainbowSpeedValue.get(), 1), 10), saturationValue.get(), brightnessValue.get()) : new Color(colorRedValue.get(), colorGreenValue.get(), colorBlueValue.get());
	}

	@Override
	public void onEnable()
	{
		updateStyle();

		mc.displayGuiScreen(classProvider.wrapGuiScreen(LiquidBounce.clickGui));
	}

	private void updateStyle()
	{
		switch (styleValue.get().toLowerCase())
		{
			case "liquidbounce":
				LiquidBounce.clickGui.style = new LiquidBounceStyle();
				break;
			case "null":
				LiquidBounce.clickGui.style = new NullStyle();
				break;
			case "slowly":
				LiquidBounce.clickGui.style = new SlowlyStyle();
				break;
		}
	}

	@EventTarget(ignoreCondition = true)
	public void onPacket(final PacketEvent event)
	{
		final IPacket packet = event.getPacket();

		if (classProvider.isSPacketCloseWindow(packet) && classProvider.isClickGui(mc.getCurrentScreen()))
			event.cancelEvent();
	}
}
