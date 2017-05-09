package com.bellabeat.statemachine

import io.reactivex.rxjava.traits.*
import rx.Observable
import java.util.concurrent.TimeUnit
import rx.schedulers.TestScheduler

/**
 * Created by juraj on 09/05/2017.
 */

sealed class TestState {
    sealed class Operation {
        object Start : Operation()
        data class Work(val data: String) : Operation()
        object Finish : Operation()
    }

    data class Operation1(val state: Operation): TestState()
    data class Operation2(val state: Operation): TestState()
    object Cancel: TestState()
    data class Error(val error: Throwable): TestState()
}

class TestStateFeedbackLoops(val scheduler: TestScheduler) {
    fun feedbackLoops(key: Int): (Driver<TestState>) -> Driver<Command<Int, TestState>> {
        return { state ->
            state.switchMap { state ->
                Observable.defer {
                    when (state) {
                        is TestState.Operation1 -> {
                            when (state.state) {
                                is TestState.Operation.Start -> this.update(5, key, TestState.Operation1(TestState.Operation.Work("op1")))
                                is TestState.Operation.Work -> this.update(20, key, TestState.Operation1(TestState.Operation.Finish))
                                is TestState.Operation.Finish -> this.finish(5, key)
                            }
                        }
                        is TestState.Operation2 -> {
                            when (state.state) {
                                is TestState.Operation.Start -> this.update(5, key, TestState.Operation2(TestState.Operation.Work("op2")))
                                is TestState.Operation.Work -> this.update(20, key, TestState.Operation2(TestState.Operation.Finish))
                                is TestState.Operation.Finish -> this.finish(5, key)
                            }
                        }
                        is TestState.Cancel -> this.finish(0, key)
                        is TestState.Error -> this.finish(0, key)
                    }
                }.onErrorReturn { Command.Update(Pair(key, TestState.Error(it))) }
                        .asDriverIgnoreError()
            }
        }
    }

    private fun update(period: Long, key: Int, state: TestState): Observable<Command<Int, TestState>> {
        return Observable
                .interval(period, TimeUnit.SECONDS, this.scheduler)
                .take(1)
                .map { Command.Update(Pair(key, state)) }
    }

    private fun finish(period: Long, key: Int): Observable<Command<Int, TestState>> {
        return Observable
                .interval(period, TimeUnit.SECONDS, this.scheduler)
                .take(1)
                .map { Command.Finish<Int, TestState>(key) }
    }
}
