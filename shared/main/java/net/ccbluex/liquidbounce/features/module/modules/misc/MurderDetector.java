package net.ccbluex.liquidbounce.features.module.modules.misc;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import net.ccbluex.liquidbounce.api.minecraft.client.entity.player.IEntityPlayer;
import net.ccbluex.liquidbounce.api.minecraft.item.IItem;
import net.ccbluex.liquidbounce.event.EventTarget;
import net.ccbluex.liquidbounce.event.TickEvent;
import net.ccbluex.liquidbounce.event.WorldEvent;
import net.ccbluex.liquidbounce.features.module.Module;
import net.ccbluex.liquidbounce.features.module.ModuleCategory;
import net.ccbluex.liquidbounce.features.module.ModuleInfo;
import net.ccbluex.liquidbounce.utils.ClientUtils;

import org.jetbrains.annotations.Nullable;

/**
 * LiquidBounce Hacked Client A minecraft forge injection client using Mixin
 *
 * @author CCBlueX
 * @game   Minecraft
 */
@ModuleInfo(name = "MurderDetector", description = "Detects murder in murder mystery.", category = ModuleCategory.MISC)
public class MurderDetector extends Module
{
	public final List<IEntityPlayer> murders = new ArrayList<>();

	@Override
	public final void onEnable()
	{
		murders.clear();
	}

	@EventTarget
	public final void onTick(final TickEvent event)
	{
		for (final Object o : Objects.requireNonNull(mc.getTheWorld()).getLoadedEntityList())
			if (o instanceof IEntityPlayer)
			{
				final IEntityPlayer ent = (IEntityPlayer) o;
				if (ent != mc.getThePlayer() && ent.getCurrentEquippedItem() != null && isMurder(ent.getCurrentEquippedItem().getItem()) && !murders.contains(ent))
				{
					murders.add(ent);
					ClientUtils.displayChatMessage("\u00A7a\u00A7l" + ent.getName() + "\u00A7r is the \u00A74\u00A7lmurderer\u00A7r!");
				}
			}
	}

	@EventTarget
	public final void onWorldChange(final WorldEvent event)
	{
		murders.clear();
	}

	public static boolean isMurder(final IItem item)
	{
		return !classProvider.isItemMap(item) && !"item.ingotGold".equalsIgnoreCase(item.getUnlocalizedName()) && !classProvider.isItemBow(item) && !"item.arrow".equalsIgnoreCase(item.getUnlocalizedName()) && !"item.potion".equalsIgnoreCase(item.getUnlocalizedName()) && !"item.paper".equalsIgnoreCase(item.getUnlocalizedName()) && !"tile.tnt".equalsIgnoreCase(item.getUnlocalizedName()) && !"item.web".equalsIgnoreCase(item.getUnlocalizedName()) && !"item.bed".equalsIgnoreCase(item.getUnlocalizedName()) && !"item.compass".equalsIgnoreCase(item.getUnlocalizedName()) && !"item.comparator".equalsIgnoreCase(item.getUnlocalizedName()) && !"item.shovelWood".equalsIgnoreCase(item.getUnlocalizedName());
	}

	@Nullable
	@Override
	public final String getTag()
	{
		return murders.isEmpty() ? null : String.valueOf(murders.get(0).getName());
	}
}
