package com.codahale.guild

import concurrent.forkjoin.TransferQueue

trait Message

case class AsyncMessage(val msg :Any)

case class CallMessage(val msg : Any, val replyQueue : TransferQueue[Any]) extends Message