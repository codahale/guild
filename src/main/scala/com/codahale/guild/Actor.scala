package com.codahale.guild

import org.jetlang.core.Callback
import org.jetlang.channels.{AsyncRequest, Request, MemoryRequestChannel}
import java.util.concurrent.TimeUnit
import concurrent.forkjoin.{TransferQueue, LinkedTransferQueue}

/**
 * A callback wrapper which sends a message to an actor and relays its reply
 * to the scheduler.
 */
private case class RequestCallback(actor: Actor) extends Callback[Request[Any, Any]] {
  def onMessage(message: Request[Any, Any]) {
    message.reply(actor.onMessage(message.getRequest))
  }
}

/**
 * A callback wrapper which relays the reply to the receiver.
 */
private case class ReplyCallback(queue: TransferQueue[Any]) extends Callback[Any] {
  def onMessage(reply: Any) {
    queue.put(reply)
  }
}

/**
 * A callback wrapper which sends a message to an actor.
 */
private case class ActorExecution(actor: Actor, msg: Any) extends Runnable {
  def run() {
    actor.onMessage(msg)
  }
}

/**
 * An actor class which receives messages in order and safely.
 */
abstract class Actor {
  /**
   * Override this method to use a different scheduler.
   */
  protected def scheduler = Scheduler.Default
  private val fiber = scheduler.createFiber()

  /**
   * Starts the actor. Actors which are not started do not process messages.
   */
  def start() = fiber.start()

  /**
   * Stops the actor. Actors which have been stopped cannot be restarted.
   */
  def stop() = fiber.dispose()

  /**
   * An abstract method which is called when the actor receives a message.
   */
  def onMessage(message: Any): Any

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
    AsyncRequest.withOneReply(fiber, channel, msg, ReplyCallback(queue))

    val value = queue.poll(Long.MaxValue, TimeUnit.DAYS)
    fiber.dispose()
    return value
  }
}
