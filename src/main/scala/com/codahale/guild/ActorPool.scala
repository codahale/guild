package com.codahale.guild

import org.jetlang.fibers.Fiber
import org.jetlang.core.Callback
import org.jetlang.channels.{MemoryChannel, AsyncRequest, Request, MemoryRequestChannel}
import java.util.concurrent.TimeUnit
import concurrent.forkjoin.{TransferQueue, LinkedTransferQueue}

trait ActorFactory[T <: ActorBehavior] {
  def createActor() : T
}

/**
 * Implements a pool of actors which all consume from a single message channel.
 */
class ActorPool[T <: ActorBehavior](factory : ActorFactory[T]) extends Sendable with Callable {
  /**
   * Override this method to use a different scheduler.
   */
  protected def scheduler = Scheduler.Default
  protected val asyncQueue = new LinkedTransferQueue[Any]
  
  protected var actors = Seq[(Fiber, ActorBehavior)]()
  
  /**
   * Creates and start i actors.
   */
  def start(i : Int) {
    actors = (0 until i).map { n =>
      val fiber = scheduler.createFiber
      val actor = factory.createActor
      fiber.start
      fiber.execute(ActorInit(actor))
      fiber.execute(PollQueue(actor, asyncQueue))
      (fiber, actor)
    }
  }
  
  def stop() {
    actors.foreach({ (fiber : Fiber, actor : ActorBehavior) => fiber.dispose }.tupled)
  }
  
  def send(msg : Any) {
    asyncQueue.put(AsyncMessage(msg))
  }
  
  def call(msg : Any) : Any = {
    val returnQueue = new LinkedTransferQueue[Any]
    asyncQueue.put(CallMessage(msg, returnQueue))    
    returnQueue.poll(Long.MaxValue, TimeUnit.DAYS)
  }
}
