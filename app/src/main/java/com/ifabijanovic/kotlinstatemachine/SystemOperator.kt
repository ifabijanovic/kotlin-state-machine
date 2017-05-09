package com.ifabijanovic.kotlinstatemachine

import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.rxjava2.traits.*
import io.reactivex.subjects.ReplaySubject

/**
 * Created by Ivan Fabijanovic on 06/05/2017.
 */

fun <State, Event> Observable<Any>.system(
        initialState: State,
        accumulator: (State, Event) -> State,
        scheduler: Scheduler,
        vararg feedback: (Observable<State>) -> Observable<Event>
): Observable<State> {
    return Observable.defer {
        val replaySubject: ReplaySubject<State> = ReplaySubject.createWithSize(1)

        val inputs: Observable<Event> = Observable
                .merge(feedback.map { it(replaySubject) })
                .observeOn(scheduler)

        return@defer inputs
                .scan(initialState, accumulator)
                .doOnNext { replaySubject.onNext(it) }
    }
}

fun <State, Event> SharedSequence.Companion.system(
        initialState: State,
        accumulator: (State, Event) -> State,
        vararg feedback: (Driver<State>) -> Driver<Event>
): Driver<State> {
    return Driver.defer {
        val replaySubject: ReplaySubject<State> = ReplaySubject.createWithSize(1)

        val inputs: Driver<Event> = Driver
                .merge(feedback.map { it(replaySubject.asDriverIgnoreError()) })

        return@defer inputs
                .scan(initialState, accumulator)
                .doOnNext { replaySubject.onNext(it) }
    }
}