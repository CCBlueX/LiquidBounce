/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge;

import java.util.Map;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;

import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.MixinEnvironment.Side;
import org.spongepowered.asm.mixin.Mixins;

public class MixinLoader implements IFMLLoadingPlugin
{

	public MixinLoader()
	{
		System.out.println("[LiquidBounce] Injecting with IFMLLoadingPlugin.");

		MixinBootstrap.init();
		Mixins.addConfiguration("liquidbounce.forge.mixins.json");
		MixinEnvironment.getDefaultEnvironment().setSide(Side.CLIENT);
	}

	@Override
	public String[] getASMTransformerClass()
	{
		return ZERO_LENGTH_STRING_ARRAY;
	}

	@Override
	public String getModContainerClass()
	{
		return null;
	}

	@Override
	public String getSetupClass()
	{
		return null;
	}

	@Override
	public void injectData(final Map<String, Object> map)
	{
	}

	@Override
	public String getAccessTransformerClass()
	{
		return null;
	}

	private static final String[] ZERO_LENGTH_STRING_ARRAY = new String[0];
}
