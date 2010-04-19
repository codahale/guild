package com.codahale.guild

import concurrent.forkjoin.TransferQueue

sealed abstract class Message[-M]

case class AsyncMessage[M](val msg : M) extends Message[M]
case class CallMessage[M,R](val msg : M, val replyQueue : TransferQueue[Message[R]]) extends Message[M]
case class ErrorMessage(val ex : Throwable) extends Message[Any]
