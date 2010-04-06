package com.codahale.guild

import org.jetlang.fibers.Fiber
import org.jetlang.core.Callback
import org.jetlang.channels.{MemoryChannel, AsyncRequest, Request, MemoryRequestChannel}
import java.util.concurrent.TimeUnit
import concurrent.forkjoin.{TransferQueue, LinkedTransferQueue}

trait ActorFactory[M,R,T <: ActorBehavior[M,R]] {
  def createActor() : T
}

/**
 * Implements a pool of actors which all consume from a single message channel.
 */
class ActorPool[M,R, T <: ActorBehavior[M,R]](factory : ActorFactory[M,R,T]) extends Sendable[M] with Callable[M,R] {
  /**
   * Override this method to use a different scheduler.
   */
  protected def scheduler = Scheduler.Default
  protected val asyncQueue = new LinkedTransferQueue[Message[M]]
  
  protected var actors = Seq[(Fiber, ActorBehavior[M,R])]()
  
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
    actors.foreach({ (fiber : Fiber, actor : ActorBehavior[M,R]) => fiber.dispose }.tupled)
  }
  
  def send(msg : M) {
    asyncQueue.put(AsyncMessage(msg))
  }
  
  def call(msg : M) : R = {
    val returnQueue = new LinkedTransferQueue[Message[R]]
    asyncQueue.put(CallMessage(msg, returnQueue))
    returnQueue.poll(Long.MaxValue, TimeUnit.DAYS) match {
      case CallMessage(msg, _) => msg.asInstanceOf[R]
      case AsyncMessage(msg) => msg.asInstanceOf[R]
      case ErrorMessage(ex) => throw ex
    }
  }
}
