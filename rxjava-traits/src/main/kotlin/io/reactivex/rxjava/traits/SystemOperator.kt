package io.reactivex.rxjava.traits

import rx.Observable
import rx.Scheduler
import rx.subjects.ReplaySubject

/**
 * Created by juraj on 09/05/2017.
 */

fun <S, C> Observable<Any>.system(
    initState: S,
    accumulator: (S, C) -> S,
    scheduler: Scheduler,
    vararg feedbacks: (Observable<S>) -> Observable<C>): Observable<S> {
  return Observable.defer {
    val replaySubject: ReplaySubject<S> = ReplaySubject.createWithSize(1)
    
    val command = Observable.merge(feedbacks.map { it(replaySubject) })
        .observeOn(scheduler)
    
    command.scan(initState, accumulator)
        .doOnNext { replaySubject.onNext(it) }
  }
}

fun <S, C> SharedSequence.Companion.system(
    initialState: S,
    accumulator: (S, C) -> S,
    vararg feedback: (Driver<S>) -> Driver<C>): Driver<S> {
  return Driver.defer {
    val replaySubject: ReplaySubject<S> = ReplaySubject.createWithSize(1)
    
    val command = Driver.merge(feedback.map { it(replaySubject.asDriverIgnoreError()) })
    
    command.scan(initialState, accumulator)
        .doOnNext { replaySubject.onNext(it) }
  }
}
