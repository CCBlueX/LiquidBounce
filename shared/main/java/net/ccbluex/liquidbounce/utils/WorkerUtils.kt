package net.ccbluex.liquidbounce.utils

import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger

object WorkerUtils
{
	@JvmStatic
	val workers: ThreadPoolExecutor = ThreadPoolExecutor(1, Runtime.getRuntime().availableProcessors(), 30L, TimeUnit.SECONDS, LinkedBlockingQueue(), LiquidBounceThreadFactory())

	@JvmStatic
	val scheduledWorkers: ScheduledThreadPoolExecutor = ScheduledThreadPoolExecutor(1, LiquidBounceThreadFactory("Scheduled"))

	init
	{
		scheduledWorkers.setKeepAliveTime(30L, TimeUnit.SECONDS)
	}

	private class LiquidBounceThreadFactory(prefix: String = "") : ThreadFactory
	{
		private var group: ThreadGroup
		private val threadNumber = AtomicInteger()
		private var namePrefix: String? = null

		init
		{
			val s = System.getSecurityManager()
			group = if (s != null) s.threadGroup else Thread.currentThread().threadGroup
			namePrefix = "LiquidBounce_${prefix}Worker #"
		}

		override fun newThread(task: Runnable): Thread
		{
			val t = Thread(group, task, namePrefix + threadNumber.getAndIncrement(), 0)
			if (t.isDaemon) t.isDaemon = false
			if (t.priority != Thread.NORM_PRIORITY) t.priority = Thread.NORM_PRIORITY
			return t
		}
	}
}

fun runAsync(block: () -> Unit) = WorkerUtils.workers.execute(block)

// from: https://www.baeldung.com/java-completablefuture
// TODO: Better solution
fun runAsyncParallel(blocks: Collection<() -> Unit>): CompletableFuture<Void>
{
	val futures = arrayOfNulls<CompletableFuture<*>>(blocks.size)
	blocks.forEachIndexed { index, block ->
		val future = CompletableFuture<Void>()

		runAsync {
			try
			{
				block()
				future.complete(null)
			}
			catch (t: Throwable)
			{
				future.completeExceptionally(t)
			}
		}

		futures[index] = future
	}

	return CompletableFuture.allOf(*futures)
}

fun <T> supplyAsync(block: () -> T): Future<T> = WorkerUtils.workers.submit(block)

// delayMillis: Long, task: () -> Unit -> WorkerUtils.scheduledWorkers.schedule(task, delayMillis, TimeUnit.MILLISECONDS)

fun <T> runAsyncDelayed(delayInMillis: Long, block: () -> T): ScheduledFuture<T> = WorkerUtils.scheduledWorkers.schedule(block, delayInMillis, TimeUnit.MILLISECONDS)

