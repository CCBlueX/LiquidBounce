package net.ccbluex.liquidbounce.utils

import java.util.concurrent.*

object WorkerUtils
{
	@JvmStatic
	val workers: ThreadPoolExecutor = ThreadPoolExecutor(1, Runtime.getRuntime().availableProcessors(), 30L, TimeUnit.SECONDS, LinkedBlockingQueue())

	@JvmStatic
	val forkJoinWorkers: ForkJoinPool = ForkJoinPool(Runtime.getRuntime().availableProcessors())

	@JvmStatic
	val scheduledWorkers: ScheduledThreadPoolExecutor = ScheduledThreadPoolExecutor(1)

	init
	{
		scheduledWorkers.setKeepAliveTime(30L, TimeUnit.SECONDS)
	}
}

fun runAsync(block: () -> Unit) = WorkerUtils.workers.execute(block)

fun <T> supplyAsync(block: () -> T): Future<T> = WorkerUtils.workers.submit(block)

// delayMillis: Long, task: () -> Unit -> WorkerUtils.scheduledWorkers.schedule(task, delayMillis, TimeUnit.MILLISECONDS)

fun <T> runAsyncDelayed(delayInMillis: Long, block: () -> T): ScheduledFuture<T> = WorkerUtils.scheduledWorkers.schedule(block, delayInMillis, TimeUnit.MILLISECONDS)

