package net.ccbluex.liquidbounce.utils

import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger

object WorkerUtils
{
	@JvmStatic
	val workers: ThreadPoolExecutor

	@JvmStatic
	val timeCriticalWorkers: ThreadPoolExecutor

	init
	{
		val availableProcessors = Runtime.getRuntime().availableProcessors()
		workers = ThreadPoolExecutor(availableProcessors, availableProcessors, 5L, TimeUnit.MINUTES, LinkedBlockingQueue(), LiquidBounceThreadFactory(Thread.NORM_PRIORITY))
		timeCriticalWorkers = ThreadPoolExecutor(availableProcessors, availableProcessors, 5L, TimeUnit.MINUTES, LinkedBlockingQueue(), LiquidBounceThreadFactory(Thread.MAX_PRIORITY))
	}

	private class LiquidBounceThreadFactory(private val priority: Int) : ThreadFactory
	{
		private val poolNumber = AtomicInteger(1)
		private var group: ThreadGroup
		private val threadNumber = AtomicInteger(1)
		private var namePrefix: String? = null

		init
		{
			val s = System.getSecurityManager()
			group = if (s != null) s.threadGroup else Thread.currentThread().threadGroup
			namePrefix = "LiquidBounceWorkers-" + poolNumber.getAndIncrement() + "-thread-"
		}

		override fun newThread(task: Runnable): Thread
		{
			val t = Thread(group, task, namePrefix + threadNumber.getAndIncrement(), 0)
			if (t.isDaemon) t.isDaemon = false
			t.priority = priority
			return t
		}
	}
}
