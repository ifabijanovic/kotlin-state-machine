package com.bellabeat.statemachine

import io.reactivex.rxjava.traits.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import rx.observers.TestSubscriber
import rx.schedulers.TestScheduler
import java.util.concurrent.TimeUnit

/**
 * Created by juraj on 09/05/2017.
 */

class DictionaryStateMachineTests {
  var scheduler = TestScheduler()
  var observer = TestSubscriber<Map<Int, TestState>>()
  var stateMachine = DriverTraits.schedulerIsNow({ this.scheduler }) {
    DictionaryStateMachine<Int, TestState>({ _ -> { _ -> Driver.empty() } })
  }
  
  @Before
  fun setUp() {
    this.scheduler = TestScheduler()
    this.observer = TestSubscriber()
    DriverTraits.schedulerIsNow({ this.scheduler }) {
      this.stateMachine = DictionaryStateMachine(TestStateFeedbackLoops(this.scheduler)::feedbackLoops)
      this.stateMachine.state.catchErrorAndComplete().drive(this.observer)
    }
  }
  
  @After
  fun tearDown() {
    this.observer.unsubscribe()
    this.stateMachine.dispose()
  }
  
  fun performOp1(key: Int) {
    this.stateMachine.transition(Pair(key, TestState.Operation1(TestState.Operation.Start)))
  }
  
  fun performOp2(key: Int) {
    this.stateMachine.transition(Pair(key, TestState.Operation2(TestState.Operation.Start)))
  }
  
  @Test
  fun singleKeySingleOperation() {
    DriverTraits.schedulerIsNow({ this.scheduler }) {
      this.scheduler.createWorker().schedule({ this.performOp1(1) }, 300, TimeUnit.SECONDS)
      this.scheduler.advanceTimeBy(1000, TimeUnit.SECONDS)
      
      assertEquals(listOf(
          mapOf(),
          mapOf(Pair(1, TestState.Operation1(TestState.Operation.Start))),
          mapOf(Pair(1, TestState.Operation1(TestState.Operation.Work("op1")))),
          mapOf(Pair(1, TestState.Operation1(TestState.Operation.Finish))),
          mapOf()
      ), this.observer.onNextEvents)
    }
  }
  
  @Test
  fun singleKeyMultipleSameSequentialOperations() {
    DriverTraits.schedulerIsNow({ this.scheduler }) {
      this.scheduler.createWorker().schedule({ this.performOp1(1) }, 300, TimeUnit.SECONDS)
      this.scheduler.createWorker().schedule({ this.performOp1(1) }, 400, TimeUnit.SECONDS)
      this.scheduler.createWorker().schedule({ this.performOp1(1) }, 500, TimeUnit.SECONDS)
      this.scheduler.advanceTimeBy(1000, TimeUnit.SECONDS)
      
      assertEquals(listOf(
          mapOf(),
          mapOf(Pair(1, TestState.Operation1(TestState.Operation.Start))),
          mapOf(Pair(1, TestState.Operation1(TestState.Operation.Work("op1")))),
          mapOf(Pair(1, TestState.Operation1(TestState.Operation.Finish))),
          mapOf(),
          mapOf(Pair(1, TestState.Operation1(TestState.Operation.Start))),
          mapOf(Pair(1, TestState.Operation1(TestState.Operation.Work("op1")))),
          mapOf(Pair(1, TestState.Operation1(TestState.Operation.Finish))),
          mapOf(),
          mapOf(Pair(1, TestState.Operation1(TestState.Operation.Start))),
          mapOf(Pair(1, TestState.Operation1(TestState.Operation.Work("op1")))),
          mapOf(Pair(1, TestState.Operation1(TestState.Operation.Finish))),
          mapOf()
      ), this.observer.onNextEvents)
    }
  }
  
  @Test
  fun singleKeyMultipleDifferentSequentialOperations() {
    DriverTraits.schedulerIsNow({ this.scheduler }) {
      this.scheduler.createWorker().schedule({ this.performOp2(1) }, 300, TimeUnit.SECONDS)
      this.scheduler.createWorker().schedule({ this.performOp2(1) }, 400, TimeUnit.SECONDS)
      this.scheduler.createWorker().schedule({ this.performOp1(1) }, 500, TimeUnit.SECONDS)
      this.scheduler.createWorker().schedule({ this.performOp2(1) }, 600, TimeUnit.SECONDS)
      this.scheduler.createWorker().schedule({ this.performOp1(1) }, 700, TimeUnit.SECONDS)
      this.scheduler.advanceTimeBy(1000, TimeUnit.SECONDS)
      
      assertEquals(listOf(
          mapOf(),
          mapOf(Pair(1, TestState.Operation2(TestState.Operation.Start))),
          mapOf(Pair(1, TestState.Operation2(TestState.Operation.Work("op2")))),
          mapOf(Pair(1, TestState.Operation2(TestState.Operation.Finish))),
          mapOf(),
          mapOf(Pair(1, TestState.Operation2(TestState.Operation.Start))),
          mapOf(Pair(1, TestState.Operation2(TestState.Operation.Work("op2")))),
          mapOf(Pair(1, TestState.Operation2(TestState.Operation.Finish))),
          mapOf(),
          mapOf(Pair(1, TestState.Operation1(TestState.Operation.Start))),
          mapOf(Pair(1, TestState.Operation1(TestState.Operation.Work("op1")))),
          mapOf(Pair(1, TestState.Operation1(TestState.Operation.Finish))),
          mapOf(),
          mapOf(Pair(1, TestState.Operation2(TestState.Operation.Start))),
          mapOf(Pair(1, TestState.Operation2(TestState.Operation.Work("op2")))),
          mapOf(Pair(1, TestState.Operation2(TestState.Operation.Finish))),
          mapOf(),
          mapOf(Pair(1, TestState.Operation1(TestState.Operation.Start))),
          mapOf(Pair(1, TestState.Operation1(TestState.Operation.Work("op1")))),
          mapOf(Pair(1, TestState.Operation1(TestState.Operation.Finish))),
          mapOf()
      ), this.observer.onNextEvents)
    }
  }
  
  @Test
  fun singleKeyInterruptOperationWithSameOperation() {
    DriverTraits.schedulerIsNow({ this.scheduler }) {
      this.scheduler.createWorker().schedule({ this.performOp1(1) }, 300, TimeUnit.SECONDS)
      this.scheduler.createWorker().schedule({ this.performOp1(1) }, 320, TimeUnit.SECONDS)
      this.scheduler.createWorker().schedule({ this.performOp1(1) }, 340, TimeUnit.SECONDS)
      this.scheduler.advanceTimeBy(1000, TimeUnit.SECONDS)
      
      assertEquals(listOf(
          mapOf(),
          mapOf(Pair(1, TestState.Operation1(TestState.Operation.Start))),
          mapOf(Pair(1, TestState.Operation1(TestState.Operation.Work("op1")))),
          mapOf(Pair(1, TestState.Operation1(TestState.Operation.Start))),
          mapOf(Pair(1, TestState.Operation1(TestState.Operation.Work("op1")))),
          mapOf(Pair(1, TestState.Operation1(TestState.Operation.Start))),
          mapOf(Pair(1, TestState.Operation1(TestState.Operation.Work("op1")))),
          mapOf(Pair(1, TestState.Operation1(TestState.Operation.Finish))),
          mapOf()
      ), this.observer.onNextEvents)
    }
  }
  
  @Test
  fun singleKeyInterruptOperationWithDifferentOperation() {
    DriverTraits.schedulerIsNow({ this.scheduler }) {
      this.scheduler.createWorker().schedule({ this.performOp1(1) }, 300, TimeUnit.SECONDS)
      this.scheduler.createWorker().schedule({ this.performOp2(1) }, 320, TimeUnit.SECONDS)
      this.scheduler.advanceTimeBy(1000, TimeUnit.SECONDS)
      
      assertEquals(listOf(
          mapOf(),
          mapOf(Pair(1, TestState.Operation1(TestState.Operation.Start))),
          mapOf(Pair(1, TestState.Operation1(TestState.Operation.Work("op1")))),
          mapOf(Pair(1, TestState.Operation2(TestState.Operation.Start))),
          mapOf(Pair(1, TestState.Operation2(TestState.Operation.Work("op2")))),
          mapOf(Pair(1, TestState.Operation2(TestState.Operation.Finish))),
          mapOf()
      ), this.observer.onNextEvents)
    }
  }
  
  @Test
  fun twoKeysSameOperationSequential() {
    DriverTraits.schedulerIsNow({ this.scheduler }) {
      this.scheduler.createWorker().schedule({ this.performOp1(1) }, 300, TimeUnit.SECONDS)
      this.scheduler.createWorker().schedule({ this.performOp1(2) }, 400, TimeUnit.SECONDS)
      this.scheduler.advanceTimeBy(1000, TimeUnit.SECONDS)
      
      assertEquals(listOf(
          mapOf(),
          mapOf(Pair(1, TestState.Operation1(TestState.Operation.Start))),
          mapOf(Pair(1, TestState.Operation1(TestState.Operation.Work("op1")))),
          mapOf(Pair(1, TestState.Operation1(TestState.Operation.Finish))),
          mapOf(),
          mapOf(Pair(2, TestState.Operation1(TestState.Operation.Start))),
          mapOf(Pair(2, TestState.Operation1(TestState.Operation.Work("op1")))),
          mapOf(Pair(2, TestState.Operation1(TestState.Operation.Finish))),
          mapOf()
      ), this.observer.onNextEvents)
    }
  }
  
  @Test
  fun twoKeysDifferentOperationSequential() {
    DriverTraits.schedulerIsNow({ this.scheduler }) {
      this.scheduler.createWorker().schedule({ this.performOp1(1) }, 300, TimeUnit.SECONDS)
      this.scheduler.createWorker().schedule({ this.performOp2(2) }, 400, TimeUnit.SECONDS)
      this.scheduler.advanceTimeBy(1000, TimeUnit.SECONDS)
      
      assertEquals(listOf(
          mapOf(),
          mapOf(Pair(1, TestState.Operation1(TestState.Operation.Start))),
          mapOf(Pair(1, TestState.Operation1(TestState.Operation.Work("op1")))),
          mapOf(Pair(1, TestState.Operation1(TestState.Operation.Finish))),
          mapOf(),
          mapOf(Pair(2, TestState.Operation2(TestState.Operation.Start))),
          mapOf(Pair(2, TestState.Operation2(TestState.Operation.Work("op2")))),
          mapOf(Pair(2, TestState.Operation2(TestState.Operation.Finish))),
          mapOf()
      ), this.observer.onNextEvents)
    }
  }
  
  @Test
  fun twoKeysSameOperationOverlapping() {
    DriverTraits.schedulerIsNow({ this.scheduler }) {
      this.scheduler.createWorker().schedule({ this.performOp1(1) }, 300, TimeUnit.SECONDS)
      this.scheduler.createWorker().schedule({ this.performOp1(2) }, 310, TimeUnit.SECONDS)
      this.scheduler.advanceTimeBy(1000, TimeUnit.SECONDS)
      
      assertEquals(listOf(
          mapOf(),
          mapOf(Pair(1, TestState.Operation1(TestState.Operation.Start))),
          mapOf(Pair(1, TestState.Operation1(TestState.Operation.Work("op1")))),
          mapOf(
              Pair(1, TestState.Operation1(TestState.Operation.Work("op1"))),
              Pair(2, TestState.Operation1(TestState.Operation.Start))
          ),
          mapOf(
              Pair(1, TestState.Operation1(TestState.Operation.Work("op1"))),
              Pair(2, TestState.Operation1(TestState.Operation.Work("op1")))
          ),
          mapOf(
              Pair(1, TestState.Operation1(TestState.Operation.Finish)),
              Pair(2, TestState.Operation1(TestState.Operation.Work("op1")))
          ),
          mapOf(Pair(2, TestState.Operation1(TestState.Operation.Work("op1")))),
          mapOf(Pair(2, TestState.Operation1(TestState.Operation.Finish))),
          mapOf()
      ), this.observer.onNextEvents)
    }
  }
  
  @Test
  fun twoKeysDifferentOperationOverlapping() {
    DriverTraits.schedulerIsNow({ this.scheduler }) {
      this.scheduler.createWorker().schedule({ this.performOp2(1) }, 300, TimeUnit.SECONDS)
      this.scheduler.createWorker().schedule({ this.performOp1(2) }, 310, TimeUnit.SECONDS)
      this.scheduler.advanceTimeBy(1000, TimeUnit.SECONDS)
      
      assertEquals(listOf(
          mapOf(),
          mapOf(Pair(1, TestState.Operation2(TestState.Operation.Start))),
          mapOf(Pair(1, TestState.Operation2(TestState.Operation.Work("op2")))),
          mapOf(
              Pair(1, TestState.Operation2(TestState.Operation.Work("op2"))),
              Pair(2, TestState.Operation1(TestState.Operation.Start))
          ),
          mapOf(
              Pair(1, TestState.Operation2(TestState.Operation.Work("op2"))),
              Pair(2, TestState.Operation1(TestState.Operation.Work("op1")))
          ),
          mapOf(
              Pair(1, TestState.Operation2(TestState.Operation.Finish)),
              Pair(2, TestState.Operation1(TestState.Operation.Work("op1")))
          ),
          mapOf(Pair(2, TestState.Operation1(TestState.Operation.Work("op1")))),
          mapOf(Pair(2, TestState.Operation1(TestState.Operation.Finish))),
          mapOf()
      ), this.observer.onNextEvents)
    }
  }
  
  @Test
  fun twoKeysDifferentOperationOverlappingWithInterrupts() {
    DriverTraits.schedulerIsNow({ this.scheduler }) {
      this.scheduler.createWorker().schedule({ this.performOp2(1) }, 300, TimeUnit.SECONDS)
      this.scheduler.createWorker().schedule({ this.performOp1(2) }, 310, TimeUnit.SECONDS)
      this.scheduler.createWorker().schedule({ this.performOp1(1) }, 313, TimeUnit.SECONDS)
      this.scheduler.createWorker().schedule({ this.performOp2(2) }, 337, TimeUnit.SECONDS)
      this.scheduler.advanceTimeBy(1000, TimeUnit.SECONDS)
      
      assertEquals(listOf(
          mapOf(),
          mapOf(Pair(1, TestState.Operation2(TestState.Operation.Start))),
          mapOf(Pair(1, TestState.Operation2(TestState.Operation.Work("op2")))),
          mapOf(
              Pair(1, TestState.Operation2(TestState.Operation.Work("op2"))),
              Pair(2, TestState.Operation1(TestState.Operation.Start))
          ),
          mapOf(
              Pair(1, TestState.Operation1(TestState.Operation.Start)),
              Pair(2, TestState.Operation1(TestState.Operation.Start))
          ),
          mapOf(
              Pair(1, TestState.Operation1(TestState.Operation.Start)),
              Pair(2, TestState.Operation1(TestState.Operation.Work("op1")))
          ),
          mapOf(
              Pair(1, TestState.Operation1(TestState.Operation.Work("op1"))),
              Pair(2, TestState.Operation1(TestState.Operation.Work("op1")))
          ),
          mapOf(
              Pair(1, TestState.Operation1(TestState.Operation.Work("op1"))),
              Pair(2, TestState.Operation1(TestState.Operation.Finish))
          ),
          mapOf(
              Pair(1, TestState.Operation1(TestState.Operation.Work("op1"))),
              Pair(2, TestState.Operation2(TestState.Operation.Start))
          ),
          mapOf(
              Pair(1, TestState.Operation1(TestState.Operation.Finish)),
              Pair(2, TestState.Operation2(TestState.Operation.Start))
          ),
          mapOf(
              Pair(1, TestState.Operation1(TestState.Operation.Finish)),
              Pair(2, TestState.Operation2(TestState.Operation.Work("op2")))
          ),
          mapOf(Pair(2, TestState.Operation2(TestState.Operation.Work("op2")))),
          mapOf(Pair(2, TestState.Operation2(TestState.Operation.Finish))),
          mapOf()
      ), this.observer.onNextEvents)
    }
  }
}
