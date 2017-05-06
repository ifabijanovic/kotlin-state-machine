package com.ifabijanovic.kotlinstatemachine

import io.reactivex.Observable

/**
 * Created by Ivan Fabijanovic on 06/05/2017.
 */

fun <Element, State, Emit> scanAndMaybeEmit(
        observable: Observable<Element>,
        state: State,
        accumulator: (Pair<State, Element>
        ) -> Pair<State, Emit?>): Observable<Emit> {
    return observable
            .scan(Pair<State, Emit?>(state, null), { stateEmitPair, element ->
                accumulator(Pair(stateEmitPair.first, element))
            })
            .flatMap { stateEmitPair ->
                stateEmitPair.second?.let { Observable.just(it) } ?: Observable.empty()
            }
}
