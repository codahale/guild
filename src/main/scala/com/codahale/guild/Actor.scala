package com.codahale.guild

import org.jetlang.channels.{AsyncRequest, MemoryRequestChannel}
import java.util.concurrent.TimeUnit
import concurrent.forkjoin.{LinkedTransferQueue}


trait Callable[-M, +R] {
  def call(msg : M) : R
}

trait Sendable[-M] {
  def send(msg : M)
}

/**
 * This is dogshit, but I'd rather not break the interface for fucking everyone.
 */
trait ActorBehavior[-M, +R] {
  /**
   * An abstract method which is called when the actor receives a message.
   */
  def onMessage(message: M): R
  
  /**
   * An abstract method which is called when the actor first starts up
   */
  def onStart() {
    
  }
}

/**
 * An actor class which receives messages in order and safely.
 */
abstract class Actor[M,R] extends ActorBehavior[M,R] with Callable[M, R] with Sendable[M] {
  /**
   * Override this method to use a different scheduler.
   */
  protected def scheduler = Scheduler.Default
  private val fiber = scheduler.createFiber()

  /**
   * Starts the actor. Actors which are not started do not process messages.
   */
  def start() = {
    fiber.start()
    fiber.execute(ActorInit(this))
  }

  /**
   * Stops the actor. Actors which have been stopped cannot be restarted.
   */
  def stop() = fiber.dispose()
  
  /**
   * An overridable method which is called when the actor first starts up.
   */
  override def onStart {
    
  }

  /**
   * Asynchronously sends a message to the actor.
   */
  def send(msg: M) {
    fiber.execute(ActorExecution(this, msg))
  }

  /**
   * Synchronously sends a message to the actor and returns the response.
   */
  def call(msg: M): R = {
    val queue = new LinkedTransferQueue[R]

    val fiber = scheduler.createFiber()
    fiber.start()

    val channel = new MemoryRequestChannel[M, R]
    channel.subscribe(fiber, RequestCallback(this))
    val disposable = AsyncRequest.withOneReply(fiber, channel, msg, ReplyCallback(queue))

    val value = queue.poll(Long.MaxValue, TimeUnit.DAYS)
    fiber.dispose()
    disposable.dispose()
    return value
  }
}
