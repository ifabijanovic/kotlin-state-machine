package com.ifabijanovic.kotlinstatemachine

import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.subjects.ReplaySubject

/**
 * Created by Ivan Fabijanovic on 06/05/2017.
 */

fun <R, Element> system(
        initialState: R,
        accumulator: (R, Element) -> R,
        scheduler: Scheduler,
        vararg feedback: (Observable<R>) -> Observable<Element>
): Observable<R> {
    return Observable.defer {
        val replaySubject: ReplaySubject<R> = ReplaySubject.createWithSize(1)

        val inputs: Observable<Element> = Observable
                .merge(feedback.map { it(replaySubject) })
                .observeOn(scheduler)

        return@defer inputs
                .scan(initialState, accumulator)
                .startWith(initialState)
                .doOnNext { replaySubject.onNext(it) }
    }
}
