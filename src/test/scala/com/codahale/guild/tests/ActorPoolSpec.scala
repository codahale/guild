package com.codahale.guild.tests

import org.scalatest._
import org.scalatest.matchers.MustMatchers
import org.scalatest.mock.MockitoSugar
import com.codahale.guild._
import org.mockito.Mockito.verify

class TestActorFactory extends ActorFactory[TestActor] with MockitoSugar {
  var mockOnMessages = List[Runnable]()
  var mockOnInits = List[Runnable]()
  
  def createActor() : TestActor = {
    val mockOnMsg = mock[Runnable]
    val mockOnInit = mock[Runnable]
    val actor = new TestActor(mockOnMsg, mockOnInit)
    mockOnMessages = mockOnMsg :: mockOnMessages
    mockOnInits = mockOnInit :: mockOnInits
    actor
  }
}

class ActorPoolSpec extends Spec
    with MustMatchers with OneInstancePerTest
    with MockitoSugar with BeforeAndAfterEach {
  
  val factory = new TestActorFactory()
  val pool = new ActorPool(factory)
  
  override protected def beforeEach() {
    pool.start(3)
  }
  
  override protected def afterEach() {
    pool.stop()
  }
  
  describe("an actor pool") {
    it("should load balance across the pool") {
      pool.send('run)
      pool.send('run)
      pool.send('run)
      
      Thread.sleep(50)
      factory.mockOnInits.foreach {m => verify(m).run() }
      factory.mockOnMessages.foreach {m => verify(m).run() }
    }
    
    it ("should execute calls") {
      val answer = pool.call(1).asInstanceOf[Int]
      answer must be(21)
    }
  }
}