package com.codahale.guild

import org.jetlang.fibers.{Fiber, PoolFiberFactory}
import org.jetlang.core.BatchExecutorImpl
import java.util.concurrent.{ExecutorService, TimeUnit, Executors}

object Scheduler {
  val Default = new Scheduler()
}

/**
 * A basic scheduler backed by a cached thread pool.
 */
class Scheduler {
  /**
   * Override this to provide a custom ExecutorService.
   */
  protected def createExecutor: ExecutorService = Executors.newCachedThreadPool
  
  private val executor = createExecutor
  private val batch = new BatchExecutorImpl
  private val factory = new PoolFiberFactory(executor)

  /**
   * Returns a newly created, unstarted Fiber.
   */
  private[guild] def createFiber() = factory.create(batch)

  /**
   * Shuts down the scheduler with a timeout.
   */
  def shutdown(timeout: Long, unit: TimeUnit) {
    executor.shutdownNow()
    executor.awaitTermination(timeout, unit)
    factory.dispose()
  }
}
