package net.ccbluex.liquidbounce.features.module.modules.render;

import java.util.Random;

import net.ccbluex.liquidbounce.event.EventTarget;
import net.ccbluex.liquidbounce.event.Render3DEvent;
import net.ccbluex.liquidbounce.features.module.Module;
import net.ccbluex.liquidbounce.features.module.ModuleCategory;
import net.ccbluex.liquidbounce.features.module.ModuleInfo;
import net.ccbluex.liquidbounce.value.FloatValue;

import org.jetbrains.annotations.Nullable;

@ModuleInfo(name = "ItemPhysics", description = "Integrated ItemPhysics-lite mod.", category = ModuleCategory.RENDER)
public class ItemPhysics extends Module
{
	public long tick;
	public final FloatValue itemRotationSpeed = new FloatValue("RotateSpeed", 1.0F, 0.0F, 10.0F);

	@EventTarget
	public final void onRender3D(final Render3DEvent event)
	{
		tick = System.nanoTime();
	}

	@Nullable
	@Override
	public String getTag()
	{
		return itemRotationSpeed.get().toString();
	}
}
