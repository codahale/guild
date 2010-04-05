package com.codahale.guild

import org.jetlang.core.Callback
import org.jetlang.channels.{AsyncRequest, Request, MemoryRequestChannel}
import java.util.concurrent.TimeUnit
import concurrent.forkjoin.{TransferQueue, LinkedTransferQueue}


trait Callable {
  def call(msg : Any) : Any
}

trait Sendable {
  def send(msg : Any)
}

/**
 * This is dogshit, but I'd rather not break the interface for fucking everyone.
 */
trait ActorBehavior {
  /**
   * An abstract method which is called when the actor receives a message.
   */
  def onMessage(message: Any): Any
  
  /**
   * An abstract method which is called when the actor first starts up
   */
  def onStart() {
    
  }
}

/**
 * An actor class which receives messages in order and safely.
 */
abstract class Actor extends ActorBehavior with Callable with Sendable {
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
   * An abstract method which is called when the actor receives a message.
   */
  def onMessage(message: Any): Any
  
  /**
   * An overridable method which is called when the actor first starts up.
   */
  def onStart {
    
  }

  /**
   * Asynchronously sends a message to the actor.
   */
  def send(msg: Any) {
    fiber.execute(ActorExecution(this, msg))
  }

  /**
   * Synchronously sends a message to the actor and returns the response.
   */
  def call(msg: Any): Any = {
    val queue = new LinkedTransferQueue[Any]

    val fiber = scheduler.createFiber()
    fiber.start()

    val channel = new MemoryRequestChannel[Any, Any]
    channel.subscribe(fiber, RequestCallback(this))
    val disposable = AsyncRequest.withOneReply(fiber, channel, msg, ReplyCallback(queue))

    val value = queue.poll(Long.MaxValue, TimeUnit.DAYS)
    fiber.dispose()
    disposable.dispose()
    return value
  }
}
