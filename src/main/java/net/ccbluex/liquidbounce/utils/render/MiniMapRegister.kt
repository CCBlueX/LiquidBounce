/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.utils.render

import net.ccbluex.liquidbounce.api.minecraft.util.WBlockPos
import net.ccbluex.liquidbounce.api.minecraft.world.IChunk
import net.ccbluex.liquidbounce.utils.MinecraftInstance
import java.util.concurrent.atomic.AtomicBoolean

object MiniMapRegister : MinecraftInstance()
{
	private val chunkTextureMap = HashMap<ChunkLocation, MiniMapTexture>()
	private val queuedChunkUpdates = HashSet<IChunk>(256)
	private val queuedChunkDeletions = HashSet<ChunkLocation>(256)
	private val deleteAllChunks = AtomicBoolean(false)

	fun updateChunk(chunk: IChunk)
	{
		synchronized(queuedChunkUpdates) {
			queuedChunkUpdates.add(chunk)
		}
	}

	fun getChunkTextureAt(x: Int, z: Int): MiniMapTexture? = chunkTextureMap[ChunkLocation(x, z)]

	fun updateChunks()
	{
		synchronized(queuedChunkUpdates) {
			if (deleteAllChunks.get())
			{
				synchronized(queuedChunkDeletions, queuedChunkDeletions::clear)
				queuedChunkUpdates.clear()

				chunkTextureMap.forEach { it.value.delete() }

				chunkTextureMap.clear()

				deleteAllChunks.set(false)
			}
			else synchronized(queuedChunkDeletions) {
				queuedChunkDeletions.forEach {
					chunkTextureMap.remove(it)?.delete()
				}

				queuedChunkDeletions.clear()
			}

			queuedChunkUpdates.forEach {
				chunkTextureMap.computeIfAbsent(ChunkLocation(it.x, it.z)) {
					MiniMapTexture()
				}::updateChunkData
			}

			queuedChunkUpdates.clear()
		}
	}

	fun getLoadedChunkCount(): Int = chunkTextureMap.size

	fun unloadChunk(x: Int, z: Int)
	{
		synchronized(queuedChunkDeletions) {
			queuedChunkDeletions.add(ChunkLocation(x, z))
		}
	}

	fun unloadAllChunks()
	{
		deleteAllChunks.set(true)
	}

	class MiniMapTexture
	{
		val texture = classProvider.createDynamicTexture(16, 16)
		private var deleted = false

		fun updateChunkData(chunk: IChunk)
		{
			val theWorld = mc.theWorld ?: return

			val rgbValues = texture.textureData

			(0..15).forEach { x ->
				(0..15).forEach { z ->
					val bp = WBlockPos(x, chunk.getHeightValue(x, z) - 1, z)
					val blockState = chunk.getBlockState(bp)

					rgbValues[rgbValues.size - ((z shl 4) + x + 1)] = blockState.block.getMapColor(blockState, theWorld, bp) or (0xFF shl 24)
				}
			}

			texture.updateDynamicTexture()
		}

		internal fun delete()
		{
			if (!deleted)
			{
				texture.deleteGlTexture()
				deleted = true
			}
		}

		protected fun finalize()
		{
			// We don't need to set deleted to true since the object is deleted after this method call
			if (!deleted) texture.deleteGlTexture()
		}
	}

	data class ChunkLocation(val x: Int, val z: Int)
}
