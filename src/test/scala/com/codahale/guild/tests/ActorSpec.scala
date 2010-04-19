package com.codahale.guild.tests

import org.scalatest._
import org.scalatest.matchers.MustMatchers
import org.scalatest.mock.MockitoSugar
import com.codahale.guild.Actor
import org.mockito.Mockito.verify

class TestActor(r: Runnable, initR : Runnable) extends Actor[Any, Any] {
  override def onStart {
    initR.run
  }
  
  def onMessage(message: Any) = {
    message match {
      case i: Int => i + 20
      case 'ex =>
        Thread.sleep(10)
        throw new Exception("fuck right off")
      case 'run => 
        Thread.sleep(10)
        r.run()
    }
  }
}

class StartingActor(r: Runnable) extends Actor[Any,Any] {
  override def onStart() {
    r.run()
  }
  
  def onMessage(msg: Any) = null
}

class ActorSpec extends Spec
        with MustMatchers with OneInstancePerTest
        with MockitoSugar with BeforeAndAfterEach {
  
  val runnable = mock[Runnable]
  val initR = mock[Runnable]
  val actor = new TestActor(runnable, initR)

  override protected def beforeEach() {
    actor.start()
  }

  override protected def afterEach() {
    actor.stop()
  }

  describe("an actor") {
    it("receives messages") {
      actor.send('run)

      Thread.sleep(50)
      verify(initR).run()
      verify(runnable).run()
    }

    it("replies to messages") {
      val i = actor.call(1).asInstanceOf[Int]

      i must be(21)
    }
  }
  
  describe("an actor being started") {
    val actor = new StartingActor(runnable)
    
    it("runs its onStart method") {
      actor.start()
      Thread.sleep(50)
      verify(runnable).run()
      actor.stop()
    }
  }




}
