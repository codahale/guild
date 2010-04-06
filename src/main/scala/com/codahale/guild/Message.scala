package com.codahale.guild

import concurrent.forkjoin.TransferQueue

sealed abstract class Message[-M]

case class AsyncMessage[M](val msg : M) extends Message
case class CallMessage[M,R](val msg : M, val replyQueue : TransferQueue[R]) extends Message
