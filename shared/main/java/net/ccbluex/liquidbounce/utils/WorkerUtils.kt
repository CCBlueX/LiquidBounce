package net.ccbluex.liquidbounce.utils

import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger

object WorkerUtils
{
	@JvmStatic
	val workers: ThreadPoolExecutor

	init
	{
		val availableProcessors = Runtime.getRuntime().availableProcessors()
		workers = ThreadPoolExecutor(availableProcessors, availableProcessors, 5L, TimeUnit.MINUTES, LinkedBlockingQueue(), LiquidBounceThreadFactory())
	}

	private class LiquidBounceThreadFactory : ThreadFactory
	{
		private val poolNumber = AtomicInteger()
		private var group: ThreadGroup
		private val threadNumber = AtomicInteger()
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
			if (t.priority != Thread.NORM_PRIORITY) t.priority = Thread.NORM_PRIORITY
			return t
		}
	}
}
