package com.ifabijanovic.kotlinstatemachine

import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import io.reactivex.rxjava2.traits.*
import io.reactivex.schedulers.TestScheduler
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * Created by Ivan Fabijanovic on 06/05/2017.
 */

class DictionaryStateMachineTests {
    var scheduler = TestScheduler()
    var observer = TestObserver<Map<Int, TestState>>()
    var stateMachine = DictionaryStateMachine<Int,TestState>({ _ -> { _ -> Driver.empty() } })

    @Before
    fun setUp() {
        this.scheduler = TestScheduler()
        this.observer = TestObserver()
        DriverTraits.schedulerIsNow({ this.scheduler }) {
            this.stateMachine = DictionaryStateMachine(TestStateFeedbackLoops(this.scheduler)::feedbackLoops)
            this.stateMachine.state.drive(this.observer)
        }
    }

    @After
    fun tearDown() {
        this.observer.dispose()
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
        this.scheduler.createWorker().schedule({ this.performOp1(1) }, 300, TimeUnit.SECONDS)
        this.scheduler.advanceTimeBy(1000, TimeUnit.SECONDS)

        assertEquals(this.observer.values(), listOf(
                mapOf(),
                mapOf(Pair(1, TestState.Operation1(TestState.Operation.Start))),
                mapOf(Pair(1, TestState.Operation1(TestState.Operation.Work("op1")))),
                mapOf(Pair(1, TestState.Operation1(TestState.Operation.Finish))),
                mapOf()
        ))
    }

    @Test
    fun singleKeyMultipleSameSequentialOperations() {
        this.scheduler.createWorker().schedule({ this.performOp1(1) }, 300, TimeUnit.SECONDS)
        this.scheduler.createWorker().schedule({ this.performOp1(1) }, 400, TimeUnit.SECONDS)
        this.scheduler.createWorker().schedule({ this.performOp1(1) }, 500, TimeUnit.SECONDS)
        this.scheduler.advanceTimeBy(1000, TimeUnit.SECONDS)

        assertEquals(this.observer.values(), listOf(
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
        ))
    }

    @Test
    fun singleKeyMultipleDifferentSequentialOperations() {
        this.scheduler.createWorker().schedule({ this.performOp2(1) }, 300, TimeUnit.SECONDS)
        this.scheduler.createWorker().schedule({ this.performOp2(1) }, 400, TimeUnit.SECONDS)
        this.scheduler.createWorker().schedule({ this.performOp1(1) }, 500, TimeUnit.SECONDS)
        this.scheduler.createWorker().schedule({ this.performOp2(1) }, 600, TimeUnit.SECONDS)
        this.scheduler.createWorker().schedule({ this.performOp1(1) }, 700, TimeUnit.SECONDS)
        this.scheduler.advanceTimeBy(1000, TimeUnit.SECONDS)

        assertEquals(this.observer.values(), listOf(
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
        ))
    }

    @Test
    fun singleKeyInterruptOperationWithSameOperation() {
        this.scheduler.createWorker().schedule({ this.performOp1(1) }, 300, TimeUnit.SECONDS)
        this.scheduler.createWorker().schedule({ this.performOp1(1) }, 320, TimeUnit.SECONDS)
        this.scheduler.createWorker().schedule({ this.performOp1(1) }, 340, TimeUnit.SECONDS)
        this.scheduler.advanceTimeBy(1000, TimeUnit.SECONDS)

        assertEquals(this.observer.values(), listOf(
                mapOf(),
                mapOf(Pair(1, TestState.Operation1(TestState.Operation.Start))),
                mapOf(Pair(1, TestState.Operation1(TestState.Operation.Work("op1")))),
                mapOf(Pair(1, TestState.Operation1(TestState.Operation.Start))),
                mapOf(Pair(1, TestState.Operation1(TestState.Operation.Work("op1")))),
                mapOf(Pair(1, TestState.Operation1(TestState.Operation.Start))),
                mapOf(Pair(1, TestState.Operation1(TestState.Operation.Work("op1")))),
                mapOf(Pair(1, TestState.Operation1(TestState.Operation.Finish))),
                mapOf()
        ))
    }

    @Test
    fun singleKeyInterruptOperationWithDifferentOperation() {
        this.scheduler.createWorker().schedule({ this.performOp1(1) }, 300, TimeUnit.SECONDS)
        this.scheduler.createWorker().schedule({ this.performOp2(1) }, 320, TimeUnit.SECONDS)
        this.scheduler.advanceTimeBy(1000, TimeUnit.SECONDS)

        assertEquals(this.observer.values(), listOf(
                mapOf(),
                mapOf(Pair(1, TestState.Operation1(TestState.Operation.Start))),
                mapOf(Pair(1, TestState.Operation1(TestState.Operation.Work("op1")))),
                mapOf(Pair(1, TestState.Operation2(TestState.Operation.Start))),
                mapOf(Pair(1, TestState.Operation2(TestState.Operation.Work("op2")))),
                mapOf(Pair(1, TestState.Operation2(TestState.Operation.Finish))),
                mapOf()
        ))
    }

    @Test
    fun twoKeysSameOperationSequential() {
        this.scheduler.createWorker().schedule({ this.performOp1(1) }, 300, TimeUnit.SECONDS)
        this.scheduler.createWorker().schedule({ this.performOp1(2) }, 400, TimeUnit.SECONDS)
        this.scheduler.advanceTimeBy(1000, TimeUnit.SECONDS)

        assertEquals(this.observer.values(), listOf(
                mapOf(),
                mapOf(Pair(1, TestState.Operation1(TestState.Operation.Start))),
                mapOf(Pair(1, TestState.Operation1(TestState.Operation.Work("op1")))),
                mapOf(Pair(1, TestState.Operation1(TestState.Operation.Finish))),
                mapOf(),
                mapOf(Pair(2, TestState.Operation1(TestState.Operation.Start))),
                mapOf(Pair(2, TestState.Operation1(TestState.Operation.Work("op1")))),
                mapOf(Pair(2, TestState.Operation1(TestState.Operation.Finish))),
                mapOf()
        ))
    }

    @Test
    fun twoKeysDifferentOperationSequential() {
        this.scheduler.createWorker().schedule({ this.performOp1(1) }, 300, TimeUnit.SECONDS)
        this.scheduler.createWorker().schedule({ this.performOp2(2) }, 400, TimeUnit.SECONDS)
        this.scheduler.advanceTimeBy(1000, TimeUnit.SECONDS)

        assertEquals(this.observer.values(), listOf(
                mapOf(),
                mapOf(Pair(1, TestState.Operation1(TestState.Operation.Start))),
                mapOf(Pair(1, TestState.Operation1(TestState.Operation.Work("op1")))),
                mapOf(Pair(1, TestState.Operation1(TestState.Operation.Finish))),
                mapOf(),
                mapOf(Pair(2, TestState.Operation2(TestState.Operation.Start))),
                mapOf(Pair(2, TestState.Operation2(TestState.Operation.Work("op2")))),
                mapOf(Pair(2, TestState.Operation2(TestState.Operation.Finish))),
                mapOf()
        ))
    }

    @Test
    fun twoKeysSameOperationOverlapping() {
        this.scheduler.createWorker().schedule({ this.performOp1(1) }, 300, TimeUnit.SECONDS)
        this.scheduler.createWorker().schedule({ this.performOp1(2) }, 310, TimeUnit.SECONDS)
        this.scheduler.advanceTimeBy(1000, TimeUnit.SECONDS)

        assertEquals(this.observer.values(), listOf(
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
        ))
    }

    @Test
    fun twoKeysDifferentOperationOverlapping() {
        this.scheduler.createWorker().schedule({ this.performOp2(1) }, 300, TimeUnit.SECONDS)
        this.scheduler.createWorker().schedule({ this.performOp1(2) }, 310, TimeUnit.SECONDS)
        this.scheduler.advanceTimeBy(1000, TimeUnit.SECONDS)

        assertEquals(this.observer.values(), listOf(
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
        ))
    }

    @Test
    fun twoKeysDifferentOperationOverlappingWithInterrupts() {
        this.scheduler.createWorker().schedule({ this.performOp2(1) }, 300, TimeUnit.SECONDS)
        this.scheduler.createWorker().schedule({ this.performOp1(2) }, 310, TimeUnit.SECONDS)
        this.scheduler.createWorker().schedule({ this.performOp1(1) }, 313, TimeUnit.SECONDS)
        this.scheduler.createWorker().schedule({ this.performOp2(2) }, 337, TimeUnit.SECONDS)
        this.scheduler.advanceTimeBy(1000, TimeUnit.SECONDS)

        assertEquals(this.observer.values(), listOf(
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
        ))
    }
}
