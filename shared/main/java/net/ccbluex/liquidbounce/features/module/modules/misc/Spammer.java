/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.misc;

import java.util.Random;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.event.EventTarget;
import net.ccbluex.liquidbounce.event.UpdateEvent;
import net.ccbluex.liquidbounce.features.module.Module;
import net.ccbluex.liquidbounce.features.module.ModuleCategory;
import net.ccbluex.liquidbounce.features.module.ModuleInfo;
import net.ccbluex.liquidbounce.utils.misc.RandomUtils;
import net.ccbluex.liquidbounce.utils.timer.MSTimer;
import net.ccbluex.liquidbounce.utils.timer.TimeUtils;
import net.ccbluex.liquidbounce.value.BoolValue;
import net.ccbluex.liquidbounce.value.IntegerValue;
import net.ccbluex.liquidbounce.value.TextValue;

@ModuleInfo(name = "Spammer", description = "Spams the chat with a given message.", category = ModuleCategory.MISC)
public class Spammer extends Module
{

	final IntegerValue maxDelayValue = new IntegerValue("MaxDelay", 1000, 0, 5000)
	{
		@Override
		protected void onChanged(final Integer oldValue, final Integer newValue)
		{
			final int minDelayValueObject = minDelayValue.get();

			if (minDelayValueObject > newValue)
				set(minDelayValueObject);
			delay = TimeUtils.randomDelay(minDelayValue.get(), maxDelayValue.get());
		}
	};

	final IntegerValue minDelayValue = new IntegerValue("MinDelay", 500, 0, 5000)
	{

		@Override
		protected void onChanged(final Integer oldValue, final Integer newValue)
		{
			final int maxDelayValueObject = maxDelayValue.get();

			if (maxDelayValueObject < newValue)
				set(maxDelayValueObject);
			delay = TimeUtils.randomDelay(minDelayValue.get(), maxDelayValue.get());
		}
	};

	private final TextValue messageValue = new TextValue("Message", LiquidBounce.CLIENT_NAME + " Client | liquidbounce(.net) | CCBlueX on yt");
	private final BoolValue customValue = new BoolValue("Custom", false);

	private final MSTimer msTimer = new MSTimer();
	long delay = TimeUtils.randomDelay(minDelayValue.get(), maxDelayValue.get());

	@EventTarget
	public void onUpdate(final UpdateEvent event)
	{
		if (msTimer.hasTimePassed(delay))
		{
			mc.getThePlayer().sendChatMessage(customValue.get() ? replace(messageValue.get()) : messageValue.get() + " >" + RandomUtils.randomString(5 + new Random().nextInt(5)) + "<");
			msTimer.reset();
			delay = TimeUtils.randomDelay(minDelayValue.get(), maxDelayValue.get());
		}
	}

	private static String replace(String object)
	{
		final Random r = new Random();

		while (object.contains("%f"))
			object = object.substring(0, object.indexOf("%f")) + r.nextFloat() + object.substring(object.indexOf("%f") + "%f".length());

		while (object.contains("%i"))
			object = object.substring(0, object.indexOf("%i")) + r.nextInt(10000) + object.substring(object.indexOf("%i") + "%i".length());

		while (object.contains("%s"))
			object = object.substring(0, object.indexOf("%s")) + RandomUtils.randomString(r.nextInt(8) + 1) + object.substring(object.indexOf("%s") + "%s".length());

		while (object.contains("%ss"))
			object = object.substring(0, object.indexOf("%ss")) + RandomUtils.randomString(r.nextInt(4) + 1) + object.substring(object.indexOf("%ss") + "%ss".length());

		while (object.contains("%ls"))
			object = object.substring(0, object.indexOf("%ls")) + RandomUtils.randomString(r.nextInt(15) + 1) + object.substring(object.indexOf("%ls") + "%ls".length());
		return object;
	}

}
