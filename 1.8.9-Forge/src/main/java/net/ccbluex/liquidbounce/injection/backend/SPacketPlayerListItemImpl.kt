package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.network.play.server.ISPacketPlayerListItem
import net.ccbluex.liquidbounce.api.util.WrappedMutableList
import net.ccbluex.liquidbounce.injection.backend.utils.unwrap
import net.ccbluex.liquidbounce.injection.backend.utils.wrap
import net.minecraft.network.play.server.S38PacketPlayerListItem

class SPacketPlayerListItemImpl<out T : S38PacketPlayerListItem>(wrapped: T) : PacketImpl<T>(wrapped), ISPacketPlayerListItem
{
	override val action: ISPacketPlayerListItem.WAction
		get() = wrapped.action.wrap()

	override val players: MutableList<ISPacketPlayerListItem.WAddPlayerData>
		get() = WrappedMutableList(wrapped.entries, ISPacketPlayerListItem.WAddPlayerData::unwrap, S38PacketPlayerListItem.AddPlayerData::wrap)
}

fun ISPacketPlayerListItem.unwrap(): S38PacketPlayerListItem = (this as SPacketPlayerListItemImpl<*>).wrapped
fun S38PacketPlayerListItem.wrap(): ISPacketPlayerListItem = SPacketPlayerListItemImpl(this)
