package net.ccbluex.liquidbounce.injection.forge.mixins.gui;

import java.util.Comparator;
import java.util.List;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.minecraft.util.TabCompleter;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TabCompleter.class)
public abstract class MixinTabCompleter
{

	@Shadow
	protected List<String> completions;

	@Shadow
	protected boolean requestedCompletions;
	@Shadow
	protected boolean didComplete;

	@Shadow
	public abstract void setCompletions(String... p_setCompletions_1_);

	@Inject(method = "complete", at = @At("HEAD"))
	private void complete(final CallbackInfo ci)
	{
		completions.sort(Comparator.comparing(s -> !LiquidBounce.fileManager.friendsConfig.isFriend(s)));
	}

	/**
	 * Adds client command auto completion and cancels sending an auto completion request packet to the server if the message contains a client command.
	 *
	 * @author NurMarvin
	 */
	@Inject(method = "requestCompletions", at = @At("HEAD"), cancellable = true)
	private void handleClientCommandCompletion(final String prefix, final CallbackInfo callbackInfo)
	{
		if (LiquidBounce.commandManager.autoComplete(prefix))
		{
			requestedCompletions = true;

			final String[] latestAutoComplete = LiquidBounce.commandManager.getLatestAutoComplete();

			if (prefix.toLowerCase().endsWith(latestAutoComplete[latestAutoComplete.length - 1].toLowerCase()))
				return;

			setCompletions(latestAutoComplete);

			callbackInfo.cancel();
		}
	}

	/**
	 * Add this callback, to check if the User complete a Playername or a Liquidbounce command. To fix this bug: https://github.com/CCBlueX/LiquidBounce1.8-Issues/issues/3795
	 *
	 * @author derech1e
	 */
	@Inject(method = "setCompletions", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/TabCompleter;complete()V", shift = Shift.BEFORE), cancellable = true)
	private void onAutocompleteResponse(final String[] autoCompleteResponse, final CallbackInfo callbackInfo)
	{
		if (LiquidBounce.commandManager.getLatestAutoComplete().length != 0)
			callbackInfo.cancel();
	}

}
