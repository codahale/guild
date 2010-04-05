package com.codahale.guild

import org.jetlang.core.Callback
import org.jetlang.channels.{AsyncRequest, Request, MemoryRequestChannel}
import java.util.concurrent.TimeUnit
import concurrent.forkjoin.{TransferQueue, LinkedTransferQueue}


/**
 * A callback wrapper which sends a message to an actor and relays its reply
 * to the scheduler.
 */
private case class RequestCallback(actor: ActorBehavior) extends Callback[Request[Any, Any]] {
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

private case class ActorInit(actor : ActorBehavior) extends Runnable {
  def run() {
    actor.onStart
  }
}

private case class PollQueue(actor : ActorBehavior, queue : TransferQueue[Any]) extends Runnable {
  def run() {
    try {
      queue.poll(Long.MaxValue, TimeUnit.DAYS) match {
        case CallMessage(msg, replyQueue) =>
          replyQueue.put(actor.onMessage(msg))
        case AsyncMessage(msg) =>
          println("got message")
          actor.onMessage(msg)
      }
    } catch {
      case e : InterruptedException => run
    }
  }
}

private case class AsyncCallback(actor : ActorBehavior) extends Callback[Any] {
  def onMessage(msg : Any) {
    actor.onMessage(msg)
  }
}

/**
 * A callback wrapper which sends a message to an actor.
 */
private case class ActorExecution(actor: ActorBehavior, msg: Any) extends Runnable {
  def run() {
    actor.onMessage(msg)
  }
}