package com.ifabijanovic.kotlinstatemachine

import io.reactivex.Observable
import io.reactivex.rxjava2.traits.*

/**
 * Created by Ivan Fabijanovic on 06/05/2017.
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
