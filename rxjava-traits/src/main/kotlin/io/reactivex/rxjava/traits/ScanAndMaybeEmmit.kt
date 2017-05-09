package io.reactivex.rxjava.traits

import rx.Observable

/**
 * Created by juraj on 09/05/2017.
 */

fun <Element, State, Emit> Observable<Element>.scanAndMaybeEmit(
    state: State,
    accumulator: (Pair<State, Element>) -> Pair<State, Emit?>): Observable<Emit> {
  return this
      .scan(Pair<State, Emit?>(state, null), { (state, _), element ->
        accumulator(Pair(state, element))
      })
      .flatMap { stateEmitPair ->
        stateEmitPair.second?.let { Observable.just(it) } ?: Observable.empty()
      }
}

fun <Element, State, Emit> Driver<Element>.scanAndMaybeEmit(
    state: State,
    accumulator: (Pair<State, Element>) -> Pair<State, Emit?>): Driver<Emit> {
  return this
      .scan(Pair<State, Emit?>(state, null), { (state, _), element ->
        accumulator(Pair(state, element))
      })
      .flatMap { stateEmitPair ->
        stateEmitPair.second?.let { Driver.just(it) } ?: Driver.empty()
      }
}
